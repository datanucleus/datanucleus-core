/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.metadata;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * Exception thrown when an annotation has been specified that is invalid in the circumstances.
 */
public class InvalidAnnotationException extends NucleusUserException
{
    /** Message resources key */
    protected String messageKey;

    /** Cause of the exception */
    protected Throwable cause;

    /**
     * Constructor with message resource, message param and cause exception
     * @param key message resources key
     * @param params parameters
     * @param cause cause exception
     */
    public InvalidAnnotationException(String key, Throwable cause, Object... params)
    {
        this(key, params);
        this.cause = cause;
        setFatal();
    }

    /**
     * Constructor with message resource, message params
     * @param key message resources key
     * @param params parameters to the message
     */
    public InvalidAnnotationException(String key, Object... params)
    {
        super(Localiser.msg(key, params));
        setFatal();
    }

    /**
     * Return message resource key
     * @return Message resource key
     */
    public String getMessageKey()
    {
        return messageKey;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace()
     */
    public void printStackTrace()
    {
        super.printStackTrace();
        if (cause != null)
        {
            cause.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
     */
    public void printStackTrace(PrintStream s)
    {
        super.printStackTrace(s);
        if (cause != null)
        {
            cause.printStackTrace(s);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
     */
    public void printStackTrace(PrintWriter s)
    {
        super.printStackTrace(s);
        if (cause != null)
        {
            cause.printStackTrace(s);
        }
    }
}