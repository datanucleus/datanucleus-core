/**********************************************************************
Copyright (c) 2009 Erik Bengtson and others. All rights reserved. 
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
2012 Andy Jefferson - fix default groups
    ...
**********************************************************************/
package org.datanucleus;

import java.lang.annotation.ElementType;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.TraversableResolver;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.StringUtils;

/**
 * Handles the integration of "javax.validation" Bean Validation API (JSR 303).
 * Note that this is the only class referring to BeanValidation classes so that it is usable in environments without BeanValidation present.
 */
public class BeanValidationHandler
{
    Validator validator;

    ClassLoaderResolver clr;

    Configuration conf;

    /**
     * Constructor for a validation handler.
     * @param ec ExecutionContext that we are persisting in
     * @param factory Validation factory
     */
    public BeanValidationHandler(ExecutionContext ec, ValidatorFactory factory)
    {
        conf = ec.getNucleusContext().getConfiguration();
        clr = ec.getClassLoaderResolver();

        validator = factory.usingContext().traversableResolver(new PersistenceTraversalResolver(ec)).getValidator();
    }

    public void close()
    {
    }

    /**
     * Validate the constraints of an object
     * @param pc the object
     * @param callbackName Name of the callback
     * @param groups the validation groups
     */
    public void validate(Object pc, String callbackName, Class<?>[] groups)
    {
        if (validator == null)
        {
            return;
        }

        Set<ConstraintViolation<Object>> violations = validator.validate(pc, groups);
        if (!violations.isEmpty())
        {
            throw new javax.validation.ConstraintViolationException(
                "Validation failed for " + StringUtils.toJVMIDString(pc) + " during " + callbackName +
                " for groups "+StringUtils.objectArrayToString(groups) + " - exceptions are attached", (Set<ConstraintViolation<?>>)(Object)(violations)); 
        }
    }

    public void preDelete(Object pc)
    {
        Class<?>[] groups = getGroups(conf.getStringProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREREMOVE), "pre-remove");
        if (groups != null)
        {
            validate(pc, "pre-remove", groups);
        }
    }

    public void preStore(Object pc)
    {
        Class<?>[] groups = getGroups(conf.getStringProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREUPDATE), "pre-update");
        if (groups != null)
        {
            validate(pc, "pre-update", groups);
        }
    }

    public void prePersist(Object pc)
    {
        Class<?>[] groups = getGroups(conf.getStringProperty(PropertyNames.PROPERTY_VALIDATION_GROUP_PREPERSIST), "pre-persist");
        if (groups != null)
        {
            validate(pc, "pre-persist", groups);
        }
    }

    /**
     * Parse comma separated string of class names and return a corresponding array of classes
     * @param property the string with comma separated class names
     * @return The groups
     */
    private Class<?>[] getGroups(String property, String eventName)
    {
        if (property == null || property.trim().length() == 0)
        {
            // Default to Default for pre-persist/pre-update and nothing otherwise
            if (eventName.equals("pre-persist") || eventName.equals("pre-update"))
            {
                return new Class<?>[] {javax.validation.groups.Default.class};
            }
            return null;
        }

        String[] classNames = property.trim().split(",");
        Class<?>[] groups = new Class<?>[classNames.length];
        for (int i=0; i<classNames.length; i++)
        {
            groups[i] = clr.classForName(classNames[i].trim());
        }
        return groups;
    }

    /**
     * Resolver for traversal of validation.
     */
    static class PersistenceTraversalResolver implements TraversableResolver
    {
        ExecutionContext ec;

        PersistenceTraversalResolver(ExecutionContext ec)
        {
            this.ec = ec;
        }

        /**
         * Determine if the Bean Validation provider is allowed to cascade validation on the bean instance returned by the
         * property value marked as <code>@Valid</code>. Note that this method is called only if <code>isReachable</code>
         * returns true for the same set of arguments and if the property is marked as <code>@Valid</code>
         * @param traversableObject object hosting <code>traversableProperty</code> or null if <code>validateValue</code> is called
         * @param traversableProperty the traversable property.
         * @param rootBeanType type of the root object passed to the Validator.
         * @param pathToTraversableObject path from the root object to <code>traversableObject</code> (using the path specification defined by Bean Validator).
         * @param elementType either <code>FIELD</code> or <code>METHOD</code>.
         * @return <code>true</code> if the Bean Validation provider is allowed to cascade validation, <code>false</code> otherwise.
         */
        public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType, Path pathToTraversableObject, ElementType elementType)
        {
            // we do not cascade
            return false;
        }

        /**
         * Determine if the Bean Validation provider is allowed to reach the property state
         * @param traversableObject object hosting <code>traversableProperty</code> or null if <code>validateValue</code> is called
         * @param traversableProperty the traversable property.
         * @param rootBeanType type of the root object passed to the Validator.
         * @param pathToTraversableObject path from the root object to <code>traversableObject</code> (using the path specification defined by Bean Validator).
         * @param elementType either <code>FIELD</code> or <code>METHOD</code>.
         * @return <code>true</code> if the Bean Validation provider is allowed to reach the property state, <code>false</code> otherwise.
         */
        public boolean isReachable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType, Path pathToTraversableObject, ElementType elementType)
        {
            AbstractClassMetaData acmd = ec.getMetaDataManager().getMetaDataForClass(traversableObject.getClass(), ec.getClassLoaderResolver());
            if (acmd == null)
            {
                return false;
            }

            AbstractMemberMetaData mmd = acmd.getMetaDataForMember(traversableProperty.getName());
            if (mmd.getPersistenceModifier() == FieldPersistenceModifier.NONE)
            {
                // Just pass through for non-persistent fields
                return true;
            }

            // Return whether the field is loaded (and don't cause its loading)
            ObjectProvider op = ec.findObjectProvider(traversableObject);
            return op.isFieldLoaded(mmd.getAbsoluteFieldNumber());
        }
    }
}