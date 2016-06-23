/**********************************************************************
Copyright (c) 2014 Marco Schulze and others. All rights reserved.
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
2005 Andy Jefferson - initial code embodied in DataNucleusEnhancer
    ...
**********************************************************************/
package org.datanucleus.enhancer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

import org.datanucleus.PropertyNames;
import org.datanucleus.util.CommandLine;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;

/**
 * Helper used by {@link DataNucleusEnhancer#main(String[])} to process the command line arguments.
 * <p>
 * <b>Important:</b> This class uses {@link System#exit(int)} in case of failures. It must therefore not be
 * used anywhere else than a <code>main(...)</code> method!
 * @author Marco หงุ่ยตระกูล-Schulze - marco at codewizards dot co
 */
class CommandLineHelper
{
    /** Logger for enhancing. */
    public static final NucleusLogger LOGGER = NucleusLogger.getLoggerInstance("DataNucleus.Enhancer");

    private final CommandLine cl;

    private String[] files;

    public CommandLineHelper(String[] args)
    {
        cl = createCommandLine();
        cl.parse(args);
    }

    private static CommandLine createCommandLine()
    {
        final CommandLine cl = new CommandLine();
        cl.addOption("flf", "fileListFile", "<file-with-list-of-files>",
            "relative or absolute path to a file containing a list of classes and other files (usually *.jdo) to enhance (one per line) - file is DELETED when read");
        cl.addOption("pu", "persistenceUnit", "<name-of-persistence-unit>", "name of the persistence unit to enhance");
        cl.addOption("dir", "directory", "<name-of-directory>", "name of the directory containing things to enhance");

        cl.addOption("d", "dest", "<directory>", "output directory");
        cl.addOption("checkonly", "checkonly", null, "only check if the class is enhanced");
        cl.addOption("q", "quiet", null, "no output");
        cl.addOption("v", "verbose", null, "verbose output");
        cl.addOption("api", "api", "<api-name>", "API Name (JDO, JPA, etc)");

        cl.addOption("ignoreMetaDataForMissingClasses", "ignoreMetaDataForMissingClasses", null, "Ignore metadata for classes that are missing?");
        cl.addOption("alwaysDetachable", "alwaysDetachable", null, "Always detachable?");

        cl.addOption("generatePK", "generatePK", "<generate-pk>", "Generate PK class where needed?");
        cl.addOption("generateConstructor", "generateConstructor", "<generate-constructor>", "Generate default constructor where needed?");
        cl.addOption("detachListener", "detachListener", "<detach-listener>", "Use Detach Listener?");
        return cl;
    }

    public boolean isQuiet()
    {
        return cl.hasOption("q");
    }

    public boolean isVerbose()
    {
        return cl.hasOption("v");
    }

    public boolean isValidating()
    {
        return cl.hasOption("checkonly") ? true : false;
    }

    public String getPersistenceUnitName()
    {
        return cl.hasOption("pu") ? cl.getOptionArg("pu") : null;
    }

    public String getDirectory()
    {
        return cl.hasOption("dir") ? cl.getOptionArg("dir") : null;
    }

    /**
     * Gets the files to be enhanced.
     * <p>
     * This is either the list of default arguments (i.e. program arguments without a "-"-prefix) or the
     * contents of the file-list-file passed as "-flf" argument. If a file-list-file was specified, the
     * default arguments are ignored.
     * @return the files to be enhanced. Never <code>null</code>.
     */
    public String[] getFiles()
    {
        if (files == null)
        {
            final String fileListFile = getFileListFile();
            if (fileListFile != null && !fileListFile.isEmpty())
            {
                final List<String> fileList = readAndDeleteFileListFile();
                files = fileList.toArray(new String[fileList.size()]);
            }
            else
                files = cl.getDefaultArgs();
        }
        return files;
    }

    protected String getFileListFile()
    {
        return cl.hasOption("flf") ? cl.getOptionArg("flf") : null;
    }

    public DataNucleusEnhancer createDataNucleusEnhancer()
    {
        // Create the DataNucleusEnhancer to the required API/enhancer
        String apiName = cl.hasOption("api") ? cl.getOptionArg("api") : "JDO";

        // TODO Add a way of defining input properties for startup
        Properties props = new Properties();
        props.setProperty("datanucleus.plugin.allowUserBundles", "true");
        if (cl.hasOption("alwaysDetachable"))
        {
            props.setProperty(PropertyNames.PROPERTY_METADATA_ALWAYS_DETACHABLE, "true");
        }
        if (cl.hasOption("ignoreMetaDataForMissingClasses"))
        {
            props.setProperty(PropertyNames.PROPERTY_METADATA_IGNORE_METADATA_FOR_MISSING_CLASSES, "true");
        }

        final DataNucleusEnhancer enhancer = new DataNucleusEnhancer(apiName, props);

        configureQuietAndVerbose(enhancer);

        logEnhancerVersion(enhancer, apiName);
        configureDestination(enhancer);
        configureGenerateConstructor(enhancer);
        configureGeneratePK(enhancer);
        configureDetachListener(enhancer);
        logClasspath(enhancer);

        return enhancer;
    }

