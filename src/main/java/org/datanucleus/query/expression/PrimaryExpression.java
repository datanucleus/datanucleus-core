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
2008 Andy Jefferson - added bind check on whether class literal. Javadocs
2008 Andy Jefferson - cater for expression of field of an expression.
2010 Andy Jefferson - cater for expression being an invoke, or variable, or class static call
    ...
**********************************************************************/
package org.datanucleus.query.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.query.symbol.PropertySymbol;
import org.datanucleus.query.symbol.Symbol;
import org.datanucleus.query.symbol.SymbolTable;
import org.datanucleus.util.ClassUtils;

/**
 * Expression for a primary object. 
 * This may be a field, or an explicit variable/parameter, or a field invoked on an expression.
 */
public class PrimaryExpression extends Expression
{
    private static final long serialVersionUID = 6725075523258882792L;
    /** The components of the expression. e.g "a.b.c" will have "a", "b", "c". */
    List<String> tuples;

    /**
     * PrimaryExpression made up of a series of field names.
     * e.g "a.b.c"
     * @param tuples The components of the expression
     */
    public PrimaryExpression(List<String> tuples)
    {
        this.tuples = tuples;
    }

    /**
     * PrimaryExpression on an expression.
     * e.g "((B)a).c" so the left expression is a CastExpression, and the tuples are "c".
     * @param left The left expression
     * @param tuples The tuples of the primary
     */
    public PrimaryExpression(Expression left, List<String> tuples)
    {
        this.left = left;
        if (left != null)
        {
            left.parent = this;
        }
        this.tuples = tuples;
    }

    /**
     * Accessor for the expression "id". This will be something like "a.b.c".
     * @return The id
     */
    public String getId()
    {
        StringBuilder str = new StringBuilder();
        Iterator<String> iter = tuples.iterator();
        while (iter.hasNext())
        {
            String tuple = iter.next();
            if (str.length() > 0)
            {
                str.append('.');
            }
            str.append(tuple);
        }
        return str.toString();
    }

    public List<String> getTuples()
    {
        return tuples;
    }

