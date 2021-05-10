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
2008 Andy Jefferson - cater for input alias
2008 Andy Jefferson - cater for Long, Integer, Short, Double, BigInteger, BigDecimal types
    ...
**********************************************************************/
package org.datanucleus.store.query.inmemory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.ExpressionEvaluator;
import org.datanucleus.util.NucleusLogger;

/**
 * Expression for the aggregation of a set of object values.
 * Provides basic aggregation methods "count", "min", "max", "avg", "sum" and makes use of the
 * AggregateExpression sub-types for Java type handling.
 */
public class SetExpression
{
    String alias = "this";

    Iterator itemIterator;

    /**
     * Constructor for a SetExpression to perform the aggregation.
     * @param items The items to aggregate
     * @param alias The alias for these items
     */
    public SetExpression(Collection items, String alias)
    {
        this.itemIterator = items != null ? items.iterator() : null;
        this.alias = alias;
    }
    
    public Object count(Expression expr, ExpressionEvaluator eval)
    {
        if (itemIterator == null)
        {
            return 0;
        }

        int i=0;
        while (itemIterator.hasNext())
        {
            itemIterator.next();
            i++;
        }
        return Long.valueOf(i);
    }

    public Object min(Expression paramExpr, ExpressionEvaluator eval, Map<String, Object> state)
    {
        if (itemIterator == null)
        {
            if (paramExpr.getSymbol() != null && Number.class.isAssignableFrom(paramExpr.getSymbol().getValueType()))
            {
                return 0;
            }
            return null;
        }

        int i=0;
        Object val = null;
        while (itemIterator.hasNext())
        {
            state.put(alias, itemIterator.next());
            Object result = paramExpr.evaluate(eval);
            AggregateExpression memexpr = null;
            if (i == 0)
            {
                val = result;
            }

            if (result instanceof Float)
            {
                memexpr = new FloatAggregateExpression((Float)result);
            }
            else if (result instanceof Double)
            {
                memexpr = new DoubleAggregateExpression((Double)result);
            }
            else if (result instanceof Long)
            {
                memexpr = new LongAggregateExpression((Long)result);
            }
            else if (result instanceof Integer)
            {
                memexpr = new IntegerAggregateExpression((Integer)result);
            }
            else if (result instanceof Short)
            {
                memexpr = new ShortAggregateExpression((Short)result);
            }
            else if (result instanceof BigInteger)
            {
                memexpr = new BigIntegerAggregateExpression((BigInteger)result);
            }
            else if (result instanceof BigDecimal)
            {
                memexpr = new BigDecimalAggregateExpression((BigDecimal)result);
            }
            else if (result instanceof Date)
            {
                memexpr = new DateAggregateExpression((Date)result);
            }
            else if (result instanceof String)
            {
                memexpr = new StringAggregateExpression((String)result);
            }
            else if (result instanceof InMemoryFailure)
            {
                return result;
            }
            else
            {
                throw new NucleusException("Evaluation of min() on object of type " + 
                    result.getClass().getName() + " - not supported");
            }
            if (Boolean.TRUE.equals(memexpr.lt(val)))
            {
                val = result;
            }
            i++;
        }
        return val;
    }

    public Object max(Expression paramExpr, ExpressionEvaluator eval, Map<String, Object> state)
    {
        if (itemIterator == null)
        {
            if (paramExpr.getSymbol() != null && Number.class.isAssignableFrom(paramExpr.getSymbol().getValueType()))
            {
                return 0;
            }
            return null;
        }

        int i=0;
        Object val = null;
        while (itemIterator.hasNext())
        {
            state.put(alias, itemIterator.next());
            Object result = paramExpr.evaluate(eval);
            AggregateExpression memexpr = null;
            if (i == 0)
            {
                val = result;
            }

            if (result instanceof Float)
            {
                memexpr = new FloatAggregateExpression((Float)result);
            }
            else if (result instanceof Double)
            {
                memexpr = new DoubleAggregateExpression((Double)result);
            }
            else if (result instanceof Long)
            {
                memexpr = new LongAggregateExpression((Long)result);
            }
            else if (result instanceof Integer)
            {
                memexpr = new IntegerAggregateExpression((Integer)result);
            }
            else if (result instanceof Short)
            {
                memexpr = new ShortAggregateExpression((Short)result);
            }
            else if (result instanceof BigInteger)
            {
                memexpr = new BigIntegerAggregateExpression((BigInteger)result);
            }
            else if (result instanceof BigDecimal)
            {
                memexpr = new BigDecimalAggregateExpression((BigDecimal)result);
            }
            else if (result instanceof Date)
            {
                memexpr = new DateAggregateExpression((Date)result);
            }
            else if (result instanceof String)
            {
                memexpr = new StringAggregateExpression((String)result);
            }
            else if (result instanceof InMemoryFailure)
            {
                return result;
            }
            else
            {
                throw new NucleusException("Evaluation of max() on object of type " + 
                    result.getClass().getName() + " - not supported");
            }
            if (Boolean.TRUE.equals(memexpr.gt(val)))
            {
                val = result;
            }
            i++;
        }
        return val;
    }

