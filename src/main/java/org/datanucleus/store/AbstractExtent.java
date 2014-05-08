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

import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.util.Localiser;

/**
 * Abstract representation of a JDO Extent.
 * Suitable for use with all datastores.
 */
public abstract class AbstractExtent<T> implements org.datanucleus.store.Extent<T>
{
    /** Localised messages source */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /** ExecutionContext */
    protected final ExecutionContext ec;

    /** The candidate class. We store the class since we need to retain it for class loading. */
    protected final Class<T> candidateClass;

    /** Whether to include subclasses. */
    protected final boolean subclasses;

    /** ClassMetaData for the candidate class. */
    protected final AbstractClassMetaData cmd;

    /**
     * Constructor.
     * @param ec Execution Context
     * @param cls candidate class
     * @param subclasses Whether to include subclasses
     * @param cmd MetaData for the candidate class
     */
    public AbstractExtent(ExecutionContext ec, Class<T> cls, boolean subclasses, AbstractClassMetaData cmd)
    {
        if (cls == null)
        {
            throw new NucleusUserException(LOCALISER.msg("033000")).setFatal();
        }

        // Find the MetaData for this class
        this.cmd = cmd;
        if (cmd == null)
        {
            throw new NucleusUserException(LOCALISER.msg("033001", cls.getName())).setFatal();
        }

        this.ec = ec;
        this.candidateClass = cls;
        this.subclasses = subclasses;
    }

    /**
     * Returns whether this Extent was defined to contain subclasses.
     * @return true if this Extent was defined to include subclasses.
     */
    public boolean hasSubclasses()
    {
        return subclasses;
    }

    /**
     * Accessor for the class of instances in this Extent.
     * @return the Class of instances of this Extent
     */
    public Class<T> getCandidateClass()
    {
        return candidateClass;
    }

    /**
     * Accessor for the owning execution context.
     * @return execution context
     */
    public ExecutionContext getExecutionContext()
    {
        return ec;
    }

    /**
     * Stringifier method.
     * @return Stringified form of this object
     */
    public String toString()
    {
        return LOCALISER.msg("033002", candidateClass.getName(), "" + subclasses);
    }
}