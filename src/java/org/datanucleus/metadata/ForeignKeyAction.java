/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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
2006 Andy Jefferson - merged with DeleteAction, UpdateAction classes
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.io.Serializable;

/**
 * Foreign keys represent a consistency constraint in the database that must be
 * maintained. This class enumerates the actions which happens when foreign-keys
 * are updated or deleted.
 */
public class ForeignKeyAction implements Serializable
{
    /**
     * update/delete-action="cascade". The database will automatically delete all rows
     * that refer to the row being deleted
     */
    public static final ForeignKeyAction CASCADE = new ForeignKeyAction(1);

    /**
     * update/delete-action="restrict". The user is required to explicitly make the
     * relationship valid by application code
     */
    public static final ForeignKeyAction RESTRICT = new ForeignKeyAction(2);

    /**
     * update/delete-action="null". The database will automatically nullify the columns
     * in all rows that refer to the row being deleted
     */
    public static final ForeignKeyAction NULL = new ForeignKeyAction(3);

    /**
     * update/delete-action="default". The database will automatically set the columns
     * in all rows that refer to the row being deleted to their default value
     */
    public static final ForeignKeyAction DEFAULT = new ForeignKeyAction(4);

    /**
     * update/delete-action="none". No foreign-key should be created.
     */
    public static final ForeignKeyAction NONE = new ForeignKeyAction(5);

    /** The type id */
    private final int typeId;

    /**
     * constructor
     * @param i type id
     */
    protected ForeignKeyAction(int i)
    {
        this.typeId = i;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    public String toString()
    {
        switch (getType())
        {
            case 1 :
                return "cascade";
            case 2 :
                return "restrict";
            case 3 :
                return "null";
            case 4 :
                return "default";
            case 5 :
                return "none";
        }
        return "";
    }

    public int hashCode()
    {
        return typeId;
    }

    public boolean equals(Object o)
    {
        if (o instanceof ForeignKeyAction)
        {
            return ((ForeignKeyAction)o).typeId == typeId;
        }
        return false;
    }

    /**
     * Accessor for the type.
     * @return The type
     */
    protected int getType()
    {
        return typeId;
    }

    /**
     * Return ForeignKeyDeleteAction from String.
     * @param value delete-action attribute value
     * @return Instance of ForeignKeyDeleteAction. 
     *         If value invalid, return null.
     */
    public static ForeignKeyAction getForeignKeyAction(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else if (ForeignKeyAction.CASCADE.toString().equalsIgnoreCase(value))
        {
            return ForeignKeyAction.CASCADE;
        }
        else if (ForeignKeyAction.DEFAULT.toString().equalsIgnoreCase(value))
        {
            return ForeignKeyAction.DEFAULT;
        }
        else if (ForeignKeyAction.NULL.toString().equalsIgnoreCase(value))
        {
            return ForeignKeyAction.NULL;
        }
        else if (ForeignKeyAction.RESTRICT.toString().equalsIgnoreCase(value))
        {
            return ForeignKeyAction.RESTRICT;
        }
        else if (ForeignKeyAction.NONE.toString().equalsIgnoreCase(value))
        {
            return ForeignKeyAction.NONE;
        }
        return null;
    }
}