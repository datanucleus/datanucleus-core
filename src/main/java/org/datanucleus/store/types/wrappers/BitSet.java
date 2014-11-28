/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus.store.types.wrappers;

import java.io.ObjectStreamException;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.SCO;

/**
 * A mutable second-class BitSet object.
 */
public class BitSet extends java.util.BitSet implements SCO<java.util.BitSet>
{
    protected transient ObjectProvider ownerOP;
    protected transient AbstractMemberMetaData ownerMmd;

    /**
     * Creates a <tt>BitSet</tt> object. Assigns owning object and field name.
     * @param op ObjectProvider for the owning object
     * @param mmd Metadata for the member
     */
    public BitSet(ObjectProvider op, AbstractMemberMetaData mmd)
    {
        super();
        this.ownerOP = op;
        this.ownerMmd = mmd;
    }

    /**
     * Method to initialise the SCO for use.
     */
    public void initialise()
    {
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.SCO#initialise(java.lang.Object, java.lang.Object)
     */
    public void initialise(java.util.BitSet newValue, Object oldValue)
    {
        initialise(newValue);
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param set The Object
     */
    public void initialise(java.util.BitSet set)
    {
        for (int i = 0; i < length(); i++)
        {
            super.clear(i);
        }
        super.or(set);
    }

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.BitSet getValue()
    {
        java.util.BitSet bits = new java.util.BitSet();
        bits.or(this);
        return bits;
    }

    // ------------------------- SCO Methods -----------------------------

    /**
     * Utility to unset the owner.
     **/
    public void unsetOwner()
    {
        ownerOP = null;
        ownerMmd = null;
    }

    /**
     * Accessor for the owner.
     * @return The owner 
     **/
    public Object getOwner()
    {
        return (ownerOP != null ? ownerOP.getObject() : null);
    }

    /**
     * Accessor for the field name
     * @return The field name
     **/ 
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Utility to mark the object as dirty
     **/
    public void makeDirty()
    {
        if (ownerOP != null)
        {
            ownerOP.makeDirty(ownerMmd.getAbsoluteFieldNumber());
            if (!ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
    }

    /**
     * Method to detach a copy.
     * @param state State for detachment process
     * @return A copy of the object
     */
    public java.util.BitSet detachCopy(FetchPlanState state)
    {
        java.util.BitSet detached = new java.util.BitSet();
        detached.or(this);
        return detached;
    }

    /**
     * Method to attached the passed value.
     * @param value The new value
     */
    public void attachCopy(java.util.BitSet value)
    {
        initialise(value);
        makeDirty();
    }

    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return A clone of the object
     */
    public Object clone()
    {
        Object obj = super.clone();

        ((BitSet)obj).unsetOwner();

        return obj;
    }
    
    /**
     * The writeReplace method is called when ObjectOutputStream is preparing to write the object to the stream. The
     * ObjectOutputStream checks whether the class defines the writeReplace method. If the method is defined, the
     * writeReplace method is called to allow the object to designate its replacement in the stream. The object returned
     * should be either of the same type as the object passed in or an object that when read and resolved will result in
     * an object of a type that is compatible with all references to the object.
     * 
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        java.util.BitSet copy = new java.util.BitSet();
        copy.and(this);
        return copy;
    }         
    
    //  ------------------------- BitSet Methods -----------------------------

    /* (non-Javadoc)
     * @see java.util.BitSet#and(java.util.BitSet)
     */
    public void and(java.util.BitSet set)
    {
        super.and(set);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#andNot(java.util.BitSet)
     */
    public void andNot(java.util.BitSet set)
    {
        super.andNot(set);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#clear(int)
     */
    public void clear(int bitIndex)
    {
        super.clear(bitIndex);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#or(java.util.BitSet)
     */
    public void or(java.util.BitSet set)
    {
        super.or(set);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#set(int)
     */
    public void set(int bitIndex)
    {
        super.set(bitIndex);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#xor(java.util.BitSet)
     */
    public void xor(java.util.BitSet set)
    {
        super.xor(set);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#clear()
     */
    public void clear()
    {
        super.clear();
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#clear(int, int)
     */
    public void clear(int fromIndex, int toIndex)
    {
        super.clear(fromIndex, toIndex);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#flip(int, int)
     */
    public void flip(int fromIndex, int toIndex)
    {
        super.flip(fromIndex, toIndex);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#flip(int)
     */
    public void flip(int bitIndex)
    {
        super.flip(bitIndex);
        makeDirty();
    }    
    
    /* (non-Javadoc)
     * @see java.util.BitSet#set(int, boolean)
     */
    public void set(int bitIndex, boolean value)
    {
        super.set(bitIndex, value);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#set(int, int, boolean)
     */
    public void set(int fromIndex, int toIndex, boolean value)
    {
        super.set(fromIndex, toIndex, value);
        makeDirty();
    }

    /* (non-Javadoc)
     * @see java.util.BitSet#set(int, int)
     */
    public void set(int fromIndex, int toIndex)
    {
        super.set(fromIndex, toIndex);
        makeDirty();
    }
}