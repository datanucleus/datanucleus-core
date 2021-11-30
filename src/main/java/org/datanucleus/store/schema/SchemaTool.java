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
2004 Joerg Van Frantzius - changes to support a form of DDL output
2004 Erik Bengtson - dbinfo() mode
2004 Andy Jefferson - added "mapping" property to allow ORM files
2010 Andy Jefferson - rewritten the commandline interface to not need Context etc
    ...
**********************************************************************/
package org.datanucleus.store.schema;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.datanucleus.AbstractNucleusContext;
import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PersistenceNucleusContextImpl;
import org.datanucleus.Configuration;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.FileMetaData;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.metadata.MetaDataUtils;
import org.datanucleus.metadata.PersistenceUnitMetaData;
import org.datanucleus.store.StoreManager;
import org.datanucleus.util.CommandLine;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.PersistenceUtils;
import org.datanucleus.util.StringUtils;

/**
 * SchemaTool providing an interface for the maintenance of schemas.
 * These utilities include:-
 * <ul>
 * <li>creation of a schema/catalog in the datastore</li>
 * <li>deletion of a schema/catalog in the datastore</li>
 * <li>creation of tables representing classes specified in input data</li>
 * <li>deletion of tables representing classes specified in input data</li>
 * <li>validation of tables representing classes specified in input data</li>
 * <li>details about the datastore</li>
 * </ul>
 */
public class SchemaTool
{
    public static final NucleusLogger LOGGER = NucleusLogger.getLoggerInstance("DataNucleus.SchemaTool");

    public static final String OPTION_CREATE_DATABASE = "createDatabase";
    public static final String OPTION_DELETE_DATABASE = "deleteDatabase";
    public static final String OPTION_CREATE_TABLES_FOR_CLASSES = "create";
    public static final String OPTION_DELETE_TABLES_FOR_CLASSES = "delete";
    public static final String OPTION_DELETE_CREATE_TABLES_FOR_CLASSES = "deletecreate";
    public static final String OPTION_VALIDATE_TABLES_FOR_CLASSES = "validate";
    public static final String OPTION_DBINFO = "dbinfo";
    public static final String OPTION_SCHEMAINFO = "schemainfo";
    public static final String OPTION_DDL_FILE = "ddlFile";
    public static final String OPTION_COMPLETE_DDL = "completeDdl";
    public static final String OPTION_INCLUDE_AUTO_START = "includeAutoStart";
    public static final String OPTION_API = "api";
    public static final String OPTION_CATALOG_NAME = "catalog";
    public static final String OPTION_SCHEMA_NAME = "schema";

    public enum Mode
    {
        CREATE_DATABASE,
        DELETE_DATABASE,
        CREATE,
        DELETE,
        DELETE_CREATE,
        VALIDATE,
        DATABASE_INFO,
        SCHEMA_INFO
    }

    /** Name of the persistence API to use. */
    private String apiName = "JDO";

    /** Name of the schema (for use with createDatabase, deleteDatabase modes). */
    private String schemaName = null;

    /** Name of the schema (for use with createDatabase, deleteDatabase modes). */
    private String catalogName = null;

    /** Name of a file in which to put the DDL (or null if wanting to execute in the datastore). */
    private String ddlFilename = null;

    /** When generating DDL to a file, whether to generate complete DDL, or just for missing components. */
    private boolean completeDdl = false;

    /** When updating the schema, whether to include any auto-start mechanism. */
    private boolean includeAutoStart = false;

    /** Whether to operate in verbose mode. */
    private boolean verbose = false;

