/******************************************************************
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
*****************************************************************/
package org.datanucleus.store.exceptions;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * Exception thrown when trying to update a datastores contents when it is read-only.
 * The ClassLoaderResolver is used when converting for associated JDO exception.
 */
public class DatastoreReadOnlyException extends NucleusUserException
{
    ClassLoaderResolver clr;

    /**
     * Constructor for an exception with a specified message.
     * @param msg the detail message
     */
    public DatastoreReadOnlyException(String msg, ClassLoaderResolver clr)
    {
        super(msg);
        this.clr = clr;
        setFatal();
    }

    public ClassLoaderResolver getClassLoaderResolver()
    {
        return clr;
    }
}