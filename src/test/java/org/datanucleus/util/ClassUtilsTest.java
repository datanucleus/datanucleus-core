/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;

import junit.framework.TestCase;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassLoaderResolverImpl;
import org.datanucleus.ExecutionContext;
import org.datanucleus.enhancer.Persistable;
import org.datanucleus.state.StateManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.ClassUtilsTest.MyBaseClass;

/**
 * Tests for class utilities.
 */
public class ClassUtilsTest extends TestCase
{
    /**
     * Start of the test, so log it and initialise.
     * @param name Name of the test case (not used)
     */
    public ClassUtilsTest(String name)
    {
        super(name);
    }

    /**
     * Test for the methods "getBitFromInt" and "setBitInInt"
     */
    public void testBitsStoredInInt()
    {
        // Try 0
        int bits = 0;
        for (int i=0;i<32;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }

        // Set bit 5
        bits = ClassUtils.setBitInInt(bits, 5, true);
        for (int i=0;i<5;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }
        assertTrue(ClassUtils.getBitFromInt(bits, 5));
        for (int i=6;i<32;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }

        // Set bits 6-8
        bits = ClassUtils.setBitInInt(bits, 6, true);
        bits = ClassUtils.setBitInInt(bits, 7, true);
        bits = ClassUtils.setBitInInt(bits, 8, true);
        for (int i=0;i<5;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }
        for (int i=5;i<9;i++)
        {
            assertTrue(ClassUtils.getBitFromInt(bits, i));
        }
        for (int i=9;i<32;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }

        // Unset bit 7
        bits = ClassUtils.setBitInInt(bits, 7, false);
        for (int i=0;i<5;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }
        for (int i=5;i<7;i++)
        {
            assertTrue(ClassUtils.getBitFromInt(bits, i));
        }
        assertFalse(ClassUtils.getBitFromInt(bits, 7));
        assertTrue(ClassUtils.getBitFromInt(bits, 8));
        for (int i=9;i<32;i++)
        {
            assertFalse(ClassUtils.getBitFromInt(bits, i));
        }

    }

    /**
     * Test of whether a class is an inner class
     */
    public void testIsInnerClass()
    {
        assertTrue("Should have identified inner class, but failed", ClassUtils.isInnerClass("java.text.DateFormat$Field"));
    }
    
    /**
     * Test of whether a class has a default constructor
     */
    public void testHasDefaultConstructor()
    {
        assertTrue("Class java.lang.String should have been identified as having a default constructor, but failed",
            ClassUtils.hasDefaultConstructor(java.lang.String.class));
        assertTrue("Class java.sql.DriverPropertyInfo should have been identified as not having a default constructor, but passed",
            !ClassUtils.hasDefaultConstructor(java.sql.DriverPropertyInfo.class));
    }

    /**
     * Test for the constructor with arguments.
     */
    public void testGetConstructorWithArguments()
    {
        Class[] argsEmpty = {};
        assertNotNull("", ClassUtils.getConstructorWithArguments(MyConstructorClass.class, argsEmpty));

        Class[] argsBase = {MyBaseClass.class};
        assertNotNull("", ClassUtils.getConstructorWithArguments(MyConstructorClass.class, argsBase));

        Class[] argsDerived = {MyDerivedClass.class};
        assertNotNull("", ClassUtils.getConstructorWithArguments(MyConstructorClass.class, argsDerived));

        Class[] argsString = {String.class};
        assertNull("", ClassUtils.getConstructorWithArguments(MyConstructorClass.class, argsString));
    }

    /**
     * Test for the superclasses of a class.
     */
    public void testGetSuperclasses()
    {
        Collection<Class<?>> superclasses=ClassUtils.getSuperclasses(java.util.ArrayList.class);
        assertTrue("java.util.ArrayList should have had 3 superclasses, but had " + superclasses.size(),
            superclasses.size() == 3);
        assertTrue("java.util.ArrayList should have had a superclass of AbstractList, but didn't !",
            superclasses.contains(AbstractList.class));
        assertTrue("java.util.ArrayList should have had a superclass of AbstractCollection, but didn't !",
            superclasses.contains(AbstractCollection.class));
        assertTrue("java.util.ArrayList should have had a superclass of Object, but didn't !",
            superclasses.contains(Object.class));
    }

