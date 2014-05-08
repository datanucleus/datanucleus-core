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
2008 Andy Jefferson - much restructuring, static methods, negate/complement operators
2009 Andy Jefferson - from clause, result clause, cast expression
    ...
**********************************************************************/
package org.datanucleus.query.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.query.JDOQLQueryHelper;
import org.datanucleus.store.query.QueryCompilerSyntaxException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of a parser for JDOQL query language.
 * Generates Node tree(s) by use of the various parseXXX() methods.
 */
public class JDOQLParser implements Parser
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    private static String[] jdoqlMethodNames = {"contains", "get", "containsKey", "containsValue", "isEmpty",
        "size", "toLowerCase", "toUpperCase", "indexOf", "matches", "substring", "startsWith", "endsWith",
        "getObjectId", "abs", "sqrt"
    };

    private enum ParameterType { IMPLICIT, EXPLICIT };

    private ParameterType paramType = ParameterType.IMPLICIT;

    private String jdoqlMode = "DataNucleus";

    private Lexer p;
    private Stack<Node> stack = new Stack();

    /** Characters that parameters can be prefixed by. */
    private static String paramPrefixes = ":";

    private boolean allowSingleEquals = false;

    /**
     * Constructor for a JDOQL Parser.
     * Supports "jdoql.level" option so can have strict JDO2 syntax, or flexible.
     * @param options parser options
     */
    public JDOQLParser(Map options)
    {
        if (options != null && options.containsKey("jdoql.level"))
        {
            jdoqlMode = (String)options.get("jdoql.level");
        }
        if (options != null && options.containsKey("explicitParameters"))
        {
            paramType = ParameterType.EXPLICIT;
        }
    }

    public void allowSingleEquals(boolean flag)
    {
        allowSingleEquals = flag;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parse(java.lang.String)
     */
    public Node parse(String expression)
    {
        p = new Lexer(expression, paramPrefixes, true);
        stack = new Stack();
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
        p = new Lexer(expression, paramPrefixes, true);
        stack = new Stack();
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
        p = new Lexer(expression, paramPrefixes, true);
        stack = new Stack();
        return processFromExpression();
    }

    /**
     * The FROM expression in JDOQL (subquery) is a "candidate alias" expression, like
     * <ul>
     * <li>mydomain.MyClass [AS] alias"</li>
     * </ul>
     * @return Node tree(s) for the FROM expression
     */
    private Node[] processFromExpression()
    {
        // "candidate [AS] alias"
        // This will create a node of type "Node.CLASS" and child of type "Node.NAME" (alias)
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
        if (alias == null)
        {
            alias = "this";
        }

        // Create candidate class node with alias, and put at top of stack
        Node classNode = new Node(NodeType.CLASS, className.toString());
        Node aliasNode = new Node(NodeType.NAME, alias);
        classNode.insertChildNode(aliasNode);
        stack.push(classNode);

        return new Node[] {classNode};
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseUpdate(java.lang.String)
     */
    public Node[] parseUpdate(String expression)
    {
        // Bulk update not supported by JDOQL
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseOrder(java.lang.String)
     */
    public Node[] parseOrder(String expression)
    {
        p = new Lexer(expression, paramPrefixes, true);
        stack = new Stack();
        return processOrderExpression();
    }

    /**
     * The RESULT expression in JDOQL can include aggregates, fields, as well as aliases
     * <ul>
     * <li>myfield [AS] alias, myfield2"</li>
     * </ul>
     * The Node tree for this would be
     * <pre>[
     * [IDENTIFIER : myfield.
     *      [NAME : alias]],
     * [IDENTIFIER : myfield2]
     * ]</pre>
     * @return Node tree(s) for the RESULT expression
     */
    public Node[] parseResult(String expression)
    {
        p = new Lexer(expression, paramPrefixes, true);
        stack = new Stack();
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node expr = stack.pop();

            String alias = p.parseIdentifier();
            if (alias != null && alias.equalsIgnoreCase("AS"))
            {
                alias = p.parseIdentifier();
            }
            if (alias != null)
            {
                Node aliasNode = new Node(NodeType.NAME, alias);
                expr.appendChildNode(aliasNode);
            }

            nodes.add(expr);
        }
        while (p.parseString(","));
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseTupple(java.lang.String)
     */
    public Node[] parseTupple(String expression)
    {
        p = new Lexer(expression, paramPrefixes, true);
        stack = new Stack();
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node expr = stack.pop();
            nodes.add(expr);
        }
        while (p.parseString(","));
        return (Node[])nodes.toArray(new Node[nodes.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.compiler.Parser#parseVariables(java.lang.String)
     */
    public Node[][] parseVariables(String expression)
    {
        p = new Lexer(expression, paramPrefixes, true);
        List nodes = new ArrayList();
        do
        {
            if (StringUtils.isWhitespace(p.remaining()))
            {
                break;
            }

            processPrimary();
            if (stack.isEmpty())
            {
                throw new QueryCompilerSyntaxException("Parsing variable list and expected variable type", 
                    p.getIndex(), p.getInput());
            }
            if (!processIdentifier())
            {
                throw new QueryCompilerSyntaxException("Parsing variable list and expected variable name",
                    p.getIndex(), p.getInput());
            }

            Node nodeVariable = stack.pop();
            String varName = (String)nodeVariable.getNodeValue();
            if (!JDOQLQueryHelper.isValidJavaIdentifierForJDOQL(varName))
            {
                throw new NucleusUserException(LOCALISER.msg("021105",varName));
            }

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
        List nodes = new ArrayList();
        StringTokenizer tokeniser = new StringTokenizer(expression, ",");
        while (tokeniser.hasMoreTokens())
        {
            String token = tokeniser.nextToken();
            StringTokenizer subTokeniser = new StringTokenizer(token, " ");
            if (subTokeniser.countTokens() != 2)
            {
                throw new QueryCompilerSyntaxException(LOCALISER.msg("021101", expression));
            }
            String classDecl = subTokeniser.nextToken();
            String parameterName = subTokeniser.nextToken();
            Node declNode = new Node(NodeType.IDENTIFIER, classDecl);
            Node nameNode = new Node(NodeType.IDENTIFIER, parameterName);
            nodes.add(new Node[]{declNode, nameNode});
        }

        return (Node[][]) nodes.toArray(new Node[nodes.size()][2]);
    }

    private Node[] processOrderExpression()
    {
        List nodes = new ArrayList();
        do
        {
            processExpression();
            Node directionNode = null;
            if (p.parseString("ascending") || p.parseString("asc") ||
                p.parseString("ASCENDING") || p.parseString("ASC"))
            {
                directionNode = new Node(NodeType.OPERATOR, "ascending");
            }
            else if (p.parseString("descending") || p.parseString("desc") ||
                     p.parseString("DESCENDING") || p.parseString("DESC"))
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

            if (!stack.empty()) // How can this be not empty?
            {
                expr.insertChildNode(stack.pop());
            }
            nodes.add(expr);
        }
        while (p.parseChar(','));

        return (Node[]) nodes.toArray(new Node[nodes.size()]);
    }

    private Node processExpression()
    {
        processConditionalOrExpression();
        return stack.peek();
    }

    /**
     * This method deals with the OR condition.
     * A condition specifies a combination of one or more expressions and logical (Boolean) operators and 
     * returns a value of TRUE, FALSE, or unknown
     */
    private void processConditionalOrExpression()
    {
        processConditionalAndExpression();

        while (p.parseString("||"))
        {
            processConditionalAndExpression();
            Node expr = new Node(NodeType.OPERATOR, "||");
            expr.insertChildNode(stack.pop());
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
    }

    /**
     * This method deals with the AND condition.
     * A condition specifies a combination of one or more expressions and
     * logical (Boolean) operators and returns a value of TRUE, FALSE, or unknown
     */
    private void processConditionalAndExpression()
    {
        processInclusiveOrExpression();

        while (p.parseString("&&"))
        {
            processInclusiveOrExpression();
            Node expr = new Node(NodeType.OPERATOR, "&&");
            expr.insertChildNode(stack.pop());
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
    }

    private void processInclusiveOrExpression()
    {
        processExclusiveOrExpression();

        while (p.parseChar('|', '|'))
        {
            processExclusiveOrExpression();
            Node expr = new Node(NodeType.OPERATOR, "|");
            expr.insertChildNode(stack.pop());
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
    }

    private void processExclusiveOrExpression()
    {
        processAndExpression();

        while (p.parseChar('^'))
        {
            processAndExpression();
            Node expr = new Node(NodeType.OPERATOR, "^");
            expr.insertChildNode(stack.pop());
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
    }

    private void processAndExpression()
    {
        processRelationalExpression();

        while (p.parseChar('&', '&'))
        {
            processRelationalExpression();
            Node expr = new Node(NodeType.OPERATOR, "&");
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
            if (p.parseString("=="))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "==");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseString("!="))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "!=");
                expr.insertChildNode(stack.pop());
                expr.insertChildNode(stack.pop());
                stack.push(expr);
            }
            else if (p.parseString("="))
            {
                if (allowSingleEquals)
                {
                    // Allowed when processing UPDATE clause ("SET xyz = val")
                    processAdditiveExpression();
                    Node expr = new Node(NodeType.OPERATOR, "==");
                    expr.insertChildNode(stack.pop());
                    expr.insertChildNode(stack.pop());
                    stack.push(expr);
                }
                else
                {
                    // Assignment operator is invalid (user probably meant to specify "==")
                    throw new QueryCompilerSyntaxException("Invalid operator \"=\". Did you mean to use \"==\"?");
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
            else if (p.parseString("instanceof"))
            {
                processAdditiveExpression();
                Node expr = new Node(NodeType.OPERATOR, "instanceof");
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
            throw new QueryCompilerSyntaxException("Unsupported operator '++'");
        }
        else if (p.parseString("--"))
        {
            throw new QueryCompilerSyntaxException("Unsupported operator '--'");
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
        else if (p.parseChar('~'))
        {
            processUnaryExpression();
            Node expr = new Node(NodeType.OPERATOR, "~");
            expr.insertChildNode(stack.pop());
            stack.push(expr);
        }
        else if (p.parseChar('!'))
        {
            processUnaryExpression();
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
     * Parses the primary. First look for a literal (e.g. "text"), then
     * an identifier(e.g. variable) In the next step, call a function, if
     * executing a function, on the literal or the identifier found.
     */
    protected void processPrimary()
    {
        if (p.parseString("DISTINCT ") || p.parseString("distinct"))
        {
            // Aggregates can have "count(DISTINCT field1)"
            Node distinctNode = new Node(NodeType.OPERATOR, "DISTINCT");
            processExpression();
            Node identifierNode = stack.pop();
            distinctNode.appendChildNode(identifierNode);
            stack.push(distinctNode);
            return;
        }

        // Find any cast first, and remove from top of stack since we put after the cast component
        Node castNode = null;
        if (processCast())
        {
            castNode = stack.pop();
        }

        if (processCreator())
        {
            // "new MyClass(...)", allow chain of method invocations
            boolean endOfChain = false;
            while (!endOfChain)
            {
                if (p.parseChar('.'))
                {
                    if (processMethod())
                    {
                        // Add method invocation as a child node of what it is invoked on
                        Node invokeNode = stack.pop();
                        Node invokedNode = stack.peek();
                        invokedNode.appendChildNode(invokeNode);
                    }
                }
                else
                {
                    endOfChain = true;
                }
            }
            if (castNode != null)
            {
                // TODO Add cast of creator expression
                throw new NucleusException("Dont currently support compile of cast of creator expression");
            }
            return;
        }
        else if (processLiteral())
        {
            // Literal, allow chain of method invocations
            boolean endOfChain = false;
            while (!endOfChain)
            {
                if (p.parseChar('.'))
                {
                    if (processMethod())
                    {
                        // Add method invocation as a child node of what it is invoked on
                        Node invokeNode = stack.pop();
                        Node invokedNode = stack.peek();
                        invokedNode.appendChildNode(invokeNode);
                    }
                }
                else
                {
                    endOfChain = true;
                }
            }
            if (castNode != null)
            {
                throw new NucleusException("Dont currently support compile of cast of literal expression");
                // TODO Add cast of literal
            }
            return;
        }
        else if (processMethod())
        {
            // Static method call
            if (castNode != null)
            {
                throw new NucleusException("Dont currently support compile of cast of static method call");
                // TODO Add cast of static method call
            }
            return;
        }
        else if (processArray())
        {
            // Array, allow chain of method invocations
            boolean endOfChain = false;
            while (!endOfChain)
            {
                if (p.parseChar('.'))
                {
                    if (processMethod())
                    {
                        // Add method invocation as a child node of what it is invoked on
                        Node invokeNode = stack.pop();
                        Node invokedNode = stack.peek();
                        invokedNode.appendChildNode(invokeNode);
                    }
                }
                else
                {
                    endOfChain = true;
                }
            }
            if (castNode != null)
            {
                throw new NucleusException("Dont currently support compile of cast of array expression");
                // TODO Add cast of array
            }
            return;
        }

        int sizeBeforeBraceProcessing = stack.size();
        boolean braceProcessing = false;
        if (p.parseChar('('))
        {
            // "({expr1})"
            processExpression();
            if (!p.parseChar(')'))
            {
                throw new QueryCompilerSyntaxException("expected ')'", p.getIndex(), p.getInput());
            }

            if (!p.parseChar('.'))
            {
                // TODO If we have a cast, then apply to the current node (with the brackets)
                return;
            }

            // Has follow on expression "({expr1}).{expr2}" so continue
            braceProcessing = true;
        }

        // We will have an identifier (variable, parameter, or field of candidate class)
        if (processMethod())
        {
        }
        else if (processIdentifier())
        {
        }
        else
        {
            throw new QueryCompilerSyntaxException("Method/Identifier expected", p.getIndex(), p.getInput());
        }

        // Save the stack size just before this component for use later in squashing all nodes
        // down to a single Node in the stack with all others chained off from it
        int size = stack.size();
        if (braceProcessing)
        {
            size = sizeBeforeBraceProcessing+1;
        }

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

        // Generate Node tree, including chained operations
        // e.g identifier.methodX().methodY().methodZ() 
        //     -> node (IDENTIFIER) with child (INVOKE), with child (INVOKE), with child (INVOKE)
        // e.g identifier.fieldX.fieldY.fieldZ
        //     -> node (IDENTIFIER) with child (IDENTIFIER), with child (IDENTIFIER), with child (IDENTIFIER)
        if (castNode != null)
        {
            // Put the CAST as a child of the Node being cast
            stack.peek().appendChildNode(castNode);
        }

        // For all added nodes in this block, step back and chain them. Examples :-
        // a.b.c being compiled to "Node[IDENTIFIER, a] -> Node[IDENTIFIER, b] -> Node[IDENTIFIER, c]"
        // ((B)a).c being compiled to "Node[IDENTIFIER, a] -> Node[CAST, B] -> Node[IDENTIFIER, c]"
        while (stack.size() > size)
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
     * Convenience method to navigate down through descendants to find the last one.
     * Uses the first child node each time, so doesn't cope if there are multiple.
     * @param node The node
     * @return The last descendant
     */
    private Node getLastDescendantNodeForNode(Node node)
    {
        if (node == null)
        {
            return null;
        }
        if (node.getChildNodes() == null)
        {
            return node;
        }
        else if (node.getChildNodes().size() > 1)
        {
            return null;
        }
        if (!node.hasNextChild())
        {
            return node;
        }
        return getLastDescendantNodeForNode(node.getChildNode(0));
    }

    /**
     * Parse any cast expression.
     * If a cast is found will create a Node of type CAST with the value being the class to cast to.
     * @return Whether a cast was processed
     */
    private boolean processCast()
    {
        String typeName = p.parseCast();
        if (typeName == null)
        {
            return false;
        }

        Node castNode = new Node(NodeType.CAST, typeName);
        stack.push(castNode);
        return true;
    }

    /**
     * Method to parse "new a.b.c(param1[,param2], ...)" and create a Node of type CREATOR.
     * The Node at the top of the stack after this call will have any arguments defined in its "properties".
     * @return whether method syntax was found.
     */
    private boolean processCreator()
    {
        if (p.parseString("new "))
        {
            int size = stack.size();
            if (!processMethod())
            {
                if (!processIdentifier())
                {
                    throw new QueryCompilerSyntaxException("Identifier expected", p.getIndex(), p.getInput());
                }

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
                Node top = stack.pop();
                Node peek = stack.peek();
                peek.insertChildNode(top);
            }
            Node expr = stack.pop();
            Node newExpr = new Node(NodeType.CREATOR);
            newExpr.insertChildNode(expr);
            stack.push(newExpr);
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

            if (jdoqlMode.equals("JDO2"))
            {
                // Enable strictness checking for levels of JDOQL
                // Note that this only checks the method name and not the arguments/types
                if (Arrays.binarySearch(jdoqlMethodNames, method) < 0)
                {
                    throw new QueryCompilerSyntaxException("Query uses method \"" + method + 
                        "\" but this is not a standard JDOQL method name");
                }
            }

            // Found syntax for a method, so invoke the method
            Node expr = new Node(NodeType.INVOKE, method);
            if (!p.parseChar(')'))
            {
                do
                {
                    // Argument for the method call, add as a node property
                    processExpression();
                    expr.addProperty(stack.pop());
                }
                while (p.parseChar(','));

                if (!p.parseChar(')'))
                {
                    throw new QueryCompilerSyntaxException("')' expected", p.getIndex(), p.getInput());
                }
            }

            stack.push(expr);
            return true;
        }
        return false;
    }

    /**
     * Method to parse an array expression ("{a,b,c}"), creating an ARRAY node with the node value
     * being a List<Node> of the elements. Also handles processing of subsequent "length" 'method'
     * which is special case for arrays and not caught by processMethod() since it doesn't have "()".
     * @return Whether an array was parsed from the current position
     */
    private boolean processArray()
    {
        if (p.parseChar('{'))
        {
            // Array
            List<Node> elements = new ArrayList();
            while (!p.parseChar('}'))
            {
                processPrimary();
                Node elementNode = stack.pop();
                elements.add(elementNode);

                if (p.parseChar('}'))
                {
                    break;
                }
                else if (!p.parseChar(','))
                {
                    throw new QueryCompilerSyntaxException("',' or '}' expected", p.getIndex(), p.getInput());
                }
            }

            Node arrayNode = new Node(NodeType.ARRAY, elements);
            stack.push(arrayNode);

            // Check for "length" since won't be picked up by processMethod
            if (p.parseString(".length"))
            {
                Node lengthMethod = new Node(NodeType.INVOKE, "length");
                arrayNode.appendChildNode(lengthMethod);
            }

            return true;
        }
        return false;
    }

    /**
     * A literal is one value of any type.
     * Supported literals are of types String, Floating Point, Integer,
     * Character, Boolean and null e.g. 'J', "String", 1, 1.8, true, false, null.
     * @return The parsed literal
     */
    protected boolean processLiteral()
    {
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
        else if ((bLiteral = p.parseBooleanLiteral()) != null)
        {
            litValue = bLiteral;
        }
        else if (p.parseNullLiteral())
        {
        }
        else
        {
            return false;
        }

        stack.push(new Node(NodeType.LITERAL, litValue));
        return true;
    }

    /**
     * An identifier always designates a reference to a single value.
     * A single value can be one collection, one field.
     * @return The parsed identifier
     */
    private boolean processIdentifier()
    {
        String id = p.parseIdentifier();
        if (id == null)
        {
            return false;
        }
        char first = id.charAt(0);
        if (first == ':')
        {
            if (paramType == ParameterType.EXPLICIT)
            {
                throw new QueryCompilerSyntaxException("Explicit parameters defined for query, yet implicit parameter syntax (\"" + id + "\") found");
            }

            // Named parameter - stored as String
            String name = id.substring(1);
            Node expr = new ParameterNode(NodeType.PARAMETER, name, getPositionFromParameterName(name));
            stack.push(expr);
            return true;
        }
        else
        {
            Node expr = new Node(NodeType.IDENTIFIER, id);
            stack.push(expr);
            return true;
        }
    }

    private ArrayList<Object> parameterNameList = null;

    private int getPositionFromParameterName(Object name)
    {
        if (parameterNameList == null)
        {
            parameterNameList = new ArrayList<Object>(1);
        }

        int pos = parameterNameList.indexOf(name);
        if (pos == -1)
        {
            pos = parameterNameList.size();
            parameterNameList.add(name);
        }

        return pos;
    }
}
