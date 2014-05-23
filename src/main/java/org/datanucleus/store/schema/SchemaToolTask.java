/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
2004 Andy Jefferson - added sysproperty and classpath input options
2004 Andy Jefferson - changed to be derived from Java taskdef
2004 Andy Jefferson - removed redundant methods. Changed to se fork=true always
    ...
**********************************************************************/
package org.datanucleus.store.schema;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;
import org.datanucleus.store.schema.SchemaTool.Mode;
import org.datanucleus.util.Localiser;

/**
 * SchemaTool Ant Task. Accepts the following parameters
 * <UL>
 * <LI><B>mode</B> Mode of operation (<B>"create"</B>, "delete", "deletecreate", "validate", "dbinfo", "schemainfo", etc).</LI>
 * <LI><B>verbose</B> Verbose output.</LI>
 * <LI><B>props</B> Name of a properties file for use in SchemaTool (PMF properties)</LI>
 * <LI><B>ddlFile</B> Name of a file to output the DDL into</LI>
 * <LI><B>completeDdl</B> Whether to output complete DDL (otherwise just the missing tables/constraints)</LI>
 * <LI><B>persistenceUnit</B> Name of a persistence-unit to manage the schema for</LI>
 * </UL>
 */
public class SchemaToolTask extends Java
{
    /** Operating mode */
    private Mode mode = Mode.CREATE;

    /** Schema name (optional, for when using CREATE_SCHEMA, DELETE_SCHEMA). */
    private String schemaName;

    /** Filesets of files (mapping or class) to be used in generating the schema. */
    List<FileSet> filesets = new ArrayList<FileSet>();

    /**
     * Constructor.
     */
    public SchemaToolTask()
    {
        setClassname("org.datanucleus.store.schema.SchemaTool");
        setFork(true); // Default to fork=true
    }

    /**
     * Execute method, to execute the task.
     * @throws BuildException if any error happens while running the task
     **/
    public void execute()
    throws BuildException
    {
        if (mode == Mode.CREATE_SCHEMA)
        {
            if (schemaName == null)
            {
                throw new BuildException("If using 'create schema' then need to set schemaName");
            }
            createArg().setValue("-createSchema " + schemaName);
        }
        else if (mode == Mode.DELETE_SCHEMA)
        {
            if (schemaName == null)
            {
                throw new BuildException("If using 'create schema' then need to set schemaName");
            }
            createArg().setValue("-deleteSchema " + schemaName);
        }
        else if (mode == Mode.CREATE)
        {
            createArg().setValue("-create");
        }
        else if (mode == Mode.DELETE)
        {
            createArg().setValue("-delete");
        }
        else if (mode == Mode.DELETE_CREATE)
        {
            createArg().setValue("-deletecreate");
        }
        else if (mode == Mode.VALIDATE)
        {
            createArg().setValue("-validate");
        }
        else if (mode == Mode.DATABASE_INFO)
        {
            createArg().setValue("-dbinfo");
        }
        else if (mode == Mode.SCHEMA_INFO)
        {
            createArg().setValue("-schemainfo");
        }

        File[] files = getFiles();
        for (int i=0; i<files.length; i++)
        {
            createArg().setFile(files[i]);
        }

        super.execute();
    }

    /**
     * Add a fileset. @see ant manual
     * @param fs the FileSet
     */
    public void addFileSet(FileSet fs)
    {
        filesets.add(fs);
    }

    protected File[] getFiles()
    {
        List<File> v = new ArrayList<File>();

        Iterator<FileSet> filesetIter = filesets.iterator();
        while (filesetIter.hasNext())
        {
            FileSet fs = filesetIter.next();
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());
            ds.scan();
            String[] f = ds.getIncludedFiles();
            for (int j = 0; j < f.length; j++)
            {
                String pathname = f[j];
                File file = new File(ds.getBasedir(), pathname);
                file = getProject().resolveFile(file.getPath());
                v.add(file);
            }
        }

