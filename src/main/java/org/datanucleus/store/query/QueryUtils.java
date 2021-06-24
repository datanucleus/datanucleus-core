/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringTokenizer;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.store.query.compiler.JPQLCompiler;
import org.datanucleus.store.query.compiler.JavaQueryCompiler;
import org.datanucleus.store.query.compiler.QueryCompilation;
import org.datanucleus.store.query.expression.DyadicExpression;
import org.datanucleus.store.query.expression.Expression;
import org.datanucleus.store.query.expression.InvokeExpression;
import org.datanucleus.store.query.expression.Literal;
import org.datanucleus.store.query.expression.OrderExpression;
import org.datanucleus.store.query.expression.ParameterExpression;
import org.datanucleus.store.query.expression.Expression.Operator;
import org.datanucleus.store.query.inmemory.InMemoryExpressionEvaluator;
import org.datanucleus.store.query.inmemory.InMemoryFailure;
import org.datanucleus.store.types.converters.TypeConversionHelper;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Imports;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Utilities for use in queries.
 */
public class QueryUtils
{
    /** Convenience Class[] for parameter types in getMethod call. */
    final static Class[] MAP_PUT_METHOD_ARG_TYPES = new Class[]{Object.class, Object.class};

    /**
     * Utility to return if the passed result class is a user-type, and so requires fields matching up.
     * @param className the class name looked for 
     * @return Whether it is a user class
     */
    public static boolean resultClassIsUserType(String className)
    {
        return !resultClassIsSimple(className) && !className.equals(java.util.Map.class.getName()) && !className.equals(ClassNameConstants.Object);
    }

    /**
     * Utility to return if the passed result class is a simple type with a single value.
     * Checks the class name against the supported "simple" query result-class types.
     * @param className the class name looked for 
     * @return Whether the result class is "simple".
     */
    public static boolean resultClassIsSimple(String className)
    {
        if (className.equals(ClassNameConstants.JAVA_LANG_BOOLEAN) ||
            className.equals(ClassNameConstants.JAVA_LANG_BYTE) ||
            className.equals(ClassNameConstants.JAVA_LANG_CHARACTER) ||
            className.equals(ClassNameConstants.JAVA_LANG_DOUBLE) ||
            className.equals(ClassNameConstants.JAVA_LANG_FLOAT) ||
            className.equals(ClassNameConstants.JAVA_LANG_INTEGER) ||
            className.equals(ClassNameConstants.JAVA_LANG_LONG) ||
            className.equals(ClassNameConstants.JAVA_LANG_SHORT) ||
            className.equals(ClassNameConstants.JAVA_LANG_STRING) ||
            className.equals(ClassNameConstants.JAVA_MATH_BIGDECIMAL) ||
            className.equals(ClassNameConstants.JAVA_MATH_BIGINTEGER) ||
            className.equals(ClassNameConstants.JAVA_UTIL_DATE) ||
            className.equals(ClassNameConstants.JAVA_SQL_DATE) ||
            className.equals(ClassNameConstants.JAVA_SQL_TIME) ||
            className.equals(ClassNameConstants.JAVA_SQL_TIMESTAMP) ||
            className.equals(ClassNameConstants.JAVA_TIME_LOCALDATE) ||
            className.equals(ClassNameConstants.JAVA_TIME_LOCALTIME) ||
            className.equals(ClassNameConstants.JAVA_TIME_LOCALDATETIME) ||
            className.equals(ClassNameConstants.Object) ||
            className.equals("java.net.URL") || 
            className.equals("java.net.URI") ||
            className.equals("java.util.Calendar") ||
            className.equals("java.util.Currency") ||
            className.equals("java.util.UUID") ||
            className.equals("java.util.Optional") ||
            className.equals("java.util.OptionalDouble") ||
            className.equals("java.util.OptionalInt") ||
            className.equals("java.util.OptionalLong"))
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method to return if the "result" clause from a java string-based query language includes only aggregates. 
     * This provides useful information when determining if the results will be a single row.
     * @param result The result required
     * @return Whether it has only aggregates
     */
    public static boolean resultHasOnlyAggregates(String result)
    {
        if (result == null)
        {
            return false;
        }

        String resultDefn = result.toLowerCase().startsWith("distinct") ? result.substring(8) : result;
        StringTokenizer tokenizer = new StringTokenizer(resultDefn, ",");
        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken().trim().toLowerCase();
            if (token.startsWith("max") || token.startsWith("min") || token.startsWith("avg") || token.startsWith("sum"))
            {
                token = token.substring(3).trim();
                if (!token.startsWith("("))
                {
                    // Not aggregate (some name that starts min, max, avg, sum etc)
                    return false;
                }
            }
            else if (token.startsWith("count"))
            {
                token = token.substring(5).trim();
                if (!token.startsWith("("))
                {
                    // Not aggregate (some name that starts count...)
                    return false;
                }
            }
            else
            {
                // Doesn't start with aggregate keyword so is not an aggregate
                return false;
            }
        }

        return true;
    }

