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

/**
 * Null implementation of a NucleusLogger.
 * Simply does nothing :-)
 */
public class NullLogger extends NucleusLogger
{
    /**
     * Constructor for a NucleusLogger that does nothing
     */
    public NullLogger(String logName)
    {
    }

    public void debug(Object msg)
    {
    }

    public void debug(Object msg, Throwable thr)
    {
    }

    public void error(Object msg)
    {
    }

    public void error(Object msg, Throwable thr)
    {
    }

    public void fatal(Object msg)
    {
    }

    public void fatal(Object msg, Throwable thr)
    {
    }

    public void info(Object msg)
    {
    }

    public void info(Object msg, Throwable thr)
    {
    }

    public boolean isDebugEnabled()
    {
        return false;
    }

    public boolean isInfoEnabled()
    {
        return false;
    }

    public void warn(Object msg)
    {
    }

    public void warn(Object msg, Throwable thr)
    {
    }
}