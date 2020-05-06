/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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

import java.net.URI;

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.StringUtils;

/**
 * Class to handle the conversion between java.net.URI and a String form.
 */
public class URIStringConverter implements TypeConverter<URI, String>
{
    private static final long serialVersionUID = -3784990025093845546L;

    public URI toMemberType(String str)
    {
        if (StringUtils.isWhitespace(str))
        {
            return null;
        }

        try
        {
            return java.net.URI.create(str.trim());
        }
        catch (IllegalArgumentException iae)
        {
            throw new NucleusDataStoreException(Localiser.msg("016002", str, URI.class.getName()), iae);
        }
    }

    public String toDatastoreType(URI uri)
    {
        return uri != null ? uri.toString() : null;
    }
}