/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.util.StringUtils;

/**
 * Representation of the mapping of (SQL) Query results into a desired output form.
 * The results of a (SQL) query can be mapped into a mixture of
 * <ul>
 * <li>instances of persistent classes - mapping from the result columns to the persistent fields</li>
 * <li>scalar values - names of result columns that are returned as scalars (Integer, String etc)</li>
 * </ul>
 */
public class QueryResultMetaData extends MetaData
{
    /** Name of the query result mapping. */
    protected final String name;

    /** Collection of mappings of persistent types returned from the result set. */
    protected List<PersistentTypeMapping> persistentTypeMappings;

    /** Collection of column names in the result set that are returned as scalars. */
    protected List<String> scalarColumns;

    /**
     * Constructor.
     * @param name The Query name
     */
    public QueryResultMetaData(final String name)
    {
        this.name = name;
    }

    /**
     * Accessor for the name of the result mapping.
     * @return Name of the mapping
     */
    public String getName()
    {
        return name;
    }

    /**
     * Method to add a persistent type as an output for the mapping.
     * @param className Name of the persistent type
     * @param fieldColumnMap Map of column name, keyed by the field name in the persistent type
     * @param discrimColumn Name of any discriminator column
     */
    public void addPersistentTypeMapping(String className, Map fieldColumnMap, String discrimColumn)
    {
        if (persistentTypeMappings == null)
        {
            persistentTypeMappings = new ArrayList();
        }
        PersistentTypeMapping m = new PersistentTypeMapping();
        m.className = className;
        m.discriminatorColumn = (StringUtils.isWhitespace(discrimColumn) ? null : discrimColumn);
        m.fieldColumnMap = fieldColumnMap;
        persistentTypeMappings.add(m);
    }

    /**
     * Method to add a mapping for the specified persistent class.
     * @param className Name of the persistent class
     * @param fieldName Field in the persistent class
     * @param columnName Name of the column in the result set to map to this field
     */
    public void addMappingForPersistentTypeMapping(String className, String fieldName, String columnName)
    {
        PersistentTypeMapping m = null;
        if (persistentTypeMappings == null)
        {
            persistentTypeMappings = new ArrayList();
        }
        else
        {
            Iterator<PersistentTypeMapping> iter = persistentTypeMappings.iterator();
            while (iter.hasNext())
            {
                PersistentTypeMapping mapping = iter.next();
                if (mapping.className.equals(className))
                {
                    m = mapping;
                    break;
                }
            }
        }
        if (m == null)
        {
            m = new PersistentTypeMapping();
            m.className = className;
        }
        if (m.fieldColumnMap == null)
        {
            m.fieldColumnMap = new HashMap();
        }
        // Add the field/column pair
        m.fieldColumnMap.put(fieldName, columnName);
    }

    /**
     * Class to wrap the mapping for a persistent type.
     */
    public static class PersistentTypeMapping
    {
        String className;
        Map fieldColumnMap;
        String discriminatorColumn;
        public String getClassName()
        {
            return className;
        }
        public String getDiscriminatorColumn()
        {
            return discriminatorColumn;
        }
        public String getColumnForField(String fieldName)
        {
            if (fieldColumnMap == null)
            {
                return null;
            }
            return (String)fieldColumnMap.get(fieldName);
        }
    }

    /**
     * Method to register a column as being scalar.
     * @param columnName Name of the column
     */
    public void addScalarColumn(String columnName)
    {
        if (scalarColumns == null)
        {
            scalarColumns = new ArrayList();
        }
        scalarColumns.add(columnName);
    }

    /**
     * Accessor for the persistent type mapping information for this result set.
     * @return Array of persistent types and their mapping info
     */
    public PersistentTypeMapping[] getPersistentTypeMappings()
    {
        if (persistentTypeMappings == null)
        {
            return null;
        }
        return persistentTypeMappings.toArray(new PersistentTypeMapping[persistentTypeMappings.size()]);
    }

    /**
     * Accessor for the names of the result set columns that are returned as scalars.
     * @return Column names whose values are returned as scalars
     */
    public String[] getScalarColumns()
    {
        if (scalarColumns == null)
        {
            return null;
        }
        return scalarColumns.toArray(new String[scalarColumns.size()]);
    }
}