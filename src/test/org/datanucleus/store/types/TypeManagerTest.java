/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import junit.framework.TestCase;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.NucleusContext;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.types.TypeManager;
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Component tests for the TypeManager class.
 */
public class TypeManagerTest extends TestCase
{
    TypeManager typeMgr = null;

    public TypeManagerTest(String name)
    {
        super(name);
    }

    /**
     * Create a TypeManager for testing.
     */
    protected void setUp() throws Exception
    {
        ClassLoaderResolver clr = new ClassLoaderResolverImpl();
        Properties props = new Properties();
        props.setProperty("bundle-check-action", "EXCEPTION");
        PluginManager pluginMgr = new PluginManager(null, clr, props);
        NucleusContext nucCtx = new NucleusContext(null, null, pluginMgr);
        typeMgr = nucCtx.getTypeManager();
    }

    /**
     * Test of the isSupportedSecondClassType() method.
     */
    public void testIsSupportedSecondClassType()
    {
        checkIsSupportedSecondClassType(java.util.Collection.class, true);
        checkIsSupportedSecondClassType(java.util.Set.class, true);
        checkIsSupportedSecondClassType(java.util.HashSet.class, true);
        checkIsSupportedSecondClassType(java.util.Map.class, true);
        checkIsSupportedSecondClassType(java.util.HashMap.class, true);
        checkIsSupportedSecondClassType(java.util.Hashtable.class, true);
        checkIsSupportedSecondClassType(java.util.List.class, true);
        checkIsSupportedSecondClassType(java.util.ArrayList.class, true);
        checkIsSupportedSecondClassType(java.util.LinkedList.class, true);
        checkIsSupportedSecondClassType(java.util.Stack.class, true);
        checkIsSupportedSecondClassType(java.util.Vector.class, true);

        checkIsSupportedSecondClassType(java.util.TreeMap.class, true);
        checkIsSupportedSecondClassType(java.util.TreeSet.class, true);

        checkIsSupportedSecondClassType(boolean.class, true);
        checkIsSupportedSecondClassType(byte.class, true);
        checkIsSupportedSecondClassType(char.class, true);
        checkIsSupportedSecondClassType(double.class, true);
        checkIsSupportedSecondClassType(float.class, true);
        checkIsSupportedSecondClassType(int.class, true);
        checkIsSupportedSecondClassType(long.class, true);
        checkIsSupportedSecondClassType(short.class, true);

        checkIsSupportedSecondClassType(Boolean.class, true);
        checkIsSupportedSecondClassType(Byte.class, true);
        checkIsSupportedSecondClassType(Character.class, true);
        checkIsSupportedSecondClassType(Double.class, true);
        checkIsSupportedSecondClassType(Float.class, true);
        checkIsSupportedSecondClassType(Integer.class, true);
        checkIsSupportedSecondClassType(Long.class, true);
        checkIsSupportedSecondClassType(Short.class, true);

        checkIsSupportedSecondClassType(java.io.File.class, false);
        checkIsSupportedSecondClassType(java.util.Date.class, true);
        checkIsSupportedSecondClassType(java.util.Locale.class, true);
        checkIsSupportedSecondClassType(java.math.BigDecimal.class, true);
        checkIsSupportedSecondClassType(java.math.BigInteger.class, true);
        checkIsSupportedSecondClassType(java.sql.Date.class, true);
        checkIsSupportedSecondClassType(java.sql.Time.class, true);
        checkIsSupportedSecondClassType(java.sql.Timestamp.class, true);

        checkIsSupportedSecondClassType(TestOnlyList.class, true); // Should be true since List is supported
    }

