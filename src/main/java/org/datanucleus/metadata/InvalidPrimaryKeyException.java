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
 * Exception thrown when a primary key class is found to be invalid for some reason.
 * This is due to an invalid specification of MetaData, or maybe the class specified
 * is just wrong, but we just throw it as a meta-data issue.
 */
public class InvalidPrimaryKeyException extends InvalidClassMetaDataException
{
    private static final long serialVersionUID = 4755699002846237657L;

    public InvalidPrimaryKeyException(String key, String className, Object... params)
    {
        super(key, className, params);
    }
}