/**********************************************************************
Copyright (c) 2010 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus;

import java.io.IOException;
import java.util.Enumeration;

import junit.framework.TestCase;

public class ClassLoaderResolverTest extends TestCase
{

	/** test if getResources is idempotent which could be affected by caching **/
    public void testResources1() throws IOException
    {
    	ClassLoaderResolver clr = new ClassLoaderResolverImpl();
    	Enumeration urls = clr.getResources("/org/datanucleus/ClassLoaderResolverTest.class", ClassConstants.CLASS_LOADER_RESOLVER.getClassLoader());
        assertTrue(urls.hasMoreElements());
        urls.nextElement();
        assertFalse(urls.hasMoreElements());
    	urls = clr.getResources("/org/datanucleus/ClassLoaderResolverTest.class", ClassConstants.CLASS_LOADER_RESOLVER.getClassLoader());
        assertTrue(urls.hasMoreElements());
        urls.nextElement();
        assertFalse(urls.hasMoreElements());
    }
}

