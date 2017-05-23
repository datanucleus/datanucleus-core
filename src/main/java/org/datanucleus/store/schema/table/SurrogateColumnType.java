/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.schema.table;

/**
 * Enum defining the types of surrogate columns for a class.
 * The <pre>fieldNumber</pre> is used where we need to pass a list of fields to be fetched, or updated, and want to include surrogate fields.
 * The normal fields of a class are numbered from 0 upwards, so we use negative numbers for the surrogates.
 */
public enum SurrogateColumnType
{
    DATASTORE_ID(-1),
    VERSION(-2),
    DISCRIMINATOR(-3),
    MULTITENANCY(-4),
    CREATE_TIMESTAMP(-5),
    UPDATE_TIMESTAMP(-6),
    SOFTDELETE(-7);

    int fieldNumber;

    private SurrogateColumnType(int fieldNumber)
    {
        this.fieldNumber = fieldNumber;
    }

    public int getFieldNumber()
    {
        return fieldNumber;
    }
}
