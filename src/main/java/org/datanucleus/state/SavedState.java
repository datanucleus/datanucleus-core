/**********************************************************************
Copyright (c) 2022 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.enhancement.Persistable;

/**
 * Definition of the saved state of a Persistable object.
 * Used during the rollback process.
 */
public class SavedState
{
    /** Copy (shallow) of the Persistable instance when the instance is enlisted in the transaction. */
    protected Persistable pc = null;

    /** Flags of the persistable instance when the instance is enlisted in the transaction. */
    protected byte persistenceFlags;

    /** Loaded fields of the persistable instance when the instance is enlisted in the transaction. */
    protected boolean[] loadedFields = null;

    public SavedState(Persistable pc, boolean[] loadedFields, byte persistenceFlags)
    {
        this.pc = pc;
        this.loadedFields = loadedFields;
        this.persistenceFlags = persistenceFlags;
    }

    public Persistable getPC()
    {
        return pc;
    }

    public byte getPersistenceFlags()
    {
        return persistenceFlags;
    }

    public void setPersistenceFlags(byte persistenceFlags)
    {
        this.persistenceFlags = persistenceFlags;
    }

    public boolean[] getLoadedFields()
    {
        return loadedFields;
    }

    public void setLoadedFields(boolean[] loadedFields)
    {
        this.loadedFields = loadedFields;
    }
}