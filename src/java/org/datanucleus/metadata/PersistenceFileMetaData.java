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
package org.datanucleus.metadata;

import java.util.HashSet;
import java.util.Iterator;

/**
 * Representation of a Meta-Data "persistence.xml" file. 
 * Contains a set of "persistence-unit" meta-data.
 */
public class PersistenceFileMetaData extends MetaData
{
    /** Filename of the "persistence.xml" */
    protected String filename = null;

    /** Persistence units defined in this file. */
    protected HashSet<PersistenceUnitMetaData> persistenceUnits = new HashSet();

    /**
     * Constructor.
     * @param filename The file where this is stored (or null).
     */
    public PersistenceFileMetaData(String filename)
    {
        this.filename = filename;
    }

    /**
     * Accessor for the filename
     * @return The filename of this MetaData file.
     */
    public String getFilename()
    {
        return filename;
    }

    /**
     * Accessor for the number of persistence units.
     * @return no of persistence units.
     */
    public int getNoOfPersistenceUnits()
    {
        return persistenceUnits.size();
    }

    /**
     * Accessor for the Meta-Data of a persistence unit with a given name.
     * @param name Name of the persistence unit
     * @return Meta-Data for the persistence unit
     */
    public PersistenceUnitMetaData getPersistenceUnit(String name)
    {
        Iterator<PersistenceUnitMetaData> iter = persistenceUnits.iterator();
        while (iter.hasNext())
        {
            PersistenceUnitMetaData p = iter.next();
            if (p.name.equals(name))
            {
                return p;
            }
        }
        return null;
    }

    /**
     * Accessor for the persistence units in this "persistence.xml" file.
     * @return The persistence units
     */
    public PersistenceUnitMetaData[] getPersistenceUnits()
    {
        if (persistenceUnits.size() == 0)
        {
            return null;
        }

        return persistenceUnits.toArray(new PersistenceUnitMetaData[persistenceUnits.size()]);
    }

    /**
     * Mutator for the filename for this MetaData file.
     * @param filename The filename of this MetaData file.
     */
    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    /**
     * Method to add a persistence unit
     * @param pumd The PersistenceUnitMetaData to add.
     **/
    public void addPersistenceUnit(PersistenceUnitMetaData pumd)
    {
        if (pumd == null)
        {
            return;
        }

        pumd.parent = this;
        Iterator<PersistenceUnitMetaData> iter = persistenceUnits.iterator();
        while (iter.hasNext())
        {
            PersistenceUnitMetaData p = iter.next();
            // Check if already exists
            if (pumd.getName().equals(p.getName()))
            {
                return;
            }
        }
        persistenceUnits.add(pumd);
    }

    // -------------------------------- Utilities ------------------------------

    /**
     * Returns a string representation of the object.
     * @param indent The indent
     * @return a string representation of the object.
     */
    public String toString(String indent)
    {
        if (indent == null)
        {
            indent = "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append("<persistence>\n");

        // Add persistence units
        Iterator<PersistenceUnitMetaData> iter = persistenceUnits.iterator();
        while (iter.hasNext())
        {
            sb.append(iter.next().toString(indent, indent));
        }

        sb.append("</persistence>");
        return sb.toString();
    }
}