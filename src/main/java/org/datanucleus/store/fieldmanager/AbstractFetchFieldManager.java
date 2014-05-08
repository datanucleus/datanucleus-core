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
package org.datanucleus.store.fieldmanager;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.state.ObjectProvider;

/**
 * Abstract field manager for retrieval of objects.
 * To be extended by store plugins.
 */
public abstract class AbstractFetchFieldManager extends AbstractFieldManager
{
    protected ExecutionContext ec;

    protected ObjectProvider op;

    protected AbstractClassMetaData cmd;

    /**
     * Constructor to use when retrieving values of fields of existing objects.
     * @param op ObjectProvider for the object
     */
    public AbstractFetchFieldManager(ObjectProvider op)
    {
        this.op = op;
        this.ec = op.getExecutionContext();
        this.cmd = op.getClassMetaData();
    }

    /**
     * Constructor to use when creating new objects of the specified type, say from a query.
     * @param ec ExecutionContext
     * @param cmd Metadata for the class
     */
    public AbstractFetchFieldManager(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        this.ec = ec;
        this.cmd = cmd;
    }
}