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

import java.util.Iterator;
import java.util.List;

import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;

/**
 * JDOQL query helper class providing key information about the language etc.
 */
public class JDOQLQueryHelper
{
    /** Keywords used in single-string JDOQL. Uppercase variants specified here, but we allow the lowercase form. */
    static final String[] SINGLE_STRING_KEYWORDS = {
            "SELECT", "UNIQUE", "INTO", "FROM", "EXCLUDE", "SUBCLASSES", "WHERE",
            "VARIABLES", "PARAMETERS", "GROUP", "ORDER", "BY", "RANGE"
            };
    /** Keywords in lowercase (we avoid calling toLowerCase() multiple times, which is expensive operation) **/
    static final String[] SINGLE_STRING_KEYWORDS_LOWERCASE = {
            "select", "unique", "into", "from", "exclude", "subclasses", "where",
            "variables", "parameters", "group", "order", "by", "range"
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
            // JDOQL is case-sensitive - lowercase or UPPERCASE only
            if (name.equals(SINGLE_STRING_KEYWORDS[i]) || 
                name.equals(SINGLE_STRING_KEYWORDS_LOWERCASE[i]))
            {
                return true;
            }
        }
        if (name.equals("IMPORT") || name.equals("import"))
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method returning if the supplied name is a keyword for this query language, allowing
     * the DataNucleus extension keywords (UPDATE, DELETE, SET).
     * @param name Name to check
     * @return Whether it is a keyword
     */
    public static boolean isKeywordExtended(String name)
    {
        for (int i=0;i<SINGLE_STRING_KEYWORDS.length;i++)
        {
            // JDOQL is case-sensitive - lowercase or UPPERCASE only
            if (name.equals(SINGLE_STRING_KEYWORDS[i]) || 
                name.equals(SINGLE_STRING_KEYWORDS_LOWERCASE[i]))
            {
                return true;
            }
        }
        if (name.equals("DELETE") || name.equals("delete"))
        {
            return true;
        }
        if (name.equals("UPDATE") || name.equals("update"))
        {
            return true;
        }
        if (name.equals("SET") || name.equals("set"))
        {
            return true;
        }
        if (name.equals("IMPORT") || name.equals("import"))
        {
            return true;
        }
        return false;
    }

    /**
     * Utility to check if a name is a valid Java identifier.
     * Used by JDOQL in validating the names of parameters/variables.
     * @param s The name
     * @return Whether it is a valid identifier in Java.
     **/
    public static boolean isValidJavaIdentifierForJDOQL(String s)
    {
        int len = s.length();
        if (len < 1)
        {
            return false;
        }

        if (s.equals("this"))
        {
            // Use of "this" is restricted in JDOQL
            return false;
        }

        char[] c = new char[len];
        s.getChars(0, len, c, 0);

        if (!Character.isJavaIdentifierStart(c[0]))
        {
            return false;
        }

        for (int i = 1; i < len; ++i)
        {
            if (!Character.isJavaIdentifierPart(c[i]))
            {
                return false;
            }
        }

        return true;
    }

    public static String getJDOQLForExpression(Expression expr)
    {
        if (expr instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression)expr;
            Expression left = dyExpr.getLeft();
            Expression right = dyExpr.getRight();
            StringBuffer str = new StringBuffer("(");
            if (dyExpr.getOperator() == Expression.OP_DISTINCT)
            {
                // Distinct goes in front of the left expression
                str.append("DISTINCT ");
            }

            if (left != null)
            {
                str.append(JDOQLQueryHelper.getJDOQLForExpression(left));
            }

            // Special cases
            if (dyExpr.getOperator() == Expression.OP_AND)
            {
                str.append(" && ");
            }
            else if (dyExpr.getOperator() == Expression.OP_OR)
            {
                str.append(" || ");
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
                str.append(" == ");
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
                str.append(" != ");
            }
            else if (dyExpr.getOperator() == Expression.OP_DISTINCT)
            {
                // Processed above
            }
            else
            {
                // TODO Support other operators
                throw new UnsupportedOperationException("Dont currently support operator " + dyExpr.getOperator() + " in JDOQL conversion");
            }

            if (right != null)
            {
                str.append(JDOQLQueryHelper.getJDOQLForExpression(right));
            }
            str.append(")");
            return str.toString();
        }
        else if (expr instanceof PrimaryExpression)
        {
            PrimaryExpression primExpr = (PrimaryExpression)expr;
            if (primExpr.getLeft() != null)
            {
                return JDOQLQueryHelper.getJDOQLForExpression(primExpr.getLeft()) + "." + primExpr.getId();
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
            else
            {
                return "?" + paramExpr.getPosition();
            }
        }
        else if (expr instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)expr;
            return varExpr.getId();
        }
        else if (expr instanceof InvokeExpression)
        {
            InvokeExpression invExpr = (InvokeExpression)expr;
            StringBuffer str = new StringBuffer();
            if (invExpr.getLeft() != null)
            {
                str.append(JDOQLQueryHelper.getJDOQLForExpression(invExpr.getLeft())).append(".");
            }
            str.append(invExpr.getOperation());
            str.append("(");
            List<Expression> args = invExpr.getArguments();
            if (args != null)
            {
                Iterator<Expression> iter = args.iterator();
                while (iter.hasNext())
                {
                    str.append(JDOQLQueryHelper.getJDOQLForExpression(iter.next()));
                    if (iter.hasNext())
                    {
                        str.append(",");
                    }
                }
            }
            str.append(")");
            return str.toString();
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
                if (litExpr.getLiteral() == null)
                {
                    return "null";
                }
                else
                {
                    return litExpr.getLiteral().toString();
                }
            }
        }
        else if (expr instanceof VariableExpression)
        {
            VariableExpression varExpr = (VariableExpression)expr;
            return varExpr.getId();
        }
        else
        {
            throw new UnsupportedOperationException("Dont currently support " + expr.getClass().getName() + " in JDOQLHelper");
        }
    }
}