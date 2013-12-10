/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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
2003 Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.valuegenerator;

/**
 * Generator interface for values.
 * Uses the same methods as in the javax.jdo.datastore.Sequence interface
 */
public interface ValueGenerator
{
    /**
     * Returns the fully qualified name of the <code>Sequence</code>.
     * @return the name of the sequence
     */
    String getName ();

    /**
     * Returns the next sequence value as an Object. If the next
     * sequence value is not available, throw NucleusDataStoreException.
     * @return the next value
     */
    Object next ();

    /**
     * Provides a hint to the implementation that the application
     * will need <code>additional</code> sequence value objects in
     * short order. There is no externally visible behavior of this
     * method. It is used to potentially improve the efficiency of
     * the algorithm of obtaining additional sequence value objects.
     * @param additional the number of additional values to allocate
     */
    void allocate (int additional);

    /**
     * Returns the current sequence value object if it is available. 
     * It is intended to return a sequence value object previously used. 
     * If the current sequence value is not available, throw NucleusDataStoreException.
     * @return the current value
     */
    Object current ();

    /**
     * Returns the next sequence value as a long. 
     * If the next sequence value is not available or is not numeric, throw NucleusDataStoreException.
     * @return the next value
     */
    long nextValue();

    /**
     * Returns the current sequence value as a long. 
     * If the current sequence value is not available or is not numeric, throw NucleusDataStoreException.
     * @return the current value
     */
    long currentValue();
}