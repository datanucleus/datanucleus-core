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
    ...
 **********************************************************************/
package org.datanucleus.validation;

import java.lang.annotation.ElementType;

import javax.validation.Path;
import javax.validation.TraversableResolver;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;

/**
 * Resolver for traversal of validation.
 */
class PersistenceTraversalResolver implements TraversableResolver
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
     * @param traversableObject object hosting <code>traversableProperty</code> or null if <code>validateValue</code> is
     * called
     * @param traversableProperty the traversable property.
     * @param rootBeanType type of the root object passed to the Validator.
     * @param pathToTraversableObject path from the root object to <code>traversableObject</code> (using the path
     * specification defined by Bean Validator).
     * @param elementType either <code>FIELD</code> or <code>METHOD</code>.
     * @return <code>true</code> if the Bean Validation provider is allowed to cascade validation, <code>false</code>
     * otherwise.
     */
    public boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
            Path pathToTraversableObject, ElementType elementType)
    {
        // we do not cascade
        return false;
    }

    /**
     * Determine if the Bean Validation provider is allowed to reach the property state
     * @param traversableObject object hosting <code>traversableProperty</code> or null if <code>validateValue</code> is
     * called
     * @param traversableProperty the traversable property.
     * @param rootBeanType type of the root object passed to the Validator.
     * @param pathToTraversableObject path from the root object to <code>traversableObject</code> (using the path
     * specification defined by Bean Validator).
     * @param elementType either <code>FIELD</code> or <code>METHOD</code>.
     * @return <code>true</code> if the Bean Validation provider is allowed to reach the property state,
     * <code>false</code> otherwise.
     */
    public boolean isReachable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType,
            Path pathToTraversableObject, ElementType elementType)
    {
        AbstractClassMetaData acmd = ec.getMetaDataManager().getMetaDataForClass(traversableObject.getClass(), 
            ec.getClassLoaderResolver());
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