    /**
     * Convenience method to return whether the query should return a single row.
     * @param query The query
     * @return Whether it represents a unique row
     */
    public static boolean queryReturnsSingleRow(Query query)
    {
        if (query.isUnique())
        {
            return true;
        }
        else if (query.getGrouping() != null)
        {
            return false;
        }
        else if (QueryUtils.resultHasOnlyAggregates(query.getResult()))
        {
            return true;
        }
        return false;
    }

    /**
     * Convenience method to create an instance of the result class with the provided field values, using a constructor taking the arguments. 
     * If the returned object is null there is no constructor with the correct signature. Tries to find a constructor taking the required arguments. 
     * Uses the fieldTypes first (if specified), then (if not specified) uses the type of the fieldValues, otherwise uses Object as the argument type.
     * @param resultClass The class of results that need creating
     * @param fieldValues The field values
     * @param fieldTypes The field types (optional). If specified needs same number as fieldValues
     * @return The result class object
     */
    public static Object createResultObjectUsingArgumentedConstructor(Class resultClass, Object[] fieldValues, Class[] fieldTypes)
    {
        Object obj = null;
        Class[] ctrTypes = new Class[fieldValues.length];
        for (int i=0;i<ctrTypes.length;i++)
        {
            if (fieldTypes != null && fieldTypes[i] != null)
            {
                ctrTypes[i] = fieldTypes[i];
            }
            else if (fieldValues[i] != null)
            {
                ctrTypes[i] = fieldValues[i].getClass();
            }
            else
            {
                // Constructor needs an argument yet we don't know the type, so use null (see ClassUtils method)
                ctrTypes[i] = null;
//                ctrTypes[i] = Object.class;
            }
        }

        Constructor ctr = ClassUtils.getConstructorWithArguments(resultClass, ctrTypes);
        if (ctr != null)
        {
            try
            {
                obj = ctr.newInstance(fieldValues);
                if (NucleusLogger.QUERY.isDebugEnabled())
                {
                    NucleusLogger.QUERY.debug("ResultObject of type " + resultClass.getName() + 
                        " created with following constructor arguments: " + StringUtils.objectArrayToString(fieldValues));
                }
            }
            catch (Exception e)
            {
                // do nothing
            }
        }

        return obj;
    }

    /**
     * Convenience method to create an instance of the result class with the provided field values, using the default constructor and setting the fields 
     * using either public fields, or setters, or a put method. 
     * If one of these parts is not found in the result class the returned object is null.
     * @param resultClass Result class that we need to create an object of
     * @param resultFieldNames Names of the fields in the results
     * @param resultClassFieldNames Map of the result class fields, keyed by the field name
     * @param fieldValues The field values
     * @return The result class object
     */
    public static Object createResultObjectUsingDefaultConstructorAndSetters(Class resultClass, String[] resultFieldNames, Map<String, Field> resultClassFieldNames, 
            Object[] fieldValues)
    {
        Object obj = null;
        try
        {
            // Create the object
            obj = resultClass.getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            String msg = Localiser.msg("021205", resultClass.getName());
            NucleusLogger.QUERY.error(msg);
            throw new NucleusUserException(msg);
        }

        for (int i=0;i<fieldValues.length;i++)
        {
            // Update the fields of our object with the field values
            Field field = resultClassFieldNames.get(resultFieldNames[i].toUpperCase());
            if (!setFieldForResultObject(obj, resultFieldNames[i], field, fieldValues[i]))
            {
                if (field == null) 
                {
                    // Field was null, and impossible to find setter etc either. Column doesn't exist in result class
                    NucleusLogger.GENERAL.info(Localiser.msg("021215", resultFieldNames[i]));
                }
                else
                {
                    String fieldType = (fieldValues[i] != null) ? fieldValues[i].getClass().getName() : "null";
                    String msg = Localiser.msg("021204", resultClass.getName(), resultFieldNames[i], fieldType);
                    NucleusLogger.QUERY.error(msg);
                    throw new NucleusUserException(msg);
                }
            }
        }

        return obj;
    }

    /**
     * Convenience method to create an instance of the result class with the provided field values, 
     * using the default constructor and setting the fields using either public fields, or setters, or a put method. 
     * If one of these parts is not found in the result class the returned object is null.
     * @param resultClass Result class that we need to create an object of
     * @param resultFieldNames Names of the fields in the results
     * @param resultFields (java.lang.reflect.)Field objects for the fields in the results
     * @param fieldValues The field values
     * @return The result class object
     */
    public static Object createResultObjectUsingDefaultConstructorAndSetters(Class resultClass, String[] resultFieldNames, Field[] resultFields, Object[] fieldValues)
    {
        Object obj = null;
        try
        {
            // Create the object
            obj = resultClass.getDeclaredConstructor().newInstance();
        }
        catch (Exception e)
        {
            String msg = Localiser.msg("021205", resultClass.getName());
            NucleusLogger.QUERY.error(msg);
            throw new NucleusUserException(msg);
        }

        for (int i=0;i<fieldValues.length;i++)
        {
            // Update the fields of our object with the field values
            if (!setFieldForResultObject(obj, resultFieldNames[i], resultFields[i], fieldValues[i]))
            {
                String fieldType = "null";
                if (fieldValues[i] != null)
                {
                    fieldType = fieldValues[i].getClass().getName();
                }
                String msg = Localiser.msg("021204", resultClass.getName(), resultFieldNames[i], fieldType);
                NucleusLogger.QUERY.error(msg);
                throw new NucleusUserException(msg);
            }
        }

        return obj;
    }

