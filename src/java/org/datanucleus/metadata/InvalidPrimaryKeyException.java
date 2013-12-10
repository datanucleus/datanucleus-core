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

import org.datanucleus.util.Localiser;

/**
 * Exception thrown when a primary key class is found to be invalid for some reason.
 * This is due to an invalid specification of MetaData, or maybe the class specified
 * is just wrong, but we just throw it as a meta-data issue.
 */
public class InvalidPrimaryKeyException extends InvalidClassMetaDataException
{
    public InvalidPrimaryKeyException(Localiser localiser, String key, String className)
    {
        super(localiser, key, className);
    }

    public InvalidPrimaryKeyException(Localiser localiser, String key, String className,
            Object param1)
    {
        super(localiser, key, className, param1);
    }

    public InvalidPrimaryKeyException(Localiser localiser, String key, String className,
            Object param1, Object param2)
    {
        super(localiser, key, className, param1, param2);
    }

    public InvalidPrimaryKeyException(Localiser localiser, String key, String className,
            Object param1, Object param2, Object param3)
    {
        super(localiser, key, className, param1, param2, param3);
        this.messageKey = key;
    }

    public InvalidPrimaryKeyException(Localiser localiser, String key, String className,
            Object param1, Object param2, Object param3, Object param4)
    {
        super(localiser, key, className, param1, param2, param3, param4);
        this.messageKey = key;
    }
}