    private void configureQuietAndVerbose(DataNucleusEnhancer enhancer)
    {
        if (!isQuiet())
        {
            if (isVerbose()) // Verbose only recognised when not quiet
            {
                enhancer.setVerbose(true);
            }
            enhancer.setSystemOut(true);
        }
    }

    private void configureDestination(DataNucleusEnhancer enhancer)
    {
        if (cl.hasOption("d"))
        {
            String destination = cl.getOptionArg("d");
            File tmp = new File(destination);
            if (tmp.exists())
            {
                if (!tmp.isDirectory())
                {
                    System.err.println(destination + " is not directory. please set directory.");
                    System.exit(1);
                }
            }
            else
            {
                tmp.mkdirs();
            }
            enhancer.setOutputDirectory(destination);
        }
    }

    private void configureGenerateConstructor(DataNucleusEnhancer enhancer)
    {
        if (cl.hasOption("generateConstructor"))
        {
            String val = cl.getOptionArg("generateConstructor");
            if (val.equalsIgnoreCase("false"))
            {
                enhancer.setGenerateConstructor(false);
            }
        }
    }

    private void configureGeneratePK(DataNucleusEnhancer enhancer)
    {
        if (cl.hasOption("generatePK"))
        {
            String val = cl.getOptionArg("generatePK");
            if (val.equalsIgnoreCase("false"))
            {
                enhancer.setGeneratePK(false);
            }
        }
    }

    private void configureDetachListener(DataNucleusEnhancer enhancer)
    {
        if (cl.hasOption("detachListener"))
        {
            String val = cl.getOptionArg("detachListener");
            if (val.equalsIgnoreCase("true"))
            {
                enhancer.setDetachListener(true);
            }
        }
    }

    private void logEnhancerVersion(DataNucleusEnhancer enhancer, String apiName)
    {
        String msg = Localiser.msg("005000", enhancer.getEnhancerVersion(), apiName);
        LOGGER.info(msg);
        if (!isQuiet())
        {
            System.out.println(msg);
        }
    }

    private void logClasspath(DataNucleusEnhancer enhancer)
    {
        // Debug Info : CLASSPATH
        LOGGER.debug(Localiser.msg("005001"));
        if (enhancer.isVerbose())
        {
            System.out.println(Localiser.msg("005001"));
        }
        StringTokenizer tokeniser = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        while (tokeniser.hasMoreTokens())
        {
            String entry = Localiser.msg("005002", tokeniser.nextToken());
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(entry);
            }
            if (enhancer.isVerbose())
            {
                System.out.println(entry);
            }
        }
        if (enhancer.isVerbose())
        {
            System.out.flush();
        }
    }

    /**
     * Reads the file-list-file.
     * <p>
     * This file serves as replacement for directly passing the files to be enhanced (classes or *.jdo files)
     * to the enhancer as program arguments. It must be UTF-8-encoded and it must contain one file per line.
     * <p>
     * See: <a href="http://www.datanucleus.org/servlet/jira/browse/NUCACCECLIPSE-11">NUCACCECLIPSE-11</a>
     * @return the contents of the file-list-file. Never <code>null</code>. Empty lines and comments are
     * filtered out.
     */
    private List<String> readAndDeleteFileListFile()
    {
        final String fileListFile = getFileListFile();
        if (isVerbose())
            System.out.println("Reading fileListFile: " + fileListFile);

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Reading fileListFile: " + fileListFile);

        final File flf = new File(fileListFile);
        if (!flf.isFile())
        {
            System.err.println(fileListFile + " is not an existing file. please set fileListFile (argument '-flf') to a valid file path.");
            System.exit(2);
        }
        List<String> result = new ArrayList<String>();
        try
        {
            InputStream in = new FileInputStream(flf);
            try
            {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                String line;
                while (null != (line = r.readLine()))
                {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#"))
                        result.add(line);
                }
                r.close();
            }
            finally
            {
                in.close();
            }
            flf.delete();
        }
        catch (IOException e)
        {
            System.err.println(fileListFile + " could not be read: " + e.getMessage());
            System.exit(3);
        }
        return result;
    }
}