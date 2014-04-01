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
package org.datanucleus.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.state.CallbackHandler;
import org.datanucleus.util.StringUtils;

/**
 * Handles the integration of "javax.validation". 
 * Should only be invoked if validation "mode" != none.
 * Implements only the methods preDelete, preStore, prePersist
 */
public class BeanValidatorHandler implements CallbackHandler
{
    Validator validator;
    ClassLoaderResolver clr;
    Configuration conf;

    /**
     * Constructor for a validation handler.
     * @param ec ExecutionContext that we are persisting in
     * @param factory Validation factory
     */
    public BeanValidatorHandler(ExecutionContext ec, ValidatorFactory factory)
    {
        conf = ec.getNucleusContext().getConfiguration();
        clr = ec.getClassLoaderResolver();

        validator = factory.usingContext().traversableResolver(new PersistenceTraversalResolver(ec)).getValidator();
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
                "Validation failed for " + StringUtils.toJVMIDString(pc) +
                " during "+ callbackName +
                " for groups "+StringUtils.objectArrayToString(groups) + " - exceptions are attached",
                (Set<ConstraintViolation<?>>)(Object)(violations)); 
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

    public void close()
    {
    }

    public void setValidationListener(CallbackHandler handler)
    {
    }

    public void addListener(Object listener, Class[] classes)
    {
    }

    public void removeListener(Object listener)
    {
    }

    public void postAttach(Object pc, Object detachedPC)
    {
    }

    public void postClear(Object pc)
    {
    }

    public void postCreate(Object pc)
    {
    }

    public void postDelete(Object pc)
    {
    }

    public void postDetach(Object pc, Object detachedPC)
    {
    }

    public void postDirty(Object pc)
    {
    }

    public void postLoad(Object pc)
    {
    }

    public void postRefresh(Object pc)
    {
    }

    public void postStore(Object pc)
    {
    }

    public void preAttach(Object detachedPC)
    {
    }

    public void preClear(Object pc)
    {
    }

    public void preDetach(Object pc)
    {
    }

    public void preDirty(Object pc)
    {
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
        else
        {
            String[] classNames = property.trim().split(",");
            Class<?>[] groups = new Class<?>[classNames.length];
            for (int i=0; i<classNames.length; i++)
            {
                groups[i] = clr.classForName(classNames[i].trim());
            }
            return groups;
        }
    }
}