    /**
     * Entry method when invoked from the command line.
     * @param args List of options for processing by the available methods in this class.
     * @throws Exception If an error occurs in operation
     */
    public static void main(String[] args) throws Exception
    {
        SchemaTool tool = new SchemaTool();

        CommandLine cmd = new CommandLine();
        cmd.addOption(OPTION_CREATE_DATABASE, OPTION_CREATE_DATABASE, null, Localiser.msg("014024"));
        cmd.addOption(OPTION_DELETE_DATABASE, OPTION_DELETE_DATABASE, null, Localiser.msg("014025"));

        cmd.addOption(OPTION_CREATE_TABLES_FOR_CLASSES, OPTION_CREATE_TABLES_FOR_CLASSES, null, Localiser.msg("014026"));
        cmd.addOption(OPTION_DELETE_TABLES_FOR_CLASSES, OPTION_DELETE_TABLES_FOR_CLASSES, null, Localiser.msg("014027"));
        cmd.addOption(OPTION_DELETE_CREATE_TABLES_FOR_CLASSES, OPTION_DELETE_CREATE_TABLES_FOR_CLASSES, null, Localiser.msg("014044"));
        cmd.addOption(OPTION_VALIDATE_TABLES_FOR_CLASSES, OPTION_VALIDATE_TABLES_FOR_CLASSES, null, Localiser.msg("014028"));

        cmd.addOption(OPTION_DBINFO, OPTION_DBINFO, null, Localiser.msg("014029"));
        cmd.addOption(OPTION_SCHEMAINFO, OPTION_SCHEMAINFO, null, Localiser.msg("014030"));
        cmd.addOption("help", "help", null, Localiser.msg("014033"));

        cmd.addOption(OPTION_DDL_FILE, OPTION_DDL_FILE, "ddlFile", Localiser.msg("014031"));
        cmd.addOption(OPTION_COMPLETE_DDL, OPTION_COMPLETE_DDL, null, Localiser.msg("014032"));
        cmd.addOption(OPTION_INCLUDE_AUTO_START, OPTION_INCLUDE_AUTO_START, null, "Include Auto-Start Mechanisms");
        cmd.addOption(OPTION_API, OPTION_API, "api", "API Adapter (JDO, JPA, etc)");
        cmd.addOption(OPTION_CATALOG_NAME, OPTION_CATALOG_NAME, "catalog", "CatalogName");
        cmd.addOption(OPTION_SCHEMA_NAME, OPTION_SCHEMA_NAME, "schema", "SchemaName");
        cmd.addOption("v", "verbose", null, "verbose output");
        cmd.addOption("pu", "persistenceUnit", "<persistence-unit>", "name of the persistence unit to handle the schema for");
        cmd.addOption("props", "properties", "props", "path to a properties file");
        cmd.addOption("ignoreMetaDataForMissingClasses", "ignoreMetaDataForMissingClasses", null, "Ignore metadata for classes that are missing?");

        cmd.parse(args);

        // Remaining command line args are filenames (class files, metadata files)
        String[] filenames = cmd.getDefaultArgs();

        if (cmd.hasOption("api"))
        {
            tool.setApi(cmd.getOptionArg("api"));
        }
        if (cmd.hasOption(OPTION_CATALOG_NAME))
        {
            tool.setCatalogName(cmd.getOptionArg(OPTION_CATALOG_NAME));
        }
        if (cmd.hasOption(OPTION_SCHEMA_NAME))
        {
            tool.setSchemaName(cmd.getOptionArg(OPTION_SCHEMA_NAME));
        }

        // Determine the mode of operation required
        String msg = null;
        Mode mode = Mode.CREATE;
        if (cmd.hasOption(OPTION_CREATE_TABLES_FOR_CLASSES))
        {
            mode = Mode.CREATE;
            msg = Localiser.msg("014000");
        }
        else if (cmd.hasOption(OPTION_DELETE_TABLES_FOR_CLASSES))
        {
            mode = Mode.DELETE;
            msg = Localiser.msg("014001");
        }
        else if (cmd.hasOption(OPTION_DELETE_CREATE_TABLES_FOR_CLASSES))
        {
            mode = Mode.DELETE_CREATE;
            msg = Localiser.msg("014045");
        }
        else if (cmd.hasOption(OPTION_VALIDATE_TABLES_FOR_CLASSES))
        {
            mode = Mode.VALIDATE;
            msg = Localiser.msg("014002");
        }
        else if (cmd.hasOption(OPTION_CREATE_DATABASE))
        {
            mode = Mode.CREATE_DATABASE;
            msg = Localiser.msg("014034", tool.getCatalogName(), tool.getSchemaName());
        }
        else if (cmd.hasOption(OPTION_DELETE_DATABASE))
        {
            mode = Mode.DELETE_DATABASE;
            msg = Localiser.msg("014035", tool.getCatalogName(), tool.getSchemaName());
        }
        else if (cmd.hasOption(OPTION_DBINFO))
        {
            mode = Mode.DATABASE_INFO;
            msg = Localiser.msg("014003");
        }
        else if (cmd.hasOption(OPTION_SCHEMAINFO))
        {
            mode = Mode.SCHEMA_INFO;
            msg = Localiser.msg("014004");
        }
        else if (cmd.hasOption("help"))
        {
            System.out.println(Localiser.msg("014023", cmd.toString()));
            System.exit(0);
        }
        LOGGER.info(msg);
        System.out.println(msg);

        // Extract the selected options
        String propsFileName = null;
        String persistenceUnitName = null;
        if (cmd.hasOption(OPTION_DDL_FILE))
        {
            tool.setDdlFile(cmd.getOptionArg(OPTION_DDL_FILE));
        }
        if (cmd.hasOption(OPTION_COMPLETE_DDL))
        {
            tool.setCompleteDdl(true);
        }
        if (cmd.hasOption(OPTION_INCLUDE_AUTO_START))
        {
            tool.setIncludeAutoStart(true);
        }
        if (cmd.hasOption("v"))
        {
            tool.setVerbose(true);
        }

        boolean ignoreMetaDataForMissingClasses = false;
        if (cmd.hasOption("ignoreMetaDataForMissingClasses"))
        {
            ignoreMetaDataForMissingClasses = true;
        }

        if (cmd.hasOption("pu"))
        {
            persistenceUnitName = cmd.getOptionArg("pu");
        }
        if (cmd.hasOption("props"))
        {
            propsFileName = cmd.getOptionArg("props");
        }

        // Classpath
        msg = Localiser.msg("014005");
        LOGGER.info(msg);
        if (tool.isVerbose())
        {
            System.out.println(msg);
        }
        StringTokenizer tokeniser = new StringTokenizer(System.getProperty("java.class.path"), File.pathSeparator);
        while (tokeniser.hasMoreTokens())
        {
            msg = Localiser.msg("014006", tokeniser.nextToken());
            LOGGER.info(msg);
            if (tool.isVerbose())
            {
                System.out.println(msg);
            }
        }
        if (tool.isVerbose())
        {
            System.out.println();
        }

        // DDL file
        String ddlFilename = tool.getDdlFile();
        if (ddlFilename != null)
        {
            msg = Localiser.msg(tool.getCompleteDdl() ? "014018" : "014019", ddlFilename);
            LOGGER.info(msg);
            if (tool.isVerbose())
            {
                System.out.println(msg);
                System.out.println();
            }
        }

        // Create a NucleusContext for use with this mode
        PersistenceNucleusContext nucleusCtx = null;
        try
        {
            Properties props = (propsFileName!=null) ? PersistenceUtils.setPropertiesUsingFile(propsFileName) : null;
            nucleusCtx = getNucleusContextForMode(mode, tool.getApi(), props, persistenceUnitName, ddlFilename, tool.isVerbose(), ignoreMetaDataForMissingClasses);
        }
        catch (Exception e)
        {
            // Unable to create a NucleusContext so likely input errors
            LOGGER.error("Error creating NucleusContext", e);
            System.out.println(Localiser.msg("014008", e.getMessage()));
            System.exit(1);
            return;
        }

        Set<String> classNames = null;
        if (mode != Mode.SCHEMA_INFO && mode != Mode.DATABASE_INFO)
        {
            // Find the names of the classes to be processed
            // This will load up all MetaData for the specified input and throw exceptions where errors are found
            try
            {
                MetaDataManager metaDataMgr = nucleusCtx.getMetaDataManager();
                ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(null);

                if (filenames == null && persistenceUnitName == null)
                {
                    msg = Localiser.msg("014007");
                    LOGGER.error(msg);
                    System.out.println(msg);
                    throw new NucleusUserException(msg);
                }

                FileMetaData[] filemds = null;
                if (persistenceUnitName != null)
                {
                    // Schema management via "persistence-unit"
                    msg = Localiser.msg("014015", persistenceUnitName);
                    LOGGER.info(msg);
                    if (tool.isVerbose())
                    {
                        System.out.println(msg);
                        System.out.println();
                    }

                    // The NucleusContext will have initialised the MetaDataManager with the persistence-unit
                    filemds = metaDataMgr.getFileMetaData();
                }
                else
                {
                    // Schema management via "Input Files" (metadata/class)
                    msg = Localiser.msg("014009");
                    LOGGER.info(msg);
                    if (tool.isVerbose())
                    {
                        System.out.println(msg);
                    }
                    for (int i = 0; i < filenames.length; i++)
                    {
                        String entry = Localiser.msg("014010", filenames[i]);
                        LOGGER.info(entry);
                        if (tool.isVerbose())
                        {
                            System.out.println(entry);
                        }
                    }
                    if (tool.isVerbose())
                    {
                        System.out.println();
                    }

                    LOGGER.debug(Localiser.msg("014011", "" + filenames.length));
                    filemds = MetaDataUtils.getFileMetaDataForInputFiles(metaDataMgr, clr, filenames);
                    LOGGER.debug(Localiser.msg("014012", "" + filenames.length));
                }

                classNames = new TreeSet<String>();
                if (filemds == null)
                {
                    msg = Localiser.msg("014021");
                    LOGGER.error(msg);
                    System.out.println(msg);
                    System.exit(2);
                    return;
                }
                for (int i=0;i<filemds.length;i++)
                {
                    for (int j=0;j<filemds[i].getNoOfPackages();j++)
                    {
                        for (int k=0;k<filemds[i].getPackage(j).getNoOfClasses();k++)
                        {
                            String className = filemds[i].getPackage(j).getClass(k).getFullClassName();
                            if (!classNames.contains(className))
                            {
                                classNames.add(className);
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                // Exception will have been logged and sent to System.out in "getFileMetaDataForInput()"
                System.exit(2);
                return;
            }
        }

        // Run SchemaTool
        StoreManager storeMgr = nucleusCtx.getStoreManager();
        if (!(storeMgr instanceof SchemaAwareStoreManager))
        {
            LOGGER.error("StoreManager of type " + storeMgr.getClass().getName() + " is not schema-aware so cannot be used with SchemaTool");
            System.exit(2);
            return;
        }
        SchemaAwareStoreManager schemaStoreMgr = (SchemaAwareStoreManager) storeMgr;

        try
        {
            if (mode == Mode.CREATE_DATABASE)
            {
                tool.createDatabase(schemaStoreMgr, tool.getCatalogName(), tool.getSchemaName());
            }
            else if (mode == Mode.DELETE_DATABASE)
            {
                tool.deleteDatabase(schemaStoreMgr, tool.getCatalogName(), tool.getSchemaName());
            }
            else if (mode == Mode.CREATE)
            {
                tool.createSchemaForClasses(schemaStoreMgr, classNames);
            }
            else if (mode == Mode.DELETE)
            {
                tool.deleteSchemaForClasses(schemaStoreMgr, classNames);
            }
            else if (mode == Mode.DELETE_CREATE)
            {
                tool.deleteSchemaForClasses(schemaStoreMgr, classNames);
                tool.createSchemaForClasses(schemaStoreMgr, classNames);
            }
            else if (mode == Mode.VALIDATE)
            {
                tool.validateSchemaForClasses(schemaStoreMgr, classNames);
            }
            else if (mode == Mode.DATABASE_INFO)
            {
                storeMgr.printInformation("DATASTORE", System.out);
            }
            else if (mode == Mode.SCHEMA_INFO)
            {
                storeMgr.printInformation("SCHEMA", System.out);
            }

            msg = Localiser.msg("014043");
            LOGGER.info(msg);
            System.out.println(msg);
        }
        catch (Exception e)
        {
            msg = Localiser.msg("014037", e.getMessage());
            System.out.println(msg);
            LOGGER.error(msg, e);
            System.exit(2);
            return;
        }
        finally
        {
            storeMgr.close();
        }
    }

    /**
     * Constructor
     */
    public SchemaTool()
    {
    }

    /**
     * Method to generate the properties to be used by SchemaTool.
     * This includes whether to create DDL to a file, and whether to include any auto-start mechanism
     * @return The properties to use with SchemaTool.
     */
    public Properties getPropertiesForSchemaTool()
    {
        Properties props = new Properties();
        if (getDdlFile() != null)
        {
            props.setProperty("ddlFilename", getDdlFile());
        }
        if (getCompleteDdl())
        {
            props.setProperty("completeDdl", "true");
        }
        if (getIncludeAutoStart())
        {
            props.setProperty("autoStartTable", "true");
        }
        return props;
    }

    public void createDatabase(SchemaAwareStoreManager storeMgr, String catalogName, String schemaName)
    {
        storeMgr.createDatabase(catalogName, schemaName, getPropertiesForSchemaTool());
    }

    public void deleteDatabase(SchemaAwareStoreManager storeMgr, String catalogName, String schemaName)
    {
        storeMgr.deleteDatabase(catalogName, schemaName, getPropertiesForSchemaTool());
    }

    public void createSchemaForClasses(SchemaAwareStoreManager storeMgr, Set<String> classNames)
    {
        storeMgr.createSchemaForClasses(classNames, getPropertiesForSchemaTool());
    }

    public void deleteSchemaForClasses(SchemaAwareStoreManager storeMgr, Set<String> classNames)
    {
        storeMgr.deleteSchemaForClasses(classNames, getPropertiesForSchemaTool());
    }

    public void validateSchemaForClasses(SchemaAwareStoreManager storeMgr, Set<String> classNames)
    {
        storeMgr.validateSchemaForClasses(classNames, getPropertiesForSchemaTool());
    }

    /**
     * Method to create a NucleusContext for the specified mode of SchemaTool
     * @param mode Mode of operation of SchemaTool
     * @param api Persistence API
     * @param userProps Map containing user provided properties (usually input via a file)
     * @param persistenceUnitName Name of the persistence-unit (if any)
     * @param ddlFile Name of a file to output DDL to
     * @param verbose Verbose mode
     * @return The NucleusContext to use
     * @throws NucleusException Thrown if an error occurs in creating the required NucleusContext
     */
    public static PersistenceNucleusContext getNucleusContextForMode(Mode mode, String api, Map userProps, String persistenceUnitName, String ddlFile, boolean verbose)
    {
        return getNucleusContextForMode(mode, api, userProps, persistenceUnitName, ddlFile, verbose, false);
    }

    /**
     * Method to create a NucleusContext for the specified mode of SchemaTool
     * @param mode Mode of operation of SchemaTool
     * @param api Persistence API
     * @param userProps Map containing user provided properties (usually input via a file)
     * @param persistenceUnitName Name of the persistence-unit (if any)
     * @param ddlFile Name of a file to output DDL to
     * @param verbose Verbose mode
     * @param ignoreMetaDataForMissingClasses Whether to ignore metadata for missing classes
     * @return The NucleusContext to use
     * @throws NucleusException Thrown if an error occurs in creating the required NucleusContext
     */
    public static PersistenceNucleusContext getNucleusContextForMode(Mode mode, String api, Map userProps, String persistenceUnitName, String ddlFile, boolean verbose,
            boolean ignoreMetaDataForMissingClasses)
    {
        // Extract any properties that affect NucleusContext startup
        Map startupProps = null;
        if (userProps != null)
        {
            // Possible properties to check for
            for (String startupPropName : AbstractNucleusContext.STARTUP_PROPERTIES)
            {
                if (userProps.containsKey(startupPropName))
                {
                    if (startupProps == null)
                    {
                        startupProps = new HashMap();
                    }
                    startupProps.put(startupPropName, userProps.get(startupPropName));
                }
            }
        }

        // Initialise the context for this API
        PersistenceNucleusContext nucleusCtx = new PersistenceNucleusContextImpl(api, startupProps);
        Configuration propConfig = nucleusCtx.getConfiguration();

        // Generate list of properties for SchemaTool usage
        Map props = new HashMap();

        // Get properties from PersistenceUnit first...
        PersistenceUnitMetaData pumd = null;
        if (persistenceUnitName != null)
        {
            props.put(PropertyNames.PROPERTY_PERSISTENCE_UNIT_NAME.toLowerCase(), persistenceUnitName);

            // Extract the persistence-unit metadata
            String filename = nucleusCtx.getConfiguration().getStringProperty(PropertyNames.PROPERTY_PERSISTENCE_XML_FILENAME);
            boolean validateXML = nucleusCtx.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_VALIDATE);
            boolean supportXMLNamespaces = nucleusCtx.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_NAMESPACE_AWARE);
            ClassLoaderResolver clr = nucleusCtx.getClassLoaderResolver(null);
            pumd = MetaDataUtils.getMetaDataForPersistenceUnit(nucleusCtx.getPluginManager(), filename, persistenceUnitName, validateXML, supportXMLNamespaces, clr);
            if (pumd != null)
            {
                // Add the properties for the unit
                if (pumd.getProperties() != null)
                {
                    // Ignore (schema) properties that are not needed by SchemaTool
                    pumd.getProperties().remove(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_ORDER);
                    pumd.getProperties().remove(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_CREATE_SCRIPT);
                    pumd.getProperties().remove(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_ORDER);
                    pumd.getProperties().remove(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_DROP_SCRIPT);
                    pumd.getProperties().remove(PropertyNames.PROPERTY_SCHEMA_GENERATE_DATABASE_MODE);

                    // These have not had chance to be converted to the DN internal property since straight from persistence.xml
                    pumd.getProperties().remove("javax.persistence.schema-generation.database.action");
                    pumd.getProperties().remove("javax.persistence.schema-generation.create-script-source");
                    pumd.getProperties().remove("javax.persistence.schema-generation.create-source");
                    pumd.getProperties().remove("javax.persistence.schema-generation.drop-script-source");
                    pumd.getProperties().remove("javax.persistence.schema-generation.drop-source");

                    props.putAll(pumd.getProperties());
                }
            }
            else
            {
                throw new NucleusUserException("SchemaTool has been specified to use persistence-unit with name " + persistenceUnitName + " but none was found with that name");
            }

            if (api.equalsIgnoreCase("JPA") || api.equalsIgnoreCase("Jakarta")) // TODO Put this in API so we dont need to check on strings
            {
                pumd.clearJarFiles(); // Don't use JARs when in JavaSE for JPA
            }
        }

        // Add/override with user properties
        if (userProps != null)
        {
            // Properties specified by the user in a file
            for (Object key : userProps.keySet())
            {
                String propName = (String)key;
                props.put(propName.toLowerCase(Locale.ENGLISH), userProps.get(propName));
            }
        }

        // Finally add/override with system properties (only support particular ones, and in correct case)
        String[] propNames = 
        {
                PropertyNames.PROPERTY_CONNECTION_URL,
                PropertyNames.PROPERTY_CONNECTION_DRIVER_NAME,
                PropertyNames.PROPERTY_CONNECTION_USER_NAME,
                PropertyNames.PROPERTY_CONNECTION_PASSWORD,
                PropertyNames.PROPERTY_MAPPING,
                "javax.jdo.option.ConnectionURL",
                "javax.jdo.option.ConnectionDriverName",
                "javax.jdo.option.ConnectionUserName",
                "javax.jdo.option.ConnectionPassword",
                "javax.jdo.option.Mapping",
                "javax.persistence.jdbc.url",
                "javax.persistence.jdbc.driver",
                "javax.persistence.jdbc.user",
                "javax.persistence.jdbc.password",
        };
        for (int i=0;i<propNames.length;i++)
        {
            if (System.getProperty(propNames[i]) != null)
            {
                props.put(propNames[i].toLowerCase(Locale.ENGLISH), System.getProperty(propNames[i]));
            }
        }
        props.put(PropertyNames.PROPERTY_AUTOSTART_MECHANISM.toLowerCase(), "None"); // Interferes with usage

        // Tag on the mandatory props that we must have for each mode
        if (mode == Mode.CREATE)
        {
            if (ddlFile != null)
            {
                // the tables must not be created in the DB, so do not validate (DDL is being output to a file)
                props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_TABLES.toLowerCase(), "false");
                props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_COLUMNS.toLowerCase(), "false");
                props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS.toLowerCase(), "false");
            }
            props.remove(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_ALL.toLowerCase()); // use tables/columns/constraints settings
            if (!props.containsKey(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES.toLowerCase()))
            {
                props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES.toLowerCase(), "true");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS.toLowerCase()))
            {
                props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS.toLowerCase(), "true");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS.toLowerCase()))
            {
                props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS.toLowerCase(), "true");
            }
            props.put(PropertyNames.PROPERTY_DATASTORE_READONLY.toLowerCase(), "false");
            props.put("datanucleus.rdbms.checkexisttablesorviews", "true");
        }
        else if (mode == Mode.DELETE)
        {
            props.put(PropertyNames.PROPERTY_DATASTORE_READONLY.toLowerCase(), "false");
        }
        else if (mode == Mode.DELETE_CREATE)
        {
            if (ddlFile != null)
            {
                // the tables must not be created in the DB, so do not validate (DDL is being output to a file)
                props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_TABLES.toLowerCase(), "false");
                props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_COLUMNS.toLowerCase(), "false");
                props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS.toLowerCase(), "false");
            }
            props.remove(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_ALL.toLowerCase()); // use tables/columns/constraints settings
            if (!props.containsKey(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES.toLowerCase()))
            {
                props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES.toLowerCase(), "true");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS.toLowerCase()))
            {
                props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS.toLowerCase(), "true");
            }
            if (!props.containsKey(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS.toLowerCase()))
            {
                props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS.toLowerCase(), "true");
            }
            props.put(PropertyNames.PROPERTY_DATASTORE_READONLY.toLowerCase(), "false");
            props.put("datanucleus.rdbms.checkexisttablesorviews", "true");
        }
        else if (mode == Mode.VALIDATE)
        {
            props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_ALL.toLowerCase(), "false");
            props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_TABLES.toLowerCase(), "false");
            props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_CONSTRAINTS.toLowerCase(), "false");
            props.put(PropertyNames.PROPERTY_SCHEMA_AUTOCREATE_COLUMNS.toLowerCase(), "false");
            props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_TABLES.toLowerCase(), "true");
            props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_COLUMNS.toLowerCase(), "true");
            props.put(PropertyNames.PROPERTY_SCHEMA_VALIDATE_CONSTRAINTS.toLowerCase(), "true");
        }
        if (ignoreMetaDataForMissingClasses)
        {
            props.put(PropertyNames.PROPERTY_METADATA_IGNORE_METADATA_FOR_MISSING_CLASSES, "true");
        }

        // Apply remaining persistence properties
        propConfig.setPersistenceProperties(props);

        if (pumd != null)
        {
            // Initialise the MetaDataManager with all files/classes for this persistence-unit
            // This is done now that all persistence properties are set (including the persistence-unit props)
            nucleusCtx.getMetaDataManager().loadPersistenceUnit(pumd, null);
        }

        // Initialise the NucleusContext for use
        nucleusCtx.initialise();

        if (verbose)
        {
            String msg = Localiser.msg("014020");
            LOGGER.info(msg);
            System.out.println(msg);

            // TODO Some persistence properties will be stored against the StoreManager
            Map<String,Object> pmfProps = propConfig.getPersistenceProperties();
            Set<String> keys = pmfProps.keySet();
            List<String> keyNames = new ArrayList<String>(keys);
            Collections.sort(keyNames);
            Iterator keyNamesIter = keyNames.iterator();
            while (keyNamesIter.hasNext())
            {
                String key = (String)keyNamesIter.next();
                Object value = pmfProps.get(key);
                boolean display = true;
                if (!key.startsWith("datanucleus"))
                {
                    display = false;
                }
                else if (key.equals(PropertyNames.PROPERTY_CONNECTION_PASSWORD.toLowerCase()))
                {
                    // Don't show passwords
                    display = false;
                }
                else if (value == null)
                {
                    display = false;
                }
                else if (value instanceof String && StringUtils.isWhitespace((String)value))
                {
                    display = false;
                }

                if (display)
                {
                    // Print the property to sysout
                    msg = Localiser.msg("014022", key, value);
                    LOGGER.info(msg);
                    System.out.println(msg);
                }
            }
            System.out.println();            
        }

        return nucleusCtx;
    }

    /**
     * Accessor for the metadata API (JDO, JPA) in use (metadata definition)
     * @return the API
     */
    public String getApi()
    {
        return apiName;
    }

    /**
     * Mutator for the metadata API (JDO, JPA)
     * @param api the API
     * @return The SchemaTool instance
     */
    public SchemaTool setApi(String api)
    {
        this.apiName = api;
        return this;
    }

    /**
     * @return the verbose
     */
    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * @param verbose the verbose to set
     * @return The SchemaTool instance
     */
    public SchemaTool setVerbose(boolean verbose)
    {
        this.verbose = verbose;
        return this;
    }

    public String getSchemaName()
    {
        return schemaName;
    }
    public SchemaTool setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
        return this;
    }

    public String getCatalogName()
    {
        return catalogName;
    }
    public SchemaTool setCatalogName(String catalogName)
    {
        this.catalogName = catalogName;
        return this;
    }

    public String getDdlFile()
    {
        return ddlFilename;
    }
    public SchemaTool setDdlFile(String file)
    {
        this.ddlFilename = file;
        return this;
    }

    public SchemaTool setCompleteDdl(boolean completeDdl)
    {
        this.completeDdl = completeDdl;
        return this;
    }
    public boolean getCompleteDdl()
    {
        return completeDdl;
    }

    public SchemaTool setIncludeAutoStart(boolean include)
    {
        this.includeAutoStart = include;
        return this;
    }
    public boolean getIncludeAutoStart()
    {
        return includeAutoStart;
    }
}