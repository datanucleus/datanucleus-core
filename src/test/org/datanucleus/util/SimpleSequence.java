/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import javax.jdo.datastore.Sequence;

/**
 * Simple implementation of a sequence that counts from 0 and increments
 * by 1 every time. Allocation is not necessary because it increments
 * a long value internally.
 */
public class SimpleSequence implements Sequence
{
    String name;
    long current = 0;

    public SimpleSequence(String name)
    {
        this.name = name;
    }

    /**
     * 
     * @see javax.jdo.datastore.Sequence#getName()
     */
    public String getName()
    {
        return name;
    }

    /**
     * 
     * @see javax.jdo.datastore.Sequence#next()
     */
    public Object next()
    {
        current++;
        return new Long(current);
    }

    /**
     * 
     * @see javax.jdo.datastore.Sequence#nextValue()
     */
    public long nextValue()
    {
        current++;
        return current;
    }

    /**
     * 
     * @see javax.jdo.datastore.Sequence#allocate(int)
     */
    public void allocate(int arg0)
    {
    }

    /**
     * 
     * @see javax.jdo.datastore.Sequence#current()
     */
    public Object current()
    {
        return new Long(current);
    }

    /**
     * 
     * @see javax.jdo.datastore.Sequence#currentValue()
     */
    public long currentValue()
    {
        return current;
    }
}