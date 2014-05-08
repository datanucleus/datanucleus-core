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

import org.datanucleus.util.StringUtils;

/**
 * Representation of whether an item is indexed or not.
 */
public enum IndexedValue
{
    TRUE,
    FALSE,
    UNIQUE;

    /**
     * Obtain a IndexedValue for the given name by <code>value</code>
     * @param value the name
     * @return the IndexedValue found or IndexedValue.TRUE if not found. If <code>value</code> is null, returns null.
     */
    public static IndexedValue getIndexedValue(final String value)
    {
        if (StringUtils.isWhitespace(value))
        {
            return null;
        }
        else if (IndexedValue.TRUE.toString().equals(value))
        {
            return IndexedValue.TRUE;
        }
        else if (IndexedValue.FALSE.toString().equals(value))
        {
            return IndexedValue.FALSE;
        }
        else if (IndexedValue.UNIQUE.toString().equals(value))
        {
            return IndexedValue.UNIQUE;
        }
        return IndexedValue.TRUE;
    }
}