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
package org.datanucleus.identity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.enhancer.EnhancementHelper;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager for identity operations.
 */
public class IdentityManagerImpl implements IdentityManager
{
    /** Default DatastoreId implementation used by DataNucleus. */
    protected Class datastoreIdClass = null;

    /** Identity string translator (if any). */
    protected IdentityStringTranslator idStringTranslator = null;

    /** Identity key translator (if any). */
    protected IdentityKeyTranslator idKeyTranslator = null;

    /** Cache of id class Constructor, keyed by string of the type+args. */
    private Map<String, Constructor<?>> constructorCache = new ConcurrentHashMap<String, Constructor<?>>();

    public IdentityManagerImpl(PersistenceNucleusContext nucCtx)
    {
        // Datastore Identity type
        String dsidName = nucCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_DATASTORE_IDENTITY_TYPE);
        String datastoreIdentityClassName = nucCtx.getPluginManager().getAttributeValueForExtension("org.datanucleus.store_datastoreidentity", "name", dsidName, "class-name");
        if (datastoreIdentityClassName == null)
        {
            // User has specified a datastore_identity plugin that has not registered
            throw new NucleusUserException(Localiser.msg("002001", dsidName)).setFatal();
        }
        ClassLoaderResolver clr = nucCtx.getClassLoaderResolver(null);
        try
        {
            datastoreIdClass = clr.classForName(datastoreIdentityClassName, org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);
        }
        catch (ClassNotResolvedException cnre)
        {
            throw new NucleusUserException(Localiser.msg("002002", dsidName, datastoreIdentityClassName)).setFatal();
        }

