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
package org.datanucleus.query.evaluator;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.query.expression.CaseExpression;
import org.datanucleus.query.expression.CreatorExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.SubqueryExpression;
import org.datanucleus.query.expression.VariableExpression;

/**
 * Abstract evaluator for an expression.
 * Provides a stack-like process, working its way through the expression tree.
 * Provides methods processXXX that should be implemented by the subclass where it supports
 * the particular operator or expression.
 */
public class AbstractExpressionEvaluator implements org.datanucleus.query.expression.ExpressionEvaluator
{
    /**
     * Method to "evaluate" the expression.
     * @param expr The expression
     * @return The evaluated expression
     */
    public Object evaluate(Expression expr)
    {
        return compileOrAndExpression(expr);
    }

    /**
     * This method deals with the OR/AND conditions.
     * A condition specifies a combination of one or more expressions and
     * logical (Boolean) operators and returns a value of TRUE, FALSE, or 
     * unknown
     */
    protected Object compileOrAndExpression(Expression expr)
    {
        if (expr.getOperator() == Expression.OP_OR)
        {
            return processOrExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_AND)
        {
            return processAndExpression(expr);
        }
        return compileRelationalExpression(expr);
    }

    /**
     * Method to handle a relational expression comparing two expressions and returning a BooleanExpression.
     * @param expr The (relational) expression
     * @return The processed expression
     */
    protected Object compileRelationalExpression(Expression expr)
    {
        if (expr.getOperator() == Expression.OP_EQ)
        {
            return processEqExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_NOTEQ)
        {
            return processNoteqExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_LIKE)
        {
            return processLikeExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_GTEQ)
        {
            return processGteqExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_LTEQ)
        {
            return processLteqExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_GT)
        {
            return processGtExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_LT)
        {
            return processLtExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_IS)
        {
            return processIsExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_ISNOT)
        {
            return processIsnotExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_CAST)
        {
            return processCastExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_IN)
        {
            return processInExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_NOTIN)
        {
            return processNotInExpression(expr);
        }
        return compileAdditiveMultiplicativeExpression(expr);
    }

    protected Object compileAdditiveMultiplicativeExpression(Expression expr)
    {
        if (expr.getOperator() == Expression.OP_ADD)
        {
            return processAddExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_SUB)
        {
            return processSubExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_MUL)
        {
            return processMulExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_DIV)
        {
            return processDivExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_MOD)
        {
            return processModExpression(expr);
        }
        return compileUnaryExpression(expr);
    }

    protected Object compileUnaryExpression(Expression expr)
    {
        if (expr.getOperator() == Expression.OP_NEG)
        {
            return processNegExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_COM)
        {
            return processComExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_NOT)
        {
            return processNotExpression(expr);
        }
        else if (expr.getOperator() == Expression.OP_DISTINCT)
        {
            return processDistinctExpression(expr);
        }
        return compilePrimaryExpression(expr);
    }

    protected Object compilePrimaryExpression(Expression expr)
    {
        if (expr instanceof CreatorExpression)
        {
            return processCreatorExpression((CreatorExpression)expr);
        }
        else if (expr instanceof PrimaryExpression)
        {
            return processPrimaryExpression((PrimaryExpression)expr);
        }
        else if (expr instanceof ParameterExpression)
        {
            return processParameterExpression((ParameterExpression)expr);
        }
        else if (expr instanceof VariableExpression)
        {
            return processVariableExpression((VariableExpression)expr);
        }
        else if (expr instanceof SubqueryExpression)
        {
            return processSubqueryExpression((SubqueryExpression)expr);
        }
        else if (expr instanceof CaseExpression)
        {
            return processCaseExpression((CaseExpression)expr);
        }
        else if (expr instanceof InvokeExpression)
        {
            return processInvokeExpression((InvokeExpression)expr);
        }
        else if (expr instanceof Literal)
        {
            return processLiteral((Literal)expr);
        }
        return null;
    }

    // Methods below here should be implemented by subclasses where they require that feature
 
