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
2008 Andy Jefferson - javadocs. Support for DISTINCT. Support for implicit parameters
2008 Andy Jefferson - compileFrom(), VariableExpression, ParameterExpression
2008 Andy Jefferson - support for CastExpression
2009 Andy Jefferson - support for Literal.invoke
2009 Andy Jefferson - support for arrays
2009 Andy Jefferson - support for subqueries
    ...
**********************************************************************/
package org.datanucleus.query.expression;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.query.compiler.JavaQueryCompiler;
import org.datanucleus.query.compiler.Node;
import org.datanucleus.query.compiler.NodeType;
import org.datanucleus.query.compiler.ParameterNode;
import org.datanucleus.query.compiler.Symbol;
import org.datanucleus.query.compiler.SymbolTable;
import org.datanucleus.query.expression.JoinExpression.JoinQualifier;
import org.datanucleus.query.expression.JoinExpression.JoinType;
import org.datanucleus.store.query.QueryCompilerSyntaxException;
import org.datanucleus.util.NucleusLogger;

/**
 * Compiler for expressions. Responsible for taking a Node tree and creating an Expression tree.
 */
public class ExpressionCompiler
{
    SymbolTable symtbl;

    Map<String, String> aliasByPrefix = null;

    public void setMethodAliases(Map<String, String> aliasByPrefix)
    {
        this.aliasByPrefix = aliasByPrefix;
    }

    public void setSymbolTable(SymbolTable symtbl)
    {
        this.symtbl = symtbl;
    }

    /**
     * Primary entry point for compiling a node for the order clause.
     * @param node The node
     * @return Its compiled expression
     */
    public Expression compileOrderExpression(Node node)
    {
        if (isOperator(node, "order"))
        {
            Node nameNode = node.getFirstChild();
            if (node.getChildNodes().size() > 1)
            {
                String node1Value = (String)node.getNextChild().getNodeValue();
                String node2Value = node.hasNextChild() ? (String)node.getNextChild().getNodeValue() : null;
                String ordering = null;
                String nullOrdering = null;
                if (node1Value.equalsIgnoreCase("ascending") || node1Value.equalsIgnoreCase("descending"))
                {
                    ordering = node1Value;
                    if (node2Value != null)
                    {
                        nullOrdering = node2Value;
                    }
                }
                else
                {
                    nullOrdering = node1Value;
                }
                return new OrderExpression(compileExpression(nameNode), ordering, nullOrdering);
            }
            if (node.getChildNodes().size() == 1)
            {
                return new OrderExpression(compileExpression(nameNode));
            }
        }
        return compileExpression(node.getFirstChild());
    }

