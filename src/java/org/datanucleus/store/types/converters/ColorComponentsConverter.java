/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.converters;

import java.awt.Color;

/**
 * TypeConverter for storing a java.awt.Color as its 4 components (red, green, blue, alpha).
 */
public class ColorComponentsConverter implements TypeConverter<Color, int[]>, MultiColumnConverter
{
    public int[] toDatastoreType(Color memberValue)
    {
        return new int[] {memberValue.getRed(), memberValue.getGreen(), memberValue.getBlue(), memberValue.getAlpha()};
    }

    public Color toMemberType(int[] datastoreValue)
    {
        if (datastoreValue == null)
        {
            return null;
        }

        return new Color(datastoreValue[0], datastoreValue[1], datastoreValue[2], datastoreValue[3]);
    }

    public Class[] getDatastoreColumnTypes()
    {
        return new Class[]{int.class, int.class, int.class, int.class};
    }
}