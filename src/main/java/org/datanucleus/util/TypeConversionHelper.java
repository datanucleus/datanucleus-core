/**********************************************************************
Copyright (c) 2004 Brendan de Beer and others. All rights reserved.
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
2004 Brendan de Beer - Initial contributor for conversion methods
2005 Erik Bengtson - refactor mapping
2005 Andy Jefferson - added Timestamp/String converters
    ...
**********************************************************************/
package org.datanucleus.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

/**
 * Class with methods for type conversion.
 */
public class TypeConversionHelper
{
    private static int NR_BIGINTEGER_BYTES = 40; //not sure how big we need the array to be, so use 40
    private static int NR_SCALE_BYTES = 4;
    private static int NR_SIGNAL_BYTES = 1;
    private static int TOTAL_BYTES = NR_BIGINTEGER_BYTES+NR_SCALE_BYTES+NR_SIGNAL_BYTES;
    
    /**
     * Convert an instance of our value class into a boolean[].
     * @param value Object to be converted
     * @return converted boolean array
     */
    public static boolean[] getBooleanArrayFromBitSet(BitSet value)
    {
        if (value == null)
        {
            return null;
        }
       
        boolean[] a = new boolean[value.length()];
        for( int i=0; i<a.length; i++)
        {
            a[i] = value.get(i);
        }
        return a;
    }
    
