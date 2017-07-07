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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
public class ValueGenerationManager
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
    public ValueGenerationManager(StoreManager storeMgr)
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

    /**
     * Method to clear out the generators managed by this manager.
     */
    public void clear()
    {
        generatorsByMemberKey.clear();
        uniqueGeneratorsByName.clear();
    }

    /**
     * Method to access the currently defined ValueGenerator for the specified member "key" (if any).
     * @param memberKey The member "key"
     * @return Its ValueGenerator
     */
    public ValueGenerator getValueGeneratorForMemberKey(String memberKey)
    {
        return generatorsByMemberKey.get(memberKey);
    }

    /**
     * Method to store a ValueGenerator for the specified member "key".
     * @param memberKey The member "key"
     * @param generator The ValueGenerator to use for that member key
     */
    public void registerValueGeneratorForMemberKey(String memberKey, ValueGenerator generator)
    {
        this.generatorsByMemberKey.put(memberKey, generator);
    }

    /**
     * Accessor for the "unique" ValueGenerator for the specified name (if any).
     * @param name The (strategy) name.
     * @return The ValueGenerator for that name
     */
    public ValueGenerator getUniqueValueGeneratorByName(String name)
    {
        return uniqueGeneratorsByName.get(name);
    }

    /**
     * Method to register a "unique" ValueGenerator for the specified (strategy) name.
     * @param name The name
     * @param generator The "unique" ValueGenerator
     */
    public void registerUniqueValueGeneratorForName(String name, ValueGenerator generator)
    {
        this.uniqueGeneratorsByName.put(name, generator);
    }

    /**
     * Method to create a ValueGenerator that is "unique" for the StoreManager.
     * @param strategyName Name of the strategy
     * @param props Any properties controlling its behaviour
     * @return The ValueGenerator
     */
    public ValueGenerator createUniqueValueGenerator(String strategyName, Properties props)
    {
        // Firstly try the built-in generators
        if ("timestamp".equalsIgnoreCase(strategyName))
        {
            return new TimestampGenerator(strategyName, props);
        }
        else if ("timestamp-value".equalsIgnoreCase(strategyName))
        {
            return new TimestampValueGenerator(strategyName, props);
        }
        else if ("auid".equalsIgnoreCase(strategyName))
        {
            return new AUIDGenerator(strategyName, props);
        }
        else if ("uuid".equalsIgnoreCase(strategyName))
        {
            return new UUIDGenerator(strategyName, props);
        }
        else if ("uuid-object".equalsIgnoreCase(strategyName))
        {
            return new UUIDObjectGenerator(strategyName, props);
        }
        else if ("uuid-hex".equalsIgnoreCase(strategyName))
        {
            return new UUIDHexGenerator(strategyName, props);
        }
        else if ("uuid-string".equalsIgnoreCase(strategyName))
        {
            return new UUIDStringGenerator(strategyName, props);
        }

        // Fallback to the plugin mechanism
        try
        {
            return (ValueGenerator)storeMgr.getNucleusContext().getPluginManager().createExecutableExtension("org.datanucleus.store_valuegenerator", 
                new String[] {"name", "unique"}, new String[] {strategyName, "true"},
                "class-name", new Class[] {String.class, Properties.class}, new Object[] {null, null});
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Method to create a ValueGenerator when the generator is datastore based.
     * This is used solely by the NucleusSequence API to create a generator, but not to register it here for further use.
     * @param name Symbolic name of the generator
     * @param generatorClass Class for the generator type
     * @param props Properties to control the generator
     * @param connectionProvider Provider for connections
     * @return The ValueGenerator
     */
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