    /**
     * Method to set a field of an object, using set/put methods, or via a public field.
     * See JDO spec [14.6.12] describing the 3 ways of setting the field values after creating the result object using the default constructor.
     * @param obj The object to update the field for
     * @param fieldName Name of the field
     * @param field The field
     * @param value The value to apply
     * @return Whether it was updated
     */
    private static boolean setFieldForResultObject(final Object obj, String fieldName, Field field, Object value)
    {
        boolean fieldSet = false;

        // Try setting the (public) field directly
        if (!fieldSet)
        {
            String declaredFieldName = fieldName;
            if (field != null)
            {
                declaredFieldName = field.getName();
            }
            Field f = ClassUtils.getFieldForClass(obj.getClass(),declaredFieldName);
            if (f != null && Modifier.isPublic(f.getModifiers()))
            {
                try
                {
                    f.set(obj, value);
                    fieldSet = true;
                }
                catch (Exception e)
                {
                    // Unable to set directly, so try converting the value to the type of the field
                    Object convertedValue = TypeConversionHelper.convertTo(value, f.getType());
                    if (convertedValue != value)
                    {
                        // Value has been converted so try setting the field now
                        try
                        {
                            f.set(obj, convertedValue);
                            fieldSet = true;
                            if (NucleusLogger.QUERY.isDebugEnabled())
                            {
                                NucleusLogger.QUERY.debug("ResultObject set field=" + fieldName + " using reflection");
                            }
                        }
                        catch (Exception e2)
                        {
                            // Do nothing since unable to convert it
                        }
                    }
                }
            }
            if (!fieldSet && NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(Localiser.msg("021209", obj.getClass().getName(), declaredFieldName));
            }
        }

        // Try (public) setMethod()
        if (!fieldSet)
        {
            String setMethodName = (field != null) ? 
                "set" + fieldName.substring(0,1).toUpperCase() + field.getName().substring(1) : "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);

            Class argType = value != null ? value.getClass() : (field != null) ? field.getType() : null;

            Method m = ClassUtils.getMethodWithArgument(obj.getClass(), setMethodName, argType);
            if (m != null && Modifier.isPublic(m.getModifiers()))
            {
                // Where a set method with the exact argument type exists use it
                try
                {
                    m.invoke(obj, new Object[]{value});
                    fieldSet = true;
                    if (NucleusLogger.QUERY.isDebugEnabled())
                    {
                        NucleusLogger.QUERY.debug("ResultObject set field=" + fieldName + " using public " + setMethodName + "() method");
                    }
                }
                catch (Exception e)
                {
                    //do nothing
                }
            }
            else if (m == null)
            {
                // Find a method with the right name and a single arg and try conversion of the supplied value
                Method[] methods = (Method[])AccessController.doPrivileged(new PrivilegedAction() 
                {
                    public Object run()
                    {
                        return obj.getClass().getDeclaredMethods();
                    }
                });
                for (int i=0;i<methods.length;i++)
                {
                    Class[] args = methods[i].getParameterTypes();
                    if (methods[i].getName().equals(setMethodName) && Modifier.isPublic(methods[i].getModifiers()) && args != null && args.length == 1)
                    {
                        try
                        {
                            Object convValue = TypeConversionHelper.convertTo(value, args[0]);
                            methods[i].invoke(obj, new Object[]{convValue});
                            fieldSet = true;
                            if (NucleusLogger.QUERY.isDebugEnabled())
                            {
                                NucleusLogger.QUERY.debug("ResultObject set field=" + fieldName + " using " + setMethodName + "() method after converting value");
                            }
                            break;
                        }
                        catch (Exception e)
                        {
                            //do nothing
                        }
                    }
                }
            }
            if (!fieldSet && NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(Localiser.msg("021207", obj.getClass().getName(), setMethodName, argType != null ? argType.getName() : null));
            }
        }

        // Try (public) putMethod()
        if (!fieldSet)
        {
            Method m = getPublicPutMethodForResultClass(obj.getClass());
            if (m != null)
            {
                try
                {
                    m.invoke(obj, new Object[]{fieldName, value});
                    fieldSet = true;
                    if (NucleusLogger.QUERY.isDebugEnabled())
                    {
                        NucleusLogger.QUERY.debug("ResultObject set field=" + fieldName + " using put() method");
                    }
                }
                catch (Exception e)
                {
                    //do nothing
                }
            }
            if (!fieldSet && NucleusLogger.QUERY.isDebugEnabled())
            {
                NucleusLogger.QUERY.debug(Localiser.msg("021208", obj.getClass().getName(), "put"));
            }
        }

        return fieldSet;
    }

