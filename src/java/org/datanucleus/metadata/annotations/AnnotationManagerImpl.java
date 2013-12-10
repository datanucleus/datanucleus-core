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
    ...
**********************************************************************/
package org.datanucleus.metadata.annotations;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.PackageMetaData;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager for annotations.
 * Acts as a registry of the available annotation readers and allows use of all
 * types of registered annotations.
 */
public class AnnotationManagerImpl implements AnnotationManager
{
    /** Localiser for messages */
    protected static final Localiser LOCALISER=Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** MetaData Manager that we work for. */
    protected final MetaDataManager metadataMgr;

    /** Lookup of annotation reader name keyed by the annotation class name. */
    Map<String, String> annotationReaderLookup = new HashMap<String, String>();

    /** Cache of the available annotation readers (keyed by the class name). */
    Map<String, AnnotationReader> annotationReaders = new HashMap<String, AnnotationReader>();

    /** Set of (class) annotations that have handlers. */
    Set<String> classAnnotationHandlerAnnotations = null;

    /** Cache of ClassAnnotationHandler keyed by the annotation name that they handle. */
    Map<String, ClassAnnotationHandler> classAnnotationHandlers = null;

    /** Set of (member) annotations that have handlers. */
    Set<String> memberAnnotationHandlerAnnotations = null;

    /** Cache of MemberAnnotationHandler keyed by the annotation name that they handle. */
    Map<String, MemberAnnotationHandler> memberAnnotationHandlers = null;

    /**
     * Constructor.
     * @param metadataMgr Manager for MetaData
     */
    public AnnotationManagerImpl(MetaDataManager metadataMgr)
    {
        this.metadataMgr = metadataMgr;

        PluginManager pluginMgr = metadataMgr.getNucleusContext().getPluginManager();

        // Load up the registry of available annotation readers
        ConfigurationElement[] elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.annotations", null, null);
        if (elems != null)
        {
            for (int i=0;i<elems.length;i++)
            {
                annotationReaderLookup.put(elems[i].getAttribute("annotation-class"), elems[i].getAttribute("reader"));
            }
        }

        // Load up the registry of available class annotation handlers
        elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.class_annotation_handler", null, null);
        if (elems != null && elems.length > 0)
        {
            classAnnotationHandlerAnnotations = new HashSet<String>(elems.length);
            classAnnotationHandlers = new HashMap<String, ClassAnnotationHandler>(elems.length);
            for (int i=0; i<elems.length; i++)
            {
                classAnnotationHandlerAnnotations.add(elems[i].getAttribute("annotation-class"));
            }
        }

        // Load up the registry of available member annotation handlers
        elems = pluginMgr.getConfigurationElementsForExtension("org.datanucleus.member_annotation_handler", null, null);
        if (elems != null && elems.length > 0)
        {
            memberAnnotationHandlerAnnotations = new HashSet<String>(elems.length);
            memberAnnotationHandlers = new HashMap<String, MemberAnnotationHandler>(elems.length);
            for (int i=0; i<elems.length; i++)
            {
                memberAnnotationHandlerAnnotations.add(elems[i].getAttribute("annotation-class"));
            }
        }
    }

    /**
     * Accessor for the MetaData for the specified class, read from annotations.
     * The annotations can be of any supported type.
     * @param cls The class
     * @param pmd PackageMetaData to use as a parent
     * @param clr ClassLoader resolver
     * @return The ClassMetaData
     */
    public AbstractClassMetaData getMetaDataForClass(Class cls, PackageMetaData pmd, ClassLoaderResolver clr)
    {
        if (cls == null)
        {
            return null;
        }

        Annotation[] annotations = cls.getAnnotations();
        if (annotations == null || annotations.length == 0)
        {
            return null;
        }

        // Find an annotation reader for this classes annotations (if we have one)
        String readerClassName = null;
        for (int i=0;i<annotations.length;i++)
        {
            String reader = annotationReaderLookup.get(annotations[i].annotationType().getName());
            if (reader != null)
            {
                readerClassName = reader;
                break;
            }
        }
        if (readerClassName == null)
        {
            NucleusLogger.METADATA.debug(LOCALISER.msg("044202", cls.getName()));
            return null;
        }

        AnnotationReader reader = annotationReaders.get(readerClassName);
        if (reader == null)
        {
            // Try to create this AnnotationReader
            try
            {
                Class[] ctrArgs = new Class[] {ClassConstants.METADATA_MANAGER};
                Object[] ctrParams = new Object[] {metadataMgr};
                PluginManager pluginMgr = metadataMgr.getNucleusContext().getPluginManager();
                reader = (AnnotationReader)pluginMgr.createExecutableExtension("org.datanucleus.annotations",
                    "reader", readerClassName, "reader", ctrArgs, ctrParams);
                annotationReaders.put(readerClassName, reader); // Save the annotation reader in case we have more of this type
            }
            catch (Exception e)
            {
                NucleusLogger.METADATA.warn(LOCALISER.msg("MetaData.AnnotationReaderNotFound", readerClassName));
                return null;
            }
        }

        return reader.getMetaDataForClass(cls, pmd, clr);
    }

    public boolean getClassAnnotationHasHandler(String annotationName)
    {
        if (classAnnotationHandlerAnnotations == null || !classAnnotationHandlerAnnotations.contains(annotationName))
        {
            return false;
        }
        return true;
    }

    public boolean getMemberAnnotationHasHandler(String annotationName)
    {
        if (memberAnnotationHandlerAnnotations == null || !memberAnnotationHandlerAnnotations.contains(annotationName))
        {
            return false;
        }
        return true;
    }

    public ClassAnnotationHandler getHandlerForClassAnnotation(String annotationName)
    {
        if (classAnnotationHandlerAnnotations == null || !classAnnotationHandlerAnnotations.contains(annotationName))
        {
            return null;
        }

        ClassAnnotationHandler handler = classAnnotationHandlers.get(annotationName);
        if (handler == null)
        {
            // Try to create this ClassAnnotationHandler
            try
            {
                PluginManager pluginMgr = metadataMgr.getNucleusContext().getPluginManager();
                handler = (ClassAnnotationHandler)pluginMgr.createExecutableExtension("org.datanucleus.class_annotation_handler",
                    "annotation-class", annotationName, "handler", null, null);
                classAnnotationHandlers.put(annotationName, handler);
            }
            catch (Exception e)
            {
                NucleusLogger.METADATA.warn(LOCALISER.msg("MetaData.ClassAnnotationHandlerNotFound", annotationName));
                return null;
            }
        }

        return handler;
    }

    public MemberAnnotationHandler getHandlerForMemberAnnotation(String annotationName)
    {
        if (memberAnnotationHandlerAnnotations == null || !memberAnnotationHandlerAnnotations.contains(annotationName))
        {
            return null;
        }

        MemberAnnotationHandler handler = memberAnnotationHandlers.get(annotationName);
        if (handler == null)
        {
            // Try to create this MemberAnnotationHandler
            try
            {
                PluginManager pluginMgr = metadataMgr.getNucleusContext().getPluginManager();
                handler = (MemberAnnotationHandler)pluginMgr.createExecutableExtension("org.datanucleus.member_annotation_handler",
                    "annotation-class", annotationName, "handler", null, null);
                memberAnnotationHandlers.put(annotationName, handler);
            }
            catch (Exception e)
            {
                NucleusLogger.METADATA.warn(LOCALISER.msg("MetaData.MemberAnnotationHandlerNotFound", annotationName));
                return null;
            }
        }

        return handler;
    }
}