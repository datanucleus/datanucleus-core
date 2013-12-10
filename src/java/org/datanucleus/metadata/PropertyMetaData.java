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
 * The property element declares mapping between a virtual field of an implemented 
 * interface and the corresponding persistent field of a persistence-capable class. 
 * The name attribute is required, and declares the name for the property. The naming 
 * conventions for JavaBeans property names is used: the property name is the same as 
 * the corresponding get method for the property with the get removed and the resulting
 * name lower-cased. 
 * The field-name attribute is required; it associates a persistent field with the named property.
 */
public class PropertyMetaData extends AbstractMemberMetaData implements Comparable, ColumnMetaDataContainer
{
    /** Name of the field that this property is wrapping (when part of a persistent class). */
    protected String fieldName;

    /**
     * Convenience constructor to copy the specification from the passed field.
     * This is used when we have an overriding field and we make a copy of the baseline
     * field as a starting point.
     * @param parent The parent
     * @param fmd The field to copy
     */
    public PropertyMetaData(MetaData parent, PropertyMetaData fmd)
    {
        super(parent, fmd);
        this.fieldName = fmd.fieldName;
    }

    /**
     * Constructor. Saves the MetaData with the specified values. The object is
     * then in an "unpopulated" state. It can become "populated" by calling the
     * <B>populate()</B> method which compares it against the field it is to
     * represent and updates any unset attributes and flags up any errors.
     * @param parent parent MetaData instance
     * @param name field/property name 
     */
    public PropertyMetaData(MetaData parent, final String name)    
    {
        super(parent, name);
    }

    /**
     * Accessor for the field name 
     * if a concrete implementation of the interface is generated the field name for this property.
     * @return field name. null if no field name is set, or if this is a property in a concrete class.
     */
    public String getFieldName()
    {
        return fieldName;
    }

    /**
     * Method to set the field name that this property wraps (persistent interface implementation)
     * @param name Field name
     */
    public PropertyMetaData setFieldName(String name)
    {
        this.fieldName = name;
        return this;
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
        // If this field is static, don't bother with MetaData since JDO will ignore it anway.
        if (isStatic())
        {
            return "";
        }

        // Field needs outputting so generate metadata
        StringBuffer sb = new StringBuffer();
        sb.append(prefix).append("<property name=\"" + name + "\"");
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
        if (fieldName != null)
        {
            sb.append("\n").append(prefix).append("       field-name=\"" + fieldName + "\"");
        }
        if (table != null)
        {
            sb.append("\n").append(prefix).append("       table=\"" + table + "\"");
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

        sb.append(prefix).append("</property>\n");
        return sb.toString();
    }
}