    /**
     * Test of the isSupportedSecondClassType() method for arrays.
     */
    public void testIsSupportedSecondClassTypeForArrays()
    {
        checkIsSupportedArrayType(boolean[].class, true);
        checkIsSupportedArrayType(byte[].class, true);
        checkIsSupportedArrayType(char[].class, true);
        checkIsSupportedArrayType(double[].class, true);
        checkIsSupportedArrayType(float[].class, true);
        checkIsSupportedArrayType(int[].class, true);
        checkIsSupportedArrayType(long[].class, true);
        checkIsSupportedArrayType(short[].class, true);
        checkIsSupportedArrayType(BigInteger[].class, true);
        checkIsSupportedArrayType(BigDecimal[].class, true);
        checkIsSupportedArrayType(String[].class, true);
        checkIsSupportedArrayType(Locale[].class, true);
        checkIsSupportedArrayType(Date[].class, true);

        checkIsSupportedArrayType(java.sql.Date[].class, true); // Should be true since java.util.Date[] is supported
    }

    /**
     * Test for the default value of persistence-modifier.
     * Section 18.13 of JDO2 spec.
     */
    public void testIsDefaultPersistentType()
    {
        // primitives
        checkIsDefaultPersistent(boolean.class, true);
        checkIsDefaultPersistent(byte.class, true);
        checkIsDefaultPersistent(char.class, true);
        checkIsDefaultPersistent(double.class, true);
        checkIsDefaultPersistent(float.class, true);
        checkIsDefaultPersistent(int.class, true);
        checkIsDefaultPersistent(long.class, true);
        checkIsDefaultPersistent(short.class, true);

        // wrappers
        checkIsDefaultPersistent(Boolean.class, true);
        checkIsDefaultPersistent(Byte.class, true);
        checkIsDefaultPersistent(Character.class, true);
        checkIsDefaultPersistent(Double.class, true);
        checkIsDefaultPersistent(Float.class, true);
        checkIsDefaultPersistent(Integer.class, true);
        checkIsDefaultPersistent(Long.class, true);
        checkIsDefaultPersistent(Short.class, true);

        // java.lang
        checkIsDefaultPersistent(String.class, true);
        checkIsDefaultPersistent(Number.class, true);

        // java.math
        checkIsDefaultPersistent(BigDecimal.class, true);
        checkIsDefaultPersistent(BigInteger.class, true);

        // java.util
        checkIsDefaultPersistent(java.util.Currency.class, true);
        checkIsDefaultPersistent(java.util.Locale.class, true);
        checkIsDefaultPersistent(java.util.Date.class, true);
        checkIsDefaultPersistent(java.util.ArrayList.class, true);
        checkIsDefaultPersistent(java.util.HashMap.class, true);
        checkIsDefaultPersistent(java.util.HashSet.class, true);
        checkIsDefaultPersistent(java.util.Hashtable.class, true);
        checkIsDefaultPersistent(java.util.LinkedHashMap.class, true);
        checkIsDefaultPersistent(java.util.LinkedHashSet.class, true);
        checkIsDefaultPersistent(java.util.TreeMap.class, true);
        checkIsDefaultPersistent(java.util.TreeSet.class, true);
        checkIsDefaultPersistent(java.util.LinkedList.class, true);
        checkIsDefaultPersistent(java.util.List.class, true);
        checkIsDefaultPersistent(java.util.Set.class, true);
        checkIsDefaultPersistent(java.util.Map.class, true);

        // Arrays
        checkIsDefaultPersistent(boolean[].class, true);
        checkIsDefaultPersistent(byte[].class, true);
        checkIsDefaultPersistent(char[].class, true);
        checkIsDefaultPersistent(double[].class, true);
        checkIsDefaultPersistent(float[].class, true);
        checkIsDefaultPersistent(int[].class, true);
        checkIsDefaultPersistent(long[].class, true);
        checkIsDefaultPersistent(short[].class, true);
        checkIsDefaultPersistent(Boolean[].class, true);
        checkIsDefaultPersistent(Byte[].class, true);
        checkIsDefaultPersistent(Character[].class, true);
        checkIsDefaultPersistent(Double[].class, true);
        checkIsDefaultPersistent(Float[].class, true);
        checkIsDefaultPersistent(Integer[].class, true);
        checkIsDefaultPersistent(Long[].class, true);
        checkIsDefaultPersistent(Short[].class, true);
        checkIsDefaultPersistent(String[].class, true);
        checkIsDefaultPersistent(Number[].class, true);
        checkIsDefaultPersistent(java.util.Date[].class, true);
        checkIsDefaultPersistent(java.util.Locale[].class, true);
        checkIsDefaultPersistent(BigDecimal[].class, true);
        checkIsDefaultPersistent(BigInteger[].class, true);

        // java.sql (derived from a JDO spec defined type so assumed to be the same)
        checkIsDefaultPersistent(java.sql.Date.class, true);
        checkIsDefaultPersistent(java.sql.Time.class, true);
        checkIsDefaultPersistent(java.sql.Timestamp.class, true);
    }

