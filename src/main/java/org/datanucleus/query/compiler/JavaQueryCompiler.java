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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.ExpressionCompiler;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.PrimaryExpressionIsClassLiteralException;
import org.datanucleus.query.expression.PrimaryExpressionIsClassStaticFieldException;
import org.datanucleus.query.expression.PrimaryExpressionIsVariableException;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.store.query.QueryCompilerSyntaxException;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Imports;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Typical implementation of a compiler for a java-based query language.
 * The constructor takes in the components of the query, and the method compile() compiles it
 * returning the compiled query, for use elsewhere.
 * <p>
 * Each "Expression" is effectively a tree of Expressions. You can navigate through each expression based on
 * their type. For example, a DyadicExpression has a "left" and "right" and an operator between them.
 * The left could also be a DyadicExpression, so you would navigate to its left/right components etc etc.
 * </p>
 */
public abstract class JavaQueryCompiler implements SymbolResolver
{
    public static final String JOIN_INNER = "JOIN_INNER";
    public static final String JOIN_INNER_FETCH = "JOIN_INNER_FETCH";
    public static final String JOIN_OUTER = "JOIN_OUTER";
    public static final String JOIN_OUTER_FETCH = "JOIN_OUTER_FETCH";
    public static final String JOIN_OUTER_RIGHT = "JOIN_OUTER_RIGHT";
    public static final String JOIN_OUTER_FETCH_RIGHT = "JOIN_OUTER_FETCH_RIGHT";

    protected JavaQueryCompiler parentCompiler;
    protected Map<Object, String> parameterSubtitutionMap;
    protected int parameterSubstitutionNumber = 0;

    protected final MetaDataManager metaDataManager;
    protected final ClassLoaderResolver clr;

    protected boolean caseSensitiveAliases = true;

    /** Primary candidate class (if defined). */
    protected Class candidateClass;

    /** Alias for the primary candidate. Default to "this" (JDOQL) but can be set. */
    protected String candidateAlias = "this";

    /** Default candidate alias in use (only set when in a subquery and the same as the outer query). */
    protected String candidateAliasOrig = null;

    protected String from;
    protected Collection candidates;

    protected String update;
    protected String filter;
    protected String ordering;
    protected String parameters;
    protected String variables;
    protected String grouping;
    protected String having;
    protected String result;
    protected Imports imports;

    /** Compiled Symbol Table. */
    protected SymbolTable symtbl;

    /** Parser specific to the type of query being compiled. */
    protected Parser parser;

    protected Map<String, String> queryMethodAliasByPrefix = null;

    protected Map<String, Object> options;

    public JavaQueryCompiler(MetaDataManager metaDataManager, ClassLoaderResolver clr, 
            String from, Class candidateClass, Collection candidates,
            String filter, Imports imports, String ordering, String result, String grouping, String having, 
            String params, String variables, String update)
    {
        this.metaDataManager = metaDataManager;
        this.clr = clr;

        ConfigurationElement[] queryMethodAliases = metaDataManager.getNucleusContext().getPluginManager().getConfigurationElementsForExtension(
            "org.datanucleus.query_method_prefix", null, null);
        if (queryMethodAliases != null && queryMethodAliases.length > 0)
        {
            queryMethodAliasByPrefix = new HashMap<String, String>();
            for (int i=0;i<queryMethodAliases.length;i++)
            {
                queryMethodAliasByPrefix.put(queryMethodAliases[i].getAttribute("prefix"), queryMethodAliases[i].getAttribute("alias"));
            }
        }

        this.from = from;
        this.candidateClass = candidateClass;
        this.candidates = candidates;

        this.filter = filter;
        this.result = result;
        this.grouping = grouping;
        this.having = having;
        this.ordering = ordering;
        this.parameters = params;
        this.variables = variables;
        this.update = update;

        this.imports = imports;
        if (imports == null)
        {
            this.imports = new Imports();
            if (candidateClass != null)
            {
                // Add candidate class
                this.imports.importClass(candidateClass.getName());
                this.imports.importPackage(candidateClass.getName());
            }
        }
    }

