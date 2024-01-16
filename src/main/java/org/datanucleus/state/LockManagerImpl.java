/**********************************************************************
Copyright (c) 2010 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.state;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusOptimisticException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.identity.IdentityUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.metadata.VersionMetaData;
import org.datanucleus.metadata.VersionStrategy;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Implementation of a lock manager for objects.
 */
public class LockManagerImpl implements LockManager
{
    protected final ExecutionContext ec;

    /** Map of lock mode, keyed by the object identity. Utilised on a find operation. */
    private Map<Object, LockMode> requiredLockModesById = null;

    /** Map of lock mode, keyed by StateManager. */
    private Map<DNStateManager, LockMode> lockModeBySM = null;

    public LockManagerImpl(ExecutionContext ec)
    {
        this.ec = ec;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#close()
     */
    public void close()
    {
        clear();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#clear()
     */
    public void clear()
    {
        if (requiredLockModesById != null)
        {
            requiredLockModesById.clear();
        }
        if (lockModeBySM != null)
        {
            lockModeBySM.clear();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#lock(java.lang.Object, org.datanucleus.state.LockMode)
     */
    public void lock(Object id, LockMode lockMode)
    {
        if (requiredLockModesById == null)
        {
            requiredLockModesById = new HashMap<>();
        }
        requiredLockModesById.put(id, lockMode);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#getLockMode(java.lang.Object)
     */
    public LockMode getLockMode(Object id)
    {
        if (requiredLockModesById != null)
        {
            LockMode lockMode = requiredLockModesById.get(id);
            if (lockMode != null)
            {
                return lockMode;
            }
        }

        return LockMode.LOCK_NONE;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#lock(org.datanucleus.DNStateManager, org.datanucleus.state.LockMode)
     */
    public void lock(DNStateManager sm, LockMode lockMode)
    {
        if (lockModeBySM == null)
        {
            lockModeBySM = new HashMap<>();
        }
        lockModeBySM.put(sm, lockMode);

        if (lockMode == LockMode.LOCK_PESSIMISTIC_READ || lockMode == LockMode.LOCK_PESSIMISTIC_WRITE)
        {
            // Do a SELECT ... FOR UPDATE (for RDBMS) or alternative to lock the object in the datastore
            sm.locate();
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#unlock(org.datanucleus.DNStateManager)
     */
    public void unlock(DNStateManager sm)
    {
        if (lockModeBySM != null)
        {
            lockModeBySM.remove(sm);
        }
        // TODO Need to remove any row lock from the datastore. How, if we did "SELECT ... FOR UPDATE" in RDBMS?
    }

    /* (non-Javadoc)
     * @see org.datanucleus.state.lock.LockManager#getLockMode(org.datanucleus.DNStateManager)
     */
    public LockMode getLockMode(DNStateManager sm)
    {
        if (sm == null)
        {
            return LockMode.LOCK_NONE;
        }

        if (lockModeBySM != null)
        {
            return lockModeBySM.containsKey(sm) ? lockModeBySM.get(sm) : LockMode.LOCK_NONE;
        }
        return LockMode.LOCK_NONE;
    }


    /**
     * Perform an optimistic version check on the passed object, against the passed version in the datastore.
     * @param sm StateManager of the object to check
     * @param versionStrategy Version strategy
     * @param versionDatastore Version of the object in the datastore
     * @throws NucleusUserException thrown when an invalid strategy is specified
     * @throws NucleusOptimisticException thrown when the version check fails
     */
    public void performOptimisticVersionCheck(DNStateManager sm, VersionStrategy versionStrategy, Object versionDatastore)
    {
        // Extract the version of the object (that we are updating)
        Object versionObject = sm.getTransactionalVersion();
        if (versionObject == null)
        {
            return;
        }

        if (versionStrategy == null)
        {
            // No version specification so no check needed
            NucleusLogger.PERSISTENCE.info(sm.getClassMetaData().getFullClassName() + 
                " has no version metadata so no check of version is required, since this will not have the version flag in its table");
            return;
        }

        boolean valid;
        if (versionStrategy == VersionStrategy.DATE_TIME)
        {
            if (versionObject instanceof Calendar)
            {
                valid = ((Calendar)versionObject).getTimeInMillis() == ((Calendar)versionDatastore).getTimeInMillis();
            }
            else if (versionObject instanceof java.time.Instant)
            {
                valid = ((java.time.Instant)versionObject).equals(versionDatastore);
            }
            // TODO Support other date-time types e.g java.time
            else
            {
                valid = ((java.util.Date)versionObject).getTime() == ((java.util.Date)versionDatastore).getTime();
            }
        }
        else if (versionStrategy == VersionStrategy.VERSION_NUMBER)
        {
            valid = ((Number)versionObject).longValue() == ((Number)versionDatastore).longValue();
        }
        else if (versionStrategy == VersionStrategy.STATE_IMAGE)
        {
            // TODO Support state-image strategy
            throw new NucleusUserException(Localiser.msg("032017", sm.getClassMetaData().getFullClassName(), versionStrategy));
        }
        else
        {
            throw new NucleusUserException(Localiser.msg("032017", sm.getClassMetaData().getFullClassName(), versionStrategy));
        }

        if (!valid)
        {
            throw new NucleusOptimisticException(Localiser.msg("032016", IdentityUtils.getPersistableIdentityForId(sm.getInternalObjectId()), 
                "" + versionDatastore, "" + versionObject), sm.getObject());
        }
    }

    /**
     * Convenience method to provide the next version to use given the VersionMetaData and the current version.
     * @param currentVersion The current version
     * @return The next version
     * @throws NucleusUserException Thrown if the strategy is not supported.
     */
    public Object getNextVersion(VersionMetaData vermd, Object currentVersion)
    {
        if (vermd == null)
        {
            return null;
        }

        VersionStrategy versionStrategy = vermd.getStrategy();
        if (versionStrategy == null)
        {
            return null;
        }
        else if (versionStrategy == VersionStrategy.NONE)
        {
            if (currentVersion == null)
            {
                // Set an initial value, using numeric as the basis
                if (vermd.getMemberName() != null)
                {
                    AbstractMemberMetaData verMmd = ((AbstractClassMetaData)vermd.getParent()).getMetaDataForMember(vermd.getMemberName());
                    if (verMmd.getType() == Integer.class || verMmd.getType() == int.class)
                    {
                        return Integer.valueOf(1);
                    }
                    else if (verMmd.getType() == Short.class || verMmd.getType() == short.class)
                    {
                        return Short.valueOf((short)1);
                    }
                }
                return Long.valueOf(1);
            }

            return currentVersion;
        }
        else if (versionStrategy == VersionStrategy.DATE_TIME)
        {
            if (vermd.getMemberName() != null)
            {
                AbstractMemberMetaData verMmd = ((AbstractClassMetaData)vermd.getParent()).getMetaDataForMember(vermd.getMemberName());
                if (Calendar.class.isAssignableFrom(verMmd.getType()))
                {
                    return Calendar.getInstance();
                }
                else if (java.sql.Time.class.isAssignableFrom(verMmd.getType()))
                {
                    return new java.sql.Time(System.currentTimeMillis());
                }
                else if (java.sql.Date.class.isAssignableFrom(verMmd.getType()))
                {
                    return new java.sql.Date(System.currentTimeMillis());
                }
                else if (java.time.Instant.class.isAssignableFrom(verMmd.getType()))
                {
                    return java.time.Instant.now();
                }
                // TODO Support other date-time types e.g java.time.XXX
            }
            return new java.sql.Timestamp(System.currentTimeMillis());
        }
        else if (versionStrategy == VersionStrategy.VERSION_NUMBER)
        {
            // First check if a max-value is defined in version-meta-data
            // and if so then check if we should roll over - back to initial value.
            Integer maxValue = null;
            if (vermd.hasExtension(MetaData.EXTENSION_VERSION_NUMBER_MAX_VALUE))
            {
                maxValue = Integer.valueOf(vermd.getValueForExtension(MetaData.EXTENSION_VERSION_NUMBER_MAX_VALUE));
            }
            if (currentVersion == null ||
                    (maxValue != null && currentVersion instanceof Number && ((Number)currentVersion).intValue() >= maxValue))
            {
                // Get the initial value from the VersionMetaData extension if provided, otherwise the global default (for the context)
                Integer initValue = null;
                if (vermd.hasExtension(MetaData.EXTENSION_VERSION_NUMBER_INITIAL_VALUE))
                {
                    initValue = Integer.valueOf(vermd.getValueForExtension(MetaData.EXTENSION_VERSION_NUMBER_INITIAL_VALUE));
                }
                else
                {
                    initValue = ec.getIntProperty(PropertyNames.PROPERTY_VERSION_NUMBER_INITIAL_VALUE);
                }

                if (vermd.getMemberName() != null)
                {
                    AbstractMemberMetaData verMmd = ((AbstractClassMetaData)vermd.getParent()).getMetaDataForMember(vermd.getMemberName());
                    if (verMmd.getType() == Integer.class || verMmd.getType() == int.class)
                    {
                        return initValue;
                    }
                    else if (verMmd.getType() == Short.class || verMmd.getType() == short.class)
                    {
                        return initValue.shortValue();
                    }
                }
                return Long.valueOf(initValue);
            }

            if (currentVersion instanceof Integer)
            {
                return Integer.valueOf(((Integer)currentVersion).intValue()+1);
            }
            else if (currentVersion instanceof Short)
            {
                return Short.valueOf((short) (((Short)currentVersion).shortValue()+1));
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