/**********************************************************************
Copyright (c) 2004 Kikuchi Kousuke and others. All rights reserved.
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
2004 Erik Bengtson  - adapted to existing ant "command line" 
2004 Andy Jefferson - adapted to JPOX usage
2004 Andy Jefferson - updated to allow fork=true
2004 Andy Jefferson - removed overridden methods since redundant.
    ...
**********************************************************************/
package org.datanucleus.enhancer;

import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.FileSet;

/**
 * Enhancer Ant Task.
 */
public class EnhancerTask extends Java
{
    private File dir;

    /** Only runs this task if the property is set. */
    private String ifpropertyset;

    /** The suffixes of the files to use. Defaults to files suffixed "jdo". */
    private String fileSuffixes = "jdo";

    /** Filesets of metadata files or class files to be enhanced. */
    Vector<FileSet> filesets = new Vector<FileSet>();

    /**
     * Default constructor
     */
    public EnhancerTask()
    {
        setClassname("org.datanucleus.enhancer.DataNucleusEnhancer");
        setFork(true); // Default to fork=true
    }

    /**
     * Execution method
     * @throws BuildException Thrown when an error occurs when processing the task
     */
    public void execute() throws BuildException
    {
        if (ifpropertyset != null)
        {
            if (getProject().getProperty(ifpropertyset) == null)
            {
                log("Property " + ifpropertyset + " is not set. This task will not execute.", Project.MSG_VERBOSE);
                return;
            }
        }

        File[] files = getFiles();
        if (files.length == 0)
        {
            log("Scanning for files with suffixes: " + fileSuffixes, Project.MSG_VERBOSE);
            StringTokenizer token = new StringTokenizer(fileSuffixes, ",");
            while (token.hasMoreTokens())
            {
                DirectoryScanner ds = getDirectoryScanner(getDir());
                ds.setIncludes(new String[]{"**\\*." + token.nextToken()});
                ds.scan();
                for (int i = 0; i < ds.getIncludedFiles().length; i++)
                {
                    createArg().setFile(new File(getDir(), ds.getIncludedFiles()[i]));
                }
            }
        }
        else
        {
            log("FileSet has " + files.length + " files. Enhancer task will not scan for additional files.", Project.MSG_VERBOSE);
            for (int i = 0; i < files.length; i++)
            {
                createArg().setFile(files[i]);
            }
        }

        super.execute();
    }

    /**
     * Whether to just check the enhancement state
     * @param checkonly Whether to just check
     */
    public void setCheckonly(boolean checkonly)
    {
        if (checkonly)
        {
            createArg().setValue("-checkonly");
            createArg().setValue(""+checkonly);
            log("Enhancer checkonly: " + checkonly, Project.MSG_VERBOSE);
        }
    }

    /**
     * Whether to allow generation of PKs where required.
     * @param flag Allow PK generation
     */
    public void setGeneratePK(boolean flag)
    {
        if (flag)
        {
            createArg().setValue("-generatePK");
            createArg().setValue(""+flag);
            log("Enhancer generatePK: " + flag, Project.MSG_VERBOSE);
        }
    }

    /**
     * Whether to allow generation of default constructor where required.
     * @param flag Allow default constructor addition
     */
    public void setGenerateConstructor(boolean flag)
    {
        if (flag)
        {
            createArg().setValue("-generateConstructor");
            createArg().setValue(""+flag);
            log("Enhancer generateConstructor: " + flag, Project.MSG_VERBOSE);
        }
    }

    /**
     * Whether to use detach listener.
     * @param flag to detach listener use  
     */
    public void setDetachListener(boolean flag)
    {
        if (flag)
        {
            createArg().setValue("-detachListener");
            createArg().setValue(""+flag);
            log("Enhancer detachListener: " + flag, Project.MSG_VERBOSE);
        }
    }

    private DirectoryScanner getDirectoryScanner(File dir)
    {
        FileSet fileset = new FileSet();
        fileset.setDir(dir);
        return fileset.getDirectoryScanner(getProject());
    }

    /**
     * set output directory
     * @param destdir output dir
     */
    public void setDestination(File destdir)
    {
        if (destdir != null && destdir.isDirectory())
        {
            createArg().setValue("-d");
            createArg().setFile(destdir);
            log("Enhancer destdir: " + destdir, Project.MSG_VERBOSE);
        }
        else
        {
            log("Ignoring destination: " + destdir, Project.MSG_WARN);
        }
    }

    /**
     * set API Adapter
     * @param api API Adapter
     */
    public void setApi(String api)
    {
        if (api != null && api.length() > 0)
        {
            createArg().setValue("-api");
            createArg().setValue(api);
            log("Enhancer api: " + api, Project.MSG_VERBOSE);
        }
    }

    /**
     * Set the persistence-unit name to enhance
     * @param unit Name of the persistence-unit to enhance
     */
    public void setPersistenceUnit(String unit)
    {
        if (unit != null && unit.length() > 0)
        {
            createArg().setValue("-pu");
            createArg().setValue(unit);
            log("Enhancer pu: " + unit, Project.MSG_VERBOSE);
        }
    }

    /**
     * Sets the root dir for looking for files
     * @param dir the root dir
     */
    public void setDir(File dir)
    {
        this.dir = dir;
    }

    /**
     * Gets the root dir for looking for files
     * @return the root dir
     */
    public File getDir()
    {
        return dir == null ? getProject().getBaseDir() : dir;
    }

    /**
     * Set one or more file suffixes for the input files. Suffixes are separated with a comma(,) 
     * @param suffixes the suffices
     */
    public void setFileSuffixes(String suffixes)
    {
        this.fileSuffixes = suffixes;
    }

    /**
     * set always detachable.
     * @param detachable Whether all enhanced classes should be detachable
     */
    public void setAlwaysDetachable(boolean detachable)
    {
        if (detachable)
        {
            createArg().setValue("-alwaysDetachable");
            log("Enhancer alwaysDetachable: " + detachable, Project.MSG_VERBOSE);
        }
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
            log("Enhancer verbose: " + verbose, Project.MSG_VERBOSE);
        }
    }

    /**
     * set quiet
     * @param quiet Whether to give no output
     */
    public void setQuiet(boolean quiet)
    {
        if (quiet)
        {
            createArg().setValue("-q");
            log("Enhancer quiet: " + quiet, Project.MSG_VERBOSE);
        }
    }

    /**
     * Add a fileset. @see ant manual
     * @param fs the FileSet
     */
    public void addFileSet(FileSet fs)
    {
        filesets.addElement(fs);
    }
    
    protected File[] getFiles()
    {
        Vector<File> v = new Vector<File>();
        final int size = filesets.size();
        for (int i = 0; i < size; i++)
        {
            FileSet fs = filesets.elementAt(i);
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
        File[] files = new File[v.size()];
        v.copyInto(files);
        return files;
    }
    
    /**
     * Executes this task only if the property is set
     * @param ifpropertyset
     */
    public void setIf(String ifpropertyset)
    {
        this.ifpropertyset = ifpropertyset;
    }
}