        return v.toArray(new File[v.size()]);
    }

    /**
     * set verbose
     * @param verbose Whether to give verbose output
     */
    public void setVerbose(boolean verbose)
    {
        if (verbose)
        {
            createArg().setValue("-v");
            log("SchemaTool verbose: " + verbose, Project.MSG_VERBOSE);
        }
    }

    /**
     * Get persistence properties from a file
     * @param propsFileName Name of props file
     */
    public void setProps(String propsFileName)
    {
        if (propsFileName != null && propsFileName.length() > 0)
        {
            createArg().setLine("-props " + propsFileName);
            log("SchemaTool props: " + propsFileName, Project.MSG_VERBOSE);
        }
    }

    /**
     * Set the file to output DDL to
     * @param file Name of DDL file
     */
    public void setDdlFile(String file)
    {
        if (file != null && file.length() > 0)
        {
            createArg().setLine("-ddlFile " + file);
            log("SchemaTool ddlFile: " + file, Project.MSG_VERBOSE);
        }
    }

    /**
     * Set the schema name (for use with CREATE_SCHEMA/DELETE_SCHEMA methods).
     * @param schemaName Name of the schema
     */
    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

    /**
     * Mutator for whether to output complete DDL.
     * @param complete Whether to give complete DDL
     */
    public void setCompleteDdl(boolean complete)
    {
        if (complete)
        {
            createArg().setValue("-completeDdl");
            log("SchemaTool completeDdl: " + complete, Project.MSG_VERBOSE);
        }
    }

    /**
     * Mutator for whether to include any auto-start mechanism in schema operations.
     * @param include Whether to include auto-start mechanisms
     */
    public void setIncludeAutoStart(boolean include)
    {
        if (include)
        {
            createArg().setValue("-includeAutoStart");
            log("SchemaTool includeAutoStart: " + include, Project.MSG_VERBOSE);
        }
    }

    /**
     * Set the name of the persistence unit to manage
     * @param unitName Name of persistence-unit
     */
    public void setPersistenceUnit(String unitName)
    {
        if (unitName != null && unitName.length() > 0)
        {
            createArg().setLine("-pu " + unitName);
            log("SchemaTool pu: " + unitName, Project.MSG_VERBOSE);
        }
    }

    /**
     * Set the API Adapter
     * @param api API Adapter
     */
    public void setApi(String api)
    {
        if (api != null && api.length() > 0)
        {
            createArg().setValue("-api");
            createArg().setValue(api);
            log("SchemaTool api: " + api, Project.MSG_VERBOSE);
        }
    }

    /**
     * Sets the mode of operation.
     * @param mode The mode of operation ("create", "delete", "deletecreate", "validate", "dbinfo", "schemainfo")
     */
    public void setMode(String mode)
    {
        if (mode == null)
        {
            return;
        }
        if (mode.equalsIgnoreCase("createSchema"))
        {
            this.mode = Mode.CREATE_SCHEMA;
        }
        else if (mode.equalsIgnoreCase("deleteSchema"))
        {
            this.mode = Mode.DELETE_SCHEMA;
        }
        else if (mode.equalsIgnoreCase("create"))
        {
            this.mode = Mode.CREATE;
        }
        else if (mode.equalsIgnoreCase("delete"))
        {
            this.mode = Mode.DELETE;
        }
        else if (mode.equalsIgnoreCase("deletecreate"))
        {
            this.mode = Mode.DELETE_CREATE;
        }
        else if (mode.equalsIgnoreCase("validate"))
        {
            this.mode = Mode.VALIDATE;
        }
        else if (mode.equalsIgnoreCase("dbinfo"))
        {
            this.mode = Mode.DATABASE_INFO;
        }        
        else if (mode.equalsIgnoreCase("schemainfo"))
        {
            this.mode = Mode.SCHEMA_INFO;
        }        
        else
        {
            System.err.println(Localiser.msg("014036"));
        }
    }
}