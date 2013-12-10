/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
 * Utility class providing enums for the different relation types.
 * TODO Consider adding the other subtypes of relations ... join table, foreign key etc
 */
public enum RelationType
{
    NONE,
    ONE_TO_ONE_UNI,
    ONE_TO_ONE_BI,
    ONE_TO_MANY_UNI,
    ONE_TO_MANY_BI,
    MANY_TO_MANY_BI,
    MANY_TO_ONE_BI,
    MANY_TO_ONE_UNI;

    public static boolean isRelationSingleValued(RelationType type)
    {
        if (type == ONE_TO_ONE_UNI || type == ONE_TO_ONE_BI || type == MANY_TO_ONE_UNI || type == MANY_TO_ONE_BI)
        {
            return true;
        }
        return false;
    }

    public static boolean isRelationMultiValued(RelationType type)
    {
        if (type == ONE_TO_MANY_UNI || type == ONE_TO_MANY_BI || type == MANY_TO_MANY_BI)
        {
            return true;
        }
        return false;
    }

    public static boolean isBidirectional(RelationType type)
    {
        if (type == ONE_TO_ONE_BI || type == ONE_TO_MANY_BI || type == MANY_TO_MANY_BI || type == MANY_TO_ONE_BI)
        {
            return true;
        }
        return false;
    }
}