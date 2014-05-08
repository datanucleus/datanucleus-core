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
     * Default Constructor
     */
    public InvalidAnnotationException()
    {
        super();
        setFatal();
    }

    /**
     * Constructor with message resource and cause exception
     * @param localiser message resources
     * @param key message resources key
     * @param cause cause exception
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Throwable cause)
    {
        this(localiser, key, "", "", "");
        this.cause = cause;
        setFatal();
    }

    /**
     * Constructor with message resource, message param and cause exception
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param0
     * @param cause cause exception
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Throwable cause)
    {
        this(localiser, key, param1, "", "");
        this.cause = cause;
        setFatal();
    }

    /**
     * Constructor with message resource, message params and cause exception
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param0
     * @param param2 message resources param1
     * @param cause cause exception
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Object param2, Throwable cause)
    {
        this(localiser, key, param1, param2, "");
        this.cause = cause;
        setFatal();
    }

    /**
     * Constructor with message resource, message params and cause exception
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param0
     * @param param2 message resources param1
     * @param param3 message resources param2
     * @param cause cause exception
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Object param2, Object param3, Throwable cause)
    {
        super(localiser.msg(key, param1, param2, param3));
        this.messageKey = key;
        this.cause = cause;
        setFatal();
    }

    /**
     * Constructor with message resource 
     * @param localiser message resources
     * @param key message resources key
     */
    public InvalidAnnotationException(Localiser localiser, String key)
    {
        this(localiser, key, "", "", "");
        setFatal();
    }

    /**
     * Constructor with message resource, message param
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param0
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1)
    {
        this(localiser, key, param1, "", "");
        setFatal();
    }

    /**
     * Constructor with message resource, message params
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param0
     * @param param2 message resources param1
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Object param2)
    {
        this(localiser, key, param1, param2, "");
        setFatal();
    }

    /**
     * Constructor with message resource, message params
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param0
     * @param param2 message resources param1
     * @param param3 message resources param2
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Object param2, Object param3)
    {
        super(localiser.msg(key, param1, param2, param3));
        this.messageKey = key;
        setFatal();
    }

    /**
     * Constructor with message resource, message params
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param1
     * @param param2 message resources param2
     * @param param3 message resources param3
     * @param param4 message resources param4
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Object param2, Object param3, Object param4)
    {
        super(localiser.msg(key, param1, param2, param3, param4));
        this.messageKey = key;
        setFatal();
    }
    
    /**
     * Constructor with message resource, message params
     * @param localiser message resources
     * @param key message resources key
     * @param param1 message resources param1
     * @param param2 message resources param2
     * @param param3 message resources param3
     * @param param4 message resources param4
     * @param param5 message resources param5
     */
    public InvalidAnnotationException(Localiser localiser, String key,
        Object param1, Object param2, Object param3, Object param4, Object param5)
    {
        super(localiser.msg(key, param1, param2, param3, param4, param5));
        this.messageKey = key;
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