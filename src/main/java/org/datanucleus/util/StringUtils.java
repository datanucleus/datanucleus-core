/**********************************************************************
Copyright (c) 2003 Andy Jefferson and others. All rights reserved. 
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
2003 Erik Bengtson - moved replaceAll from Column class to here
2004 Andy Jefferson - moved intArrayToString, booleanArrayToString from SM
2007 Xuan Baldauf - toJVMIDString hex fix
    ...
**********************************************************************/
package org.datanucleus.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;

import org.datanucleus.exceptions.NucleusException;

/**
 * Utilities for String manipulation.
 */
public class StringUtils
{
    /**
     * Convert an exception to a String with full stack trace
     * @param ex the exception
     * @return a String with the full stacktrace error text
     */
    public static String getStringFromStackTrace(Throwable ex)
    {
        if (ex == null)
        {
            return "";
        }

        StringWriter str = new StringWriter();
        PrintWriter writer = new PrintWriter(str);
        try
        {
            ex.printStackTrace(writer);
            return str.getBuffer().toString();
        }
        finally
        {
            try
            {
                str.close();
                writer.close();
            }
            catch (IOException e)
            {
                //ignore
            }
        }
    }
    
    /**
     * Convenience method to get a File for the specified filename.
     * Caters for URL-encoded characters in the filename (treatment of spaces on Windows etc)
     * @param filename Name of file
     * @return The File
     */
    public static File getFileForFilename(String filename)
    {
        return new File(getDecodedStringFromURLString(filename));
    }

    /**
     * Convenience method to get a JarFile for the specified filename.
     * Caters for URL-encoded characters in the filename (treatment of spaces on Windows etc)
     * @param filename Name of file
     * @return The JarFile
     * @throws IOException if there is a problem opening the JarFile
     */
    public static JarFile getJarFileForFilename(String filename)
    throws IOException
    {
        return new JarFile(getDecodedStringFromURLString(filename));
    }

    /**
     * Convenience method to decode a URL string for use (so spaces are allowed)
     * @param urlString The URL string
     * @return The string
     */
    public static String getDecodedStringFromURLString(String urlString)
    {
        // Replace any "+" with "%2B" as per http://www.w3.org/MarkUp/html-spec/html-spec_8.html#SEC8.2.1
        String str = urlString.replace("+", "%2B");
        try
        {
            return URLDecoder.decode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new NucleusException("Error attempting to decode string", uee);
        }
    }

