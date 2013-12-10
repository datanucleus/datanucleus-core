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
package org.datanucleus.query.evaluator.memory;

/**
 * Expression representing an Integer, used in evaluation of aggregates.
 */
public class IntegerAggregateExpression extends NumericAggregateExpression
{
    public IntegerAggregateExpression(Integer value)
    {
        super(value);
    }

    public Object add(Object obj)
    {
        if (obj instanceof Integer)
        {
            return Integer.valueOf(((Integer) obj).intValue() + ((Integer) value).intValue());
        }
        return super.add(obj);
    }

    public Object sub(Object obj)
    {
        return super.sub(obj);
    }

    public Object div(Object obj)
    {
        if (obj instanceof Integer)
        {
            return Integer.valueOf(((Integer) value).intValue() / ((Integer) obj).intValue());
        }
        return super.add(obj);
    }

    public Boolean gt(Object obj)
    {
        if (obj instanceof Integer)
        {
            if (((Integer) value).intValue() > ((Integer) obj).intValue())
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.gt(obj);
    }

    public Boolean lt(Object obj)
    {
        if (obj instanceof Integer)
        {
            if (((Integer) value).intValue() < ((Integer) obj).intValue())
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.lt(obj);
    }

    public Boolean eq(Object obj)
    {
        if (obj instanceof Integer)
        {
            if (((Integer) value).intValue() == ((Integer) obj).intValue())
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.eq(obj);
    }    
}