    /**
     * Accessor for the query language name.
     * @return Name of the query language.
     */
    public abstract String getLanguage();

    public void setOption(String name, Object value)
    {
        if (options == null)
        {
            options = new HashMap();
        }
        options.put(name, value);
    }

    /**
     * Method to set the linkage to the parent query.
     * @param parentCompiler Compiler for the parent query
     * @param paramSubstitutionMap Map of parameters in this subquery and what they are
     *      in the parent query.
     */
    public void setLinkToParentQuery(JavaQueryCompiler parentCompiler, Map paramSubstitutionMap)
    {
        this.parentCompiler = parentCompiler;
        this.parameterSubtitutionMap = paramSubstitutionMap;
    }

    /**
     * Method to compile the query.
     * @param parameters The parameter values keyed by name.
     * @param subqueryMap Map of subqueries keyed by the subquery name
     * @return The query compilation
     */
    public abstract QueryCompilation compile(Map parameters, Map subqueryMap);

    /**
     * Compile the candidates, variables and parameters.
     * @param parameters Map of parameter values keyed by their name
     */
    public void compileCandidatesParametersVariables(Map parameters)
    {
        compileCandidates();
        compileVariables();
        compileParameters();
    }

    /**
     * Method to compile the "from" clause (if present for the query language).
     * @return The compiled from expression(s)
     */
    protected Expression[] compileFrom()
    {
        if (from == null)
        {
            return null;
        }

        Node[] node = parser.parseFrom(from);
        Expression[] expr = new Expression[node.length];
        for (int i = 0; i < node.length; i++)
        {
            String className = (String)node[i].getNodeValue();
            String classAlias = null;
            Class cls = null;
            if (parentCompiler != null)
            {
                cls = getClassForSubqueryClassExpression(className);
            }
            else
            {
                cls = resolveClass(className);
            }

            List children = node[i].getChildNodes();
            for (int j=0;j<children.size();j++)
            {
                Node child = (Node)children.get(j);
                if (child.getNodeType() == NodeType.NAME) // Alias - maybe should assume it is the first child
                {
                    classAlias = (String)child.getNodeValue();
                }
            }

            if (i == 0 && classAlias == null)
            {
                throw new QueryCompilerSyntaxException("FROM clause of query has class " + cls.getName() + " but no alias");
            }

            if (classAlias != null)
            {
                if (i == 0)
                {
                    // First expression so set up candidateClass/alias
                    candidateClass = cls;
                    if (parentCompiler != null && parentCompiler.candidateAlias.equals(classAlias))
                    {
                        // The defined alias is the same as the parent query, so rename
                        candidateAliasOrig = classAlias;
                        candidateAlias = "sub_" + candidateAlias;
                        classAlias = candidateAlias;
                        swapCandidateAliasNodeName(node[i].getChildNode(0));
                    }
                    else
                    {
                        candidateAlias = classAlias;
                    }
                }
                if (symtbl.getSymbol(classAlias) == null)
                {
                    // Add symbol for this candidate under its alias
                    symtbl.addSymbol(new PropertySymbol(classAlias, cls));
                }
            }

            Iterator childIter = node[i].getChildNodes().iterator();
            while (childIter.hasNext())
            {
                // Add entries in symbol table for any joined aliases
                Node childNode = (Node)childIter.next();
                if (childNode.getNodeType() == NodeType.OPERATOR)
                {
                    Node joinedNode = childNode.getFirstChild();

                    // Extract alias node
                    Node aliasNode = childNode.getNextChild();

                    // Extract ON node (if present)
                    Node onExprNode = null;
                    if (childNode.hasNextChild())
                    {
                        onExprNode = childNode.getNextChild();
                    }

                    String joinedAlias = (String)joinedNode.getNodeValue();
                    Symbol joinedSym = caseSensitiveAliases ? symtbl.getSymbol(joinedAlias) : symtbl.getSymbolIgnoreCase(joinedAlias);
                    if (joinedSym == null)
                    {
                        // DN Extension : Check for FROM clause including join to root
                        if (childNode.hasNextChild())
                        {
                            Node next = childNode.getNextChild();
                            joinedAlias = (String)next.getNodeValue();
                            cls = resolveClass((String)joinedNode.getNodeValue());
                            if (symtbl.getSymbol(joinedAlias) == null)
                            {
                                // Add symbol for this candidate under its alias
                                symtbl.addSymbol(new PropertySymbol(joinedAlias, cls));
                            }
                            joinedSym = caseSensitiveAliases ? symtbl.getSymbol(joinedAlias) : symtbl.getSymbolIgnoreCase(joinedAlias);
                            NucleusLogger.QUERY.debug("Found suspected ROOT node joined to in FROM clause : attempting to process as alias=" + joinedAlias);
                        }

                        if (joinedSym == null)
                        {
                            throw new QueryCompilerSyntaxException("FROM clause has identifier " + joinedNode.getNodeValue() + " but this is unknown");
                        }
                    }

                    AbstractClassMetaData joinedCmd = metaDataManager.getMetaDataForClass(joinedSym.getValueType(), clr);
                    Class joinedCls = joinedSym.getValueType();
                    AbstractMemberMetaData joinedMmd = null;
                    while (joinedNode.getFirstChild() != null)
                    {
                        joinedNode = joinedNode.getFirstChild();
                        String joinedMember = (String)joinedNode.getNodeValue();
                        if (joinedNode.getNodeType() == NodeType.CAST)
                        {
                            // JOIN to "TREAT(identifier AS subcls)"
                            String castTypeName = (String)joinedNode.getNodeValue();
                            if (castTypeName.indexOf('.') < 0)
                            {
                                // Fully-qualify with the current class name?
                                castTypeName = ClassUtils.createFullClassName(joinedCmd.getPackageName(), castTypeName);
                            }
                            joinedCls = clr.classForName(castTypeName);
                            joinedNode.setNodeValue(castTypeName); // Update cast type now that we have resolved it
                        }
                        else
                        {
                            // Allow for multi-field joins
                            String[] joinedMembers = joinedMember.contains(".") ? StringUtils.split(joinedMember, ".") : new String[] {joinedMember};
                            for (int k=0;k<joinedMembers.length;k++)
                            {
                                AbstractMemberMetaData mmd = joinedCmd.getMetaDataForMember(joinedMembers[k]);
                                if (mmd == null)
                                {
                                    if (childNode.getNodeValue().equals(JOIN_OUTER) || childNode.getNodeValue().equals(JOIN_OUTER_FETCH))
                                    {
                                        // Polymorphic join, where the field exists in a subclass (doable since we have outer join)
                                        String[] subclasses = metaDataManager.getSubclassesForClass(joinedCmd.getFullClassName(), true);
                                        if (subclasses != null)
                                        {
                                            for (int l=0;l<subclasses.length;l++)
                                            {
                                                AbstractClassMetaData subCmd = metaDataManager.getMetaDataForClass(subclasses[l], clr);
                                                if (subCmd != null)
                                                {
                                                    mmd = subCmd.getMetaDataForMember(joinedMembers[k]);
                                                    if (mmd != null)
                                                    {
                                                        NucleusLogger.QUERY.debug("Polymorphic join found at " + joinedMembers[k] + " of " + subCmd.getFullClassName());
                                                        joinedCmd = subCmd;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (mmd == null)
                                    {
                                        throw new QueryCompilerSyntaxException("FROM clause has reference to " + joinedCmd.getFullClassName() + "." + joinedMembers[k] + " but it doesn't exist!");
                                    }
                                }

                                RelationType relationType = mmd.getRelationType(clr);
                                joinedMmd = mmd;
                                if (RelationType.isRelationSingleValued(relationType))
                                {
                                    joinedCls = mmd.getType();
                                    joinedCmd = metaDataManager.getMetaDataForClass(joinedCls, clr);
                                }
                                else if (RelationType.isRelationMultiValued(relationType))
                                {
                                    if (mmd.hasCollection())
                                    {
                                        // TODO Don't currently allow interface field navigation
                                        joinedCmd = mmd.getCollection().getElementClassMetaData(clr);
                                        joinedCls = clr.classForName(joinedCmd.getFullClassName());
                                    }
                                    else if (mmd.hasMap())
                                    {
                                        joinedCmd = mmd.getMap().getValueClassMetaData(clr);
                                        if (joinedCmd != null)
                                        {
                                            // JPA assumption that the value is an entity ... but it may not be!
                                            joinedCls = clr.classForName(joinedCmd.getFullClassName());
                                        }
                                    }
                                    else if (mmd.hasArray())
                                    {
                                        // TODO Don't currently allow interface field navigation
                                        joinedCmd = mmd.getArray().getElementClassMetaData(clr);
                                        joinedCls = clr.classForName(joinedCmd.getFullClassName());
                                    }
                                }
                            }
                        }
                    }

                    if (aliasNode.getNodeType() == NodeType.NAME)
                    {
                        // Add JOIN alias to symbol table
                        String alias = (String)aliasNode.getNodeValue();
                        symtbl.addSymbol(new PropertySymbol(alias, joinedCls));
                        if (joinedMmd != null && joinedMmd.hasMap())
                        {
                            Class keyCls = clr.classForName(joinedMmd.getMap().getKeyType());
                            symtbl.addSymbol(new PropertySymbol(alias + "#KEY", keyCls)); // Add the KEY so that we can have joins to the key from the value alias
                            Class valueCls = clr.classForName(joinedMmd.getMap().getValueType());
                            symtbl.addSymbol(new PropertySymbol(alias + "#VALUE", valueCls)); // Add the VALUE so that we can have joins to the value from the key alias
                        }
                    }

                    if (onExprNode != null)
                    {
                        // ON condition
                        ExpressionCompiler comp = new ExpressionCompiler();
                        comp.setSymbolTable(symtbl);
                        comp.setMethodAliases(queryMethodAliasByPrefix);
                        Expression nextExpr = comp.compileExpression(onExprNode);
                        nextExpr.bind(symtbl);
                    }
                }
            }

            boolean classIsExpression = false;
            String[] tokens = StringUtils.split(className, ".");
            if (symtbl.getParentSymbolTable() != null)
            {
                if (symtbl.getParentSymbolTable().hasSymbol(tokens[0]))
                {
                    classIsExpression = true;
                }
            }

            ExpressionCompiler comp = new ExpressionCompiler();
            comp.setSymbolTable(symtbl);
            comp.setMethodAliases(queryMethodAliasByPrefix);
            expr[i] = comp.compileFromExpression(node[i], classIsExpression);
            if (expr[i] != null)
            {
                expr[i].bind(symtbl);
            }
        }
        return expr;
    }

    /**
     * Convenience method to find the class that a subquery class expression refers to.
     * Allows for reference to the parent query candidate class, or to a class name.
     * @param classExpr The class expression
     * @return The class that it refers to
     */
    private Class getClassForSubqueryClassExpression(String classExpr)
    {
        if (classExpr == null)
        {
            return null;
        }

        String[] tokens = StringUtils.split(classExpr, ".");
        Class cls = null;
        if (tokens[0].equalsIgnoreCase(parentCompiler.candidateAlias))
        {
            // Starts with candidate of parent query
            cls = parentCompiler.candidateClass;
        }
        else
        {
            // Try alias from parent query
            Symbol sym = parentCompiler.symtbl.getSymbolIgnoreCase(tokens[0]);
            if (sym != null)
            {
                cls = sym.getValueType();
            }
            else
            {
                // Must be a class name
                return resolveClass(classExpr);
            }
        }

        AbstractClassMetaData cmd = metaDataManager.getMetaDataForClass(cls, clr);
        for (int i=1;i<tokens.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForMember(tokens[i]);
            RelationType relationType = mmd.getRelationType(clr);
            if (RelationType.isRelationSingleValued(relationType))
            {
                cls = mmd.getType();
            }
            else if (RelationType.isRelationMultiValued(relationType))
            {
                if (mmd.hasCollection())
                {
                    cls = clr.classForName(mmd.getCollection().getElementType());
                }
                else if (mmd.hasMap())
                {
                    // Assume we're using the value
                    cls = clr.classForName(mmd.getMap().getValueType());
                }
                else if (mmd.hasArray())
                {
                    cls = clr.classForName(mmd.getArray().getElementType());
                }
            }

            if (i < tokens.length-1)
            {
                cmd = metaDataManager.getMetaDataForClass(cls, clr);
            }
        }

        return cls;
    }

    private void compileCandidates()
    {
        if (symtbl.getSymbol(candidateAlias) == null)
        {
            // Add candidate symbol if not already present (from "compileFrom")
            if (parentCompiler != null && parentCompiler.candidateAlias.equals(candidateAlias))
            {
                candidateAliasOrig = candidateAlias;
                candidateAlias = "sub_" + candidateAlias;
            }
            PropertySymbol symbol = new PropertySymbol(candidateAlias, candidateClass);
            symtbl.addSymbol(symbol);
        }
    }

    public Expression[] compileUpdate()
    {
        if (update == null)
        {
            return null;
        }
        Node[] node = parser.parseTuple(update);
        Expression[] expr = new Expression[node.length];
        for (int i = 0; i < node.length; i++)
        {
            ExpressionCompiler comp = new ExpressionCompiler();
            comp.setSymbolTable(symtbl);
            comp.setMethodAliases(queryMethodAliasByPrefix);
            expr[i] = comp.compileExpression(node[i]);
            expr[i].bind(symtbl);
        }
        return expr;
    }

    /**
     * Compile the filter and return the compiled expression.
     * @return The compiled expression
     */
    public Expression compileFilter()
    {
        if (filter != null)
        {
            // Generate the node tree for the filter
            Node node = parser.parse(filter);
            if (candidateAliasOrig != null)
            {
                swapCandidateAliasNodeName(node);
            }
            if (parameterSubtitutionMap != null)
            {
                node = swapSubqueryParameters(node);
            }

            ExpressionCompiler comp = new ExpressionCompiler();
            comp.setSymbolTable(symtbl);
            comp.setMethodAliases(queryMethodAliasByPrefix);
            Expression expr = comp.compileExpression(node);
            expr.bind(symtbl);
            return expr;
        }
        return null;
    }

    /**
     * Convenience method that takes the input node and if it is set to the original candidate alias
     * then swaps the value to the candidate alias. This is for the situation where we have a subquery
     * which had no alias defined so defaults to "this" (the same as the outer query), so we change
     * the subquery alias from "this" to "sub_this".
     * @param node The node to process
     */
    protected void swapCandidateAliasNodeName(Node node)
    {
        if (node == null)
        {
            return;
        }

        switch (node.getNodeType())
        {
            case IDENTIFIER :
                if (node.getNodeValue().equals(candidateAliasOrig))
                {
                    node.setNodeValue(candidateAlias);
                }
                break;
            case OPERATOR :
                while (node.hasNextChild())
                {
                    Node childNode = node.getNextChild();
                    swapCandidateAliasNodeName(childNode);
                }
                break;
            case INVOKE :
                if (node.hasProperties())
                {
                    Iterator<Node> propIter = node.getProperties().iterator();
                    while (propIter.hasNext())
                    {
                        Node propNode = propIter.next();
                        swapCandidateAliasNodeName(propNode);
                    }
                }
                break;
            case CAST :
                Node childNode = node.getChildNode(0);
                swapCandidateAliasNodeName(childNode);
                break;
            case NAME :
                if (node.getNodeValue().equals(candidateAliasOrig))
                {
                    node.setNodeValue(candidateAlias);
                }
                break;

            case CLASS :
            case CASE :
            case PARAMETER :
            case SUBQUERY :
            case LITERAL :
                break;
            default :
                // TODO Update other node types
                break;
        }
    }

    /**
     * Convenience method that takes the input node if it is a parameter node swaps the node to be 
     * @param node The node to process
     * @return The Node with the swap
     */
    protected Node swapSubqueryParameters(Node node)
    {
        if (node == null || parameterSubtitutionMap == null)
        {
            return null;
        }

        Node swapNode = null;
        switch (node.getNodeType())
        {
            case PARAMETER :
                // Swap the parameter for node(s) for the value
                Object paramName = node.getNodeValue();
                if (parameterSubtitutionMap.containsKey(paramName))
                {
                    String paramValue = parameterSubtitutionMap.get(paramName);
                    swapNode = parser.parse(paramValue);
                }
                else
                {
                    // Positional subquery parameters, starting at 0
                    String paramValue = parameterSubtitutionMap.get(Integer.valueOf(parameterSubstitutionNumber++));
                    swapNode = parser.parse(paramValue);
                }
                return swapNode;
            case OPERATOR :
                List childNodes = node.getChildNodes();
                for (int i=0;i<childNodes.size();i++)
                {
                    Node swappedNode = swapSubqueryParameters((Node) childNodes.get(i));
                    node.removeChildNode((Node) childNodes.get(i));
                    node.insertChildNode(swappedNode, i);
                }
                break;
            case INVOKE :
                if (node.hasProperties())
                {
                    List<Node> propNodes = node.getProperties();
                    for (int i=0;i<propNodes.size();i++)
                    {
                        Node propNode = propNodes.get(i);
                        swapNode = swapSubqueryParameters(propNode);
                        if (swapNode != propNode)
                        {
                            node.setPropertyAtPosition(i, swapNode);
                        }
                    }
                }
                break;

            // TODO Handle other types of nodes
            default :
                break;
        }
        return node;
    }

    public Expression[] compileResult()
    {
        if (result == null)
        {
            return null;
        }

        Node[] node = parser.parseResult(result);
        Expression[] expr = new Expression[node.length];
        for (int i = 0; i < node.length; i++)
        {
            ExpressionCompiler comp = new ExpressionCompiler();
            comp.setSymbolTable(symtbl);
            comp.setMethodAliases(queryMethodAliasByPrefix);

            String alias = null;
            Node aliasNode = null;
            while (node[i].hasNextChild())
            {
                Node childNode = node[i].getNextChild();
                if (childNode.getNodeType() == NodeType.NAME)
                {
                    // Alias node
                    aliasNode = childNode;
                }
            }
            if (aliasNode != null)
            {
                alias = (String)aliasNode.getNodeValue();
                node[i].removeChildNode(aliasNode);
            }
            if (candidateAliasOrig != null)
            {
                swapCandidateAliasNodeName(node[i]);
            }
            if (parameterSubtitutionMap != null)
            {
                node[i] = swapSubqueryParameters(node[i]);
            }

            expr[i] = comp.compileExpression(node[i]);
            if (alias != null)
            {
                expr[i].setAlias(alias);
            }
            try
            {
                expr[i].bind(symtbl);
            }
            catch (PrimaryExpressionIsClassLiteralException peil)
            {
                // PrimaryExpression should be swapped for a class Literal
                expr[i] = peil.getLiteral();
                expr[i].bind(symtbl);
            }
            catch (PrimaryExpressionIsClassStaticFieldException peil)
            {
                // PrimaryExpression should be swapped for a static field Literal
                Field fld = peil.getLiteralField();
                try
                {
                    // Get the value of the static field
                    Object value = fld.get(null);
                    expr[i] = new Literal(value);
                    expr[i].bind(symtbl);
                }
                catch (Exception e)
                {
                    throw new NucleusUserException("Error processing static field " + fld.getName(), e);
                }
            }
            catch (PrimaryExpressionIsVariableException pive)
            {
                // PrimaryExpression should be swapped for an implicit variable
                expr[i] = pive.getVariableExpression();
                expr[i].bind(symtbl);
            }

            if (expr[i] instanceof PrimaryExpression)
            {
                String id = ((PrimaryExpression)expr[i]).getId();
                if (isKeyword(id))
                {
                    throw new NucleusUserException(Localiser.msg("021052", getLanguage(), id));
                }
            }
            else if (expr[i] instanceof ParameterExpression)
            {
                String id = ((ParameterExpression)expr[i]).getId();
                if (isKeyword(id))
                {
                    throw new NucleusUserException(Localiser.msg("021052", getLanguage(), id));
                }
            }
            else if (expr[i] instanceof VariableExpression)
            {
                String id = ((VariableExpression)expr[i]).getId();
                if (isKeyword(id))
                {
                    throw new NucleusUserException(Localiser.msg("021052", getLanguage(), id));
                }
            }
        }

        return expr;
    }

    public Expression[] compileGrouping()
    {
        if (grouping == null)
        {
            return null;
        }
        Node[] node = parser.parseTuple(grouping);
        Expression[] expr = new Expression[node.length];
        for (int i = 0; i < node.length; i++)
        {
            if (candidateAliasOrig != null)
            {
                swapCandidateAliasNodeName(node[i]);
            }
            if (parameterSubtitutionMap != null)
            {
                node[i] = swapSubqueryParameters(node[i]);
            }

            ExpressionCompiler comp = new ExpressionCompiler();
            comp.setSymbolTable(symtbl);
            comp.setMethodAliases(queryMethodAliasByPrefix);
            expr[i] = comp.compileExpression(node[i]);
            expr[i].bind(symtbl);
        }
        return expr;
    }

    public Expression compileHaving()
    {
        if (having == null)
        {
            return null;
        }

        Node node = parser.parse(having);
        if (candidateAliasOrig != null)
        {
            swapCandidateAliasNodeName(node);
        }
        if (parameterSubtitutionMap != null)
        {
            node = swapSubqueryParameters(node);
        }

        ExpressionCompiler comp = new ExpressionCompiler();
        comp.setSymbolTable(symtbl);
        comp.setMethodAliases(queryMethodAliasByPrefix);
        Expression expr = comp.compileExpression(node);
        expr.bind(symtbl);
        return expr;
    }

    private void compileVariables()
    {
        if (variables == null)
        {
            return;
        }

        Node[][] node = parser.parseVariables(variables);
        for (int i = 0; i < node.length; i++)
        {
            String varName = (String) node[i][1].getNodeValue();
            if (isKeyword(varName) || varName.equals(candidateAlias))
            {
                throw new NucleusUserException(Localiser.msg("021052", getLanguage(), varName));
            }
            Symbol varSym = symtbl.getSymbol(varName);
            Class nodeCls = resolveClass(node[i][0].getNodeChildId());
            if (varSym != null)
            {
                if (nodeCls != null)
                {
                    // Update the value type
                    varSym.setValueType(nodeCls);
                }
            }
            else
            {
                PropertySymbol sym = new PropertySymbol(varName, nodeCls);
                sym.setType(Symbol.VARIABLE);
                symtbl.addSymbol(sym);
            }
        }
    }

    private void compileParameters()
    {
        if (parameters == null)
        {
            return;
        }

        Node[][] node = parser.parseParameters(parameters);
        for (int i = 0; i < node.length; i++)
        {
            String paramName = (String) node[i][1].getNodeValue();
            if (isKeyword(paramName) || paramName.equals(candidateAlias))
            {
                throw new NucleusUserException(Localiser.msg("021052", getLanguage(), paramName));
            }

            Symbol paramSym = symtbl.getSymbol(paramName);
            Class nodeCls = resolveClass(node[i][0].getNodeChildId());
            if (paramSym != null)
            {
                // TODO Update the type ?
            }
            else
            {
                PropertySymbol sym = new PropertySymbol(paramName, nodeCls);
                sym.setType(Symbol.PARAMETER);
                symtbl.addSymbol(sym);
            }
        }
    }

    public Expression[] compileOrdering()
    {
        if (ordering == null)
        {
            return null;
        }

        Node[] node = parser.parseOrder(ordering);
        Expression[] expr = new Expression[node.length];
        for (int i = 0; i < node.length; i++)
        {
            if (candidateAliasOrig != null)
            {
                swapCandidateAliasNodeName(node[i]);
            }
            if (parameterSubtitutionMap != null)
            {
                node[i] = swapSubqueryParameters(node[i]);
            }

            ExpressionCompiler comp = new ExpressionCompiler();
            comp.setSymbolTable(symtbl);
            comp.setMethodAliases(queryMethodAliasByPrefix);
            expr[i] = comp.compileOrderExpression(node[i]);
            expr[i].bind(symtbl);
        }
        return expr;
    }

    public Class getPrimaryClass()
    {
        return candidateClass;
    }

    /**
     * Method to perform a lookup of the class name from the input name.
     * Makes use of the query "imports" and the lookup to "entity name".
     * @param className Name of the class
     * @return The class corresponding to this name
     * @throws ClassNotResolvedException thrown if not resolvable using imports or entity name
     */
    public Class resolveClass(String className)
    {
        if (imports != null)
        {
            // Try using the imports
            try
            {
                Class cls = imports.resolveClassDeclaration(className, clr, null);
                if (cls != null)
                {
                    return cls;
                }
            }
            catch (NucleusException ne)
            {
                // Ignore
            }
        }

        // Try via "entity name"
        AbstractClassMetaData acmd = metaDataManager.getMetaDataForEntityName(className);
        if (acmd != null)
        {
            String fullClassName = acmd.getFullClassName();
            if (fullClassName != null)
            {
                return clr.classForName(fullClassName);
            }
        }

        throw new ClassNotResolvedException("Class " + className + " for query has not been resolved. Check the query and any imports/aliases specification");
    }

    public Class getType(List tuples)
    {
        Class type = null;
        Symbol symbol = null;
        String firstTuple = (String)tuples.get(0);
        if (caseSensitiveSymbolNames())
        {
            symbol = symtbl.getSymbol(firstTuple);
        }
        else
        {
            symbol = symtbl.getSymbol(firstTuple);
            if (symbol == null)
            {
                symbol = symtbl.getSymbol(firstTuple.toUpperCase());
            }
            if (symbol == null)
            {
                symbol = symtbl.getSymbol(firstTuple.toLowerCase());
            }
        }
        int tupleSize = tuples.size();
        if (symbol != null)
        {
            type = symbol.getValueType();
            if (type == null)
            {
                // Implicit variables don't have their type defined
                throw new NucleusUserException("Cannot find type of " + tuples.get(0) + " since symbol has no type; implicit variable?");
            }

            for (int i=1; i<tupleSize; i++)
            {
                type = getType(type, (String)tuples.get(i), i == tupleSize-1);
            }
        }
        else
        {
            symbol = symtbl.getSymbol(candidateAlias);
            type = symbol.getValueType();
            for (int i=0; i<tupleSize; i++)
            {
                type = getType(type, (String)tuples.get(i), i == tupleSize-1);
            }
        }
        return type;
    }

    Class getType(Class cls, String fieldName, boolean lastEntry)
    {
        AbstractClassMetaData acmd = metaDataManager.getMetaDataForClass(cls, clr);
        if (acmd != null)
        {
            AbstractMemberMetaData fmd = acmd.getMetaDataForMember(fieldName);
            if (fmd == null)
            {
                throw new NucleusUserException("Cannot access field "+fieldName+" on type "+cls.getName());
            }
            
            // Nested properties on single element collections can be referred as regular fields, 
            // e.g. person.address.street, where address is a single collection. If the expression refers to 
            // the property inside the container it returns the element type, on the other hand if
            // the expression is resolving the collection it returns the collection type.
            boolean nestedSingleElementProperty = !lastEntry && fmd.isSingleCollection();
            
            return nestedSingleElementProperty ? clr.classForName(fmd.getCollection().getElementType()) : fmd.getType();
        }

        Field field = ClassUtils.getFieldForClass(cls, fieldName);
        if (field == null)
        {
            throw new NucleusUserException("Cannot access field "+fieldName+" on type "+cls.getName());
        }
        return field.getType();
    }

    /**
     * Method to return if the supplied name is a keyword.
     * Keywords can only appear at particular places in a query so we need to detect for valid queries.
     * @param name The name
     * @return Whether it is a keyword
     */
    protected abstract boolean isKeyword(String name);
}