    /**
     * Convenience method to return the setXXX method for a field of the result class.
     * @param resultClass The result class
     * @param fieldName Name of the field
     * @param fieldType The type of the field being set
     * @return The setter method
     */
    public static Method getPublicSetMethodForFieldOfResultClass(Class resultClass, String fieldName, Class fieldType)
    {
        String setMethodName = "set" + fieldName.substring(0,1).toUpperCase() + fieldName.substring(1);
        Method m = ClassUtils.getMethodWithArgument(resultClass, setMethodName, fieldType);

        if (m != null && Modifier.isPublic(m.getModifiers()))
        {
            return m;
        }
        return null;
    }

    /**
     * Convenience method to return the put(Object, Object method for the result class.
     * @param resultClass The result class
     * @return The put(Object, Object) method
     */
    public static Method getPublicPutMethodForResultClass(final Class resultClass)
    {
        return (Method)AccessController.doPrivileged(new PrivilegedAction() 
        {
            public Object run() 
            {
                try
                {
                    return resultClass.getMethod("put", MAP_PUT_METHOD_ARG_TYPES);
                }
                catch (NoSuchMethodException ex)
                {
                    return null;
                }
            }
        });
    }

    /**
     * Convenience method to split an expression string into its constituent parts where separated by commas.
     * This is used in the case of, for example, a result specification, to get the column definitions.
     * @param str The expression string
     * @return The expression parts
     */
    public static String[] getExpressionsFromString(String str)
    {
        CharacterIterator ci = new StringCharacterIterator(str);
        int braces = 0;
        String text = "";
        List<String> exprList = new ArrayList();
        while (ci.getIndex() != ci.getEndIndex())
        {
            char c = ci.current();
            if (c == ',' && braces == 0)
            {
                exprList.add(text);
                text = "";
            }
            else if (c == '(')
            {
                braces++;
                text += c;
            }
            else if (c == ')')
            {
                braces--;
                text += c;
            }
            else
            {
                text += c;
            }
            ci.next();
        }
        exprList.add(text);
        return exprList.toArray(new String[exprList.size()]);
    }

    /**
     * Convenience method to get the value for a ParameterExpression.
     * If the parameterValues is supplied as named then it will be like
     * {[name1, val1], [name2, val2], ...} and paramExpr as id="name1", pos=0.
     * If the parameterValues is supplied as positional then it will be like
     * {[0, val1], [1, val2], ...} and paramExpr as id="name1", pos=0 (JDOQL) or id="1", pos=0 (JPQL)
     * @param parameterValues Input parameter values keyed by the parameter name/position
     * @param paramExpr Expression
     * @return The value in the object for this expression
     */
    public static Object getValueForParameterExpression(Map parameterValues, ParameterExpression paramExpr)
    {
        if (parameterValues == null)
        {
            return null;
        }

        Iterator paramEntryIter = parameterValues.entrySet().iterator();
        while (paramEntryIter.hasNext())
        {
            Entry entry = (Entry) paramEntryIter.next();
            Object key = entry.getKey();
            if (key instanceof Integer)
            {
                // Positional parameter
                if (((Integer) key).intValue() == paramExpr.getPosition())
                {
                    return entry.getValue();
                }
            }
            else if (key instanceof String)
            {
                // Named parameter
                if (((String)key).equals(paramExpr.getId()))
                {
                    return entry.getValue();
                }
            }
        }

        // Parameter not found, neither as positional or named
        return null;
    }

    /**
     * Convenience method to get the String value for an Object.
     * Currently String, Character and Number are supported.
     * @param obj Object
     * @return The String value for the Object
     */
    public static String getStringValue(Object obj)
    {
        String value = null;
        if (obj instanceof String)
        {
            value = (String) obj;
        }
        else if (obj instanceof Character)
        {
            value = ((Character) obj).toString();
        }
        else if (obj instanceof Number)
        {
            value = ((Number) obj).toString();
        }
        else if (obj != null)
        {
            throw new NucleusException("getStringValue(obj) where obj is instanceof " + obj.getClass().getName() + " not supported");
        }
        return value;
    }

    /**
     * Convenience method to get the String value for an Expression. 
     * Currently only ParameterExpression and Literal are supported.
     * @param expr Expression
     * @param parameters Input parameters
     * @return The String value in the object for this expression
     */
    public static String getStringValueForExpression(Expression expr, Map parameters)
    {
        if (expr instanceof ParameterExpression)
        {
            return getStringValue(getValueForParameterExpression(parameters, (ParameterExpression) expr));
        }
        else if (expr instanceof Literal)
        {
            return getStringValue(((Literal)expr).getLiteral());
        }

        throw new NucleusException("getStringValueForExpression(expr) where expr is instanceof " + expr.getClass().getName() + " not supported");
    }

