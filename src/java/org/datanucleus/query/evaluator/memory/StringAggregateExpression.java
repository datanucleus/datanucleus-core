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
 * Expression representing a String, used in evaluation of aggregates.
 */
public class StringAggregateExpression extends AggregateExpression
{
    String value;
    public StringAggregateExpression(String value)
    {
        this.value = value;
    }
    
    public Boolean gt(Object obj)
    {
        if ( obj instanceof String)
        {
            if (value.compareTo((String)obj)>0)
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.gt(obj);
    }

    public Boolean lt(Object obj)
    {
        if (obj instanceof String)
        {
            if (value.compareTo((String)obj)<0)
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.lt(obj);
    }

    public Boolean eq(Object obj)
    {
        if (obj instanceof String)
        {
            if (value.equals(obj))
            {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return super.eq(obj);
    }
}