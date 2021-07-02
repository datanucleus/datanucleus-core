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
2004 Andy Jefferson - added toString(), MetaData docs, javadocs.
2004 Andy Jefferson - added multiple column options
2008 Andy Jefferson - add getNextVersion()
    ...
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Three common strategies for versioning instances are supported by standard metadata.
 * <ul>
 * <li>State-comparison involves comparing the values in specific columns to determine if the database row was changed.</li>
 * <li>Timestamp involves comparing the value in a date-time column in the table.
 * The first time in a transaction the row is updated, the timestamp value is updated to the current time.</li>
 * <li>Version-number involves comparing the value in a numeric column in the table.
 * The first time in a transaction the row is updated, the version-number column value is incremented.</li>
 * </ul>
 * <p>
 * There are two forms of version storage.
 * <ul>
 * <li>Surrogate column - the default in JDO, using the column/columns/index info in this class</li>
 * <li>Defined field - the default in JPA. This uses the "fieldName" info only</li>
 * </ul>
 */
public class VersionMetaData extends MetaData
{
    private static final long serialVersionUID = 8277278092349220294L;

    /** strategy for generating the version. */
    protected VersionStrategy versionStrategy;

    /** column name */
    protected String columnName;

    /** Contains the metadata for column. */
    protected ColumnMetaData columnMetaData;

    /** Detail of any indexing of the version column (optional). */
    protected IndexMetaData indexMetaData;

    /** Indexed value. */
    protected IndexedValue indexed = null;

    /** Name of the field that contains the version (if not generating a surrogate column). */
    protected String fieldName = null;

    public VersionMetaData()
    {
    }

    /**
     * Initialisation method. 
     * This should be called AFTER using the populate method if you are going to use populate.
     * It creates the internal convenience arrays etc needed for normal operation.
     */
    public void initialise(ClassLoaderResolver clr)
    {
        if (hasExtension(MetaData.EXTENSION_CLASS_VERSION_FIELD_NAME))
        {
            // User has provided extension "field-name" meaning that we store the version in the column
            // for the specified field (like in JPA)
            String val = getValueForExtension(MetaData.EXTENSION_CLASS_VERSION_FIELD_NAME);
            if (!StringUtils.isWhitespace(val))
            {
                this.fieldName = val;
                this.columnName = null;
            }
        }

        if (fieldName == null)
        {
            // Cater for user specifying column name, or column
            if (columnMetaData == null && columnName != null)
            {
                columnMetaData = new ColumnMetaData();
                columnMetaData.setName(columnName);
                columnMetaData.parent = this;
            }

            // Interpret the "indexed" value to create our IndexMetaData where it wasn't specified that way
            if (indexMetaData == null && (indexed == IndexedValue.UNIQUE || indexed == IndexedValue.TRUE))
            {
                indexMetaData = new IndexMetaData();
                indexMetaData.setUnique(indexed == IndexedValue.UNIQUE);
                if (columnMetaData != null)
                {
                    indexMetaData.addColumn(columnMetaData.getName());
                }
                indexMetaData.parent = this;
            }
        }
        else
        {
            if (getParent() instanceof AbstractClassMetaData)
            {
                AbstractMemberMetaData vermmd = ((AbstractClassMetaData)getParent()).getMetaDataForMember(fieldName);
                if (vermmd != null)
                {
                    if (java.util.Date.class.isAssignableFrom(vermmd.getType()))
                    {
                        // java.util.Date, java.sql.Date, java.sql.Time, java.sql.Timestamp
                        NucleusLogger.METADATA.debug("Setting version-strategy of field " + vermmd.getFullFieldName() + " to DATE_TIME since is Date-based");
                        versionStrategy = VersionStrategy.DATE_TIME;
                    }
                    else if (java.util.Calendar.class.isAssignableFrom(vermmd.getType()))
                    {
                        NucleusLogger.METADATA.debug("Setting version-strategy of field " + vermmd.getFullFieldName() + " to DATE_TIME since is Calendar-based");
                        versionStrategy = VersionStrategy.DATE_TIME;
                    }
                    else if (java.time.Instant.class.isAssignableFrom(vermmd.getType()))
                    {
                        NucleusLogger.METADATA.debug("Setting version-strategy of field " + vermmd.getFullFieldName() + " to DATE_TIME since is Instant-based");
                        versionStrategy = VersionStrategy.DATE_TIME;
                    }
                    // TODO Support other date-time types e.g java.time XXXDateTime
                }
            }
        }
    }

    public final ColumnMetaData getColumnMetaData()
    {
        return columnMetaData;
    }

    /**
     * Mutator for column MetaData.
     * @param columnMetaData The column MetaData to set.
     */
    public void setColumnMetaData(ColumnMetaData columnMetaData)
    {
        this.columnMetaData = columnMetaData;
        this.columnMetaData.parent = this;
    }

    /**
     * Method to create a new ColumnMetaData, add it, and return it.
     * @return The Column metadata
     */
    public ColumnMetaData newColumnMetaData()
    {
        ColumnMetaData colmd = new ColumnMetaData();
        setColumnMetaData(colmd);
        return colmd;
    }

    // TODO Rename to getStrategy for consistency
    public final VersionStrategy getVersionStrategy()
    {
        return versionStrategy;
    }

    public VersionMetaData setStrategy(VersionStrategy strategy)
    {
        this.versionStrategy = strategy;
        return this;
    }

    public VersionMetaData setStrategy(String strategy)
    {
        if (StringUtils.isWhitespace(strategy) || VersionStrategy.getVersionStrategy(strategy) == null)
        {
            throw new RuntimeException(Localiser.msg("044156"));
        }
        this.versionStrategy = VersionStrategy.getVersionStrategy(strategy);
        return this;
    }

    public final IndexMetaData getIndexMetaData()
    {
        return indexMetaData;
    }

    public final void setIndexMetaData(IndexMetaData indexMetaData)
    {
        this.indexMetaData = indexMetaData;
    }

    /**
     * Method to create a new Index metadata, add it, and return it.
     * @return The Index metadata
     */
    public IndexMetaData newIndexMetaData()
    {
        IndexMetaData idxmd = new IndexMetaData();
        setIndexMetaData(idxmd);
        return idxmd;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public VersionMetaData setColumnName(String columnName)
    {
        this.columnName = StringUtils.isWhitespace(columnName) ? null : columnName;
        return this;
    }

    public IndexedValue getIndexed()
    {
        return indexed;
    }

    public VersionMetaData setIndexed(IndexedValue indexed)
    {
        this.indexed = indexed;
        return this;
    }

    public final String getFieldName()
    {
        return fieldName;
    }

    public VersionMetaData setFieldName(String fieldName)
    {
        this.fieldName = fieldName;
        return this;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("VersionMetaData[");
        str.append("strategy=").append(versionStrategy);
        if (fieldName != null)
        {
            str.append(", field=").append(fieldName);
        }
        if (columnMetaData != null)
        {
            str.append(", column=").append(columnMetaData);
        }
        str.append("]");
        return str.toString();
    }
}