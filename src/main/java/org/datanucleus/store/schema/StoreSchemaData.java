/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema;

/**
 * Interface representing definition of some schema information in the datastore.
 * Each component can have a set of properties.
 * Typically extended by ListStoreSchemaData, or MapStoreSchemaData if part of a hierarchy of information.
 */
public interface StoreSchemaData
{
    /**
     * Method to define a property for this component.
     * @param name Name of the property
     * @param value Value
     */
    public void addProperty(String name, Object value);

    /**
     * Accessor for a property of this store metadata component.
     * @param name Name of the property
     * @return Property value
     */
    public Object getProperty(String name);
}