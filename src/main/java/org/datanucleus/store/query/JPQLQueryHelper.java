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
package org.datanucleus.store.query;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.expression.CaseExpression;
import org.datanucleus.store.query.expression.DyadicExpression;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.ParameterExpression;
import org.datanucleus.store.query.expression.PrimaryExpression;
import org.datanucleus.store.query.expression.SubqueryExpression;
import org.datanucleus.store.query.expression.VariableExpression;
import org.datanucleus.store.query.expression.CaseExpression.ExpressionPair;

/**
 * JPQL query helper class providing key information about the language etc.
 */
public class JPQLQueryHelper
{
    /** Keywords used in single-string JPQL. Uppercase variants specified here, but JPQL allows case-insensitive. */
    static final String[] SINGLE_STRING_KEYWORDS = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY"
        };

    /** JPQL single-string keywords when allowing RANGE. */
    static final String[] SINGLE_STRING_KEYWORDS_INCLUDING_RANGE = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY", "RANGE"
        };

    // TODO Only allow INSERT when not using strict JPQL

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
     * Convenience method returning if the supplied name is a keyword for this query language (including RANGE).
     * @param name Name to check
     * @param allowRange Whether to allow RANGE syntax
     * @return Whether it is a keyword
     */
    public static boolean isKeyword(String name, boolean allowRange)
    {
        if (!allowRange)
        {
            return isKeyword(name);
        }

        for (int i=0;i<SINGLE_STRING_KEYWORDS_INCLUDING_RANGE.length;i++)
        {
            // JPQL is case-insensitive
            if (name.equalsIgnoreCase(SINGLE_STRING_KEYWORDS_INCLUDING_RANGE[i]))
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
     * Convenience method to extract the FROM candidate entity name from a JPQL query string.
     * @param query The query string
     * @return The candidate entity name
     */
    public static String getEntityNameFromJPQLString(String query)
    {
        if (query == null)
        {
            return null;
        }
        int fromStartPos = query.indexOf(" FROM ");
        if (fromStartPos < 0)
        {
            fromStartPos = query.indexOf(" from ");
        }
        if (fromStartPos < 0)
        {
            return null;
        }

        String fromStr = query.substring(fromStartPos + 6).trim();
        int fromEndPos = fromStr.indexOf(' ');
        String fromCandidate = fromStr;
        if (fromEndPos > 0)
        {
            fromCandidate = fromStr.substring(0, fromEndPos);
        }
        return fromCandidate;
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
            StringBuilder str = new StringBuilder();

            if (dyExpr.getOperator() == Expression.OP_CAST)
            {
                str.append("TREAT(");
                str.append(JPQLQueryHelper.getJPQLForExpression(left));
                str.append(" AS ");
                if (right == null)
                {
                    throw new NucleusUserException("Attempt to CAST but right argument is null");
                }
                str.append(((Literal)right).getLiteral());
                str.append(")");
                return str.toString();
            }

            str.append("(");
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
            Expression exprLeft = expr.getLeft();
            if (exprLeft != null)
            {
                return JPQLQueryHelper.getJPQLForExpression(exprLeft) + "." + primExpr.getId();
            }
            return primExpr.getId();
        }
        else if (expr instanceof ParameterExpression)
        {
            ParameterExpression paramExpr = (ParameterExpression)expr;
            if (paramExpr.getId() != null)
            {
                return ":" + paramExpr.getId();
            }
            return "?" + paramExpr.getPosition();
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
            else if (method.equalsIgnoreCase("LOCAL_DATE"))
            {
                return "LOCAL_DATE";
            }
            else if (method.equalsIgnoreCase("LOCAL_TIME"))
            {
                return "LOCAL_TIME";
            }
            else if (method.equalsIgnoreCase("LOCAL_DATETIME"))
            {
                return "LOCAL_DATETIME";
            }
            else if (method.equalsIgnoreCase("length"))
            {
                StringBuilder str = new StringBuilder("LENGTH(");
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
                StringBuilder str = new StringBuilder();
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(" IS EMPTY");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("indexOf"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform LOCATE without any arguments");
                }
                StringBuilder str = new StringBuilder("LOCATE(");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
                {
                    Expression firstExpr = args.get(0);
                    str.append(",").append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                    if (args.size() > 1)
                    {
                        Expression secondExpr = args.get(1);
                        str.append(",").append(JPQLQueryHelper.getJPQLForExpression(secondExpr));
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("substring"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform SUBSTRING without any arguments");
                }
                StringBuilder str = new StringBuilder("SUBSTRING(");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
                {
                    Expression firstExpr = args.get(0);
                    str.append(",").append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                    if (args.size() > 1)
                    {
                        Expression secondExpr = args.get(1);
                        str.append(",").append(JPQLQueryHelper.getJPQLForExpression(secondExpr));
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trim"))
            {
                StringBuilder str = new StringBuilder("TRIM(BOTH ");

                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
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
                StringBuilder str = new StringBuilder("TRIM(LEADING ");

                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
                {
                    Expression trimChrExpr = args.get(0);
                    str.append(JPQLQueryHelper.getJPQLForExpression(trimChrExpr));
                }

                str.append(" FROM ");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("trimRight"))
            {
                StringBuilder str = new StringBuilder("TRIM(TRAILING ");

                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                if (args != null && !args.isEmpty())
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform LIKE without any arguments");
                }
                StringBuilder str = new StringBuilder();
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
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform MEMBER OF without any arguments");
                }
                StringBuilder str = new StringBuilder();
                Expression firstExpr = args.get(0);
                str.append(JPQLQueryHelper.getJPQLForExpression(firstExpr));
                str.append(" MEMBER OF ");
                str.append(JPQLQueryHelper.getJPQLForExpression(invoked));
                return str.toString();
            }
            else if (method.equalsIgnoreCase("COUNT"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform COUNT without any arguments");
                }
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "COUNT(DISTINCT " + JPQLQueryHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                return "COUNT(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("COALESCE"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform COALESCE without any arguments");
                }
                StringBuilder str = new StringBuilder("COALESCE(");
                if (args != null && !args.isEmpty())
                {
                    for (int i=0;i<args.size();i++)
                    {
                        Expression argExpr = args.get(i);
                        str.append(JPQLQueryHelper.getJPQLForExpression(argExpr));
                        if (i < args.size()-1)
                        {
                            str.append(",");
                        }
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("NULLIF"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform NULLIF without any arguments");
                }
                StringBuilder str = new StringBuilder("NULLIF(");
                if (args != null && !args.isEmpty())
                {
                    for (int i=0;i<args.size();i++)
                    {
                        Expression argExpr = args.get(i);
                        str.append(JPQLQueryHelper.getJPQLForExpression(argExpr));
                        if (i < args.size()-1)
                        {
                            str.append(",");
                        }
                    }
                }
                str.append(")");
                return str.toString();
            }
            else if (method.equalsIgnoreCase("ABS"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform ABS without any arguments");
                }
                String argExprStr = (args != null && !args.isEmpty()) ? JPQLQueryHelper.getJPQLForExpression(args.get(0)) : "";
                return "ABS(" + argExprStr + ")";
            }
            else if (method.equalsIgnoreCase("AVG"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform AVG without any arguments");
                }
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "AVG(DISTINCT " + JPQLQueryHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                return "AVG(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MAX"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform MAX without any arguments");
                }
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "MAX(DISTINCT " + JPQLQueryHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                return "MAX(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("MIN"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform MIN without any arguments");
                }
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "MIN(DISTINCT " + JPQLQueryHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                return "MIN(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQRT"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform SQRT without any arguments");
                }
                Expression argExpr = args.get(0);
                return "SQRT(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SUM"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform SUM without any arguments");
                }
                Expression argExpr = args.get(0);
                if (argExpr instanceof DyadicExpression && ((DyadicExpression)argExpr).getOperator() == Expression.OP_DISTINCT)
                {
                    DyadicExpression dyExpr = (DyadicExpression)argExpr;
                    return "SUM(DISTINCT " + JPQLQueryHelper.getJPQLForExpression(dyExpr.getLeft()) + ")";
                }
                return "SUM(" + JPQLQueryHelper.getJPQLForExpression(argExpr) + ")";
            }
            else if (method.equalsIgnoreCase("SQL_function"))
            {
                if (args == null || args.isEmpty())
                {
                    throw new NucleusUserException("Attempt to perform FUNCTION without any arguments");
                }
                StringBuilder str = new StringBuilder("FUNCTION(");
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
            else if (value instanceof Time)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
                String timeStr = formatter.format((Time)value);
                return "{t '" + timeStr + "'}";
            }
            else if (value instanceof Date)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String dateStr = formatter.format((Date)value);
                return "{d '" + dateStr + "'}";
            }
            else if (value instanceof Timestamp)
            {
                return "{ts '" + value.toString() + "'}";
            }
            else if (value instanceof java.util.Date)
            {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String datetimeStr = formatter.format((java.util.Date)value);
                return "{ts '" + datetimeStr + "'}";
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
        else if (expr instanceof SubqueryExpression)
        {
            SubqueryExpression subqExpr = (SubqueryExpression)expr;
            return subqExpr.getKeyword() + " " + JPQLQueryHelper.getJPQLForExpression(subqExpr.getRight());
        }
        else if (expr instanceof CaseExpression)
        {
            CaseExpression caseExpr = (CaseExpression)expr;
            List<ExpressionPair> conds = caseExpr.getConditions();
            Expression elseExpr = caseExpr.getElseExpression();
            StringBuilder str = new StringBuilder("CASE ");
            if (conds != null)
            {
                for (ExpressionPair cond : conds)
                {
                    Expression whenExpr = cond.getWhenExpression();
                    Expression actionExpr = cond.getActionExpression();
                    str.append("WHEN ");
                    str.append(JPQLQueryHelper.getJPQLForExpression(whenExpr));
                    str.append(" THEN ");
                    str.append(JPQLQueryHelper.getJPQLForExpression(actionExpr));
                    str.append(" ");
                }
            }
            if (elseExpr != null)
            {
                str.append("ELSE ");
                str.append(JPQLQueryHelper.getJPQLForExpression(elseExpr));
                str.append(" ");
            }
            str.append("END");
            return str.toString();
        }
        else
        {
            throw new UnsupportedOperationException("Dont currently support " + expr.getClass().getName() + " in JPQLQueryHelper");
        }
    }
}