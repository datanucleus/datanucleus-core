/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.identity;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.datanucleus.exceptions.NucleusUserException;

/**
 * This class is for identity with a single Object type field.
 */
public class ObjectId extends SingleFieldId<Object, ObjectId>
{
    private Object key;

    /**
     * Constructor with class and key.
     * @param pcClass the class
     * @param param the key
     */
    public ObjectId(Class<?> pcClass, Object param)
    {
        super(pcClass);
        assertKeyNotNull(param);
        String paramString = null;
        String keyString = null;
        String keyClassName = null;
        if (param instanceof String)
        {
            // The paramString is of the form "<keyClassName>:<keyString>"
            paramString = (String) param;
            if (paramString.length() < 3)
            {
                throw new NucleusUserException("ObjectId constructor from String was expecting a longer string than " + paramString);
            }
            int indexOfDelimiter = paramString.indexOf(STRING_DELIMITER);
            if (indexOfDelimiter < 0)
            {
                throw new NucleusUserException("ObjectId constructor from String was expecting a delimiter of " + STRING_DELIMITER + " but not present!");
            }
            keyString = paramString.substring(indexOfDelimiter + 1);
            keyClassName = paramString.substring(0, indexOfDelimiter);
            key = constructKey(keyClassName, keyString);
        }
        else
        {
            key = param;
        }
        hashCode = targetClassName.hashCode() ^ key.hashCode();
    }

    public ObjectId()
    {
    }

    public Object getKey()
    {
        return key;
    }

    public Object getKeyAsObject()
    {
        return key;
    }

    /**
     * Return the String form of the object id. The class of the object id is written as the first part of the
     * result so that the class can be reconstructed later. Then the toString of the key instance is appended.
     * During construction, this process is reversed. The class is extracted from the first part of the
     * String, and the String constructor of the key is used to construct the key itself.
     * @return the String form of the key
     */
    @Override
    public String toString()
    {
        return key.getClass().getName() + STRING_DELIMITER + key.toString();
    }

    @Override
    protected boolean keyEquals(ObjectId obj)
    {
        return key.equals(obj.key);
    }

    public int compareTo(ObjectId other)
    {
        int result = super.compare(other);
        if (result == 0)
        {
            if (other.key instanceof Comparable && key instanceof Comparable)
            {
                return ((Comparable) key).compareTo(other.key);
            }
            throw new ClassCastException("The key class (" + key.getClass().getName() + ") does not implement Comparable");
        }
        return result;
    }

    /**
     * Write this object. Write the superclass first.
     * @param out the output
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeObject(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readObject();
    }

    /**
     * Construct an instance of a key class using a String as input.
     * Classes without a String constructor (such as those in java.lang and java.util) will use this interface for constructing new instances. 
     * The result might be a singleton or use some other strategy.
     */
    public interface StringConstructor
    {
        /**
         * Construct an instance of the class for which this instance is registered.
         * @param s the parameter for construction
         * @return the constructed object
         */
        public Object construct(String s);
    }

    /**
     * Special StringConstructor instances for use with specific classes that have no public String constructor. 
     * The Map is keyed on class instance and the value is an instance of StringConstructor.
     */
    static final Map<Class, StringConstructor> stringConstructorMap = new HashMap<Class, StringConstructor>();

    /**
     * Register special StringConstructor instances. These instances are for constructing instances from
     * String parameters where there is no String constructor for them.
     * @param cls the class to register a StringConstructor for
     * @param sc the StringConstructor instance
     * @return the previous StringConstructor registered for this class
     */
    public static Object registerStringConstructor(Class cls, StringConstructor sc)
    {
        synchronized (stringConstructorMap)
        {
            return stringConstructorMap.put(cls, sc);
        }
    }

