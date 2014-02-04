/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved.
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
    Andy Jefferson - coding standards
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.EmbeddedMetaData;
import org.datanucleus.metadata.RelationType;

/**
 * Abstract representation of a field manager.
 */
public abstract class AbstractFieldManager implements FieldManager
{
    /**
     * Default constructor
     */
    public AbstractFieldManager()
    {
        //default constructor
    }

    /**
     * Convenience method to return if the specified member is embedded.
     * Only applies to relation fields, since all other fields are always "embedded".
     * @param mmd Metadata for the member we are interested in
     * @param relationType Relation type of the member we are interested in
     * @param ownerMmd Optional metadata for the owner member (for nested embeddeds only. Set to null if not relevant to the member in question).
     * @return Whether the member is embedded
     */
    protected boolean isMemberEmbedded(AbstractMemberMetaData mmd, RelationType relationType, AbstractMemberMetaData ownerMmd)
    {
        boolean embedded = false;
        if (relationType != RelationType.NONE)
        {
            // Determine if this relation field is embedded
            if (mmd.isEmbedded() || mmd.getEmbeddedMetaData() != null)
            {
                // Does this field have @Embedded definition?
                embedded = true;
            }
            else if (RelationType.isRelationMultiValued(relationType))
            {
                // Is this an embedded element/key/value?
                if (mmd.hasCollection() && mmd.getElementMetaData() != null && mmd.getElementMetaData().getEmbeddedMetaData() != null)
                {
                    // Embedded collection element
                    embedded = true;
                }
                else if (mmd.hasArray() && mmd.getElementMetaData() != null && mmd.getElementMetaData().getEmbeddedMetaData() != null)
                {
                    // Embedded array element
                    embedded = true;
                }
                else if (mmd.hasMap() && 
                        ((mmd.getKeyMetaData() != null && mmd.getKeyMetaData().getEmbeddedMetaData() != null) || 
                        (mmd.getValueMetaData() != null && mmd.getValueMetaData().getEmbeddedMetaData() != null)))
                {
                    // Embedded map key/value
                    embedded = true;
                }
            }

            if (!embedded)
            {
                if (RelationType.isRelationSingleValued(relationType) && ownerMmd != null)
                {
                    if (ownerMmd.hasCollection())
                    {
                        // This is a field of the element of the collection, so check for any metadata spec for it
                        EmbeddedMetaData embmd = ownerMmd.getElementMetaData().getEmbeddedMetaData();
                        if (embmd != null)
                        {
                            AbstractMemberMetaData[] embMmds = embmd.getMemberMetaData();
                            if (embMmds != null)
                            {
                                for (AbstractMemberMetaData embMmd : embMmds)
                                {
                                    if (embMmd.getName().equals(mmd.getName()))
                                    {
                                        if (embMmd.isEmbedded() || embMmd.getEmbeddedMetaData() != null)
                                        {
                                            // Embedded Field is marked in nested embedded definition as embedded
                                            embedded = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (ownerMmd.getEmbeddedMetaData() != null)
                    {
                        // Is this a nested embedded (from JDO definition) with specification for this field?
                        AbstractMemberMetaData[] embMmds = ownerMmd.getEmbeddedMetaData().getMemberMetaData();
                        if (embMmds != null)
                        {
                            for (int i=0;i<embMmds.length;i++)
                            {
                                if (embMmds[i].getName().equals(mmd.getName()))
                                {
                                    embedded = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return embedded;
    }

    private String failureMessage(String method)
    {
        return "Somehow " + getClass().getName() + "." + method + "() was called, which should have been impossible";
    }

    public void storeBooleanField(int fieldNumber, boolean value)
    {
        throw new NucleusException(failureMessage("storeBooleanField")).setFatal();
    }

    public boolean fetchBooleanField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchBooleanField")).setFatal();
    }

    public void storeCharField(int fieldNumber, char value)
    {
        throw new NucleusException(failureMessage("storeCharField")).setFatal();
    }

    public char fetchCharField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchCharField")).setFatal();
    }

    public void storeByteField(int fieldNumber, byte value)
    {
        throw new NucleusException(failureMessage("storeByteField")).setFatal();
    }

    public byte fetchByteField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchByteField")).setFatal();
    }

    public void storeShortField(int fieldNumber, short value)
    {
        throw new NucleusException(failureMessage("storeShortField")).setFatal();
    }

    public short fetchShortField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchShortField")).setFatal();
    }

    public void storeIntField(int fieldNumber, int value)
    {
        throw new NucleusException(failureMessage("storeIntField")).setFatal();
    }

    public int fetchIntField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchIntField")).setFatal();
    }

    public void storeLongField(int fieldNumber, long value)
    {
        throw new NucleusException(failureMessage("storeLongField")).setFatal();
    }

    public long fetchLongField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchLongField")).setFatal();
    }

    public void storeFloatField(int fieldNumber, float value)
    {
        throw new NucleusException(failureMessage("storeFloatField")).setFatal();
    }

    public float fetchFloatField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchFloatField")).setFatal();
    }

    public void storeDoubleField(int fieldNumber, double value)
    {
        throw new NucleusException(failureMessage("storeDoubleField")).setFatal();
    }

    public double fetchDoubleField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchDoubleField")).setFatal();
    }

    public void storeStringField(int fieldNumber, String value)
    {
        throw new NucleusException(failureMessage("storeStringField")).setFatal();
    }

    public String fetchStringField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchStringField")).setFatal();
    }

    public void storeObjectField(int fieldNumber, Object value)
    {
        throw new NucleusException(failureMessage("storeObjectField")).setFatal();
    }

    public Object fetchObjectField(int fieldNumber)
    {
        throw new NucleusException(failureMessage("fetchObjectField")).setFatal();
    }
}
