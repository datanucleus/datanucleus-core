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
package org.datanucleus.store.fieldmanager;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.state.ObjectProvider;

/**
 * Abstract field manager for storage of objects.
 * To be extended by store plugins.
 */
public abstract class AbstractStoreFieldManager extends AbstractFieldManager
{
    protected ExecutionContext ec;

    protected ObjectProvider op;

    protected AbstractClassMetaData cmd;

    protected boolean insert;

    public AbstractStoreFieldManager(ObjectProvider op, boolean insert)
    {
        this.ec = op.getExecutionContext();
        this.op = op;
        this.cmd = op.getClassMetaData();
        this.insert = insert;
    }

    protected boolean isStorable(int fieldNumber)
    {
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        return isStorable(mmd);
    }

    protected boolean isStorable(AbstractMemberMetaData mmd)
    {
        if (mmd.getPersistenceModifier() != FieldPersistenceModifier.PERSISTENT)
        {
            // Member not persistent so ignore
            return false;
        }

        if ((insert && mmd.isInsertable()) || (!insert && mmd.isUpdateable()))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}