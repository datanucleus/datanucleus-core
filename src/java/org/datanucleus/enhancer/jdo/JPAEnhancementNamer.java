/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer.jdo;

/**
 * Definition of enhancement naming for use with the JPA API.
 * Follows the JDO spec except for the a couple of exceptions that can be thrown direct from 
 * enhanced methods, whereby JPA will throw IllegalAccessException (when accessing a field that wasnt
 * detached), and IllegalStateException (when invoking a method inappropriately - shouldn't happen ever).
 * TODO Override more methods and make use of the bytecode enhancement contract classes 
 * under org.datanucleus.enhancer.spi
 */
public class JPAEnhancementNamer extends JDOEnhancementNamer
{
    private static JPAEnhancementNamer instance = null;

    public static JPAEnhancementNamer getInstance()
    {
        if (instance == null)
        {
            instance = new JPAEnhancementNamer();
        }
        return instance;
    }

    protected JPAEnhancementNamer()
    {
    }

    private static final String ACN_DetachedFieldAccessException = IllegalAccessException.class.getName().replace('.', '/');
    private static final String ACN_FatalInternalException = IllegalStateException.class.getName().replace('.', '/');

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.jdo.JDOEnhancementNamer#getDetachedFieldAccessExceptionAsmClassName()
     */
    @Override
    public String getDetachedFieldAccessExceptionAsmClassName()
    {
        return ACN_DetachedFieldAccessException;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.jdo.JDOEnhancementNamer#getFatalInternalExceptionAsmClassName()
     */
    @Override
    public String getFatalInternalExceptionAsmClassName()
    {
        return ACN_FatalInternalException;
    }
}
