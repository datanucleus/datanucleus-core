/**********************************************************************
Copyright (c) 2003 Andy Jefferson and others. All rights reserved. 
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
2004 Andy Jefferson - changed to use StoreData instead of AutoStartData
2004 Erik Bengtson - added open()/close()/isOpen()
    ...
**********************************************************************/
package org.datanucleus.store.autostart;

import java.util.Collection;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.exceptions.DatastoreInitialisationException;

/**
 * Interface defining an Auto-Start Mechanism.
 * An Auto-Start Mechanism is a means of auto-populating the classes supported by a StoreManager.
 * <P>
 * If the user changes their persistence definition a problem can occur when starting up DataNucleus. 
 * DataNucleus loads up its existing data from a repository (e.g the table "NUCLEUS_TABLES" for 
 * SchemaTableAutoStarter) and finds that a table/class required by the repository data no longer exists. 
 * There are 3 options for what DataNucleus will do in this situation. 
 * <ul>
 * <li>Checked will mean that DataNucleus will throw an exception and the user will be expected to manually 
 *     fix their datastore mismatch (perhaps by removing the existing tables).</li>
 * <li>Quiet (the default) will simply remove the entry from the repository and continue without exception.</li>
 * <li>Ignored will simply continue without doing anything.</li>
 * </ul>
 * </P>
 * 
 * Implementations must have a public constructor taking the arguments {@link StoreManager} and 
 * {@link ClassLoaderResolver}
 */
public interface AutoStartMechanism
{
    public static enum Mode 
    {
        NONE,
        QUIET,
        CHECKED,
        IGNORED;
    }

    /**
     * Accessor for the mode of operation.
     * @return The mode of operation
     */
    Mode getMode();

    /**
     * Mutator for the mode of operation.
     * @param mode The mode of operation
     */
    void setMode(Mode mode);

    /**
     * Accessor for the data for the classes that are currently auto started.
     * @return Collection of {@link StoreData} elements
     * @throws DatastoreInitialisationException
     */
    Collection getAllClassData() throws DatastoreInitialisationException;

    /**
     * Starts a transaction for writing (add/delete) classes to the auto start mechanism.
     */
    void open();

    /**
     * Closes a transaction for writing (add/delete) classes to the auto start mechanism.
     */
    void close();

    /**
     * Whether it's open for writing (add/delete) classes to the auto start mechanism.
     * @return whether this is open for writing 
     */
    public boolean isOpen();

    /**
     * Method to add a class/field (with its data) to the currently-supported list.
     * @param data The data for the class.
     */
    void addClass(StoreData data);

    /**
     * Method to delete a class/field that is currently listed as supported in
     * the internal storage.
     * It does not drop the schema of the DatastoreClass 
     * neither the contents of it. It only removes the class from the 
     * AutoStart mechanism.
     * TODO Rename this method to allow for deleting fields
     * @param name The name of the class/field
     */
    void deleteClass(String name);

    /**
     * Method to delete all classes that are currently listed as supported in
     * the internal storage. It does not drop the schema of the DatastoreClass 
     * neither the contents of it. It only removes the classes from the 
     * AutoStart mechanism.
     */
    void deleteAllClasses();

    /**
     * Utility to return a description of the storage for this mechanism.
     * @return The storage description.
     */
    String getStorageDescription();
}