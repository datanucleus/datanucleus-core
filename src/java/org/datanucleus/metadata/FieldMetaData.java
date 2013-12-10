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
 * Representation of the Meta-Data for a field of a class.
 *
 * <H3>Lifecycle state</H3>
 * An object of this type has 2 lifecycle states. The first is the raw
 * constructed object which represents pure MetaData (maybe from a MetaData
 * file). The second is a "populated" object which represents MetaData for a
 * Field of a class with the metadata aligned to be appropriate for that Field. 
 *
 * <H3>Containers</H3>
 * Each field can represent a container. The container can be an array, a
 * Collection or a Map. The field type must be of the correct type to represent
 * these.
 *
 * <H3>Field Management</H3>
 * Each field can be managed by us or not. The class provides a method for
 * identifying if a field is managed by us (<I>isJdoField()</I>). If a field
 * is managed by us, it will have a field "id" (within its class). In a class
 * the field "id" will start at 0 (for the first field, in alphabetical order).
 */
public class FieldMetaData extends AbstractMemberMetaData
{
    /**
     * Convenience constructor to copy the specification from the passed field.
     * This is used when we have an overriding field and we make a copy of the baseline
     * field as a starting point.
     * @param parent The parent
     * @param fmd The field to copy
     */
    public FieldMetaData(MetaData parent, AbstractMemberMetaData fmd)
    {
        super(parent, fmd);
    }

    /**
     * Constructor. Saves the MetaData with the specified values. The object is
     * then in an "unpopulated" state. It can become "populated" by calling the
     * <B>populate()</B> method which compares it against the field it is to
     * represent and updates any unset attributes and flags up any errors.
     * @param parent parent MetaData instance
     * @param name field name 
     */
    public FieldMetaData(MetaData parent, final String name)
    {
        super(parent, name);
    }

    /**
     * Returns a string representation of the object using a prefix
     * This can be used as part of a facility to output a MetaData file. 
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        // If this field is static or final, don't bother with MetaData since we will ignore it anyway.
        if (isStatic() || isFinal())
        {
            return "";
        }

        // Field needs outputting so generate metadata
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<field name=\"" + name + "\"");
        if (persistenceModifier != null && !StringUtils.isWhitespace(persistenceModifier.toString()))
        {
            sb.append("\n").append(prefix).append("       persistence-modifier=\"" + persistenceModifier + "\"");
        }
        if (!StringUtils.isWhitespace(table))
        {
            sb.append("\n").append(prefix).append("       table=\"" + table + "\"");
        }
        if (primaryKey != null && primaryKey.booleanValue())
        {
            sb.append("\n").append(prefix).append("       primary-key=\"" + primaryKey + "\"");
        }
        sb.append("\n").append(prefix).append("       null-value=\"" + nullValue + "\"");
        if (defaultFetchGroup != null && !StringUtils.isWhitespace(defaultFetchGroup.toString()))
        {
            sb.append("\n").append(prefix).append("       default-fetch-group=\"" + defaultFetchGroup + "\"");
        }
        if (embedded != null && !StringUtils.isWhitespace(embedded.toString()))
        {
            sb.append("\n").append(prefix).append("       embedded=\"" + embedded + "\"");
        }
        if (serialized != null && !StringUtils.isWhitespace(serialized.toString()))
        {
            sb.append("\n").append(prefix).append("       serialized=\"" + serialized + "\"");
        }
        if (dependent != null)
        {
            sb.append("\n").append(prefix).append("       dependent=\"" + dependent + "\"");
        }
        if (mappedBy != null)
        {
            sb.append("\n").append(prefix).append("       mapped-by=\"" + mappedBy + "\"");
        }
        if (fieldTypes != null)
        {
            sb.append("\n").append(prefix).append("       field-type=\"");
            for (int i=0;i<fieldTypes.length;i++)
            {
                sb.append(fieldTypes[i]);
            }
            sb.append("\"");
        }
        if (!StringUtils.isWhitespace(loadFetchGroup))
        {
            sb.append("\n").append(prefix).append("       load-fetch-group=\"" + loadFetchGroup + "\"");
        }
        if (recursionDepth != DEFAULT_RECURSION_DEPTH && recursionDepth != UNDEFINED_RECURSION_DEPTH)
        {
            sb.append("\n").append(prefix).append("       recursion-depth=\"" + recursionDepth + "\"");
        }
        if (valueStrategy != null)
        {
            sb.append("\n").append(prefix).append("       value-strategy=\"" + valueStrategy + "\"");
        }
        if (sequence != null)
        {
            sb.append("\n").append(prefix).append("       sequence=\"" + sequence + "\"");
        }
        if (indexMetaData == null && indexed != null)
        {
            sb.append("\n").append(prefix).append("       indexed=\"" + indexed.toString() + "\"");
        }
        if (uniqueMetaData == null)
        {
            sb.append("\n").append(prefix).append("       unique=\"" + uniqueConstraint + "\"");
        }
        if (columnMetaData == null && column != null)
        {
            sb.append("\n").append(prefix).append("       column=\"" + column + "\"");
        }
        sb.append(">\n");

        // Add field containers
        if (containerMetaData != null)
        {
            if (containerMetaData instanceof CollectionMetaData)
            {
                CollectionMetaData c = (CollectionMetaData)containerMetaData;
                sb.append(c.toString(prefix + indent,indent));
            }
            else if (containerMetaData instanceof ArrayMetaData)
            {
                ArrayMetaData c = (ArrayMetaData)containerMetaData;
                sb.append(c.toString(prefix + indent,indent));
            }
            else if (containerMetaData instanceof MapMetaData)
            {
                MapMetaData c = (MapMetaData)containerMetaData;
                sb.append(c.toString(prefix + indent,indent));
            }
        }

        // Add columns
        if (columnMetaData != null)
        {
            for (int i=0; i<columnMetaData.length; i++)
            {
                sb.append(columnMetaData[i].toString(prefix + indent,indent));
            }
        }

        // Add join
        if (joinMetaData != null)
        {
            sb.append(joinMetaData.toString(prefix + indent,indent));
        }

        // Add element
        if (elementMetaData != null)
        {
            sb.append(elementMetaData.toString(prefix + indent,indent));
        }

        // Add key
        if (keyMetaData != null)
        {
            sb.append(keyMetaData.toString(prefix + indent,indent));
        }

        // Add value
        if (valueMetaData != null)
        {
            sb.append(valueMetaData.toString(prefix + indent,indent));
        }

        // TODO Add fetch-groups

        // Add order
        if (orderMetaData != null)
        {
            sb.append(orderMetaData.toString(prefix + indent,indent));
        }

        // Add embedded
        if (embeddedMetaData != null)
        {
            sb.append(embeddedMetaData.toString(prefix + indent,indent));
        }

        // Add index
        if (indexMetaData != null)
        {
            sb.append(indexMetaData.toString(prefix + indent,indent));
        }

        // Add unique
        if (uniqueMetaData != null)
        {
            sb.append(uniqueMetaData.toString(prefix + indent,indent));
        }

        // Add foreign-key
        if (foreignKeyMetaData != null)
        {
            sb.append(foreignKeyMetaData.toString(prefix + indent,indent));
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</field>\n");
        return sb.toString();
    }
}