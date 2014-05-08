/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer;

import org.datanucleus.exceptions.NucleusException;

/**
 * Exception thrown during enhancement when an error occurs.
 */
public class NucleusEnhanceException extends NucleusException
{
    /**
     * Message-based exception constructor.
     * @param msg The message
     */
    public NucleusEnhanceException(String msg)
    {
        super(msg);
    }

    /**
     * @param msg The message
     * @param nested Nested exceptions
     */
    public NucleusEnhanceException(String msg, Throwable[] nested)
    {
        super(msg, nested);
    }

    /**
     * @param msg The message
     * @param nested Nested exception
     */
    public NucleusEnhanceException(String msg, Throwable nested)
    {
        super(msg, nested);
    }
}