        // Identity key translation
        String keyTranslatorType = nucCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_IDENTITY_KEY_TRANSLATOR_TYPE);
        if (keyTranslatorType != null)
        {
            try
            {
                idKeyTranslator = (IdentityKeyTranslator)nucCtx.getPluginManager().createExecutableExtension(
                    "org.datanucleus.identity_key_translator", "name", keyTranslatorType, "class-name", null, null);
            }
            catch (Exception e)
            {
                // User has specified a identity key translator plugin that has not registered
                throw new NucleusUserException(Localiser.msg("002001", keyTranslatorType)).setFatal();
            }
        }

        // Identity string translation
        String stringTranslatorType = nucCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_IDENTITY_STRING_TRANSLATOR_TYPE);
        if (stringTranslatorType != null)
        {
            try
            {
                idStringTranslator = (IdentityStringTranslator)nucCtx.getPluginManager().createExecutableExtension(
                    "org.datanucleus.identity_string_translator", "name", stringTranslatorType, "class-name", null, null);
            }
            catch (Exception e)
            {
                // User has specified a string identity translator plugin that has not registered
                throw new NucleusUserException(Localiser.msg("002001", stringTranslatorType)).setFatal();
            }
        }
    }

    protected String getConstructorNameForCache(Class type, Class[] ctrArgTypes)
    {
        StringBuilder name = new StringBuilder(type.getName());
        if (ctrArgTypes != null)
        {
            for (int i=0;i<ctrArgTypes.length; i++)
            {
                name.append("-").append(ctrArgTypes[i].getName());
            }
        }
        return name.toString();
    }

    public Class getDatastoreIdClass()
    {
        return datastoreIdClass;
    }

    public IdentityStringTranslator getIdentityStringTranslator()
    {
        return idStringTranslator;
    }

    public IdentityKeyTranslator getIdentityKeyTranslator()
    {
        return idKeyTranslator;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.identity.IdentityManager#getDatastoreId(java.lang.String, java.lang.Object)
     */
    @Override
    public DatastoreId getDatastoreId(String className, Object value)
    {
        if (datastoreIdClass == ClassConstants.IDENTITY_DATASTORE_IMPL)
        {
            // Hardcoded for performance
            return new DatastoreIdImpl(className, value);
        }

        // Others are pluggable
        try
        {
            Class[] ctrArgTypes = new Class[] {String.class, Object.class};
            String ctrName = getConstructorNameForCache(datastoreIdClass, ctrArgTypes);
            Constructor ctr = constructorCache.get(ctrName);
            if (ctr == null)
            {
                ctr = datastoreIdClass.getConstructor(ctrArgTypes);
                constructorCache.put(ctrName, ctr);
            }
            return (DatastoreId)ctr.newInstance(new Object[] {className, value});
        }
        catch (Exception e)
        {
            // TODO Localise this
            throw new NucleusException("Error encountered while creating datastore instance for class \"" + className + "\"", e);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.identity.IdentityManager#getDatastoreId(long)
     */
    @Override
    public DatastoreId getDatastoreId(long value)
    {
        if (datastoreIdClass == DatastoreUniqueLongId.class)
        {
            // Hardcoded for performance
            return new DatastoreUniqueLongId(value);
        }

        // Others are pluggable
        try
        {
            Class[] ctrArgTypes = new Class[] {Long.class};
            String ctrName = getConstructorNameForCache(datastoreIdClass, ctrArgTypes);
            Constructor ctr = constructorCache.get(ctrName);
            if (ctr == null)
            {
                ctr = datastoreIdClass.getConstructor(ctrArgTypes);
                constructorCache.put(ctrName, ctr);
            }
            return (DatastoreId)ctr.newInstance(new Object[] {Long.valueOf(value)});
        }
        catch (Exception e)
        {
            // TODO Localise this
            throw new NucleusException("Error encountered while creating datastore instance for unique value \"" + value + "\"", e);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.identity.IdentityManager#getDatastoreId(java.lang.String)
     */
    @Override
    public DatastoreId getDatastoreId(String idString)
    {
        if (datastoreIdClass == ClassConstants.IDENTITY_DATASTORE_IMPL)
        {
            // Hardcoded for performance
            return new DatastoreIdImpl(idString);
        }

        // Others are pluggable
        try
        {
            Class[] ctrArgTypes = new Class[] {String.class};
            String ctrName = getConstructorNameForCache(datastoreIdClass, ctrArgTypes);
            Constructor ctr = constructorCache.get(ctrName);
            if (ctr == null)
            {
                ctr = datastoreIdClass.getConstructor(ctrArgTypes);
                constructorCache.put(ctrName, ctr);
            }
            return (DatastoreId)ctr.newInstance(new Object[] {idString});
        }
        catch (Exception e)
        {
            // TODO Localise this
            throw new NucleusException("Error encountered while creating datastore instance for string \"" + idString + "\"", e);
        }
    }

    /**
     * Utility to create a new SingleFieldId using reflection when you know the type of the Persistable, and also which SingleFieldId type, and the value of the key.
     * @param idType Type of SingleFieldId
     * @param pcType Type of the Persistable
     * @param key The value for the identity (the Long, or Int, or ... etc).
     * @return Single field identity
     * @throws NucleusException if invalid input is received
     */
    public SingleFieldId getSingleFieldId(Class idType, Class pcType, Object key)
    {
        if (idType == null)
        {
            throw new NucleusException(Localiser.msg("029001", pcType)).setFatal();
        }
        if (pcType == null)
        {
            throw new NucleusException(Localiser.msg("029000", idType)).setFatal();
        }
        if (key == null)
        {
            throw new NucleusException(Localiser.msg("029003", idType, pcType)).setFatal();
        }
        if (!SingleFieldId.class.isAssignableFrom(idType))
        {
            throw new NucleusException(Localiser.msg("029002", idType.getName(), pcType.getName())).setFatal();
        }

        SingleFieldId id = null;
        Class keyType = null;
        if (idType == ClassConstants.IDENTITY_SINGLEFIELD_LONG)
        {
            keyType = Long.class;
            if (!(key instanceof Long))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Long")).setFatal();
            }
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_INT)
        {
            keyType = Integer.class;
            if (!(key instanceof Integer))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Integer")).setFatal();
            }
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_STRING)
        {
            keyType = String.class;
            if (!(key instanceof String))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "String")).setFatal();
            }
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_BYTE)
        {
            keyType = Byte.class;
            if (!(key instanceof Byte))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Byte")).setFatal();
            }
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_SHORT)
        {
            keyType = Short.class;
            if (!(key instanceof Short))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Short")).setFatal();
            }
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_CHAR)
        {
            keyType = Character.class;
            if (!(key instanceof Character))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Character")).setFatal();
            }
        }
        else
        {
            // ObjectIdentity
            keyType = Object.class;
        }

        try
        {
            Class[] ctrArgs = new Class[] {Class.class, keyType};
            String ctrName = getConstructorNameForCache(idType, ctrArgs);
            Constructor ctr = constructorCache.get(ctrName);
            if (ctr == null)
            {
                ctr = idType.getConstructor(ctrArgs);
                constructorCache.put(ctrName, ctr);
            }
            id = (SingleFieldId)ctr.newInstance(new Object[] {pcType, key});
        }
        catch (Exception e)
        {
            NucleusLogger.PERSISTENCE.error("Error encountered while creating SingleFieldIdentity instance of type \"" + idType.getName() + "\"", e);
            return null;
        }

        return id;
    }

    /**
     * Utility to create a new application identity when you know the metadata for the target class, and the toString() output of the identity.
     * @param clr ClassLoader resolver
     * @param acmd MetaData for the target class
     * @param keyToString String form of the key
     * @return The identity
     */
    public Object getApplicationId(ClassLoaderResolver clr, AbstractClassMetaData acmd, String keyToString)
    {
        if (acmd.getIdentityType() != IdentityType.APPLICATION)
        {
            // TODO Localise this
            throw new NucleusException("This class (" + acmd.getFullClassName() + ") doesn't use application-identity!");
        }

        Class targetClass = clr.classForName(acmd.getFullClassName());
        Class idType = clr.classForName(acmd.getObjectidClass());

        if (acmd.usesSingleFieldIdentityClass())
        {
            try
            {
                Class[] ctrArgTypes;
                if (ClassConstants.IDENTITY_SINGLEFIELD_OBJECT.isAssignableFrom(idType))
                {
                    ctrArgTypes = new Class[] {Class.class, Object.class};
                }
                else
                {
                    ctrArgTypes = new Class[] {Class.class, String.class};
                }
                String ctrName = getConstructorNameForCache(idType, ctrArgTypes);
                Constructor ctr = constructorCache.get(ctrName);
                if (ctr == null)
                {
                    ctr = idType.getConstructor(ctrArgTypes);
                    constructorCache.put(ctrName, ctr);
                }
                return ctr.newInstance(new Object[] {targetClass, keyToString});
            }
            catch (Exception e)
            {
                // TODO Localise this
                throw new NucleusException("Error encountered while creating single-field identity instance with key \"" + keyToString + "\"", e);
            }
        }

        if (Modifier.isAbstract(targetClass.getModifiers()) && acmd.getObjectidClass() != null) 
        {
            try
            {
                Class type = clr.classForName(acmd.getObjectidClass());
                Class[] ctrArgTypes = new Class[] {String.class};
                String ctrName = getConstructorNameForCache(type, ctrArgTypes);
                Constructor ctr = constructorCache.get(ctrName);
                if (ctr == null)
                {
                    ctr = type.getConstructor(ctrArgTypes);
                    constructorCache.put(ctrName, ctr);
                }
                return ctr.newInstance(new Object[] {keyToString});
            }
            catch (Exception e) 
            {
                String msg = Localiser.msg("010030", acmd.getObjectidClass(), acmd.getFullClassName());
                NucleusLogger.PERSISTENCE.error(msg, e);
                throw new NucleusUserException(msg);
            }
        }

        clr.classForName(targetClass.getName(), true);
        return EnhancementHelper.getInstance().newObjectIdInstance(targetClass, keyToString);
    }

    /**
     * Method to create a new (application) identity for the passed object with the supplied MetaData (when using APPLICATION identity).
     * @param pc The persistable object
     * @param cmd Metadata for the persistable object
     * @return The new identity object
     */
    public Object getApplicationId(Object pc, AbstractClassMetaData cmd)
    {
        if (pc == null || cmd == null)
        {
            return null;
        }

        try
        {
            Object id = ((Persistable)pc).dnNewObjectIdInstance();
            if (!cmd.usesSingleFieldIdentityClass())
            {
                ((Persistable)pc).dnCopyKeyFieldsToObjectId(id);
            }

            if (!(id instanceof SingleFieldId))
            {
                // DataNucleus feature to allow user-defined id classes to have a field "targetClassName" storing the class name of the object being represented
                Field classField = ClassUtils.getFieldForClass(id.getClass(), "targetClassName");
                if (classField != null)
                {
                    try
                    {
                        classField.set(id, cmd.getFullClassName());
                    }
                    catch (IllegalArgumentException | IllegalAccessException e)
                    {
                    }
                }
            }

            return id;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Method to return a new object identity for the specified class, and key (possibly toString() output).
     * @param cls Persistable class
     * @param key form of the object id
     * @return The object identity
     */
    public Object getApplicationId(Class cls, Object key)
    {
        return EnhancementHelper.getInstance().newObjectIdInstance(cls, key);
    }
}