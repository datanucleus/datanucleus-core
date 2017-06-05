/**********************************************************************
Copyright (c) 2017 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.flush;

/**
 * Flush mode for the persistence process.
 */
public enum FlushMode
{
    /** Flush automatically on any update. */
    AUTO,

    /** Flush on explicit flush()/commit() calls, and just before any query. */
    QUERY,

    /** Flush on explicit flush()/commit() calls only. */
    MANUAL;

    /**
     * Return the FlushMode from a name string.
     * @param value Flush mode string
     * @return Instance of FlushMode. Defaults to MANUAL if null
     */
    public static FlushMode getFlushModeForString(final String value)
    {
        if (value != null)
        {
            if (FlushMode.AUTO.toString().equalsIgnoreCase(value))
            {
                return FlushMode.AUTO;
            }
            else if (FlushMode.QUERY.toString().equalsIgnoreCase(value))
            {
                return FlushMode.QUERY;
            }
            else if (FlushMode.MANUAL.toString().equalsIgnoreCase(value))
            {
                return FlushMode.MANUAL;
            }
            return null;
        }
        return FlushMode.MANUAL;
    }
}