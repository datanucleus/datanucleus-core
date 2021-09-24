/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved. 
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
2005 Andy Jefferson - cater for object not being persistent
2007 Andy Jefferson - attach in-situ
2008 Andy Jefferson - loading of fields handling
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.RelationType;
import org.datanucleus.state.DNStateManager;
import org.datanucleus.store.types.SCO;
import org.datanucleus.store.types.SCOContainer;
import org.datanucleus.store.types.SCOUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Utility class to handle the attach of fields.
 * The attachment process has 2 distinct cases to cater for.
 * <OL>
 * <LI>The object was detached, has been updated, and needs reattaching.</LI>
 * <LI>The object was detached from a different datastore, and is being attached here and we want to
 *     do a pass through the object to update the fields in the object before it is persisted</LI>
 * </OL>
 * In the first case, the fields which are specified have their values (and dirty flags) updated.
 * In the second case, all fields have their fields (and dirty flags) updated.
 * In addition this field manager allows attaching a copy, or attaching in-situ.
 */
public class AttachFieldManager extends AbstractFieldManager
{
    /** StateManager for the attached instance */
    private final DNStateManager attachedSM;

    /** The second class mutable fields. */
    private final boolean[] secondClassMutableFields;

    /** Fields that were marked as dirty at attach. */
    private final boolean dirtyFields[];

    /** Whether the attached instance is persistent yet. */
    private final boolean persistent;

    /** Whether to cascade the attach to related fields. */
    private final boolean cascadeAttach;

    /** Whether we should create attached copies, or attach in situ. */
    boolean copy = true;

    /**
     * Constructor.
     * @param attachedSM StateManager for the attached instance
     * @param secondClassMutableFields second class mutable field flags
     * @param dirtyFields Flags for whether the field(s) are dirty
     * @param persistent whether the object being "attached" is persistent (yet)
     * @param cascadeAttach Whether to cascade any attach calls to related fields
     * @param copy Whether to attach copy
     */
    public AttachFieldManager(DNStateManager attachedSM, boolean[] secondClassMutableFields, 
            boolean[] dirtyFields, boolean persistent, boolean cascadeAttach, boolean copy)
    {
        this.attachedSM = attachedSM;
        this.secondClassMutableFields = secondClassMutableFields;
        this.dirtyFields = dirtyFields;
        this.persistent = persistent;
        this.cascadeAttach = cascadeAttach;
        this.copy = copy;
    }

