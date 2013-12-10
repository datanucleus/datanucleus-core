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

/**
 * Generated value. The value can be of any kind : String, Long, Date, Application ID, etc.
 */
class ValueGeneration implements Serializable
{
    /** the value. */
    private Object value;
 
    /**
     * Constructor for the value.
     * @param val The value
     */
    ValueGeneration(Object val)
    {
        this.value = val;
    }

    /**
     * Returns the value.
     * @return The Value
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * Sets the value.
     * @param val The value to set
     */
    public void setValue(Object val)
    {
        this.value = val;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof ValueGeneration))
        {
            return false;
        }
        return ((ValueGeneration)obj).getValue().equals(this.getValue());
    }

    public int hashCode()
    {
        return value.hashCode();
    }
}