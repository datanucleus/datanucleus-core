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
2008 Andy Jefferson - javadocs
2008 Andy Jefferson - rewritten to allow chaining. Use "left" for what we invoke on, and store "method" explicitly.
    ...
**********************************************************************/
package org.datanucleus.store.query.expression;

import java.util.Iterator;
import java.util.List;

import org.datanucleus.store.query.compiler.Symbol;
import org.datanucleus.store.query.compiler.SymbolTable;
import org.datanucleus.util.StringUtils;

/**
 * Expression representing invocation of a method.
 * This may be an aggregate in a result clause (like "count(this)"), or a method on a class, or a function.
 * The "left" expression is what we are invoking on. This is typically a PrimaryExpression, or an InvokeExpression.
 * This then allows chaining of invocations.
 */
public class InvokeExpression extends Expression
{
    private static final long serialVersionUID = -4907486904172153963L;

    /** Name of the method to invoke. */
    String methodName;

    /** Arguments for the method invocation. */
    List<Expression> arguments;

    /**
     * Constructor for an expression for the invocation of a method/function.
     * @param invoked Expression on which we are invoking
     * @param methodName Name of the method
     * @param args Arguments passed in to the method/function call
     */
    public InvokeExpression(Expression invoked, String methodName, List<Expression> args)
    {
        this.left = invoked;
        this.methodName = methodName;
        this.arguments = args;
        if (invoked != null)
        {
            invoked.parent = this;
        }
        if (args != null && !args.isEmpty())
        {
            Iterator<Expression> argIter = args.iterator();
            while (argIter.hasNext())
            {
                argIter.next().parent = this;
            }
        }
    }

    /**
     * The method/function invoked.
     * @return The method/function invoked.
     */
    public String getOperation()
    {
        return methodName;
    }

    /**
     * Accessor for any arguments to be passed in the invocation.
     * @return The arguments.
     */
    public List<Expression> getArguments()
    {
        return arguments;
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
            catch (PrimaryExpressionIsVariableException pive)
            {
                left = pive.getVariableExpression();
                left.bind(symtbl);
            }
            catch (PrimaryExpressionIsInvokeException piie)
            {
                left = piie.getInvokeExpression();
                left.bind(symtbl);
            }
            catch (PrimaryExpressionIsClassLiteralException picle)
            {
                // We're invoking on a class literal so just convert this invoke into a static invoke on that class
                methodName = ((PrimaryExpression)left).getId() + "." + methodName;
                left = null;
            }
        }
        // TODO Set symbol using the invoked method so we represent what the type really is

        if (arguments != null && !arguments.isEmpty())
        {
            for (int i=0;i<arguments.size();i++)
            {
                Expression expr = arguments.get(i);
                try
                {
                    expr.bind(symtbl);
                }
                catch (PrimaryExpressionIsVariableException pive)
                {
                    VariableExpression ve = pive.getVariableExpression();
                    ve.bind(symtbl);
                    arguments.remove(i);
                    arguments.add(i, ve);
                }
                catch (PrimaryExpressionIsInvokeException piie)
                {
                    InvokeExpression ve = piie.getInvokeExpression();
                    ve.bind(symtbl);
                    arguments.remove(i);
                    arguments.add(i, ve);
                }
                catch (PrimaryExpressionIsClassLiteralException picle)
                {
                    Literal l = picle.getLiteral();
                    l.bind(symtbl);
                    arguments.remove(i);
                    arguments.add(i, l);
                }
            }
        }
        return symbol;
    }

    /**
     * Method to return the string form of this without the alias component.
     * This is useful when we want to compare expressions in a query compile, and the alias is not important
     * to the comparison. Returns a String of the form
     * "InvokeExpression{[left].methodName(args)}" or "InvokeExpression{STATIC.methodName(args)}".
     * @return The string form
     */
    public String toStringWithoutAlias()
    {
        if (left == null)
        {
            return "InvokeExpression{STATIC." + methodName + "(" + StringUtils.collectionToString(arguments) + ")}";
        }
        return "InvokeExpression{[" + left + "]." + methodName + "(" + StringUtils.collectionToString(arguments) + ")}";
    }

    /**
     * Method to return the string form of this expression. Returns a String of the form
     * "InvokeExpression{[left].methodName(args)} AS alias" or "InvokeExpression{STATIC.methodName(args)} AS alias".
     * where it only adds the "AS alias" when the alias is non-null.
     * @return The string form
     */
    public String toString()
    {
        if (left == null)
        {
            return "InvokeExpression{STATIC." + methodName +
                "(" + StringUtils.collectionToString(arguments) + ")}" + (alias != null ? " AS " + alias : "");
        }
        return "InvokeExpression{[" + left + "]." + methodName +
                "(" + StringUtils.collectionToString(arguments) + ")}" + (alias != null ? " AS " + alias : "");
    }
}