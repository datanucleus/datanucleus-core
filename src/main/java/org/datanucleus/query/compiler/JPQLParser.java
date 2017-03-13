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
package org.datanucleus.query.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.QueryCompilerSyntaxException;

/**
 * Implementation of a parser for JPQL query language.
 * Generates Node tree(s) by use of the various parseXXX() methods.
 */
public class JPQLParser extends AbstractParser
{
    /** Characters that parameters can be prefixed by. */
    private static String paramPrefixes = ":?";

    /**
     * Constructor for a JPQL Parser.
     */
    public JPQLParser()
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parse(java.lang.String)
     */
    public Node parse(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        Node result = processExpression();

        if (lexer.ci.getIndex() != lexer.ci.getEndIndex())
        {
            // Error occurred in the JDOQL processing due to syntax error(s)
            throw new QueryCompilerSyntaxException("Portion of expression could not be parsed: " + lexer.getInput().substring(lexer.ci.getIndex()));
        }
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseVariable(java.lang.String)
     */
    public Node parseVariable(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        if (!processIdentifier())
        {
            throw new QueryCompilerSyntaxException("expected identifier", lexer.getIndex(), lexer.getInput());
        }
        if (!processIdentifier())
        {
            throw new QueryCompilerSyntaxException("expected identifier", lexer.getIndex(), lexer.getInput());
        }
        Node nodeVariable = stack.pop();
        Node nodeType = stack.pop();
        nodeType.appendChildNode(nodeVariable);
        return nodeType;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseFrom(java.lang.String)
     */
    public Node[] parseFrom(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        return processFromExpression();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseUpdate(java.lang.String)
     */
    public Node[] parseUpdate(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        return parseTuple(expression);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseOrder(java.lang.String)
     */
    public Node[] parseOrder(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        return processOrderExpression();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseResult(java.lang.String)
     */
    public Node[] parseResult(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node node = stack.pop();

            String alias = lexer.parseIdentifier();
            if (alias != null && alias.equalsIgnoreCase("AS"))
            {
                alias = lexer.parseIdentifier();
            }
            if (alias != null)
            {
                Node aliasNode = new Node(NodeType.NAME, alias.toLowerCase()); // Save all aliases in lowercase
                node.appendChildNode(aliasNode);
            }

            nodes.add(node);
        }
        while (lexer.parseString(","));
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseTuple(java.lang.String)
     */
    public Node[] parseTuple(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node node = stack.pop();
            nodes.add(node);
        }
        while (lexer.parseString(","));
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseVariables(java.lang.String)
     */
    public Node[][] parseVariables(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        List nodes = new ArrayList();
        do
        {
            processPrimary();
            if (stack.isEmpty())
            {
                throw new QueryCompilerSyntaxException("expected identifier", lexer.getIndex(), lexer.getInput());
            }
            if (!processIdentifier())
            {
                throw new QueryCompilerSyntaxException("expected identifier", lexer.getIndex(), lexer.getInput());
            }
            Node nodeVariable = stack.pop();
            Node nodeType = stack.pop();
            nodes.add(new Node[]{nodeType, nodeVariable});
        }
        while (lexer.parseString(";"));
        return (Node[][]) nodes.toArray(new Node[nodes.size()][2]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseParameters(java.lang.String)
     */
    public Node[][] parseParameters(String expression)
    {
        lexer = new Lexer(expression, paramPrefixes, false);
        List nodes = new ArrayList();
        do
        {
            processPrimary();
            if (stack.isEmpty())
            {
                throw new QueryCompilerSyntaxException("expected identifier", lexer.getIndex(), lexer.getInput());
            }
            if (!processIdentifier())
            {
                throw new QueryCompilerSyntaxException("expected identifier", lexer.getIndex(), lexer.getInput());
            }
            Node nodeVariable = stack.pop();
            Node nodeType = stack.pop();
            nodes.add(new Node[]{nodeType, nodeVariable});
        }
        while (lexer.parseString(","));
        return (Node[][]) nodes.toArray(new Node[nodes.size()][2]);
    }

    /**
     * The FROM expression in JPQL is a comma-separated list of expressions. Each expression can be
     * <ul>
     * <li>"IN {expression} [AS] alias [JOIN ... AS ...]"</li>
     * <li>mydomain.MyClass [AS] alias [JOIN ... AS ...]"</li>
     * </ul>
     * @return Node tree(s) for the FROM expression
     */
    private Node[] processFromExpression()
    {
        String candidateClassName = null;
        String candidateAlias = null;
        List nodes = new ArrayList();
        do
        {
            if (lexer.peekStringIgnoreCase("IN(") || lexer.peekStringIgnoreCase("IN "))
            {
                lexer.parseStringIgnoreCase("IN");
                // IN expression
                // This will create a node of type "Node.CLASS" (candidate) and child of type "Node.NAME" (alias)
                // Any IN/JOINs will be child nodes of type "Node.OPERATOR"
                // Node(CLASS, "org.datanucleus.CandidateClass)
                // +--- Node(NAME, "c");
                // +--- Node(OPERATOR, "JOIN_INNER")
                //    +--- Node(IDENTIFIER, "p.myField")
                //    +--- Node(NAME, "f")
                // +--- Node(OPERATOR, "JOIN_INNER")
                //    +--- Node(IDENTIFIER, "f.myField2")
                //    +--- Node(NAME, "g")
                if (!lexer.parseChar('('))
                {
                    throw new QueryCompilerSyntaxException("Expected: '(' but got " + lexer.remaining(), 
                        lexer.getIndex(), lexer.getInput());
                }

                // Find what we are joining to
                String name = lexer.parseIdentifier();
                Node joinedNode = new Node(NodeType.IDENTIFIER, name);
                Node parentNode = joinedNode;
                while (lexer.nextIsDot())
                {
                    lexer.parseChar('.');
                    String subName = lexer.parseIdentifier();
                    Node subNode = new Node(NodeType.IDENTIFIER, subName);
                    parentNode.appendChildNode(subNode);
                    parentNode = subNode;
                }

                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("Expected: ')' but got " + lexer.remaining(), 
                        lexer.getIndex(), lexer.getInput());
                }
                lexer.parseStringIgnoreCase("AS"); // Optional
                String alias = lexer.parseIdentifier();

                // Create candidate class node with alias, and put at top of stack
                Node classNode = new Node(NodeType.CLASS, candidateClassName);
                Node classAliasNode = new Node(NodeType.NAME, candidateAlias);
                classNode.insertChildNode(classAliasNode);
                stack.push(classNode);

                // Translate the "IN(...) alias" into the equivalent JOIN syntax nodes
                Node joinNode = new Node(NodeType.OPERATOR, JavaQueryCompiler.JOIN_INNER);
                joinNode.appendChildNode(joinedNode);
                Node joinAliasNode = new Node(NodeType.NAME, alias);
                joinNode.appendChildNode(joinAliasNode);
                classNode.appendChildNode(joinNode);

                // Include any joins for this expression
                processFromJoinExpression();

                nodes.add(classNode);
            }
            else
            {
                // "candidate [AS] alias"
                // This will create a node of type "Node.CLASS" and child of type "Node.NAME" (alias)
                // Any JOINs will be child nodes of type "Node.OPERATOR"
                processExpression();
                Node id = stack.pop();
                StringBuilder className = new StringBuilder(id.getNodeValue().toString());
                while (!id.getChildNodes().isEmpty())
                {
                    id = id.getFirstChild();
                    className.append(".").append(id.getNodeValue().toString());
                }

                String alias = lexer.parseIdentifier();
                if (alias != null && alias.equalsIgnoreCase("AS"))
                {
                    alias = lexer.parseIdentifier();
                }
                if (candidateClassName == null)
                {
                    candidateClassName = className.toString();
                    candidateAlias = alias;
                }

                // Create candidate class node with alias, and put at top of stack
                Node classNode = new Node(NodeType.CLASS, className.toString());
                Node aliasNode = new Node(NodeType.NAME, alias);
                classNode.insertChildNode(aliasNode);
                stack.push(classNode);

                // Include any joins for this expression
                processFromJoinExpression();

                nodes.add(classNode);
            }
        }
        while (lexer.parseString(","));

        return (Node[]) nodes.toArray(new Node[nodes.size()]);
    }

    /**
     * Convenience method to process what remains of a component of the FROM clause, processing
     * the JOIN conditions. The leading part (candidate, or "IN") was processed just before.
     * This will append a child to the candidate node for each join condition encountered.
     * For example, "org.datanucleus.MyClass p INNER JOIN p.myField AS f" will translate to
     * <pre>
     * Node(CLASS, "org.datanucleus.MyClass)
     * +--- Node(NAME, "p")
     * +--- Node(OPERATOR, "JOIN_INNER")
     *    +--- Node(IDENTIFIER, "p.myField")
     *    +--- Node(NAME, "f")
     *    +--- Node(OPERATOR, "==")       <====== ON condition
     *       +--- Node(IDENTIFIER, "f.someField")
     *       +--- Node(NAME, "value")
     * </pre>
     * When we enter this method we expect the candidate node to be at the top of the stack, and when
     * we leave this method we leave the candidate node at the top of the stack.
     */
    private void processFromJoinExpression()
    {
        Node candidateNode = stack.pop();
        boolean moreJoins = true;
        while (moreJoins)
        {
            // Check for JOIN syntax "[LEFT|RIGHT [OUTER] | INNER] JOIN ... [ON {cond_expr}]"  (EJB3 syntax)
            boolean leftJoin = false;
            boolean rightJoin = false; // extension to JPA
            boolean innerJoin = false;
            if (lexer.parseStringIgnoreCase("INNER "))
            {
                innerJoin = true;
            }
            else if (lexer.parseStringIgnoreCase("LEFT "))
            {
                //optional and useless (for parser) outer keyword
                lexer.parseStringIgnoreCase("OUTER");
                leftJoin = true;
            }
            else if (lexer.parseStringIgnoreCase("RIGHT "))
            {
                //optional and useless (for parser) outer keyword
                lexer.parseStringIgnoreCase("OUTER");
                rightJoin = true;
            }

            if (lexer.parseStringIgnoreCase("JOIN "))
            {
                if (!innerJoin && !leftJoin && !rightJoin)
                {
                    innerJoin = true;
                }
                // Process the join
                boolean fetch = false;
                if (lexer.parseStringIgnoreCase("FETCH"))
                {
                    fetch = true;
                }

                String joinType = JavaQueryCompiler.JOIN_INNER;
                if (innerJoin)
                {
                    joinType = fetch ? JavaQueryCompiler.JOIN_INNER_FETCH : JavaQueryCompiler.JOIN_INNER;
                }
                else if (leftJoin)
                {
                    joinType = fetch ? JavaQueryCompiler.JOIN_OUTER_FETCH : JavaQueryCompiler.JOIN_OUTER;
                }
                else if (rightJoin)
                {
                    joinType = fetch ? JavaQueryCompiler.JOIN_OUTER_FETCH_RIGHT : JavaQueryCompiler.JOIN_OUTER_RIGHT;
                }
                Node joinNode = new Node(NodeType.OPERATOR, joinType);

                // Find what we are joining to
                if (processTreat())
                {
                    // Joining to a TREAT expression with alias : "TREAT (path_expression AS subtype) [AS alias]" TODO Support nested TREAT?
                    Node treatNode = stack.pop();
                    joinNode.appendChildNode(treatNode);

                    // And the alias we know this joined field by
                    lexer.parseStringIgnoreCase("AS "); // Optional
                    String alias = lexer.parseName();
                    Node joinedAliasNode = new Node(NodeType.NAME, alias);
                    joinNode.appendChildNode(joinedAliasNode);
                }
                else if (processKey(true))
                {
                    Node keyNode = stack.pop();
                    joinNode.appendChildNode(keyNode);

                    // Add the alias we know this joined field by
                    lexer.parseStringIgnoreCase("AS "); // Optional
                    String alias = lexer.parseName();
                    Node joinedAliasNode = new Node(NodeType.NAME, alias);
                    joinNode.appendChildNode(joinedAliasNode);
                }
                else if (processValue(true))
                {
                    Node valNode = stack.pop();
                    joinNode.appendChildNode(valNode);

                    // And the alias we know this joined field by
                    lexer.parseStringIgnoreCase("AS "); // Optional
                    String alias = lexer.parseName();
                    Node joinedAliasNode = new Node(NodeType.NAME, alias);
                    joinNode.appendChildNode(joinedAliasNode);
                }
                else
                {
                    // Joining to an identifier with alias : "path_expression [AS alias]"
                    String id = lexer.parseIdentifier();

                    Node joinedNode = new Node(NodeType.IDENTIFIER, id);
                    Node parentNode = joinedNode;
                    while (lexer.nextIsDot())
                    {
                        lexer.parseChar('.');
                        Node subNode = new Node(NodeType.IDENTIFIER, lexer.parseName());
                        parentNode.appendChildNode(subNode);
                        parentNode = subNode;
                    }
                    joinNode.appendChildNode(joinedNode);

                    // And the alias we know this joined field by
                    lexer.parseStringIgnoreCase("AS "); // Optional
                    String alias = lexer.parseName();
                    Node joinedAliasNode = new Node(NodeType.NAME, alias);
                    joinNode.appendChildNode(joinedAliasNode);
                }

                // Optional ON clause
                if (lexer.parseStringIgnoreCase("ON "))
                {
                    // JPA2.1 : Process "ON {cond_expr}"
                    processExpression();
                    Node onNode = stack.pop();
                    joinNode.appendChildNode(onNode);
                }

                candidateNode.appendChildNode(joinNode);
            }
            else
            {
                if (innerJoin || leftJoin)
                {
                    throw new NucleusUserException("Expected JOIN after INNER/LEFT keyword at"+lexer.remaining());
                }
                moreJoins = false;
            }
        }
        stack.push(candidateNode);
    }

    private Node[] processOrderExpression()
    {
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node directionNode = null;
            if (lexer.parseStringIgnoreCase("asc"))
            {
                directionNode = new Node(NodeType.OPERATOR, "ascending");
            }
            else if (lexer.parseStringIgnoreCase("desc"))
            {
                directionNode = new Node(NodeType.OPERATOR, "descending");
            }
            else
            {
                // Default to ascending
                directionNode = new Node(NodeType.OPERATOR, "ascending");
            }

            // Nulls positioning
            Node nullsNode = null;
            if (lexer.parseString("NULLS FIRST") || lexer.parseString("nulls first"))
            {
                nullsNode = new Node(NodeType.OPERATOR, "nulls first");
            }
            else if (lexer.parseString("NULLS LAST") || lexer.parseString("nulls last"))
            {
                nullsNode = new Node(NodeType.OPERATOR, "nulls last");
            }

            Node expr = new Node(NodeType.OPERATOR, "order");
            expr.insertChildNode(directionNode);
            if (nullsNode != null)
            {
                expr.appendChildNode(nullsNode);
            }

            if (!stack.isEmpty())
            {
                expr.insertChildNode(stack.pop());
            }
            nodes.add(expr);
        }
        while (lexer.parseString(","));
        return (Node[]) nodes.toArray(new Node[nodes.size()]);
    }

    private Node processExpression()
    {
        processOrExpression();
        return stack.peek();
    }

    /**
     * This method deals with the OR condition.
     * A condition specifies a combination of one or more expressions and logical (Boolean) operators and 
     * returns a value of TRUE, FALSE, or unknown
     */
    private void processOrExpression()
    {
        processAndExpression();

        while (lexer.parseStringIgnoreCase("OR "))
        {
            processAndExpression();
            Node expr = new Node(NodeType.OPERATOR, "||");
            expr.insertChildNode(stack.pop());
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
    }

    /**
     * This method deals with the AND condition.
     * A condition specifies a combination of one or more expressions and
     * logical (Boolean) operators and returns a value of TRUE, FALSE, or unknown.
     */
    private void processAndExpression()
    {
        processRelationalExpression();

        while (lexer.parseStringIgnoreCase("AND "))
        {
            processRelationalExpression();
            Node expr = new Node(NodeType.OPERATOR, "&&");
            expr.insertChildNode(stack.pop());
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
    }

    /**
     * Parse of an expression that is a relation between expressions.
     */
    private void processRelationalExpression()
    {
        processAdditiveExpression();

        for (;;)
        {
            if (lexer.parseString("="))
            {
                processAdditiveExpression();
                Node right = stack.pop();
                Node left = stack.pop();
                if (right.getNodeType() == NodeType.TYPE)
                {
                    // Convert TYPE into "instanceof"
                    Node primNode = right.getFirstChild();
                    Node expr = new Node(NodeType.OPERATOR, "instanceof");
                    expr.appendChildNode(primNode);
                    expr.appendChildNode(left);
                    stack.push(expr);
                }
                else if (left.getNodeType() == NodeType.TYPE)
                {
                    // Convert TYPE into "instanceof"
                    Node primNode = left.getFirstChild();
                    Node expr = new Node(NodeType.OPERATOR, "instanceof");
                    expr.appendChildNode(primNode);
                    expr.appendChildNode(right);
                    stack.push(expr);
                }
                else
                {
                    Node expr = new Node(NodeType.OPERATOR, "==");
                    expr.insertChildNode(right);
                    expr.insertChildNode(left);
                    stack.push(expr);
                }
            }
            else if (lexer.parseString("<>"))
            {
                processAdditiveExpression();
                Node right = stack.pop();
                Node left = stack.pop();
                if (right.getNodeType() == NodeType.TYPE)
                {
                    // Convert TYPE into "instanceof"
                    Node primNode = right.getFirstChild();
                    Node expr = new Node(NodeType.OPERATOR, "instanceof");
                    expr.appendChildNode(primNode);
                    expr.appendChildNode(left);
                    Node notNode = new Node(NodeType.OPERATOR, "!");
                    notNode.appendChildNode(expr);
                    stack.push(notNode);
                }
                else if (left.getNodeType() == NodeType.TYPE)
                {
                    // Convert TYPE into "instanceof"
                    Node primNode = left.getFirstChild();
                    Node expr = new Node(NodeType.OPERATOR, "instanceof");
                    expr.appendChildNode(primNode);
                    expr.appendChildNode(right);
                    Node notNode = new Node(NodeType.OPERATOR, "!");
                    notNode.appendChildNode(expr);
                    stack.push(notNode);
                }
                else
                {
                    Node expr = new Node(NodeType.OPERATOR, "!=");
                    expr.insertChildNode(right);
                    expr.insertChildNode(left);
                    stack.push(expr);
                }
            }
            else if (lexer.parseStringIgnoreCase("NOT "))
            {
                if (lexer.parseStringIgnoreCase("BETWEEN "))
                {
                    // {expression} NOT BETWEEN {lower} AND {upper}
                    Node inputNode = stack.pop();
                    processAdditiveExpression();
                    Node lowerNode = stack.pop();
                    if (lexer.parseStringIgnoreCase("AND "))
                    {
                        processAdditiveExpression();
                        Node upperNode = stack.pop();

                        Node leftNode = new Node(NodeType.OPERATOR, "<");
                        leftNode.appendChildNode(inputNode);
                        leftNode.appendChildNode(lowerNode);

                        Node rightNode = new Node(NodeType.OPERATOR, ">");
                        rightNode.appendChildNode(inputNode);
                        rightNode.appendChildNode(upperNode);

                        Node betweenNode = new Node(NodeType.OPERATOR, "||");
                        betweenNode.appendChildNode(leftNode);
                        betweenNode.appendChildNode(rightNode);
                        stack.push(betweenNode);
                    }
                    else
                    {
                        throw new NucleusUserException("Query has BETWEEN keyword with no AND clause");
                    }
                }
                else if (lexer.parseStringIgnoreCase("LIKE "))
                {
                    processLikeExpression();
                    Node notNode = new Node(NodeType.OPERATOR, "!");
                    notNode.insertChildNode(stack.pop());
                    stack.push(notNode);
                }
                else if (lexer.parseStringIgnoreCase("IN"))
                {
                    // {expression} NOT IN (expr1 [,expr2[,expr3]])
                    processInExpression(true);
                }
                else if (lexer.parseStringIgnoreCase("MEMBER "))
                {
                    processMemberExpression(true);
                }
                else
                {
                    throw new NucleusException("Unsupported query syntax NOT followed by unsupported keyword");
                }
            }
            else if (lexer.parseStringIgnoreCase("BETWEEN "))
            {
                // {expression} BETWEEN {lower} AND {upper}
                Node inputNode = stack.pop();
                processAdditiveExpression();
                Node lowerNode = stack.pop();
                if (lexer.parseStringIgnoreCase("AND "))
                {
                    processAdditiveExpression();
                    Node upperNode = stack.pop();
                    Node leftNode = new Node(NodeType.OPERATOR, ">=");
                    leftNode.appendChildNode(inputNode);
                    leftNode.appendChildNode(lowerNode);

                    Node rightNode = new Node(NodeType.OPERATOR, "<=");
                    rightNode.appendChildNode(inputNode);
                    rightNode.appendChildNode(upperNode);

                    Node betweenNode = new Node(NodeType.OPERATOR, "&&");
                    betweenNode.appendChildNode(rightNode);
                    betweenNode.appendChildNode(leftNode);
                    stack.push(betweenNode);
                }
                else
                {
                    throw new NucleusUserException("Query has BETWEEN keyword with no AND clause");
                }
            }
            else if (lexer.parseStringIgnoreCase("LIKE "))
            {
                // {expression} LIKE {pattern_value} [ESCAPE {escape_char}]
                processLikeExpression();
            }
            else if (lexer.parseStringIgnoreCase("IN"))
            {
                // {expression} IN (expr1 [,expr2[,expr3]])
                processInExpression(false);
            }
            else if (lexer.parseStringIgnoreCase("MEMBER "))
            {
                processMemberExpression(false);
            }
            else if (lexer.parseStringIgnoreCase("IS "))
            {
                // {expression} IS [NOT] [NULL | EMPTY]
                Node inputNode = stack.pop();
                Node inputRootNode = inputNode;
                if (inputNode.getNodeType() == NodeType.IDENTIFIER)
                {
                    // Find the end of the identifier chain
                    while (inputNode.getFirstChild() != null)
                    {
                        inputNode = inputNode.getFirstChild();
                    }
                }

                boolean not = false;
                if (lexer.parseStringIgnoreCase("NOT "))
                {
                    not = true;
                }

                if (lexer.parseStringIgnoreCase("NULL"))
                {
                    Node isNode = new Node(NodeType.OPERATOR, not ? "!=" : "==");
                    Node compareNode = new Node(NodeType.LITERAL, null);
                    isNode.insertChildNode(compareNode);
                    isNode.insertChildNode(inputRootNode);
                    stack.push(isNode);
                }
                else if (lexer.parseStringIgnoreCase("EMPTY"))
                {
                    // Convert IS EMPTY to a method call of "size()==0" on collection/map
                    Node sizeNode = new Node(NodeType.INVOKE, "size");
                    inputNode.insertChildNode(sizeNode);
                    Node isEmptyNode = new Node(NodeType.OPERATOR, not ? "!=" : "==");
                    isEmptyNode.appendChildNode(inputNode);
                    Node zeroNode = new Node(NodeType.LITERAL, 0);
                    isEmptyNode.appendChildNode(zeroNode);
                    stack.push(isEmptyNode);
                }
                else
                {
                    throw new NucleusException("Encountered IS " + (not ? "NOT " : " ") + " that should be followed by NULL | EMPTY but isnt");
                }
            }
            else if (lexer.parseString("<="))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "<=");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (lexer.parseString(">="))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, ">=");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (lexer.parseChar('<'))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "<");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (lexer.parseChar('>'))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, ">");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else
            {
                break;
            }
        }
    }

    /**
     * Convenience handler to parse a LIKE expression.
     * Expression is of the form "{expression} LIKE {pattern_value} [ESCAPE {escape_char}]".
     * Requires that the node at the top of the stack is the field expression node.
     * Returns "{primaryNode}.INVOKE("matches", (likeExprNode))"
     */
    private void processLikeExpression()
    {
        Node primaryNode = stack.pop();
        Node primaryRootNode = primaryNode;
        if (primaryNode.getNodeType() == NodeType.IDENTIFIER)
        {
            // Make sure we put the INVOKE at the end of the identifier chain
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }
        }

        processAdditiveExpression();
        Node likeExprNode = stack.pop();

        if (lexer.parseStringIgnoreCase("ESCAPE"))
        {
            // Return matchesNode with 2 property nodes - the pattern expression, and the escape char
            processAdditiveExpression();
            Node escapeNode = stack.pop();
            Node matchesNode = new Node(NodeType.INVOKE, "matches");
            matchesNode.addProperty(likeExprNode);
            matchesNode.addProperty(escapeNode);

            primaryNode.appendChildNode(matchesNode);
            stack.push(primaryRootNode);
        }
        else
        {
            // Return matchesNode with 1 property node - the pattern expression
            Node matchesNode = new Node(NodeType.INVOKE, "matches");
            matchesNode.addProperty(likeExprNode);

            primaryNode.appendChildNode(matchesNode);
            stack.push(primaryRootNode);
        }
    }

    /**
     * Convenience handler to parse an IN expression.
     * Expression is typically of the form "{expression} IN (expr1 [,expr2 [,expr3]] | subquery)".
     * Can generate a node tree like
     * <pre>({expression} == expr1) || ({expression} == expr2) || ({expression} == expr3)</pre> 
     * depending on the precise situation.
     * @param not Whether this is an expression "NOT IN"
     */
    private void processInExpression(boolean not)
    {
        Node inputNode = stack.pop(); // The left hand side expression
        boolean usesType = false;
        if (inputNode.getNodeType() == NodeType.TYPE)
        {
            usesType = true;
        }

        if (!lexer.parseChar('('))
        {
            // "{expr} IN value" : Subquery/Parameter
            processPrimary();
            Node subqueryNode = stack.pop();

            Node inNode;
            if (usesType)
            {
                // "TYPE(p) IN :collectionClasses". Convert TYPE into "(p instanceof :collectionClasses)"
                inNode = new Node(NodeType.OPERATOR, "instanceof");
                inNode.appendChildNode(inputNode.getFirstChild());
                inNode.appendChildNode(subqueryNode);

                if (not)
                {
                    Node notNode = new Node(NodeType.OPERATOR, "!");
                    notNode.appendChildNode(inNode);
                    inNode = notNode;
                }
            }
            else
            {
                inNode = new Node(NodeType.OPERATOR, not ? "NOT IN" : "IN");
                inNode.appendChildNode(inputNode);
                inNode.appendChildNode(subqueryNode);
            }

            stack.push(inNode);
            return;
        }

        List<Node> valueNodes = new ArrayList();
        do
        {
            // "IN ((literal|parameter) [, (literal|parameter)])"
            processPrimary();
            if (stack.peek() == null)
            {
                throw new QueryCompilerSyntaxException("Expected literal|parameter but got " + lexer.remaining(), lexer.getIndex(), lexer.getInput());
            }

            // Generate node for comparison with this value
            Node valueNode = stack.pop();
            valueNodes.add(valueNode);
            lexer.skipWS();

        } while (lexer.parseChar(','));

        if (!lexer.parseChar(')'))
        {
            throw new QueryCompilerSyntaxException("Expected: ')' but got " + lexer.remaining(), lexer.getIndex(), lexer.getInput());
        }
        else if (valueNodes.isEmpty())
        {
            throw new QueryCompilerSyntaxException("IN expression had zero arguments!");
        }

        if (usesType)
        {
            // "TYPE(a) in (b1,b2,b3)". Convert TYPE into "a instanceof (b1,b2,b3)"
            Node inNode = new Node(NodeType.OPERATOR, "instanceof");
            inNode.appendChildNode(inputNode.getFirstChild());
            for (Node valueNode : valueNodes)
            {
                inNode.appendChildNode(valueNode);
            }

            if (not)
            {
                Node notNode = new Node(NodeType.OPERATOR, "!");
                notNode.appendChildNode(inNode);
                inNode = notNode;
            }

            stack.push(inNode);
            return;
        }

        // Create the returned Node representing this IN expression
        Node inNode = null;
        Node firstValueNode = valueNodes.get(0);
        if (valueNodes.size() == 1 && firstValueNode.getNodeType() != NodeType.LITERAL)
        {
            // Compile as (input IN val)
            // Note that we exclude LITERAL single args from here since they can be represented using ==, and also RDBMS needs that currently TODO Fix RDBMS query processor
            inNode = new Node(NodeType.OPERATOR, not ? "NOT IN" : "IN");
            inNode.appendChildNode(inputNode);
            inNode.appendChildNode(valueNodes.get(0));
        }
        else
        {
            // Compile as (input == val1 || input == val2 || input == val3) 
            //      or as (input != val1 && input != val2 && input != val3)
            // TODO Would be nice to do as (input IN (val1, val2, ...)) but that implies changes in datastore query processors
            for (Node valueNode : valueNodes)
            {
                Node compareNode = new Node(NodeType.OPERATOR, not ? "!=" : "==");
                compareNode.appendChildNode(inputNode);
                compareNode.appendChildNode(valueNode);
                if (inNode == null)
                {
                    inNode = compareNode;
                }
                else
                {
                    Node newInNode = new Node(NodeType.OPERATOR, not ? "&&" : "||");
                    newInNode.appendChildNode(inNode);
                    newInNode.appendChildNode(compareNode);
                    inNode = newInNode;
                }
            }
        }

        stack.push(inNode);
    }

    /**
     * Convenience handler to parse an MEMBER expression.
     * Expression is of the form "{expr1} MEMBER [OF] expr2".
     * @param not Whether this is an expression "NOT MEMBER"
     */
    private void processMemberExpression(boolean not)
    {
        Node inputNode = stack.pop(); // The left hand side expression
        lexer.parseStringIgnoreCase("OF"); // Ignore any "OF" keyword here (optional)
        processPrimary(); // Container node at top of stack
        Node containerNode = stack.peek();

        // Make sure we put the INVOKE at the end of the IDENTIFIER chain
        Node lastNode = containerNode;
        while (lastNode.getFirstChild() != null)
        {
            lastNode = lastNode.getFirstChild();
        }

        if (not)
        {
            Node notNode = new Node(NodeType.OPERATOR, "!");
            stack.pop();
            notNode.insertChildNode(containerNode);
            stack.push(notNode);
        }

        // Node (IDENTIFIER, container)
        // ---> Node (INVOKE, "contains")
        //      ---> Node(IDENTIFIER, containsNode)
        Node containsNode = new Node(NodeType.INVOKE, "contains");
        containsNode.addProperty(inputNode);
        lastNode.appendChildNode(containsNode);
    }

    /**
     * Method to parse the query syntax for a CASE expression.
     * Creates a Node of type "CASE" with children in the order 
     * <pre>whenNode, actionNode [, whenNode, actionNode]*, elseNode</pre>
     */
    private void processCaseExpression()
    {
        Node caseNode = new Node(NodeType.CASE);
        boolean simple = true;
        if (lexer.peekStringIgnoreCase("WHEN "))
        {
            simple = false;
        }
        if (simple)
        {
            // Simple CASE "CASE {expr} WHEN {eqExpr} THEN {actionExpr} WHEN {eqExpr} THEN {actionExpr} ELSE {actionExpr} END"
            Node exprNode = processExpression();
            stack.pop();

            while (lexer.parseStringIgnoreCase("WHEN "))
            {
                processExpression();
                Node eqCondNode = stack.pop(); // exprNode == eqCondNode
                Node whenNode = new Node(NodeType.OPERATOR, "==");
                whenNode.insertChildNode(exprNode.clone(null));
                whenNode.insertChildNode(eqCondNode);
                caseNode.appendChildNode(whenNode);

                boolean hasThen = lexer.parseStringIgnoreCase("THEN ");
                if (!hasThen)
                {
                    throw new QueryCompilerSyntaxException("expected 'THEN' as part of CASE", lexer.getIndex(), lexer.getInput());
                }
                processExpression();
                Node actionNode = stack.pop();
                caseNode.appendChildNode(actionNode);
            }
            if (lexer.parseStringIgnoreCase("ELSE "))
            {
                processExpression();
                Node elseNode = stack.pop();
                caseNode.appendChildNode(elseNode);
            }
            if (!lexer.parseStringIgnoreCase("END")) 
            {
                throw new QueryCompilerSyntaxException("expected 'END' as part of CASE", lexer.getIndex(), lexer.getInput());
            }
        }
        else
        {
            // General CASE 
            while (lexer.parseStringIgnoreCase("WHEN "))
            {
                processExpression();
                Node whenNode = stack.pop();
                caseNode.appendChildNode(whenNode);

                boolean hasThen = lexer.parseStringIgnoreCase("THEN ");
                if (!hasThen)
                {
                    throw new QueryCompilerSyntaxException("expected 'THEN' as part of CASE", lexer.getIndex(), lexer.getInput());
                }
                processExpression();
                Node actionNode = stack.pop();
                caseNode.appendChildNode(actionNode);
            }
            if (lexer.parseStringIgnoreCase("ELSE "))
            {
                processExpression();
                Node elseNode = stack.pop();
                caseNode.appendChildNode(elseNode);
            }
            if (!lexer.parseStringIgnoreCase("END")) 
            {
                throw new QueryCompilerSyntaxException("expected 'END' as part of CASE", lexer.getIndex(), lexer.getInput());
            }
        }
        stack.push(caseNode);
    }

    protected void processAdditiveExpression()
    {
        processMultiplicativeExpression();

        for (;;)
        {
            if (lexer.parseChar('+'))
            {
                processMultiplicativeExpression();
                Node expr = new Node(NodeType.OPERATOR, "+");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (lexer.parseChar('-'))
            {
                processMultiplicativeExpression();
                Node expr = new Node(NodeType.OPERATOR, "-");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else
            {
                break;
            }
        }
    }

    protected void processMultiplicativeExpression()
    {
        processUnaryExpression();

        for (;;)
        {
            if (lexer.parseChar('*'))
            {
                processUnaryExpression();
                Node expr = new Node(NodeType.OPERATOR, "*");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (lexer.parseChar('/'))
            {
                processUnaryExpression();
                Node expr = new Node(NodeType.OPERATOR, "/");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (lexer.parseChar('%'))
            {
                processUnaryExpression();
                Node expr = new Node(NodeType.OPERATOR, "%");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else
            {
                break;
            }
        }
    }

    protected void processUnaryExpression()
    {
        if (lexer.parseString("++"))
        {
            throw new NucleusUserException("Unsupported operator '++'");
        }
        else if (lexer.parseString("--"))
        {
            throw new NucleusUserException("Unsupported operator '--'");
        }

        if (lexer.parseChar('+'))
        {
            // Just swallow + and leave remains on the stack
            processUnaryExpression();
        }
        else if (lexer.parseChar('-'))
        {
            processUnaryExpression();
            Node expr = new Node(NodeType.OPERATOR, "-");
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
        else if (lexer.parseStringIgnoreCase("NOT "))
        {
            processRelationalExpression();
            Node expr = new Node(NodeType.OPERATOR, "!");
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
        else
        {
            processPrimary();
        }
    }

    /**
     * Parses the primary. First look for a literal (e.g. "text"), then an identifier(e.g. variable). 
     * In the next step, call a function, if executing a function, on the literal or the identifier found.
     */
    protected void processPrimary()
    {
        String subqueryKeyword = null;
        Node subqueryNode = null;

        // Process all syntax that cannot chain off subfield references from
        if (lexer.parseStringIgnoreCase("SOME "))
        {
            subqueryKeyword = "SOME";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        else if (lexer.parseStringIgnoreCase("ALL "))
        {
            subqueryKeyword = "ALL";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        else if (lexer.parseStringIgnoreCase("ANY "))
        {
            subqueryKeyword = "ANY";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        else if (lexer.parseStringIgnoreCase("EXISTS "))
        {
            subqueryKeyword = "EXISTS";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        if (subqueryKeyword != null && subqueryNode != null)
        {
            Node subNode = new Node(NodeType.SUBQUERY, subqueryKeyword);
            subNode.appendChildNode(subqueryNode);
            stack.push(subNode);
            return;
        }

        if (!strict)
        {
            // Series of user-convenience methods that are not part of strict JPQL
            if (lexer.parseStringIgnoreCase("COUNT(*)"))
            {
                // Convert to a method call of COUNTSTAR
                Node node = new Node(NodeType.INVOKE, "COUNTSTAR");
                stack.push(node);
                return;
            }
            else if (lexer.parseStringIgnoreCase("CURRENT_DATE()")) // Some people put "()" in JPQL
            {
                // Convert to a method call of CURRENT_DATE
                Node node = new Node(NodeType.INVOKE, "CURRENT_DATE");
                stack.push(node);
                return;
            }
            else if (lexer.parseStringIgnoreCase("CURRENT_TIMESTAMP()")) // Some people put "()" in JPQL
            {
                // Convert to a method call
                Node node = new Node(NodeType.INVOKE, "CURRENT_TIMESTAMP");
                stack.push(node);
                return;
            }
            else if (lexer.parseStringIgnoreCase("CURRENT_TIME()")) // Some people put "()" in JPQL
            {
                // Convert to a method call
                Node node = new Node(NodeType.INVOKE, "CURRENT_TIME");
                stack.push(node);
                return;
            }
        }

        if (lexer.parseStringIgnoreCase("CURRENT_DATE"))
        {
            // Convert to a method call
            Node node = new Node(NodeType.INVOKE, "CURRENT_DATE");
            stack.push(node);
            return;
        }
        else if (lexer.parseStringIgnoreCase("CURRENT_TIMESTAMP"))
        {
            // Convert to a method call
            Node node = new Node(NodeType.INVOKE, "CURRENT_TIMESTAMP");
            stack.push(node);
            return;
        }
        else if (lexer.parseStringIgnoreCase("CURRENT_TIME"))
        {
            // Convert to a method call
            Node node = new Node(NodeType.INVOKE, "CURRENT_TIME");
            stack.push(node);
            return;
        }
        else if (lexer.parseStringIgnoreCase("CASE "))
        {
            processCaseExpression();
            return;
        }
        else if (lexer.parseStringIgnoreCase("DISTINCT "))
        {
            // Aggregates can have "count(DISTINCT field1)"
            Node distinctNode = new Node(NodeType.OPERATOR, "DISTINCT");
            processExpression();
            Node identifierNode = stack.pop();
            distinctNode.appendChildNode(identifierNode);
            stack.push(distinctNode);
            return;
        }
        else if (lexer.peekStringIgnoreCase("TREAT("))
        {
            // So we don't get interpreted as a method. Processed later
        }
        else if (processKey(false)) // TODO Can we chain fields/methods off a KEY ?
        {
            return;
        }
        else if (processValue(false)) // TODO Can we chain fields/methods off a VALUE ?
        {
            return;
        }
        else if (processEntry())
        {
            return;
        }
        else if (processCreator() || processLiteral() || processMethod())
        {
            return;
        }

        int sizeBeforeBraceProcessing = stack.size();
        boolean braceProcessing = false;
        if (lexer.parseChar('('))
        {
            // "({expr1})"
            processExpression();
            if (!lexer.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("expected ')'", lexer.getIndex(), lexer.getInput());
            }

            if (!lexer.parseChar('.'))
            {
                // TODO If we have a cast, then apply to the current node (with the brackets)
                return;
            }

            // Has follow on expression "({expr1}).{expr2}" so continue
            braceProcessing = true;
        }

        // We will have an identifier (variable, parameter, or field of candidate class)
        if (processTreat())
        {
        }
        else if (processMethod())
        {
        }
        else if (processIdentifier())
        {
        }
        else
        {
            throw new QueryCompilerSyntaxException("Method/Identifier expected", lexer.getIndex(), lexer.getInput());
        }

        // Save the stack size just before this component for use later in squashing all nodes
        // down to a single Node in the stack with all others chained off from it
        int size = stack.size();
        if (braceProcessing)
        {
            size = sizeBeforeBraceProcessing+1;
        }

        // Generate Node tree, including chained operations
        // e.g identifier.methodX().methodY().methodZ() 
        //     -> node (IDENTIFIER) with child (INVOKE), with child (INVOKE), with child (INVOKE)
        // e.g identifier.fieldX.fieldY.fieldZ
        //     -> node (IDENTIFIER) with child (IDENTIFIER), with child (IDENTIFIER), with child (IDENTIFIER)
        while (lexer.parseChar('.'))
        {
            if (processMethod())
            {
                // "a.method()" Note this is invalid in JPQL, where they have FUNCTIONs, but we allow it
            }
            else if (processIdentifier())
            {
                // "a.field"
            }
            else
            {
                throw new QueryCompilerSyntaxException("Identifier expected", lexer.getIndex(), lexer.getInput());
            }
        }

        // For all added nodes, step back and chain them so we have
        // Node[IDENTIFIER, a]
        // +--- Node[IDENTIFIER, b]
        //      +--- Node[IDENTIFIER, c]
        while (stack.size() > size) // Nodes added
        {
            Node top = stack.pop();
            Node peek = stack.peek();
            Node lastDescendant = getLastDescendantNodeForNode(peek);
            if (lastDescendant != null)
            {
                lastDescendant.appendChildNode(top);
            }
            else
            {
                // The peek node has multiple children, so cannot just put the top Node after the last child
                Node primNode = new Node(NodeType.PRIMARY);
                primNode.appendChildNode(peek);
                primNode.appendChildNode(top);

                // Remove "peek" node and replace with primNode
                stack.pop();
                stack.push(primNode);
            }
        }
    }

    /**
     * Method to parse "new a.b.c(param1[,param2], ...)" and create a Node of type CREATOR.
     * The Node at the top of the stack after this call will have any arguments defined in its "properties".
     * @return whether method syntax was found.
     */
    private boolean processCreator()
    {
        if (lexer.parseStringIgnoreCase("NEW "))
        {
            // "new MyClass(arg1, arg2)"
            int size = stack.size();
            if (!processMethod())
            {
                if (!processIdentifier())
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", lexer.getIndex(), lexer.getInput());
                }

                // run function on literals or identifiers e.g. "primary.runMethod(arg)"
                while (lexer.parseChar('.'))
                {
                    if (processMethod())
                    {
                        // "a.method(...)"
                    }
                    else if (processIdentifier())
                    {
                        // "a.field"
                    }
                    else
                    {
                        throw new QueryCompilerSyntaxException("Identifier expected", lexer.getIndex(), lexer.getInput());
                    }
                }
            }
            while (stack.size() - 1 > size)
            {
                Node top =  stack.pop();
                Node peek = stack.peek();
                peek.insertChildNode(top);
            }
            Node node = stack.pop();
            Node newNode = new Node(NodeType.CREATOR);
            newNode.insertChildNode(node);
            stack.push(newNode);
            return true;
        }
        return false;
    }

    /**
     * Process an ENTRY construct. Puts the ENTRY Node on the stack.
     * @return Whether a ENTRY construct was found by the lexer.
     */
    protected boolean processEntry()
    {
        if (lexer.parseString("ENTRY"))
        {
            // ENTRY(identification_variable)
            // Convert to be {primary}.INVOKE(mapEntry)
            lexer.skipWS();
            lexer.parseChar('(');
            Node invokeNode = new Node(NodeType.INVOKE, "mapEntry");
            processExpression();
            if (!lexer.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
            }

            Node primaryNode = stack.pop(); // Could check type ? (Map)
            Node primaryRootNode = primaryNode;
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }
            primaryNode.appendChildNode(invokeNode);
            stack.push(primaryRootNode);
            return true;
        }
        return false;
    }

    /**
     * Process for a KEY construct. Puts the KEY Node on the stack if one is found.
     * @param fromClause Whether this is a FROM clause
     * @return Whether a KEY construct was found by the lexer.
     */
    protected boolean processKey(boolean fromClause)
    {
        if (lexer.parseString("KEY"))
        {
            // KEY(identification_variable)
            lexer.skipWS();
            lexer.parseChar('(');
            processExpression();
            if (!lexer.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
            }

            Node primaryNode = stack.pop();
            Node primaryRootNode = primaryNode;
            Node lastNode = primaryNode;
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }

            if (fromClause)
            {
                // Convert to be {primary#KEY}
                // Handle as a primary but with #KEY appended to the last primary
                Object keyValue = primaryNode.getNodeValue();
                primaryNode.setNodeValue(keyValue + "#KEY");
            }
            else
            {
                // Convert to be {primary}.INVOKE(mapKey)
                Node invokeNode = new Node(NodeType.INVOKE, "mapKey");
                primaryNode.appendChildNode(invokeNode);
                lastNode = invokeNode;
            }
            stack.push(primaryRootNode);

            // Allow referral to chain of field(s) of key i.e "KEY(...).field1.field2" etc
            int size = stack.size();
            while (lexer.parseChar('.'))
            {
                if (processIdentifier())
                {
                    // "a.field"
                }
                else
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", lexer.getIndex(), lexer.getInput());
                }
            }

            // For all added nodes, step back and chain them so we have
            // Node[IDENTIFIER, a]
            // +--- Node[IDENTIFIER, b]
            //      +--- Node[IDENTIFIER, c]
            if (size != stack.size())
            {
                while (stack.size() > size)
                {
                    Node top = stack.pop();
                    lastNode.insertChildNode(top);
                    lastNode = top;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Process for a VALUE construct. Puts the VALUE Node on the stack if one is found.
     * @param fromClause Whether this is a FROM clause
     * @return Whether a VALUE construct was found by the lexer.
     */
    protected boolean processValue(boolean fromClause)
    {
        if (lexer.parseString("VALUE"))
        {
            // VALUE(identification_variable)
            // Convert to be {primary}.INVOKE(mapValue)
            lexer.skipWS();
            lexer.parseChar('(');
            processExpression();
            if (!lexer.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
            }

            Node primaryNode = stack.pop(); // Could check type ? (Map)
            Node primaryRootNode = primaryNode;
            Node lastNode = primaryNode;
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }

            if (fromClause)
            {
                // Convert to be {primary#VALUE}
                // Handle as a primary but with #VALUE appended to the last primary
                Object keyValue = primaryNode.getNodeValue();
                primaryNode.setNodeValue(keyValue + "#VALUE");
            }
            else
            {
                // Convert to be {primary}.INVOKE(mapValue)
                Node invokeNode = new Node(NodeType.INVOKE, "mapValue");
                primaryNode.appendChildNode(invokeNode);
                lastNode = invokeNode;
            }
            stack.push(primaryRootNode);

            // Allow referral to chain of field(s) of key i.e "VALUE(...).field1.field2" etc
            int size = stack.size();
            while (lexer.parseChar('.'))
            {
                if (processIdentifier())
                {
                    // "a.field"
                }
                else
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", lexer.getIndex(), lexer.getInput());
                }
            }

            // For all added nodes, step back and chain them so we have
            // Node[IDENTIFIER, a]
            // +--- Node[IDENTIFIER, b]
            //      +--- Node[IDENTIFIER, c]
            if (size != stack.size())
            {
                while (stack.size() > size)
                {
                    Node top = stack.pop();
                    lastNode.insertChildNode(top);
                    lastNode = top;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Method to parse "methodName(param1[,param2], ...)" and create a Node of type INVOKE.
     * The Node at the top of the stack after this call will have any arguments defined in its "properties".
     * @return whether method syntax was found.
     */
    private boolean processMethod()
    {
        String method = lexer.parseMethod();
        if (method != null)
        {
            lexer.skipWS();
            lexer.parseChar('(');

            // Use uppercase forms of aggregate methods in generic compilation
            if (method.equalsIgnoreCase("COUNT"))
            {
                method = "COUNT";
            }
            else if (method.equalsIgnoreCase("AVG"))
            {
                method = "AVG";
            }
            else if (method.equalsIgnoreCase("MIN"))
            {
                method = "MIN";
            }
            else if (method.equalsIgnoreCase("MAX"))
            {
                method = "MAX";
            }
            else if (method.equalsIgnoreCase("SUM"))
            {
                method = "SUM";
            }
            else if (method.equalsIgnoreCase("ABS"))
            {
                method = "ABS";
            }
            else if (method.equalsIgnoreCase("INDEX"))
            {
                method = "INDEX";
            }
            else if (method.equalsIgnoreCase("FUNCTION"))
            {
                // JPA 2.1 FUNCTION for invoking SQL functions
                method = "FUNCTION";
            }

            if (method.equalsIgnoreCase("Object"))
            {
                // "Object(p)", so interpret as "p"
                processExpression(); // identifier at top of stack
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }
                return true;
            }
            else if (method.equalsIgnoreCase("MOD"))
            {
                // Convert to be {first} % {second}
                Node modNode = new Node(NodeType.OPERATOR, "%");
                processExpression(); // argument 1
                Node firstNode = stack.pop();
                if (!lexer.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }
                processExpression(); // argument 2
                Node secondNode = stack.pop();
                modNode.appendChildNode(firstNode);
                modNode.appendChildNode(secondNode);
                stack.push(modNode);
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }
                return true;
            }
            else if (method.equalsIgnoreCase("TYPE"))
            {
                // Convert to a TYPE node with the primary as a child node
                Node typeNode = new Node(NodeType.TYPE);
                processExpression(); // argument
                Node typePrimaryNode = stack.pop();
                typeNode.appendChildNode(typePrimaryNode);
                stack.push(typeNode);
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }
                return true;
            }
            else if (method.equalsIgnoreCase("SUBSTRING"))
            {
                // SUBSTRING(string_primary, simple_arithmetic_expression[, simple_arithmetic_expression])
                // Convert to be {primary}.INVOKE(substring, {arg1[, arg2]})
                Node invokeNode = new Node(NodeType.INVOKE, "substring");
                processExpression();
                Node primaryNode = stack.pop();
                if (!lexer.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }

                // First arg to substring(...) has origin 0, but JPQL has origin 1!
                processExpression();
                Node arg1 = stack.pop();
                Node oneNode = new Node(NodeType.LITERAL, 1);
                Node arg1Node = new Node(NodeType.OPERATOR, "-");
                arg1Node.insertChildNode(arg1);
                arg1Node.appendChildNode(oneNode);

                if (lexer.parseChar(','))
                {
                    // String.substring(arg1, arg2)
                    // Second arg to substring(...) has origin 0, but in JPQL is length of result!
                    processExpression();
                    Node arg2 = stack.pop();
                    Node arg2Node = new Node(NodeType.OPERATOR, "+");
                    arg2Node.appendChildNode(arg2);
                    arg2Node.appendChildNode(arg1Node);
                    if (!lexer.parseChar(')'))
                    {
                        throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                    }

                    primaryNode.appendChildNode(invokeNode);
                    invokeNode.addProperty(arg1Node);
                    invokeNode.addProperty(arg2Node);
                    stack.push(primaryNode);
                    return true;
                }
                else if (lexer.parseChar(')'))
                {
                    // String.substring(arg1)
                    primaryNode.appendChildNode(invokeNode);
                    invokeNode.addProperty(arg1Node);
                    stack.push(primaryNode);
                    return true;
                }
                else
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }
            }
            else if (method.equalsIgnoreCase("UPPER"))
            {
                // UPPER(string_primary)
                // Convert to be {primary}.INVOKE(toUpper)
                Node invokeNode = new Node(NodeType.INVOKE, "toUpperCase");
                processExpression();
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }

                Node primaryNode = stack.pop();
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("LOWER"))
            {
                // UPPER(string_primary)
                // Convert to be {primary}.INVOKE(toLower)
                Node invokeNode = new Node(NodeType.INVOKE, "toLowerCase");
                processExpression();
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }

                Node primaryNode = stack.pop();
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("LENGTH"))
            {
                // LENGTH(string_primary)
                // Convert to be {primary}.INVOKE(length)
                Node invokeNode = new Node(NodeType.INVOKE, "length");
                processExpression();
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }

                Node primaryNode = stack.pop();
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("CONCAT"))
            {
                // CONCAT(string_primary, string_primary[, string_primary])
                // Convert to be {primary1}+{primary2}[+primary3]
                processExpression();
                Node prevNode = stack.pop();

                while (true)
                {
                    if (!lexer.parseChar(','))
                    {
                        throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                    }

                    processExpression();
                    Node thisNode = stack.pop();

                    Node currentNode = new Node(NodeType.OPERATOR, "+");
                    currentNode.appendChildNode(prevNode);
                    currentNode.appendChildNode(thisNode);
                    if (lexer.parseChar(')'))
                    {
                        stack.push(currentNode);
                        return true;
                    }
                    prevNode = currentNode;
                    currentNode = null;
                }
            }
            else if (method.equalsIgnoreCase("LOCATE"))
            {
                // LOCATE(string_primary, string_primary[, simple_arithmetic_expression])
                // Convert to ({stringExpr}.indexOf(strExpr[, posExpr]) + 1)
                processExpression();
                Node searchNode = stack.pop();
                Node invokeNode = new Node(NodeType.INVOKE, "indexOf");
                invokeNode.addProperty(searchNode);
                if (!lexer.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }

                processExpression();
                Node primaryNode = stack.pop();
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);

                Node oneNode = new Node(NodeType.LITERAL, 1);
                if (lexer.parseChar(','))
                {
                    processExpression();
                    Node fromPosNode = stack.pop();
                    Node positionNode = new Node(NodeType.OPERATOR, "-");
                    positionNode.appendChildNode(fromPosNode);
                    positionNode.appendChildNode(oneNode);
                    invokeNode.addProperty(positionNode);
                }
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }

                Node locateNode = new Node(NodeType.OPERATOR, "+");
                locateNode.appendChildNode(primaryRootNode);
                locateNode.appendChildNode(oneNode);
                stack.push(locateNode);
                return true;
            }
            else if (method.equalsIgnoreCase("TRIM"))
            {
                // TRIM([[LEADING | TRAILING | BOTH] [trim_character] FROM] string_primary)
                // Convert to be {primary}.INVOKE(trim|trimLeft|trimRight, [{trimChar}])
                String methodName = "trim";
                if (lexer.parseStringIgnoreCase("LEADING"))
                {
                    methodName = "trimLeft";
                }
                else if (lexer.parseStringIgnoreCase("TRAILING"))
                {
                    methodName = "trimRight";
                }
                else if (lexer.parseStringIgnoreCase("BOTH"))
                {
                    // Default
                }
                Node invokeNode = new Node(NodeType.INVOKE, methodName);

                processExpression();
                Node next = stack.pop();

                if (lexer.parseChar(')'))
                {
                    // TRIM(string_primary)
                    // Find the last part of the identifier node and append the invoke
                    Node primaryNode = next;
                    while (primaryNode.getFirstChild() != null)
                    {
                        primaryNode = primaryNode.getFirstChild();
                    }
                    primaryNode.appendChildNode(invokeNode);

                    stack.push(next);
                    return true;
                }

                if (next.getNodeType() == NodeType.LITERAL)
                {
                    // TRIM(dir trimChar FROM string_primary)
                    Node trimCharNode = next;
                    if (lexer.parseStringIgnoreCase("FROM "))
                    {
                        // Ignore the FROM
                    }
                    processExpression();
                    next = stack.pop();

                    if (trimCharNode != null)
                    {
                        // Append the trim character to the invoke node
                        invokeNode.addProperty(trimCharNode);
                    }
                }
                else if (next.getNodeType() == NodeType.IDENTIFIER)
                {
                    // TRIM(dir FROM string_primary)
                    Object litValue = next.getNodeValue();
                    if (litValue instanceof String && ((String)litValue).equals("FROM"))
                    {
                        // FROM so ignore
                        processExpression(); // field expression that we are trimming
                        next = stack.pop();
                    }
                    else
                    {
                        throw new QueryCompilerSyntaxException("Unexpected expression", lexer.getIndex(), lexer.getInput());
                    }
                }
                else
                {
                    // No "trimChar" or FROM, so "next" is the string expression node
                }

                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }

                // Find the last part of the identifier node and append the invoke
                Node primaryNode = next;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);

                stack.push(next);
                return true;
            }
            else if (method.equalsIgnoreCase("SIZE"))
            {
                // SIZE(collection_valued_path_expression)
                // Convert to be {primary}.INVOKE(size)
                Node invokeNode = new Node(NodeType.INVOKE, "size");
                processExpression();
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", lexer.getIndex(), lexer.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Collection/Map/array)
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("FUNCTION"))
            {
                // FUNCTION - Convert to be {primary}.INVOKE("SQL_function", ...)
                // Extract sql function name
                processExpression();
                Node sqlFunctionNode = stack.pop();
                Node invokeNode = new Node(NodeType.INVOKE, "SQL_function");
                invokeNode.addProperty(sqlFunctionNode);
                if (lexer.parseChar(','))
                {
                    // Process arguments for function "aaa[,bbb[,ccc]] etc )"
                    do
                    {
                        // Argument for the method call, add as a node property
                        processExpression();
                        invokeNode.addProperty(stack.pop());
                    }
                    while (lexer.parseChar(','));
                }
                if (!lexer.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                }

                stack.push(invokeNode);
                return true;
            }
            else
            {
                // Found syntax for a method, so invoke the method
                // TODO What if the method is not supported for JPQL?
                Node node = new Node(NodeType.INVOKE, method);
                if (!lexer.parseChar(')'))
                {
                    do
                    {
                        // Argument for the method call, add as a node property
                        processExpression();
                        node.addProperty(stack.pop());
                    }
                    while (lexer.parseChar(','));

                    if (!lexer.parseChar(')'))
                    {
                        throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
                    }
                }
                stack.push(node);
                return true;
            }
        }
        return false;
    }

    /**
     * Process a TREAT construct, and put the node on the stack.
     * @return Whether TREAT was found by the lexer.
     */
    protected boolean processTreat()
    {
        if (lexer.parseString("TREAT("))
        {
            // "TREAT(p AS Employee)" will create a Node tree as
            // [IDENTIFIER : p.
            //     [CAST : Employee]]
            // with the "p" node on the stack.
            processExpression();
            Node identifierNode = stack.pop();

            String typeName = lexer.parseIdentifier();
            if (typeName != null && typeName.equalsIgnoreCase("AS"))
            {
                processExpression();
                Node typeNode = stack.pop();
                typeName = typeNode.getNodeChildId();
            }
            else
            {
                throw new QueryCompilerSyntaxException("TREAT should always be structured as 'TREAT(id AS typeName)'");
            }

            Node castNode = new Node(NodeType.CAST, typeName);
            Node endNode = getLastDescendantNodeForNode(identifierNode);
            castNode.setParent(endNode);
            endNode.appendChildNode(castNode);
            if (!lexer.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("')' expected", lexer.getIndex(), lexer.getInput());
            }

            stack.push(identifierNode);
            return true;
        }
        return false;
    }

    /**
     * A literal is one value of any type.
     * Supported literals are of types String, Floating Point, Integer, Character, Boolean and null e.g. 'J', "String", 1, 1.8, true, false, null.
     * Also supports JDBC "escape syntax" for literals.
     * @return The compiled literal
     */
    protected boolean processLiteral()
    {
        if (lexer.parseChar('{'))
        {
            // JDBC escape syntax
            // {d '...'}
            // {ts '...'}
            // {t '...'}
            StringBuilder jdbcLiteralStr = new StringBuilder("{");
            if (lexer.parseChar('d'))
            {
                jdbcLiteralStr.append("d ");
            }
            else if (lexer.parseString("ts"))
            {
                jdbcLiteralStr.append("ts ");
            }
            else if (lexer.parseChar('t'))
            {
                jdbcLiteralStr.append("t ");
            }
            else
            {
                throw new QueryCompilerSyntaxException("d, ts or t expected after { (JDBC escape syntax)", lexer.getIndex(), lexer.getInput());
            }

            if (lexer.nextIsSingleQuote())
            {
                String datetimeLit = lexer.parseStringLiteral();
                jdbcLiteralStr.append("'").append(datetimeLit).append("'");
                if (lexer.parseChar('}'))
                {
                    jdbcLiteralStr.append('}');
                    stack.push(new Node(NodeType.LITERAL, jdbcLiteralStr.toString()));
                    return true;
                }

                throw new QueryCompilerSyntaxException("} expected in JDBC escape syntax", lexer.getIndex(), lexer.getInput());
            }
            throw new QueryCompilerSyntaxException("'...' expected in JDBC escape syntax", lexer.getIndex(), lexer.getInput());
        }

        Object litValue = null;
        String sLiteral;
        BigDecimal fLiteral;
        BigInteger iLiteral;
        Boolean bLiteral;
        boolean single_quote_next = lexer.nextIsSingleQuote();
        if ((sLiteral = lexer.parseStringLiteral()) != null)
        {
            // Both String and Character are allowed to use single-quotes so we need to check if it was single-quoted and use CharacterLiteral if length is 1.
            if (sLiteral.length() == 1 && single_quote_next)
            {
                litValue = Character.valueOf(sLiteral.charAt(0));
            }
            else
            {
                litValue = sLiteral;
            }
        }
        else if ((fLiteral = lexer.parseFloatingPointLiteral()) != null)
        {
            litValue = fLiteral;
        }
        else if ((iLiteral = lexer.parseIntegerLiteral()) != null)
        {
            // Represent as BigInteger or Long depending on length
            String longStr = "" + iLiteral.longValue();
            if (longStr.length() < iLiteral.toString().length())
            {
                litValue = iLiteral;
            }
            else
            {
                litValue = iLiteral.longValue();
            }
        }
        else if ((bLiteral = lexer.parseBooleanLiteralIgnoreCase()) != null)
        {
            litValue = bLiteral;
        }
        else if (lexer.parseNullLiteralIgnoreCase())
        {
        }
        else
        {
            return false;
        }

        stack.push(new Node(NodeType.LITERAL, litValue));
        return true;
    }

    int parameterPosition = 0;

    /**
     * An identifier always designates a reference to a single value.
     * A single value can be one collection, one field.
     * @return The compiled identifier
     */
    private boolean processIdentifier()
    {
        String id = lexer.parseIdentifier();
        if (id == null || id.length() == 0)
        {
            return false;
        }
        char first = id.charAt(0);
        if (first == '?')
        {
            // Numbered parameter - but we treat as "named" by storing as String
            String paramName = id.substring(1);
            Node node = new ParameterNode(NodeType.PARAMETER, paramName, parameterPosition);
            parameterPosition++;
            stack.push(node);
            return true;
        }
        else if (first == ':')
        {
            // Named parameter - stored as String
            Node node = new ParameterNode(NodeType.PARAMETER, id.substring(1), parameterPosition);
            parameterPosition++;
            stack.push(node);
            return true;
        }
        else
        {
            Node node = new Node(NodeType.IDENTIFIER, id);
            stack.push(node);
            return true;
        }
    }
}