/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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

import java.sql.Timestamp;

import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Helper methods for handling optimistic versioning.
 */
public class VersionHelper
{
    /** Localiser for messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance(
        "org.datanucleus.Localisation", org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Perform an optimistic version check on the passed object, against the passed version in the datastore.
     * @param op ObjectProvider of the object to check
     * @param versionDatastore Version of the object in the datastore
     * @param versionMetaData VersionMetaData to use for checking
     * @throws NucleusUserException thrown when an invalid strategy is specified
     * @throws NucleusOptimisticException thrown when the version check fails
     */
    public static void performVersionCheck(ObjectProvider op, Object versionDatastore, VersionMetaData versionMetaData)
    {
        // Extract the version of the object (that we are updating)
        Object versionObject = op.getTransactionalVersion();
        if (versionObject == null)
        {
            return;
        }

        if (versionMetaData == null)
        {
            // No version specification so no check needed
            NucleusLogger.PERSISTENCE.info(op.getClassMetaData().getFullClassName() + 
                " has no version metadata so no check of version is required, since this will not have the version flag in its table");
            return;
        }

        boolean valid;
        if (versionMetaData.getVersionStrategy() == VersionStrategy.DATE_TIME)
        {
            valid = ((Timestamp)versionObject).getTime() == ((Timestamp)versionDatastore).getTime();
        }
        else if (versionMetaData.getVersionStrategy() == VersionStrategy.VERSION_NUMBER)
        {
            valid = ((Number)versionObject).longValue() == ((Number)versionDatastore).longValue();
        }
        else if (versionMetaData.getVersionStrategy() == VersionStrategy.STATE_IMAGE)
        {
            // TODO Support state-image strategy
            throw new NucleusUserException(LOCALISER.msg("032017",
                op.getClassMetaData().getFullClassName(), versionMetaData.getVersionStrategy()));
        }
        else
        {
            throw new NucleusUserException(LOCALISER.msg("032017",
                op.getClassMetaData().getFullClassName(), versionMetaData.getVersionStrategy()));
        }

        if (!valid)
        {
            String msg = LOCALISER.msg("032016", 
                op.getObjectAsPrintable(), op.getInternalObjectId(), "" + versionDatastore, "" + versionObject);
            NucleusLogger.PERSISTENCE.error(msg);
            throw new NucleusOptimisticException(msg, op.getObject());
        }
    }

    /**
     * Convenience method to provide the next version, using the version strategy given the
     * supplied current version.
     * @param versionStrategy Version strategy
     * @param currentVersion The current version
     * @return The next version
     * @throws NucleusUserException Thrown if the strategy is not supported.
     */
    public static Object getNextVersion(VersionStrategy versionStrategy, Object currentVersion)
    {
        if (versionStrategy == null)
        {
            return null;
        }
        else if (versionStrategy == VersionStrategy.NONE)
        {
            // Just increment the version - is this really necessary?
            if (currentVersion == null)
            {
                return Long.valueOf(1);
            }
            if (currentVersion instanceof Integer)
            {
                return Integer.valueOf(((Integer)currentVersion).intValue()+1);
            }
            return Long.valueOf(((Long)currentVersion).longValue()+1);
        }
        else if (versionStrategy == VersionStrategy.DATE_TIME)
        {
            return new Timestamp(System.currentTimeMillis());
        }
        else if (versionStrategy == VersionStrategy.VERSION_NUMBER)
        {
            if (currentVersion == null)
            {
                return Long.valueOf(1);
            }
            if (currentVersion instanceof Integer)
            {
                return Integer.valueOf(((Integer)currentVersion).intValue()+1);
            }
            return Long.valueOf(((Long)currentVersion).longValue()+1);
        }
        else if (versionStrategy == VersionStrategy.STATE_IMAGE)
        {
            // TODO Support state-image strategy
            throw new NucleusUserException("DataNucleus doesnt currently support version strategy \"state-image\"");
        }
        else
        {
            throw new NucleusUserException("Unknown version strategy - not supported");
        }
    }

}