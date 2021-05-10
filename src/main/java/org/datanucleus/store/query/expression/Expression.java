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
2008 Andy Jefferson - javadoced. Added some operators.
    ...
**********************************************************************/
package org.datanucleus.store.query.expression;

import java.io.Serializable;

import org.datanucleus.store.query.compiler.Symbol;
import org.datanucleus.store.query.compiler.SymbolTable;

/**
 * A Scalar expression in a Query. Used to compute values with a resulting type.
 */
public abstract class Expression implements Serializable
{
    private static final long serialVersionUID = -847871617806099111L;

    /** Parent of this expression in the tree (if any). */
    protected Expression parent;

    protected Operator op;
    protected Expression left;
    protected Expression right;
    protected Symbol symbol;
    protected String alias;

    /**
     * Representation of an Operator.
     */
    public static class Operator implements Serializable
    {
        private static final long serialVersionUID = -5417485338482984402L;
        protected final String symbol;
        protected final int precedence;

        /**
         * Operator
         * @param symbol the source text or symbol of an operator. e.g  =, ==, +, /, &gt;, &lt;, etc
         * @param precedence the order of precedence where the expression is compiled 
         */
        public Operator(String symbol, int precedence)
        {
            this.symbol = symbol;
            this.precedence = precedence;
         }

        public String toString()
        {
            return symbol;
        }
    }

    /**
     * "Monadic" operator performs a function on one operand.
     * It can be in either postfix or prefix notation. 
     * <ul>
     * <li>Prefix notation meaning the operator appears before its operand. <i>operator operand</i></li>
     * <li>Postfix notation meaning the operator appears after its operand. <i>operand operator</i></li>
     * </ul>
     */
    public static class MonadicOperator extends Operator
    {
        private static final long serialVersionUID = 1663447359955939741L;

        /**
         * Monodiac operator
         * @param symbol the source text or symbol of an operator. e.g  =, ==, +, /, &gt;, &lt;, etc
         * @param precedence the order of precedence where the expression is compiled 
         */
        public MonadicOperator(String symbol, int precedence)
        {
            super(symbol, precedence);
        }

        /**
         * Check if this operator has higher precedence than <code>op</code>
         * @param op the operator
         * @return true if this operation is higher
         */
        public boolean isHigherThan(Operator op)
        {
            if (op == null)
            {
                return false;
            }
            return precedence > op.precedence;
        }
    }

    /**
     * "Dyadic" operator performs operation on one or two operands.
     */
    public static class DyadicOperator extends Operator
    {
        private static final long serialVersionUID = -2975478176127144417L;
        /**
         * An associative operator is one for which parentheses can be inserted and removed without 
         * changing the meaning of the expression 
         */
        private final boolean isAssociative;

        /**
         * Dyadic operator
         * @param symbol the source text or symbol of an operator. e.g  =, ==, +, /, &gt;, &lt;, etc
         * @param precedence the order of precedence where the expression is compiled 
         * @param isAssociative true if associative operator. An associative operator is one for which 
         *     parentheses can be inserted and removed without changing the meaning of the expression 
         */
        public DyadicOperator(String symbol, int precedence, boolean isAssociative)
        {
            super(" " + symbol + " ", precedence);
            this.isAssociative = isAssociative;
        }

        /**
         * Checks if this operation is higher than operator <code>op</code> in left side of the expression
         * @param op the operator in the left side of the expression
         * @return true if this operation is higher than operator <code>op</code> in left side of the expression
         */
        public boolean isHigherThanLeftSide(Operator op)
        {
            if (op == null)
            {
                return false;
            }
            return precedence > op.precedence;
        }
        /**
         * Checks if this operation is higher than operator <code>op</code> in right side of the expression
         * @param op the operator in the right side of the expression
         * @return true if this operation is higher than operator <code>op</code> in right side of the expression
         */
        public boolean isHigherThanRightSide(Operator op)
        {
            if (op == null)
            {
                return false;
            }
            else if (precedence == op.precedence)
            {
                return !isAssociative;
            }
            else
            {
                return precedence > op.precedence;
            }
        }
    }