    /**
     * Method to process the supplied OR expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processOrExpression(Expression expr)
    {
        throw new NucleusException("Operation OR is not supported by this mapper");
    }

    /**
     * Method to process the supplied AND expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processAndExpression(Expression expr)
    {
        throw new NucleusException("Operation AND is not supported by this mapper");
    }

    /**
     * Method to process the supplied EQ expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processEqExpression(Expression expr)
    {
        throw new NucleusException("Operation EQ is not supported by this mapper");
    }

    /**
     * Method to process the supplied NOTEQ expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processNoteqExpression(Expression expr)
    {
        throw new NucleusException("Operation NOTEQ is not supported by this mapper");
    }

    /**
     * Method to process the supplied LIKE expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processLikeExpression(Expression expr)
    {
        throw new NucleusException("Operation LIKE is not supported by this mapper");
    }

    /**
     * Method to process the supplied GT expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processGtExpression(Expression expr)
    {
        throw new NucleusException("Operation GT is not supported by this mapper");
    }

    /**
     * Method to process the supplied LT expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processLtExpression(Expression expr)
    {
        throw new NucleusException("Operation LT is not supported by this mapper");
    }

    /**
     * Method to process the supplied GTEQ expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processGteqExpression(Expression expr)
    {
        throw new NucleusException("Operation GTEQ is not supported by this mapper");
    }

    /**
     * Method to process the supplied LTEQ expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processLteqExpression(Expression expr)
    {
        throw new NucleusException("Operation LTEQ is not supported by this mapper");
    }

    /**
     * Method to process the supplied IS (instanceof) expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processIsExpression(Expression expr)
    {
        throw new NucleusException("Operation IS (instanceof) is not supported by this mapper");
    }

    /**
     * Method to process the supplied ISNOT (!instanceof) expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processIsnotExpression(Expression expr)
    {
        throw new NucleusException("Operation ISNOT (!instanceof) is not supported by this mapper");
    }

    /**
     * Method to process the supplied IN expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processInExpression(Expression expr)
    {
        throw new NucleusException("Operation IN is not supported by this mapper");
    }

    /**
     * Method to process the supplied NOT IN expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processNotInExpression(Expression expr)
    {
        throw new NucleusException("Operation NOT IN is not supported by this mapper");
    }

    /**
     * Method to process the supplied ADD expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processAddExpression(Expression expr)
    {
        throw new NucleusException("Operation ADD is not supported by this mapper");
    }

    /**
     * Method to process the supplied SUB expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processSubExpression(Expression expr)
    {
        throw new NucleusException("Operation SUB is not supported by this mapper");
    }

    /**
     * Method to process the supplied MUL expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processMulExpression(Expression expr)
    {
        throw new NucleusException("Operation MUL is not supported by this mapper");
    }

    /**
     * Method to process the supplied DIV expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processDivExpression(Expression expr)
    {
        throw new NucleusException("Operation DIV is not supported by this mapper");
    }

    /**
     * Method to process the supplied MOD expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processModExpression(Expression expr)
    {
        throw new NucleusException("Operation MOD is not supported by this mapper");
    }

    /**
     * Method to process the supplied NEG expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processNegExpression(Expression expr)
    {
        throw new NucleusException("Operation NEG is not supported by this mapper");
    }

    /**
     * Method to process the supplied COM expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processComExpression(Expression expr)
    {
        throw new NucleusException("Operation COM is not supported by this mapper");
    }

    /**
     * Method to process the supplied NOT expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processNotExpression(Expression expr)
    {
        throw new NucleusException("Operation NOT is not supported by this mapper");
    }

    /**
     * Method to process the supplied DISTINCT expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processDistinctExpression(Expression expr)
    {
        throw new NucleusException("Operation DISTINCT is not supported by this mapper");
    }

    /**
     * Method to process the supplied creator expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processCreatorExpression(CreatorExpression expr)
    {
        throw new NucleusException("Creator expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied primary expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processPrimaryExpression(PrimaryExpression expr)
    {
        throw new NucleusException("Primary expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied parameter expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processParameterExpression(ParameterExpression expr)
    {
        throw new NucleusException("Parameter expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied variable expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processVariableExpression(VariableExpression expr)
    {
        throw new NucleusException("Variable expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied subquery expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processSubqueryExpression(SubqueryExpression expr)
    {
        throw new NucleusException("Subquery expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied invoke expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processInvokeExpression(InvokeExpression expr)
    {
        throw new NucleusException("Invoke expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied cast expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processCastExpression(Expression expr)
    {
        throw new NucleusException("Cast expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied case expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processCaseExpression(CaseExpression expr)
    {
        throw new NucleusException("Case expression is not supported by this mapper");
    }

    /**
     * Method to process the supplied invoke expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processLiteral(Literal expr)
    {
        throw new NucleusException("Literals are not supported by this mapper");
    }
}