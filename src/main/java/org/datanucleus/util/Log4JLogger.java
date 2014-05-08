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

import org.apache.log4j.Logger;

/**
 * Log4J implementation of a NucleusLogger.
 * See http://logging.apache.org/log4j for details of Log4J.
 * Assumes that all configuration of the loggers are done by external
 * configuration (System property "log4j.configuration").
 * Maps logging levels as follows :
 * <ul>
 * <li>debug maps to Log4J <i>DEBUG</i></li>
 * <li>info maps to Log4J <i>INFO</i></li>
 * <li>warn maps to Log4J <i>WARN</i></li>
 * <li>error maps to Log4J <i>ERROR</i></li>
 * <li>fatal maps to Log4J <i>FATAL</i></li>
 * </ul>
 */
public class Log4JLogger extends NucleusLogger
{
    /** The Log4J logger being used */
    private Logger logger = null;

    /**
     * Constructor using Log4J.
     * @param logName Name of the logging category
     */
    public Log4JLogger(String logName)
    {
        // Logging assumed to be configured by user via "log4j.configuration"
        // Use Logger instead of LogManager so that "logback" (http://logback.qos.ch) can work
        logger = Logger.getLogger(logName);
    }

    /**
     * Log a debug message.
     * @param msg The message
     */
    public void debug(Object msg)
    {
        logger.debug(msg);
    }

    /**
     * Log a debug message with throwable.
     * @param msg The message
     * @param thr A throwable
     */
    public void debug(Object msg, Throwable thr)
    {
        logger.debug(msg, thr);
    }

    /**
     * Log an info message.
     * @param msg The message
     */
    public void info(Object msg)
    {
        logger.info(msg);
    }

    /**
     * Log an info message with throwable.
     * @param msg The message
     * @param thr A throwable
     */
    public void info(Object msg, Throwable thr)
    {
        logger.info(msg, thr);
    }

    /**
     * Log a warning message.
     * @param msg The message
     */
    public void warn(Object msg)
    {
        logger.warn(msg);
    }

    /**
     * Log a warning message with throwable.
     * @param msg The message
     * @param thr A throwable
     */
    public void warn(Object msg, Throwable thr)
    {
         logger.warn(msg, thr);
    }

    /**
     * Log an error message.
     * @param msg The message
     */
    public void error(Object msg)
    {
         logger.error(msg);
    }

    /**
     * Log an error message with throwable.
     * @param msg The message
     * @param thr A throwable
     */
    public void error(Object msg, Throwable thr)
    {
        logger.error(msg, thr);
    }

    /**
     * Log an fatal message.
     * @param msg The message
     */
    public void fatal(Object msg)
    {
        logger.fatal(msg);
    }

    /**
     * Log a fatal message with throwable.
     * @param msg The message
     * @param thr A throwable
     */
    public void fatal(Object msg, Throwable thr)
    {
        logger.fatal(msg, thr);
    }

    /**
     * Accessor for whether debug logging is enabled
     * @return Whether it is enabled
     */
    public boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    /**
     * Accessor for whether info logging is enabled
     * @return Whether it is enabled
     */
    public boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }
}