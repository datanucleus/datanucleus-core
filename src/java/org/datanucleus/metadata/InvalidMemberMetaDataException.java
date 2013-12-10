/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.util.Localiser;

/**
 * Exception thrown when meta-data specific to a member of a class is invalid.
 */
public class InvalidMemberMetaDataException extends InvalidMetaDataException
{
    String className;
    String memberName;

    public InvalidMemberMetaDataException(Localiser localiser, String key, Object... params)
    {
        super(localiser, key, params);
        this.className = (String)params[0];
        this.memberName = (String)params[1];
    }

    public InvalidMemberMetaDataException(Localiser localiser, String key, String className, String memberName)
    {
        super(localiser, key, className, memberName);
        this.className = className;
        this.memberName = memberName;
    }

    public InvalidMemberMetaDataException(Localiser localiser, String key, String className, String memberName,
            Object param1)
    {
        super(localiser, key, className, memberName, param1);
        this.className = className;
        this.memberName = memberName;
    }

    public InvalidMemberMetaDataException(Localiser localiser, String key, String className, String memberName,
            Object param1, Object param2)
    {
        super(localiser, key, className, memberName, param1, param2);
        this.className = className;
        this.memberName = memberName;
    }

    public InvalidMemberMetaDataException(Localiser localiser, String key, String className, String memberName,
            Object param1, Object param2, Object param3)
    {
        super(localiser, key, className, memberName, param1, param2, param3);
        this.className = className;
        this.memberName = memberName;
    }

    public String getClassName()
    {
        return className;
    }

    public String getMemberName()
    {
        return memberName;
    }
}