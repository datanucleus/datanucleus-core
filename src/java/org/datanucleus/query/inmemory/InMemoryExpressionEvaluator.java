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
2008 Andy Jefferson - support for all methods
2008 Andy Jefferson - support for "+", "-", "/", "%", ">", "<", ">=", "<=", "instanceof", !, ~
2008 Andy Jefferson - support for implicit parameters
2008 Andy Jefferson - support for chained PrimaryExpression/InvokeExpressions
    ...
**********************************************************************/
package org.datanucleus.query.inmemory;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.query.QueryUtils;
import org.datanucleus.query.evaluator.AbstractExpressionEvaluator;
import org.datanucleus.query.evaluator.JavaQueryEvaluator;
import org.datanucleus.query.expression.ArrayExpression;
import org.datanucleus.query.expression.CaseExpression;
import org.datanucleus.query.expression.CreatorExpression;
import org.datanucleus.query.expression.DyadicExpression;
import org.datanucleus.query.expression.Expression;
import org.datanucleus.query.expression.InvokeExpression;
import org.datanucleus.query.expression.Literal;
import org.datanucleus.query.expression.ParameterExpression;
import org.datanucleus.query.expression.PrimaryExpression;
import org.datanucleus.query.expression.VariableExpression;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.query.QueryManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Imports;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Class providing evaluation of java "string-based" queries in-memory.
 */
public class InMemoryExpressionEvaluator extends AbstractExpressionEvaluator
{
    /** Localisation utility for output messages */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    String queryLanguage = null;

    Stack stack = new Stack();

    /** Map of input parameter values, keyed by their name. */
    Map parameterValues;

    /** Map of variable values, keyed by the variable name. Set during execution. */
    Map<String, Object> variableValues;

    /** Map of state variables for query evaluation. */
    Map<String, Object> state;

    Imports imports;

    ExecutionContext ec;

    ClassLoaderResolver clr;

    QueryManager queryMgr;

    /** Alias name for the candidate. */
    final String candidateAlias;

    /**
     * Constructor for an in-memory evaluator.
     * @param ec ExecutionContext
     * @param params Input parameters
     * @param state Map of state values keyed by their symbolic name
     * @param imports Any imports
     * @param clr ClassLoader resolver 
     * @param candidateAlias Alias for the candidate class. With JDOQL this is "this".
     * @param queryLang Query language (JDOQL, JPQL etc)
     */
    public InMemoryExpressionEvaluator(ExecutionContext ec, Map params, Map<String, Object> state, 
            Imports imports, ClassLoaderResolver clr, String candidateAlias, String queryLang)
    {
        this.ec = ec;
        this.queryMgr = ec.getStoreManager().getQueryManager();
        this.parameterValues = (params != null ? params : new HashMap());
        this.state = state;
        this.imports = imports;
        this.clr = clr;
        this.candidateAlias = candidateAlias;
        this.queryLanguage = queryLang;
    }

    public Localiser getLocaliser()
    {
        return LOCALISER;
    }

    public Map getParameterValues()
    {
        return parameterValues;
    }

