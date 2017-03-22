/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.identity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.NucleusLogger;

/**
 * Series of utilities for handling identities of objects.
 */
public class IdentityUtils
{
    /**
     * Checks whether the passed class name is valid for a single field application-identity.
     * @param className the identity class name
     * @return Whether it is a single field class
     */
    public static boolean isSingleFieldIdentityClass(String className)
    {
        if (className == null || className.length() < 1)
        {
            return false;
        }
        return className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_BYTE) || 
               className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_CHAR) || 
               className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_INT) ||
               className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_LONG) || 
               className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_OBJECT) || 
               className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_SHORT) ||
               className.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_STRING);
    }

    /**
     * Simple method to return the target class name of the persistable object that the provided id represents.
     * If this is a datastore identity (OID) or single-field identity then returns the class name.
     * Otherwise returns null. Does no inheritance checking.
     * TODO Cater for user-provided object-id class that has a field "className" defining the target name
     * @param id The identity
     * @return Class name for the identity if easily determinable
     */
    public static String getTargetClassNameForIdentity(Object id)
    {
        if (id instanceof DatastoreId)
        {
            // Object is an OID
            return ((DatastoreId)id).getTargetClassName();
        }
        else if (id instanceof SingleFieldId)
        {
            // Using SingleFieldIdentity so can assume that object is of the target class
            return ((SingleFieldId)id).getTargetClassName();
        }
        else
        {
            try
            {
                Object val = ClassUtils.getValueOfFieldByReflection(id, "className");
                // TODO Convert to String and return
                NucleusLogger.GENERAL.info(">> IdentityUtils.getTargetClassNameForIdentity type=" + id.getClass().getName() + " -> className=" + val);
            }
            catch (NucleusException ne)
            {
                NucleusLogger.GENERAL.debug(">> Attempt to find field 'className' of object of type " + id.getClass() + " gave exception : " + ne.getMessage());
            }
        }

        // Must be user-specified identity so just return
        return null;
    }

    /**
     * Accessor for whether the passed identity is a valid single-field application-identity for this API.
     * @param id The id
     * @return Whether it is valid
     */
    public static boolean isSingleFieldIdentity(Object id)
    {
        return id instanceof SingleFieldId;
    }

    /**
     * Accessor for whether the provided id is datastore identity.
     * @param id The identity to check
     * @return Whether it is datastore identity
     */
    public static boolean isDatastoreIdentity(Object id)
    {
        return id != null && id instanceof DatastoreId;
    }

    /**
     * Accessor for the key object for the specified single field application-identity.
     * @param id The identity
     * @return The key object
     */
    public static Object getTargetKeyForSingleFieldIdentity(Object id)
    {
        return id instanceof SingleFieldId ? ((SingleFieldId)id).getKeyAsObject() : null;
    }

    /**
     * Accessor for the key object for the specified datastore-identity.
     * @param id The identity
     * @return The key object
     */
    public static Object getTargetKeyForDatastoreIdentity(Object id)
    {
        return id instanceof DatastoreId ? ((DatastoreId)id).getKeyAsObject() : null;
    }

    /**
     * Accessor for the type of the single field application-identity key given the single field identity type.
     * @param idType Single field identity type
     * @return key type
     */
    public static Class getKeyTypeForSingleFieldIdentityType(Class idType)
    {
        if (idType == null)
        {
            return null;
        }
        if (!IdentityUtils.isSingleFieldIdentityClass(idType.getName()))
        {
            return null;
        }

        if (ClassConstants.IDENTITY_SINGLEFIELD_LONG.isAssignableFrom(idType))
        {
            return Long.class;
        }
        else if (ClassConstants.IDENTITY_SINGLEFIELD_INT.isAssignableFrom(idType))
        {
            return Integer.class;
        }
        else if (ClassConstants.IDENTITY_SINGLEFIELD_SHORT.isAssignableFrom(idType))
        {
            return Short.class;
        }
        else if (ClassConstants.IDENTITY_SINGLEFIELD_BYTE.isAssignableFrom(idType))
        {
            return Byte.class;
        }
        else if (ClassConstants.IDENTITY_SINGLEFIELD_CHAR.isAssignableFrom(idType))
        {
            return Character.class;
        }
        else if (ClassConstants.IDENTITY_SINGLEFIELD_STRING.isAssignableFrom(idType))
        {
            return String.class;
        }
        else if (ClassConstants.IDENTITY_SINGLEFIELD_OBJECT.isAssignableFrom(idType))
        {
            return Object.class;
        }
        return null;
    }

    /**
     * Method to return a persistable form of the identity of a persistable object.
     * This can be used by datastores that don't use foreign keys and want to store the explicit class of the persistable object.
     * @param id The id
     * @return String form
     */
    public static String getPersistableIdentityForId(Object id)
    {
        if (id == null)
        {
            return null;
        }
        if (IdentityUtils.isSingleFieldIdentity(id))
        {
            return ((SingleFieldId)id).getTargetClassName() + ":" + ((SingleFieldId)id).getKeyAsObject();
        }
        return id.toString();
    }

    /**
     * Convenience method to find an object given a string form of its identity, and the metadata for the class (or a superclass).
     * @param persistableId The persistable id
     * @param cmd (Root) metadata for the class
     * @param ec Execution Context
     * @return The object
     */
    public static Object getObjectFromPersistableIdentity(String persistableId, AbstractClassMetaData cmd, ExecutionContext ec)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        Object id = null;
        if (cmd == null)
        {
            throw new NucleusException("Cannot get object from id=" + persistableId + " since class name was not supplied!");
        }
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            id = ec.getNucleusContext().getIdentityManager().getDatastoreId(persistableId);
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            if (cmd.usesSingleFieldIdentityClass())
            {
                String className = persistableId.substring(0, persistableId.indexOf(':'));
                cmd = ec.getMetaDataManager().getMetaDataForClass(className, clr);

                String idStr = persistableId.substring(persistableId.indexOf(':')+1);
                if (cmd.getObjectidClass().equals(ClassNameConstants.IDENTITY_SINGLEFIELD_OBJECT))
                {
                    // For ObjectId we need to pass "PkFieldType:{id}" - see ObjectId.toString()
                    // For all other SingleFieldId we pass "{id}" - see XXXId.toString()
                    int[] pkMemberPositions = cmd.getPKMemberPositions();
                    AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkMemberPositions[0]);
                    idStr = pkMmd.getTypeName() + ":" + idStr;
                }
                id = ec.getNucleusContext().getIdentityManager().getApplicationId(clr, cmd, idStr);
            }
            else
            {
                Class cls = clr.classForName(cmd.getFullClassName());
                id = ec.newObjectId(cls, persistableId);
            }
        }
        return ec.findObject(id, true, false, null);
    }

    /**
     * Method to return the object application identity for a row of the result set.
     * If the class isn't using application identity then returns null
     * @param ec Execution Context
     * @param cmd Metadata for the class
     * @param pcClass The class required
     * @param inheritanceCheck Whether need an inheritance check (may be for a subclass)
     * @param resultsFM FieldManager servicing the results
     * @return The identity (if found) or null (if either not sure of inheritance, or not known).
     */
    public static Object getApplicationIdentityForResultSetRow(ExecutionContext ec, AbstractClassMetaData cmd, 
            Class pcClass, boolean inheritanceCheck, FieldManager resultsFM)
    {
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            if (pcClass == null)
            {
                pcClass = ec.getClassLoaderResolver().classForName(cmd.getFullClassName());
            }
            ApiAdapter api = ec.getApiAdapter();
            int[] pkFieldNums = cmd.getPKMemberPositions();
            Object[] pkFieldValues = new Object[pkFieldNums.length];
            for (int i=0;i<pkFieldNums.length;i++)
            {
                AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[i]);
                if (pkMmd.getType() == int.class)
                {
                    pkFieldValues[i] = resultsFM.fetchIntField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == short.class)
                {
                    pkFieldValues[i] = resultsFM.fetchShortField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == long.class)
                {
                    pkFieldValues[i] = resultsFM.fetchLongField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == char.class)
                {
                    pkFieldValues[i] = resultsFM.fetchCharField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == boolean.class)
                {
                    pkFieldValues[i] = resultsFM.fetchBooleanField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == byte.class)
                {
                    pkFieldValues[i] = resultsFM.fetchByteField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == double.class)
                {
                    pkFieldValues[i] = resultsFM.fetchDoubleField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == float.class)
                {
                    pkFieldValues[i] = resultsFM.fetchFloatField(pkFieldNums[i]);
                }
                else if (pkMmd.getType() == String.class)
                {
                    pkFieldValues[i] = resultsFM.fetchStringField(pkFieldNums[i]);
                }
                else
                {
                    pkFieldValues[i] = resultsFM.fetchObjectField(pkFieldNums[i]);
                }
            }

            Class idClass = ec.getClassLoaderResolver().classForName(cmd.getObjectidClass());
            if (cmd.usesSingleFieldIdentityClass())
            {
                // Create SingleField identity with query key value
                Object id = ec.getNucleusContext().getIdentityManager().getSingleFieldId(idClass, pcClass, pkFieldValues[0]);
                if (inheritanceCheck)
                {
                    // Check if this identity exists in the cache(s)
                    if (ec.hasIdentityInCache(id))
                    {
                        return id;
                    }

                    // Check if this id for any known subclasses is in the cache to save searching
                    String[] subclasses = ec.getMetaDataManager().getSubclassesForClass(pcClass.getName(), true);
                    if (subclasses != null)
                    {
                        for (int i=0;i<subclasses.length;i++)
                        {
                            Object subid = ec.getNucleusContext().getIdentityManager().getSingleFieldId(idClass, 
                                ec.getClassLoaderResolver().classForName(subclasses[i]), IdentityUtils.getTargetKeyForSingleFieldIdentity(id));
                            if (ec.hasIdentityInCache(subid))
                            {
                                return subid;
                            }
                        }
                    }

                    // Check the inheritance with the store manager (may involve a trip to the datastore)
                    String className = ec.getStoreManager().getClassNameForObjectID(id, ec.getClassLoaderResolver(), ec);
                    return ec.getNucleusContext().getIdentityManager().getSingleFieldId(idClass, ec.getClassLoaderResolver().classForName(className), pkFieldValues[0]);
                }
                return id;
            }

            // Create user-defined PK class with PK field values
            try
            {
                // All user-defined PK classes have a default constructor
                Object id = idClass.newInstance();

                for (int i=0;i<pkFieldNums.length;i++)
                {
                    AbstractMemberMetaData pkMmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[i]);
                    Object value = pkFieldValues[i];
                    if (api.isPersistable(value))
                    {
                        // CompoundIdentity, so use id
                        value = api.getIdForObject(value);
                    }
                    if (pkMmd instanceof FieldMetaData)
                    {
                        // Set the field directly (assumed to be public)
                        Field pkField = ClassUtils.getFieldForClass(idClass, pkMmd.getName());
                        pkField.set(id, value);
                    }
                    else
                    {
                        // Use the setter
                        Method pkMethod = ClassUtils.getSetterMethodForClass(idClass, pkMmd.getName(), pkMmd.getType());
                        pkMethod.invoke(id, value);
                    }
                }
                return id;
            }
            catch (Exception e)
            {
                return null;
            }
        }
        return null;
    }

    /**
     * Convenience method that interrogates a user-supplied object identity and returns the value of a particular member
     * in that id. A user-supplied PK has to provide either public/package/protected fields with the same names as the
     * owning class, or getters for bean properties of the same name as the class members.
     * @param id The (user-defined) identity
     * @param pkMmd Metadata for the member that we require the value for
     * @return The value for this member in the id
     */
    public static Object getValueForMemberInId(Object id, AbstractMemberMetaData pkMmd)
    {
        if (id == null || pkMmd == null || !pkMmd.isPrimaryKey())
        {
            return null;
        }

        String memberName = pkMmd.getName();
        Field fld = ClassUtils.getFieldForClass(id.getClass(), memberName);
        if (fld != null && !Modifier.isPrivate(fld.getModifiers()))
        {
            try
            {
                return fld.get(id);
            }
            catch (Exception e)
            {
            }
        }

        Method getter = ClassUtils.getGetterMethodForClass(id.getClass(), memberName);
        if (getter != null && !Modifier.isPrivate(getter.getModifiers()))
        {
            try
            {
                return getter.invoke(id);
            }
            catch (Exception e)
            {
            }
        }

        return null;
    }

    /**
     * Convenience method to find an object given a string form of its identity, and the metadata for the class (or a superclass).
     * <b>Developers should move to using "persistable identity" and method getObjectFromPersistableIdentity()</b>.
     * @param idStr The id string
     * @param cmd Metadata for the class
     * @param ec Execution Context
     * @param checkInheritance Whether to check the inheritance level of this object
     * @return The object
     */
    public static Object getObjectFromIdString(String idStr, AbstractClassMetaData cmd, ExecutionContext ec, boolean checkInheritance)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        Object id = null;
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            id = ec.getNucleusContext().getIdentityManager().getDatastoreId(idStr);
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            if (cmd.usesSingleFieldIdentityClass())
            {
                id = ec.getNucleusContext().getIdentityManager().getApplicationId(clr, cmd, idStr);
            }
            else
            {
                Class cls = clr.classForName(cmd.getFullClassName());
                id = ec.newObjectId(cls, idStr);
            }
        }
        return ec.findObject(id, true, checkInheritance, null);
    }

    /**
     * Convenience method to find an object given a string form of its identity, and the metadata for the member.
     * @param idStr The id string
     * @param mmd Metadata for the member
     * @param fieldRole Role of this field (see org.datanucleus.metadata.FieldRole)
     * @param ec Execution Context
     * @param checkInheritance Whether to check the inheritance level of this object
     * @return The object
     */
    public static Object getObjectFromIdString(String idStr, AbstractMemberMetaData mmd, FieldRole fieldRole, ExecutionContext ec, boolean checkInheritance)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        if (fieldRole == FieldRole.ROLE_FIELD && mmd.getType().isInterface())
        {
            // Interface field, so use information about possible implementation types
            String[] implNames = MetaDataUtils.getInstance().getImplementationNamesForReferenceField(mmd, fieldRole, 
                clr, ec.getMetaDataManager());
            if (implNames == null || implNames.length == 0)
            {
                // No known implementations so no way of knowing the type
                return null;
            }

            AbstractClassMetaData cmd = ec.getMetaDataManager().getMetaDataForClass(implNames[0], clr);
            if (cmd.getIdentityType() == IdentityType.DATASTORE)
            {
                Object id = ec.getNucleusContext().getIdentityManager().getDatastoreId(idStr);
                return ec.findObject(id, true, checkInheritance, null);
            }
            else if (cmd.getIdentityType() == IdentityType.APPLICATION)
            {
                Object id = null;
                for (int i=0;i<implNames.length;i++)
                {
                    if (i != 0)
                    {
                        cmd = ec.getMetaDataManager().getMetaDataForClass(implNames[i], clr);
                    }

                    if (cmd.usesSingleFieldIdentityClass())
                    {
                        id = ec.getNucleusContext().getIdentityManager().getApplicationId(clr, cmd, idStr);
                    }
                    else
                    {
                        id = ec.newObjectId(clr.classForName(cmd.getFullClassName()), idStr);
                    }
                    try
                    {
                        return ec.findObject(id, true, checkInheritance, null);
                    }
                    catch (NucleusObjectNotFoundException nonfe)
                    {
                        // Presumably not this implementation
                    }
                }
            }
        }
        // TODO Allow for collection<interface>, map<interface, ?>, map<?, interface>, interface[]
        else
        {
            AbstractClassMetaData cmd = null;
            if (fieldRole == FieldRole.ROLE_COLLECTION_ELEMENT)
            {
                cmd = mmd.getCollection().getElementClassMetaData(clr);
            }
            else if (fieldRole == FieldRole.ROLE_ARRAY_ELEMENT)
            {
                cmd = mmd.getArray().getElementClassMetaData(clr);
            }
            else if (fieldRole == FieldRole.ROLE_MAP_KEY)
            {
                cmd = mmd.getMap().getKeyClassMetaData(clr);
            }
            else if (fieldRole == FieldRole.ROLE_MAP_KEY)
            {
                cmd = mmd.getMap().getKeyClassMetaData(clr);
            }
            else
            {
                cmd = ec.getMetaDataManager().getMetaDataForClass(mmd.getType(), clr);
            }

            Object id = null;
            if (cmd.getIdentityType() == IdentityType.DATASTORE)
            {
                id = ec.getNucleusContext().getIdentityManager().getDatastoreId(idStr);
            }
            else if (cmd.getIdentityType() == IdentityType.APPLICATION)
            {
                if (cmd.usesSingleFieldIdentityClass())
                {
                    // Single-Field identity doesn't have the class name in the string, so cater for the root being abstract
                    Class cls = clr.classForName(cmd.getFullClassName());
                    if (Modifier.isAbstract(cls.getModifiers()))
                    {
                        // Try to find a non-abstract subclass candidate
                        // TODO Allow for all possibilities rather than just first non-abstract branch
                        String[] subclasses = ec.getMetaDataManager().getSubclassesForClass(cmd.getFullClassName(), false);
                        if (subclasses != null)
                        {
                            for (int i=0;i<subclasses.length;i++)
                            {
                                cls = clr.classForName(subclasses[i]);
                                if (!Modifier.isAbstract(cls.getModifiers()))
                                {
                                    cmd = ec.getMetaDataManager().getMetaDataForClass(cls, clr);
                                    break;
                                }
                            }
                        }
                    }

                    id = ec.getNucleusContext().getIdentityManager().getApplicationId(clr, cmd, idStr);
                }
                else
                {
                    Class cls = clr.classForName(cmd.getFullClassName());
                    id = ec.newObjectId(cls, idStr);
                }
            }
            return ec.findObject(id, true, checkInheritance, null);
        }
        return null;
    }
}