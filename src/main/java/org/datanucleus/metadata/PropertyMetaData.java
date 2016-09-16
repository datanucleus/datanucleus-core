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

/**
 * The property element declares mapping between a virtual field of an implemented 
 * interface and the corresponding persistent field of a persistence-capable class. 
 * The name attribute is required, and declares the name for the property. The naming 
 * conventions for JavaBeans property names is used: the property name is the same as 
 * the corresponding get method for the property with the get removed and the resulting
 * name lower-cased. 
 * The field-name attribute is required; it associates a persistent field with the named property.
 */
public class PropertyMetaData extends AbstractMemberMetaData
{
    private static final long serialVersionUID = -1281091318359894652L;

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
     * @return This metadata
     */
    public PropertyMetaData setFieldName(String name)
    {
        this.fieldName = name;
        return this;
    }
}