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
package org.datanucleus.query;

import junit.framework.TestCase;

/**
 * Test for some QueryUtils convenience methods.
 */
public class QueryUtilsTest extends TestCase
{
    public QueryUtilsTest(String name)
    {
        super(name);
    }
    
    public void testClassical()
    {
        Object[] fieldValues = new Object[2];
        fieldValues[0] = new Long(2);
        fieldValues[1] = "jerome";
        
        Object myObject = QueryUtils.createResultObjectUsingArgumentedConstructor(MyResultClass.class, 
            fieldValues, new Class[]{Long.class, String.class});
        assertNotNull("My Object should not be null",myObject);
        
        assertTrue("myObject should be instance of ResultClassBean", myObject instanceof MyResultClass);
        MyResultClass myResult = (MyResultClass)myObject;
        assertEquals("id should be the same",fieldValues[0],myResult.getId());
        assertEquals("userId should be the same",fieldValues[1],myResult.getUserId());
    }
    
    public void testNullField()
    {
        Object[] fieldValues = new Object[2];
        fieldValues[0] = new Long(2);
        fieldValues[1] = null;
        Object myObject = QueryUtils.createResultObjectUsingArgumentedConstructor(MyResultClass.class, 
            fieldValues, new Class[] {Long.class, String.class});
        assertNotNull("My Object should not be null",myObject);
        assertTrue("myObject should be instance of ResultClassBean", myObject instanceof MyResultClass);
        MyResultClass myResult = (MyResultClass)myObject;
        assertEquals("id should be the same",fieldValues[0],myResult.getId());
        assertEquals("userId should be the same",fieldValues[1],myResult.getUserId());
    }

}