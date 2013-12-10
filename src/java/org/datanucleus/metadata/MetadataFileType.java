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
package org.datanucleus.metadata;

/**
 * Enum for the different types of metadata "files".
 */
public enum MetadataFileType 
{
    JDO_FILE("jdo"),
    JDO_ORM_FILE("orm"),
    JDO_QUERY_FILE("jdoquery"),
    ANNOTATIONS("annotations"),
    JPA_MAPPING_FILE("jpa_mapping");

    String name;

    private MetadataFileType(String name)
    {
        this.name = name;
    }

    public String toString()
    {
        return name;
    }
}