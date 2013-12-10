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
2008 Andy Jefferson - support for swapping PrimaryExpression to class Literal
    ...
**********************************************************************/
package org.datanucleus.query.expression;

import java.lang.reflect.Field;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;

/**
 * Expression between two other expressions and an operation.
 * For example, "this.myField < myValue" will become
 * left = PrimaryExpression, right = Literal, op = Expression.OP_LT.
 * A special case is where we have an expression like "!(condition)". In this case we become
 * left = expression, right = null, op = Expression.OP_NOT.
 */
public class DyadicExpression extends Expression
{
    /**
     * Perform a function <code>op</code> on <code>operand</code> 
     * @param op operator
     * @param operand operand
     */
    public DyadicExpression(MonadicOperator op, Expression operand)
    {
        super(op,operand);
    }

    /**
     * Performs a function on two arguments.
     * op(operand1,operand2)
     * operand1 op operand2 
     * @param operand1 the first expression
     * @param op the operator between operands
     * @param operand2 the second expression
     */
    public DyadicExpression(Expression operand1, DyadicOperator op, Expression operand2)
    {
        super(operand1,op,operand2);
    }

    /**
     * Method to evaluate this expression, using the passed evaluator.
     * @param eval Evaluator
     * @return The result
     */
    public Object evaluate(ExpressionEvaluator eval)
    {
        // Evaluate left and/or right first, then this expression
        left.evaluate(eval);
        if (right != null)
        {
            right.evaluate(eval);
        }

        return super.evaluate(eval);
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        if (left != null)
        {
            try
            {
                left.bind(symtbl);
            }
            catch (PrimaryExpressionIsClassLiteralException peil)
            {
                // PrimaryExpression should be swapped for a class Literal
                left = peil.getLiteral();
                left.bind(symtbl);
            }
            catch (PrimaryExpressionIsClassStaticFieldException peil)
            {
                // PrimaryExpression should be swapped for a static field Literal
                Field fld = peil.getLiteralField();
                try
                {
                    // Get the value of the static field
                    Object value = fld.get(null);
                    left = new Literal(value);
                    left.bind(symtbl);
                }
                catch (Exception e)
                {
                    throw new NucleusUserException("Error processing static field " + fld.getName(), e);
                }
            }
            catch (PrimaryExpressionIsVariableException pive)
            {
                // PrimaryExpression should be swapped for a VariableExpression
                left = pive.getVariableExpression();
                left.bind(symtbl);
            }
            catch (PrimaryExpressionIsInvokeException piie)
            {
                // PrimaryExpression should be swapped for a InvokeExpression
                left = piie.getInvokeExpression();
                left.bind(symtbl);
            }
        }

        if (right != null)
        {
            try
            {
                right.bind(symtbl);
            }
            catch (PrimaryExpressionIsClassLiteralException peil)
            {
                // PrimaryExpression should be swapped for a class Literal
                right = peil.getLiteral();
                right.bind(symtbl);
            }
            catch (PrimaryExpressionIsClassStaticFieldException peil)
            {
                // PrimaryExpression should be swapped for a static field Literal
                Field fld = peil.getLiteralField();
                try
                {
                    // Get the value of the static field
                    Object value = fld.get(null);
                    right = new Literal(value);
                    right.bind(symtbl);
                }
                catch (Exception e)
                {
                    throw new NucleusUserException("Error processing static field " + fld.getName(), e);
                }
            }
            catch (PrimaryExpressionIsVariableException pive)
            {
                // PrimaryExpression should be swapped for a VariableExpression
                right = pive.getVariableExpression();
                right.bind(symtbl);
            }
            catch (PrimaryExpressionIsInvokeException piie)
            {
                // PrimaryExpression should be swapped for a InvokeExpression
                right = piie.getInvokeExpression();
                right.bind(symtbl);
            }
        }

        if (left != null && left instanceof VariableExpression)
        {
            Symbol leftSym = left.getSymbol();
            if (leftSym != null && leftSym.getValueType() == null)
            {
                // Set type of implicit variable
                if (right instanceof Literal && ((Literal)right).getLiteral() != null)
                {
                    leftSym.setValueType(((Literal)right).getLiteral().getClass());
                }
            }
        }
        if (right != null)
        {
            Symbol rightSym = right.getSymbol();
            if (rightSym != null && rightSym.getValueType() == null)
            {
                // Set type of implicit variable
                if (left instanceof Literal && ((Literal)left).getLiteral() != null)
                {
                    rightSym.setValueType(((Literal)left).getLiteral().getClass());
                }
            }
        }

        // Interpret types of parameters etc when used in comparison operator
        if (op == Expression.OP_EQ || op == Expression.OP_NOTEQ || op == Expression.OP_GT ||
            op == Expression.OP_GTEQ || op == Expression.OP_LT || op == Expression.OP_LTEQ)
        {
            Class leftType = (left.getSymbol() != null ? left.getSymbol().getValueType() : null);
            Class rightType = (right.getSymbol() != null ? right.getSymbol().getValueType() : null);
            if (left instanceof ParameterExpression && leftType == null && rightType != null)
            {
                // parameter {op} primary
                left.getSymbol().setValueType(rightType);
            }
            else if (right instanceof ParameterExpression && rightType == null && leftType != null)
            {
                // primary {op} parameter
                right.getSymbol().setValueType(leftType);
            }
            leftType = (left.getSymbol() != null ? left.getSymbol().getValueType() : null);
            rightType = (right.getSymbol() != null ? right.getSymbol().getValueType() : null);
        }

        return null;
    }
    
    public String toString()
    {
        return "DyadicExpression{"+ getLeft() + " " + getOperator() + " " + getRight() + "}";
    }
}