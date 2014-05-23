/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.converters;

import java.io.Serializable;

/**
 * Converter for a java type to another type suitable for the datastore.
 * 
 * @param X The type of the class member
 * @param Y The type stored in the datastore
 */
public interface TypeConverter<X, Y> extends Serializable
{
    /**
     * Method to convert the passed member value to the datastore type.
     * @param memberValue Value from the member
     * @return Value for the datastore
     */
    Y toDatastoreType(X memberValue);

    /**
     * Method to convert the passed datastore value to the member type.
     * @param datastoreValue Value from the datastore
     * @return Value for the member
     */
    X toMemberType(Y datastoreValue);
}