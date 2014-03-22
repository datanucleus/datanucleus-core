/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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

/**
 * Interface implemented by a TypeConverter when it converts a member to multiple columns, providing the
 * information about what types the columns store.
 */
public interface MultiColumnConverter
{
    /**
     * Accessor for the number of columns the member is stored into.
     * @return Number of columns
     */
    int getNumberOfColumns();

    /**
     * Accessor for the java type of the datastore column for the specified position.
     * @param position The position (0=first, 1=second, etc)
     * @return The java type of the column value
     */
    Class getDatastoreColumnType(int position);
}
