/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema.naming;

import java.util.HashMap;
import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IndexMetaData;
import org.datanucleus.util.StringUtils;

/**
 * Abstract base for any naming factory, providing convenience facilities like truncation.
 */
public abstract class AbstractNamingFactory implements NamingFactory
{
    /** Separator to use for words in the identifiers. */
    protected String wordSeparator = "_";

    /** Quote used when the identifier case selected requires it. */
    protected String quoteString = "\"";

    protected NamingCase namingCase = NamingCase.MIXED_CASE;

    protected NucleusContext nucCtx;

    protected ClassLoaderResolver clr;

    /** Map of max name length, keyed by the schema component type */
    Map<SchemaComponent, Integer> maxLengthByComponent = new HashMap<SchemaComponent, Integer>();

    public AbstractNamingFactory(NucleusContext nucCtx)
    {
        this.nucCtx = nucCtx;
        this.clr = nucCtx.getClassLoaderResolver(null);
    }

    public NamingFactory setQuoteString(String quote)
    {
        if (quote != null)
        {
            this.quoteString = quote;
        }
        return this;
    }

    public NamingFactory setWordSeparator(String sep)
    {
        if (sep != null)
        {
            this.wordSeparator = sep;
        }
        return this;
    }

    public NamingFactory setNamingCase(NamingCase nameCase)
    {
        if (nameCase != null)
        {
            this.namingCase = nameCase;
        }
        return this;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#setMaximumLength(org.datanucleus.store.schema.naming.SchemaComponent, int)
     */
    public NamingFactory setMaximumLength(SchemaComponent cmpt, int max)
    {
        maxLengthByComponent.put(cmpt, max);
        return this;
    }

    protected int getMaximumLengthForComponent(SchemaComponent cmpt)
    {
        if (maxLengthByComponent.containsKey(cmpt))
        {
            return maxLengthByComponent.get(cmpt);
        }
        else
        {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getTableName(org.datanucleus.metadata.AbstractClassMetaData)
     */
    public String getTableName(AbstractClassMetaData cmd)
    {
        String name = null;
        if (cmd.getTable() != null)
        {
            name = cmd.getTable();
            // TODO This may have "catalog.schema.name"
        }
        if (name == null)
        {
            name = cmd.getName();
        }

        // Apply any truncation necessary
        int maxLength = getMaximumLengthForComponent(SchemaComponent.TABLE);
        if (name != null && maxLength > 0 && name.length() > maxLength)
        {
            name = truncate(name, maxLength);
        }

        // Apply any case and quoting
        name = getNameInRequiredCase(name);

        return name;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getColumnName(org.datanucleus.metadata.AbstractMemberMetaData, org.datanucleus.store.schema.naming.ColumnType)
     */
    public String getColumnName(AbstractMemberMetaData mmd, ColumnType type)
    {
        return getColumnName(mmd, type, 0);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getIndexName(org.datanucleus.metadata.AbstractClassMetaData, org.datanucleus.metadata.IndexMetaData, int)
     */
    @Override
    public String getIndexName(AbstractClassMetaData cmd, IndexMetaData idxmd, int position)
    {
        if (!StringUtils.isWhitespace(idxmd.getName()))
        {
            String idxName = idxmd.getName();

            int maxLength = getMaximumLengthForComponent(SchemaComponent.CONSTRAINT);
            if (maxLength > 0 && idxName.length() > maxLength)
            {
                idxName = truncate(idxName, maxLength);
            }
            return getNameInRequiredCase(idxName);
        }

        String idxName = cmd.getName() + wordSeparator + position + wordSeparator + "IDX";
        int maxLength = getMaximumLengthForComponent(SchemaComponent.CONSTRAINT);
        if (maxLength > 0 && idxName.length() > maxLength)
        {
            idxName = truncate(idxName, maxLength);
        }
        return getNameInRequiredCase(idxName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.schema.naming.NamingFactory#getIndexName(org.datanucleus.metadata.AbstractMemberMetaData, org.datanucleus.metadata.IndexMetaData)
     */
    @Override
    public String getIndexName(AbstractMemberMetaData mmd, IndexMetaData idxmd)
    {
        if (!StringUtils.isWhitespace(idxmd.getName()))
        {
            String idxName = idxmd.getName();

            int maxLength = getMaximumLengthForComponent(SchemaComponent.CONSTRAINT);
            if (maxLength > 0 && idxName.length() > maxLength)
            {
                idxName = truncate(idxName, maxLength);
            }
            return getNameInRequiredCase(idxName);
        }

        String idxName = mmd.getClassName(false) + wordSeparator + mmd.getName() + wordSeparator + "IDX";
        int maxLength = getMaximumLengthForComponent(SchemaComponent.CONSTRAINT);
        if (maxLength > 0 && idxName.length() > maxLength)
        {
            idxName = truncate(idxName, maxLength);
        }
        return getNameInRequiredCase(idxName);
    }

    /** The number of characters used to build the hash. */
    private static final int TRUNCATE_HASH_LENGTH = 4;

    /**
     * Range to use for creating hashed ending when truncating identifiers. The actual hashes have a value
     * between 0 and <code>HASH_RANGE</code> - 1.
     */
    private static final int TRUNCATE_HASH_RANGE = calculateHashMax();
    private static final int calculateHashMax()
    {
        int hm = 1;
        for (int i = 0; i < TRUNCATE_HASH_LENGTH; ++i)
        {
            hm *= Character.MAX_RADIX;
        }

        return hm;
    }

    /**
     * Method to truncate a name to fit within the specified name length.
     * If truncation is necessary will use a 4 char hashcode (defined by {@link #TRUNCATE_HASH_LENGTH}) (at the end) to attempt 
     * to create uniqueness.
     * @param name The name
     * @param length The (max) length to use
     * @return The truncated name.
     */
    protected static String truncate(String name, int length)
    {
        if (length == 0) // Special case of no truncation
        {
            return name;
        }
        if (name.length() > length)
        {
            if (length < TRUNCATE_HASH_LENGTH)
                throw new IllegalArgumentException("The length argument (=" + length + ") is less than HASH_LENGTH(=" + TRUNCATE_HASH_LENGTH + ")!");

            // Truncation is necessary so cut down to "maxlength-HASH_LENGTH" and add HASH_LENGTH chars hashcode
            int tailIndex = length - TRUNCATE_HASH_LENGTH;
            int tailHash = name.hashCode();

            // We have to scale down the hash anyway, so we can simply ignore the sign
            if (tailHash < 0)
                tailHash *= -1;

            // Scale the hash code down to the range 0 ... (HASH_RANGE - 1)
            tailHash %= TRUNCATE_HASH_RANGE;

            String suffix = Integer.toString(tailHash, Character.MAX_RADIX);
            if (suffix.length() > TRUNCATE_HASH_LENGTH)
                throw new IllegalStateException("Calculated hash \"" + suffix + "\" has more characters than defined by HASH_LENGTH (=" + TRUNCATE_HASH_LENGTH + ")! This should never happen!");

            // we add prefix "0", if it's necessary
            if (suffix.length() < TRUNCATE_HASH_LENGTH)
            {
                StringBuilder sb = new StringBuilder(TRUNCATE_HASH_LENGTH);
                sb.append(suffix);
                while (sb.length() < TRUNCATE_HASH_LENGTH)
                    sb.insert(0, '0');

                suffix = sb.toString();
            }

            return name.substring(0, tailIndex) + suffix;
        }
        else
        {
            return name;
        }
    }

    /**
     * Convenience method to convert the passed name into a name in the required "case".
     * Also adds on any required quoting.
     * @param name The name
     * @return The updated name in the correct case
     */
    protected String getNameInRequiredCase(String name)
    {
        if (name == null)
        {
            return null;
        }

        StringBuilder id = new StringBuilder();
        if (namingCase == NamingCase.LOWER_CASE_QUOTED ||
            namingCase == NamingCase.MIXED_CASE_QUOTED ||
            namingCase == NamingCase.UPPER_CASE_QUOTED)
        {
            if (!name.startsWith(quoteString))
            {
                id.append(quoteString);
            }
        }

        if (namingCase == NamingCase.LOWER_CASE ||
            namingCase == NamingCase.LOWER_CASE_QUOTED)
        {
            id.append(name.toLowerCase());
        }
        else if (namingCase == NamingCase.UPPER_CASE ||
            namingCase == NamingCase.UPPER_CASE_QUOTED)
        {
            id.append(name.toUpperCase());
        }
        else
        {
            id.append(name);
        }

        if (namingCase == NamingCase.LOWER_CASE_QUOTED ||
            namingCase == NamingCase.MIXED_CASE_QUOTED ||
            namingCase == NamingCase.UPPER_CASE_QUOTED)
        {
            if (!name.endsWith(quoteString))
            {
                id.append(quoteString);
            }
        }
        return id.toString();
    }


    protected String prepareColumnNameForUse(String name)
    {
        String preparedName = name;

        // Apply any truncation necessary
        int maxLength = getMaximumLengthForComponent(SchemaComponent.COLUMN);
        if (preparedName != null && maxLength > 0 && preparedName.length() > maxLength)
        {
            preparedName = truncate(preparedName, maxLength);
        }

        // Apply any case and quoting
        return getNameInRequiredCase(preparedName);
    }
}