/**********************************************************************
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
**********************************************************************/
package org.datanucleus.metadata;

/**
 * Representation of the primary query languages.
 * Other query languages can be supported but this is just for the primary ones as shortcuts.
 */
public enum QueryLanguage
{
    JDOQL,
    SQL,
    JPQL,
    STOREDPROC;

    /**
     * Return QueryLanguage from String.
     * @param value identity-type attribute value
     * @return Instance of QueryLanguage. If parse failed, return null.
     */
    public static QueryLanguage getQueryLanguage(final String value)
    {
        if (value == null)
        {
            // Default to JDOQL if nothing passed in
            return QueryLanguage.JDOQL;
        }
        else if (QueryLanguage.JDOQL.toString().equalsIgnoreCase(value))
        {
            return QueryLanguage.JDOQL;
        }
        else if (QueryLanguage.SQL.toString().equalsIgnoreCase(value))
        {
            return QueryLanguage.SQL;
        }
        else if (QueryLanguage.JPQL.toString().equalsIgnoreCase(value))
        {
            return QueryLanguage.JPQL;
        }
        else if (QueryLanguage.STOREDPROC.toString().equalsIgnoreCase(value))
        {
            return QueryLanguage.STOREDPROC;
        }
        return null;
    }
}