    public String getQueryLanguage()
    {
        return queryLanguage;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processAndExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processAndExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        stack.push((left == Boolean.TRUE && right == Boolean.TRUE) ? Boolean.TRUE : Boolean.FALSE);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processEqExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processEqExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        Boolean result = QueryUtils.compareExpressionValues(left, right, expr.getOperator()) ? Boolean.TRUE : Boolean.FALSE;
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processLikeExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processLikeExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        if (!(left instanceof String))
        {
            throw new NucleusUserException(
                "LIKE expression can only be used on a String expression, but found on " + 
                left.getClass().getName());
        }
        if (right instanceof String)
        {
            // Just use String.matches(String)
            Boolean result = ((String)left).matches((String)right) ? Boolean.TRUE : Boolean.FALSE;
            stack.push(result);
            return result;
        }
        else
        {
            throw new NucleusUserException(
                "Dont currently support expression on right of LIKE to be other than String but was " + 
                right.getClass().getName());
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processNoteqExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processNoteqExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        Boolean result = QueryUtils.compareExpressionValues(left, right, expr.getOperator()) ? Boolean.TRUE : Boolean.FALSE;
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processOrExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processOrExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        stack.push((left == Boolean.TRUE || right == Boolean.TRUE) ? Boolean.TRUE : Boolean.FALSE);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processGteqExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processGteqExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        Boolean result = QueryUtils.compareExpressionValues(left, right, expr.getOperator()) ? Boolean.TRUE : Boolean.FALSE;
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processGtExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processGtExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        Boolean result = QueryUtils.compareExpressionValues(left, right, expr.getOperator()) ? Boolean.TRUE : Boolean.FALSE;
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processIsExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processIsExpression(Expression expr)
    {
        // field instanceof className
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        if (!(right instanceof Class))
        {
            throw new NucleusException("Attempt to invoke instanceof with argument of type " + 
                right.getClass().getName() + " has to be Class");
        }
        try
        {
            Boolean result = ((Class)right).isAssignableFrom(left.getClass()) ? Boolean.TRUE : Boolean.FALSE;
            stack.push(result);
            return result;
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new NucleusException("Attempt to invoke instanceof with " + 
                right + " yet class was not found!");
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processIsnotExpression(org.datanucleus.query.expression.Expression)
     */
    @Override
    protected Object processIsnotExpression(Expression expr)
    {
        processIsExpression(expr);
        Boolean val = (Boolean)stack.pop();
        val = !val;
        stack.push(val);
        return val;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processCastExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processCastExpression(Expression expr)
    {
        // field instanceof className
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }

        throw new NucleusException("CAST not yet supported in in-memory evaluator");
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processLteqExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processLteqExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        Boolean result = QueryUtils.compareExpressionValues(left, right, expr.getOperator()) ? Boolean.TRUE : Boolean.FALSE;
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processLtExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processLtExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        if (left instanceof InMemoryFailure || right instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }
        Boolean result = QueryUtils.compareExpressionValues(left, right, expr.getOperator()) ? Boolean.TRUE : Boolean.FALSE;
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processAddExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processAddExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        Object value = null;
        if (right instanceof String && left instanceof String)
        {
            value = "" + left + right;
        }
        else if (right instanceof Number && left instanceof Number)
        {
            value = new BigDecimal(left.toString()).add(new BigDecimal(right.toString()));
        }
        else if (left instanceof String)
        {
            value = "" + left + right;
        }
        else
        {
            throw new NucleusException("Performing ADD operation on " + left + " and " + right + " is not supported");
        }
        stack.push(value);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processSubExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processSubExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        Object value = new BigDecimal(left.toString()).subtract(new BigDecimal(right.toString()));
        stack.push(value);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processDivExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processDivExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        double firstValue = new BigDecimal(left.toString()).doubleValue();
        double secondValue = new BigDecimal(right.toString()).doubleValue();
        BigDecimal value = new BigDecimal(firstValue/secondValue);
        stack.push(value);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processModExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processModExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        BigDecimal firstValue = new BigDecimal(left.toString());
        BigDecimal divisor = new BigDecimal(right.toString());
        Object value = firstValue.subtract(firstValue.divideToIntegralValue(divisor).multiply(divisor));
        stack.push(value);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processMulExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processMulExpression(Expression expr)
    {
        Object right = stack.pop();
        Object left = stack.pop();
        Object value = new BigDecimal(left.toString()).multiply(new BigDecimal(right.toString()));
        stack.push(value);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processNegExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processNegExpression(Expression expr)
    {
        Number val = null;
        if (expr instanceof DyadicExpression)
        {
            DyadicExpression dyExpr = (DyadicExpression)expr;
            if (dyExpr.getLeft() instanceof PrimaryExpression)
            {
                val = (Number)getValueForPrimaryExpression((PrimaryExpression)expr.getLeft());
            }
            else if (dyExpr.getLeft() instanceof ParameterExpression)
            {
                val = (Number)QueryUtils.getValueForParameterExpression(parameterValues, (ParameterExpression)expr.getLeft());
            }
            else
            {
                throw new NucleusException("No current support for negation of dyadic expression on type " + 
                    dyExpr.getLeft().getClass().getName());
            }
        }
        else if (expr instanceof Literal)
        {
            throw new NucleusException("No current support for negation of expression of type Literal");
        }
        else
        {
            throw new NucleusException("No current support for negation of expression of type " + 
                expr.getClass().getName());
        }

        // Other types?
        if (val instanceof Integer)
        {
            stack.push(Integer.valueOf(-val.intValue()));
            return stack.peek();
        }
        else if (val instanceof Long)
        {
            stack.push(Long.valueOf(-val.longValue()));
            return stack.peek();
        }
        else if (val instanceof Short)
        {
            stack.push(Short.valueOf((short)-val.shortValue()));
            return stack.peek();
        }
        else if (val instanceof BigInteger)
        {
            stack.push(BigInteger.valueOf(-val.longValue()));
            return stack.peek();
        }
        else if (val instanceof Double)
        {
            stack.push(Double.valueOf(-val.doubleValue()));
            return stack.peek();
        }
        else if (val instanceof Float)
        {
            stack.push(Float.valueOf(-val.floatValue()));
            return stack.peek();
        }
        else if (val instanceof BigDecimal)
        {
            stack.push(new BigDecimal(-val.doubleValue()));
            return stack.peek();
        }
        else
        {
            throw new NucleusException("Attempt to negate value of type " + val + " not supported");
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processComExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processComExpression(Expression expr)
    {
        // Bitwise complement - only for integer values
        PrimaryExpression primExpr = (PrimaryExpression)expr.getLeft();
        Object primVal = getValueForPrimaryExpression(primExpr);
        int val = -1;
        if (primVal instanceof Number)
        {
            val = ((Number)primVal).intValue();
        }
        Integer result = Integer.valueOf(~val);
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processNotExpression(org.datanucleus.query.expression.Expression)
     */
    protected Object processNotExpression(Expression expr)
    {
        // Logical complement - only for boolean values
        Object left = stack.pop();
        if (left instanceof InMemoryFailure)
        {
            stack.push(Boolean.FALSE);
            return stack.peek();
        }

        Boolean leftExpr = (Boolean)left;
        Boolean result = (leftExpr.booleanValue() ? Boolean.FALSE : Boolean.TRUE);
        stack.push(result);
        return stack.peek();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processCreatorExpression(org.datanucleus.query.expression.CreatorExpression)
     */
    protected Object processCreatorExpression(CreatorExpression expr)
    {
        List params = new ArrayList();
        for (int i = 0; i < expr.getArguments().size(); i++)
        {
            params.add((expr.getArguments().get(i)).evaluate(this));
        }
        Class cls = imports.resolveClassDeclaration(expr.getId(), clr, null);
        Object value = QueryUtils.createResultObjectUsingArgumentedConstructor(cls, params.toArray(), null);
        stack.push(value);
        // TODO What about CreateExpression.InvokeExpression or CreateExpression.PrimaryExpression ?
        return value;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processInvokeExpression(org.datanucleus.query.expression.InvokeExpression)
     */
    protected Object processInvokeExpression(InvokeExpression expr)
    {
        // Process expressions like :-
        // a). aggregates : count(...), avg(...), sum(...), min(...), max(...)
        // b). methods/functions : FUNCTION(...), field.method(...)
        Object result = getValueForInvokeExpression(expr);
        stack.push(result);
        return result;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processLiteral(org.datanucleus.query.expression.Literal)
     */
    protected Object processLiteral(Literal expr)
    {
        Object value = expr.getLiteral();
        stack.push(value);
        return value;
    }

    /**
     * Method to process the supplied variable expression.
     * To be implemented by subclasses.
     * @param expr The expression
     * @return The result
     */
    protected Object processVariableExpression(VariableExpression expr)
    {
        if (expr.getLeft() == null && state.containsKey(expr.getId()))
        {
            // Variable defined
            Object value = state.get(expr.getId());
            if (value == null)
            {
                NucleusLogger.QUERY.warn("Variable expression " + expr.getId() +
                    " doesnt have its value set yet. Unsupported query structure");
                value = new InMemoryFailure();
            }
            stack.push(value);
            return value;
        }
        return super.processVariableExpression(expr);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processParameterExpression(org.datanucleus.query.expression.ParameterExpression)
     */
    protected Object processParameterExpression(ParameterExpression expr)
    {
        Object value = QueryUtils.getValueForParameterExpression(parameterValues, expr);
        stack.push(value);
        return value;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processPrimaryExpression(org.datanucleus.query.expression.PrimaryExpression)
     */
    protected Object processPrimaryExpression(PrimaryExpression expr)
    {
        Object paramValue = (parameterValues != null ? parameterValues.get(expr.getId()) : null);
        if (expr.getLeft() == null && paramValue != null)
        {
            // Explicit Parameter
            stack.push(paramValue);
            return paramValue;
        }
        else
        {
            // Field
            Object value = getValueForPrimaryExpression(expr);
            stack.push(value);
            return value;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.query.evaluator.AbstractExpressionEvaluator#processCaseExpression(org.datanucleus.query.expression.CaseExpression)
     */
    @Override
    protected Object processCaseExpression(CaseExpression expr)
    {
        Map<Expression, Expression> exprs = expr.getConditions();
        Iterator<Entry<Expression, Expression>> entryIter = exprs.entrySet().iterator();
        while (entryIter.hasNext())
        {
            Entry<Expression, Expression> entry = entryIter.next();
            Expression keyExpr = entry.getKey();
            Expression valExpr = entry.getValue();

            Object keyResult = keyExpr.evaluate(this);
            if (keyResult instanceof Boolean)
            {
                if ((Boolean)keyResult)
                {
                    // This case clause resolves to true, so return its result
                    Object value = valExpr.evaluate(this);
                    stack.push(value);
                    return value;
                }
            }
            else
            {
                NucleusLogger.QUERY.error("Case expression " + expr + " clause " + keyExpr + " did not return boolean");
                Object value = new InMemoryFailure();
                stack.push(value);
                return value;
            }
        }

        // No case clause resolves to true, so return the else result
        Object value = expr.getElseExpression().evaluate(this);
        stack.push(value);
        return value;
    }

    /**
     * Method to evaluate an InvokeExpression.
     * Will navigate along chained invocations, evaluating the first one, then the second one etc
     * until it gets the value for the passed in expression.
     * @param invokeExpr The InvokeExpression
     * @return The value
     */
    public Object getValueForInvokeExpression(InvokeExpression invokeExpr)
    {
        String method = invokeExpr.getOperation();
        if (invokeExpr.getLeft() == null)
        {
            // Static function
            if (method.toLowerCase().equals("count"))
            {
                Collection coll = (Collection)state.get(JavaQueryEvaluator.RESULTS_SET);
                SetExpression setexpr = new SetExpression(coll, candidateAlias);
                Expression paramExpr = invokeExpr.getArguments().get(0);
                if (paramExpr.getOperator() == Expression.OP_DISTINCT)
                {
                    Collection processable = new HashSet(coll); // No dups in HashSet
                    coll = processable;
                }

                int stackSizeOrig = stack.size();
                Object returnVal = setexpr.count(paramExpr, this);
                while (stack.size() > stackSizeOrig)
                {
                    // Remove any expressions put on the stack while evaluating the aggregate
                    stack.pop();
                }
                return returnVal;
            }
            else if (method.toLowerCase().equals("sum"))
            {
                Collection coll = (Collection)state.get(JavaQueryEvaluator.RESULTS_SET);
                SetExpression setexpr = new SetExpression(coll, candidateAlias);
                Expression paramExpr = invokeExpr.getArguments().get(0);
                if (paramExpr.getOperator() == Expression.OP_DISTINCT)
                {
                    Collection processable = new HashSet(coll); // No dups in HashSet
                    coll = processable;
                }

                int stackSizeOrig = stack.size();
                Object returnVal = setexpr.sum(paramExpr, this, state);
                while (stack.size() > stackSizeOrig)
                {
                    // Remove any expressions put on the stack while evaluating the aggregate
                    stack.pop();
                }
                return returnVal;
            }
            else if (method.toLowerCase().equals("avg"))
            {
                Collection coll = (Collection)state.get(JavaQueryEvaluator.RESULTS_SET);
                SetExpression setexpr = new SetExpression(coll, candidateAlias);
                Expression paramExpr = invokeExpr.getArguments().get(0);
                if (paramExpr.getOperator() == Expression.OP_DISTINCT)
                {
                    Collection processable = new HashSet(coll); // No dups in HashSet
                    coll = processable;
                }

                int stackSizeOrig = stack.size();
                Object returnVal = setexpr.avg(paramExpr, this, state);
                while (stack.size() > stackSizeOrig)
                {
                    // Remove any expressions put on the stack while evaluating the aggregate
                    stack.pop();
                }
                return returnVal;
            }
            else if (method.toLowerCase().equals("min"))
            {
                Collection coll = (Collection)state.get(JavaQueryEvaluator.RESULTS_SET);
                SetExpression setexpr = new SetExpression(coll, candidateAlias);
                Expression paramExpr = invokeExpr.getArguments().get(0);
                if (paramExpr.getOperator() == Expression.OP_DISTINCT)
                {
                    Collection processable = new HashSet(coll); // No dups in HashSet
                    coll = processable;
                }

                int stackSizeOrig = stack.size();
                Object returnVal = setexpr.min(paramExpr, this, state);
                while (stack.size() > stackSizeOrig)
                {
                    // Remove any expressions put on the stack while evaluating the aggregate
                    stack.pop();
                }
                return returnVal;
            }
            else if (method.toLowerCase().equals("max"))
            {
                Collection coll = (Collection)state.get(JavaQueryEvaluator.RESULTS_SET);
                SetExpression setexpr = new SetExpression(coll, candidateAlias);
                Expression paramExpr = invokeExpr.getArguments().get(0);
                if (paramExpr.getOperator() == Expression.OP_DISTINCT)
                {
                    Collection processable = new HashSet(coll); // No dups in HashSet
                    coll = processable;
                }

                int stackSizeOrig = stack.size();
                Object returnVal = setexpr.max(paramExpr, this, state);
                while (stack.size() > stackSizeOrig)
                {
                    // Remove any expressions put on the stack while evaluating the aggregate
                    stack.pop();
                }
                return returnVal;
            }
            else
            {
                // Try to find a supported static method with this name
                InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(null, method);
                if (methodEval != null)
                {
                    return methodEval.evaluate(invokeExpr, null, this);
                }
                else
                {
                    NucleusLogger.QUERY.warn("Query contains call to static method " + method + 
                        " yet no support is available for in-memory evaluation of this");
                    return new InMemoryFailure();
                }
            }
        }
        else if (invokeExpr.getLeft() instanceof ParameterExpression)
        {
            // {paramExpr}.method(...)
            Object invokedValue =
                QueryUtils.getValueForParameterExpression(parameterValues, (ParameterExpression)invokeExpr.getLeft());

            // Invoke method on this object
            Class invokedType = (invokedValue != null ? invokedValue.getClass() : invokeExpr.getLeft().getSymbol().getValueType());
            InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(invokedType, method);
            if (methodEval != null)
            {
                return methodEval.evaluate(invokeExpr, invokedValue, this);
            }
            else
            {
                NucleusLogger.QUERY.warn("Query contains call to method " + 
                    invokedValue.getClass().getName() + "." + method + " yet no support is available for this");
                return new InMemoryFailure();
            }
        }
        else if (invokeExpr.getLeft() instanceof PrimaryExpression)
        {
            // {primaryExpr}.method(...)
            Object invokedValue = getValueForPrimaryExpression((PrimaryExpression)invokeExpr.getLeft());
            if (invokedValue instanceof InMemoryFailure)
            {
                return invokedValue;
            }

            // Invoke method on this object
            Class invokedType = (invokedValue != null ? invokedValue.getClass() : invokeExpr.getLeft().getSymbol().getValueType());
            InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(invokedType, method);
            if (methodEval != null)
            {
                return methodEval.evaluate(invokeExpr, invokedValue, this);
            }
            else
            {
                NucleusLogger.QUERY.warn("Query contains call to method " + 
                    invokedType.getName() + "." + method + " yet no support is available for this");
                return new InMemoryFailure();
            }
        }
        else if (invokeExpr.getLeft() instanceof InvokeExpression)
        {
            // {invokeExpr}.method(...)
            Object invokedValue = getValueForInvokeExpression((InvokeExpression)invokeExpr.getLeft());

            // Invoke method on this object
            Class invokedType = (invokedValue != null ? invokedValue.getClass() : 
                (invokeExpr.getLeft().getSymbol() != null ? invokeExpr.getLeft().getSymbol().getValueType() : null));
            if (invokedType == null)
            {
                return new InMemoryFailure();
            }
            InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(invokedType, method);
            if (methodEval != null)
            {
                return methodEval.evaluate(invokeExpr, invokedValue, this);
            }
            else
            {
                NucleusLogger.QUERY.warn("Query contains call to method " + 
                    invokedType.getName() + "." + method + " yet no support is available for this");
                return new InMemoryFailure();
            }
        }
        else if (invokeExpr.getLeft() instanceof VariableExpression)
        {
            // {invokeExpr}.method(...)
            Object invokedValue = getValueForVariableExpression((VariableExpression)invokeExpr.getLeft());

            // Invoke method on this object
            Class invokedType = (invokedValue != null ? invokedValue.getClass() : invokeExpr.getLeft().getSymbol().getValueType());
            InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(invokedType, method);
            if (methodEval != null)
            {
                return methodEval.evaluate(invokeExpr, invokedValue, this);
            }
            else
            {
                NucleusLogger.QUERY.warn("Query contains call to method " + 
                    invokedType.getName() + "." + method + " yet no support is available for this");
                return new InMemoryFailure();
            }
        }
        else if (invokeExpr.getLeft() instanceof Literal)
        {
            // {invokeExpr}.method(...)
            Object invokedValue = ((Literal)invokeExpr.getLeft()).getLiteral();

            // Invoke method on this object
            Class invokedType = (invokedValue != null ? invokedValue.getClass() : invokeExpr.getLeft().getSymbol().getValueType());
            InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(invokedType, method);
            if (methodEval != null)
            {
                return methodEval.evaluate(invokeExpr, invokedValue, this);
            }
            else
            {
                NucleusLogger.QUERY.warn("Query contains call to method " + 
                    invokedType.getName() + "." + method + " yet no support is available for this");
                return new InMemoryFailure();
            }
        }
        else if (invokeExpr.getLeft() instanceof ArrayExpression)
        {
            // {invokeExpr}.method(...)
            Object invokedValue = getValueForArrayExpression((ArrayExpression)invokeExpr.getLeft());

            // Invoke method on this object
            Class invokedType = (invokedValue != null ? invokedValue.getClass() : invokeExpr.getLeft().getSymbol().getValueType());
            InvocationEvaluator methodEval = queryMgr.getInMemoryEvaluatorForMethod(invokedType, method);
            if (methodEval != null)
            {
                return methodEval.evaluate(invokeExpr, invokedValue, this);
            }
            else
            {
                NucleusLogger.QUERY.warn("Query contains call to method " + 
                    invokedType.getName() + "." + method + " yet no support is available for this");
                return new InMemoryFailure();
            }
        }
        else
        {
            NucleusLogger.QUERY.warn("No support is available for in-memory evaluation of methods invoked" +
                " on expressions of type " + invokeExpr.getLeft().getClass().getName());
            return new InMemoryFailure();
        }
    }

    private Object getValueForArrayExpression(ArrayExpression arrayExpr)
    {
        Object value = new Object[arrayExpr.getArraySize()];

        for (int i=0;i<Array.getLength(value);i++)
        {
            Expression elem = arrayExpr.getElement(i);
            if (elem instanceof Literal)
            {
                Array.set(value, i, ((Literal)elem).getLiteral());
            }
            else if (elem instanceof PrimaryExpression)
            {
                Array.set(value, i, getValueForPrimaryExpression((PrimaryExpression)elem));
            }
            else if (elem instanceof ParameterExpression)
            {
                Array.set(value, i, 
                    QueryUtils.getValueForParameterExpression(parameterValues, (ParameterExpression)elem));
            }
            else
            {
                NucleusLogger.QUERY.warn("No support is available for array expression with element of type " +
                    elem.getClass().getName());
                return new InMemoryFailure();
            }
        }

        return value;
    }

    /**
     * Convenience method to get an int value from the supplied literal.
     * Returns a value if it is convertible into an int.
     * @param lit The literal
     * @return The int value
     * @throws NucleusException if impossible to convert into an int
     */
    public int getIntegerForLiteral(Literal lit)
    {
        Object val = lit.getLiteral();
        if (val instanceof BigDecimal)
        {
            return ((BigDecimal)val).intValue();
        }
        else if (val instanceof BigInteger)
        {
            return ((BigInteger)val).intValue();
        }
        else if (val instanceof Long)
        {
            return ((Long)val).intValue();
        }
        else if (val instanceof Integer)
        {
            return ((Integer)val).intValue();
        }
        else if (val instanceof Short)
        {
            return ((Short)val).intValue();
        }
        throw new NucleusException("Attempt to convert literal with value " + val + " (" + 
            val.getClass().getName() + ") into an int failed");
    }

    /**
     * Convenience method to get the value for a PrimaryExpression.
     * @param primExpr Expression
     * @return The value in the object for this expression
     */
    public Object getValueForPrimaryExpression(PrimaryExpression primExpr)
    {
        Object value = null;
        if (primExpr.getLeft() != null)
        {
            // Get value of left expression
            if (primExpr.getLeft() instanceof DyadicExpression)
            {
                DyadicExpression dyExpr = (DyadicExpression)primExpr.getLeft();
                if (dyExpr.getOperator() == Expression.OP_CAST)
                {
                    Expression castLeftExpr = dyExpr.getLeft();
                    if (castLeftExpr instanceof PrimaryExpression)
                    {
                        value = getValueForPrimaryExpression((PrimaryExpression)castLeftExpr);
                        String castClassName = (String)((Literal)dyExpr.getRight()).getLiteral();
                        if (value != null)
                        {
                            // TODO Do this in the compilation stage, and check for ClassNotResolvedException
                            Class castClass = imports.resolveClassDeclaration(castClassName, clr, null);
                            if (!castClass.isAssignableFrom(value.getClass()))
                            {
                                NucleusLogger.QUERY.warn("Candidate for query results in attempt to cast " +
                                    StringUtils.toJVMIDString(value) + " to " + castClass.getName() +
                                    " which is impossible!");
                                return new InMemoryFailure();
                            }
                        }
                    }
                    else if (castLeftExpr instanceof VariableExpression)
                    {
                        value = getValueForVariableExpression((VariableExpression)castLeftExpr);
                        String castClassName = (String)((Literal)dyExpr.getRight()).getLiteral();
                        if (value != null)
                        {
                            // TODO Do this in the compilation stage, and check for ClassNotResolvedException
                            Class castClass = imports.resolveClassDeclaration(castClassName, clr, null);
                            if (!castClass.isAssignableFrom(value.getClass()))
                            {
                                NucleusLogger.QUERY.warn("Candidate for query results in attempt to cast " +
                                    StringUtils.toJVMIDString(value) + " to " + castClass.getName() +
                                    " which is impossible!");
                                return new InMemoryFailure();
                            }
                        }
                    }
                    else
                    {
                        // TODO Allow for cast of ParameterExpression
                        NucleusLogger.QUERY.warn("Dont currently support CastExpression of " + castLeftExpr);
                        return new InMemoryFailure();
                    }
                }
                else
                {
                    NucleusLogger.QUERY.error("Dont currently support PrimaryExpression starting with DyadicExpression of " + dyExpr);
                    return new InMemoryFailure();
                }
            }
            else if (primExpr.getLeft() instanceof ParameterExpression)
            {
                value = QueryUtils.getValueForParameterExpression(parameterValues, (ParameterExpression)primExpr.getLeft());
            }
            else if (primExpr.getLeft() instanceof VariableExpression)
            {
                VariableExpression varExpr = (VariableExpression)primExpr.getLeft();
                try
                {
                    value = getValueForVariableExpression(varExpr);
                }
                catch (VariableNotSetException vnse)
                {
                    // We don't know the possible values here!
                    NucleusLogger.QUERY.error("Attempt to access variable " + varExpr.getId() + " as part of primaryExpression " + primExpr);
                    return new InMemoryFailure();
                }
            }
            else
            {
                NucleusLogger.QUERY.warn("Dont currently support PrimaryExpression with left-side of " + primExpr.getLeft());
                return new InMemoryFailure();
            }
        }

        int firstTupleToProcess = 0;
        if (value == null)
        {
            if (state.containsKey(primExpr.getTuples().get(0)))
            {
                // Get value from the first node
                value = state.get(primExpr.getTuples().get(0));
                firstTupleToProcess = 1;
            }
            else if (state.containsKey(candidateAlias))
            {
                // Get value from the candidate
                value = state.get(candidateAlias);
            }
        }

        // Evaluate the field of this value
        for (int i = firstTupleToProcess; i < primExpr.getTuples().size(); i++)
        {
            String fieldName = primExpr.getTuples().get(i);
            if (!fieldName.equals(candidateAlias))
            {
                boolean getValueByReflection = true;
                if (ec.getApiAdapter().isPersistent(value))
                {
                    // Make sure this field is loaded
                    ObjectProvider valueOP = ec.findObjectProvider(value);
                    if (valueOP != null)
                    {
                        AbstractMemberMetaData mmd = valueOP.getClassMetaData().getMetaDataForMember(fieldName);
                        if (mmd == null)
                        {
                            NucleusLogger.QUERY.error("Cannot find " + fieldName + " member of " + valueOP.getClassMetaData().getFullClassName());
                            return new InMemoryFailure();
                        }
                        if (mmd.getAbsoluteFieldNumber() >= 0)
                        {
                            // Field is managed so make sure it is loaded, and get its value
                            valueOP.isLoaded(mmd.getAbsoluteFieldNumber());
                            value = valueOP.provideField(mmd.getAbsoluteFieldNumber());
                            getValueByReflection = false;
                        }
                    }
                }
                if (getValueByReflection)
                {
                    value = ClassUtils.getValueOfFieldByReflection(value, fieldName);
                }
            }
        }

        return value;
    }

    /**
     * Method to set the value for a variable.
     * @param id Id of the variable
     * @param value The value to use
     */
    public void setVariableValue(String id, Object value)
    {
        if (variableValues == null)
        {
            variableValues = new HashMap<String, Object>();
        }
        variableValues.put(id, value);
    }

    /**
     * Method to remove a variable value.
     * @param id The id of the variable
     */
    public void removeVariableValue(String id)
    {
        variableValues.remove(id);
    }

    /**
     * Convenience method to get the (current) value for a VariableExpression.
     * If the variable has no current value, throws a {@link VariableNotSetException}.
     * @param varExpr Variable Expression
     * @return The value
     * @throws VariableNotSetException Thrown when the variable has no value currently set.
     */
    public Object getValueForVariableExpression(VariableExpression varExpr)
    {
        if (variableValues == null || !variableValues.containsKey(varExpr.getId()))
        {
            throw new VariableNotSetException(varExpr);
        }

        return variableValues.get(varExpr.getId());
    }
}