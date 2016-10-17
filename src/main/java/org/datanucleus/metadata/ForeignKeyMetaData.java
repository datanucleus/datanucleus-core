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
2004 Andy Jefferson- added toString(), "column", MetaData, javadocs
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.List;

import org.datanucleus.util.StringUtils;

/**
 * Foreign keys in metadata serve two quite different purposes. First, when
 * generating schema, the foreign key element identifies foreign keys to be
 * generated. Second, when using the database, foreign key elements identify
 * foreign keys that are assumed to exist in the database. This is important for
 * the runtime to properly order insert, update, and delete statements to avoid
 * constraint violations. A foreign-key element can be contained by a field,
 * element, key, value, or join element, if all of the columns mapped are to be
 * part of the same foreign key. A foreign-key element can be contained within a
 * class element. In this case, the column elements are mapped elsewhere, and
 * the column elements contained in the foreign-key element have only the column name.
 */
public class ForeignKeyMetaData extends ConstraintMetaData
{
    private static final long serialVersionUID = 3207934394330383432L;

    /**
     * The columns for this foreign key. 
     * Note that we don't use the "columnNames" in the superclass since the user can define the column target name also for a foreign-key.
     */
    protected List<ColumnMetaData> columns = null;

    /**
     * The unique attribute specifies whether the foreign key constraint is
     * defined to be a unique constraint as well. This is most often used with
     * one-to-one mappings.
     */
    protected boolean unique = false;

    /**
     * The deferred attribute specifies whether the foreign key constraint is
     * defined to be checked only at commit time.
     */
    protected boolean deferred = false;

    /**
     * Foreign keys represent a consistency constraint in the database that must
     * be maintained. The user can specify by the value of the delete-action
     * attribute what happens if the target row of a foreign key is deleted.
     */
    protected ForeignKeyAction deleteAction;

    /**
     * Foreign keys represent a consistency constraint in the database that must
     * be maintained. The user can specify by the update-action attribute what
     * happens if the target row of a foreign key is updated.
     */
    protected ForeignKeyAction updateAction;

    /** Alternative method of specifying FK where we just supply the string defining it (JPA crap). */
    protected String fkDefinition = null;

    protected boolean fkDefinitionApplies = false;

    public ForeignKeyMetaData()
    {
    }

    /**
     * Copy constructor.
     * @param fkmd The metadata to copy
     */
    public ForeignKeyMetaData(ForeignKeyMetaData fkmd)
    {
        super(fkmd);

        if (fkmd.columns != null)
        {
            for (ColumnMetaData colmd : fkmd.columns)
            {
                addColumn(new ColumnMetaData(colmd));
            }
        }

        this.unique = fkmd.unique;
        this.deferred = fkmd.deferred;
        this.deleteAction = fkmd.deleteAction;
        this.updateAction = fkmd.updateAction;
    }

    public void addColumn(ColumnMetaData colmd)
    {
        if (columns == null)
        {
            columns = new ArrayList<>();
        }
        columns.add(colmd);
        addColumn(colmd.getName());
        colmd.parent = this;
    }

    /**
     * Method to create a new column, add it, and return it.
     * @return The column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        addColumn(colmd);
        return colmd;
    }

    public final ColumnMetaData[] getColumnMetaData()
    {
        return (columns == null) ? null : columns.toArray(new ColumnMetaData[columns.size()]);
    }

    public final boolean isDeferred()
    {
        return deferred;
    }

    public ForeignKeyMetaData setDeferred(boolean deferred)
    {
        this.deferred = deferred;
        return this;
    }

    public ForeignKeyMetaData setDeferred(String deferred)
    {
        if (!StringUtils.isWhitespace(deferred))
        {
            this.deferred = Boolean.parseBoolean(deferred);
        }
        return this;
    }

    public final ForeignKeyAction getDeleteAction()
    {
        return deleteAction;
    }

    public void setDeleteAction(ForeignKeyAction deleteAction)
    {
        this.deleteAction = deleteAction;
    }

    public final boolean isUnique()
    {
        return unique;
    }

    public ForeignKeyMetaData setUnique(boolean unique)
    {
        this.unique = unique;
        return this;
    }

    public ForeignKeyMetaData setUnique(String unique)
    {
        if (!StringUtils.isWhitespace(unique))
        {
            this.deferred = Boolean.parseBoolean(unique);
        }
        return this;
    }

    public final ForeignKeyAction getUpdateAction()
    {
        return updateAction;
    }

    public ForeignKeyMetaData setUpdateAction(ForeignKeyAction updateAction)
    {
        this.updateAction = updateAction;
        return this;
    }

    public void setFkDefinition(String def)
    {
        if (StringUtils.isWhitespace(def))
        {
            return;
        }
        this.fkDefinition = def;
        this.fkDefinitionApplies = true;
        this.updateAction = null;
        this.deleteAction = null;
    }

    public String getFkDefinition()
    {
        return fkDefinition;
    }

    public void setFkDefinitionApplies(boolean flag)
    {
        this.fkDefinitionApplies = flag;
    }
    
    public boolean getFkDefinitionApplies()
    {
        return this.fkDefinitionApplies;
    }
}