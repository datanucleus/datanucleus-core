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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldRole;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.store.fieldmanager.FieldManager;
import org.datanucleus.util.ClassUtils;

/**
 * Series of utilities for handling identities of objects.
 */
public class IdentityUtils
{
    /**
     * Simple method to return the class name that the provided id represents. 
     * If this is a datastore identity (OID) or single-field identity then returns the class name.
     * Otherwise returns null. Does no inheritance checking.
     * @param api The API adapter
     * @param id The identity
     * @return Class name for the identity if easily determinable
     */
    public static String getClassNameForIdentitySimple(ApiAdapter api, Object id)
    {
        if (id instanceof OID)
        {
            // Object is an OID
            return ((OID)id).getPcClass();
        }
        else if (api.isSingleFieldIdentity(id))
        {
            // Using SingleFieldIdentity so can assume that object is of the target class
            return api.getTargetClassNameForSingleFieldIdentity(id);
        }
        // Must be user-specified identity so just return
        return null;
    }

    /**
     * Convenience method to return the identity as a String.
     * Typically outputs the toString() form of the identity object however with SingleFieldIdentity
     * it outputs the class+key since SingleFieldIdentity just return the key.
     * @param id The id
     * @return String form
     */
    public static String getIdentityAsString(ApiAdapter api, Object id)
    {
        if (id == null)
        {
            return null;
        }
        if (api.isSingleFieldIdentity(id))
        {
            return api.getTargetClassNameForSingleFieldIdentity(id) + ":" +
                api.getTargetKeyForSingleFieldIdentity(id);
        }
        else
        {
            return id.toString();
        }
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
                Object id = api.getNewSingleFieldIdentity(idClass, pcClass, pkFieldValues[0]);
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
                            Object subid = api.getNewSingleFieldIdentity(idClass, 
                                ec.getClassLoaderResolver().classForName(subclasses[i]), 
                                api.getTargetKeyForSingleFieldIdentity(id));
                            if (ec.hasIdentityInCache(subid))
                            {
                                return subid;
                            }
                        }
                    }

                    // Check the inheritance with the store manager (may involve a trip to the datastore)
                    String className = ec.getStoreManager().getClassNameForObjectID(id, ec.getClassLoaderResolver(), ec);
                    return api.getNewSingleFieldIdentity(idClass, 
                        ec.getClassLoaderResolver().classForName(className), pkFieldValues[0]);
                }
                return id;
            }
            else
            {
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
                return getter.invoke(id, null);
            }
            catch (Exception e)
            {
            }
        }

        return null;
    }

    /**
     * Convenience method to find an object given a string form of its identity, and the metadata for the
     * class (or a superclass).
     * @param idStr The id string
     * @param cmd Metadata for the class
     * @param ec Execution Context
     * @param checkInheritance Whether to check the inheritance level of this object
     * @return The object
     */
    public static Object getObjectFromIdString(String idStr, AbstractClassMetaData cmd, ExecutionContext ec,
            boolean checkInheritance)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();
        Object id = null;
        if (cmd.getIdentityType() == IdentityType.DATASTORE)
        {
            id = OIDFactory.getInstance(ec.getNucleusContext(), idStr);
        }
        else if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            if (cmd.usesSingleFieldIdentityClass())
            {
                id = ec.getApiAdapter().getNewApplicationIdentityObjectId(clr, cmd, idStr);
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
    public static Object getObjectFromIdString(String idStr, AbstractMemberMetaData mmd, int fieldRole, 
            ExecutionContext ec, boolean checkInheritance)
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
                Object id = OIDFactory.getInstance(ec.getNucleusContext(), idStr);
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
                        id = ec.getApiAdapter().getNewApplicationIdentityObjectId(clr, cmd, idStr);
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
                cmd = mmd.getCollection().getElementClassMetaData(clr, ec.getMetaDataManager());
            }
            else if (fieldRole == FieldRole.ROLE_ARRAY_ELEMENT)
            {
                cmd = mmd.getArray().getElementClassMetaData(clr, ec.getMetaDataManager());
            }
            else if (fieldRole == FieldRole.ROLE_MAP_KEY)
            {
                cmd = mmd.getMap().getKeyClassMetaData(clr, ec.getMetaDataManager());
            }
            else if (fieldRole == FieldRole.ROLE_MAP_KEY)
            {
                cmd = mmd.getMap().getKeyClassMetaData(clr, ec.getMetaDataManager());
            }
            else
            {
                cmd = ec.getMetaDataManager().getMetaDataForClass(mmd.getType(), clr);
            }

            Object id = null;
            if (cmd.getIdentityType() == IdentityType.DATASTORE)
            {
                id = OIDFactory.getInstance(ec.getNucleusContext(), idStr);
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

                    id = ec.getApiAdapter().getNewApplicationIdentityObjectId(clr, cmd, idStr);
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