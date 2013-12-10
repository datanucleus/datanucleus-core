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

import java.io.Serializable;

import org.datanucleus.util.StringUtils;

/**
 * Representation of the Meta-Data for an extension.
 * Takes the form of key and value. The vendor will be "datanucleus".
 */
public class ExtensionMetaData implements Serializable
{
    /** vendor-name tag value. */
    protected String vendorName;

    /** key tag value. */
    protected String key;

    /** value tag value. */
    protected String value;

    /**
     * Constructor
     * @param vendorName vendor-name tag value
     * @param key key tag value
     * @param value  value tag value
     */
    public ExtensionMetaData(String vendorName, String key, String value)
    {
        this.vendorName = (StringUtils.isWhitespace(vendorName) ? null : vendorName);
        this.key = (StringUtils.isWhitespace(key) ? null : key);
        this.value = (StringUtils.isWhitespace(value) ? null : value);
    }

    /**
     * Accessor for the key tag value.
     * @return key tag value
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Accessor for the value tag value .
     * @return value tag value
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Accessor for the vendor-name tag value.
     * @return vendor-name tag value
     */
    public String getVendorName()
    {
        return vendorName;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    public String toString()
    {
        return "<extension vendor-name=\"" + vendorName + "\" key=\"" + key + "\" value=\"" + value + "\"/>";
    }
}