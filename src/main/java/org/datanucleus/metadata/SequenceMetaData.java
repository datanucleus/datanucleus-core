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
 * Representation of the MetaData of a named Sequence (JDO, or JPA).
 */
public class SequenceMetaData extends MetaData
{
    private static final long serialVersionUID = 3146160559285680230L;

    /** Name under which this sequence generator is known. */
    protected String name;

    /** Datastore Sequence name */
    protected String datastoreSequence;

    /** factory class name (JDO). */
    protected String factoryClass;

    /** Strategy for this sequence (JDO). */
    protected SequenceStrategy strategy;

    /** Initial value of the sequence. */
    protected int initialValue = -1;

    /** Allocation size for the sequence. */
    protected int allocationSize = -1;

    protected String schemaName;

    protected String catalogName;

    /**
     * Constructor.
     * @param name The sequence name
     * @param strategyValue The strategy value
     */
    public SequenceMetaData(final String name, final String strategyValue)
    {
        this.name = name;
        this.strategy = SequenceStrategy.getStrategy(strategyValue);
    }

    /**
     * Convenience accessor for the fully-qualified name of the sequence.
     * @return Fully-qualified name of the sequence (including the package name).
     */
    public String getFullyQualifiedName()
    {
        PackageMetaData pmd = (PackageMetaData)getParent();
        return pmd.getName() + "." + name;
    }

    public String getName()
    {
        return name;
    }

    public SequenceMetaData setName(String name)
    {
        this.name = StringUtils.isWhitespace(name) ? this.name : name;
        return this;
    }

    public String getCatalogName()
    {
        return catalogName;
    }
    public SequenceMetaData setCatalogName(String name)
    {
        this.catalogName = StringUtils.isWhitespace(name) ? this.catalogName : name;
        return this;
    }

    public String getSchemaName()
    {
        return schemaName;
    }
    public SequenceMetaData setSchemaName(String name)
    {
        this.schemaName = StringUtils.isWhitespace(name) ? this.schemaName : name;
        return this;
    }

    public SequenceStrategy getStrategy()
    {
        return strategy;
    }

    public SequenceMetaData setStrategy(SequenceStrategy strategy)
    {
        this.strategy = strategy;
        return this;
    }

    public String getDatastoreSequence()
    {
        return datastoreSequence;
    }

    public SequenceMetaData setDatastoreSequence(String datastoreSequence)
    {
        this.datastoreSequence = StringUtils.isWhitespace(datastoreSequence) ? null : datastoreSequence;
        return this;
    }

    public String getFactoryClass()
    {
        return factoryClass;
    }

    public SequenceMetaData setFactoryClass(String factoryClass)
    {
        this.factoryClass = StringUtils.isWhitespace(factoryClass) ? null : factoryClass;
        return this;
    }

    public int getInitialValue()
    {
        return initialValue;
    }

    public SequenceMetaData setInitialValue(int initialValue)
    {
        this.initialValue = initialValue;
        return this;
    }

    public SequenceMetaData setInitialValue(String initialValue)
    {
        if (!StringUtils.isWhitespace(initialValue))
        {
            try
            {
                this.initialValue = Integer.parseInt(initialValue);
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }

    public int getAllocationSize()
    {
        return allocationSize;
    }

    public SequenceMetaData setAllocationSize(int allocationSize)
    {
        this.allocationSize = allocationSize;
        return this;
    }

    public SequenceMetaData setAllocationSize(String allocationSize)
    {
        if (!StringUtils.isWhitespace(allocationSize))
        {
            try
            {
                this.allocationSize = Integer.parseInt(allocationSize);
            }
            catch (NumberFormatException nfe)
            {
            }
        }
        return this;
    }
}