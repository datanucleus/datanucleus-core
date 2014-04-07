/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Iterator;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.FetchPlanState;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.SCOMap;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * A mutable second-class Map object. 
 * This is the simplified form that intercepts mutators and marks the field as dirty.
 * It also handles cascade-delete triggering for persistable elements.
 */
public class Map extends AbstractMap implements java.util.Map, SCOMap, Cloneable, java.io.Serializable
{
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    protected transient ObjectProvider ownerOP;
    protected transient AbstractMemberMetaData ownerMmd;

    /** The internal "delegate". */
    protected java.util.Map delegate;

    /**
     * Constructor, using the ObjectProvider of the "owner" and the field name.
     * @param ownerOP The owner ObjectProvider
     * @param mmd Metadata for the member
     */
    public Map(ObjectProvider ownerOP, AbstractMemberMetaData mmd)
    {
        this.ownerOP = ownerOP;
        this.ownerMmd = mmd;
    }

    /**
     * Method to initialise the SCO from an existing value.
     * @param o  The object to set from
     * @param forInsert Whether the object needs inserting in the datastore with this value
     * @param forUpdate Whether to update the datastore with this value
     */
    public synchronized void initialise(Object o, boolean forInsert, boolean forUpdate)
    {
        java.util.Map m = (java.util.Map)o;
        if (m != null)
        {
            delegate = new java.util.HashMap(m); // Make copy of container rather than using same memory
        }
        else
        {
            delegate = new java.util.HashMap();
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023003", 
                ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, true, false)));
        }
    }

    /**
     * Method to initialise the SCO for use.
     */
    public void initialise()
    {
        delegate = new java.util.HashMap();
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(LOCALISER.msg("023003", 
                ownerOP.getObjectAsPrintable(), ownerMmd.getName(), "" + size(), 
                SCOUtils.getSCOWrapperOptionsMessage(true, false, true, false)));
        }
    }

    // ------------------------ Implementation of SCO methods ------------------

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public Object getValue()
    {
        return delegate;
    }

    /**
     * Method to effect the load of the data in the SCO.
     * Used when the SCO supports lazy-loading to tell it to load all now.
     */
    public void load()
    {
        // Always loaded
    }

    /**
     * Method to return if the SCO has its contents loaded. Returns true.
     * @return Whether it is loaded
     */
    public boolean isLoaded()
    {
        return true;
    }

    /**
     * Method to update an embedded key in this map.
     * @param key The key
     * @param fieldNumber Number of field in the key
     * @param newValue New value for this field
     */
    public void updateEmbeddedKey(Object key, int fieldNumber, Object newValue)
    {
        // Just mark field in embedded owners as dirty
        makeDirty();
    }

    /**
     * Method to update an embedded value in this map.
     * @param value The value
     * @param fieldNumber Number of field in the value
     * @param newValue New value for this field
     */
    public void updateEmbeddedValue(Object value, int fieldNumber, Object newValue)
    {
        // Just mark field in embedded owners as dirty
        makeDirty();
    }

    /**
     * Accessor for the field name.
     * @return The field name.
     **/
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Accessor for the owner.
     * @return The owner.
     **/
    public Object getOwner()
    {
        return (ownerOP != null ? ownerOP.getObject() : null);
    }

    /**
     * Method to unset the owner and field information.
     */
    public synchronized void unsetOwner()
    {
        if (ownerOP != null)
        {
            ownerOP = null;
            ownerMmd = null;
        }
    }

    /**
     * Utility to mark the object as dirty
     **/
    public void makeDirty()
    {
        if (ownerOP != null)
        {
            ownerOP.makeDirty(ownerMmd.getAbsoluteFieldNumber());
        }
    }

    /**
     * Method to return a detached copy of the container.
     * Recurse sthrough the keys/values so that they are likewise detached.
     * @param state State for detachment process
     * @return The detached container
     */
    public Object detachCopy(FetchPlanState state)
    {
        java.util.Map detached = new java.util.HashMap();
        SCOUtils.detachCopyForMap(ownerOP, entrySet(), state, detached);
        return detached;
    }

    /**
     * Method to return an attached copy of the passed (detached) value. The returned attached copy
     * is a SCO wrapper. Goes through the existing keys/values in the store for this owner field and
     * removes ones no longer present, and adds new keys/values. All keys/values in the (detached)
     * value are attached.
     * @param value The new (map) value
     */
    public void attachCopy(Object value)
    {
        java.util.Map m = (java.util.Map) value;

        // Attach all of the keys/values in the new map
        boolean keysWithoutIdentity = SCOUtils.mapHasKeysWithoutIdentity(ownerMmd);
        boolean valuesWithoutIdentity = SCOUtils.mapHasValuesWithoutIdentity(ownerMmd);

        java.util.Map attachedKeysValues = new java.util.HashMap(m.size());
        SCOUtils.attachCopyForMap(ownerOP, m.entrySet(), attachedKeysValues, keysWithoutIdentity, valuesWithoutIdentity);

        // Update the attached map with the detached entries
        SCOUtils.updateMapWithMapKeysValues(ownerOP.getExecutionContext().getApiAdapter(), this, attachedKeysValues);
    }

    // -------------------- Implementation of Map Methods ----------------------
 
    /**
     * Creates and returns a copy of this object.
     *
     * <P>Mutable second-class Objects are required to provide a public
     * clone method in order to allow for copying PersistenceCapable
     * objects. In contrast to Object.clone(), this method must not throw a
     * CloneNotSupportedException.
     * @return Clone of the object
     */
    public Object clone()
    {
        return ((java.util.HashMap)delegate).clone();
    }

    /**
     * Utility to check if a key is contained in the Map.
     * @param key The key to check
     * @return Whether it is contained
     **/
    public synchronized boolean containsKey(Object key)
    {
        return delegate.containsKey(key);
    }

    /**
     * Utility to check if a value is contained in the Map.
     * @param value The value to check
     * @return Whether it is contained
     **/
    public synchronized boolean containsValue(Object value)
    {
        return delegate.containsValue(value);
    }

    /**
     * Accessor for the set of entries in the Map.
     * @return Set of entries
     **/
    public synchronized java.util.Set entrySet()
    {
        return delegate.entrySet();
    }

    /**
     * Method to check the equality of this map, and another.
     * @param o The map to compare against.
     * @return Whether they are equal.
     **/
    public synchronized boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    /**
     * Accessor for the value stored against a key.
     * @param key The key
     * @return The value.
     **/
    public synchronized Object get(Object key)
    {
        return delegate.get(key);
    }

    /**
     * Method to generate a hashcode for this Map.
     * @return The hashcode.
     **/
    public synchronized int hashCode()
    {
        return delegate.hashCode();
    }

    /**
     * Method to return if the Map is empty.
     * @return Whether it is empty.
     **/
    public synchronized boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    /**
     * Accessor for the set of keys in the Map.
     * @return Set of keys.
     **/
    public synchronized java.util.Set keySet()
    {
        return delegate.keySet();
    }

    /**
     * Method to return the size of the Map.
     * @return The size
     **/
    public synchronized int size()
    {
        return delegate.size();
    }

    /**
     * Accessor for the set of values in the Map.
     * @return Set of values.
     **/
    public synchronized Collection values()
    {
        return delegate.values();
    }

    /**
     * Method to clear the Map.
     **/
    public synchronized void clear()
    {
        if (ownerOP != null && !delegate.isEmpty())
        {
            // Cascade delete
            if (SCOUtils.hasDependentKey(ownerMmd) || SCOUtils.hasDependentValue(ownerMmd)) 
            {
                Iterator<Map.Entry> entryIter = delegate.entrySet().iterator();
                while (entryIter.hasNext())
                {
                    Map.Entry entry = entryIter.next();
                    if (SCOUtils.hasDependentKey(ownerMmd))
                    {
                        ownerOP.getExecutionContext().deleteObjectInternal(entry.getKey());
                    }
                    if (SCOUtils.hasDependentValue(ownerMmd))
                    {
                        ownerOP.getExecutionContext().deleteObjectInternal(entry.getValue());
                    }
                }
            }
        }

        delegate.clear();

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to add a value to the Map.
     * @param key The key for the value.
     * @param value The value
     * @return The previous value against this key (if any).
     **/
    public synchronized Object put(Object key, Object value)
    {
        // Reject inappropriate elements
        Object oldValue = delegate.put(key, value);
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
        return oldValue;
    }

    /**
     * Method to add a Map of values to this map.
     * @param m The Map to add
     **/
    public synchronized void putAll(java.util.Map m)
    {
        delegate.putAll(m);
        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }
    }

    /**
     * Method to remove a value from the Map.
     * @param key The key for the value.
     * @return The value removed.
     **/
    public synchronized Object remove(Object key)
    {
        Object value = delegate.remove(key);

        if (ownerOP != null)
        {
            // Cascade delete
            if (SCOUtils.hasDependentKey(ownerMmd) || SCOUtils.hasDependentValue(ownerMmd)) 
            {
                if (SCOUtils.hasDependentKey(ownerMmd))
                {
                    ownerOP.getExecutionContext().deleteObjectInternal(key);
                }
                if (SCOUtils.hasDependentValue(ownerMmd))
                {
                    ownerOP.getExecutionContext().deleteObjectInternal(value);
                }
            }
        }

        makeDirty();
        if (ownerOP != null && !ownerOP.getExecutionContext().getTransaction().isActive())
        {
            ownerOP.getExecutionContext().processNontransactionalUpdate();
        }

        return value;
    }

    /**
     * The writeReplace method is called when ObjectOutputStream is preparing
     * to write the object to the stream. The ObjectOutputStream checks whether
     * the class defines the writeReplace method. If the method is defined, the
     * writeReplace method is called to allow the object to designate its
     * replacement in the stream. The object returned should be either of the
     * same type as the object passed in or an object that when read and
     * resolved will result in an object of a type that is compatible with all
     * references to the object.
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace() throws ObjectStreamException
    {
        return new java.util.HashMap(delegate);
    }
}