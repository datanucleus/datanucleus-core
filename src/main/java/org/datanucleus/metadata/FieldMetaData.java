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
    private static final long serialVersionUID = 2280126411219542L;

    /**
     * Convenience constructor to copy the specification from the passed field.
     * This is used when we have an overriding field and we make a copy of the baseline field as a starting point.
     * @param parent The parent
     * @param fmd The field to copy
     */
    public FieldMetaData(MetaData parent, AbstractMemberMetaData fmd)
    {
        super(parent, fmd);
    }

    /**
     * Constructor. Saves the MetaData with the specified values.
     * The object is then in an "unpopulated" state. It can become "populated" by calling the <B>populate()</B> method which 
     * compares it against the field it is to represent and updates any unset attributes and flags up any errors.
     * @param parent parent MetaData instance
     * @param name field name 
     */
    public FieldMetaData(MetaData parent, final String name)
    {
        super(parent, name);
    }

    public String toString()
    {
        StringBuilder str = new StringBuilder(super.toString()).append(" [").append(this.getFullFieldName()).append("]");
        str.append(" type=").append(getTypeName());
        if (primaryKey == Boolean.TRUE)
        {
            str.append(", primary-key");
        }
        if (embedded == Boolean.TRUE)
        {
            str.append(", embedded");
        }
        if (serialized == Boolean.TRUE)
        {
            str.append(", serialised");
        }
        str.append(", persistence-modifier=").append(persistenceModifier.toString());
        if (valueStrategy != null)
        {
            str.append(", valueStrategy=").append(valueStrategy.toString());
        }
        if (parent instanceof AbstractClassMetaData)
        {
            AbstractClassMetaData parentAsCmd = (AbstractClassMetaData)parent;
            if (!parentAsCmd.getFullClassName().equals(getClassName()))
            {
                str.append(" OVERRIDE");
            }
        }
        return str.toString();
    }
}