    /**
     * Method to bind the expression to the symbol table as appropriate.
     * @param symtbl Symbol Table
     * @return The symbol for this expression
     */
    public Symbol bind(SymbolTable symtbl)
    {
        if (left != null)
        {
            left.bind(symtbl);
        }

        // TODO Cater for finding field of left expression
        if (left == null && symtbl.hasSymbol(getId()))
        {
            symbol = symtbl.getSymbol(getId());
            if (symbol.getType() == Symbol.VARIABLE)
            {
                // This is a variable, so needs converting
                throw new PrimaryExpressionIsVariableException(symbol.getQualifiedName());
            }
            return symbol;
        }

        if (left != null)
        {
            return null;
        }

        if (symbol == null)
        {
            // Try with our symbol table
            try
            {
                Class symbolType = symtbl.getSymbolResolver().getType(tuples);
                symbol = new PropertySymbol(getId(), symbolType);
            }
            catch (NucleusUserException nue)
            {
                // Thrown if a field in the primary expression doesn't exist.
            }
        }

        if (symbol == null && symtbl.getParentSymbolTable() != null)
        {
            // Try parent symbol table if present
            try
            {
                Class symbolType = symtbl.getParentSymbolTable().getSymbolResolver().getType(tuples);
                symbol = new PropertySymbol(getId(), symbolType);
            }
            catch (NucleusUserException nue)
            {
                // Thrown if a field in the primary expression doesn't exist.
            }
        }

        if (symbol == null)
        {
            // This may be due to an entry like "org.jpox.samples.MyClass" used for "instanceof"
            String className = getId();

            try
            {
                // Try to find this as a complete class name (e.g as used in "instanceof")
                Class cls = symtbl.getSymbolResolver().resolveClass(className);

                // Represents a valid class so throw exception to get the PrimaryExpression swapped
                throw new PrimaryExpressionIsClassLiteralException(cls);
            }
            catch (ClassNotResolvedException cnre)
            {
                // Try to find classname.staticField
                if (className.indexOf('.') < 0)
                {
                    // {candidateCls}.primary so "primary" is staticField ?
                    Class primaryCls = symtbl.getSymbolResolver().getPrimaryClass();
                    if (primaryCls == null)
                    {
                        throw new NucleusUserException("Class name " + className + " could not be resolved");
                    }
                    try
                    {
                        Field fld = primaryCls.getDeclaredField(className);
                        if (!Modifier.isStatic(fld.getModifiers()))
                        {
                            throw new NucleusUserException("Identifier " + className + " is unresolved (not a static field)");
                        }
                        throw new PrimaryExpressionIsClassStaticFieldException(fld);
                    }
                    catch (NoSuchFieldException nsfe)
                    {
                        if (symtbl.getSymbolResolver().supportsImplicitVariables() && left == null)
                        {
                            // Implicit variable assumed so swap this primary for it
                            throw new PrimaryExpressionIsVariableException(className);
                        }
                        throw new NucleusUserException("Class name " + className + " could not be resolved");
                    }
                }

                try
                {
                    String staticFieldName = className.substring(className.lastIndexOf('.')+1);
                    className = className.substring(0, className.lastIndexOf('.'));
                    Class cls = symtbl.getSymbolResolver().resolveClass(className);
                    try
                    {
                        Field fld = cls.getDeclaredField(staticFieldName);
                        if (!Modifier.isStatic(fld.getModifiers()))
                        {
                            throw new NucleusUserException("Identifier " + className + "." + staticFieldName +
                                    " is unresolved (not a static field)");
                        }
                        throw new PrimaryExpressionIsClassStaticFieldException(fld);
                    }
                    catch (NoSuchFieldException nsfe)
                    {
                        throw new NucleusUserException("Identifier " + className + "." + staticFieldName + 
                                " is unresolved (not a static field)");
                    }
                }
                catch (ClassNotResolvedException cnre2)
                {
                    if (getId().indexOf(".") > 0)
                    {
                        Iterator<String> tupleIter = tuples.iterator();
                        Class cls = null;
                        while (tupleIter.hasNext())
                        {
                            String tuple = tupleIter.next();
                            if (cls == null)
                            {
                                Symbol sym = symtbl.getSymbol(tuple);
                                if (sym == null)
                                {
                                    sym = symtbl.getSymbol("this");
                                    if (sym == null)
                                    {
                                        // TODO Need to get hold of candidate alias
                                        break;
                                    }
                                }
                                cls = sym.getValueType();
                            }
                            else
                            {
                                // Look for member of the current class
                                if (cls.isArray() && tuple.equals("length") && !tupleIter.hasNext())
                                {
                                    // Special case of Array.length
                                    PrimaryExpression primExpr = new PrimaryExpression(left, tuples.subList(0, tuples.size()-1));
                                    InvokeExpression invokeExpr = new InvokeExpression(primExpr, "size", null);
                                    throw new PrimaryExpressionIsInvokeException(invokeExpr);
                                }
                                cls = ClassUtils.getClassForMemberOfClass(cls, tuple);
                            }
                        }
                        if (cls != null)
                        {
                            // TODO Add symbol
                        }
                    }
                }

                if (symtbl.getSymbolResolver().supportsImplicitVariables() && left == null)
                {
                    // Implicit variable, so put as "left" and remove from tuples
                    String varName = tuples.remove(0);
                    VariableExpression varExpr = new VariableExpression(varName);
                    varExpr.bind(symtbl);
                    left = varExpr;
                }
                else
                {
                    // Just throw the original exception
                    throw new NucleusUserException("Cannot find type of (part of) " + getId() + " since symbol has no type; implicit variable?");
                }
            }
        }
        return symbol;
    }

    /**
     * Accessor for string form of the expression.
     * Returns something like "PrimaryExpression {a.b.c}" when left is null, or 
     * "PrimaryExpression {ParameterExpression {a}.b.c}" when left is the ParameterExpression
     */
    public String toString()
    {
        if (left != null)
        {
            return "PrimaryExpression{" + left + "." + getId() + "}" + (alias != null ? " AS " + alias : "");
        }
        return "PrimaryExpression{" + getId() + "}" + (alias != null ? " AS " + alias : "");
    }
}