    /**
     * Convert a boolean[] into an instance of our value class.
     *
     * @param buf boolean array to be converted
     *
     * @return converted boolean array as BitSet
     */
    public static BitSet getBitSetFromBooleanArray(boolean[] buf)
    {
        BitSet set = new BitSet();
        for (int i = 0; i < buf.length; i++)
        {
            if( buf[i] )
            {
                set.set(i);
            }
        }

        return set;
    }    
    
    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted boolean array as object
     */
    public static boolean[] getBooleanArrayFromByteArray(byte[] buf)
    {
        int n = buf.length;
        boolean[] a = new boolean[n];

        for (int i = 0; i < n; i++)
        {
            a[i] = buf[i] != 0;
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBooleanArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        boolean[] a = (boolean[]) value;
        int n = a.length;
        byte[] buf = new byte[n];

        for (int i = 0; i < n; i++)
        {
            buf[i] = a[i] ? (byte) 1 : (byte) 0;
        }

        return buf;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted char array as object
     */
    public static char[] getCharArrayFromByteArray(byte[] buf)
    {
        int n = buf.length / 2;
        char[] a = new char[n];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            a[i++] = (char) (((buf[j++] & 0xFF) << 8) + (buf[j++] & 0xFF));
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromCharArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        char[] a = (char[]) value;
        int n = a.length;
        byte[] buf = new byte[n * 2];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            char x = a[i++];
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted double array as object
     */
    public static double[] getDoubleArrayFromByteArray(byte[] buf)
    {
        int n = buf.length / 8;
        double[] a = new double[n];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            a[i++] = Double.longBitsToDouble(((long) (buf[j++] & 0xFF) << 56) + ((long) (buf[j++] & 0xFF) << 48) + ((long) (buf[j++] & 0xFF) << 40) + ((long) (buf[j++] & 0xFF) << 32) + ((long) (buf[j++] & 0xFF) << 24) + ((buf[j++] & 0xFF) << 16) + ((buf[j++] & 0xFF) << 8) + (buf[j++] & 0xFF));
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromDoubleArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        double[] a = (double[]) value;
        int n = a.length;
        byte[] buf = new byte[n * 8];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            long x = Double.doubleToRawLongBits(a[i++]);
            buf[j++] = (byte) ((x >>> 56) & 0xFF);
            buf[j++] = (byte) ((x >>> 48) & 0xFF);
            buf[j++] = (byte) ((x >>> 40) & 0xFF);
            buf[j++] = (byte) ((x >>> 32) & 0xFF);
            buf[j++] = (byte) ((x >>> 24) & 0xFF);
            buf[j++] = (byte) ((x >>> 16) & 0xFF);
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted float array as object
     */
    public static float[] getFloatArrayFromByteArray(byte[] buf)
    {
        int n = buf.length / 4;
        float[] a = new float[n];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            a[i++] = Float.intBitsToFloat(((buf[j++] & 0xFF) << 24) + ((buf[j++] & 0xFF) << 16) + ((buf[j++] & 0xFF) << 8) + (buf[j++] & 0xFF));
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromFloatArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        float[] a = (float[]) value;
        int n = a.length;
        byte[] buf = new byte[n * 4];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            int x = Float.floatToRawIntBits(a[i++]);
            buf[j++] = (byte) ((x >>> 24) & 0xFF);
            buf[j++] = (byte) ((x >>> 16) & 0xFF);
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     *
     * @param buf byte array to be converted
     *
     * @return converted int array as object
     */
    public static int[] getIntArrayFromByteArray(byte[] buf)
    {
        int n = buf.length / 4;
        int[] a = new int[n];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            a[i++] = ((buf[j++] & 0xFF) << 24) + ((buf[j++] & 0xFF) << 16) + ((buf[j++] & 0xFF) << 8) + (buf[j++] & 0xFF);
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromIntArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        int[] a = (int[]) value;
        int n = a.length;
        byte[] buf = new byte[n * 4];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            int x = a[i++];
            buf[j++] = (byte) ((x >>> 24) & 0xFF);
            buf[j++] = (byte) ((x >>> 16) & 0xFF);
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted long array as object
     */
    public static long[] getLongArrayFromByteArray(byte[] buf)
    {
        int n = buf.length / 8;
        long[] a = new long[n];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            a[i++] = ((long) (buf[j++] & 0xFF) << 56) + ((long) (buf[j++] & 0xFF) << 48) + ((long) (buf[j++] & 0xFF) << 40) + ((long) (buf[j++] & 0xFF) << 32) + ((long) (buf[j++] & 0xFF) << 24) + ((buf[j++] & 0xFF) << 16) + ((buf[j++] & 0xFF) << 8) + (buf[j++] & 0xFF);
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromLongArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        long[] a = (long[]) value;
        int n = a.length;
        byte[] buf = new byte[n * 8];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            long x = a[i++];
            buf[j++] = (byte) ((x >>> 56) & 0xFF);
            buf[j++] = (byte) ((x >>> 48) & 0xFF);
            buf[j++] = (byte) ((x >>> 40) & 0xFF);
            buf[j++] = (byte) ((x >>> 32) & 0xFF);
            buf[j++] = (byte) ((x >>> 24) & 0xFF);
            buf[j++] = (byte) ((x >>> 16) & 0xFF);
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     *
     * @param buf byte array to be converted
     *
     * @return converted short array as object
     */
    public static short[] getShortArrayFromByteArray(byte[] buf)
    {
        int n = buf.length / 2;
        short[] a = new short[n];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            a[i++] = (short) (((buf[j++] & 0xFF) << 8) + (buf[j++] & 0xFF));
        }

        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromShortArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        short[] a = (short[]) value;
        int n = a.length;
        byte[] buf = new byte[n * 2];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            short x = a[i++];
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }
    
    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBigDecimalArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        BigDecimal[] a = (BigDecimal[]) value;
        byte[] total = new byte[a.length * TOTAL_BYTES];

        int index = 0;
        for (int i=0; i < a.length; i++)
        {
            //set signal
            System.arraycopy(new byte[] {(byte)a[i].signum()},0,total,index,NR_SIGNAL_BYTES);
            index += NR_SIGNAL_BYTES;

            //set big integer
            byte[] b = a[i].unscaledValue().abs().toByteArray();
            System.arraycopy(b,0,total,index+(NR_BIGINTEGER_BYTES-b.length),b.length);
            index += NR_BIGINTEGER_BYTES;
            
            //set scale
            byte[] s = getByteArrayFromIntArray(new int[] { a[i].scale() });
            System.arraycopy(s,0,total,index,NR_SCALE_BYTES);
            index += NR_SCALE_BYTES;
        }
        return total;
    }    

    /**
     * Convert a byte[] into an instance of our value class.
     *
     * @param buf byte array to be converted
     *
     * @return converted BigDecimal array as object
     */
    public static BigDecimal[] getBigDecimalArrayFromByteArray(byte[] buf)
    {        
        BigDecimal[] a = new BigDecimal[buf.length/TOTAL_BYTES];
        
        int index = 0;
        for (int i=0; i < a.length; i++)
        {
            //get signal
            byte[] signal = new byte[NR_SIGNAL_BYTES];
            System.arraycopy(buf,index,signal,0,NR_SIGNAL_BYTES);
            index += NR_SIGNAL_BYTES;
            
            //get big integer
            byte[] b = new byte[NR_BIGINTEGER_BYTES];
            System.arraycopy(buf,index,b,0,NR_BIGINTEGER_BYTES);
            BigInteger integer = new BigInteger(signal[0],b);
            index += NR_BIGINTEGER_BYTES;

            //get scale
            byte[] s = new byte[4];           
            System.arraycopy(buf,index,s,0,NR_SCALE_BYTES);            
            int[] scale = getIntArrayFromByteArray(s);
            a[i] = new BigDecimal(integer,scale[0]);
            index += NR_SCALE_BYTES;
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     *
     * @param value Object to be converted
     *
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBigIntegerArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        BigInteger[] a = (BigInteger[]) value;
        long[] d = new long[a.length];

        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].longValue();
        }

        return getByteArrayFromLongArray(d);
    }    

    /**
     * Convert a byte[] into an instance of our value class.
     *
     * @param buf byte array to be converted
     *
     * @return converted short array as object
     */
    public static BigInteger[] getBigIntegerArrayFromByteArray(byte[] buf)
    {
        long[] d = getLongArrayFromByteArray(buf);
        BigInteger[] a = new BigInteger[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = BigInteger.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Boolean[] to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBooleanObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Boolean[] a = (Boolean[]) value;
        boolean[] d = new boolean[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].booleanValue();
        }

        return getByteArrayFromBooleanArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Boolean array as object
     */
    public static Boolean[] getBooleanObjectArrayFromByteArray(byte[] buf)
    {
        boolean[] d = getBooleanArrayFromByteArray(buf);
        Boolean[] a = new Boolean[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = Boolean.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Byte[] to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromByteObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Byte[] a = (Byte[]) value;
        byte[] d = new byte[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].byteValue();
        }

        return d;
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Byte array as object
     */
    public static Byte[] getByteObjectArrayFromByteArray(byte[] buf)
    {
        if (buf == null)
        {
            return null;
        }

        Byte[] a = new Byte[buf.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = Byte.valueOf(buf[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Character array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromCharObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Character[] a = (Character[]) value;
        char[] d = new char[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].charValue();
        }

        return getByteArrayFromCharArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Character array as object
     */
    public static Character[] getCharObjectArrayFromByteArray(byte[] buf)
    {
        char[] d = getCharArrayFromByteArray(buf);
        Character[] a = new Character[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = Character.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Double array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromDoubleObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Double[] a = (Double[]) value;
        double[] d = new double[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].doubleValue();
        }

        return getByteArrayFromDoubleArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Double array as object
     */
    public static Double[] getDoubleObjectArrayFromByteArray(byte[] buf)
    {
        double[] d = getDoubleArrayFromByteArray(buf);
        Double[] a = new Double[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = new Double(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Float array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromFloatObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Float[] a = (Float[]) value;
        float[] d = new float[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].floatValue();
        }

        return getByteArrayFromFloatArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Float array as object
     */
    public static Float[] getFloatObjectArrayFromByteArray(byte[] buf)
    {
        float[] d = getFloatArrayFromByteArray(buf);
        Float[] a = new Float[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = new Float(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Integer array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromIntObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Integer[] a = (Integer[]) value;
        int[] d = new int[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].intValue();
        }

        return getByteArrayFromIntArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Integer array as object
     */
    public static Integer[] getIntObjectArrayFromByteArray(byte[] buf)
    {
        int[] d = getIntArrayFromByteArray(buf);
        Integer[] a = new Integer[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = Integer.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Long array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromLongObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Long[] a = (Long[]) value;
        long[] d = new long[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].longValue();
        }

        return getByteArrayFromLongArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Long array as object
     */
    public static Long[] getLongObjectArrayFromByteArray(byte[] buf)
    {
        long[] d = getLongArrayFromByteArray(buf);
        Long[] a = new Long[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = Long.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Short array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromShortObjectArray(Object value)
    {
        if (value == null)
        {
            return null;
        }

        Short[] a = (Short[]) value;
        short[] d = new short[a.length];
        for (int i=0; i < a.length; i++)
        {
            d[i] = a[i].shortValue();
        }

        return getByteArrayFromShortArray(d);
    }

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
     * @return converted Short array as object
     */
    public static Short[] getShortObjectArrayFromByteArray(byte[] buf)
    {
        short[] d = getShortArrayFromByteArray(buf);
        Short[] a = new Short[d.length];
        for (int i=0; i < a.length; i++)
        {
            a[i] = Short.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert the value to a instance of the given type. The value is converted only
     * if the type can't be assigned from the current type of the value instance. 
     * @param value the value to be converted
     * @param type the type of the expected object returned from the conversion
     * @return the converted object, or null if the object can't be converted
     */
    public static Object convertTo(Object value, Class type)
    {
        if (type == null || value == null)
        {
            return value;
        }

        //check if the id can be assigned for the field type, otherwise convert the id to the appropriate type
        if (!type.isAssignableFrom(value.getClass()))
        {
            if (type == short.class || type == Short.class)
            {
                return Short.valueOf(value.toString());
            }
            else if (type == char.class || type == Character.class)
            {
                return Character.valueOf(value.toString().charAt(0));
            }
            else if (type == int.class || type == Integer.class)
            {
                return Integer.valueOf(value.toString());
            }
            else if (type == long.class || type == Long.class)
            {
                return Long.valueOf(value.toString());
            }
            else if (type == boolean.class || type == Boolean.class)
            {
                return Boolean.valueOf(value.toString());
            }
            else if (type == byte.class || type == Byte.class)
            {
                return Byte.valueOf(value.toString());
            }
            else if (type == float.class || type == Float.class)
            {
                return Float.valueOf(value.toString());
            }
            else if (type == double.class || type == Double.class)
            {
                return Double.valueOf(value.toString());
            }
            else if (type == BigDecimal.class)
            {
                return new BigDecimal(value.toString());
            }
            else if (type == BigInteger.class)
            {
                return new BigInteger(value.toString());
            }
            else if (type == Timestamp.class)
            {
                if (value instanceof Date)
                {
                    return new Timestamp(((Date)value).getTime());
                }
                return Timestamp.valueOf(value.toString());
            }
            else if (type == String.class)
            {
                return value.toString();
            }
            else
            {
                return null;
            }
        }
        return value;
    }

    /**
     * Utility to convert an int into a byte array
     * @param val The int
     * @return The bytes
     */
    public static byte[] getBytesFromInt(int val)
    {
        byte[] arr = new byte[4];
        for (int i=3;i>=0;i--)
        {
            arr[i] = (byte) ((0xFFl & val) + Byte.MIN_VALUE);
            val >>>= 8;
        }
        return arr;
    }

    /**
     * Utility to convert a short into a a byte array
     * @param val The short
     * @return The bytes
     */
    public static byte[] getBytesFromShort(short val)
    {
        byte[] arr = new byte[2];
        for (int i=1;i>=0;i--)
        {
            arr[i] = (byte) ((0xFFl & val)  + Byte.MIN_VALUE);
            val >>>= 8;
        }
        return arr;
    }

    /**
     * Utility to convert an int into a byte-generated String
     * @param val The int
     * @return The String form of the bytes
     */
    public static String getStringFromInt(int val)
    {
        byte[] arr = new byte[4];
        for (int i=3;i>=0;i--)
        {
            arr[i] = (byte) ((0xFFl & val) + Byte.MIN_VALUE);
            val >>>= 8;
        }
        return new String(arr);
    }

    /**
     * Utility to convert a short into a byte-generated String
     * @param val The short
     * @return The String form of the bytes
     */
    public static String getStringFromShort(short val)
    {
        byte[] arr = new byte[2];
        for (int i=1;i>=0;i--)
        {
            arr[i] = (byte) ((0xFFl & val)  + Byte.MIN_VALUE);
            val >>>= 8;
        }
        return new String(arr);
    }

    /**
     * Utility to convert an int into a 8-char hex String
     * @param val The int
     * @return The hex String form of the int
     */
    public static String getHexFromInt(int val)
    {
        StringBuilder str = new StringBuilder("00000000");
        String hexstr = Integer.toHexString(val);
        str.replace(8 - hexstr.length(), 8, hexstr);
        return str.toString();
    }

    /**
     * Utility to convert a short into a 4-char hex String
     * @param val The short
     * @return The hex String form of the short
     */
    public static String getHexFromShort(short val)
    {
        StringBuilder str = new StringBuilder("0000");
        String hexstr = Integer.toHexString(val);
        str.replace(4 - hexstr.length(), 4, hexstr);
        return str.toString();
    }

    /**
     * Utility to convert a byte array to an int.
     * @param bytes The byte array
     * @return The int
     */
    public static int getIntFromByteArray(byte[] bytes)
    {
        int val = 0;
        for (int i=0; i<4; i++)
        {
            val = (val << 8) - Byte.MIN_VALUE + bytes[i];
        }
        return val;
    }

    /**
     * Converts a string in JDBC timestamp escape format to a Timestamp object.
     * To be precise, we prefer to find a JDBC escape type sequence in the format
     * "yyyy-mm-dd hh:mm:ss.fffffffff", but this does accept other separators
     * of fields, so as long as the numbers are in the order
     * year, month, day, hour, minute, second then we accept it.
     * @param s Timestamp string
     * @param cal The Calendar to use for conversion
     * @return Corresponding <tt>java.sql.Timestamp</tt> value.
     * @exception java.lang.IllegalArgumentException Thrown if the format of the
     * String is invalid
     */
    public static Timestamp stringToTimestamp(String s, Calendar cal)
    {
        int[] numbers = convertStringToIntArray(s);
        if (numbers == null || numbers.length < 6)
        {
            throw new IllegalArgumentException(Localiser.msg("030003", s));
        }

        int year = numbers[0];
        int month = numbers[1];
        int day = numbers[2];
        int hour = numbers[3];
        int minute = numbers[4];
        int second = numbers[5];
        int nanos = 0;
        if (numbers.length > 6)
        {
            String zeroedNanos = "" + numbers[6];
            if (zeroedNanos.length() < 9)
            {
                // Add trailing zeros
                int numZerosToAdd = 9-zeroedNanos.length();
                for (int i=0;i<numZerosToAdd;i++)
                {
                    zeroedNanos += "0";
                }
                nanos = Integer.valueOf(zeroedNanos);
            }
            else
            {
                nanos = numbers[6];
            }
        }

        Calendar thecal = cal;
        if (cal == null)
        {
            thecal = new GregorianCalendar();
        }
        thecal.set(Calendar.ERA, GregorianCalendar.AD);
        thecal.set(Calendar.YEAR, year);
        thecal.set(Calendar.MONTH, month - 1);
        thecal.set(Calendar.DATE, day);
        thecal.set(Calendar.HOUR_OF_DAY, hour);
        thecal.set(Calendar.MINUTE, minute);
        thecal.set(Calendar.SECOND, second);
        Timestamp ts = new Timestamp(thecal.getTime().getTime());
        ts.setNanos(nanos);

        return ts;
    }

    /**
     * Convenience method to convert a String containing numbers (separated by assorted
     * characters) into an int array. The separators can be ' '  '-'  ':'  '.'  ',' etc.
     * @param str The String
     * @return The int array
     */
    private static int[] convertStringToIntArray(String str)
    {
        if (str == null)
        {
            return null;
        }

        int[] values = null;
        ArrayList list = new ArrayList();

        int start = -1;
        for (int i=0;i<str.length();i++)
        {
            if (start == -1 && Character.isDigit(str.charAt(i)))
            {
                start = i;
            }
            if (start != i && start >= 0)
            {
                if (!Character.isDigit(str.charAt(i)))
                {
                    list.add(Integer.valueOf(str.substring(start, i)));
                    start = -1;
                }
            }
            if (i == str.length()-1 && start >= 0)
            {
                list.add(Integer.valueOf(str.substring(start)));
            }
        }

        if (list.size() > 0)
        {
            values = new int[list.size()];
            Iterator iter = list.iterator();
            int n = 0;
            while (iter.hasNext())
            {
                values[n++] = ((Integer)iter.next()).intValue();
            }
        }
        return values;
    }

    /** Used to zero-fill the fractional seconds to nine digits. */
    private static final String ZEROES = "000000000";

    /**
     * Formats a timestamp in JDBC timestamp escape format using the timezone
     * of the passed Calendar.
     * @param ts The timestamp to be formatted.
     * @param cal The Calendar
     * @return  A String in <tt>yyyy-mm-dd hh:mm:ss.fffffffff</tt> format.
     * @see java.sql.Timestamp
     */
    public static String timestampToString(Timestamp ts, Calendar cal)
    {
        cal.setTime(ts);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Months are zero based in Calendar
        int day = cal.get(Calendar.DATE);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        String yearString = Integer.toString(year);
        String monthString = month < 10 ? "0" + month : Integer.toString(month);
        String dayString = day < 10 ? "0" + day : Integer.toString(day);
        String hourString = hour < 10 ? "0" + hour : Integer.toString(hour);
        String minuteString = minute < 10 ? "0" + minute : Integer.toString(minute);
        String secondString = second < 10 ? "0" + second : Integer.toString(second);
        String nanosString = Integer.toString(ts.getNanos());

        if (ts.getNanos() != 0)
        {
            // Add leading zeroes
            nanosString = ZEROES.substring(0, ZEROES.length() - nanosString.length()) + nanosString;

            // Truncate trailing zeroes
            int truncIndex = nanosString.length() - 1;
            while (nanosString.charAt(truncIndex) == '0')
            {
                --truncIndex;
            }

            nanosString = nanosString.substring(0, truncIndex + 1);
        }

        return (yearString + "-" + monthString + "-" + dayString + " " + hourString + ":" + minuteString + ":" + secondString + "." + nanosString);
    }

    /**
     * Convert a string into an integer.
     * Returns the default value if not convertable.
     * @param str The string
     * @param dflt The default value
     * @return The converted int value
     */
    public static int intFromString(String str, int dflt)
    {
        try
        {
            Integer val = Integer.valueOf(str);
            return val.intValue();
        }
        catch (NumberFormatException nfe)
        {
            return dflt;
        }
    }
}