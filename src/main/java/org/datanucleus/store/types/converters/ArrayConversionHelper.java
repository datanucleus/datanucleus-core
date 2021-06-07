/**********************************************************************
Copyright (c) 2021 Andy Jefferson and others. All rights reserved.
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

package org.datanucleus.store.types.converters;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.BitSet;

/**
 * Helper class providing conversion methods for use with arrays.
 */
public class ArrayConversionHelper
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBooleanArray(boolean[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n];

        for (int i = 0; i < n; i++)
        {
            buf[i] = value[i] ? (byte) 1 : (byte) 0;
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromCharArray(char[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n * 2];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            char x = value[i++];
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromDoubleArray(double[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n * 8];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            long x = Double.doubleToRawLongBits(value[i++]);
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromFloatArray(float[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n * 4];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            int x = Float.floatToRawIntBits(value[i++]);
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromIntArray(int[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n * 4];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            int x = value[i++];
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromLongArray(long[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n * 8];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            long x = value[i++];
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromShortArray(short[] value)
    {
        if (value == null)
        {
            return null;
        }

        int n = value.length;
        byte[] buf = new byte[n * 2];
        int i = 0;
        int j = 0;

        for (; i < n;)
        {
            short x = value[i++];
            buf[j++] = (byte) ((x >>> 8) & 0xFF);
            buf[j++] = (byte) (x & 0xFF);
        }

        return buf;
    }
    
    /**
     * Convert an instance of our value class into a byte[].
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBigDecimalArray(BigDecimal[] value)
    {
        if (value == null)
        {
            return null;
        }

        byte[] total = new byte[value.length * TOTAL_BYTES];

        int index = 0;
        for (int i=0; i < value.length; i++)
        {
            //set signal
            System.arraycopy(new byte[] {(byte)value[i].signum()},0,total,index,NR_SIGNAL_BYTES);
            index += NR_SIGNAL_BYTES;

            //set big integer
            byte[] b = value[i].unscaledValue().abs().toByteArray();
            System.arraycopy(b,0,total,index+(NR_BIGINTEGER_BYTES-b.length),b.length);
            index += NR_BIGINTEGER_BYTES;
            
            //set scale
            byte[] s = getByteArrayFromIntArray(new int[] { value[i].scale() });
            System.arraycopy(s,0,total,index,NR_SCALE_BYTES);
            index += NR_SCALE_BYTES;
        }
        return total;
    }    

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
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
     * @param value Object to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromBigIntegerArray(BigInteger[] value)
    {
        if (value == null)
        {
            return null;
        }

        long[] d = new long[value.length];

        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].longValue();
        }

        return getByteArrayFromLongArray(d);
    }    

    /**
     * Convert a byte[] into an instance of our value class.
     * @param buf byte array to be converted
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
    public static byte[] getByteArrayFromBooleanObjectArray(Boolean[] value)
    {
        if (value == null)
        {
            return null;
        }

        boolean[] d = new boolean[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].booleanValue();
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
    public static byte[] getByteArrayFromByteObjectArray(Byte[] value)
    {
        if (value == null)
        {
            return null;
        }

        byte[] d = new byte[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].byteValue();
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
    public static byte[] getByteArrayFromCharObjectArray(Character[] value)
    {
        if (value == null)
        {
            return null;
        }

        char[] d = new char[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].charValue();
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
    public static byte[] getByteArrayFromDoubleObjectArray(Double[] value)
    {
        if (value == null)
        {
            return null;
        }

        double[] d = new double[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].doubleValue();
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
            a[i] = Double.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Float array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromFloatObjectArray(Float[] value)
    {
        if (value == null)
        {
            return null;
        }

        float[] d = new float[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].floatValue();
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
            a[i] = Float.valueOf(d[i]);
        }
        return a;
    }

    /**
     * Convert an instance of our value class into a byte[].
     * @param value Integer array to be converted
     * @return converted byte array
     */
    public static byte[] getByteArrayFromIntObjectArray(Integer[] value)
    {
        if (value == null)
        {
            return null;
        }

        int[] d = new int[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].intValue();
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
    public static byte[] getByteArrayFromLongObjectArray(Long[] value)
    {
        if (value == null)
        {
            return null;
        }

        long[] d = new long[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].longValue();
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
    public static byte[] getByteArrayFromShortObjectArray(Short[] value)
    {
        if (value == null)
        {
            return null;
        }

        short[] d = new short[value.length];
        for (int i=0; i < value.length; i++)
        {
            d[i] = value[i].shortValue();
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
}