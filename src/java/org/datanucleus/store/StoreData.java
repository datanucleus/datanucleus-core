/**********************************************************************
 Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store;

import java.util.HashMap;
import java.util.Map;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.util.Localiser;

/**
 * Basic store information about an object that is stored in a datastore.
 * Can be a class or field.
 */
public class StoreData
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** First class object (FCO) type */
    public static final int FCO_TYPE = 1;

    /** Second class object (SCO) type */
    public static final int SCO_TYPE = 2;

    /** Name of the class/field. */
    protected final String name;

    /** Type of data being stored (FCO, SCO). */
    protected final int type;

    /** Extension props. Available for store manager to save additional info if required. */
    protected Map properties = new HashMap();

    /**
     * Constructor.
     * @param name Name of the class/field
     * @param type Type of data (FCO/SCO)
     */
    public StoreData(String name, int type)
    {
        this(name, null, type, null);
    }

    /**
     * Constructor.
     * @param name Name of the class/field
     * @param metadata MetaData for the class or field (if available)
     * @param type Type of data (FCO/SCO)
     * @param interfaceName Name of persistent-interface being implemented
     */
    public StoreData(String name, MetaData metadata, int type, String interfaceName)
    {
        this.name = name;
        this.type = type;
        if (metadata != null)
        {
            addProperty("metadata", metadata);
        }
        if (interfaceName != null)
        {
            addProperty("interface-name", interfaceName);
        }
    }

    /**
     * Accessor for class/field name.
     * @return Returns the class/field name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor for class/field meta data.
     * @return Returns the class/field meta data.
     */
    public MetaData getMetaData()
    {
        return (MetaData)properties.get("metadata");
    }

    /**
     * Method to set the MetaData for this class.
     * @param md MetaData
     */
    public void setMetaData(MetaData md)
    {
        addProperty("metadata", md);
    }

    /**
     * Accessor for whether this represents FCO data.
     * @return Whether it is FCO
     */
    public boolean isFCO()
    {
        return type == FCO_TYPE;
    }

    /**
     * Accessor for whether this represents SCO data.
     * @return Whether it is SCO.
     */
    public boolean isSCO()
    {
        return type == SCO_TYPE;
    }

    /**
     * Accessor for type.
     * @return Returns the type.
     */
    public int getType()
    {
        return type;
    }

    /**
     * Accessor for the persistent interface name
     * @return Returns the persistent interface name
     */
    public String getInterfaceName()
    {
        return (String)properties.get("interface-name");
    }

    public void addProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    /**
     * Accessor for extension props, if utilised by the store manager.
     * @return Extension props
     */
    public Map getProperties()
    {
        return properties;
    }

    /**
     * Method to return this class/field managed object as a string.
     * @return String version of this class/field managed object.
     */
    public String toString()
    {
        MetaData metadata = getMetaData();
        if (metadata instanceof ClassMetaData)
        {
            ClassMetaData cmd = (ClassMetaData)metadata;
            return LOCALISER.msg("035004", name, "(none)",
                cmd.getInheritanceMetaData().getStrategy().toString());
        }
        else if (metadata instanceof AbstractMemberMetaData)
        {
            return LOCALISER.msg("035003", name, null);
        }
        else
        {
            return LOCALISER.msg("035002", name, null);
        }
    }
}