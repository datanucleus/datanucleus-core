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

import org.datanucleus.exceptions.NucleusDataStoreException;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Abstract value generator.
 */
public abstract class AbstractGenerator implements ValueGenerator
{
    /** Localisation of messages */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** Symbolic name for the sequence. */
    protected String name;

    /** Properties controlling the generator behaviour. */
    protected Properties properties;

    /** Allocation size */
    protected int allocationSize = 5;

    /** Initial value (of the first id). */
    protected int initialValue = 0;

    /** The current block of values that have been reserved. */
    protected ValueGenerationBlock block;

    /** Flag for whether we know that the repository exists. Only applies if repository is required. */
    protected boolean repositoryExists = false;

    /**
     * Constructor.
     * Will receive the following properties (as a minimum) through this constructor.
     * <ul>
     * <li>class-name : Name of the class whose object is being inserted.</li>
     * <li>root-class-name : Name of the root class in this inheritance tree</li>
     * <li>field-name : Name of the field with the strategy (unless datastore identity field)</li>
     * <li>catalog-name : Catalog of the table (if specified)</li>
     * <li>schema-name : Schema of the table (if specified)</li>
     * <li>table-name : Name of the root table for this inheritance tree (containing the field).</li>
     * <li>column-name : Name of the column in the table (for the field)</li>
     * <li>sequence-name : Name of the sequence (if specified in MetaData as "sequence)</li>
     * </ul>
     * 
     * @param name Symbolic name for this generator
     * @param props Properties controlling the behaviour of the generator (or null if not required).
     */
    public AbstractGenerator(String name, Properties props)
    {
        this.name = name;
        this.properties = props;
    }

    /**
     * Accessor for the storage class for values generated with this generator.
     * @return Storage class (e.g Long.class)
     */
    public static Class getStorageClass()
    {
        return Long.class;
    }

    /**
     * Accessor for the symbolic name for this generator.
     * @return Symbolic name for the generator.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get next value from the reserved block of values.
     * @return The next value
     */
    public synchronized Object next()
    {
        // If the current block of ids is null or empty get a new one
        if (block == null || !block.hasNext())
        {
            // No more elements left in the block so replace it with a new one
            block = obtainGenerationBlock();
        }

        return block.next().getValue();
    }

    /**
     * Accessor for the current value allocated.
     * Returns null if none are allocated
     * @return The current value
     */
    public synchronized Object current()
    {
        if (block == null)
        {
            return null;
        }
        return block.current().getValue();
    }

    /**
     * Accessor for the next element in the sequence as a long.
     * @return The next element
     * @throws NucleusDataStoreException Thrown if not numeric
     */
    public long nextValue()
    {
        return getLongValueForObject(next());
    }

    /**
     * Accessor for the current element in the sequence as a long.
     * @return The current element
     * @throws NucleusDataStoreException Thrown if not numeric
     */
    public long currentValue()
    {
        return getLongValueForObject(current());
    }

    /**
     * Convenience method to convert a value into a long.
     * Throws NucleusDataStoreException if the value is not numeric.
     * @param oid The id
     * @return The long value
     * @throws NucleusDataStoreException Thrown if not numeric
     */
    private long getLongValueForObject(Object oid)
    {
        if (oid instanceof Long)
        {
            return ((Long)oid).longValue();
        }
        else if (oid instanceof Integer)
        {
            return ((Integer)oid).longValue();
        }
        else if (oid instanceof Short)
        {
            return ((Short)oid).longValue();
        }

        throw new NucleusDataStoreException(LOCALISER.msg("040009", name));
    }

    /**
     * Method to allocate a number of values into the block.
     * If the block already exists and has remaining values, the
     * additional values are added to the block.
     * @param additional The number to allocate
     */
    public synchronized void allocate(int additional)
    {
        if (block == null)
        {
            // No existing block so replace the existing block
            block = obtainGenerationBlock(additional);
        }
        else
        {
            // Existing block so append to it
            block.addBlock(obtainGenerationBlock(additional));
        }
    }

    /**
     * Get a new block with the default number of ids.
     * @return the block
     */
    protected ValueGenerationBlock obtainGenerationBlock()
    {
        // -1 here implies just use the default reserveBlock on the generator
        return obtainGenerationBlock(-1);
    }

    /**
     * Get a new block with the specified number of ids.
     * @param number The number of additional ids required
     * @return the block
     */
    protected ValueGenerationBlock obtainGenerationBlock(int number)
    {
        ValueGenerationBlock block = null;

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
                NucleusLogger.VALUEGENERATION.info(LOCALISER.msg("040003", vex.getMessage()));

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
                NucleusLogger.VALUEGENERATION.info(LOCALISER.msg("040003", ex.getMessage()));
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
                NucleusLogger.VALUEGENERATION.info(LOCALISER.msg("040005"));
                if (!createRepository())
                {
                    throw new ValueGenerationException(LOCALISER.msg("040002"));
                }
                else
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
            }
            finally
            {
            }
        }
        return block;
    }

    /**
     * Method to reserve a default sized block of values.
     * @return The reserved block
     */
    protected ValueGenerationBlock reserveBlock()
    {
        return reserveBlock(allocationSize);
    }

    /**
     * Method to reserve a block of "size" values.
     * @param size Number of values to reserve
     * @return The allocated block
     */
    protected abstract ValueGenerationBlock reserveBlock(long size);

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
}