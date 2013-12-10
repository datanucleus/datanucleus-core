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
package org.datanucleus.query;

import java.util.List;

import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;

/**
 * JPQL query helper class providing key information about the language etc.
 */
public class JPQLQueryHelper
{
    /** Keywords used in single-string JPQL. Uppercase variants specified here, but JPQL allows case-insensitive. */
    static final String[] SINGLE_STRING_KEYWORDS = {
        "SELECT", "UPDATE", "DELETE", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY"
        };

    /** List of identifier names not allowed by JPQL. */
    static final String[] RESERVED_IDENTIFIERS = {
        "SELECT", "FROM", "WHERE", "UPDATE", "DELETE", "JOIN", "OUTER", "INNER", "LEFT", "GROUP", "BY", 
        "HAVING", "FETCH", "DISTINCT", "OBJECT", "NULL", "TRUE", "FALSE", "NOT", "AND", "OR", "BETWEEN", 
        "LIKE", "IN", "AS", "UNKNOWN", "EMPTY", "MEMBER", "OF", "IS", "AVG", "MAX", "MIN", "SUM", "COUNT", 
        "ORDER", "ASC", "DESC", "MOD", "UPPER", "LOWER", "TRIM", "POSITION", "CHARACTER_LENGTH", 
        "CHAR_LENGTH", "BIT_LENGTH", "CURRENT_TIME", "CURRENT_DATE", "CURRENT_TIMESTAMP", "NEW", "EXISTS", 
        "ALL", "ANY", "SOME"
    };

