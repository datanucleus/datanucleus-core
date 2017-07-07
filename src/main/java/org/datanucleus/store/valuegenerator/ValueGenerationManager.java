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

import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Manager for the creation of ValueGenerators.
 * Allows creation of generators and provides lookup by symbolic name.
 */
public class ValueGenerationManager
{
    protected final StoreManager storeMgr;

    /** Map of ValueGenerator keyed by the symbolic name. */
    protected Map<String, ValueGenerator> generatorsByName = new ConcurrentHashMap<>();

    /**
     * Constructor.
     * @param storeMgr Store Manager
     */
    public ValueGenerationManager(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
    }

    /**
     * Method to clear out the generators managed by this manager.
     */
    public void clear()
    {
        generatorsByName.clear();
    }

    /**
     * Accessor for the ValueGenerator with the given symbolic name.
     * @param name Name of the ValueGenerator when created
     * @return The ValueGenerator with this name
     */
    public ValueGenerator getValueGenerator(String name)
    {
        if (name == null)
        {
            return null;
        }
        return generatorsByName.get(name);
    }

    /**
     * Method to create a ValueGenerator when the generator is datastore based.
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

        // Store the generator
        generatorsByName.put(name, generator);

        return generator;
    }

    public ValueGenerator createValueGenerator(String strategyName)
    {
        ValueGenerator generator = null;

        // Create generator so we can find the generated type
        // a). Try as unique generator first
        try
        {
            generator = (ValueGenerator)storeMgr.getNucleusContext().getPluginManager().createExecutableExtension(
                "org.datanucleus.store_valuegenerator", 
                new String[] {"name", "unique"}, new String[] {strategyName, "true"},
                "class-name", new Class[] {String.class, Properties.class}, new Object[] {null, null});
            if (generator == null)
            {
                // b). Try as datastore-specific generator
                generator = (AbstractGenerator)storeMgr.getNucleusContext().getPluginManager().createExecutableExtension(
                    "org.datanucleus.store_valuegenerator",
                    new String[] {"name", "datastore"}, new String[] {strategyName, storeMgr.getStoreManagerKey()},
                    "class-name", new Class[] {String.class, Properties.class}, new Object[] {null, null});
            }
        }
        catch (Exception e)
        {
            
        }

        return generator;
    }
}