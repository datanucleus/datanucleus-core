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
 * MetaData representing a unique constraint.
 */
public class UniqueMetaData extends AbstractConstraintMetaData implements ColumnMetaDataContainer
{
    /** Whether the unique is initially deferred. */
    boolean deferred = false;

    /**
     * Constructor to create a copy of the passed metadata using the provided parent.
     * @param umd The metadata to copy
     */
    public UniqueMetaData(UniqueMetaData umd)
    {
        super(umd);
        this.deferred = umd.deferred;
    }

    /**
     * Default constructor. Set fields using setters, before populate().
     */
    public UniqueMetaData()
    {
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = (StringUtils.isWhitespace(name) ? null : name);
    }

    public String getTable()
    {
        return table;
    }

    public void setTable(String table)
    {
        this.table = (StringUtils.isWhitespace(table) ? null : table);
    }

    public final boolean isDeferred()
    {
        return deferred;
    }

    public UniqueMetaData setDeferred(boolean deferred)
    {
        this.deferred = deferred;
        return this;
    }

    // -------------------------------- Utilities ------------------------------

    /**
     * Returns a string representation of the object.
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<unique");
        if (table != null)
        {
            sb.append(" table=\"" + table + "\"");
        }
        if (deferred)
        {
            sb.append(" deferred=\"true\"");
        }
        sb.append(name != null ? (" name=\"" + name + "\">\n") : ">\n");

        if (memberNames != null)
        {
            for (String memberName : memberNames)
            {
                sb.append(prefix).append(indent).append("<field name=\"" + memberName + "\"/>");
            }
        }
        if (columns != null)
        {
            for (ColumnMetaData colmd : columns)
            {
                sb.append(colmd.toString(prefix + indent,indent));
            }
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</unique>\n");
        return sb.toString();
    }
}