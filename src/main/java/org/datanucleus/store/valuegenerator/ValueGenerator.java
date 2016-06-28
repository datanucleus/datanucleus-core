/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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
2003 Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.valuegenerator;

/**
 * Generator interface for values.
 * @param <T> Type of the generated value (Long, String etc)
 */
public interface ValueGenerator<T>
{
    /** Name of any sequence to be used (when using SEQUENCE strategy). */
    public static final String PROPERTY_SEQUENCE_NAME = "sequence-name";

    /** Name of a table to use when using values stored in a table - JPA "table"/JDO "increment" strategy. */
    public static final String PROPERTY_SEQUENCETABLE_TABLE = "sequence-table-name";
    public static final String PROPERTY_SEQUENCETABLE_CATALOG = "sequence-catalog-name";
    public static final String PROPERTY_SEQUENCETABLE_SCHEMA = "sequence-schema-name";
    public static final String PROPERTY_SEQUENCETABLE_NAME_COLUMN = "sequence-name-column-name";
    public static final String PROPERTY_SEQUENCETABLE_NEXTVAL_COLUMN = "sequence-nextval-column-name";

    public static final String PROPERTY_KEY_INITIAL_VALUE = "key-initial-value";
    public static final String PROPERTY_KEY_CACHE_SIZE = "key-cache-size";
    public static final String PROPERTY_KEY_MIN_VALUE = "key-min-value";
    public static final String PROPERTY_KEY_MAX_VALUE = "key-max-value";
    public static final String PROPERTY_KEY_DATABASE_CACHE_SIZE = "key-database-cache-size";

    /** Catalog that the value is for. */
    public static final String PROPERTY_CATALOG_NAME = "catalog-name";
    /** Schema that the value is for. */
    public static final String PROPERTY_SCHEMA_NAME = "schema-name";
    /** Class that the value is for. */
    public static final String PROPERTY_CLASS_NAME = "class-name";
    /** Class that the value is for. */
    public static final String PROPERTY_ROOT_CLASS_NAME = "root-class-name";
    /** Field that the value is for. */
    public static final String PROPERTY_FIELD_NAME = "field-name";
    /** Table that the value is for. */
    public static final String PROPERTY_TABLE_NAME = "table-name";
    /** Column that the value is for (i.e which column will have the value applied to it, so we can check for MAX(col)). */
    public static final String PROPERTY_COLUMN_NAME = "column-name";

    /**
     * Returns the fully qualified name of the <code>Sequence</code>.
     * @return the name of the sequence
     */
    String getName ();

    /**
     * Returns the next sequence value as an Object. If the next
     * sequence value is not available, throw NucleusDataStoreException.
     * @return the next value
     */
    T next();

    /**
     * Provides a hint to the implementation that the application
     * will need <code>additional</code> sequence value objects in
     * short order. There is no externally visible behavior of this
     * method. It is used to potentially improve the efficiency of
     * the algorithm of obtaining additional sequence value objects.
     * @param additional the number of additional values to allocate
     */
    void allocate(int additional);

    /**
     * Returns the current sequence value object if it is available. 
     * It is intended to return a sequence value object previously used. 
     * If the current sequence value is not available, throw NucleusDataStoreException.
     * @return the current value
     */
    T current();

    /**
     * Returns the next sequence value as a long. 
     * If the next sequence value is not available or is not numeric, throw NucleusDataStoreException.
     * @return the next value
     */
    long nextValue();

    /**
     * Returns the current sequence value as a long. 
     * If the current sequence value is not available or is not numeric, throw NucleusDataStoreException.
     * @return the current value
     */
    long currentValue();
}