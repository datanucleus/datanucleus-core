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
    ...
**********************************************************************/
package org.datanucleus.metadata;

/**
 * Three common strategies for versioning instances are supported by standard
 * metadata. These include state-comparison, timestamp, and version-number.
 * <ul>
 * <li><b>date-time</b> involves comparing the value in a date-time column in the table.
 * The first time in a transaction the row is updated, the timestamp value is
 * updated to the current time.</li>
 * <li><b>version-number</b> involves comparing the value in a numeric column in the table.
 * The first time in a transaction the row is updated, the version-number column
 * value is incremented.</li>
 * <li><b>state-image</b> involves comparing the values in specific columns to
 * determine if the database row was changed.</li>
 * </ul>
 */
public enum VersionStrategy
{
    NONE("none"),
    VERSION_NUMBER("version-number"),
    DATE_TIME("date-time"),
    STATE_IMAGE("state-image");

    String name;

    private VersionStrategy(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }

    /**
     * Return VersionStrategy from String.
     * @param value strategy attribute value
     * @return Instance of VersionStrategy. 
     *         If value invalid, return null.
     */
    public static VersionStrategy getVersionStrategy(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else if (VersionStrategy.NONE.toString().equalsIgnoreCase(value))
        {
            return VersionStrategy.NONE;
        }
        else if (VersionStrategy.STATE_IMAGE.toString().equalsIgnoreCase(value))
        {
            return VersionStrategy.STATE_IMAGE;
        }
        else if (VersionStrategy.DATE_TIME.toString().equalsIgnoreCase(value))
        {
            return VersionStrategy.DATE_TIME;
        }
        else if (VersionStrategy.VERSION_NUMBER.toString().equalsIgnoreCase(value))
        {
            return VersionStrategy.VERSION_NUMBER;
        }
        return null;
    }
}