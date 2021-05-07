/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancement;

/**
 * Reference for an ExecutionContext, for use in the enhancement contract.
 * This is used because if we just used ExecutionContext it would drag in many other classes etc, so reducing exposure.
 * TODO The signature of findObject could return Persistable. Would need to update enhancer if we do that.
 */
public interface ExecutionContextReference
{
    /**
     * Accessor for the owner of the ExecutionContext. This will typically be a PersistenceManager (JDO) or EntityManager (JPA).
     * @return The owner
     */
    Object getOwner();

    /**
     * Accessor for an object given the object id.
     * @param id The id of the object
     * @param validate Whether to validate the id
     * @return The object
     */
    Object findObject(Object id, boolean validate);
}