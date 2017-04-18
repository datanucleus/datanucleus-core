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
package org.datanucleus.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of a StateManager for use where insertion ordering is important (such as RDBMS).
 * Adds on simple handling to be run after an object is inserted.
 */
public class ReferentialStateManagerImpl extends StateManagerImpl
{
    /** List of StateManagers that we must notify when we have completed inserting our record. */
    private List<ReferentialStateManagerImpl> insertionNotifyList = null;

    /** Fields of this object that we must update when notified of the insertion of the related objects. */
    private Map<ReferentialStateManagerImpl, FieldContainer> fieldsToBeUpdatedAfterObjectInsertion = null;

    /**
     * Constructor for object of specified type managed by the provided ExecutionContext.
     * @param ec ExecutionContext
     * @param cmd the metadata for the class.
     */
    public ReferentialStateManagerImpl(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        super(ec, cmd);
    }

    @Override
    public void connect(ExecutionContext ec, AbstractClassMetaData cmd)
    {
        super.connect(ec, cmd);
        fieldsToBeUpdatedAfterObjectInsertion = null;
        insertionNotifyList = null;
    }

    @Override
    public void disconnect()
    {
        this.fieldsToBeUpdatedAfterObjectInsertion = null;
        this.insertionNotifyList = null;
        super.disconnect();
    }

    /**
     * Change the activity state to a particular state.
     * @param activityState the new state
     */
    public void changeActivityState(ActivityState activityState)
    {
        activity = activityState;
        if (activityState == ActivityState.INSERTING_CALLBACKS && insertionNotifyList != null)
        {
            // Full insertion has just completed so notify all interested parties
            synchronized (insertionNotifyList)
            {
                Iterator<ReferentialStateManagerImpl> notifyIter = insertionNotifyList.iterator();
                while (notifyIter.hasNext())
                {
                    ReferentialStateManagerImpl notifySM = notifyIter.next();
                    notifySM.insertionCompleted(this);
                }
            }
            insertionNotifyList.clear();
            insertionNotifyList = null;
        }
    }

    /**
     * Marks the given field as being required to be updated when the specified object has been inserted.
     * @param pc The Persistable object
     * @param fieldNumber Number of the field.
     */
    public void updateFieldAfterInsert(Object pc, int fieldNumber)
    {
        ReferentialStateManagerImpl otherSM = (ReferentialStateManagerImpl) myEC.findObjectProvider(pc);

        // Register the other SM to update us when it is inserted
        if (otherSM.insertionNotifyList == null)
        {
            otherSM.insertionNotifyList = Collections.synchronizedList(new ArrayList(1));
        }
        otherSM.insertionNotifyList.add(this);

        // Register that we should update this field when the other SM informs us
        if (fieldsToBeUpdatedAfterObjectInsertion == null)
        {
            fieldsToBeUpdatedAfterObjectInsertion = new HashMap(1);
        }
        FieldContainer cont = fieldsToBeUpdatedAfterObjectInsertion.get(otherSM);
        if (cont == null)
        {
            cont = new FieldContainer(fieldNumber);
        }
        else
        {
            cont.set(fieldNumber);
        }
        fieldsToBeUpdatedAfterObjectInsertion.put(otherSM, cont);

        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("026021", 
                cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber).getFullFieldName(), 
                StringUtils.toJVMIDString(myPC), StringUtils.toJVMIDString(pc)));
        }
    }

    /**
     * Method called by another ObjectProvider when this object has registered that it needs to know
     * when the other object has been inserted.
     * @param op ObjectProvider of the other object that has just been inserted
     */
    void insertionCompleted(ReferentialStateManagerImpl op)
    {
        if (fieldsToBeUpdatedAfterObjectInsertion == null)
        {
            return;
        }

        // Go through our insertion update list and mark all required fields as dirty
        FieldContainer fldCont = fieldsToBeUpdatedAfterObjectInsertion.get(op);
        if (fldCont != null)
        {
            dirty = true;
            int[] fieldsToUpdate = fldCont.getFields();
            for (int i=0;i<fieldsToUpdate.length;i++)
            {
                if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                {
                    NucleusLogger.PERSISTENCE.debug(Localiser.msg("026022", 
                        cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldsToUpdate[i]).getFullFieldName(), 
                        IdentityUtils.getPersistableIdentityForId(myID),
                        StringUtils.toJVMIDString(op.getObject())));
                }
                dirtyFields[fieldsToUpdate[i]] = true;
            }
            fieldsToBeUpdatedAfterObjectInsertion.remove(op);
            if (fieldsToBeUpdatedAfterObjectInsertion.isEmpty())
            {
                fieldsToBeUpdatedAfterObjectInsertion = null;
            }

            try
            {
                flags |= FLAG_POSTINSERT_UPDATE;

                // Perform our update
                flush();
            }
            finally
            {
                flags &= ~FLAG_POSTINSERT_UPDATE;
            }
        }
    }

    /** Private class storing the fields to be updated for a StateManager, when it is inserted. */
    private class FieldContainer
    {
        boolean[] fieldsToUpdate = new boolean[cmd.getAllMemberPositions().length];

        public FieldContainer(int fieldNumber)
        {
            fieldsToUpdate[fieldNumber] = true;
        }
        public void set(int fieldNumber)
        {
            fieldsToUpdate[fieldNumber] = true;
        }
        public int[] getFields()
        {
            return ClassUtils.getFlagsSetTo(fieldsToUpdate,true);
        }
    }
}