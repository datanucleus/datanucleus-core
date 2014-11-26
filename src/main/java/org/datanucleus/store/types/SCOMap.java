/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved.
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
2005 Andy Jefferson - added embedded Map update methods
    ...
**********************************************************************/
package org.datanucleus.store.types;

/**
 * Representation of a wrapper for a mutable Map SCO type supported.
 **/
public interface SCOMap<T> extends SCOContainer<T>
{
    /**
     * Method to update an embedded key stored in the map.
     * @param key The key
     * @param fieldNumber Number of field in the element
     * @param newValue the new value for this field
     * @param makeDirty Whether to make the SCO field dirty.
     */
    public void updateEmbeddedKey(Object key, int fieldNumber, Object newValue, boolean makeDirty);

    /**
     * Method to update an embedded value stored in the map.
     * @param value The value
     * @param fieldNumber Number of field in the element
     * @param newValue the new value for this field
     * @param makeDirty Whether to make the SCO field dirty.
     */
    public void updateEmbeddedValue(Object value, int fieldNumber, Object newValue, boolean makeDirty);
}