/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved. 
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
package org.datanucleus.store.valuegenerator;

import java.util.Properties;

import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract representation of a ValueGenerator requiring a connection to a datastore.
 * Builds on the base AbstractGenerator, providing datastore connection information.
 */
public abstract class AbstractConnectedGenerator<T> extends AbstractGenerator<T>
{
    /** The means of connecting to the datastore (if required by the generator). */
    protected ValueGenerationConnectionProvider connectionProvider;

    /** Flag for whether we know that the repository exists. Only applies if repository is required. */
    protected boolean repositoryExists = false;

    /**
     * Constructor.
     * @param storeMgr Store Manager
     * @param name Symbolic name for the generator
     * @param props Properties controlling the behaviour of the generator
     */
    public AbstractConnectedGenerator(StoreManager storeMgr, String name, Properties props)
    {
        super(storeMgr, name, props);
        allocationSize = 1;
    }

    /**
     * Mutator for setting the connection provider.
     * @param provider The connection provider.
     */
    public void setConnectionProvider(ValueGenerationConnectionProvider provider)
    {
        connectionProvider = provider;
    }

    public enum ConnectionPreference
    {
        NONE,
        EXISTING,
        NEW
    }

    /**
     * Accessor for any requirement for connection used by this value generator.
     * EXISTING means use the same connection as the ExecutionContext is using.
     * NEW means use a new connection, and commit it after any operation.
     * NONE means use NEW and allow override by the persistence property "datanucleus.valuegeneration.transactionAttribute".
     * @return The connection preference
     */
    public ConnectionPreference getConnectionPreference()
    {
        return ConnectionPreference.NONE;
    }

    /**
     * Indicator for whether the generator requires its own repository.
     * AbstractValueGenerator returns false and this should be overridden by all
     * generators requiring a repository.
     * @return Whether a repository is required.
     */
    protected boolean requiresRepository()
    {
        return false;
    }

    /**
     * Method to return if the repository already exists.
     * @return Whether the repository exists
     */
    protected boolean repositoryExists()
    {
        return true;
    }

    /**
     * Method to create any needed repository for the values.
     * AbstractValueGenerator just returns true and should be overridden by any
     * implementing generator requiring its own repository.
     * @return If all is ready for use
     */
    protected boolean createRepository()
    {
        // Do nothing - to be overridden by generators that want to create a repository for their ids
        return true;
    }

    /**
     * Get a new block with the specified number of ids.
     * @param number The number of additional ids required
     * @return the block
     */
    protected ValueGenerationBlock<T> obtainGenerationBlock(int number)
    {
        ValueGenerationBlock<T> block = null;

        // Try getting the block
        boolean repository_exists=true; // TODO Ultimately this can be removed when "repositoryExists()" is implemented
        try
        {
            if (requiresRepository() && !repositoryExists)
            {
                // Make sure the repository is present before proceeding
                repositoryExists = repositoryExists();
                if (!repositoryExists)
                {
                    createRepository();
                    repositoryExists = true;
                }
            }

            try
            {
                if (number < 0)
                {
                    block = reserveBlock();
                }
                else
                {
                    block = reserveBlock(number);
                }
            }
            catch (ValueGenerationException vex)
            {
                NucleusLogger.VALUEGENERATION.info(Localiser.msg("040003", vex.getMessage()));

                // attempt to obtain the block of unique identifiers is invalid
                if (requiresRepository())
                {
                    repository_exists = false;
                }
                else
                {
                    throw vex;
                }
            }
            catch (RuntimeException ex)
            {
                //exceptions cached by the value should be enclosed in ValueGenerationException
                //when the exceptions are not caught exception by value, we give a new try
                //in creating the repository
                NucleusLogger.VALUEGENERATION.info(Localiser.msg("040003", ex.getMessage()));
                // attempt to obtain the block of unique identifiers is invalid
                if (requiresRepository())
                {
                    repository_exists = false;
                }
                else
                {
                    throw ex;
                }
            }
        }
        finally
        {
        }

        // If repository didn't exist, try creating it and then get block
        if (!repository_exists)
        {
            try
            {
                NucleusLogger.VALUEGENERATION.info(Localiser.msg("040005"));
                if (!createRepository())
                {
                    throw new ValueGenerationException(Localiser.msg("040002"));
                }

                if (number < 0)
                {
                    block = reserveBlock();
                }
                else
                {
                    block = reserveBlock(number);
                }
            }
            finally
            {
            }
        }
        return block;
    }

}