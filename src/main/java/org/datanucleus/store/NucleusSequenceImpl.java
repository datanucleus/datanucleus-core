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
package org.datanucleus.store;

import java.util.Map;
import java.util.Properties;

import org.datanucleus.ExecutionContext;
import org.datanucleus.metadata.SequenceMetaData;
import org.datanucleus.store.connection.ManagedConnection;
import org.datanucleus.store.valuegenerator.ValueGenerationConnectionProvider;
import org.datanucleus.store.valuegenerator.ValueGenerationManager;
import org.datanucleus.store.valuegenerator.ValueGenerator;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Basic generic implementation of a datastore sequence.
 */
public class NucleusSequenceImpl implements NucleusSequence
{
    /** Store Manager where we obtain our sequence. */
    protected final StoreManager storeManager;

    /** Name of the sequence. */
    protected final SequenceMetaData seqMetaData;

    /** The generator for the sequence. */
    protected ValueGenerator generator;

    /** execution context. */
    protected final ExecutionContext ec;

    /**
     * Constructor.
     * @param objectMgr The ExecutionContext managing the sequence
     * @param storeMgr Manager of the store where we obtain the sequence
     * @param seqmd MetaData defining the sequence
     */
    public NucleusSequenceImpl(ExecutionContext objectMgr, final StoreManager storeMgr, SequenceMetaData seqmd)
    {
        this.ec = objectMgr;
        this.storeManager = storeMgr;
        this.seqMetaData = seqmd;

        setGenerator();
    }

    /**
     * Method to set the value generator to use.
     */
    protected void setGenerator()
    {
        // Allocate the ValueGenerationManager for this sequence
        String valueGeneratorName = "sequence";

        // Create the controlling properties for this sequence
        Properties props = new Properties();
        Map<String, String> seqExtensions = seqMetaData.getExtensions();
        if (seqExtensions != null && seqExtensions.size() > 0)
        {
            props.putAll(seqExtensions);
        }
        props.put("sequence-name", seqMetaData.getDatastoreSequence());
        props.put("sequence-name", seqMetaData.getDatastoreSequence());
        if (seqMetaData.getAllocationSize() > 0)
        {
            props.put(ValueGenerator.PROPERTY_KEY_CACHE_SIZE, "" + seqMetaData.getAllocationSize());
        }
        if (seqMetaData.getInitialValue() > 0)
        {
            props.put(ValueGenerator.PROPERTY_KEY_INITIAL_VALUE, "" + seqMetaData.getInitialValue());
        }

        // Get a ValueGenerationManager to create the generator
        ValueGenerationManager mgr = storeManager.getValueGenerationManager();
        ValueGenerationConnectionProvider connProvider = new ValueGenerationConnectionProvider()
        {
            ManagedConnection mconn;
            public ManagedConnection retrieveConnection()
            {
                mconn = storeManager.getConnectionManager().getConnection(ec);
                return mconn;
            }

            public void releaseConnection()
            {
                this.mconn.release();
                this.mconn = null;
            }
        };

        generator = mgr.createValueGenerator(valueGeneratorName, seqMetaData.getName(), props, connProvider);
        if (NucleusLogger.DATASTORE.isDebugEnabled())
        {
            NucleusLogger.DATASTORE.debug(Localiser.msg("017003", seqMetaData.getName(), valueGeneratorName));
        }
    }

    /**
     * Accessor for the sequence name.
     * @return The sequence name
     */
    public String getName()
    {
        return seqMetaData.getName();
    }

    /**
     * Method to allocate a set of elements.
     * @param additional The number of additional elements to allocate
     */
    public void allocate(int additional)
    {
        generator.allocate(additional);
    }

    /**
     * Accessor for the next element in the sequence.
     * @return The next element
     */
    public Object next()
    {
        return generator.next();
    }

    /**
     * Accessor for the next element in the sequence as a long.
     * @return The next element
     */
    public long nextValue()
    {
        return generator.nextValue();
    }

    /**
     * Accessor for the current element.
     * @return The current element.
     */
    public Object current()
    {
        return generator.current();
    }

    /**
     * Accessor for the current element in the sequence as a long.
     * @return The current element
     */
    public long currentValue()
    {
        return generator.currentValue();
    }
}