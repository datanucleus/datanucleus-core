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
2004 Andy Jefferson - added initialise() method
    ...
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.ClassLoaderResolver;

/**
 * Representation of the Meta-Data defining inherited classes.
 */
public class InheritanceMetaData extends MetaData
{
    private static final long serialVersionUID = -3645685751605920718L;

    public static final String INHERITANCE_TREE_STRATEGY_JOINED = "JOINED";
    public static final String INHERITANCE_TREE_STRATEGY_TABLE_PER_CLASS = "TABLE_PER_CLASS";
    public static final String INHERITANCE_TREE_STRATEGY_SINGLE_TABLE = "SINGLE_TABLE";

    /** strategy tag value. */
    protected InheritanceStrategy strategy = null;

    /** JoinMetaData element. */
    protected JoinMetaData joinMetaData;

    /** DiscriminatorMetaData element. */
    protected DiscriminatorMetaData discriminatorMetaData;

    /** Strategy to apply for the whole inheritance tree. Optional, used by JPA. */
    protected String strategyForTree = null;

    /**
     * Default constructor. Set any fields using setters, before populate().
     */
    public InheritanceMetaData()
    {
    }

    /**
     * Method to initialise the object, creating internal convenience arrays.
     * Initialises all sub-objects.
     * @param clr Not used
     */
    public void initialise(ClassLoaderResolver clr)
    {
        if (joinMetaData != null)
        {
            joinMetaData.initialise(clr);
        }
        if (discriminatorMetaData != null)
        {
            discriminatorMetaData.initialise(clr);
        }

        setInitialised();
    }

    public InheritanceMetaData setStrategyForTree(String strategy)
    {
        this.strategyForTree = strategy;
        return this;
    }

    public String getStrategyForTree()
    {
        return strategyForTree;
    }

    public InheritanceStrategy getStrategy()
    {
        return strategy;
    }

    public InheritanceMetaData setStrategy(InheritanceStrategy strategy)
    {
        this.strategy = strategy;
        return this;
    }

    public InheritanceMetaData setStrategy(String strategy)
    {
        this.strategy = InheritanceStrategy.getInheritanceStrategy(strategy);
        return this;
    }

    public JoinMetaData getJoinMetaData()
    {
        return joinMetaData;
    }

    public void setJoinMetaData(JoinMetaData joinMetaData)
    {
        this.joinMetaData = joinMetaData;
        if (this.joinMetaData != null)
        {
            this.joinMetaData.parent = this;
        }
    }

    /**
     * Method to create a new JoinMetaData, set it, and return it.
     * @return The join metadata
     */
    public JoinMetaData newJoinMetadata()
    {
        JoinMetaData joinmd = new JoinMetaData();
        setJoinMetaData(joinmd);
        return joinmd;
    }

    public DiscriminatorMetaData getDiscriminatorMetaData()
    {
        return discriminatorMetaData;
    }

    public void setDiscriminatorMetaData(DiscriminatorMetaData discriminatorMetaData)
    {
        this.discriminatorMetaData = discriminatorMetaData;
        this.discriminatorMetaData.parent = this;
    }

    /**
     * Method to create a new discriminator metadata, assign it to this inheritance, and return it.
     * @return The discriminator metadata
     */
    public DiscriminatorMetaData newDiscriminatorMetadata()
    {
        DiscriminatorMetaData dismd = new DiscriminatorMetaData();
        setDiscriminatorMetaData(dismd);
        return dismd;
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder("InheritanceMetaData[");
        str.append("strategy=").append(strategy);
        if (strategyForTree != null)
        {
            str.append(", strategy-for-tree=").append(strategyForTree);
        }
        if (discriminatorMetaData != null)
        {
            str.append(", discriminator=").append(discriminatorMetaData);
        }
        if (joinMetaData != null)
        {
            str.append(", join=").append(joinMetaData);
        }
        str.append("]");
        return str.toString();
    }
}