    public Object sum(Expression paramExpr, ExpressionEvaluator eval, Map<String, Object> state)
    {
        if (itemIterator == null)
        {
            if (paramExpr.getSymbol() != null && Number.class.isAssignableFrom(paramExpr.getSymbol().getValueType()))
            {
                return 0;
            }
            return null;
        }

        Object val = null;
        while (itemIterator.hasNext())
        {
            state.put(alias, itemIterator.next());
            Object result = paramExpr.evaluate(eval);
            AggregateExpression memexpr = null;
            if (result instanceof Float)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((Float)result).doubleValue());
            }
            else if (result instanceof Double)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression((Double)result);
            }
            else if (result instanceof Long)
            {
                if (val == null)
                {
                    val = Long.valueOf(0);
                }
                memexpr = new LongAggregateExpression((Long)result);
            }
            else if (result instanceof Integer)
            {
                if (val == null)
                {
                    val = Long.valueOf(0);
                }
                memexpr = new LongAggregateExpression(((Integer)result).longValue());
            }
            else if (result instanceof Short)
            {
                if (val == null)
                {
                    val = Long.valueOf((short)0);
                }
                memexpr = new LongAggregateExpression(((Short)result).longValue());
            }
            else if (result instanceof BigInteger)
            {
                if (val == null)
                {
                    val = BigInteger.ZERO;
                }
                memexpr = new BigIntegerAggregateExpression((BigInteger)result);
            }
            else if (result instanceof Date)
            {
                if (val == null)
                {
                    val = new Date(0);
                }
                memexpr = new DateAggregateExpression((Date)result);
            }
            else if (result instanceof BigDecimal)
            {
                if (val == null)
                {
                    val = BigDecimal.ZERO;
                }
                memexpr = new BigDecimalAggregateExpression((BigDecimal)result);
            }
            else if (result instanceof InMemoryFailure)
            {
                return result;
            }
            else
            {
                throw new NucleusException("Evaluation of sum() on object of type " + 
                    result.getClass().getName() + " - not supported");
            }
            val = memexpr.add(val);
        }
        return val;
    }

    public Object avg(Expression paramExpr, ExpressionEvaluator eval, Map<String, Object> state)
    {
        if (itemIterator == null || !itemIterator.hasNext())
        {
            if (paramExpr.getSymbol() != null && Number.class.isAssignableFrom(paramExpr.getSymbol().getValueType()))
            {
                return 0.0d;
            }
            return null;
        }

        // This is based around the JDOQL/JPQL definition of "avg" which should return "double"
        int i = 0;
        Object val = null;
        AggregateExpression memexpr = null;
        while (itemIterator.hasNext())
        {
            state.put(alias, itemIterator.next());
            Object result = paramExpr.evaluate(eval);
            if (result instanceof Float)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((Float)result).doubleValue());
            }
            else if (result instanceof Double)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression((Double)result);
            }
            else if (result instanceof Long)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((Long)result).doubleValue());
            }
            else if (result instanceof Integer)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((Integer)result).doubleValue());
            }
            else if (result instanceof Short)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((Short)result).doubleValue());
            }
            else if (result instanceof BigInteger)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((BigInteger)result).doubleValue());
            }
            else if (result instanceof BigDecimal)
            {
                if (val == null)
                {
                    val = Double.valueOf(0);
                }
                memexpr = new DoubleAggregateExpression(((BigDecimal)result).doubleValue());
            }
            else if (result instanceof InMemoryFailure)
            {
                return result;
            }
            else
            {
                throw new NucleusException("Evaluation of avg() on object of type " + result.getClass().getName() + " - not supported");
            }
            val = memexpr.add(val);
            i++;
        }

        Object divisor = null;
        if (val instanceof Float)
        {
            memexpr = new FloatAggregateExpression((Float)val);
            divisor = Float.valueOf(i);
        }
        else if (val instanceof Double)
        {
            memexpr = new DoubleAggregateExpression((Double)val);
            divisor = Double.valueOf(i);
        }
        else if (val instanceof Long)
        {
            memexpr = new LongAggregateExpression((Long)val);
            divisor = Long.valueOf(i);
        }
        else if (val instanceof Integer)
        {
            memexpr = new IntegerAggregateExpression((Integer)val);
            divisor = Integer.valueOf(i);
        }
        else if (val instanceof Short)
        {
            memexpr = new ShortAggregateExpression((Short)val);
            divisor = Short.valueOf((short)i);
        }
        else if (val instanceof BigInteger)
        {
            memexpr = new BigIntegerAggregateExpression((BigInteger)val);
            divisor = BigInteger.valueOf(i);
        }
        else if (val instanceof BigDecimal)
        {
            memexpr = new BigDecimalAggregateExpression((BigDecimal)val);
            divisor = BigDecimal.valueOf(i);
        }

        if (memexpr == null)
        {
            NucleusLogger.QUERY.error("In-memory failure in attempt to get avg of null. Not supported. Perhaps something went wrong earlier in the query evaluation?");
            // TODO Cater for this
            return new InMemoryFailure();
        }
        return memexpr.div(divisor);
    }
}