    /**
     * Test for the superinterfaces of a class.
     */
    public void testGetSuperinterfaces()
    {
        Collection<Class<?>> superintfs = ClassUtils.getSuperinterfaces(java.util.ArrayList.class);

        assertTrue("java.util.ArrayList should have had 6 superinterfaces, but had " + superintfs.size(),
            superintfs.size() == 6);
        assertTrue("java.util.ArrayList should have had a superinterface of List, but didn't !",
            superintfs.contains(List.class));
        assertTrue("java.util.ArrayList should have had a superinterface of Collection, but didn't !",
            superintfs.contains(Collection.class));
        assertTrue("java.util.ArrayList should have had a superinterface of Iterable, but didn't !",
            superintfs.contains(Iterable.class));
        assertTrue("java.util.ArrayList should have had a superinterface of RandomAccess, but didn't !",
            superintfs.contains(RandomAccess.class));
        assertTrue("java.util.ArrayList should have had a superinterface of Cloneable, but didn't !",
            superintfs.contains(Cloneable.class));
        assertTrue("java.util.ArrayList should have had a superinterface of Serrializable, but didn't !",
            superintfs.contains(Serializable.class));
    }

    /**
     * Test for the superinterfaces of a class where there is a "diamond" inheritance.
     * There should be no duplicate entries in the result.
     */
    public void testGetSuperinterfacesDiamond()
    {
        Collection<Class<?>> superintfs = ClassUtils.getSuperinterfaces(ListSet.class);

        assertTrue("ListSet should have had 4 superinterfaces, but had " + superintfs.size(),
            superintfs.size() == 4);
        assertTrue("ListSet should have had a superinterface of List, but didn't !",
            superintfs.contains(List.class));
        assertTrue("ListSet should have had a superinterface of Collection, but didn't !",
            superintfs.contains(Collection.class));
        assertTrue("ListSet should have had a superinterface of Iterable, but didn't !",
            superintfs.contains(Iterable.class));
        assertTrue("ListSet should have had a superinterface of Set, but didn't !",
            superintfs.contains(Set.class));
    }

    private static abstract class ListSet<T> implements List<T>, Set<T>
    {
        // Class for use by testGetSuperinterfacesDiamond
    }

    /**
     * Test for the superinterfaces of a class where some interfaces can only be reached via the superclass.
     */
    public void testGetSuperinterfacesViaSuperclass()
    {
        Collection<Class<?>> superintfs = ClassUtils.getSuperinterfaces(MyArrayList.class);

        assertTrue("java.util.ArrayList should have had 0 superinterfaces, but had " + superintfs.size(),
            superintfs.size() == 0);

        // TODO Include interfaces of superclasses? (see ClassUtils.collectSuperinterfaces)

//        assertTrue("java.util.ArrayList should have had 6 superinterfaces, but had " + superintfs.size(),
//            superintfs.size() == 6);
//        assertTrue("java.util.ArrayList should have had a superinterface of List, but didn't !",
//            superintfs.contains(List.class));
//        assertTrue("java.util.ArrayList should have had a superinterface of Collection, but didn't !",
//            superintfs.contains(Collection.class));
//        assertTrue("java.util.ArrayList should have had a superinterface of Iterable, but didn't !",
//            superintfs.contains(Iterable.class));
//        assertTrue("java.util.ArrayList should have had a superinterface of RandomAccess, but didn't !",
//            superintfs.contains(RandomAccess.class));
//        assertTrue("java.util.ArrayList should have had a superinterface of Cloneable, but didn't !",
//            superintfs.contains(Cloneable.class));
//        assertTrue("java.util.ArrayList should have had a superinterface of Serrializable, but didn't !",
//            superintfs.contains(Serializable.class));
    }

    private static abstract class MyArrayList extends java.util.ArrayList
    {

        private static final long serialVersionUID = 9213661177131836840L;
        // Class for use by testGetSuperinterfacesViaSuperclass
    }

    /**
     * Test for the creation of a fully specified class name.
     */
    public void testCreateFullClassName()
    {
        assertTrue("Full classname is incorrect",
            ClassUtils.createFullClassName("org.datanucleus.samples","MyClass").equals("org.datanucleus.samples.MyClass"));
        assertTrue("Full classname is incorrect",
            ClassUtils.createFullClassName("     ","MyClass").equals("MyClass"));
        assertTrue("Full classname is incorrect",
            ClassUtils.createFullClassName("org","MyClass").equals("org.MyClass"));
    }
    
