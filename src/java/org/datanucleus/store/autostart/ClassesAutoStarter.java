/******************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved. 
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
*****************************************************************/
package org.datanucleus.store.autostart;

import java.util.Collection;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.store.exceptions.DatastoreInitialisationException;

/**
 * An auto-starter mechanism that uses a defined list of classes to be loaded at start.
 */
public class ClassesAutoStarter extends AbstractAutoStartMechanism
{
    /** Names of the classes to start with. */
    protected String classNames;

    /**
     * Constructor, taking the names of the classes to use.
     * @param storeMgr The StoreManager managing the store that we are auto-starting.
     * @param clr The ClassLoaderResolver
     */
    public ClassesAutoStarter(StoreManager storeMgr, ClassLoaderResolver clr)
    {
        this.classNames = storeMgr.getStringProperty("datanucleus.autoStartClassNames");
    }

    /**
     * Accessor for all auto start data for this starter.
     * @return The class auto start data. Collection of StoreData elements
     * @throws DatastoreInitialisationException
     */
    public Collection getAllClassData()
    throws DatastoreInitialisationException
    {
        Collection classes = new HashSet();
        if (classNames == null)
        {
            return classes;
        }

        StringTokenizer tokeniser = new StringTokenizer(classNames, ",");
        while (tokeniser.hasMoreTokens())
        {
            classes.add(new StoreData(tokeniser.nextToken().trim(), null, StoreData.FCO_TYPE, null));
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
     * Method to remove a class from the starter
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
        return LOCALISER.msg("034100");
    }
}