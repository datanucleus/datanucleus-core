/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contributors:
2006 Kundan Varma - basic annotation reading classes
    ...
**********************************************************************/
package org.datanucleus.metadata.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.PackageMetaData;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;

/**
 * Abstract implementation of a metadata annotations reader.
 * A metadata annotation reader takes in a class and converts its annotations into internal metadata.
 * Any implementation has to implement the method "processClassAnnotations" which creates the ClassMetaData 
 * record for the class, and the method "processFieldAnnotations" which updates the ClassMetaData with its
 * field definition.
 * <p>Each annotation reader supports a set of annotations. So it could support "JPA" annotations, 
 * or "JDO" annotations, or "DataNucleus" annotations or whatever.
 */
public abstract class AbstractAnnotationReader implements AnnotationReader
{
    /** Manager for MetaData operations */
    protected MetaDataManager mmgr;

    /** Supported annotations packages. */
    protected String[] supportedPackages;

    /**
     * Constructor.
     * @param mgr MetaData manager
     */
    public AbstractAnnotationReader(MetaDataManager mgr)
    {
        this.mmgr = mgr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.annotations.AnnotationReader#isPersistenceContext()
     */
    @Override
    public boolean isPersistenceContext()
    {
        return (mmgr.getNucleusContext() instanceof PersistenceNucleusContext);
    }

    /**
     * Method to set the valid annotation packages to be supported when reading.
     * @return The supported packages.
     */
    public String[] getSupportedAnnotationPackages()
    {
        return this.supportedPackages;
    }

    /**
     * Method to set the valid annotation packages to be supported when reading.
     * @param packages The supported packages.
     */
    protected void setSupportedAnnotationPackages(String[] packages)
    {
        this.supportedPackages = packages;
    }

    /**
     * Convenience method to check whether an annotation class name is supported by this reader.
     * @param annotationClassName Name of the annotation class
     * @return Whether it is supported.
     */
    protected boolean isSupportedAnnotation(String annotationClassName)
    {
        if (supportedPackages == null)
        {
            return false;
        }

        for (String supportedPackage : supportedPackages)
        {
            if (annotationClassName.startsWith(supportedPackage))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Accessor for the ClassMetaData for the specified class from its annotations.
     * The returned ClassMetaData will be unpopulated and uninitialised.
     * @param cls The class
     * @param pmd MetaData for the owning package
     * @param clr ClassLoader resolver
     * @return The ClassMetaData
     */
    public AbstractClassMetaData getMetaDataForClass(Class cls, PackageMetaData pmd, ClassLoaderResolver clr)
    {
        AnnotationObject[] classAnnotations = getClassAnnotationsForClass(cls);
        if (classAnnotations == null || classAnnotations.length == 0)
        {
            return null;
        }

        AbstractClassMetaData cmd = processClassAnnotations(pmd, cls, classAnnotations, clr);
        if (cmd != null)
        {
            // Process using any user-provided class annotation handlers
            AnnotationManager annMgr = mmgr.getAnnotationManager();
            for (AnnotationObject classAnnotation : classAnnotations)
            {
                String annName = classAnnotation.getName();
                ClassAnnotationHandler handler = annMgr.getHandlerForClassAnnotation(annName);
                if (handler != null)
                {
                    handler.processClassAnnotation(classAnnotation, cmd, clr);
                }
            }

            if (cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                Collection<AnnotatedMember> annotatedFields = getFieldAnnotationsForClass(cls);
                Collection<AnnotatedMember> annotatedGetterMethods = getJavaBeanAccessorAnnotationsForClass(cls);

                // Check for duplicate field/method and put on to field
                for (AnnotatedMember field : annotatedFields)
                {
                    Iterator<AnnotatedMember> methodIter = annotatedGetterMethods.iterator();
                    while (methodIter.hasNext())
                    {
                        AnnotatedMember method = methodIter.next();
                        if (field.getName().equals(method.getName()))
                        {
                            NucleusLogger.METADATA.info("Processable annotations specified on both field and getter for " + cls.getName() + "." + field.getName() + " so using as FIELD");
                            field.addAnnotations(method.getAnnotations());
                            methodIter.remove();
                            break;
                        }
                    }
                }

                // Check if we are using property or field access
                boolean propertyAccessor = false;
                if (cmd.getAccessViaField() != null)
                {
                    // Respect @Access setting if provided
                    if (cmd.getAccessViaField() == Boolean.FALSE)
                    {
                        propertyAccessor = true;
                    }
                }
                else
                {
                    // Extract the annotations for the getters
                    for (AnnotatedMember method : annotatedGetterMethods)
                    {
                        if (method.getAnnotations().length > 0)
                        {
                            // We have a getter with at least one annotation so must be using properties
                            propertyAccessor = true;
                        }
                    }
                }

                // TODO Clear up whether we are processing just properties or just fields or mixed mode. Currently we process both
                // Process the getters
                for (AnnotatedMember method : annotatedGetterMethods)
                {
                    AnnotationObject[] annotations = method.getAnnotations();

                    if ((annotations == null || annotations.length == 0) && method.getMember().isProperty() && !propertyAccessor)
                    {
                        // Getter with no annotations, and not using property access so ignore
                        continue;
                    }

                    // Process member annotations. We allow each annotation reader to create members if default persistent and no annotations
                    AbstractMemberMetaData mmd = processMemberAnnotations(cmd, method.getMember(), annotations);

                    if (annotations != null && annotations.length > 0)
                    {
                        // Process using any user-provided member annotation handlers
                        for (AnnotationObject annotation : annotations)
                        {
                            String annName = annotation.getName();
                            MemberAnnotationHandler handler = annMgr.getHandlerForMemberAnnotation(annName);
                            if (handler != null)
                            {
                                if (mmd == null)
                                {
                                    mmd = new PropertyMetaData(cmd, method.getMember().getName());
                                    cmd.addMember(mmd);
                                }
                                handler.processMemberAnnotation(annotation, mmd, clr);
                            }
                        }
                    }
                }

                // Process the fields
                for (AnnotatedMember field : annotatedFields)
                {
                    AnnotationObject[] annotations = field.getAnnotations();

                    if ((annotations == null || annotations.length == 0) && !field.getMember().isProperty() && propertyAccessor)
                    {
                        // Field with no annotations, and using property access so ignore
                        continue;
                    }

                    // Process fields. We allow each annotation reader to create members if default persistent and no annotations
                    AbstractMemberMetaData mmd = processMemberAnnotations(cmd, field.getMember(), annotations);

                    if (annotations != null && annotations.length > 0)
                    {
                        // Process using any user-provided member annotation handlers
                        for (AnnotationObject annotation : annotations)
                        {
                            String annName = annotation.getName();
                            MemberAnnotationHandler handler = annMgr.getHandlerForMemberAnnotation(annName);
                            if (handler != null)
                            {
                                if (mmd == null)
                                {
                                    mmd = new FieldMetaData(cmd, field.getMember().getName());
                                    cmd.addMember(mmd);
                                }
                                handler.processMemberAnnotation(annotation, mmd, clr);
                            }
                        }
                    }
                }

                // Process other methods (for listeners etc)
                Method[] methods = cls.getDeclaredMethods();
                for (Method method : methods)
                {
                    processMethodAnnotations(cmd, method);
                }
            }
        }

        return cmd;
    }

    /**
     * Method to process the "class" level annotations and create the outline ClassMetaData object.
     * @param pmd Parent PackageMetaData
     * @param cls The class
     * @param annotations Annotations for the class
     * @param clr ClassLoader resolver
     * @return The ClassMetaData (or null if no annotations)
     */
    protected abstract AbstractClassMetaData processClassAnnotations(PackageMetaData pmd, Class cls, AnnotationObject[] annotations, ClassLoaderResolver clr);

    /**
     * Method to take the passed in outline ClassMetaData and process the annotations for fields/properties adding any necessary FieldMetaData/PropertyMetaData to the ClassMetaData.
     * @param cmd The ClassMetaData (to be updated)
     * @param member The field/property being processed
     * @param annotations The annotations for this field/property
     * @return The FieldMetaData/PropertyMetaData that was added (if any)
     */
    protected abstract AbstractMemberMetaData processMemberAnnotations(AbstractClassMetaData cmd, Member member, AnnotationObject[] annotations);

    /**
     * Method to take the passed in outline ClassMetaData and process the annotations for method adding any necessary MetaData to the ClassMetaData.
     * Called for all methods and is intended for processing of any methods other than persistence specifications 
     * (for example listener methods). Methods for persistence specification are processed via "processMemberAnnotations".
     * @param cmd The ClassMetaData (to be updated)
     * @param method The method
     */
    protected abstract void processMethodAnnotations(AbstractClassMetaData cmd, Method method);

    /**
     * Method returning the annotations for the class.
     * @param cls The class
     * @return Class annotations
     */
    protected AnnotationObject[] getClassAnnotationsForClass(Class cls)
    {
        Annotation[] annotations = cls.getAnnotations();
        Set<String> annotNames = new HashSet<>();
        List<Annotation> supportedAnnots = new ArrayList<>();
        if (annotations != null && annotations.length > 0)
        {
            // Strip out unsupported annotations
            AnnotationManager annMgr = mmgr.getAnnotationManager();
            for (Annotation annotation : annotations)
            {
                String annName = annotation.annotationType().getName();
                if (isSupportedAnnotation(annName) || annMgr.getClassAnnotationHasHandler(annName))
                {
                    if (annotNames.contains(annName))
                    {
                        NucleusLogger.METADATA.warn("Annotation " + annName + " on class=" + cls.getName() + " is DUPLICATED : " + annotation + " - only the first will be used!");
                    }
                    supportedAnnots.add(annotation);
                    annotNames.add(annName);
                }
                else
                {
                    Annotation[] subAnnotations = annotation.annotationType().getAnnotations();
                    for (Annotation subAnnotation : subAnnotations)
                    {
                        annName = subAnnotation.annotationType().getName();
                        if (isSupportedAnnotation(annName) || annMgr.getClassAnnotationHasHandler(annName))
                        {
                            if (annotNames.contains(annName))
                            {
                                NucleusLogger.METADATA.warn("Annotation " + annName + " on class=" + cls.getName() + " is DUPLICATED : for sub of " + annotation + " - only the first will be used!");
                            }
                            supportedAnnots.add(subAnnotation);
                            annotNames.add(annName);
                        }
                    }
                }
            }
        }

        return getAnnotationObjectsForAnnotations(cls.getName(), supportedAnnots.toArray(new Annotation[supportedAnnots.size()]));
    }

    /**
     * Method returning a Map containing an array of the annotations for each Java Bean accessor method of the passed class, keyed by the method name.
     * @param cls The class
     * @return Collection of the annotated methods (with supported annotations)
     */
    protected Collection<AnnotatedMember> getJavaBeanAccessorAnnotationsForClass(Class cls)
    {
        Collection<AnnotatedMember> annotatedMethods = new HashSet<>();

        Method[] methods = cls.getDeclaredMethods();
        int numberOfMethods = methods.length;

        for (int i = 0; i < numberOfMethods; i++)
        {
            // Restrict to Java Bean accessor methods that are not bridge methods
            Set<String> annotNames = new HashSet<>();
            String methodName = methods[i].getName();
            if (!methods[i].isBridge() && ((methodName.startsWith("get") && methodName.length() > 3) || (methodName.startsWith("is") && methodName.length() > 2)))
            {
                // For each Method get the annotations and convert into AnnotationObjects
                Annotation[] annotations = methods[i].getAnnotations();
                if (annotations != null && annotations.length > 0)
                {
                    // Strip out unsupported annotations
                    List<Annotation> supportedAnnots = new ArrayList<>();
                    AnnotationManager annMgr = mmgr.getAnnotationManager();
                    for (Annotation annotation : annotations)
                    {
                        String annName = annotation.annotationType().getName();
                        if (isSupportedAnnotation(annName) || annMgr.getMemberAnnotationHasHandler(annName))
                        {
                            if (annotNames.contains(annName))
                            {
                                NucleusLogger.METADATA.warn("Annotation " + annName + " on class=" + cls.getName() + " is DUPLICATED : " + annotation + " - only the first will be used!");
                            }
                            supportedAnnots.add(annotation);
                            annotNames.add(annName);
                        }
                        else
                        {
                            Annotation[] subAnnotations = annotation.annotationType().getAnnotations();
                            for (Annotation subAnnotation : subAnnotations)
                            {
                                annName = subAnnotation.annotationType().getName();
                                if (isSupportedAnnotation(annName) || annMgr.getMemberAnnotationHasHandler(annName))
                                {
                                    if (annotNames.contains(annName))
                                    {
                                        NucleusLogger.METADATA.warn("Annotation " + annName + " on class=" + cls.getName() + " is DUPLICATED : for sub of " + annotation + " - only the first will be used!");
                                    }
                                    supportedAnnots.add(subAnnotation);
                                    annotNames.add(annName);
                                }
                            }
                        }
                    }

                    if (!supportedAnnots.isEmpty())
                    {
                        AnnotationObject[] annotObjects = getAnnotationObjectsForAnnotations(cls.getName(), supportedAnnots.toArray(new Annotation[supportedAnnots.size()]));
                        annotatedMethods.add(new AnnotatedMember(new Member(methods[i]), annotObjects));
                    }
                }
            }
        }

        return annotatedMethods;
    }

    /**
     * Method returning a Collection of the annotated fields for the specified class.
     * @param cls The class
     * @return Collection of the annotated fields (with supported annotations)
     */
    protected Collection<AnnotatedMember> getFieldAnnotationsForClass(Class cls)
    {
        Collection<AnnotatedMember> annotatedFields = new HashSet<>();

        Field[] fields = cls.getDeclaredFields();
        int numberOfFields = fields.length;

        for (int i = 0; i < numberOfFields; i++)
        {
            // For each Field get the annotations and convert into AnnotationObjects
            Set<String> annotNames = new HashSet<>();
            Annotation[] annotations = fields[i].getAnnotations();
            List<Annotation> supportedAnnots = new ArrayList<>();
            if (annotations != null && annotations.length > 0)
            {
                // Strip out unsupported annotations
                AnnotationManager annMgr = mmgr.getAnnotationManager();
                for (Annotation annotation : annotations)
                {
                    String annName = annotation.annotationType().getName();
                    if (isSupportedAnnotation(annName) || annMgr.getMemberAnnotationHasHandler(annName))
                    {
                        if (annotNames.contains(annName))
                        {
                            NucleusLogger.METADATA.warn("Annotation " + annName + " on class=" + cls.getName() + " is DUPLICATED : " + annotation + " - only the first will be used!");
                        }
                        supportedAnnots.add(annotation);
                        annotNames.add(annName);
                    }
                    else
                    {
                        Annotation[] subAnnotations = annotation.annotationType().getAnnotations();
                        for (Annotation subAnnotation : subAnnotations)
                        {
                            annName = subAnnotation.annotationType().getName();
                            if (isSupportedAnnotation(annName) || annMgr.getMemberAnnotationHasHandler(annName))
                            {
                                if (annotNames.contains(annName))
                                {
                                    NucleusLogger.METADATA.warn("Annotation " + annName + " on class=" + cls.getName() + " is DUPLICATED : for sub of " + annotation + " - only the first will be used!");
                                }
                                supportedAnnots.add(subAnnotation);
                                annotNames.add(annName);
                            }
                        }
                    }
                }
            }

            if (!supportedAnnots.isEmpty())
            {
                AnnotationObject[] objects = getAnnotationObjectsForAnnotations(cls.getName(), supportedAnnots.toArray(new Annotation[supportedAnnots.size()]));
                annotatedFields.add(new AnnotatedMember(new Member(fields[i]), objects));
            }
        }

        return annotatedFields;
    }

    /**
     * Convenience method to convert an array of Annotation objects into an array of AnnotationObjects.
     * @param clsName Name of the class
     * @param annotations The annotations
     * @return The annotation objects
     */
    protected AnnotationObject[] getAnnotationObjectsForAnnotations(String clsName, Annotation[] annotations)
    {
        if (annotations == null)
        {
            return null;
        }

        // Convert each Annotation into its AnnotationObject
        AnnotationObject[] objects = new AnnotationObject[annotations.length];
        int numberOfAnns = annotations.length;
        for (int i=0;i<numberOfAnns;i++)
        {
            Map<String, Object> map = new HashMap<>();
            Method[] annMethods = annotations[i].annotationType().getDeclaredMethods();
            int numberOfAnnotateMethods = annMethods.length;
            for (int j = 0; j < numberOfAnnotateMethods; j++)
            {
                try
                {
                    map.put(annMethods[j].getName(), annMethods[j].invoke(annotations[i], new Object[0]));
                }
                catch (Exception ex)
                {
                    // Error in annotation specification so log a warning
                    NucleusLogger.METADATA.warn(Localiser.msg("044201", clsName, annotations[i].annotationType().getName(), annMethods[j].getName()));
                }
            }

            objects[i] = new AnnotationObject(annotations[i].annotationType().getName(), map);
        }
        return objects;
    }
}