    /** OR **/
    public static final DyadicOperator OP_OR = new DyadicOperator("OR", 0, true);
    /** AND **/
    public static final DyadicOperator OP_AND = new DyadicOperator("AND", 1, true);
    /** NOT **/
    public static final MonadicOperator OP_NOT = new MonadicOperator("NOT ", 2);
    /** EQ **/
    public static final DyadicOperator OP_EQ = new DyadicOperator("=", 3, false);
    /** NOTEQ **/
    public static final DyadicOperator OP_NOTEQ = new DyadicOperator("<>", 3, false);
    /** LT **/
    public static final DyadicOperator OP_LT = new DyadicOperator("<", 3, false);
    /** LTEQ **/
    public static final DyadicOperator OP_LTEQ = new DyadicOperator("<=", 3, false);
    /** GT **/
    public static final DyadicOperator OP_GT = new DyadicOperator(">", 3, false);
    /** GTEQ **/
    public static final DyadicOperator OP_GTEQ = new DyadicOperator(">=", 3, false);
    /** LIKE **/
    public static final DyadicOperator OP_LIKE = new DyadicOperator("LIKE", 3, false);
    /** IS **/
    public static final DyadicOperator OP_IS = new DyadicOperator("IS", 3, false);
    /** ISNOT **/
    public static final DyadicOperator OP_ISNOT = new DyadicOperator("IS NOT", 3, false);
    /** IS **/
    public static final DyadicOperator OP_CAST = new DyadicOperator("CAST", 3, false);
    /** IN **/
    public static final DyadicOperator OP_IN = new DyadicOperator("IN", 3, false);
    /** NOTIN **/
    public static final DyadicOperator OP_NOTIN = new DyadicOperator("NOT IN", 3, false);
    /** BITWISE OR **/
    public static final DyadicOperator OP_BIT_OR = new DyadicOperator("|", 3, false); // TODO Maybe move to after AND
    /** BITWISE XOR **/
    public static final DyadicOperator OP_BIT_XOR = new DyadicOperator("^", 3, false); // TODO Maybe move to after AND
    /** BITWISE AND **/
    public static final DyadicOperator OP_BIT_AND = new DyadicOperator("&", 3, false); // TODO Maybe move to after AND
    /** ADD **/
    public static final DyadicOperator OP_ADD = new DyadicOperator("+", 4, true);
    /** SUB **/
    public static final DyadicOperator OP_SUB = new DyadicOperator("-", 4, false);
    /** CONCAT **/
    public static final DyadicOperator OP_CONCAT = new DyadicOperator("||", 4, true);
    /** MUL **/
    public static final DyadicOperator OP_MUL = new DyadicOperator("*", 5, true);
    /** DIV **/
    public static final DyadicOperator OP_DIV = new DyadicOperator("/", 5, false);
    /** MOD **/
    public static final DyadicOperator OP_MOD = new DyadicOperator("%", 5, false);
    /** NEG **/
    public static final MonadicOperator OP_NEG = new MonadicOperator("-", 6);
    /** COM **/
    public static final MonadicOperator OP_COM = new MonadicOperator("~", 6);
    /** DISTINCT **/
    public static final MonadicOperator OP_DISTINCT = new MonadicOperator("DISTINCT", 6);

    /**
     * Constructor.
     */
    protected Expression()
    {
    }

    /**
     * Perform a function <code>op</code> on <code>operand</code> 
     * @param op operator
     * @param operand operand
     */
    protected Expression(MonadicOperator op, Expression operand)
    {
        this.op = op;
        this.left = operand;
        if (this.left != null)
        {
            this.left.parent = this;
        }
    }

    /**
     * Performs a function on two arguments.
     * op(operand1,operand2)
     * operand1 op operand2 
     * @param operand1 the first expression
     * @param op the operator between operands
     * @param operand2 the second expression
     */
    protected Expression(Expression operand1, DyadicOperator op, Expression operand2)
    {
        this.op = op;
        this.left = operand1;
        this.right = operand2;
        if (this.left != null)
        {
            this.left.parent = this;
        }
        if (this.right != null)
        {
            this.right.parent = this;
        }
    }

    /**
     * Accessor for the parent expression where this expression is access from.
     * @return Parent expression
     */
    public Expression getParent()
    {
        return parent;
    }

    public void setLeft(Expression expr)
    {
        this.left = expr;
    }

    public void setRight(Expression expr)
    {
        this.right = expr;
    }

    /**
     * The operator to be performed by this expression.
     * See the OP_{name} final static properties of this class.
     * @return Operator
     */
    public Operator getOperator()
    {
        return op;
    }

    /**
     * Accessor for the left hand expression.
     * @return Left expression
     */
    public Expression getLeft()
    {
        return left;
    }

    /**
     * Accessor for the right hand expression.
     * @return Right expression
     */
    public Expression getRight()
    {
        return right;
    }

    /**
     * Accessor for the symbol for this expression (if set).
     * @return The symbol
     */
    public Symbol getSymbol()
    {
        return symbol;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public String getAlias()
    {
        return alias;
    }

    /**
     * Method to evaluate this expression, using the passed evaluator.
     * @param eval Evaluator
     * @return The result
     */
    public Object evaluate(ExpressionEvaluator eval)
    {
        return eval.evaluate(this);
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol table
     * @return The symbol for this expression
     */
    public abstract Symbol bind(SymbolTable symtbl);
}