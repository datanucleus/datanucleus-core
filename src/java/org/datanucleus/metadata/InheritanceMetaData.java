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
     */
    public void initialise(ClassLoaderResolver clr, MetaDataManager mmgr)
    {
        if (joinMetaData != null)
        {
            joinMetaData.initialise(clr, mmgr);
        }
        if (discriminatorMetaData != null)
        {
            discriminatorMetaData.initialise(clr, mmgr);
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

    // ----------------------------- Utilities ---------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<inheritance strategy=\"" + strategy + "\">\n");

        // Add join
        if (joinMetaData != null)
        {
            sb.append(joinMetaData.toString(prefix + indent,indent));
        }

        // Add discriminator
        if (discriminatorMetaData != null)
        {
            sb.append(discriminatorMetaData.toString(prefix + indent,indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</inheritance>\n");

        return sb.toString();
    }
}