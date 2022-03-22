/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.query.inmemory.method;

import java.math.BigDecimal;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.QueryUtils;
import org.datanucleus.store.query.expression.DyadicExpression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.ParameterExpression;
import org.datanucleus.store.query.expression.PrimaryExpression;
import org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.store.query.inmemory.InvocationEvaluator;

/**
 * Evaluator for mathematical function XYZ(numExpr).
 */
public abstract class MathFunction implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();

        Object paramValue = getParamValueForParam(expr.getArguments().get(0), eval, method);

        Object result = null;
        if (expr.getArguments().size() == 1)
        {
            // Math function taking in 1 argument
            if (paramValue instanceof Double)
            {
                result = Double.valueOf(evaluateMathFunction(((Double)paramValue).doubleValue()));
            }
            else if (paramValue instanceof Float)
            {
                result = Float.valueOf((float)evaluateMathFunction(((Float)paramValue).floatValue()));
            }
            else if (paramValue instanceof BigDecimal)
            {
                result = new BigDecimal(evaluateMathFunction(((BigDecimal)paramValue).doubleValue()));
            }
            else if (paramValue instanceof Integer)
            {
                result = Double.valueOf(evaluateMathFunction(((Integer)paramValue).doubleValue()));
            }
            else if (paramValue instanceof Long)
            {
                result = Double.valueOf(evaluateMathFunction(((Long)paramValue).doubleValue()));
            }
            else
            {
                throw new NucleusException("Not possible to use " + getFunctionName() + " on value of type " + paramValue.getClass().getName());
            }
        }
        else if (expr.getArguments().size() == 2)
        {
            // Math function taking in 2 arguments
            Object paramValue2 = (expr.getArguments().size() == 2) ? getParamValueForParam(expr.getArguments().get(1), eval, method) : null;

            if (paramValue instanceof Double)
            {
                result = Double.valueOf(evaluateMathFunction(((Double)paramValue).doubleValue(), ((Double)paramValue2).doubleValue()));
            }
            else if (paramValue instanceof Float)
            {
                result = Float.valueOf((float)evaluateMathFunction(((Float)paramValue).floatValue(), ((Float)paramValue2).floatValue()));
            }
            else if (paramValue instanceof BigDecimal)
            {
                result = new BigDecimal(evaluateMathFunction(((BigDecimal)paramValue).doubleValue(), ((BigDecimal)paramValue2).doubleValue()));
            }
            else if (paramValue instanceof Integer)
            {
                result = Double.valueOf(evaluateMathFunction(((Integer)paramValue).doubleValue(), ((Integer)paramValue2).doubleValue()));
            }
            else if (paramValue instanceof Long)
            {
                result = Double.valueOf(evaluateMathFunction(((Long)paramValue).doubleValue(), ((Long)paramValue2).doubleValue()));
            }
            else
            {
                throw new NucleusException("Not possible to use " + getFunctionName() + " on value of type " + paramValue.getClass().getName());
            }
        }
        return result;
    }

    protected abstract String getFunctionName();

    protected abstract double evaluateMathFunction(double num);
    
    protected double evaluateMathFunction(double num1, double num2)
    {
        throw new NucleusUserException("evaluate method with multiple arguments not implemented");
    }

    protected Object getParamValueForParam(Object param, InMemoryExpressionEvaluator eval, String method)
    {
        Object paramValue = null;
        if (param instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            paramValue = eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param;
            paramValue = QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (param instanceof InvokeExpression)
        {
            InvokeExpression invokeExpr = (InvokeExpression)param;
            paramValue = eval.getValueForInvokeExpression(invokeExpr);
        }
        else if (param instanceof Literal)
        {
            paramValue = ((Literal)param).getLiteral();
        }
        else if (param instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression)param;
            paramValue = dyExpr.evaluate(eval);
        }
        else
        {
            throw new NucleusException(method + " parameter which is instanceof " + param.getClass().getName() + " not supported");
        }

        return paramValue;
    }
}