    /**
     * Convenience method returning if the supplied name is a keyword for this query language.
     * @param name Name to check
     * @return Whether it is a keyword
     */
    public static boolean isKeyword(String name)
    {
        for (int i=0;i<SINGLE_STRING_KEYWORDS.length;i++)
        {
            // JPQL is case-insensitive
            if (name.equalsIgnoreCase(SINGLE_STRING_KEYWORDS[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method returning if the supplied name is a reserved identifier for this query language.
     * @param name Name to check
     * @return Whether it is a reserved identifier
     */
    public static boolean isReservedIdentifier(String name)
    {
        for (int i=0;i<RESERVED_IDENTIFIERS.length;i++)
        {
            // JPQL is case-insensitive
            if (name.equalsIgnoreCase(RESERVED_IDENTIFIERS[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Convenience method to return the JPQL single-string query text for the provided expression.
     * @param expr The expression
     * @return The JPQL single-string text that equates to this expression
     */
    public static String getJPQLForExpression(Expression expr)
    {
        if (expr instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression)expr;
            Expression left = dyExpr.getLeft();
            Expression right = dyExpr.getRight();
            StringBuffer str = new StringBuffer("(");
            if (left != null)
            {
                str.append(JPQLQueryHelper.getJPQLForExpression(left));
            }

            // Special cases
            if (right != null && right instanceof Literal && ((Literal)right).getLiteral() == null &&
                (dyExpr.getOperator() == Expression.OP_EQ || dyExpr.getOperator() == Expression.OP_NOTEQ))
            {
                str.append(dyExpr.getOperator() == Expression.OP_EQ ? " IS NULL" : " IS NOT NULL");
            }
            else
            {
                if (dyExpr.getOperator() == Expression.OP_AND)
                {
                    str.append(" AND ");
                }
                else if (dyExpr.getOperator() == Expression.OP_OR)
                {
                    str.append(" OR ");
                }
                else if (dyExpr.getOperator() == Expression.OP_ADD)
                {
                    str.append(" + ");
                }
                else if (dyExpr.getOperator() == Expression.OP_SUB)
                {
                    str.append(" - ");
                }
                else if (dyExpr.getOperator() == Expression.OP_MUL)
                {
                    str.append(" * ");
                }
                else if (dyExpr.getOperator() == Expression.OP_DIV)
                {
                    str.append(" / ");
                }
                else if (dyExpr.getOperator() == Expression.OP_EQ)
                {
                    str.append(" = ");
                }
                else if (dyExpr.getOperator() == Expression.OP_GT)
                {
                    str.append(" > ");
                }
                else if (dyExpr.getOperator() == Expression.OP_LT)
                {
                    str.append(" < ");
                }
                else if (dyExpr.getOperator() == Expression.OP_GTEQ)
                {
                    str.append(" >= ");
                }
                else if (dyExpr.getOperator() == Expression.OP_LTEQ)
                {
                    str.append(" <= ");
                }
                else if (dyExpr.getOperator() == Expression.OP_NOTEQ)
                {
                    str.append(" <> ");
                }
                else
                {
                    // TODO Support other operators
                    throw new UnsupportedOperationException("Dont currently support operator " + dyExpr.getOperator() + " in JPQL conversion");
                }

                if (right != null)
                {
                    str.append(JPQLQueryHelper.getJPQLForExpression(right));
                }
            }
            str.append(")");
            return str.toString();
        }
        else if (expr instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)expr;
            return primExpr.getId();
        }
        else if (expr instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)expr;
            if (paramExpr.getId() != null)
            {
                return ":" + paramExpr.getId();
            }
            else
            {
                return "?" + paramExpr.getPosition();
            }
        }
        else if (expr instanceof InvokeExpression)
        {
            InvokeExpression invExpr = (InvokeExpression)expr;
            Expression invoked = invExpr.getLeft();
            List<Expression> args = invExpr.getArguments();
            String method = invExpr.getOperation();
            if (method.equalsIgnoreCase("CURRENT_DATE"))
            {
                return "CURRENT_DATE";
            }
            else if (method.equalsIgnoreCase("CURRENT_TIME"))
            {
                return "CURRENT_TIME";
            }
            else if (method.equalsIgnoreCase("CURRENT_TIMESTAMP"))
            {
                return "CURRENT_TIMESTAMP";
            }
            else if (method.equalsIgnoreCase("length"))
            {
                StringBuffer str = new StringBuffer("LENGTH(");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
                {
                    Expression firstExpr = args.get(0);
                    str.append(",").append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                    if (args.size() == 2)
                    {
                        Expression secondExpr = args.get(1);
                        str.append(",").append(JPQLQueryHelper.getJPQLForExpression(secondExpr));
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equals("toLowerCase"))
            {
                return "LOWER(" + JPQLQueryHelper.getJPQLForExpression(invoked) + ")";
            }
            else if (method.equals("toUpperCase"))
            {
                return "UPPER(" + JPQLQueryHelper.getJPQLForExpression(invoked) + ")";
            }
            else if (method.equalsIgnoreCase("isEmpty"))
            {
                StringBuffer str = new StringBuffer();
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(" IS EMPTY");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("indexOf"))
            {
                StringBuffer str = new StringBuffer("LOCATE(");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                Expression firstExpr = args.get(0);
                str.append(",").append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                if (args.size() > 1)
                {
                    Expression secondExpr = args.get(1);
                    str.append(",").append(JPQLQueryHelper.getJPQLForExpression(secondExpr));
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("substring"))
            {
                StringBuffer str = new StringBuffer("SUBSTRING(");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                Expression firstExpr = args.get(0);
                str.append(",").append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                if (args.size() > 1)
                {
                    Expression secondExpr = args.get(1);
                    str.append(",").append(JPQLQueryHelper.getJPQLForExpression(secondExpr));
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trim"))
            {
                StringBuffer str = new StringBuffer("TRIM(BOTH ");

                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args.size() > 0)
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLQueryHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trimLeft"))
            {
                StringBuffer str = new StringBuffer("TRIM(LEADING ");

                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args.size() > 0)
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLQueryHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trimLeft"))
            {
                StringBuffer str = new StringBuffer("TRIM(TRAILING ");

                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args.size() > 0)
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLQueryHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("matches"))
            {
                StringBuffer str = new StringBuffer();
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(" LIKE ");
                Expression firstExpr = args.get(0);
                str.append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                if (args.size() > 1)
                {
                    Expression secondExpr = args.get(1);
                    str.append(" ESCAPE ").append(JPQLQueryHelper.getJPQLForExpression(secondExpr));
                }
                return str.toString();
            }
            else if (method.equalsIgnoreCase("contains"))
            {
                StringBuffer str = new StringBuffer();
                Expression firstExpr = args.get(0);
                str.append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                str.append(" MEMBER OF ");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                return str.toString();
            }
            else if (method.equalsIgnoreCase("COUNT"))
            {
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "COUNT(DISTINCT " + JPQLQueryHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                else
                {
                    return "COUNT(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
                }
            }
            else if (method.equalsIgnoreCase("COALESCE"))
            {
                StringBuffer str = new StringBuffer("COALESCE(");
                for (int i=0;i<args.size();i++)
                {
                    Expression argExpr = args.get(i);
                    str.append(JPQLQueryHelper.getJPQLForExpression(argExpr));
                    if (i < args.size()-1)
                    {
                        str.append(",");
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("NULLIF"))
            {
                StringBuffer str = new StringBuffer("NULLIF(");
                for (int i=0;i<args.size();i++)
                {
                    Expression argExpr = args.get(i);
                    str.append(JPQLQueryHelper.getJPQLForExpression(argExpr));
                    if (i < args.size()-1)
                    {
                        str.append(",");
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("ABS"))
            {
                Expression argExpr = args.get(0);
                return "ABS(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("AVG"))
            {
                Expression argExpr = args.get(0);
                return "AVG(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MAX"))
            {
                Expression argExpr = args.get(0);
                return "MAX(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MIN"))
            {
                Expression argExpr = args.get(0);
                return "MIN(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQRT"))
            {
                Expression argExpr = args.get(0);
                return "SQRT(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SUM"))
            {
                Expression argExpr = args.get(0);
                return "SUM(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            // TODO Support this
            throw new UnsupportedOperationException("Dont currently support InvokeExpression (" + invExpr + ") conversion into JPQL");
        }
        else if (expr instanceof Literal)
        {
            Literal litExpr = (Literal)expr;
            Object value = litExpr.getLiteral();
            if (value instanceof String || value instanceof Character)
            {
                return "'" + value.toString() + "'";
            }
            else if (value instanceof Boolean)
            {
                return ((Boolean)value ? "TRUE" : "FALSE");
            }
            else
            {
                return litExpr.getLiteral().toString();
            }
        }
        else if (expr instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)expr;
            return varExpr.getId();
        }
        else
        {
            throw new UnsupportedOperationException("Dont currently support " + expr.getClass().getName() + " in JPQLQueryHelper");
        }
    }
}