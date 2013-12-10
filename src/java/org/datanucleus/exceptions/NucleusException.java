/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.exceptions;

/**
 * Base exception thrown by DataNucleus.
 */
public class NucleusException extends RuntimeException
{
    /** Array of nested Throwables (optional) */
    Throwable[] nested;

    /** The object being processed when the error was encountered (optional). */
    Object failed;

    /** Flag defining if this exception is fatal, or could be retried with the possibility of success. */
    boolean fatal;

    /**
     * Constructs a new exception without a detail message.
     */
    public NucleusException()
    {
    }

    /**
     * Constructs a new exception with the specified detail message.
     * @param msg the detail message.
     */
    public NucleusException(String msg)
    {
        super(msg);
    }

    /**
     * Constructs a new exception with the specified detail message and nested <code>Throwable</code>s.
     * @param msg the detail message.
     * @param nested the nested <code>Throwable[]</code>.
     */
    public NucleusException(String msg, Throwable[] nested)
    {
        super(msg);
        this.nested = nested;
    }

    /**
     * Constructs a new exception with the specified detail message and nested <code>Throwable</code>.
     * @param msg the detail message.
     * @param nested the nested <code>Throwable</code>.
     */
    public NucleusException(String msg, Throwable nested)
    {
        super(msg);
        this.nested = new Throwable[]{nested};
    }

    /**
     * Constructs a new exception with the specified detail message and failed object.
     * @param msg the detail message.
     * @param failed the failed object.
     */
    public NucleusException(String msg, Object failed)
    {
        super(msg);
        this.failed = failed;
    }

    /**
     * Constructs a new exception with the specified detail
     * message, nested <code>Throwable</code>s, and failed object.
     * @param msg the detail message.
     * @param nested the nested <code>Throwable[]</code>.
     * @param failed the failed object.
     */
    public NucleusException(String msg, Throwable[] nested, Object failed)
    {
        super(msg);
        this.nested = nested;
        this.failed = failed;
    }

    /**
     * Constructs a new exception with the specified detail message, nested <code>Throwable</code>,
     * and failed object.
     * @param msg the detail message.
     * @param nested the nested <code>Throwable</code>.
     * @param failed the failed object.
     */
    public NucleusException(String msg, Throwable nested, Object failed)
    {
        super(msg);
        this.nested = new Throwable[]{nested};
        this.failed = failed;
    }

    /**
     * Method to set the exception as being fatal.
     * Returns the exception so that user code can call
     * "throw new NucleusException(...).setFatal();"
     * @return This exception (for convenience)
     */
    public NucleusException setFatal()
    {
        fatal = true;
        return this;
    }

    /**
     * Accessor for whether the exception is fatal, or retriable.
     * @return Whether it is fatal
     */
    public boolean isFatal()
    {
        return fatal;
    }

    /**
     * The exception may include a failed object.
     * @return the failed object.
     */
    public Object getFailedObject()
    {
        return failed;
    }

    public void setNestedException(Throwable nested)
    {
        this.nested = new Throwable[] {nested};
    }

    /**
     * The exception may have been caused by multiple exceptions in the runtime.
     * If multiple objects caused the problem, each failed object will have its
     * own <code>Exception</code>.
     * @return the nested Throwable array.
     */
    public Throwable[] getNestedExceptions()
    {
        return nested;
    }

    /**
     * Return the first nested exception (if any), otherwise null.
     * @return the first or only nested Throwable.
     */
    public synchronized Throwable getCause()
    {
        return ((nested == null || nested.length == 0) ? null : nested[0]);
    }
    
    /**
     * Prints this <code>Exception</code> and its backtrace to the standard
     * error output. Print nested Throwables' stack trace as well.
     */
    public void printStackTrace()
    {
        printStackTrace(System.err);
    }

    /**
     * Prints this <code>Exception</code> and its backtrace to the
     * specified print stream. Print nested Throwables' stack trace as well.
     * @param s <code>PrintStream</code> to use for output
     */
    public synchronized void printStackTrace(java.io.PrintStream s)
    {
        int len = nested == null ? 0 : nested.length;
        synchronized (s)
        {
            if( getMessage() != null )
            {
                s.println(getMessage());
            }
            super.printStackTrace(s);
            if (len > 0)
            {
                s.println("Nested Throwables StackTrace:");
                for (int i = 0; i < len; ++i)
                {
                    Throwable exception = nested[i];
                    if (exception != null)
                    {
                        exception.printStackTrace(s);
                    }
                }
            }
        }
    }

    /**
     * Prints this <code>Exception</code> and its backtrace to the
     * specified print writer. Print nested Throwables' stack trace as well.
     * @param s <code>PrintWriter</code> to use for output
     */
    public synchronized void printStackTrace(java.io.PrintWriter s)
    {
        int len = nested == null ? 0 : nested.length;
        synchronized (s)
        {
            if( getMessage() != null )
            {
                s.println(getMessage());
            }
            super.printStackTrace(s);
            if (len > 0)
            {
                s.println("Nested Throwables StackTrace:");
                for (int i = 0; i < len; ++i)
                {
                    Throwable exception = nested[i];
                    if (exception != null)
                    {
                        exception.printStackTrace(s);
                    }
                }
            }
        }
    }
      
}