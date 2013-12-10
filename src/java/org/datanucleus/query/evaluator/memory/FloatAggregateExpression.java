/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved.
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
 * Expression representing a Float, used in evaluation of aggregates.
 */
public class FloatAggregateExpression extends NumericAggregateExpression
{
    public FloatAggregateExpression(Float value)
    {
        super(value);
    }
    
    public Object add(Object obj)
    {
        if (obj instanceof Float)
        {
            return new Float(((Float) obj).floatValue() + ((Float) value).floatValue());
        }
        return super.add(obj);
    }

    public Object sub(Object obj)
    {
        return super.sub(obj);
    }

    public Object div(Object obj)
    {
        if (obj instanceof Float)
        {
            return new Float(((Float) value).floatValue() / ((Float) obj).floatValue());
        }
        return super.add(obj);
    }

    public Boolean gt(Object obj)
    {
        if (obj instanceof Float)
        {
            if (((Float) value).floatValue() > ((Float) obj).floatValue())
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.gt(obj);
    }

    public Boolean lt(Object obj)
    {
        if (obj instanceof Float)
        {
            if (((Float) value).floatValue() < ((Float) obj).floatValue())
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.lt(obj);
    }

    public Boolean eq(Object obj)
    {
        if (obj instanceof Float)
        {
            if (((Float) value).floatValue() == ((Float) obj).floatValue())
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.eq(obj);
    }    
}