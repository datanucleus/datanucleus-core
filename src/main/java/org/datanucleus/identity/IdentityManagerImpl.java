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
    private static final Class[] CTR_CLASS_OBJECT_ARG_TYPES = new Class[] {Class.class, Object.class};
    private static final Class[] CTR_CLASS_STRING_ARG_TYPES = new Class[] {Class.class, ClassConstants.JAVA_LANG_STRING};
    private static final Class[] CTR_STRING_OBJECT_ARG_TYPES = new Class[] {ClassConstants.JAVA_LANG_STRING, Object.class};
    private static final Class[] CTR_STRING_ARG_TYPES = new Class[] {ClassConstants.JAVA_LANG_STRING};
    private static final Class[] CTR_LONG_ARG_TYPES = new Class[] {ClassConstants.JAVA_LANG_LONG};

    /** Default DatastoreId implementation used by DataNucleus. */
    protected Class datastoreIdClass = null;

    /** Identity string translator (if any). */
    protected IdentityStringTranslator idStringTranslator = null;

    /** Identity key translator (if any). */
    protected IdentityKeyTranslator idKeyTranslator = null;

    /** Cache of id class Constructor, keyed by string of the type+args. */
    private Map<String, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

    public IdentityManagerImpl(PersistenceNucleusContext nucCtx)
    {
        // Datastore Identity type
        String dsidName = nucCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_DATASTORE_IDENTITY_TYPE);
        String dsidNameLower = dsidName.toLowerCase();
        if ("datanucleus".equals(dsidNameLower))
        {
            datastoreIdClass = DatastoreIdImpl.class;
        }
        else if ("kodo".equals(dsidNameLower))
        {
            datastoreIdClass = DatastoreIdImplKodo.class;
        }
        else if ("xcalia".equals(dsidNameLower))
        {
            datastoreIdClass = DatastoreIdImplXcalia.class;
        }
        else if ("unique".equals(dsidNameLower))
        {
            datastoreIdClass = DatastoreUniqueLongId.class;
        }
        else
        {
            // Fallback to plugin mechanism
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
            if ("xcalia".equals(stringTranslatorType.toLowerCase()))
            {
                idStringTranslator = new XcaliaIdentityStringTranslator();
            }
            else
            {
                // Fallback to the plugin mechanism
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
    }

    protected String getConstructorNameForCache(Class type, Class[] ctrArgTypes)
    {
        StringBuilder name = new StringBuilder(type.getName());
        for (int i=0;i<ctrArgTypes.length; i++)
        {
            name.append("-").append(ctrArgTypes[i].getName());
        }
        return name.toString();
    }

    @Override
    public Class getDatastoreIdClass()
    {
        return datastoreIdClass;
    }

    @Override
    public IdentityStringTranslator getIdentityStringTranslator()
    {
        return idStringTranslator;
    }

    @Override
    public IdentityKeyTranslator getIdentityKeyTranslator()
    {
        return idKeyTranslator;
    }

    @Override
    public DatastoreId getDatastoreId(String className, Object value)
    {
        // Hardcoded for performance
        if (datastoreIdClass == ClassConstants.IDENTITY_DATASTORE_IMPL)
        {
            return new DatastoreIdImpl(className, value);
        }
        else if (datastoreIdClass == DatastoreIdImplKodo.class)
        {
            return new DatastoreIdImplKodo(className, value);
        }
        else if (datastoreIdClass == DatastoreIdImplXcalia.class)
        {
            return new DatastoreIdImplXcalia(className, value);
        }

        // Pluggable type
        try
        {
            Class[] ctrArgTypes = CTR_STRING_OBJECT_ARG_TYPES;
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

    @Override
    public DatastoreId getDatastoreId(long value)
    {
        if (datastoreIdClass == DatastoreUniqueLongId.class)
        {
            // Hardcoded for performance
            return new DatastoreUniqueLongId(value);
        }

        // Pluggable type
        try
        {
            Class[] ctrArgTypes = CTR_LONG_ARG_TYPES;
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

    @Override
    public DatastoreId getDatastoreId(String idString)
    {
        // Hardcoded for performance
        if (datastoreIdClass == ClassConstants.IDENTITY_DATASTORE_IMPL)
        {
            return new DatastoreIdImpl(idString);
        }
        else if (datastoreIdClass == DatastoreIdImplKodo.class)
        {
            return new DatastoreIdImplKodo(idString);
        }
        else if (datastoreIdClass == DatastoreIdImplXcalia.class)
        {
            return new DatastoreIdImplXcalia(idString);
        }

        // Pluggable type
        try
        {
            Class[] ctrArgTypes = CTR_STRING_ARG_TYPES;
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

    @Override
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

        if (idType == ClassConstants.IDENTITY_SINGLEFIELD_LONG)
        {
            if (!(key instanceof Long))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Long")).setFatal();
            }
            return new LongId(pcType, (Long)key);
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_INT)
        {
            if (!(key instanceof Integer))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Integer")).setFatal();
            }
            return new IntId(pcType, (Integer)key);
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_STRING)
        {
            if (!(key instanceof String))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "String")).setFatal();
            }
            return new StringId(pcType, (String)key);
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_BYTE)
        {
            if (!(key instanceof Byte))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Byte")).setFatal();
            }
            return new ByteId(pcType, (Byte)key);
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_SHORT)
        {
            if (!(key instanceof Short))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Short")).setFatal();
            }
            return new ShortId(pcType, (Short)key);
        }
        else if (idType == ClassConstants.IDENTITY_SINGLEFIELD_CHAR)
        {
            if (!(key instanceof Character))
            {
                throw new NucleusException(Localiser.msg("029004", idType.getName(), pcType.getName(), key.getClass().getName(), "Character")).setFatal();
            }
            return new CharId(pcType, (Character)key);
        }
        else
        {
            // ObjectIdentity
            return new ObjectId(pcType, key);
        }
    }

    @Override
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
                    ctrArgTypes = CTR_CLASS_OBJECT_ARG_TYPES;
                }
                else
                {
                    ctrArgTypes = CTR_CLASS_STRING_ARG_TYPES;
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
                Class[] ctrArgTypes = CTR_STRING_ARG_TYPES;
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

    @Override
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
                Field classField = ClassUtils.getFieldForClass(id.getClass(), IdentityManager.IDENTITY_CLASS_TARGET_CLASS_NAME_FIELD);
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

    @Override
    public Object getApplicationId(Class cls, Object key)
    {
        return EnhancementHelper.getInstance().newObjectIdInstance(cls, key);
    }
}