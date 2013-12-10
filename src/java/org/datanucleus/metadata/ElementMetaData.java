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

/**
 * This element specifies the mapping for the element component of arrays and collections.
 * If only one column is mapped, and no additional information is needed for the column, 
 * then the column attribute can be used. Otherwise, the column element(s) are used.
 * The serialised attribute specifies that the key values are to be serialised into the named column.
 * The foreign-key attribute specifies the name of a foreign key to be generated.
 */
public class ElementMetaData extends AbstractElementMetaData
{
    /**
     * Constructor to create a copy of the passed metadata using the provided parent.
     * @param emd The metadata to copy
     */
    public ElementMetaData(ElementMetaData emd)
    {
        super(emd);
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public ElementMetaData()
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
        // Make sure element type is set and is valid
        AbstractMemberMetaData fmd = (AbstractMemberMetaData)parent;
        if (fmd.hasCollection())
        {
            fmd.getCollection().element.populate(fmd.getAbstractClassMetaData().getPackageName(), clr, primary, mmgr);
        }
        else if (fmd.hasArray())
        {
            fmd.getArray().element.populate(fmd.getAbstractClassMetaData().getPackageName(), clr, primary, mmgr);
        }
        if (embeddedMetaData == null && 
            ((AbstractMemberMetaData)parent).hasCollection() && 
            ((AbstractMemberMetaData)parent).getCollection().isEmbeddedElement() &&
            ((AbstractMemberMetaData)parent).getJoinMetaData() != null &&
            ((AbstractMemberMetaData)parent).getCollection().elementIsPersistent())
        {
            // User has specified that the element is embedded in a join table but not how we embed it
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
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<element");
        if (mappedBy != null)
        {
            sb.append(" mapped-by=\"" + mappedBy + "\"");
        }
        if (columnName != null)
        {
            sb.append("\n");
            sb.append(prefix).append("          column=\"" + columnName + "\"");
        }
        sb.append(">\n");

        // Add columns
        for (int i=0;i<columns.size();i++)
        {
            ColumnMetaData colmd = columns.get(i);
            sb.append(colmd.toString(prefix + indent,indent));
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

        sb.append(prefix).append("</element>\n");
        return sb.toString();
    }
}