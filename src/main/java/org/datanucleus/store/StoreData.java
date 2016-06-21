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
import org.datanucleus.store.schema.table.Table;
import org.datanucleus.util.Localiser;

/**
 * Basic store information about an object that is stored in a datastore.
 * Can be a class or member.
 */
public class StoreData
{
    public enum Type
    {
        FCO,
        SCO
    }

    /** Name of the class/field. */
    protected final String name;

    /** Type of data being stored (FCO, SCO). */
    protected final Type type;

    /** Metadata for the class, or member (join table) depending on what this represents. */
    protected MetaData metadata;

    /** Name of the persistent interface, when this represents one. Otherwise null. */
    protected String interfaceName;

    protected Table table;

    /** Extension props. Available for store manager to save additional info if required. */
    protected Map properties = new HashMap();

    /**
     * Constructor.
     * @param name Fully-qualified ame of the class/member.
     * @param type Type of data (FCO/SCO)
     */
    public StoreData(String name, Type type)
    {
        this(name, null, type, null);
    }

    /**
     * Constructor.
     * @param name Fully-qualified name of the class/member.
     * @param metadata MetaData for the class or field (if available)
     * @param type Type of data (FCO/SCO)
     * @param interfaceName Name of persistent-interface being implemented
     */
    public StoreData(String name, MetaData metadata, Type type, String interfaceName)
    {
        this.name = name;
        this.type = type;
        this.metadata = metadata;
        this.interfaceName = interfaceName;
    }

    /**
     * Accessor for fully-qualified class/member name.
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
        return metadata;
    }

    /**
     * Method to set the MetaData for this class.
     * @param md MetaData
     */
    public void setMetaData(MetaData md)
    {
        this.metadata = md;
    }

    /**
     * Accessor for whether this represents FCO data.
     * @return Whether it is FCO
     */
    public boolean isFCO()
    {
        return type == Type.FCO;
    }

    /**
     * Accessor for whether this represents SCO data.
     * @return Whether it is SCO.
     */
    public boolean isSCO()
    {
        return type == Type.SCO;
    }

    /**
     * Accessor for type.
     * @return Returns the type.
     */
    public Type getType()
    {
        return type;
    }

    /**
     * Accessor for the persistent interface name
     * @return Returns the persistent interface name
     */
    public String getInterfaceName()
    {
        return interfaceName;
    }

    public void setTable(Table tbl)
    {
        this.table = tbl;
    }

    /**
     * Accessor for the generic Table for this class/member (if the store plugin supports generic Tables).
     * @return The table associated with this class/member
     */
    public Table getTable()
    {
        return table;
    }

    public void addProperty(String key, Object value)
    {
        properties.put(key, value);
    }

    public Object getProperty(String key)
    {
        return properties.get(key);
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
            return Localiser.msg("035004", name, "(none)", cmd.getInheritanceMetaData().getStrategy().toString());
        }
        else if (metadata instanceof AbstractMemberMetaData)
        {
            return Localiser.msg("035003", name, null);
        }
        else
        {
            // TODO What is this situation?
            return Localiser.msg("035002", name, null);
        }
    }
}