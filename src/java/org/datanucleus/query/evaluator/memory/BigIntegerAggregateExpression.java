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

import java.math.BigInteger;

/**
 * Expression representing a BigInteger, used in evaluation of aggregates.
 */
public class BigIntegerAggregateExpression extends NumericAggregateExpression
{
    public BigIntegerAggregateExpression(BigInteger value)
    {
        super(value);
    }

    public Object add(Object obj)
    {
        if (obj instanceof BigInteger)
        {
            return BigInteger.valueOf(((BigInteger) obj).longValue() + ((BigInteger) value).longValue());
        }
        return super.add(obj);
    }

    public Object sub(Object obj)
    {
        return super.sub(obj);
    }

    public Object div(Object obj)
    {
        if (obj instanceof BigInteger)
        {
            return BigInteger.valueOf(((BigInteger) value).longValue() / ((BigInteger) obj).longValue());
        }
        return super.add(obj);
    }

    public Boolean gt(Object obj)
    {
        if (obj instanceof BigInteger)
        {
            return Boolean.valueOf(((BigInteger) value).longValue() > ((BigInteger) obj).longValue());
        }
        return super.gt(obj);
    }

    public Boolean lt(Object obj)
    {
        if (obj instanceof BigInteger)
        {
            return Boolean.valueOf(((BigInteger) value).longValue() < ((BigInteger) obj).longValue());
        }
        return super.lt(obj);
    }

    public Boolean eq(Object obj)
    {
        if (obj instanceof BigInteger)
        {
            return Boolean.valueOf(((BigInteger) value).longValue() == ((BigInteger) obj).longValue());
        }
        return super.eq(obj);
    }    
}