    /**
     * Construct an instance of the parameter class, using the keyString as an argument to the constructor. 
     * If the class has a StringConstructor instance registered, use it. 
     * If not, try to find a constructor for the class with a single String argument. Otherwise, throw a NucleusUserException.
     * @param className the name of the class
     * @param keyString the String parameter for the constructor
     * @return the result of construction
     */
    public static Object constructKey(String className, String keyString)
    {
        StringConstructor stringConstructor;
        try
        {
            Class<?> keyClass = Class.forName(className);
            synchronized (stringConstructorMap)
            {
                stringConstructor = stringConstructorMap.get(keyClass);
            }
            if (stringConstructor != null)
            {
                return stringConstructor.construct(keyString);
            }
            return keyClass.getConstructor(new Class[]{String.class}).newInstance(new Object[]{keyString});
        }
        catch (Exception ex)
        {
            // ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
            throw new NucleusUserException("Exception in Object identity String constructor", ex);
        }
    }

    /** The default DateFormat instance for handling java.util.Date instances. */
    private static DateFormat dateFormat;

    /**
     * Register the default special StringConstructor instances.
     */
    static
    {
        // TODO Add other possible types
        registerStringConstructor(java.util.Currency.class, new StringConstructor()
        {
            public Object construct(String s)
            {
                try
                {
                    return java.util.Currency.getInstance(s);
                }
                catch (Exception ex)
                {
                    throw new NucleusUserException("Exception in ObjectId String constructor for Currency", ex);
                }
            }
        });

        registerStringConstructor(java.util.Locale.class, new StringConstructor()
        {
            public Object construct(String s)
            {
                try
                {
                    String lang = s;
                    int firstUnderbar = s.indexOf('_');
                    if (firstUnderbar == -1)
                    {
                        // nothing but language
                        return new java.util.Locale(lang);
                    }
                    lang = s.substring(0, firstUnderbar);
                    String country;
                    int secondUnderbar = s.indexOf('_', firstUnderbar + 1);
                    if (secondUnderbar == -1)
                    {
                        // nothing but language, country
                        country = s.substring(firstUnderbar + 1);
                        return new java.util.Locale(lang, country);
                    }
                    country = s.substring(firstUnderbar + 1, secondUnderbar);
                    String variant = s.substring(secondUnderbar + 1);
                    return new java.util.Locale(lang, country, variant);
                }
                catch (Exception ex)
                {
                    throw new NucleusUserException("Exception in ObjectId String constructor for Locale", ex);
                }
            }
        });

        /*
         * This requires the following privileges for EnhancementHelper in the security permissions file: 
         * permission java.util.PropertyPermission "user.country", "read"; 
         * permission java.util.PropertyPermission "user.timezone", "read,write";
         * permission java.util.PropertyPermission "java.home", "read"; 
         * If these permissions are not present, or there is some other problem getting the default date format, a simple formatter is returned.
         */
        DateFormat result = null;
        try
        {
            result = AccessController.doPrivileged(new PrivilegedAction<DateFormat>()
            {
                public DateFormat run()
                {
                    return DateFormat.getDateTimeInstance();
                }
            });
        }
        catch (Exception ex)
        {
            result = DateFormat.getInstance();
        }
        dateFormat = result;

        registerStringConstructor(java.util.Date.class, new StringConstructor()
        {
            public synchronized Object construct(String s)
            {
                try
                {
                    // first, try the String as a Long
                    return new java.util.Date(Long.parseLong(s));
                }
                catch (NumberFormatException ex)
                {
                    // not a Long; try the formatted date
                    ParsePosition pp = new ParsePosition(0);
                    java.util.Date result = dateFormat.parse(s, pp);
                    if (result == null)
                    {
                        String dateFormatPattern = null;
                        if (dateFormat instanceof SimpleDateFormat)
                        {
                            dateFormatPattern = ((SimpleDateFormat)dateFormat).toPattern();
                        }
                        else
                        {
                            dateFormatPattern = "Unknown message";
                        }
                        throw new NucleusUserException("Exception in ObjectId String constructor for Date", new Object[] {s, Integer.valueOf(pp.getErrorIndex()), dateFormatPattern});
                    }
                    return result;
                }
            }
        });
    }
}