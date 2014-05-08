/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved. 
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
 * Interface defining an object that contains Columns.
 */
public interface ColumnMetaDataContainer
{
    /**
     * Accessor for columnMetaData
     * @return Returns the columnMetaData.
     */
    public ColumnMetaData[] getColumnMetaData();

    /**
     * Add a new ColumnMetaData element
     * @param colmd The Column MetaData 
     */
    public void addColumn(ColumnMetaData colmd);
}