    /**
     * Convenience method to compare two expression values against the specified operator.
     * Returns true if "left {operator} right" is true. The operator can be &lt;, &gt;, &ge;, &le;, ==, !=.
     * @param left Left object
     * @param right Right object
     * @param op Operator
     * @return Whether the comparison is true
     * @throws NucleusException if the comparison is impossible
     */
    public static boolean compareExpressionValues(Object left, Object right, Operator op)
    {
        if (left != null && left instanceof Optional)
        {
            if (!((Optional)left).isPresent())
            {
                // Equate Optional(empty) with null
                left = null;
            }
        }
        if (right != null && right instanceof Optional)
        {
            if (!((Optional)right).isPresent())
            {
                // Equate Optional(empty) with null
                right = null;
            }
        }

        if (left == null || right == null)
        {
            // TODO Support specification of NullOrderingType
            // Null comparisons - defaults to NULLS_FIRST
            if (op == Expression.OP_GT)
            {
                if (left == null && right == null)
                {
                    return false;
                }
                else if (left == null)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            else if (op == Expression.OP_LT)
            {
                if (left == null && right == null)
                {
                    return false;
                }
                else if (left == null)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else if (op == Expression.OP_GTEQ)
            {
                if (left == null && right == null)
                {
                    return true;
                }
                else if (left == null)
                {
                    return false;
                }
                else
                {
                    return true;
                }
            }
            else if (op == Expression.OP_LTEQ)
            {
                if (left == null && right == null)
                {
                    return true;
                }
                else if (left == null)
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else if (op == Expression.OP_EQ)
            {
                return left == right;
            }
            else if (op == Expression.OP_NOTEQ)
            {
                return left != right;
            }
        }
        else if (left instanceof Float || left instanceof Double || left instanceof BigDecimal ||
            right instanceof Float || right instanceof Double || right instanceof BigDecimal)
        {
            // One of the two numbers is floating point based so compare using Double
            // NOTE : Assumes double is the largest precision required
            Double leftVal = null;
            Double rightVal = null;
            if (left instanceof BigDecimal)
            {
                leftVal = Double.valueOf(((BigDecimal)left).doubleValue());
            }
            else if (left instanceof Double)
            {
                leftVal = (Double)left;
            }
            else if (left instanceof Float)
            {
                leftVal = Double.valueOf(((Float)left).doubleValue());
            }
            else if (left instanceof BigInteger)
            {
                leftVal = Double.valueOf(((BigInteger)left).doubleValue());
            }
            else if (left instanceof Long)
            {
                leftVal = Double.valueOf(((Long)left).doubleValue());
            }
            else if (left instanceof Integer)
            {
                leftVal = Double.valueOf(((Integer)left).doubleValue());
            }
            else if (left instanceof Short)
            {
                leftVal = Double.valueOf(((Short)left).doubleValue());
            }
            else if (left instanceof Enum)
            {
                leftVal = Double.valueOf(((Enum)left).ordinal());
            }
            if (right instanceof BigDecimal)
            {
                rightVal = Double.valueOf(((BigDecimal)right).doubleValue());
            }
            else if (right instanceof Double)
            {
                rightVal = (Double)right;
            }
            else if (right instanceof Float)
            {
                rightVal = Double.valueOf(((Float)right).doubleValue());
            }
            else if (right instanceof BigInteger)
            {
                rightVal = Double.valueOf(((BigInteger)right).doubleValue());
            }
            else if (right instanceof Long)
            {
                rightVal = Double.valueOf(((Long)right).doubleValue());
            }
            else if (right instanceof Integer)
            {
                rightVal = Double.valueOf(((Integer)right).doubleValue());
            }
            else if (right instanceof Short)
            {
                rightVal = Double.valueOf(((Short)right).doubleValue());
            }
            else if (right instanceof Enum)
            {
                rightVal = Double.valueOf(((Enum)right).ordinal());
            }            

            if (leftVal == null || rightVal == null)
            {
                throw new NucleusException("Attempt to evaluate relational expression between" + 
                    "\"" + left + "\" (type=" + left.getClass().getName() + ") and" +
                    "\"" + right + "\" (type=" + right.getClass().getName() + ") not possible due to types");
            }

            int comparison = leftVal.compareTo(rightVal);
            if (op == Expression.OP_GT)
            {
                return comparison > 0 ? true : false;
            }
            else if (op == Expression.OP_LT)
            {
                return comparison < 0 ? true : false;
            }
            else if (op == Expression.OP_GTEQ)
            {
                return comparison >= 0 ? true : false;
            }
            else if (op == Expression.OP_LTEQ)
            {
                return comparison <= 0 ? true : false;
            }
            else if (op == Expression.OP_EQ)
            {
                return comparison == 0 ? true : false;
            }
            else if (op == Expression.OP_NOTEQ)
            {
                return comparison != 0 ? true : false;
            }
        }
        else if (left instanceof Short || left instanceof Integer || left instanceof Long || left instanceof BigInteger || left instanceof Character ||
            right instanceof Short || right instanceof Integer || right instanceof Long || right instanceof BigInteger || right instanceof Character)
        {
            // Not floating point based and (at least) one of numbers is integral based so compare using long
            // NOTE : Assumes long is the largest precision required
            boolean leftUnset = false;
            boolean rightUnset = false;
            long leftVal = Long.MAX_VALUE;
            long rightVal = Long.MAX_VALUE;
            if (left instanceof BigInteger)
            {
                leftVal = ((BigInteger)left).longValue();
            }
            else if (left instanceof Long)
            {
                leftVal = ((Long)left).longValue();
            }
            else if (left instanceof Integer)
            {
                leftVal = ((Integer)left).longValue();
            }
            else if (left instanceof Short)
            {
                leftVal = ((Short)left).longValue();
            }
            else if (left instanceof Enum)
            {
                leftVal = ((Enum)left).ordinal();
            }
            else if (left instanceof Byte)
            {
                leftVal = ((Byte)left).longValue();
            }
            else if (left instanceof Character)
            {
                leftVal = ((Character)left).charValue();
            }
            else
            {
                leftUnset = true;
            }

            if (right instanceof BigInteger)
            {
                rightVal = ((BigInteger)right).longValue();
            }
            else if (right instanceof Long)
            {
                rightVal = ((Long)right).longValue();
            }
            else if (right instanceof Integer)
            {
                rightVal = ((Integer)right).longValue();
            }
            else if (right instanceof Short)
            {
                rightVal = ((Short)right).longValue();
            }
            else if (right instanceof Enum)
            {
                rightVal = ((Enum)right).ordinal();
            }
            else if (right instanceof Byte)
            {
                rightVal = ((Byte)right).longValue();
            }
            else if (right instanceof Character)
            {
                rightVal = ((Character)right).charValue();
            }
            else
            {
                rightUnset = true;
            }

            if (leftUnset || rightUnset)
            {
                throw new NucleusException("Attempt to evaluate relational expression between" + 
                    "\"" + left + "\" (type=" + left.getClass().getName() + ") and" +
                    "\"" + right + "\" (type=" + right.getClass().getName() + ") not possible due to types");
            }

            if (op == Expression.OP_GT)
            {
                return leftVal > rightVal ? true : false;
            }
            else if (op == Expression.OP_LT)
            {
                return leftVal < rightVal ? true : false;
            }
            else if (op == Expression.OP_GTEQ)
            {
                return leftVal >= rightVal ? true : false;
            }
            else if (op == Expression.OP_LTEQ)
            {
                return leftVal <= rightVal ? true : false;
            }
            else if (op == Expression.OP_EQ)
            {
                return leftVal == rightVal ? true : false;
            }
            else if (op == Expression.OP_NOTEQ)
            {
                return leftVal != rightVal ? true : false;
            }
        }
        else if (left instanceof Enum || right instanceof Enum || left instanceof String || right instanceof String)
        {
            String leftStr = left.toString();
            String rightStr = right.toString();
            if (op == Expression.OP_EQ)
            {
                // Use equals()
                return leftStr != null ? leftStr.equals(rightStr) : (rightStr == null);
            }
            else if (op == Expression.OP_NOTEQ)
            {
                // Use equals()
                return leftStr != null ? !leftStr.equals(rightStr) : (rightStr != null);
            }
            else if (op == Expression.OP_GT)
            {
                // Use Lexicographic comparison
                return leftStr != null ? leftStr.compareTo(rightStr) > 0 : false;
            }
            else if (op == Expression.OP_GTEQ)
            {
                // Use Lexicographic comparison
                return leftStr != null ? leftStr.compareTo(rightStr) >= 0 : false;
            }
            else if (op == Expression.OP_LT)
            {
                // Use Lexicographic comparison
                return leftStr != null ? leftStr.compareTo(rightStr) < 0 : false;
            }
            else if (op == Expression.OP_LTEQ)
            {
                // Use Lexicographic comparison
                return leftStr != null ? leftStr.compareTo(rightStr) <= 0 : false;
            }
            else
            {
                throw new NucleusException("Attempt to evaluate relational expression between" + 
                    "\"" + left + "\" (type=" + left.getClass().getName() + ") and" +
                    "\"" + right + "\" (type=" + right.getClass().getName() + ") not possible due to types");
            }
        }
        else if (left instanceof Date || right instanceof Date)
        {
            long leftVal = Long.MAX_VALUE;
            long rightVal = Long.MAX_VALUE;
            if (left instanceof Date)
            {
                leftVal = ((Date)left).getTime();
            }
            if (right instanceof Date)
            {
                rightVal = ((Date)right).getTime();
            }
            if (leftVal == Long.MAX_VALUE || rightVal == Long.MAX_VALUE)
            {
                throw new NucleusException("Attempt to evaluate relational expression between" + 
                    "\"" + left + "\" (type=" + left.getClass().getName() + ") and" +
                    "\"" + right + "\" (type=" + right.getClass().getName() + ") not possible due to types");
            }
            if (op == Expression.OP_GT)
            {
                return leftVal > rightVal ? true : false;
            }
            else if (op == Expression.OP_LT)
            {
                return leftVal < rightVal ? true : false;
            }
            else if (op == Expression.OP_GTEQ)
            {
                return leftVal >= rightVal ? true : false;
            }
            else if (op == Expression.OP_LTEQ)
            {
                return leftVal <= rightVal ? true : false;
            }
            else if (op == Expression.OP_EQ)
            {
                return leftVal == rightVal ? true : false;
            }
            else if (op == Expression.OP_NOTEQ)
            {
                return leftVal != rightVal ? true : false;
            }
        }
        else
        {
            if (op == Expression.OP_EQ)
            {
                // Use object comparison
                return left.equals(right);
            }
            else if (op == Expression.OP_NOTEQ)
            {
                // Use object comparison
                return !left.equals(right);
            }
            else
            {
                if (left instanceof Comparable && right instanceof Comparable)
                {
                    Comparable leftC = (Comparable)left;
                    Comparable rightC = (Comparable)right;
                    if (op == Expression.OP_GT)
                    {
                        return leftC.compareTo(rightC) > 0;
                    }
                    else if (op == Expression.OP_LT)
                    {
                        return leftC.compareTo(rightC) < 0;
                    }
                    else if (op == Expression.OP_LTEQ)
                    {
                        return leftC.compareTo(rightC) < 0 || leftC.compareTo(rightC) == 0;
                    }
                    else if (op == Expression.OP_GTEQ)
                    {
                        return leftC.compareTo(rightC) > 0 || leftC.compareTo(rightC) == 0;
                    }
                }

                // Can't do >, <, >=, <= with non-numeric. Maybe allow Character in future?
                throw new NucleusException("Attempt to evaluate relational expression between" + 
                    "\"" + left + "\" (type=" + left.getClass().getName() + ") and" +
                    "\"" + right + "\" (type=" + right.getClass().getName() + ") not possible due to types");
            }
        }

        throw new NucleusException("Attempt to evaluate relational expression between " + left + " and " + right + " with operation = " + op + " impossible to perform");
    }

    /**
     * Convenience method to return if there is an OR operator in the expression.
     * Allows for hierarchical expressions, navigating down through the expression tree.
     * @param expr The expression
     * @return Whether there is an OR
     */
    public static boolean expressionHasOrOperator(Expression expr)
    {
        if (expr instanceof DyadicExpression && expr.getOperator() == Expression.OP_OR)
        {
            return true;
        }

        // Try sub-components
        if (expr.getLeft() != null && expressionHasOrOperator(expr.getLeft()))
        {
            return true;
        }
        else if (expr.getRight() != null && expressionHasOrOperator(expr.getRight()))
        {
            return true;
        }
        // No other subcomponent would contain a DyadicExpression so stop

        return false;
    }

    /**
     * Convenience method to return if there is a NOT operator in the expression.
     * Allows for hierarchical expressions, navigating down through the expression tree.
     * @param expr The expression
     * @return Whether there is a NOT
     */
    public static boolean expressionHasNotOperator(Expression expr)
    {
        if (expr instanceof DyadicExpression && expr.getOperator() == Expression.OP_NOT)
        {
            return true;
        }

        // Try sub-components
        if (expr.getLeft() != null && expressionHasNotOperator(expr.getLeft()))
        {
            return true;
        }
        else if (expr.getRight() != null && expressionHasNotOperator(expr.getRight()))
        {
            return true;
        }
        // No other subcomponent would contain a DyadicExpression so stop

        return false;
    }

    /**
     * Convenience method to return the ParameterExpression for the specified position if found in the expression tree starting at <pre>rootExpr</pre>
     * @param rootExpr The expression
     * @param pos The position
     * @return The ParameterExpression (if found)
     */
    public static ParameterExpression getParameterExpressionForPosition(Expression rootExpr, int pos)
    {
        if (rootExpr instanceof ParameterExpression && ((ParameterExpression)rootExpr).getPosition() == pos)
        {
            return (ParameterExpression)rootExpr;
        }

        if (rootExpr.getLeft() != null)
        {
            ParameterExpression paramExpr = getParameterExpressionForPosition(rootExpr.getLeft(), pos);
            if (paramExpr != null)
            {
                return paramExpr;
            }
        }
        if (rootExpr.getRight() != null)
        {
            ParameterExpression paramExpr = getParameterExpressionForPosition(rootExpr.getRight(), pos);
            if (paramExpr != null)
            {
                return paramExpr;
            }
        }
        if (rootExpr instanceof InvokeExpression)
        {
            InvokeExpression invokeExpr = (InvokeExpression)rootExpr;
            List<Expression> args = invokeExpr.getArguments();
            if (args != null)
            {
                Iterator<Expression> argIter = args.iterator();
                while (argIter.hasNext())
                {
                    ParameterExpression paramExpr = getParameterExpressionForPosition(argIter.next(), pos);
                    if (paramExpr != null)
                    {
                        return paramExpr;
                    }
                }
            }
        }

        return null;
    }

    public static boolean queryParameterTypesAreCompatible(Class cls1, Class cls2)
    {
        Class first  = cls1.isPrimitive() ? ClassUtils.getWrapperTypeForPrimitiveType(cls1) : cls1;
        Class second = cls2.isPrimitive() ? ClassUtils.getWrapperTypeForPrimitiveType(cls2) : cls2;

        if (first.isAssignableFrom(second))
        {
            return true;
        }
        if (Number.class.isAssignableFrom(first) && Number.class.isAssignableFrom(cls2))
        {
            // Allow numeric conversions
            return true;
        }

        return false;
    }

    /**
     * Convenience method to generate the "key" for storing the query results of a query with parameters.
     * The key will be of the form
     * <pre>
     * JDOQL:SELECT FROM myClass WHERE myFilter:123456
     * </pre>
     * where "123456" is the hashCode of the parameters for the query
     * @param query The query
     * @param params The params
     * @return The key
     */
    public static String getKeyForQueryResultsCache(Query query, Map params)
    {
        if (params != null && !params.isEmpty())
        {
            // TODO Check if the params hashCode is adequate to use as a defining key
            return query.getLanguage() + ":" + query.toString() + ":" + params.hashCode();
        }
        return query.getLanguage() + ":" + query.toString() + ":";
    }

    /**
     * Convenience method to in-memory order the candidates, using the ordering supplied.
     * Assumes that the query is JDOQL.
     * @param candidates The candidates
     * @param type Candidate type
     * @param ordering The ordering clause
     * @param ec Execution Context
     * @param clr ClassLoader resolver
     * @return The ordered list
     */
    public static List orderCandidates(List candidates, Class type, String ordering, ExecutionContext ec, ClassLoaderResolver clr)
    {
        return orderCandidates(candidates, type, ordering, ec, clr, Query.LANGUAGE_JDOQL);
    }

    /**
     * Convenience method to in-memory order the candidates, using the ordering supplied.
     * Assumes that the query is JDOQL.
     * @param candidates The candidates
     * @param type Candidate type
     * @param ordering The ordering clause
     * @param ec Execution Context
     * @param clr ClassLoader resolver
     * @param queryLanguage The query language in use
     * @return The ordered list. Note that this typically returns Arrays$List which may not be what you want
     */
    public static List orderCandidates(List candidates, Class type, String ordering, ExecutionContext ec, ClassLoaderResolver clr, String queryLanguage)
    {
        if (candidates == null || candidates.isEmpty() || ordering == null || ordering.equals("#PK"))
        {
            return candidates;
        }

        JavaQueryCompiler compiler = new JPQLCompiler(ec.getNucleusContext(), ec.getClassLoaderResolver(), null, type, null, null, null, ordering, null, null, null, null, null);
        QueryCompilation compilation = compiler.compile(null, null);
        return QueryUtils.orderCandidates(candidates, compilation.getExprOrdering(), new HashMap(), "this", ec, clr, null, null, queryLanguage);
    }

    /**
     * Convenience method to order the input List of objects to the ordering defined by the compilation.
     * @param candidates Candidates
     * @param ordering Ordering expression(s)
     * @param state Map of state information (see JavaQueryEvaluator)
     * @param candidateAlias Candidate alias
     * @param ec ExecutionContext
     * @param clr ClassLoader resolver
     * @param parameterValues Any parameter values (maybe used by the ordering clause)
     * @param imports Imports for the query
     * @param queryLanguage The language of this query (JDOQL, JPQL etc)
     * @return The ordered List of candidates
     */
    public static List orderCandidates(List candidates, final Expression[] ordering, final Map state, final String candidateAlias, final ExecutionContext ec, 
            final ClassLoaderResolver clr, final Map parameterValues, final Imports imports, final String queryLanguage)
    {
        if (ordering == null)
        {
            // Nothing to do
            return candidates;
        }

        Object[] o = candidates.toArray();
        Arrays.sort(o, new Comparator()
        {
            public int compare(Object obj1, Object obj2)
            {
                for (int i=0; i<ordering.length; i++)
                {
                    state.put(candidateAlias, obj1);
                    Object a = ordering[i].evaluate(new InMemoryExpressionEvaluator(ec, parameterValues, state, imports, clr, candidateAlias, queryLanguage));

                    state.put(candidateAlias, obj2);
                    Object b = ordering[i].evaluate(new InMemoryExpressionEvaluator(ec, parameterValues, state, imports, clr, candidateAlias, queryLanguage));

                    if (a instanceof InMemoryFailure || b instanceof InMemoryFailure)
                    {
                        throw new NucleusException("Error encountered in in-memory evaluation of ordering. Consult the log for details");
                    }

                    OrderExpression orderExpr = (OrderExpression) ordering[i];
                    // Put any null values at the end
                    if (a == null && b == null)
                    {
                        return 0;
                    }
                    else if (a == null)
                    {
                        if (orderExpr.getNullOrder() != null)
                        {
                            // Use specified null handling
                            return orderExpr.getNullOrder() == NullOrderingType.NULLS_FIRST ? 1 : -1;
                        }
                        return -1; // Default to putting nulls at the end
                    }
                    else if (b == null)
                    {
                        if (orderExpr.getNullOrder() != null)
                        {
                            // Use specified null handling
                            return orderExpr.getNullOrder() == NullOrderingType.NULLS_FIRST ? -1 : 1;
                        }
                        return 1; // Default to putting nulls at the end
                    }
                    else
                    {
                        int result = ((Comparable)a).compareTo(b);
                        if (result != 0)
                        {
                            if (orderExpr.getSortOrder() == null || orderExpr.getSortOrder().equals("ascending"))
                            {
                                // Ascending
                                return result;
                            }
                            // Descending
                            return -1 * result;
                        }
                    }
                }
                return 0;
            }
        });
        return Arrays.asList(o);
    }
}