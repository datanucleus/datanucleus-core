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
package org.datanucleus.store.schema.table;

import java.util.List;

import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Interface to be implemented by any store plugin if it wants to set attributes on Column during its population.
 */
public interface ColumnAttributer
{
    /**
     * Method called when the specified Column has been created, for the 
     * @param col The column
     * @param mmd The member metadata that this relates to, or null if this is a column for a surrogate (datastore id, version etc).
     */
    void attributeColumn(Column col, AbstractMemberMetaData mmd);

    /**
     * Method called when the specified Column has been created for the specified embedded member.
     * @param col The column
     * @param mmds The member metadata that this column is for, allowing navigation
     */
    void attributeEmbeddedColumn(Column col, List<AbstractMemberMetaData> mmds);
}