    /**
     * Test of the retrieval of the classname for a filename, given the root directory name.
     * @throws MalformedURLException 
     */
    public void testGetClassnameForFilename() throws MalformedURLException
    {
        String externalForm = getClass().getResource("/org/datanucleus/util/ClassUtilsTest.class").toExternalForm();
        externalForm = externalForm.replace("file:/","");
        String path = externalForm.substring(0,externalForm.indexOf("/org/datanucleus/util/ClassUtilsTest.class"));
        externalForm = externalForm.replace("/", System.getProperty("file.separator"));
        externalForm = externalForm.replace("\\", System.getProperty("file.separator"));
        assertTrue("Classname for filename is incorrect",
            ClassUtils.getClassnameForFilename(externalForm, path).equals("org.datanucleus.util.ClassUtilsTest"));
    }
    
    /**
     * Test for whether classes are descendents of each other.
     */
    public void testClassesAreDescendents()
    {
        ClassLoaderResolver clr = new ClassLoaderResolverImpl(null);
        
        assertTrue("java.util.Collection and java.util.ArrayList should have been direct descendents but weren't",
            ClassUtils.classesAreDescendents(clr, "java.util.Collection", "java.util.ArrayList"));
        assertTrue("java.util.ArrayList and java.util.Collection should have been direct descendents but weren't",
            ClassUtils.classesAreDescendents(clr, "java.util.ArrayList", "java.util.Collection"));
        assertTrue("java.util.ArrayList and java.lang.String shouldn't have been direct descendents but were",
            !ClassUtils.classesAreDescendents(clr, "java.util.ArrayList", "java.lang.String"));
    }

    /**
     * Test for java bean getter name generator.
     */
    public void testJavaBeanGetterName()
    {
        assertEquals("Incorrect Java Bean getter name", "getParam", ClassUtils.getJavaBeanGetterName("param", false));
        assertEquals("Incorrect Java Bean getter name", "getABC", ClassUtils.getJavaBeanGetterName("ABC", false));
        assertEquals("Incorrect Java Bean getter name", "getA", ClassUtils.getJavaBeanGetterName("a", false));
        assertEquals("Incorrect Java Bean getter name", "isParam", ClassUtils.getJavaBeanGetterName("param", true));
        assertEquals("Incorrect Java Bean getter name", "isABC", ClassUtils.getJavaBeanGetterName("ABC", true));
        assertEquals("Incorrect Java Bean getter name", "isA", ClassUtils.getJavaBeanGetterName("a", true));
    }

    /**
     * Test for java bean setter name generator.
     */
    public void testJavaBeanSetterName()
    {
        assertEquals("Incorrect Java Bean setter name", "setParam", ClassUtils.getJavaBeanSetterName("param"));
        assertEquals("Incorrect Java Bean setter name", "setABC", ClassUtils.getJavaBeanSetterName("ABC"));
        assertEquals("Incorrect Java Bean setter name", "setA", ClassUtils.getJavaBeanSetterName("a"));
    }

    /**
     * Test for field name for java bean getter.
     */
    public void testFieldNameForJavaBeanGetter()
    {
        assertEquals("Incorrect field name for Java Bean getter", "param", ClassUtils.getFieldNameForJavaBeanGetter("getParam"));
        assertEquals("Incorrect field name for Java Bean getter", "ABC", ClassUtils.getFieldNameForJavaBeanGetter("getABC"));
        assertEquals("Incorrect field name for Java Bean getter", "a", ClassUtils.getFieldNameForJavaBeanGetter("getA"));
        assertEquals("Incorrect field name for Java Bean getter", "param", ClassUtils.getFieldNameForJavaBeanGetter("isParam"));
        assertEquals("Incorrect field name for Java Bean getter", "ABC", ClassUtils.getFieldNameForJavaBeanGetter("isABC"));
        assertEquals("Incorrect field name for Java Bean getter", "a", ClassUtils.getFieldNameForJavaBeanGetter("isA"));
    }

    /**
     * Test for field name for java bean setter.
     */
    public void testFieldNameForJavaBeanSetter()
    {
        assertEquals("Incorrect field name for Java Bean setter", "param", ClassUtils.getFieldNameForJavaBeanSetter("setParam"));
        assertEquals("Incorrect field name for Java Bean setter", "ABC", ClassUtils.getFieldNameForJavaBeanSetter("setABC"));
        assertEquals("Incorrect field name for Java Bean setter", "a", ClassUtils.getFieldNameForJavaBeanSetter("setA"));
    }