    /**
     * Convenience method to encode a URL string for use (so spaces are allowed)
     * @param string The string
     * @return The encoded string
     */
    public static String getEncodedURLStringFromString(String string)
    {
        try
        {
            return URLEncoder.encode(string, "UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new NucleusException("Error attempting to encode string", uee);
        }
    }

    /**
     * A more efficient version than {@link String#replace(CharSequence, CharSequence)} which uses
     * Regex for the implementation and requires compilation for every execution. 
     * @param theString The string to use
     * @param toReplace The string to replace.
     * @param replacement The replacement string.
     * @return The updated string after replacing.
     */
    public static String replaceAll(String theString, String toReplace, String replacement)
    {
        if (theString == null)
        {
            return null;
        }
        if (theString.indexOf(toReplace) == -1)
        {
            return theString;
        }

        StringBuilder stringBuilder = new StringBuilder(theString);
        int index = theString.length();
        int offset = toReplace.length();
        while ((index=theString.lastIndexOf(toReplace, index-1)) > -1)
        {
            stringBuilder.replace(index,index+offset, replacement);
        }

        return stringBuilder.toString();
    }

    /**
     * Utility to check if a string is whitespace.
     * If the string is null, returns true also.
     * @param str The string to check
     * @return Whether the string is just whitespace
     */
    public static boolean isWhitespace(String str)
    {
        return str==null || str.length()==0 || str.trim().length()==0;
    }

    /**
     * Utility to tell if two strings are the same. Extends the basic
     * String 'equals' method by allowing for nulls.
     * @param str1 The first string
     * @param str2 The second string
     * @return Whether the strings are equal.
     */
    public static boolean areStringsEqual(String str1,String str2)
    {
        if (str1 == null && str2 == null)
        {
            return true;
        }
        else if (str1 == null && str2 != null)
        {
            return false;
        }
        else if (str1 != null && str2 == null)
        {
            return false;
        }
        else
        {
            return str1.equals(str2);
        }
    }

    /** Utility to return a left-aligned version of a string padded to the
     * number of characters specified.
     * @param input The input string
     * @param length The length desired
     * @return The updated string 
     **/
    public static String leftAlignedPaddedString(String input,int length)
    {
        if (length <= 0)
        {
            return null;
        }

        StringBuilder output = new StringBuilder();
        char space = ' ';

        if (input != null)
        {
            if (input.length() < length)
            {
                output.append(input);
                for (int i=input.length();i<length;i++)
                {
                    output.append(space);
                }
            }
            else
            {
                output.append(input.substring(0,length));
            }
        }
        else
        {
            for (int i=0;i<length;i++)
            {
                output.append(space);
            }
        }

        return output.toString();
    }

    /** Utility to return a right-aligned version of a string padded to the
     * number of characters specified.
     * @param input The input string
     * @param length The length desired
     * @return The updated string 
     **/
    public static String rightAlignedPaddedString(String input,int length)
    {
        if (length <= 0)
        {
            return null;
        }

        StringBuilder output = new StringBuilder();
        char space = ' ';

        if (input != null)
        {
            if (input.length() < length)
            {
                for (int i=input.length();i<length;i++)
                {
                    output.append(space);
                }
                output.append(input);
            }
            else
            {
                output.append(input.substring(0,length));
            }
        }
        else
        {
            for (int i=0;i<length;i++)
            {
                output.append(space);
            }
        }

        return output.toString();
    }
    
    /**
     * Splits a list of values separated by a token
     * @param valuesString the text to be splited
     * @param token the token
     * @return an array with all values
     */
    public static String[] split(String valuesString, String token)
    {
        String[] values;
        if (valuesString != null)
        {
            StringTokenizer tokenizer = new StringTokenizer(valuesString,token);

            values = new String[tokenizer.countTokens()];
            int count = 0;
            while (tokenizer.hasMoreTokens())
            { 
                values[count++] = tokenizer.nextToken();
            }           
        }
        else
        {
            values = null;
        }
        return values;
    }

    /**
     * Converts the input comma-separated string into a Set of the individual strings, with each string trimmed and converted to UPPERCASE.
     * @param str The input string
     * @return Set of the comma-separated strings that it is comprised of
     */
    public static Set<String> convertCommaSeparatedStringToSet(String str)
    {
        Set set = new HashSet();

        StringTokenizer tokens = new StringTokenizer(str, ",");
        while (tokens.hasMoreTokens())
        {
            set.add(tokens.nextToken().trim().toUpperCase());
        }

        return set;
    }

    /**
     * Utility to convert an object to a JVM type string.
     * Returns the same as would have been output from Object.toString() if the class hadn't overridden it.
     * @param obj The object
     * @return The String version
     */
    public static String toJVMIDString(Object obj)
    {
        if (obj == null)
        {
            return "null";
        }
        // Align to the Java VM way of printing identity hash codes, that means: hexadecimal
        return obj.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(obj));
    }

    /**
     * Utility to convert a boolean[] to a String.
     * @param ba The boolean[]
     * @return String version 
     */ 
    public static String booleanArrayToString(boolean[] ba)
    {
        if (ba == null)
        {
            return "null";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i=0; i<ba.length; ++i)
        {
            sb.append(ba[i] ? 'Y' : 'N');
        }
        sb.append(']');

        return sb.toString();
    }

    /**
     * Utility to convert an int[] to a String.
     * @param ia The int[]
     * @return String version
     **/
    public static String intArrayToString(int[] ia)
    {
        if (ia == null)
        {
            return "null";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i=0; i<ia.length; ++i)
        {
            if (i > 0)
            {
                sb.append(", ");
            }

            sb.append(ia[i]);
        }
        sb.append(']');

        return sb.toString();
    }

    /**
     * Utility to convert an Object[] to a String.
     * @param arr The Object[]
     * @return String version
     **/
    public static String objectArrayToString(Object[] arr)
    {
        if (arr == null)
        {
            return "null";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i=0; i<arr.length; ++i)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(arr[i]);
        }
        sb.append(']');

