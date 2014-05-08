/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.store;

/**
 * Sequence of values.
 */
public interface NucleusSequence
{
    /**
     * Method to allocate an amount of values.
     * @param amount The amount
     */
    public void allocate(int amount);

    /**
     * Accessor for the current value object.
     * @return Current value object
     */
    public Object current();

    /**
     * Accessor for the current value.
     * @return Current value
     */
    public long currentValue();

    /**
     * Accessor for the name of the sequence.
     * @return Name of the sequence
     */
    public String getName();

    /**
     * Accessor for the next value object.
     * @return Next value object
     */
    public Object next();

    /**
     * Accessor for the next value.
     * @return next value
     */
    public long nextValue();
}