    /**
     * Test for field for class
     */
    public void testFieldForClass()
    {
        assertNull(ClassUtils.getFieldForClass(MyDerivedClass.class, "missing"));
        assertEquals("Incorrect field", "one", ClassUtils.getFieldForClass(MyDerivedClass.class, "one").getName());
        assertEquals("Incorrect field", "two", ClassUtils.getFieldForClass(MyDerivedClass.class, "two").getName());
    }

    /**
     * Test for getter method for class.
     */
    public void testGetterMethodForClass()
    {
        assertNull(ClassUtils.getGetterMethodForClass(MyDerivedClass.class, "missing"));
        assertEquals("Incorrect getter method", "getOne", ClassUtils.getGetterMethodForClass(MyDerivedClass.class, "one").getName());
        assertEquals("Incorrect getter method", "getTwo", ClassUtils.getGetterMethodForClass(MyDerivedClass.class, "two").getName());
        assertEquals("Incorrect getter method", "getThree", ClassUtils.getGetterMethodForClass(MyDerivedClass.class, "three").getName());
        assertEquals("Incorrect getter method", "getFour", ClassUtils.getGetterMethodForClass(MyDerivedClass.class, "four").getName());
        assertEquals("Incorrect getter method", "isFive", ClassUtils.getGetterMethodForClass(MyDerivedClass.class, "five").getName());
    }

    /**
     * Test for setter method for class.
     */
    public void testSetterMethodForClass()
    {
        assertNull(ClassUtils.getSetterMethodForClass(MyDerivedClass.class, "missing", Object.class));
        assertEquals("Incorrect setter method", "setOne", ClassUtils.getSetterMethodForClass(MyDerivedClass.class, "one", Object.class).getName());
        assertEquals("Incorrect setter method", "setTwo", ClassUtils.getSetterMethodForClass(MyDerivedClass.class, "two", Object.class).getName());
    }

    public class MyPCClass implements Persistable
    {
        public ExecutionContext dnGetExecutionContext()
        {
            return null;
        }

        public void dnReplaceStateManager(StateManager arg0) throws SecurityException
        {
        }

        public void dnProvideField(int arg0)
        {
        }

        public void dnProvideFields(int[] arg0)
        {
        }

        public void dnReplaceField(int arg0)
        {
        }

        public void dnReplaceFields(int[] arg0)
        {
        }

        public void dnReplaceFlags()
        {
        }

        public void dnCopyFields(Object arg0, int[] arg1)
        {
        }

        public void dnMakeDirty(String arg0)
        {
        }

        public Object dnGetObjectId()
        {
            return null;
        }

        public Object dnGetTransactionalObjectId()
        {
            return null;
        }

        public Object dnGetVersion()
        {
            return null;
        }

        public boolean dnIsDirty()
        {
            return false;
        }

        public boolean dnIsTransactional()
        {
            return false;
        }

        public boolean dnIsPersistent()
        {
            return false;
        }

        public boolean dnIsNew()
        {
            return false;
        }

        public boolean dnIsDeleted()
        {
            return false;
        }

        public boolean dnIsDetached()
        {
            return false;
        }

        public Persistable dnNewInstance(StateManager arg0)
        {
            return null;
        }

        public Persistable dnNewInstance(StateManager arg0, Object arg1)
        {
            return null;
        }

        public Object dnNewObjectIdInstance()
        {
            return null;
        }

        public Object dnNewObjectIdInstance(Object arg0)
        {
            return null;
        }

        public void dnCopyKeyFieldsToObjectId(Object arg0)
        {
        }

        public void dnCopyKeyFieldsToObjectId(ObjectIdFieldSupplier arg0, Object arg1)
        {
        }

        public void dnCopyKeyFieldsFromObjectId(ObjectIdFieldConsumer arg0, Object arg1)
        {
        }
    }

    public class MyChildPCClass extends MyPCClass
    {
    }

    class MyBaseClass
    {
        public Object two;
        public Object getTwo()
        {
            return two;
        }
        public Object getFour()
        {
            return null;
        }
        public void setTwo(Object two)
        {
            this.two = two;
        }
    }

    class MyDerivedClass extends MyBaseClass
    {
        public Object one;
        public Object getOne()
        {
            return one;
        }
        public Object getThree()
        {
            return null;
        }
        public Object isThree()
        {
            return null;
        }
        public Object isFour()
        {
            return null;
        }
        public Object isFive()
        {
            return null;
        }
        public void setOne(Object one)
        {
            this.one = one;
        }
    }
    
    
}

class MyConstructorClass {
	
	public MyConstructorClass() 
	{
	}
	
	public MyConstructorClass(MyBaseClass arg) 
	{
	}
	
}