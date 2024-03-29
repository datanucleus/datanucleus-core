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
package org.datanucleus.util;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * JRE logger (java.util.logging) implementation of a NucleusLogger.
 * Assumes that all configuration of the loggers are done by external configuration (System property "java.util.logging.config.file").
 * Maps logging levels as follows :
 * <ul>
 * <li>debug maps to java.util.logging <i>fine</i></li>
 * <li>info maps to java.util.logging <i>info</i></li>
 * <li>warn maps to java.util.logging <i>warning</i></li>
 * <li>error maps to java.util.logging <i>severe</i></li>
 * <li>fatal maps to java.util.logging <i>severe</i></li>
 * </ul>
 */
public class JRELogger extends NucleusLogger
{
    /** The JRE Logger being used */
    private final Logger logger;

    /**
     * Constructor for a JRE Logger.
     * @param logName Name of the logger
     */
    public JRELogger(String logName)
    {
        // Logging assumed to be configured by user via "java.util.logging.config.file"
        logger = Logger.getLogger(logName);
    }

    @Override
    public Object getNativeLogger()
    {
        return logger;
    }

    @Override
    public void debug(Object msg)
    {
        log(Level.FINE, msg, null);
    }

    @Override
    public void debug(Object msg, Throwable thr)
    {
        log(Level.FINE, msg, thr);
    }

    @Override
    public void info(Object msg)
    {
        log(Level.INFO, msg, null);
    }

    @Override
    public void info(Object msg, Throwable thr)
    {
        log(Level.INFO, msg, thr);
    }

    @Override
    public void warn(Object msg)
    {
        log(Level.WARNING, msg, null);
    }

    @Override
    public void warn(Object msg, Throwable thr)
    {
        log(Level.WARNING, msg, thr);
    }

    @Override
    public void error(Object msg)
    {
        log(Level.SEVERE, msg, null);
    }

    @Override
    public void error(Object msg, Throwable thr)
    {
        log(Level.SEVERE, msg, thr);
    }

    @Override
    public void fatal(Object msg)
    {
        log(Level.SEVERE, msg, null);
    }

    @Override
    public void fatal(Object msg, Throwable thr)
    {
        log(Level.SEVERE, msg, thr);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return logger.isLoggable(java.util.logging.Level.FINE);
    }

    @Override
    public boolean isInfoEnabled()
    {
        return logger.isLoggable(java.util.logging.Level.INFO);
    }

    private void log(Level level, Object msg, Throwable thrown)
    {
        if (msg == null) 
        {
            level = Level.SEVERE;
            msg = "Missing [msg] parameter";
        }

        if (logger.isLoggable(level))
        {
            LogRecord result = new LogRecord(level, String.valueOf(msg));
            if (thrown != null)
            {
                result.setThrown(thrown);
            }
            StackTraceElement[] stacktrace = new Throwable().getStackTrace();
            for (int i = 0; i < stacktrace.length; i++)
            {
                StackTraceElement element = stacktrace[i];
                if (!element.getClassName().equals(JRELogger.class.getName()))
                {
                    result.setSourceClassName(element.getClassName());
                    result.setSourceMethodName(element.getMethodName());
                    break;
                }
            }
            logger.log(result);
        }
    }
}