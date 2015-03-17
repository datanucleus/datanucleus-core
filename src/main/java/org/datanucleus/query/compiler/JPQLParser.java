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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Deque;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.QueryCompilerSyntaxException;

/**
 * Implementation of a parser for JPQL query language.
 * Generates Node tree(s) by use of the various parseXXX() methods.
 */
public class JPQLParser implements Parser
{
    private Lexer p;
    private Deque<Node> stack = new ArrayDeque<Node>();

    /** Characters that parameters can be prefixed by. */
    private static String paramPrefixes = ":?";

    private Map parameterValues;

    /**
     * Constructor for a JPQL Parser.
     * @param options parser options
     * @param params Map of parameter values keyed by name/number
     */
    public JPQLParser(Map options, Map params)
    {
        parameterValues = params;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parse(java.lang.String)
     */
    public Node parse(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        Node result = processExpression();

        if (p.ci.getIndex() != p.ci.getEndIndex())
        {
            // Error occurred in the JDOQL processing due to syntax error(s)
            String unparsed = p.getInput().substring(p.ci.getIndex());
            throw new QueryCompilerSyntaxException("Portion of expression could not be parsed: " + unparsed);
        }
        return result;
    }
    
    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseVariable(java.lang.String)
     */
    public Node parseVariable(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        if (!processIdentifier())
        {
            throw new QueryCompilerSyntaxException("expected identifier", p.getIndex(), p.getInput());
        }
        if (!processIdentifier())
        {
            throw new QueryCompilerSyntaxException("expected identifier", p.getIndex(), p.getInput());
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
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        return processFromExpression();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseUpdate(java.lang.String)
     */
    public Node[] parseUpdate(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        return parseTupple(expression);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseOrder(java.lang.String)
     */
    public Node[] parseOrder(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        return processOrderExpression();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseResult(java.lang.String)
     */
    public Node[] parseResult(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node node = stack.pop();

            String alias = p.parseIdentifier();
            if (alias != null && alias.equalsIgnoreCase("AS"))
            {
                alias = p.parseIdentifier();
            }
            if (alias != null)
            {
                Node aliasNode = new Node(NodeType.NAME, alias.toLowerCase()); // Save all aliases in lowercase
                node.appendChildNode(aliasNode);
            }

            nodes.add(node);
        }
        while (p.parseString(","));
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseTupple(java.lang.String)
     */
    public Node[] parseTupple(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        stack = new ArrayDeque<Node>();
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node node = stack.pop();
            nodes.add(node);
        }
        while (p.parseString(","));
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseVariables(java.lang.String)
     */
    public Node[][] parseVariables(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        List nodes = new ArrayList();
        do
        {
            processPrimary();
            if (stack.isEmpty())
            {
                throw new QueryCompilerSyntaxException("expected identifier", p.getIndex(), p.getInput());
            }
            if (!processIdentifier())
            {
                throw new QueryCompilerSyntaxException("expected identifier", p.getIndex(), p.getInput());
            }
            Node nodeVariable = stack.pop();
            Node nodeType = stack.pop();
            nodes.add(new Node[]{nodeType, nodeVariable});
        }
        while (p.parseString(";"));
        return (Node[][]) nodes.toArray(new Node[nodes.size()][2]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseParameters(java.lang.String)
     */
    public Node[][] parseParameters(String expression)
    {
        p = new Lexer(expression, paramPrefixes, false);
        List nodes = new ArrayList();
        do
        {
            processPrimary();
            if (stack.isEmpty())
            {
                throw new QueryCompilerSyntaxException("expected identifier", p.getIndex(), p.getInput());
            }
            if (!processIdentifier())
            {
                throw new QueryCompilerSyntaxException("expected identifier", p.getIndex(), p.getInput());
            }
            Node nodeVariable = stack.pop();
            Node nodeType = stack.pop();
            nodes.add(new Node[]{nodeType, nodeVariable});
        }
        while (p.parseString(","));
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
            if (p.peekStringIgnoreCase("IN(") || p.peekStringIgnoreCase("IN "))
            {
                p.parseStringIgnoreCase("IN");
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
                if (!p.parseChar('('))
                {
                    throw new QueryCompilerSyntaxException("Expected: '(' but got " + p.remaining(), 
                        p.getIndex(), p.getInput());
                }

                // Find what we are joining to
                String name = p.parseIdentifier();
                Node joinedNode = new Node(NodeType.IDENTIFIER, name);
                Node parentNode = joinedNode;
                while (p.nextIsDot())
                {
                    p.parseChar('.');
                    String subName = p.parseIdentifier();
                    Node subNode = new Node(NodeType.IDENTIFIER, subName);
                    parentNode.appendChildNode(subNode);
                    parentNode = subNode;
                }

                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("Expected: ')' but got " + p.remaining(), 
                        p.getIndex(), p.getInput());
                }
                p.parseStringIgnoreCase("AS"); // Optional
                String alias = p.parseIdentifier();

                // Create candidate class node with alias, and put at top of stack
                Node classNode = new Node(NodeType.CLASS, candidateClassName);
                Node classAliasNode = new Node(NodeType.NAME, candidateAlias);
                classNode.insertChildNode(classAliasNode);
                stack.push(classNode);

                // Translate the "IN(...) alias" into the equivalent JOIN syntax nodes
                Node joinNode = new Node(NodeType.OPERATOR, "JOIN_INNER");
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
                while (id.getChildNodes().size() > 0)
                {
                    id = id.getFirstChild();
                    className.append(".").append(id.getNodeValue().toString());
                }

                String alias = p.parseIdentifier();
                if (alias != null && alias.equalsIgnoreCase("AS"))
                {
                    alias = p.parseIdentifier();
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
        while (p.parseString(","));

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
            // Check for JOIN syntax "[LEFT [OUTER] | INNER] JOIN ... [ON {cond_expr}]"  (EJB3 syntax)
            boolean leftJoin = false;
            boolean innerJoin = false;
            if (p.parseStringIgnoreCase("INNER "))
            {
                innerJoin = true;
            }
            else if (p.parseStringIgnoreCase("LEFT "))
            {
                //optional and useless (for parser) outer keyword
                p.parseStringIgnoreCase("OUTER");
                leftJoin = true;
            }

            if (p.parseStringIgnoreCase("JOIN "))
            {
                if (!innerJoin && !leftJoin)
                {
                    innerJoin = true;
                }
                // Process the join
                boolean fetch = false;
                if (p.parseStringIgnoreCase("FETCH"))
                {
                    fetch = true;
                }

                // Find what we are joining to
                String id = p.parseIdentifier();
                Node joinedNode = new Node(NodeType.IDENTIFIER, id);
                Node parentNode = joinedNode;
                while (p.nextIsDot())
                {
                    p.parseChar('.');
                    Node subNode = new Node(NodeType.IDENTIFIER, p.parseName());
                    parentNode.appendChildNode(subNode);
                    parentNode = subNode;
                }

                // And the alias we know this joined field by
                p.parseStringIgnoreCase("AS "); // Optional
                String alias = p.parseName();

                Node onNode = null;
                if (p.parseStringIgnoreCase("ON "))
                {
                    // JPA2.1 : Process "ON {cond_expr}"
                    processExpression();
                    onNode = stack.pop();
                }

                String joinType = "JOIN_INNER";
                if (innerJoin)
                {
                    joinType = (fetch ? "JOIN_INNER_FETCH" : "JOIN_INNER");
                }
                else if (leftJoin)
                {
                    joinType = (fetch ? "JOIN_OUTER_FETCH" : "JOIN_OUTER");
                }
                Node joinNode = new Node(NodeType.OPERATOR, joinType);
                joinNode.appendChildNode(joinedNode);
                Node joinedAliasNode = new Node(NodeType.NAME, alias);
                joinNode.appendChildNode(joinedAliasNode);
                candidateNode.appendChildNode(joinNode);
                if (onNode != null)
                {
                    joinNode.appendChildNode(onNode);
                }
            }
            else
            {
                if (innerJoin || leftJoin)
                {
                    throw new NucleusUserException("Expected JOIN after INNER/LEFT keyword at"+p.remaining());
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
            if (p.parseStringIgnoreCase("asc"))
            {
                directionNode = new Node(NodeType.OPERATOR, "ascending");
            }
            else if (p.parseStringIgnoreCase("desc"))
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
            if (p.parseString("NULLS FIRST") || p.parseString("nulls first"))
            {
                nullsNode = new Node(NodeType.OPERATOR, "nulls first");
            }
            else if (p.parseString("NULLS LAST") || p.parseString("nulls last"))
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
        while (p.parseString(","));
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

        while (p.parseStringIgnoreCase("OR "))
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

        while (p.parseStringIgnoreCase("AND "))
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
            if (p.parseString("="))
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
            else if (p.parseString("<>"))
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
            else if (p.parseStringIgnoreCase("NOT "))
            {
                if (p.parseStringIgnoreCase("BETWEEN "))
                {
                    // {expression} NOT BETWEEN {lower} AND {upper}
                    Node inputNode = stack.pop();
                    processAdditiveExpression();
                    Node lowerNode = stack.pop();
                    if (p.parseStringIgnoreCase("AND "))
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
                else if (p.parseStringIgnoreCase("LIKE "))
                {
                    processLikeExpression();
                    Node notNode = new Node(NodeType.OPERATOR, "!");
                    notNode.insertChildNode(stack.pop());
                    stack.push(notNode);
                }
                else if (p.parseStringIgnoreCase("IN"))
                {
                    // {expression} NOT IN (expr1 [,expr2[,expr3]])
                    processInExpression(true);
                }
                else if (p.parseStringIgnoreCase("MEMBER "))
                {
                    processMemberExpression(true);
                }
                else
                {
                    throw new NucleusException("Unsupported query syntax NOT followed by unsupported keyword");
                }
            }
            else if (p.parseStringIgnoreCase("BETWEEN "))
            {
                // {expression} BETWEEN {lower} AND {upper}
                Node inputNode = stack.pop();
                processAdditiveExpression();
                Node lowerNode = stack.pop();
                if (p.parseStringIgnoreCase("AND "))
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
            else if (p.parseStringIgnoreCase("LIKE "))
            {
                // {expression} LIKE {pattern_value} [ESCAPE {escape_char}]
                processLikeExpression();
            }
            else if (p.parseStringIgnoreCase("IN"))
            {
                // {expression} IN (expr1 [,expr2[,expr3]])
                processInExpression(false);
            }
            else if (p.parseStringIgnoreCase("MEMBER "))
            {
                processMemberExpression(false);
            }
            else if (p.parseStringIgnoreCase("IS "))
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
                if (p.parseStringIgnoreCase("NOT "))
                {
                    not = true;
                }

                if (p.parseStringIgnoreCase("NULL"))
                {
                    Node isNode = new Node(NodeType.OPERATOR, (not ? "!=" : "=="));
                    Node compareNode = new Node(NodeType.LITERAL, null);
                    isNode.insertChildNode(compareNode);
                    isNode.insertChildNode(inputRootNode);
                    stack.push(isNode);
                }
                else if (p.parseStringIgnoreCase("EMPTY"))
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
                    throw new NucleusException("Encountered IS " + (not ? "NOT " : " ") + 
                        " that should be followed by NULL | EMPTY but isnt");
                }
            }
            else if (p.parseString("<="))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "<=");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseString(">="))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, ">=");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseChar('<'))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "<");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseChar('>'))
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

        if (p.parseStringIgnoreCase("ESCAPE"))
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
     * Expression is of the form "{expression} IN (expr1 [,expr2 [,expr3]] | subquery)".
     * Generates a node tree like
     * <pre>({expression} == expr1) || ({expression} == expr2) || ({expression} == expr3)</pre> 
     * @param not Whether this is an expression "NOT IN"
     */
    private void processInExpression(boolean not)
    {
        // TODO Cater for TYPE as the inputNode
        Node inputNode = stack.pop(); // The left hand side expression

        if (!p.parseChar('('))
        {
            // Subquery
            Node inNode = new Node(NodeType.OPERATOR, (not ? "NOT IN" : "IN"));
            inNode.appendChildNode(inputNode);

            processExpression(); // subquery variable
            Node subqueryNode = stack.pop();
            inNode.appendChildNode(subqueryNode);
            stack.push(inNode);
            return;
        }

        Node inNode = null;
        int numArgs = 0;
        do
        {
            // IN ((literal|parameter) [, (literal|parameter)])
            processPrimary();
            if (stack.peek() == null)
            {
                throw new QueryCompilerSyntaxException("Expected literal|parameter but got " + 
                    p.remaining(), p.getIndex(), p.getInput());
            }

            // Generate node for comparison with this value
            numArgs++;
            Node valueNode = stack.pop();

            p.skipWS();
            if (numArgs == 1 && !p.peekStringIgnoreCase(",") && valueNode.getNodeType() == NodeType.PARAMETER &&
                parameterValues != null && parameterValues.containsKey(valueNode.getNodeValue()))
            {
                // Special case of "xxx IN :param" where param is multiple-valued
                Object paramValue = parameterValues.get(valueNode.getNodeValue());
                if (paramValue instanceof Collection)
                {
                    // Node (PARAMETER, param)
                    // ---> Node (INVOKE, "contains")
                    //      ---> Node(IDENTIFIER, inputNode)
                    Node containsNode = new Node(NodeType.INVOKE, "contains");
                    containsNode.addProperty(inputNode);
                    valueNode.appendChildNode(containsNode);
                    inNode = valueNode;
                    break;
                }
            }

            Node compareNode = new Node(NodeType.OPERATOR, (not ? "!=" : "=="));
            compareNode.appendChildNode(inputNode);
            compareNode.appendChildNode(valueNode);

            if (inNode == null)
            {
                inNode = compareNode;
            }
            else
            {
                Node newInNode = new Node(NodeType.OPERATOR, (not ? "&&" : "||"));
                newInNode.appendChildNode(inNode);
                newInNode.appendChildNode(compareNode);
                inNode = newInNode;
            }
        } while (p.parseChar(','));

        if (!p.parseChar(')'))
        {
            throw new QueryCompilerSyntaxException("Expected: ')' but got " + p.remaining(), 
                p.getIndex(), p.getInput());
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
        p.parseStringIgnoreCase("OF"); // Ignore any "OF" keyword here (optional)
        processPrimary(); // Container node at top of stack
        Node containerNode = stack.peek();

        // Make sure we put the INVOKE at the end of the IDENTIFIER chain
        while (containerNode.getFirstChild() != null)
        {
            containerNode = containerNode.getFirstChild();
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
        containerNode.appendChildNode(containsNode);
    }

    /**
     * Method to parse the query syntax for a CASE expression.
     * Creates a Node of type "CASE" with children in the order 
     * <pre>whenNode, actionNode [, whenNode, actionNode]*, elseNode</pre>
     */
    private void processCaseExpression()
    {
        Node caseNode = new Node(NodeType.CASE);
        while (p.parseStringIgnoreCase("WHEN "))
        {
            processExpression();
            Node whenNode = stack.pop();
            caseNode.appendChildNode(whenNode);

            boolean hasThen = p.parseStringIgnoreCase("THEN ");
            if (!hasThen)
            {
                throw new QueryCompilerSyntaxException("expected 'THEN' as part of CASE", p.getIndex(), p.getInput());
            }
            processExpression();
            Node actionNode = stack.pop();
            caseNode.appendChildNode(actionNode);
        }
        if (p.parseStringIgnoreCase("ELSE "))
        {
            processExpression();
            Node elseNode = stack.pop();
            caseNode.appendChildNode(elseNode);
        }
        if (!p.parseStringIgnoreCase("END")) {
        	throw new QueryCompilerSyntaxException("expected 'END' as part of CASE", p.getIndex(), p.getInput());
        }
        stack.push(caseNode);
    }

    protected void processAdditiveExpression()
    {
        processMultiplicativeExpression();

        for (;;)
        {
            if (p.parseChar('+'))
            {
                processMultiplicativeExpression();
                Node expr = new Node(NodeType.OPERATOR, "+");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseChar('-'))
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
            if (p.parseChar('*'))
            {
                processUnaryExpression();
                Node expr = new Node(NodeType.OPERATOR, "*");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseChar('/'))
            {
                processUnaryExpression();
                Node expr = new Node(NodeType.OPERATOR, "/");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseChar('%'))
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
        if (p.parseString("++"))
        {
            throw new NucleusUserException("Unsupported operator '++'");
        }
        else if (p.parseString("--"))
        {
            throw new NucleusUserException("Unsupported operator '--'");
        }

        if (p.parseChar('+'))
        {
            // Just swallow + and leave remains on the stack
            processUnaryExpression();
        }
        else if (p.parseChar('-'))
        {
            processUnaryExpression();
            Node expr = new Node(NodeType.OPERATOR, "-");
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
        else if (p.parseStringIgnoreCase("NOT "))
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

        if (p.parseStringIgnoreCase("SOME "))
        {
            subqueryKeyword = "SOME";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        else if (p.parseStringIgnoreCase("ALL "))
        {
            subqueryKeyword = "ALL";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        else if (p.parseStringIgnoreCase("ANY "))
        {
            subqueryKeyword = "ANY";
            processExpression(); // subquery variable
            subqueryNode = stack.pop();
        }
        else if (p.parseStringIgnoreCase("EXISTS "))
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

        if (p.parseStringIgnoreCase("CURRENT_DATE"))
        {
            // Convert to a method call
            Node node = new Node(NodeType.INVOKE, "CURRENT_DATE");
            stack.push(node);
            return;
        }
        else if (p.parseStringIgnoreCase("CURRENT_TIMESTAMP"))
        {
            // Convert to a method call
            Node node = new Node(NodeType.INVOKE, "CURRENT_TIMESTAMP");
            stack.push(node);
            return;
        }
        else if (p.parseStringIgnoreCase("CURRENT_TIME"))
        {
            // Convert to a method call
            Node node = new Node(NodeType.INVOKE, "CURRENT_TIME");
            stack.push(node);
            return;
        }
        else if (p.parseStringIgnoreCase("CASE "))
        {
            processCaseExpression();
            return;
        }
        else if (p.parseStringIgnoreCase("DISTINCT "))
        {
            // Aggregates can have "count(DISTINCT field1)"
            Node distinctNode = new Node(NodeType.OPERATOR, "DISTINCT");
            processExpression();
            Node identifierNode = stack.pop();
            distinctNode.appendChildNode(identifierNode);
            stack.push(distinctNode);
            return;
        }
        else if (p.parseString("TREAT("))
        {
            // "TREAT(p AS Employee)" will create a Node tree as
            // [IDENTIFIER : p.
            //     [CAST : Employee]]

            processExpression();
            Node identifierNode = stack.pop();
//            String identifier = p.parseIdentifier();
//            Node identifierNode = new Node(NodeType.IDENTIFIER, identifier);

            String typeName = p.parseIdentifier();
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
            castNode.setParent(identifierNode);
            identifierNode.appendChildNode(castNode);
            if (!p.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
            }

            stack.push(castNode);
            return;
        }
        else if (p.parseString("KEY"))
        {
            // KEY(identification_variable)
            // Convert to be {primary}.INVOKE(mapKey)
            p.skipWS();
            p.parseChar('(');
            Node invokeNode = new Node(NodeType.INVOKE, "mapKey");
            processExpression();
            if (!p.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
            }

            Node primaryNode = stack.pop(); // Could check type ? (Map)
            Node primaryRootNode = primaryNode;
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }
            primaryNode.appendChildNode(invokeNode);
            stack.push(primaryRootNode);

            // Allow referral to chain of field(s) of key
            int size = stack.size();
            while (p.parseChar('.'))
            {
                if (processIdentifier())
                {
                    // "a.field"
                }
                else
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
                }
            }
            // For all added nodes, step back and chain them so we have
            // Node[IDENTIFIER, a]
            // +--- Node[IDENTIFIER, b]
            //      +--- Node[IDENTIFIER, c]
            if (size != stack.size())
            {
                Node lastNode = invokeNode;
                while (stack.size() > size)
                {
                    Node top = stack.pop();
                    lastNode.insertChildNode(top);
                    lastNode = top;
                }
            }
            return;
        }
        else if (p.parseString("VALUE"))
        {
            // VALUE(identification_variable)
            // Convert to be {primary}.INVOKE(mapValue)
            p.skipWS();
            p.parseChar('(');
            Node invokeNode = new Node(NodeType.INVOKE, "mapValue");
            processExpression();
            if (!p.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
            }

            Node primaryNode = stack.pop(); // Could check type ? (Map)
            Node primaryRootNode = primaryNode;
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }
            primaryNode.appendChildNode(invokeNode);
            stack.push(primaryRootNode);

            // Allow referral to chain of field(s) of key
            int size = stack.size();
            while (p.parseChar('.'))
            {
                if (processIdentifier())
                {
                    // "a.field"
                }
                else
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
                }
            }
            // For all added nodes, step back and chain them so we have
            // Node[IDENTIFIER, a]
            // +--- Node[IDENTIFIER, b]
            //      +--- Node[IDENTIFIER, c]
            if (size != stack.size())
            {
                Node lastNode = invokeNode;
                while (stack.size() > size)
                {
                    Node top = stack.pop();
                    lastNode.insertChildNode(top);
                    lastNode = top;
                }
            }
            return;
        }
        else if (p.parseString("ENTRY"))
        {
            // ENTRY(identification_variable)
            // Convert to be {primary}.INVOKE(mapEntry)
            p.skipWS();
            p.parseChar('(');
            Node invokeNode = new Node(NodeType.INVOKE, "mapEntry");
            processExpression();
            if (!p.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
            }

            Node primaryNode = stack.pop(); // Could check type ? (Map)
            Node primaryRootNode = primaryNode;
            while (primaryNode.getFirstChild() != null)
            {
                primaryNode = primaryNode.getFirstChild();
            }
            primaryNode.appendChildNode(invokeNode);
            stack.push(primaryRootNode);
            return;
        }
        else if (processCreator() || processLiteral() || processMethod())
        {
            return;
        }

        Node castNode = null;
        if (p.parseChar('('))
        {
            processExpression();
            if (!p.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("expected ')'", p.getIndex(), p.getInput());
            }
            Node peekNode = stack.peek();
            if (peekNode.getNodeType() == NodeType.CAST)
            {
                castNode = peekNode;
            }
            else
            {
                return;
            }
        }

        // if primary == null, literal not found...
        // We will have an identifier (variable, parameter, or field of candidate class)
        if (castNode == null && !processIdentifier())
        {
            throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
        }
        int size = stack.size();

        // Generate Node tree, including chained operations
        // e.g identifier.methodX().methodY().methodZ() 
        //     -> node (IDENTIFIER) with child (INVOKE), with child (INVOKE), with child (INVOKE)
        // e.g identifier.fieldX.fieldY.fieldZ
        //     -> node (IDENTIFIER) with child (IDENTIFIER), with child (IDENTIFIER), with child (IDENTIFIER)
        while (p.parseChar('.'))
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
                throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
            }
        }

        // For all added nodes, step back and chain them so we have
        // Node[IDENTIFIER, a]
        // +--- Node[IDENTIFIER, b]
        //      +--- Node[IDENTIFIER, c]
        while (stack.size() > size)
        {
            Node top = stack.pop();
            Node peek = stack.peek();
            peek.insertChildNode(top);
        }

        if (castNode != null)
        {
            // When we have a cast (TREAT), make sure we put the parent node (the identifier that is cast) as the node on the stack
            stack.pop();
            Node castParentNode = castNode.getParent();
            stack.push(castParentNode);
        }
    }

    /**
     * Method to parse "new a.b.c(param1[,param2], ...)" and create a Node of type CREATOR.
     * The Node at the top of the stack after this call will have any arguments defined in its "properties".
     * @return whether method syntax was found.
     */
    private boolean processCreator()
    {
        if (p.parseStringIgnoreCase("NEW "))
        {
            // "new MyClass(arg1, arg2)"
            int size = stack.size();
            if (!processMethod())
            {
                if (!processIdentifier())
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
                }

                // run function on literals or identifiers e.g. "primary.runMethod(arg)"
                while (p.parseChar('.'))
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
                        throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
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
     * Method to parse "methodName(param1[,param2], ...)" and create a Node of type INVOKE.
     * The Node at the top of the stack after this call will have any arguments defined in its "properties".
     * @return whether method syntax was found.
     */
    private boolean processMethod()
    {
        String method = p.parseMethod();
        if (method != null)
        {
            p.skipWS();
            p.parseChar('(');

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
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                }
                return true;
            }
            else if (method.equalsIgnoreCase("MOD"))
            {
                // Convert to be {first} % {second}
                Node modNode = new Node(NodeType.OPERATOR, "%");
                processExpression(); // argument 1
                Node firstNode = stack.pop();
                if (!p.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }
                processExpression(); // argument 2
                Node secondNode = stack.pop();
                modNode.appendChildNode(firstNode);
                modNode.appendChildNode(secondNode);
                stack.push(modNode);
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
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
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
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
                if (!p.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                // First arg to substring(...) has origin 0, but JPQL has origin 1!
                processExpression();
                Node arg1 = stack.pop();
                Node oneNode = new Node(NodeType.LITERAL, 1);
                Node arg1Node = new Node(NodeType.OPERATOR, "-");
                arg1Node.insertChildNode(arg1);
                arg1Node.appendChildNode(oneNode);

                if (p.parseChar(','))
                {
                    // String.substring(arg1, arg2)
                    // Second arg to substring(...) has origin 0, but in JPQL is length of result!
                    processExpression();
                    Node arg2 = stack.pop();
                    Node arg2Node = new Node(NodeType.OPERATOR, "+");
                    arg2Node.appendChildNode(arg2);
                    arg2Node.appendChildNode(arg1Node);
                    if (!p.parseChar(')'))
                    {
                        throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                    }

                    primaryNode.appendChildNode(invokeNode);
                    invokeNode.addProperty(arg1Node);
                    invokeNode.addProperty(arg2Node);
                    stack.push(primaryNode);
                    return true;
                }
                else if (p.parseChar(')'))
                {
                    // String.substring(arg1)
                    primaryNode.appendChildNode(invokeNode);
                    invokeNode.addProperty(arg1Node);
                    stack.push(primaryNode);
                    return true;
                }
                else
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                }
            }
            else if (method.equalsIgnoreCase("UPPER"))
            {
                // UPPER(string_primary)
                // Convert to be {primary}.INVOKE(toUpper)
                Node invokeNode = new Node(NodeType.INVOKE, "toUpperCase");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
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
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
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
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
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
                    if (!p.parseChar(','))
                    {
                        throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                    }

                    processExpression();
                    Node thisNode = stack.pop();

                    Node currentNode = new Node(NodeType.OPERATOR, "+");
                    currentNode.appendChildNode(prevNode);
                    currentNode.appendChildNode(thisNode);
                    if (p.parseChar(')'))
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
                if (!p.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
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
                if (p.parseChar(','))
                {
                    processExpression();
                    Node fromPosNode = stack.pop();
                    Node positionNode = new Node(NodeType.OPERATOR, "-");
                    positionNode.appendChildNode(fromPosNode);
                    positionNode.appendChildNode(oneNode);
                    invokeNode.addProperty(positionNode);
                }
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
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
                if (p.parseStringIgnoreCase("LEADING"))
                {
                    methodName = "trimLeft";
                }
                else if (p.parseStringIgnoreCase("TRAILING"))
                {
                    methodName = "trimRight";
                }
                else if (p.parseStringIgnoreCase("BOTH"))
                {
                    // Default
                }
                Node invokeNode = new Node(NodeType.INVOKE, methodName);

                Node trimCharNode = null;
                processExpression();
                Node next = stack.pop();
                if (p.parseChar(')'))
                {
                    // TRIM(string_primary)
                    next.appendChildNode(invokeNode);
                    stack.push(next);
                    return true;
                }

                if (next.getNodeType() == NodeType.LITERAL)
                {
                    // TRIM(dir trimChar FROM string_primary)
                    trimCharNode = next;
                    if (p.parseStringIgnoreCase("FROM "))
                    {
                        // Ignore the FROM
                    }
                    processExpression();
                    next = stack.pop();
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
                        throw new QueryCompilerSyntaxException("Unexpected expression", p.getIndex(), p.getInput());
                    }
                }
                else
                {
                    // No "trimChar" or FROM, so "next" is the string expression node
                }

                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                }

                next.appendChildNode(invokeNode);
                if (trimCharNode != null)
                {
                    invokeNode.addProperty(trimCharNode);
                }
                stack.push(next);
                return true;
            }
            else if (method.equalsIgnoreCase("SIZE"))
            {
                // SIZE(collection_valued_path_expression)
                // Convert to be {primary}.INVOKE(size)
                Node invokeNode = new Node(NodeType.INVOKE, "size");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
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
            else if (method.equalsIgnoreCase("YEAR"))
            {
                // Extension MONTH - Convert to be {primary}.INVOKE(getYear)
                Node invokeNode = new Node(NodeType.INVOKE, "getYear");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Date)
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("MONTH"))
            {
                // Extension MONTH - Convert to be {primary}.INVOKE(getMonth)
                Node invokeNode = new Node(NodeType.INVOKE, "getMonth");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Date)
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("DAY"))
            {
                // Extension DAY - Convert to be {primary}.INVOKE(getDay)
                Node invokeNode = new Node(NodeType.INVOKE, "getDay");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Date)
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("HOUR"))
            {
                // Extension HOUR - Convert to be {primary}.INVOKE(getHour)
                Node invokeNode = new Node(NodeType.INVOKE, "getHour");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Date)
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("MINUTE"))
            {
                // Extension MINUTE - Convert to be {primary}.INVOKE(getMinute)
                Node invokeNode = new Node(NodeType.INVOKE, "getMinute");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Date)
                Node primaryRootNode = primaryNode;
                while (primaryNode.getFirstChild() != null)
                {
                    primaryNode = primaryNode.getFirstChild();
                }
                primaryNode.appendChildNode(invokeNode);
                stack.push(primaryRootNode);
                return true;
            }
            else if (method.equalsIgnoreCase("SECOND"))
            {
                // Extension SECOND - Convert to be {primary}.INVOKE(getSecond)
                Node invokeNode = new Node(NodeType.INVOKE, "getSecond");
                processExpression();
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("',' expected", p.getIndex(), p.getInput());
                }

                Node primaryNode = stack.pop(); // Could check type ? (Date)
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
                if (p.parseChar(','))
                {
                    // Process arguments for function "aaa[,bbb[,ccc]] etc )"
                    do
                    {
                        // Argument for the method call, add as a node property
                        processExpression();
                        invokeNode.addProperty(stack.pop());
                    }
                    while (p.parseChar(','));
                }
                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                }

                stack.push(invokeNode);
                return true;
            }
            else
            {
                // Found syntax for a method, so invoke the method
                // TODO What if the method is not supported for JPQL?
                Node node = new Node(NodeType.INVOKE, method);
                if (!p.parseChar(')'))
                {
                    do
                    {
                        // Argument for the method call, add as a node property
                        processExpression();
                        node.addProperty(stack.pop());
                    }
                    while (p.parseChar(','));

                    if (!p.parseChar(')'))
                    {
                        throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                    }
                }
                stack.push(node);
                return true;
            }
        }
        return false;
    }

    /**
     * A literal is one value of any type.
     * Supported literals are of types String, Floating Point, Integer,
     * Character, Boolean and null e.g. 'J', "String", 1, 1.8, true, false, null.
     * @return The compiled literal
     */
    protected boolean processLiteral()
    {
        if (p.parseChar('{'))
        {
            // JDBC escape syntax
            // {d '...'}
            // {t '...'}
            // {ts '...'}
            StringBuilder jdbcLiteralStr = new StringBuilder("{");
            if (p.parseChar('d'))
            {
                jdbcLiteralStr.append("d ");
            }
            else if (p.parseChar('t'))
            {
                jdbcLiteralStr.append("t ");
            }
            else if (p.parseString("ts"))
            {
                jdbcLiteralStr.append("ts ");
            }
            else
            {
                throw new QueryCompilerSyntaxException("d, t or ts expected after { (JDBC escape syntax)", p.getIndex(), p.getInput());
            }

            if (p.nextIsSingleQuote())
            {
                String datetimeLit = p.parseStringLiteral();
                jdbcLiteralStr.append("'").append(datetimeLit).append("'");
                if (p.parseChar('}'))
                {
                    jdbcLiteralStr.append('}');
                    stack.push(new Node(NodeType.LITERAL, jdbcLiteralStr.toString()));
                    return true;
                }

                throw new QueryCompilerSyntaxException("} expected in JDBC escape syntax", p.getIndex(), p.getInput());
            }
            throw new QueryCompilerSyntaxException("'...' expected in JDBC escape syntax", p.getIndex(), p.getInput());
        }

        Object litValue = null;
        String sLiteral;
        BigDecimal fLiteral;
        BigInteger iLiteral;
        Boolean bLiteral;
        boolean single_quote_next = p.nextIsSingleQuote();
        if ((sLiteral = p.parseStringLiteral()) != null)
        {
            // Both String and Character are allowed to use single-quotes
            // so we need to check if it was single-quoted and
            // use CharacterLiteral if length is 1.
            if (sLiteral.length() == 1 && single_quote_next)
            {
                litValue = Character.valueOf(sLiteral.charAt(0));
            }
            else
            {
                litValue = sLiteral;
            }
        }
        else if ((fLiteral = p.parseFloatingPointLiteral()) != null)
        {
            litValue = fLiteral;
        }
        else if ((iLiteral = p.parseIntegerLiteral()) != null)
        {
            litValue = Long.valueOf(iLiteral.longValue());
        }
        else if ((bLiteral = p.parseBooleanLiteralIgnoreCase()) != null)
        {
            litValue = bLiteral;
        }
        else if (p.parseNullLiteralIgnoreCase())
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
        String id = p.parseIdentifier();
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