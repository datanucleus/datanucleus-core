/**********************************************************************
Copyright (c) 2008 Eric Sultan and others. All rights reserved.
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
2008 Andy Jefferson - updated to match DN conventions and localisation
    ...
**********************************************************************/
package org.datanucleus.store.autostart;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.PackageMetaData;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.exceptions.DatastoreInitialisationException;

/**
 * An auto-starter mechanism that uses a defined list of metadata files to be loaded at startup.
 */
public class MetaDataAutoStarter extends AbstractAutoStartMechanism 
{
    /** Names of the metadata files to start with (comma-separated). */
    protected String metaDataFiles;
    protected StoreManager storeMgr;
    protected ClassLoaderResolver clr;

    protected Collection classes = new HashSet();

    /**
     * Constructor, taking the names of the metadata to use.
     * @param storeMgr The StoreManager managing the store that we are auto-starting.
     * @param clr The ClassLoaderResolver
     */
    public MetaDataAutoStarter(StoreManager storeMgr, ClassLoaderResolver clr) 
    {
        metaDataFiles = storeMgr.getStringProperty("datanucleus.autoStartMetaDataFiles");
        this.storeMgr = storeMgr;
        this.clr = clr;
    }

    /**
     * Accessor for all auto start data for this starter.
     * @return The class auto start data. Collection of StoreData elements
     * @throws org.datanucleus.store.exceptions.DatastoreInitialisationException
     */
    public Collection getAllClassData()
    throws DatastoreInitialisationException 
    {
        if (metaDataFiles == null)
        {
            return Collections.EMPTY_SET;
        }

        Collection fileMetaData = 
            storeMgr.getNucleusContext().getMetaDataManager().loadFiles(metaDataFiles.split(","), clr);
        Iterator iter = fileMetaData.iterator();
        while (iter.hasNext()) 
        {
            FileMetaData filemd = (FileMetaData) iter.next();
            for (int i = 0; i < filemd.getNoOfPackages(); i++) 
            {
                PackageMetaData pmd = filemd.getPackage(i);
                for (int j = 0; j < pmd.getNoOfClasses(); j++) 
                {
                    ClassMetaData cmd = pmd.getClass(j);
                    classes.add(new StoreData(cmd.getFullClassName().trim(), null, StoreData.FCO_TYPE, null));
                }
            }
        }
        return classes;
    }

    /**
     * Method to add a class to the starter.
     * @param data The store data to add
     */
    public void addClass(StoreData data) 
    {
        // Do nothing. We are fixed from construction
    }

    /**
     * Method to remove a class from the starter.
     * @param className The name of the class to remove.
     */
    public void deleteClass(String className) 
    {
        // Do nothing. We are fixed from construction
    }

    /**
     * Method to remove all classes from the starter.
     */
    public void deleteAllClasses()
    {
        // Do nothing. We are fixed from construction
    }

    /**
     * Method to give a descriptive name for the starter process.
     * @return Description of the starter process.
     */
    public String getStorageDescription()
    {
        return LOCALISER.msg("034150");
    }
}