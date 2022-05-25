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
package org.datanucleus.store.autostart;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.DatastoreInitialisationException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.StoreData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Convenience methods to assist in use of AutoStartMechanism(s).
 */
public class AutoStartMechanismUtils
{
    /**
     * Convenience method to instantiate and initialise an auto-start mechanism
     * @param nucCtx NucleusContext
     * @param clr ClassLoader resolver
     * @param mechanism The mechanism name
     * @param mode The mode of operation
     * @return The AutoStartMechanism
     * @throws DatastoreInitialisationException thrown when problems are encountered initialising the starter when connected to a database
     */
    public static AutoStartMechanism createAutoStartMechanism(PersistenceNucleusContext nucCtx, ClassLoaderResolver clr, String mechanism, String mode)
    throws DatastoreInitialisationException
    {
        StoreManager storeMgr = nucCtx.getStoreManager();

        // Create the starter
        AutoStartMechanism starter = null;
        if ("Classes".equalsIgnoreCase(mechanism))
        {
            starter = new ClassesAutoStarter(storeMgr, clr);
        }
        else if ("XML".equalsIgnoreCase(mechanism))
        {
            try
            {
                starter = new XMLAutoStarter(storeMgr, clr);
            }
            catch (MalformedURLException mue)
            {
                NucleusLogger.PERSISTENCE.warn("Unable to create XML AutoStarter due to ", mue);
                starter = null;
            }
        }
        else if ("MetaData".equalsIgnoreCase(mechanism))
        {
            starter = new MetaDataAutoStarter(storeMgr, clr);
        }
        else
        {
            // Fallback to the plugin mechanism
            PluginManager pluginMgr = nucCtx.getPluginManager();
            String autoStarterClassName = pluginMgr.getAttributeValueForExtension("org.datanucleus.autostart", "name", mechanism, "class-name");
            if (autoStarterClassName != null)
            {
                Class[] argsClass = new Class[] {ClassConstants.STORE_MANAGER, ClassConstants.CLASS_LOADER_RESOLVER};
                Object[] args = new Object[] {storeMgr, clr};
                try
                {
                    starter = (AutoStartMechanism) pluginMgr.createExecutableExtension("org.datanucleus.autostart", "name", mechanism, "class-name", argsClass, args);
                }
                catch (Exception e)
                {
                    NucleusLogger.PERSISTENCE.error(StringUtils.getStringFromStackTrace(e));
                }
            }
        }
        if (starter == null)
        {
            return null;
        }

        mode = mode.toUpperCase();
        if (mode.equals(AutoStartMechanism.Mode.NONE.name()))
        {
            starter.setMode(AutoStartMechanism.Mode.NONE);
        }
        else if (mode.equals(AutoStartMechanism.Mode.CHECKED.name()))
        {
            starter.setMode(AutoStartMechanism.Mode.CHECKED);
        }
        else if (mode.equals(AutoStartMechanism.Mode.QUIET.name()))
        {
            starter.setMode(AutoStartMechanism.Mode.QUIET);
        }
        else if (mode.equals(AutoStartMechanism.Mode.IGNORED.name()))
        {
            starter.setMode(AutoStartMechanism.Mode.IGNORED);
        }

        // Initialise the starter
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("034005", mechanism));
        }
        boolean illegalState = false;
        try
        {
            if (!starter.isOpen())
            {
                starter.open();
            }
            Collection existingData = starter.getAllClassData();
            if (existingData != null && !existingData.isEmpty())
            {
                List classesNeedingAdding = new ArrayList();
                Iterator existingDataIter = existingData.iterator();
                while (existingDataIter.hasNext())
                {
                    StoreData data = (StoreData) existingDataIter.next();
                    if (data.isFCO())
                    {
                        // Catch classes that don't exist (e.g in use by a different app)
                        Class classFound = null;
                        try
                        {
                            classFound = clr.classForName(data.getName());
                        }
                        catch (ClassNotResolvedException cnre)
                        {
                            if (data.getInterfaceName() != null)
                            {
                                try
                                {
                                    nucCtx.getImplementationCreator().newInstance(clr.classForName(data.getInterfaceName()), clr);
                                    classFound = clr.classForName(data.getName());
                                }
                                catch (ClassNotResolvedException cnre2)
                                {
                                    // Do nothing
                                }
                            }
                            // Thrown if class not found
                        }

                        if (classFound != null)
                        {
                            NucleusLogger.PERSISTENCE.info(Localiser.msg("032003", data.getName()));
                            classesNeedingAdding.add(data.getName());
                            if (data.getMetaData() == null)
                            {
                                // StoreData doesnt have its metadata set yet so load it
                                // This ensures that the MetaDataManager always knows about these classes
                                AbstractClassMetaData acmd = nucCtx.getMetaDataManager().getMetaDataForClass(classFound, clr);
                                if (acmd != null)
                                {
                                    data.setMetaData(acmd);
                                }
                                else
                                {
                                    String msg = Localiser.msg("034004", data.getName());
                                    if (starter.getMode() == AutoStartMechanism.Mode.CHECKED)
                                    {
                                        NucleusLogger.PERSISTENCE.error(msg);
                                        throw new DatastoreInitialisationException(msg);
                                    }
                                    else if (starter.getMode() == AutoStartMechanism.Mode.IGNORED)
                                    {
                                        NucleusLogger.PERSISTENCE.warn(msg);
                                    }
                                    else if (starter.getMode() == AutoStartMechanism.Mode.QUIET)
                                    {
                                        NucleusLogger.PERSISTENCE.warn(msg);
                                        NucleusLogger.PERSISTENCE.warn(Localiser.msg("034001", data.getName()));
                                        starter.deleteClass(data.getName());
                                    }
                                }
                            }
                        }
                        else
                        {
                            String msg = Localiser.msg("034000", data.getName());
                            if (starter.getMode() == AutoStartMechanism.Mode.CHECKED)
                            {
                                NucleusLogger.PERSISTENCE.error(msg);
                                throw new DatastoreInitialisationException(msg);
                            }
                            else if (starter.getMode() == AutoStartMechanism.Mode.IGNORED)
                            {
                                NucleusLogger.PERSISTENCE.warn(msg);
                            }
                            else if (starter.getMode() == AutoStartMechanism.Mode.QUIET)
                            {
                                NucleusLogger.PERSISTENCE.warn(msg);
                                NucleusLogger.PERSISTENCE.warn(Localiser.msg("034001", data.getName()));
                                starter.deleteClass(data.getName());
                            }
                        }
                    }
                }
                String[] classesToLoad = new String[classesNeedingAdding.size()];
                Iterator classesNeedingAddingIter = classesNeedingAdding.iterator();
                int n = 0;
                while (classesNeedingAddingIter.hasNext())
                {
                    classesToLoad[n++] = (String)classesNeedingAddingIter.next();
                }

                // Load the classes into the StoreManager
                try
                {
                    storeMgr.manageClasses(clr, classesToLoad);
                }
                catch (Exception e)
                {
                    // Exception while adding so some of the (referenced) classes dont exist
                    NucleusLogger.PERSISTENCE.warn(Localiser.msg("034002", e));

                    // if an exception happens while loading AutoStart data, them we disable it, since it was unable to load the data from AutoStart. The memory state of AutoStart does
                    // not represent the database, and if we don't disable it, we could think that the autostart store is empty, and we would try to insert new entries in
                    // the autostart store that are already in there
                    illegalState = true;

                    // TODO Go back and add classes one-by-one to eliminate the class(es) with the problem
                }
            }
        }
        finally
        {
            if (starter.isOpen())
            {
                starter.close();
            }
            if (illegalState)
            {
                NucleusLogger.PERSISTENCE.warn(Localiser.msg("034003"));
                starter = null;
            }
            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("034006", mechanism));
            }
        }

        return starter;
    }
}