    /**
     * Method to store an object field into the attached instance.
     * @param fieldNumber Number of the field to store
     * @param value the value in the detached instance
     */
    public void storeObjectField(int fieldNumber, Object value)
    {
        // Note : when doing updates always do replaceField first and makeDirty after since the replaceField can cause flush() to be called 
        // meaning that an update with null would be made before the new value makes it into the field
        AbstractClassMetaData cmd = attachedSM.getClassMetaData();
        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        ExecutionContext ec = attachedSM.getExecutionContext();
        RelationType relationType = mmd.getRelationType(ec.getClassLoaderResolver());

//        boolean processWhenExisting = true;
        if (mmd.hasExtension("attach"))
        {
            // Undocumented extension, allow a user to mark a field as never attach. Arguably we should unload the (attached) field, so it goes to DB for original value
            if (mmd.getValueForExtension("attach").equalsIgnoreCase("never"))
            {
                // Member is tagged to not attach, so put a null
                attachedSM.replaceFieldMakeDirty(fieldNumber, null);
                return;
            }
            // TODO Support attach only when not present in the datastore (only for PCs)
//            else if (mmd.getValueForExtension("attach").equalsIgnoreCase("when-non-existing"))
//            {
//                processWhenExisting = false;
//            }
        }

        // TODO Link the mmd extension flag in with what is passed in to the AttachFieldManager?
        if (RelationType.isRelationSingleValued(relationType) && !mmd.isCascadeAttach())
        {
            // We have cascade-attach disabled for this member, so we unload it (forces load from DB)
            NucleusLogger.GENERAL.debug(">> AttachFM.storeObjectField mmd=" + mmd.getFullFieldName() + " but not allowed to merge it, so unloading field");
            // Member is tagged to not attach, so unload it
            attachedSM.unloadField(mmd.getName());
            return;
        }
        else if (RelationType.isRelationMultiValued(relationType) && !mmd.isCascadeAttach())
        {
            NucleusLogger.GENERAL.debug(">> AttachFM.storeObjectField mmd=" + mmd.getFullFieldName() + " but not allowed to merge it, so unloading field");
            // Member is tagged to not attach, so unload it
            attachedSM.unloadField(mmd.getName());
            return;
        }

        // Use ContainerHandlers to support non-JDK Collections and single element collections
        ApiAdapter api = ec.getApiAdapter();
        if (value == null)
        {
            Object oldValue = null;
            if (mmd.isDependent() && persistent)
            {
                // Get any old value of this field so we can do cascade-delete if being nulled
                try
                {
                    attachedSM.loadFieldFromDatastore(fieldNumber);
                }
                catch (Exception e)
                {
                    // Error loading the field so didn't exist before attaching anyway
                }
                oldValue = attachedSM.provideField(fieldNumber);
            }

            attachedSM.replaceField(fieldNumber, null);
            if (dirtyFields[fieldNumber] || !persistent)
            {
                attachedSM.makeDirty(fieldNumber);
            }

            if (mmd.isDependent() && !mmd.isEmbedded() && oldValue != null && api.isPersistable(oldValue))
            {
                // Check for a field storing a PC where it is being nulled and the other object is dependent
                attachedSM.flush(); // Flush the nulling of the field
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("026026", oldValue, mmd.getFullFieldName()));
                ec.deleteObjectInternal(oldValue);
            }
        }
        else if (secondClassMutableFields[fieldNumber])
        {
            if (mmd.isSerialized() && !RelationType.isRelationMultiValued(relationType))
            {
                // SCO Field is serialised, and no persistable elements so just update the column with this new value
                attachedSM.replaceFieldMakeDirty(fieldNumber, value);
                attachedSM.makeDirty(fieldNumber);
            }
            else
            {
                // Make sure that the value is a SCO wrapper
                Object oldValue = null;
                if (persistent && !attachedSM.isFieldLoaded(fieldNumber))
                {
                    attachedSM.loadField(fieldNumber);
                }
                oldValue = attachedSM.provideField(fieldNumber);
                boolean changed = dirtyFields[fieldNumber];
                if (!changed)
                {
                    // Check if the new value is different (not detected if detached field was not a wrapper and mutable)
                    if (oldValue == null)
                    {
                        changed = true;
                    }
                    else
                    {
                        if (mmd.hasCollection() && relationType != RelationType.NONE)
                        {
                            boolean collsEqual = SCOUtils.collectionsAreEqual(api, (Collection)oldValue, (Collection)value);
                            changed = !collsEqual;
                        }
                        // TODO Do like the above for relational Maps
                        else
                        {
                            changed = !(oldValue.equals(value));
                        }
                    }
                }

                SCO sco;
                if (oldValue == null || !(oldValue instanceof SCO))
                {
                    // Detached object didn't use wrapped field
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug(Localiser.msg("026029", IdentityUtils.getPersistableIdentityForId(attachedSM.getInternalObjectId()), mmd.getName()));
                    }
                    sco = ec.getTypeManager().createSCOInstance(attachedSM, mmd, value.getClass(), null, false);
                    if (sco instanceof SCOContainer)
                    {
                        // Load any containers to avoid update issues
                        ((SCOContainer)sco).load();
                    }
                    attachedSM.replaceFieldMakeDirty(fieldNumber, sco);
                }
                else
                {
                    // The field is already a SCO wrapper, so just copy the new values to it
                    sco = (SCO) oldValue;
                }

                if (cascadeAttach)
                {
                    // Only trigger the cascade when required
                    if (copy)
                    {
                        // Attach copy of the SCO
                        sco.attachCopy(value);
                    }
                    else
                    {
                        // Attach SCO in-situ
                        // TODO This doesn't seem to handle any removal of things while detached
                        // Should be changed to do things like in attachCopy above.
                        if (sco instanceof Collection)
                        {
                            // Attach all PC elements of the collection
                            SCOUtils.attachForCollection(attachedSM, ((Collection)value).toArray(), SCOUtils.collectionHasElementsWithoutIdentity(mmd));
                        }
                        else if (sco instanceof Map)
                        {
                            // Attach all PC keys/values of the map
                            SCOUtils.attachForMap(attachedSM, ((Map)value).entrySet(), SCOUtils.mapHasKeysWithoutIdentity(mmd), SCOUtils.mapHasValuesWithoutIdentity(mmd));
                        }
                        else
                        {
                            // Initialise the SCO with the new value
                            sco.initialise(value);
                        }
                    }
                }

                if (changed || !persistent)
                {
                    attachedSM.makeDirty(fieldNumber);
                }
            }
        }
        else if (mmd.getType().isArray() && RelationType.isRelationMultiValued(relationType))
        {
            // Array of persistable objects
            if (mmd.isSerialized() || mmd.isEmbedded())
            {
                // Field is serialised/embedded so just update the column with this new value TODO Make sure they have StateManagers
                attachedSM.replaceField(fieldNumber, value);
                if (dirtyFields[fieldNumber] || !persistent)
                {
                    attachedSM.makeDirty(fieldNumber);
                }
            }
            else
            {
                Object oldValue = attachedSM.provideField(fieldNumber);
                if (oldValue == null && !attachedSM.getLoadedFields()[fieldNumber] && persistent)
                {
                    // Retrieve old value for field
                    attachedSM.loadField(fieldNumber);
                    oldValue = attachedSM.provideField(fieldNumber);
                }

                if (cascadeAttach)
                {
                    // Only trigger the cascade when required
                    Object arr = Array.newInstance(mmd.getType().getComponentType(), Array.getLength(value));
                    for (int i=0;i<Array.getLength(value);i++)
                    {
                        Object elem = Array.get(value, i);
                        // TODO Compare with old value and handle delete dependent etc
                        if (copy)
                        {
                            Object elemAttached = ec.attachObjectCopy(attachedSM, elem, false);
                            Array.set(arr, i, elemAttached);
                        }
                        else
                        {
                            ec.attachObject(attachedSM, elem, false);
                            Array.set(arr, i, elem);
                        }
                    }

                    attachedSM.replaceFieldMakeDirty(fieldNumber, arr);
                }

                if (dirtyFields[fieldNumber] || !persistent)
                {
                    attachedSM.makeDirty(fieldNumber);
                }
            }
        }
        else if (RelationType.isRelationSingleValued(relationType))
        {
            // 1-1/N-1
            DNStateManager valueSM = ec.findStateManager(value);
            if (valueSM != null && valueSM.getReferencedPC() != null && !api.isPersistent(value))
            {
                // Value has StateManager and has referenced object so is being attached, so refer to attached PC
                if (dirtyFields[fieldNumber])
                {
                    attachedSM.replaceFieldMakeDirty(fieldNumber, valueSM.getReferencedPC());
                }
                else
                {
                    attachedSM.replaceField(fieldNumber, valueSM.getReferencedPC());
                }
            }

            if (cascadeAttach)
            {
                // Determine if field is persisted into the owning object (embedded/serialised)
                boolean sco = mmd.getEmbeddedMetaData() != null || mmd.isSerialized() || mmd.isEmbedded();
                if (copy)
                {
                    // Attach copy of the PC
                    value = ec.attachObjectCopy(attachedSM, value, sco);
                    if (sco || dirtyFields[fieldNumber])
                    {
                        // Either embedded/serialised or marked as changed, so make it dirty
                        attachedSM.replaceFieldMakeDirty(fieldNumber, value);
                    }
                    else
                    {
                        attachedSM.replaceField(fieldNumber, value);
                    }
                }
                else
                {
                    // Attach PC in-situ
                    ec.attachObject(attachedSM, value, sco);
                }

                // Make sure the field is marked as dirty
                if (dirtyFields[fieldNumber] || !persistent)
                {
                    attachedSM.makeDirty(fieldNumber);
                }
                else if (sco && value != null && api.isDirty(value))
                {
                    attachedSM.makeDirty(fieldNumber);
                }
            }
            else if (dirtyFields[fieldNumber] || !persistent)
            {
                attachedSM.makeDirty(fieldNumber);
            }
        }
        else
        {
            attachedSM.replaceField(fieldNumber, value);
            if (dirtyFields[fieldNumber] || !persistent)
            {
                attachedSM.makeDirty(fieldNumber);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeBooleanField(int, boolean)
     */
    public void storeBooleanField(int fieldNumber, boolean value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeBooleanField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeByteField(int, byte)
     */
    public void storeByteField(int fieldNumber, byte value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeByteField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeCharField(int, char)
     */
    public void storeCharField(int fieldNumber, char value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeCharField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeDoubleField(int, double)
     */
    public void storeDoubleField(int fieldNumber, double value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeDoubleField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeFloatField(int, float)
     */
    public void storeFloatField(int fieldNumber, float value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeFloatField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeIntField(int, int)
     */
    public void storeIntField(int fieldNumber, int value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeIntField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeLongField(int, long)
     */
    public void storeLongField(int fieldNumber, long value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeLongField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeShortField(int, short)
     */
    public void storeShortField(int fieldNumber, short value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeShortField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }

    /*
     * (non-Javadoc)
     * @see FieldConsumer#storeStringField(int, java.lang.String)
     */
    public void storeStringField(int fieldNumber, String value)
    {
        SingleValueFieldManager sfv = new SingleValueFieldManager();
        sfv.storeStringField(fieldNumber, value);
        attachedSM.replaceFields(new int[]{fieldNumber}, sfv);
        if (dirtyFields[fieldNumber] || !persistent)
        {
            attachedSM.makeDirty(fieldNumber);
        }
    }
}