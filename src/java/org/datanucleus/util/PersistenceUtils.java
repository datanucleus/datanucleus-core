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
package org.datanucleus.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.datanucleus.ClassConstants;
import org.datanucleus.PersistenceConfiguration;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * Series of convenience methods for the persistence process.
 */
public class PersistenceUtils
{
    /** Localisation of messages. */
    protected static final Localiser LOCALISER = Localiser.getInstance("org.datanucleus.Localisation",
        ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Method to return the Persistence Properties from the specified file.
     * @param filename Name of the file containing the properties
     * @return the Persistence Properties in this file
     * @throws NucleusUserException if file not readable
     */
    public static synchronized Properties setPropertiesUsingFile(String filename)
    {
        if (filename == null)
        {
            return null;
        }

        // try to load the properties file
        Properties props = new Properties();
        File file = new File(filename);
        if (file.exists())
        {
            try
            {
                InputStream is = new FileInputStream(file);
                props.load(is);
                is.close();
            }
            catch (FileNotFoundException e)
            {
                throw new NucleusUserException(LOCALISER.msg("008014", filename), e).setFatal();
            }
            catch (IOException e)
            {
                throw new NucleusUserException(LOCALISER.msg("008014", filename), e).setFatal();
            }
        }
        else
        {
            // Try to load it as a resource in the CLASSPATH
            try
            {
                InputStream is = PersistenceConfiguration.class.getClassLoader().getResourceAsStream(filename);
                props.load(is);
                is.close();
            }
            catch (Exception e)
            {
                // Still not loadable so throw exception
                throw new NucleusUserException(LOCALISER.msg("008014", filename), e).setFatal();
            }
        }

        return props;
    }
}