    public void testGetStringConverter()
    {
        checkStringConverterType(BigDecimal.class, org.datanucleus.store.types.converters.BigDecimalStringConverter.class);
        checkStringConverterType(BigInteger.class, org.datanucleus.store.types.converters.BigIntegerStringConverter.class);
        checkStringConverterType(Currency.class, org.datanucleus.store.types.converters.CurrencyStringConverter.class);
        checkStringConverterType(Date.class, org.datanucleus.store.types.converters.DateStringConverter.class);
        checkStringConverterType(Locale.class, org.datanucleus.store.types.converters.LocaleStringConverter.class);
        checkStringConverterType(StringBuffer.class, org.datanucleus.store.types.converters.StringBufferStringConverter.class);
        checkStringConverterType(TimeZone.class, org.datanucleus.store.types.converters.TimeZoneStringConverter.class);
        checkStringConverterType(URI.class, org.datanucleus.store.types.converters.URIStringConverter.class);
        checkStringConverterType(URL.class, org.datanucleus.store.types.converters.URLStringConverter.class);
        checkStringConverterType(UUID.class, org.datanucleus.store.types.converters.UUIDStringConverter.class);

        // null expected if not mappable
        assertNull(typeMgr.getTypeConverterForType(Set.class, String.class));
        assertNull(typeMgr.getTypeConverterForType(Object.class, String.class));
        assertNull(typeMgr.getTypeConverterForType(null, String.class));
    }

    public void testGetDatastoreTypeForTypeConverter()
    {
        TypeConverter conv1 = typeMgr.getTypeConverterForType(URL.class, String.class);
        Class cls1 = TypeManager.getDatastoreTypeForTypeConverter(conv1, URL.class);
        assertEquals(String.class, cls1);

        TypeConverter conv2 = typeMgr.getTypeConverterForName("dn.date-long");
        Class cls2 = TypeManager.getDatastoreTypeForTypeConverter(conv2, Date.class);
        assertEquals(Long.class, cls2);
    }

    // ------------------------------------ Utility methods --------------------------------

    private void checkIsDefaultPersistent(Class cls, boolean persistent)
    {
        assertEquals("TypeManager default-persistent for " + cls.getName() + " is wrong", 
            typeMgr.isDefaultPersistent(cls), persistent);
    }

    private void checkIsSupportedSecondClassType(Class cls, boolean supported)
    {
        assertTrue("TypeManager claims that support for " + cls.getName() + " is " + supported + " !", 
            supported == typeMgr.isSupportedSecondClassType(cls.getName()));
    }
    
    private void checkIsSupportedArrayType(Class cls, boolean supported)
    {
        assertTrue("TypeManager claims that support for " + cls.getName() + " is " + supported + " !", 
            supported == (cls.isArray() && typeMgr.isSupportedSecondClassType(cls.getName())));
    }

    private void checkStringConverterType(Class type, Class expectedStringConverterType)
    {
        TypeConverter conv = typeMgr.getTypeConverterForType(type, String.class);
        assertEquals(expectedStringConverterType, conv.getClass());
    }

    // ------------------------------------ This test class only inner classes --------------------------------

    /**
     * Only used for testing whether subclasses are supported 
     */
    class TestOnlyList extends java.util.ArrayList
    {
        private static final long serialVersionUID = 197823989232L;
        public TestOnlyList()
        {
            super();
        }
        public TestOnlyList(int initialCapacity)
        {
            super(initialCapacity);
        }
        public TestOnlyList(Collection c)
        {
            super(c);
        }
    }
}