    /**
     * Primary entry point for compiling a node for the from clause.
     * @param node The node
     * @param classIsExpression whether the class of the from node is an expression relating to the outer query
     * @return Its compiled expression
     */
    public Expression compileFromExpression(Node node, boolean classIsExpression)
    {
        if (node.getNodeType() == NodeType.CLASS)
        {
            Node aliasNode = node.getFirstChild();

            ClassExpression clsExpr = new ClassExpression((String)aliasNode.getNodeValue());
            if (classIsExpression)
            {
                clsExpr.setCandidateExpression((String)node.getNodeValue());
            }

            // Process any joins, chained down off the ClassExpression
            // So you can do clsExpr.getRight() to get the JoinExpression
            // then joinExpr.getRight() to get the next JoinExpression (if any)
            JoinExpression currentJoinExpr = null;
            Iterator childIter = node.getChildNodes().iterator();
            while (childIter.hasNext())
            {
                Node childNode = (Node)childIter.next();
                if (childNode.getNodeType() == NodeType.OPERATOR)
                {
                    String joinType = (String)childNode.getNodeValue();
                    JoinType joinTypeId = JoinType.JOIN_INNER;
                    if (joinType.equals(JavaQueryCompiler.JOIN_INNER_FETCH))
                    {
                        joinTypeId = JoinType.JOIN_INNER_FETCH;
                    }
                    else if (joinType.equals(JavaQueryCompiler.JOIN_OUTER_FETCH))
                    {
                        joinTypeId = JoinType.JOIN_LEFT_OUTER_FETCH;
                    }
                    else if (joinType.equals(JavaQueryCompiler.JOIN_OUTER_FETCH_RIGHT))
                    {
                        joinTypeId = JoinType.JOIN_RIGHT_OUTER_FETCH;
                    }
                    else if (joinType.equals(JavaQueryCompiler.JOIN_OUTER))
                    {
                        joinTypeId = JoinType.JOIN_LEFT_OUTER;
                    }
                    else if (joinType.equals(JavaQueryCompiler.JOIN_OUTER_RIGHT))
                    {
                        joinTypeId = JoinType.JOIN_RIGHT_OUTER;
                    }

                    Node joinedNode = childNode.getFirstChild();
                    Node joinedAliasNode = childNode.getNextChild();
                    Expression joinedExpr = compilePrimaryExpression(joinedNode);

                    JoinExpression joinExpr = new JoinExpression(joinedExpr, (String)joinedAliasNode.getNodeValue(), joinTypeId);
                    if (currentJoinExpr != null)
                    {
                        currentJoinExpr.setJoinExpression(joinExpr);
                    }
                    else
                    {
                        clsExpr.setJoinExpression(joinExpr);
                    }

                    if (childNode.hasNextChild())
                    {
                        Node nextNode = childNode.getNextChild();
                        if (nextNode.getNodeType() == NodeType.JOIN_QUALIFIER)
                        {
                            // JOIN "qualifier"
                            if (nextNode.getNodeValue() == "KEY")
                            {
                                joinExpr.setQualifier(JoinQualifier.MAP_KEY);
                            }
                            else if (nextNode.getNodeValue() == "VALUE")
                            {
                                joinExpr.setQualifier(JoinQualifier.MAP_VALUE);
                            }

                            if (childNode.hasNextChild())
                            {
                                nextNode = childNode.getNextChild();
                            }
                            else
                            {
                                nextNode = null;
                            }
                        }

                        if (nextNode != null)
                        {
                            // JOIN "ON" expression
                            Expression onExpr = compileExpression(nextNode);
                            if (onExpr != null)
                            {
                                joinExpr.setOnExpression(onExpr);
                            }
                        }
                    }

                    currentJoinExpr = joinExpr;
                }
            }
            return clsExpr;
        }
        return null;
    }

    /**
     * Primary entry point for compiling a node for the filter, grouping, having, result clauses.
     * @param node The node
     * @return Its compiled expression
     */
    public Expression compileExpression(Node node)
    {
        return compileOrAndExpression(node);
    }

    /**
     * This method deals with the OR/AND conditions.
     * A condition specifies a combination of one or more expressions and
     * logical (Boolean) operators and returns a value of TRUE, FALSE, or unknown
     * @param node The Node to process
     */
    private Expression compileOrAndExpression(Node node)
    {
        if (isOperator(node, "||"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_OR,right);
        }
        else if (isOperator(node, "&&"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_AND,right);
        }
        else if (isOperator(node, "|"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_BIT_OR,right);
        }
        else if (isOperator(node, "^"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_BIT_XOR,right);
        }
        else if (isOperator(node, "&"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_BIT_AND,right);
        }
        return compileRelationalExpression(node);
    }

