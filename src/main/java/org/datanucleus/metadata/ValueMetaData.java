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
import org.datanucleus.exceptions.NucleusFatalUserException;

/**
 * This element specifies the mapping for the value component of maps.
 * The serialised attribute specifies that the key values are to be serialised into the named column.
 */
public class ValueMetaData extends AbstractElementMetaData
{
    private static final long serialVersionUID = -3179830024157613599L;

    /**
     * Constructor to create a copy of the passed metadata using the provided parent.
     * @param vmd The metadata to copy
     */
    public ValueMetaData(ValueMetaData vmd)
    {
        super(vmd);
    }

    /**
     * Default constructor. Set the fields using setters, before populate().
     */
    public ValueMetaData()
    {
    }

    /**
     * Populate the MetaData.
     * @param clr Class loader to use
     * @param primary the primary ClassLoader to use (or null)
     */
    public void populate(ClassLoaderResolver clr, ClassLoader primary)
    {
        AbstractMemberMetaData fmd = (AbstractMemberMetaData)parent;
        if (fmd.getMap() == null)
        {
            // TODO Change this to InvalidMetaDataException
            throw new NucleusFatalUserException("The field "+fmd.getFullFieldName()+" is defined with <value>, however no <map> definition was found.");
        }

        // Make sure value type is set and is valid
        fmd.getMap().value.populate(fmd.getAbstractClassMetaData().getPackageName(), clr, primary);
        String valueType = fmd.getMap().getValueType();
        Class valueTypeClass = null;
        try
        {
            valueTypeClass = clr.classForName(valueType, primary);
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new InvalidMemberMetaDataException("044150", fmd.getClassName(), fmd.getName(), 
                valueType);
        }
        if (embeddedMetaData != null &&
            (valueTypeClass.isInterface() || valueTypeClass.getName().equals("java.lang.Object")))
        {
            throw new InvalidMemberMetaDataException("044152", fmd.getClassName(), fmd.getName(), valueTypeClass.getName());
        }

        // TODO This will not work currently since MapMetaData is populated *after* ValueMetaData and so the
        // valueClassMetaData is not yet populated. What we should do is provide a postPopulate() method here
        // that MapMetaData can call when it is populated
        if (embeddedMetaData == null && 
            ((AbstractMemberMetaData)parent).hasMap() && 
            ((AbstractMemberMetaData)parent).getMap().isEmbeddedValue() &&
            ((AbstractMemberMetaData)parent).getJoinMetaData() != null &&
            ((AbstractMemberMetaData)parent).getMap().valueIsPersistent())
        {
            // User has specified that the value is embedded in a join table but not how we embed it
            // so add a dummy definition
            embeddedMetaData = new EmbeddedMetaData();
            embeddedMetaData.parent = this;
        }

        super.populate(clr, primary);
    }
}