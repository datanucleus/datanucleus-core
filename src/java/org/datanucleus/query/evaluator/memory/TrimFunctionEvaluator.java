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

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the function TRIM(strExpr).
 * If the method is TRIM trims both ends. If the method is TRIM_LEADING trims just the start.
 * If the method is TRIM_TRAILING trims just the end. The first parameter of the expression
 * is the string to trim. An optional second parameter is the trim character to trim (default
 * to ' ' if not specified).
 */
public class TrimFunctionEvaluator implements InvocationEvaluator
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        String method = expr.getOperation();
        Object param = expr.getArguments().get(0);
        char trimChar = ' ';
        if (expr.getArguments().size() == 2)
        {
            trimChar = ((Character)((Literal)expr.getArguments().get(1)).getLiteral()).charValue();
        }
        String paramValue = null;
        if (param instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)param;
            paramValue = (String)eval.getValueForPrimaryExpression(primExpr);
        }
        else if (param instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)param;
            paramValue = (String)QueryUtils.getValueForParameterExpression(eval.getParameterValues(), paramExpr);
        }
        else if (param instanceof Literal)
        {
            paramValue = (String)((Literal)param).getLiteral();
        }
        else
        {
            throw new NucleusException(method + "(str1) where str1 is instanceof " + param.getClass().getName() + " not supported");
        }
        if (paramValue == null)
        {
            return null;
        }

        if (method.equals("TRIM"))
        {
            int substringStart = 0;
            for (int i=0;i<paramValue.length();i++)
            {
                if (paramValue.charAt(i) == trimChar)
                {
                    substringStart++;
                }
                else
                {
                    break;
                }
            }
            int substringEnd = paramValue.length();
            for (int i=paramValue.length()-1;i>=0;i--)
            {
                if (paramValue.charAt(i) == trimChar)
                {
                    substringEnd--;
                }
                else
                {
                    break;
                }
            }
            return paramValue.substring(substringStart, substringEnd);
        }
        else if (method.equals("TRIM_LEADING"))
        {
            int substringPos = 0;
            for (int i=0;i<paramValue.length();i++)
            {
                if (paramValue.charAt(i) == trimChar)
                {
                    substringPos++;
                }
                else
                {
                    break;
                }
            }
            return paramValue.substring(substringPos);
        }
        else if (method.equals("TRIM_TRAILING"))
        {
            int substringPos = paramValue.length();
            for (int i=paramValue.length()-1;i>=0;i--)
            {
                if (paramValue.charAt(i) == trimChar)
                {
                    substringPos--;
                }
                else
                {
                    break;
                }
            }
            return paramValue.substring(0, substringPos);
        }
        else
        {
            return null;
        }
    }
}