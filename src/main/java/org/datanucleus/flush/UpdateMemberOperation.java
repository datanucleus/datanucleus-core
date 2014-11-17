/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.flush;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.ObjectProvider;

/**
 * Flush operation for an update of the value of a member of the specified object.
 */
public class UpdateMemberOperation implements Operation
{
    final ObjectProvider op;
    final int fieldNumber;
    Object oldValue;
    Object newValue;

    public UpdateMemberOperation(ObjectProvider op, int fieldNum, Object newVal, Object oldVal)
    {
        this.op = op;
        this.fieldNumber = fieldNum;
        this.newValue = newVal;
        this.oldValue = oldVal;
    }

    public Object getNewValue()
    {
        return newValue;
    }

    public Object getOldValue()
    {
        return oldValue;
    }

    /**
     * Accessor for the metadata for the member that this operation is for.
     * @return The member metadata
     */
    public AbstractMemberMetaData getMemberMetaData()
    {
        return op.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.Operation#getObjectProvider()
     */
    public ObjectProvider getObjectProvider()
    {
        return op;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.Operation#perform()
     */
    public void perform()
    {
        // TODO Call update of the field of this object. Currently handled by FlushProcess
    }

    public String toString()
    {
        return "UPDATE : " + op + " field=" + getMemberMetaData().getName();
    }
}