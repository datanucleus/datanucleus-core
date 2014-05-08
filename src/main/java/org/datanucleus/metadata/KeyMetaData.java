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
2004 Andy Jefferson - added unique, indexed
    ...
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * This element specifies the mapping for the key component of maps.
 * The serialised attribute specifies that the key values are to be serialised into the named column.
 */
public class KeyMetaData extends AbstractElementMetaData
{
    /**
     * Constructor to create a copy of the passed metadata using the provided parent.
     * @param kmd The metadata to copy
     */
    public KeyMetaData(KeyMetaData kmd)
    {
        super(kmd);
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public KeyMetaData()
    {
    }

    /**
     * Populate the MetaData.
     * @param clr Class loader to use
     * @param primary the primary ClassLoader to use (or null)
     * @param mmgr MetaData manager
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary, MetaDataManager mmgr)
    {
        AbstractMemberMetaData fmd = (AbstractMemberMetaData)parent;
        if (fmd.getMap() == null)
        {
            // TODO Use InvalidMemberMetaDataException
            throw new NucleusUserException("The field "+fmd.getFullFieldName()+" is defined with <key>, however no <map> definition was found.").setFatal();
        }

        // Make sure key type is set and is valid
        fmd.getMap().key.populate(fmd.getAbstractClassMetaData().getPackageName(), clr, primary, mmgr);
        String keyType = fmd.getMap().getKeyType();
        Class keyTypeClass = null;
        try
        {
            keyTypeClass = clr.classForName(keyType, primary);
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new InvalidMemberMetaDataException(LOCALISER, "044147", fmd.getClassName(), fmd.getName(), keyType);
        }
        if (embeddedMetaData != null &&
            (keyTypeClass.isInterface() || keyTypeClass.getName().equals("java.lang.Object")))
        {
            throw new InvalidMemberMetaDataException(LOCALISER, "044152", fmd.getClassName(), fmd.getName(), 
                keyTypeClass.getName());
        }

        // TODO This will not work currently since MapMetaData is populated *after* KeyMetaData and so the
        // keyClassMetaData is not yet populated. What we should do is provide a postPopulate() method here
        // that MapMetaData can call when it is populated
        if (embeddedMetaData == null &&
            ((AbstractMemberMetaData)parent).hasMap() &&
            ((AbstractMemberMetaData)parent).getMap().isEmbeddedKey() &&
            ((AbstractMemberMetaData)parent).getJoinMetaData() != null &&
            ((AbstractMemberMetaData)parent).getMap().keyIsPersistent())
        {
            // User has specified that the key is embedded in a join table but not how we embed it
            // so add a dummy definition
            embeddedMetaData = new EmbeddedMetaData();
            embeddedMetaData.parent = this;
        }        

        super.populate(clr, primary, mmgr);
    }

    // ------------------------------- Utilities -------------------------------

    /**
     * Returns a string representation of the object using a prefix
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        // Field needs outputting so generate metadata
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<key");
        if (mappedBy != null)
        {
            sb.append(" mapped-by=\"" + mappedBy + "\"");
        }
        if (columnName != null)
        {
            sb.append("\n");
            sb.append(prefix).append("     column=\"" + columnName + "\"");
        }
        sb.append(">\n");

        // Add columns
        if (columns != null)
        {
            for (ColumnMetaData colmd : columns)
            {
                sb.append(colmd.toString(prefix + indent,indent));
            }
        }

        // Add index metadata
        if (indexMetaData != null)
        {
            sb.append(indexMetaData.toString(prefix + indent,indent));
        }

        // Add unique metadata
        if (uniqueMetaData != null)
        {
            sb.append(uniqueMetaData.toString(prefix + indent,indent));
        }

        // Add embedded metadata
        if (embeddedMetaData != null)
        {
            sb.append(embeddedMetaData.toString(prefix + indent,indent));
        }

        // Add foreign-key metadata
        if (foreignKeyMetaData != null)
        {
            sb.append(foreignKeyMetaData.toString(prefix + indent,indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</key>\n");
        return sb.toString();
    }
}