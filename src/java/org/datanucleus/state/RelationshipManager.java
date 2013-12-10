/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.state;

public interface RelationshipManager
{
    /**
     * Convenience method to clear all fields from being managed.
     */
    void clearFields();

    /**
     * Method that is called when the user calls setXXX() on a field.
     * @param fieldNumber Number of the field
     * @param oldValue The old value
     * @param newValue The new value
     */
    void relationChange(int fieldNumber, Object oldValue, Object newValue);

    /**
     * Method to register a change in the contents of a container field, with an object being added.
     * @param fieldNumber Number of the field
     * @param val Value being added
     */
    void relationAdd(int fieldNumber, Object val);

    /**
     * Method to register a change in the contents of a container field, with an object being removed.
     * @param fieldNumber Number of the field
     * @param val Value being removed
     */
    void relationRemove(int fieldNumber, Object val);

    /**
     * Accessor for whether a field is being managed.
     * @param fieldNumber Number of the field
     * @return Whether it is currently managed
     */
    boolean managesField(int fieldNumber);

    /**
     * Method to check for consistency the managed relations of this object with the related objects.
     */
    void checkConsistency();

    /**
     * Method to process the (bidirectional) relations for this object.
     */
    void process();
}