        return sb.toString();
    }

    /**
     * Converts the given collection of objects to string as a comma-separated
     * list.  If the list is empty the string "&lt;none&gt;" is returned.
     *
     * @param coll collection of objects to be converted
     * @return  A string containing each object in the given collection,
     *          converted toString() and separated by commas.
     */
    public static String collectionToString(Collection coll)
    {
        if (coll == null)
        {
            return "<null>";
        }
        else if (coll.isEmpty())
        {
            return "<none>";
        }
        else
        {
            StringBuilder s = new StringBuilder();
            Iterator iter = coll.iterator();
            while (iter.hasNext())
            {
                Object obj = iter.next();
                if (s.length() > 0)
                {
                    s.append(", ");
                }
                s.append(obj);
            }

            return s.toString();
        }
    }

    public static String getNameOfClass(Class cls)
    {
        if (cls.isPrimitive())
        {
            if (cls == boolean.class)
            {
                return "boolean";
            }
            else if (cls == byte.class)
            {
                return "byte";
            }
            else if (cls == char.class)
            {
                return "char";
            }
            else if (cls == double.class)
            {
                return "double";
            }
            else if (cls == float.class)
            {
                return "float";
            }
            else if (cls == int.class)
            {
                return "int";
            }
            else if (cls == long.class)
            {
                return "long";
            }
            else if (cls == short.class)
            {
                return "short";
            }
        }
        else if (cls.isArray() && cls.getComponentType().isPrimitive())
        {
            return getNameOfClass(cls.getComponentType()) + "[]";
        }

        return cls.getName();
    }

    /**
     * Converts the given map of objects to string as a comma-separated list.
     * If the map is empty the string "&lt;none&gt;" is returned.
     * @param map Map to be converted
     * @return  A string containing each object in the given map, converted toString() and separated by commas.
     */
    public static String mapToString(Map map)
    {
        if (map == null)
        {
            return "<null>";
        }
        else if (map.isEmpty())
        {
            return "<none>";
        }
        else
        {
            StringBuilder s = new StringBuilder();
            Iterator iter = map.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry entry = (Map.Entry)iter.next();
                if (s.length() > 0)
                {
                    s.append(", ");
                }

                s.append("<" + entry.getKey() + "," + entry.getValue() + ">");
            }

            return s.toString();
        }
    }

    /**
     * Convenience method to extract an integer property value from a Properties file.
     * @param props The Properties
     * @param propName Name of the property
     * @param defaultValue The default value to use (in case not specified)
     * @return The value
     */
    public static int getIntValueForProperty(Properties props, String propName, int defaultValue)
    {
        int value = defaultValue;
        if (props != null && props.containsKey(propName))
        {
            try
            {
                value = (Integer.valueOf(props.getProperty(propName))).intValue();
            }
            catch (NumberFormatException nfe)
            {
                // Do nothing
            }
        }
        return value;
    }
    
    /**
     * check string is null or length is 0.
     * @param s check string 
     * @return return true if string is null or length is 0. return false other case.
     */
    public static boolean isEmpty(String s)
    {
        return ((s == null) || (s.length() == 0));
    }

    /**
     * check string is not null and length &gt; 0.
     * @param s check string 
     * @return return true if string isnot null and length greater than 0. return false other case.
     */
    public static boolean notEmpty(String s)
    {
        return ((s != null) && (s.length() > 0));
    }

    /**
     * Formats the given BigDecimal value into a floating-point literal (like we find in SQL).
     * BigDecimal.toString() is not well suited to this purpose because it never uses E-notation, 
     * which causes some values with large exponents to be output as long strings with tons of zeroes 
     * in them.
     * @param bd  The number to format.
     * @return  The formatted String.
     */
    public static String exponentialFormatBigDecimal(BigDecimal bd)
    {
        String digits = bd.unscaledValue().abs().toString();
        int scale = bd.scale();
        int len = digits.length();

        /* Normalize by removing any trailing zeroes. */
        while (len > 1 && digits.charAt(len - 1) == '0')
        {
            --scale;
            --len;
        }

        if (len < digits.length())
        {
            digits = digits.substring(0, len);
        }

        StringBuilder sb = new StringBuilder();
        if (bd.signum() < 0)
        {
            sb.append('-');
        }

        int exponent = len - scale;
        if (exponent < 0 || exponent > len)
        {
            /* Output in E-notation. */
            sb.append('.').append(digits).append('E').append(exponent);
        }
        else if (exponent == len)
        {
            /* Output as an integer. */
            sb.append(digits);
        }
        else
        {
            /* Output as "intDigits.fracDigits". */
            sb.append(digits.substring(0, exponent)).append('.').append(digits.substring(exponent));
        }

        return sb.toString();
    }

    /**
     * Method to return the input string with all special tags (end-of-line, tab, etc) replaced
     * by spaces.
     * @param str The string
     * @return The cleaned up string
     */
    public static String removeSpecialTagsFromString(String str)
    {
        if (str == null)
        {
            return null;
        }
        str = str.replace('\n', ' ');
        str = str.replace('\t', ' ');
        str = str.replace('\r', ' ');
        return str;
    }
}