    private Expression compileRelationalExpression(Node node)
    {
        if (isOperator(node, "=="))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left, Expression.OP_EQ, right);
        }
        else if (isOperator(node, "!="))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left, Expression.OP_NOTEQ, right);
        }
        else if (isOperator(node, "LIKE"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left, Expression.OP_LIKE, right);
        }
        else if (isOperator(node, "<="))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_LTEQ,right);
        }
        else if (isOperator(node, ">="))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_GTEQ,right);
        }
        else if (isOperator(node, "<"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_LT,right);
        }
        else if (isOperator(node, ">"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_GT,right);
        }
        else if (isOperator(node, "instanceof"))
        {
            List<Node> childNodes = node.getChildNodes();
            Iterator<Node> childNodeIter = childNodes.iterator();
            Expression left = compileExpression(childNodeIter.next());
            Expression right = null;
            if (childNodes.size() == 2)
            {
                right = compileExpression(childNodeIter.next());
            }
            else
            {
                // Special case of "p instanceof (a,b,c)" meaning p is ONE of (a OR b OR c)
                List<String> collValues = new ArrayList<>();
                while (childNodeIter.hasNext())
                {
                    Node valueNode = childNodeIter.next();
                    if (valueNode.getNodeType() == NodeType.IDENTIFIER)
                    {
                        String value = (String)valueNode.getNodeValue();
                        collValues.add(value);
                    }
                }
                right = new Literal(collValues);
            }
            return new DyadicExpression(left, Expression.OP_IS, right);
        }
        else if (isOperator(node, "IN"))
        {
            // TODO Extend this so we can have multiple arguments in an IN expression, maybe using ArrayExpression
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left, Expression.OP_IN, right);
        }
        else if (isOperator(node, "NOT IN"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left, Expression.OP_NOTIN, right);
        }
        return compileAdditiveMultiplicativeExpression(node);
    }

    private Expression compileAdditiveMultiplicativeExpression(Node node)
    {
        if (isOperator(node, "+"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_ADD,right);
        }
        else if (isOperator(node, "-") && node.getChildNodes().size() > 1) // Subtract so multiple children
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_SUB,right);
        }
        else if (isOperator(node, "*"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_MUL,right);
        }
        else if (isOperator(node, "/"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_DIV,right);
        }
        else if (isOperator(node, "%"))
        {
            Expression left = compileExpression(node.getFirstChild());
            Expression right = compileExpression(node.getNextChild());
            return new DyadicExpression(left,Expression.OP_MOD,right);
        }
        return compileUnaryExpression(node);
    }

    private Expression compileUnaryExpression(Node node)
    {
        if (isOperator(node, "-") && node.getChildNodes().size() == 1) // Negate, not subtract so one child
        {
            Expression left = compileExpression(node.getFirstChild());
            if (left instanceof Literal)
            {
                // Swap sign of the literal rather than leaving a negate operator in
                ((Literal)left).negate();
                return left;
            }
            return new DyadicExpression(Expression.OP_NEG, left);
        }
        else if (isOperator(node, "~"))
        {
            Expression left = compileExpression(node.getFirstChild());
            return new DyadicExpression(Expression.OP_COM, left);
        }
        else if (isOperator(node, "!"))
        {
            Expression left = compileExpression(node.getFirstChild());
            if (left instanceof DyadicExpression && left.getOperator() == Expression.OP_IS)
            {
                // Convert "NOT ({expr} IS {expr2})" into "{expr} ISNOT {expr2}"
                DyadicExpression leftExpr = (DyadicExpression)left;
                return new DyadicExpression(leftExpr.getLeft(), Expression.OP_ISNOT, leftExpr.getRight());
            }
            return new DyadicExpression(Expression.OP_NOT, left);
        }
        else if (isOperator(node, "DISTINCT"))
        {
            Expression left = compileExpression(node.getFirstChild());
            return new DyadicExpression(Expression.OP_DISTINCT, left);
        }
        return compilePrimaryExpression(node);
    }    

    private Expression compilePrimaryExpression(Node node)
    {
        if (node.getNodeType() == NodeType.PRIMARY)
        {
            Node currentNode = node.getFirstChild();
            Node invokeNode = node.getNextChild();
            if (invokeNode.getNodeType() != NodeType.INVOKE)
            {
                // TODO Support more combinations
                throw new QueryCompilerSyntaxException("Dont support compilation of " + node);
            }
            Expression currentExpr = compileExpression(currentNode);
            String methodName = (String)invokeNode.getNodeValue();
            List parameterExprs = getExpressionsForPropertiesOfNode(invokeNode);
            Expression invokeExpr = new InvokeExpression(currentExpr, methodName, parameterExprs);
            return invokeExpr;
        }
        else if (node.getNodeType() == NodeType.IDENTIFIER)
        {
            Node currentNode = node;
            List tupple = new ArrayList();
            Expression currentExpr = null;
            while (currentNode != null)
            {
                tupple.add(currentNode.getNodeValue());

                if (currentNode.getNodeType() == NodeType.INVOKE)
                {
                    if (currentExpr == null && tupple.size() > 1)
                    {
                        // Check for starting with parameter/variable
                        String first = (String)tupple.get(0);
                        Symbol firstSym = symtbl.getSymbol(first);
                        if (firstSym != null)
                        {
                            if (firstSym.getType() == Symbol.PARAMETER)
                            {
                                currentExpr = new ParameterExpression(first, -1);
                                if (tupple.size() > 2)
                                {
                                    currentExpr = new PrimaryExpression(currentExpr, tupple.subList(1, tupple.size()-1));
                                }
                            }
                            else if (firstSym.getType() == Symbol.VARIABLE)
                            {
                                currentExpr = new VariableExpression(first);
                                if (tupple.size() > 2)
                                {
                                    currentExpr = new PrimaryExpression(currentExpr, tupple.subList(1, tupple.size()-1));
                                }
                            }
                        }
                        if (currentExpr == null)
                        {
                            currentExpr = new PrimaryExpression(tupple.subList(0, tupple.size()-1));
                        }
                    }
                    else if (currentExpr != null && tupple.size() > 1)
                    {
                        currentExpr = new PrimaryExpression(currentExpr, tupple.subList(0, tupple.size()-1));
                    }

                    String methodName = (String)tupple.get(tupple.size()-1);
                    if (currentExpr instanceof PrimaryExpression)
                    {
                        // Check if this is a defined method prefix, and if so use the alias
                        String id = ((PrimaryExpression)currentExpr).getId();
                        if (aliasByPrefix != null && aliasByPrefix.containsKey(id))
                        {
                            String alias = aliasByPrefix.get(id);
                            methodName = alias + "." + methodName;
                            currentExpr = null;
                        }
                    }
                    List parameterExprs = getExpressionsForPropertiesOfNode(currentNode);
                    currentExpr = new InvokeExpression(currentExpr, methodName, parameterExprs);

                    currentNode = currentNode.getFirstChild();
                    tupple = new ArrayList();
                }
                else if (currentNode.getNodeType() == NodeType.CAST)
                {
                    if (currentExpr == null && tupple.size() > 1)
                    {
                        // Start from PrimaryExpression and invoke on that
                        currentExpr = new PrimaryExpression(tupple.subList(0, tupple.size()-1));
                        PrimaryExpression primExpr = (PrimaryExpression)currentExpr;
                        if (primExpr.tuples.size() == 1)
                        {
                            Symbol sym = symtbl.getSymbol(primExpr.getId());
                            if (sym != null)
                            {
                                if (sym.getType() == Symbol.PARAMETER)
                                {
                                    // Parameter symbol registered for this identifier so use ParameterExpression
                                    currentExpr = new ParameterExpression(primExpr.getId(), -1);
                                }
                                else if (sym.getType() == Symbol.VARIABLE)
                                {
                                    // Variable symbol registered for this identifier so use VariableExpression
                                    currentExpr = new VariableExpression(primExpr.getId());
                                }
                            }
                        }
                    }

                    String className = (String)tupple.get(tupple.size()-1);
                    currentExpr = new DyadicExpression(currentExpr, Expression.OP_CAST, new Literal(className));

                    currentNode = currentNode.getFirstChild();
                    tupple = new ArrayList();
                }
                else
                {
                    // Part of identifier chain
                    currentNode = currentNode.getFirstChild();
                }
            }

            if (currentExpr != null && !tupple.isEmpty())
            {
                // We have a trailing identifier expression 
                // e.g "((B)a).c" where we have a CastExpression and trailing "c"
                currentExpr = new PrimaryExpression(currentExpr, tupple);
            }

            if (currentExpr == null)
            {
                // Find type of first of tupples
                String first = (String)tupple.get(0);
                Symbol firstSym = symtbl.getSymbol(first);
                if (firstSym != null)
                {
                    if (firstSym.getType() == Symbol.PARAMETER)
                    {
                        ParameterExpression paramExpr = new ParameterExpression(first, -1);
                        if (tupple.size() > 1)
                        {
                            currentExpr = new PrimaryExpression(paramExpr, tupple.subList(1, tupple.size()));
                        }
                        else
                        {
                            currentExpr = paramExpr;
                        }
                    }
                    else if (firstSym.getType() == Symbol.VARIABLE)
                    {
                        VariableExpression varExpr = new VariableExpression(first);
                        if (tupple.size() > 1)
                        {
                            currentExpr = new PrimaryExpression(varExpr, tupple.subList(1, tupple.size()));
                        }
                        else
                        {
                            currentExpr = varExpr;
                        }
                    }
                    else
                    {
                        currentExpr = new PrimaryExpression(tupple);
                    }
                }
                else
                {
                    currentExpr = new PrimaryExpression(tupple);
                }
            }

            return currentExpr;
        }
        else if (node.getNodeType() == NodeType.PARAMETER)
        {
            // "{paramExpr}", "{paramExpr}.invoke(...)", "{paramExpr}.invoke(...).invoke(...)"
            Object val = node.getNodeValue();
            Expression currentExpr = null;
            if (val instanceof Integer)
            {
                // Positional parameter TODO Store as Integer to avoid confusion
                currentExpr = new ParameterExpression("" + node.getNodeValue(),
                    ((ParameterNode)node).getPosition());
            }
            else
            {
                // Named parameter
                currentExpr = new ParameterExpression((String)node.getNodeValue(),
                    ((ParameterNode)node).getPosition());
            }

            Node childNode = node.getFirstChild();
            while (childNode != null)
            {
                if (childNode.getNodeType() == NodeType.INVOKE)
                {
                    String methodName = (String)childNode.getNodeValue();
                    List parameterExprs = getExpressionsForPropertiesOfNode(childNode);
                    currentExpr = new InvokeExpression(currentExpr, methodName, parameterExprs);
                }
                else if (childNode.getNodeType() == NodeType.IDENTIFIER)
                {
                    String identifier = childNode.getNodeId();
                    List tuples = new ArrayList();
                    tuples.add(identifier);
                    boolean moreIdentifierNodes = true;
                    while (moreIdentifierNodes)
                    {
                        Node currentNode = childNode;
                        childNode = childNode.getFirstChild();
                        if (childNode == null || childNode.getNodeType() != NodeType.IDENTIFIER)
                        {
                            moreIdentifierNodes = false;
                            childNode = currentNode;
                        }
                        else
                        {
                            // Add as a component of the primary
                            tuples.add(childNode.getNodeId());
                        }
                    }
                    currentExpr = new PrimaryExpression(currentExpr, tuples);
                }
                else
                {
                    throw new QueryCompilerSyntaxException("Dont support compilation of " + node);
                }
                childNode = childNode.getFirstChild();
            }
            return currentExpr;
        }
        else if (node.getNodeType() == NodeType.INVOKE)
        {
            Node currentNode = node;
            List tupple = new ArrayList();
            Expression currentExpr = null;
            while (currentNode != null)
            {
                tupple.add(currentNode.getNodeValue());

                if (currentNode.getNodeType() == NodeType.INVOKE)
                {
                    String methodName = (String)tupple.get(tupple.size()-1);
                    List parameterExprs = getExpressionsForPropertiesOfNode(currentNode);
                    currentExpr = new InvokeExpression(currentExpr, methodName, parameterExprs);

                    currentNode = currentNode.getFirstChild();
                    if (currentNode != null)
                    {
                        // Continue on along the chain
                        tupple = new ArrayList();
                        tupple.add(currentExpr); // Start from this expression
                    }
                }
                else
                {
                    // TODO What node type is this that comes after an INVOKE?
                    currentNode = currentNode.getFirstChild();
                }
            }
            return currentExpr;
        }
        else if (node.getNodeType() == NodeType.CREATOR)
        {
            Node currentNode = node.getFirstChild();
            List tupple = new ArrayList();
            boolean method = false;
            while (currentNode != null)
            {
                tupple.add(currentNode.getNodeValue());
                if (currentNode.getNodeType() == NodeType.INVOKE)
                {
                    method = true;
                    break;
                }
                currentNode = currentNode.getFirstChild();
            }

            List parameterExprs = null;
            if (method)
            {
                parameterExprs = getExpressionsForPropertiesOfNode(currentNode);
            }
            else
            {
                parameterExprs = new ArrayList();
            }
            return new CreatorExpression(tupple, parameterExprs);
        }
        else if (node.getNodeType() == NodeType.LITERAL)
        {
            Node currentNode = node;
            List tupple = new ArrayList();
            Expression currentExpr = null;
            while (currentNode != null)
            {
                tupple.add(currentNode.getNodeValue());

                if (currentNode.getNodeType() == NodeType.INVOKE)
                {
                    if (currentExpr == null && tupple.size() > 1)
                    {
                        // Start from Literal and invoke on that
                        currentExpr = new Literal(node.getNodeValue());
                    }

                    String methodName = (String)tupple.get(tupple.size()-1);
                    List parameterExprs = getExpressionsForPropertiesOfNode(currentNode);
                    currentExpr = new InvokeExpression(currentExpr, methodName, parameterExprs);

                    currentNode = currentNode.getFirstChild();
                    tupple = new ArrayList();
                }
                else
                {
                    currentNode = currentNode.getFirstChild();
                }
            }

            if (currentExpr == null)
            {
                currentExpr = new Literal(node.getNodeValue());
            }
            return currentExpr;
        }
        else if (node.getNodeType() == NodeType.ARRAY)
        {
            Node currentNode = node;
            List<Node> arrayElements = (List<Node>)node.getNodeValue();

            // Check if this is an array literal
            boolean literal = true;
            Class type = null;
            Iterator<Node> iter = arrayElements.iterator();
            while (iter.hasNext())
            {
                Node element = iter.next();
                if (type == null)
                {
                    type = element.getNodeValue().getClass();
                }
                if (element.getNodeType() == NodeType.IDENTIFIER)
                {
                    literal = false;
                    break;
                }
            }

            Expression currentExpr = null;
            if (literal)
            {
                Object array = Array.newInstance(type, arrayElements.size());
                iter = arrayElements.iterator();
                int index = 0;
                while (iter.hasNext())
                {
                    Node element = iter.next();
                    Array.set(array, index++, element.getNodeValue());
                }
                currentExpr = new Literal(array);
            }
            else
            {
                Expression[] arrayElementExprs = new Expression[arrayElements.size()];
                for (int i=0;i<arrayElementExprs.length;i++)
                {
                    arrayElementExprs[i] = compilePrimaryExpression(arrayElements.get(i));
                }
                currentExpr = new ArrayExpression(arrayElementExprs);
            }
            currentNode = currentNode.getFirstChild();

            List tupple = new ArrayList();
            while (currentNode != null)
            {
                tupple.add(currentNode.getNodeValue());

                if (currentNode.getNodeType() == NodeType.INVOKE)
                {
                    if (tupple.size() > 1)
                    {
                        // Start from Literal and invoke on that
                        currentExpr = new Literal(node.getNodeValue());
                    }

                    String methodName = (String)tupple.get(tupple.size()-1);
                    List parameterExprs = getExpressionsForPropertiesOfNode(currentNode);
                    currentExpr = new InvokeExpression(currentExpr, methodName, parameterExprs);

                    currentNode = currentNode.getFirstChild();
                    tupple = new ArrayList();
                }
                else
                {
                    currentNode = currentNode.getFirstChild();
                }
            }

            return currentExpr;
        }
        else if (node.getNodeType() == NodeType.SUBQUERY)
        {
            List children = node.getChildNodes();
            if (children.size() != 1)
            {
                throw new QueryCompilerSyntaxException("Invalid number of children for SUBQUERY node : " + node);
            }
            Node varNode = (Node)children.get(0);
            VariableExpression subqueryExpr = new VariableExpression(varNode.getNodeId());
            Expression currentExpr = new SubqueryExpression((String)node.getNodeValue(), subqueryExpr);
            return currentExpr;
        }
        else if (node.getNodeType() == NodeType.CASE)
        {
            // Node with children in the order "when", "action"[, "when", "action"], "else"
            List<Node> children = node.getChildNodes();
            if ((children.size()%2) == 0)
            {
                throw new QueryCompilerSyntaxException("Invalid number of children for CASE node (should be odd) : " + node);
            }

            Node elseNode = children.get(children.size()-1);
            Expression elseExpr = compileExpression(elseNode);
            CaseExpression caseExpr = new CaseExpression();
            Iterator<Node> childIter = children.iterator();
            while (childIter.hasNext())
            {
                Node whenNode = childIter.next();
                if (childIter.hasNext())
                {
                    Node actionNode = childIter.next();
                    Expression whenExpr = compileExpression(whenNode);
                    Expression actionExpr = compileExpression(actionNode);
                    caseExpr.addCondition(whenExpr, actionExpr);
                }
            }
            caseExpr.setElseExpression(elseExpr);
            return caseExpr;
        }
        else
        {
            NucleusLogger.QUERY.warn("ExpressionCompiler.compilePrimaryExpression node=" + node + " ignored by ExpressionCompiler since not of a supported type");
        }
        return null;
    }

    /**
     * Convenience method to extract properties for this node and return the associated list of expressions.
     * @param node The node
     * @return The list of expressions for the properties (or null if no properties)
     */
    private List<Expression> getExpressionsForPropertiesOfNode(Node node)
    {
        if (node.hasProperties())
        {
            List<Expression> parameterExprs = new ArrayList();
            List propNodes = node.getProperties();
            for (int i=0;i<propNodes.size();i++)
            {
                parameterExprs.add(compileExpression((Node)propNodes.get(i)));
            }
            return parameterExprs;
        }
        return Collections.EMPTY_LIST;
    }

    private boolean isOperator(Node node, String operator)
    {
        return node.getNodeType() == NodeType.OPERATOR && node.getNodeValue().equals(operator);
    }
}