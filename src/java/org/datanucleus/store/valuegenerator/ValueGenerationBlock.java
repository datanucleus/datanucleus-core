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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.datanucleus.util.StringUtils;

/**
 * Representation of a block of values.
 */
public class ValueGenerationBlock implements Serializable
{
    /** The next id position. */
    private int nextIndex = 0;

    /** The list of values in this block. */
    private final List valueList;

    /**
     * Constructor.
     * @param values The block of objects that will be considered the "values"
     */
    public ValueGenerationBlock(Object[] values)
    {
        valueList = Arrays.asList(values);
    }

    /**
     * Constructor.
     * @param oid The list of objects that will be considered the "values"
     */
    public ValueGenerationBlock(List oid)
    {
        valueList = new ArrayList(oid);
    }

    /**
     * Accessor for the current value.
     * @return The current value
     * @throws NoSuchElementException Thrown if no current value
     */
    public ValueGeneration current()
    {
        if (nextIndex == 0 || (nextIndex-1) >= valueList.size())
        {
            throw new NoSuchElementException();
        }
        return new ValueGeneration(valueList.get(nextIndex-1));
    }

    /**
     * Accessor for the next value, or null if block values exhausted
     * @return The next value
     */
    public ValueGeneration next()
    {
        if (nextIndex >= valueList.size())
        {
            throw new NoSuchElementException();
        }

        return new ValueGeneration(valueList.get(nextIndex++));
    }

    /**
     * Accessor for whether there are more values remaining in the block.
     * @return True when has more values
     */
    public boolean hasNext()
    {
        return (nextIndex < valueList.size());
    }

    /**
     * Method to append a block onto this block.
     * This is used where we have some values left, and we want to allocate more
     * to go into this block.
     * @param block The other block
     */
    public void addBlock(ValueGenerationBlock block)
    {
        if (block == null)
        {
            return;
        }

        while (block.hasNext())
        {
            valueList.add(block.next());
        }
    }

    /**
     * Stringify method.
     * @return A string version of this object
     */
    public String toString()
    {
        return "ValueGenerationBlock : " + StringUtils.collectionToString(valueList);
    }
}