/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.query.inmemory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.util.Localiser;

/**
 * Evaluator for the method "YEAR({dateExpr})".
 */
public class TemporalYearMethod implements InvocationEvaluator
{
    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.memory.InvocationEvaluator#evaluate(org.datanucleus.query.expression.InvokeExpression, org.datanucleus.query.evaluator.memory.InMemoryExpressionEvaluator)
     */
    public Object evaluate(InvokeExpression expr, Object invokedValue, InMemoryExpressionEvaluator eval)
    {
        if (invokedValue == null)
        {
            return Boolean.FALSE;
        }
        if (!(invokedValue instanceof Date))
        {
            throw new NucleusException(Localiser.msg("021011", expr.getOperation(), invokedValue.getClass().getName()));
        }

        if (invokedValue instanceof Date)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date)invokedValue);
            return Integer.valueOf(cal.get(Calendar.YEAR));
        }
        else if (invokedValue instanceof Calendar)
        {
            return Integer.valueOf(((Calendar)invokedValue).get(Calendar.YEAR));
        }
        else if (invokedValue instanceof LocalDate)
        {
            return ((LocalDate)invokedValue).getYear();
        }
        else if (invokedValue instanceof LocalDateTime)
        {
            return ((LocalDateTime)invokedValue).getYear();
        }
        else
        {
            throw new NucleusUserException("We do not currently support YEAR() with argument of type " + invokedValue.getClass().getName());
        }
    }
}