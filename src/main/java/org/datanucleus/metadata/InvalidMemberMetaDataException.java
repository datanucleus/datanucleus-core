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

/**
 * Exception thrown when meta-data specific to a member of a class is invalid.
 */
public class InvalidMemberMetaDataException extends InvalidMetaDataException
{
    private static final long serialVersionUID = -8889474376874514402L;
    String className;
    String memberName;

    /**
     * Constructor. The first params element should be the class name, and the second element should be the member name.
     * @param key The message key
     * @param params Params for the message
     */
    public InvalidMemberMetaDataException(String key, Object... params)
    {
        super(key, params);
        this.className = (String)params[0];
        this.memberName = (String)params[1];
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