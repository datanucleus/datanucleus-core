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
package org.datanucleus.cache;

import java.io.Serializable;

/**
 * Key representing a unique key for a persistable object.
 * This is used to cache a CachedPC against a key other than its primary key.
 */
public class CacheUniqueKey implements Serializable, Comparable<CacheUniqueKey>
{
    private static final long serialVersionUID = 7195826078767074981L;

    String toString = null;
    int hashCode;

    /**
     * Constructor for a unique cache key.
     * @param className Name of the class of the persistable object
     * @param fieldNames Names of the fields used to form the unique key
     * @param fieldValues Values of the fields used to form the unique key
     */
    public CacheUniqueKey(String className, String[] fieldNames, Object[] fieldValues)
    {
        int myHashCode = 1;
        myHashCode = 31 * myHashCode + (className == null ? 0 : className.hashCode());
        for (int i=0;i<fieldNames.length;i++)
        {
            myHashCode = 31 * myHashCode + fieldNames[i].hashCode();
            myHashCode = 31 * myHashCode + (fieldValues[i] != null ? fieldValues[i].hashCode() : 0);
        }
        hashCode = myHashCode;

        StringBuilder str = new StringBuilder();
        str.append(className).append(":");
        for (int i=0;i<fieldNames.length;i++)
        {
            if (i > 0)
            {
                str.append(",");
            }
            str.append("[").append(fieldNames[i]).append("=").append(fieldValues[i]).append("]");
        }
        toString = str.toString();
    }

    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public int compareTo(CacheUniqueKey o)
    {
        return hashCode - o.hashCode;
    }

    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof CacheUniqueKey))
        {
            return false;
        }
        return hashCode == other.hashCode();
    }

    public boolean equals(CacheUniqueKey other)
    {
        return hashCode == other.hashCode;
    }

    public String toString()
    {
        return toString;
    }
}