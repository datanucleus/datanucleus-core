/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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
2003 Andy Jefferson - coding standards
2004 Andy Jefferson - fixed creation of repository when block failed
2004 Andy Jefferson - removed MetaData requirement
2006 Andy Jefferson - changed to hold the generator rather than creating one each time
2006 Andy Jefferson - rewritten to handle the creation of ValueGenerator nad lookup.
    ...
**********************************************************************/
package org.datanucleus.store.valuegenerator;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager for the creation of ValueGenerators.
 * ValueGenerators are of two primary types.
 * <ul>
 * <li><b>unique</b> : apply to any datastore, and generate unique values. For example, UUID, which generates the values in Java space.</li>
 * <li><b>datastore</b> : apply to a particular datastore, and member. For example, an RDBMS SEQUENCE.</li>
 * </ul>
 * Any unique generators are loaded at initialisation. Any datastore generators are loaded when required.
 * All generators are cached once created, and can be looked up by the member "key" that they are for.
 * 
 * <h3>Member Key</h3>
 * The member "key" is either the fully-qualified member name (e.g "mydomain.MyClass.myField") that is having its values generated, or
 * is for a (surrogate) datastore id member (e.g "mydomain.MyClass (datastore-id)").
 * All unique generators can also be looked up by the strategy name (since there is one instance of that generator per strategy.
 */
public class ValueGenerationManagerImpl implements ValueGenerationManager
{
    protected final StoreManager storeMgr;

    /** Map of ValueGenerators, keyed by the member key ("{class}.{field}", or "{class} + (datastore-id)"). */
    protected Map<String, ValueGenerator> generatorsByMemberKey = new ConcurrentHashMap<>();

    /** Map of "unique" ValueGenerators, keyed by their strategy name. */
    protected Map<String, ValueGenerator> uniqueGeneratorsByName = new ConcurrentHashMap<>();

    /**
     * Constructor.
     * @param storeMgr Store Manager
     */
    public ValueGenerationManagerImpl(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;

        // Load up all built-in generators
        ValueGenerator generator = new TimestampGenerator("timestamp", null);
        uniqueGeneratorsByName.put("timestamp", generator);

        generator = new TimestampValueGenerator("timestamp-value", null);
        uniqueGeneratorsByName.put("timestamp-value", generator);

        generator = new AUIDGenerator("timestamp-value", null);
        uniqueGeneratorsByName.put("auid", generator);

        generator = new UUIDGenerator("uuid", null);
        uniqueGeneratorsByName.put("uuid", generator);

        generator = new UUIDObjectGenerator("uuid-object", null);
        uniqueGeneratorsByName.put("uuid-object", generator);

        generator = new UUIDHexGenerator("uuid-hex", null);
        uniqueGeneratorsByName.put("uuid-hex", generator);

        generator = new UUIDStringGenerator("uuid-string", null);
        uniqueGeneratorsByName.put("uuid-string", generator);

        // Load up any unique generators from the plugin mechanism
        try
        {
            ConfigurationElement[] elems = storeMgr.getNucleusContext().getPluginManager().getConfigurationElementsForExtension("org.datanucleus.store_valuegenerator",
                "unique", "true");
            if (elems != null)
            {
                for (ConfigurationElement elem : elems)
                {
                    // Assumed to not take any properties
                    generator = (ValueGenerator)storeMgr.getNucleusContext().getPluginManager().createExecutableExtension("org.datanucleus.store_valuegenerator", 
                        new String[] {"name", "unique"}, new String[] {elem.getName(), "true"},
                        "class-name", new Class[] {String.class, Properties.class}, new Object[] {elem.getName(), null});
                    uniqueGeneratorsByName.put(elem.getName(), generator);
                }
            }
        }
        catch (Exception e)
        {
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#clear()
     */
    @Override
    public void clear()
    {
        generatorsByMemberKey.clear();
        uniqueGeneratorsByName.clear();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#getValueGeneratorForMemberKey(java.lang.String)
     */
    @Override
    public ValueGenerator getValueGeneratorForMemberKey(String memberKey)
    {
        return generatorsByMemberKey.get(memberKey);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#registerValueGeneratorForMemberKey(java.lang.String, org.datanucleus.store.valuegenerator.ValueGenerator)
     */
    @Override
    public void registerValueGeneratorForMemberKey(String memberKey, ValueGenerator generator)
    {
        this.generatorsByMemberKey.put(memberKey, generator);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#getUniqueValueGeneratorByName(java.lang.String)
     */
    @Override
    public ValueGenerator getUniqueValueGeneratorByName(String name)
    {
        return uniqueGeneratorsByName.get(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#getMemberKey(org.datanucleus.metadata.AbstractClassMetaData, int)
     */
    @Override
    public String getMemberKey(AbstractClassMetaData cmd, int fieldNumber)
    {
        if (fieldNumber < 0)
        {
            return cmd.getFullClassName() + " (datastore id)";
        }

        AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(fieldNumber);
        return mmd.getFullFieldName();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#getTypeForValueGeneratorForMember(java.lang.String, java.lang.String)
     */
    @Override
    public Class getTypeForValueGeneratorForMember(String strategyName, String memberKey)
    {
        ValueGenerator generator = generatorsByMemberKey.get(memberKey);
        if (generator == null)
        {
            // No generator assigned to this member key, so check for a unique generator
            generator = uniqueGeneratorsByName.get(strategyName);
            if (generator == null)
            {
                // Try to create a datastore generator with this strategy
                try
                {
                    generator = (AbstractGenerator)storeMgr.getNucleusContext().getPluginManager().createExecutableExtension(
                        "org.datanucleus.store_valuegenerator",
                        new String[] {"name", "datastore"}, new String[] {strategyName, storeMgr.getStoreManagerKey()},
                        "class-name", new Class[] {String.class, Properties.class}, new Object[] {memberKey, null});
                }
                catch (Exception e)
                {
                }
            }
        }

        if (generator != null)
        {
            Class valueGeneratedType = null;
            try
            {
                // Use getStorageClass method if available
                valueGeneratedType = (Class) generator.getClass().getMethod("getStorageClass").invoke(null);
            }
            catch (Exception e)
            {
                if (generator.getClass().getGenericSuperclass() instanceof ParameterizedType)
                {
                    // TODO Improve this so it works always
                    ParameterizedType parameterizedType = (ParameterizedType) generator.getClass().getGenericSuperclass();
                    valueGeneratedType = (Class) parameterizedType.getActualTypeArguments()[0];
                }
            }
            return valueGeneratedType;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.valuegenerator.ValueGenerationManager#createValueGenerator(java.lang.String, java.lang.Class, java.util.Properties, org.datanucleus.store.valuegenerator.ValueGenerationConnectionProvider)
     */
    @Override
    public ValueGenerator createValueGenerator(String name, Class generatorClass, Properties props, ValueGenerationConnectionProvider connectionProvider)
    {
        // Create the requested generator
        ValueGenerator generator;
        try
        {
            if (NucleusLogger.VALUEGENERATION.isDebugEnabled())
            {
                NucleusLogger.VALUEGENERATION.debug(Localiser.msg("040001", generatorClass.getName(), name));
            }
            Class[] argTypes = new Class[] {String.class, Properties.class};
            Object[] args = new Object[] {name, props};
            Constructor ctor = generatorClass.getConstructor(argTypes);
            generator = (ValueGenerator)ctor.newInstance(args);
        }
        catch (Exception e)
        {
            NucleusLogger.VALUEGENERATION.error(e);
            throw new ValueGenerationException(Localiser.msg("040000", generatorClass.getName(),e),e);
        }

        if (generator instanceof AbstractDatastoreGenerator && storeMgr != null)
        {
            // Set the store manager and connection provider for any datastore-based generators
            ((AbstractDatastoreGenerator)generator).setStoreManager(storeMgr);
            ((AbstractDatastoreGenerator)generator).setConnectionProvider(connectionProvider);
        }

        return generator;
    }
}