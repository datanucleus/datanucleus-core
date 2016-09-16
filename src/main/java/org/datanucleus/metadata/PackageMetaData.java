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
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.util.StringUtils;

/**
 * Representation of the Meta-Data for a package.
 */
public class PackageMetaData extends MetaData
{
    private static final long serialVersionUID = 2129305063744686523L;

    /** List of interfaces (uses List to retain positioning). */
    protected List<InterfaceMetaData> interfaces = null;

    /** List of classes (uses List to retain positioning). */
    protected List<ClassMetaData> classes = null;

    /** Sequence generators. */
    protected Collection<SequenceMetaData> sequences = null;

    /** Table generators. */
    protected Collection<TableGeneratorMetaData> tableGenerators = null;

    /** Package name */
    protected final String name;

    /** Catalog name for all classes in this package */
    protected String catalog;

    /** Schema name for all classes in this package */
    protected String schema;

    /**
     * Constructor. Create packages using FileMetaData.newPackageMetadata()
     * @param name Name of package
     */
    PackageMetaData(final String name)
    {
        this.name = name != null ? name : "";
    }

    public void initialise(ClassLoaderResolver clr)
    {
        if (this.catalog == null && ((FileMetaData)parent).getCatalog() != null)
        {
            // Nothing specified for this package, but file has a value
            this.catalog = ((FileMetaData)parent).getCatalog();
        }
        if (this.schema == null && ((FileMetaData)parent).getSchema() != null)
        {
            // Nothing specified for this package, but file has a value
            this.schema = ((FileMetaData)parent).getSchema();
        }
        super.initialise(clr);
    }

    /**
     * Accessor for the parent FileMetaData.
     * @return File MetaData.
     */
    public FileMetaData getFileMetaData()
    {
        if (parent != null)
        {
            return (FileMetaData)parent;
        }
        return null;
    }

    /**
     * Accessor for the name of the package
     * @return package name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Accessor for the catalog name for all classes in this package
     * @return Catalog name to use.
     */
    public String getCatalog()
    {
        return catalog;
    }

    /**
     * Accessor for the schema name for all classes in this package
     * @return Schema name to use.
     */
    public String getSchema()
    {
        return schema;
    }

    /**
     * Accessor for the number of interfaces.
     * @return Number of interfaces.
     */
    public int getNoOfInterfaces()
    {
        return interfaces != null ? interfaces.size() : 0;
    }
 
    /**
     * Accessor for the Meta-Data of a interface in this package.
     * @param i interface index
     * @return Meta-Data for the interface
     */
    public InterfaceMetaData getInterface(int i)
    {
        return interfaces.get(i);
    }

    /**
     * Accessor for the Meta-Data of an interface with the specified name.
     * @param name the name of the interface
     * @return Meta-Data for the interface
     */
    public InterfaceMetaData getInterface(String name)
    {
        Iterator iter=interfaces.iterator();
        while (iter.hasNext())
        {
            InterfaceMetaData imd = (InterfaceMetaData)iter.next();
            if (imd.getName().equals(name))
            {
                return imd;
            }
        }
        return null;
    }

    /**
     * Accessor for the number of classes.
     * @return Number of classes.
     */
    public int getNoOfClasses()
    {
        return classes != null ? classes.size() : 0;
    }

    /**
     * Accessor for the Meta-Data of a class in this package.
     * @param i class index
     * @return Meta-Data for the class
     */
    public ClassMetaData getClass(int i)
    {
        return classes.get(i);
    }

    /**
     * Accessor for the Meta-Data of a class with the specified name.
     * @param name the name of the class
     * @return Meta-Data for the class.
     */
    public ClassMetaData getClass(String name)
    {
        Iterator iter=classes.iterator();
        while (iter.hasNext())
        {
            ClassMetaData cmd = (ClassMetaData)iter.next();
            if (cmd.getName().equals(name))
            {
                return cmd;
            }
        }
        return null;
    }

    /**
     * Accessor for the number of sequences.
     * @return Number of sequences.
     */
    public int getNoOfSequences()
    {
        return sequences != null ? sequences.size() : 0;
    }
 
    /**
     * Accessor for the Meta-Data for the sequences in this package.
     * @return Meta-Data for the sequences
     */
    public SequenceMetaData[] getSequences()
    {
        return sequences == null ? null : (SequenceMetaData[])sequences.toArray(new SequenceMetaData[sequences.size()]);
    }

    /**
     * Accessor for the Meta-Data of an sequence with the specified name.
     * @param name the name of the sequence
     * @return Meta-Data for the sequence
     */
    public SequenceMetaData getSequence(String name)
    {
        Iterator iter = sequences.iterator();
        while (iter.hasNext())
        {
            SequenceMetaData seqmd = (SequenceMetaData)iter.next();
            if (seqmd.getName().equals(name))
            {
                return seqmd;
            }
        }
        return null;
    }

    /**
     * Accessor for the number of table generators.
     * @return Number of table generators.
     */
    public int getNoOfTableGenerators()
    {
        return tableGenerators != null ? tableGenerators.size() : 0;
    }
 
    /**
     * Accessor for the Meta-Data for the table generators in this package.
     * @return Meta-Data for the table generators
     */
    public TableGeneratorMetaData[] getTableGenerators()
    {
        return tableGenerators == null ? null : (TableGeneratorMetaData[])tableGenerators.toArray(new TableGeneratorMetaData[tableGenerators.size()]);
    }

    /**
     * Accessor for the Meta-Data of a table generator with the specified name.
     * @param name the name of the table generator
     * @return Meta-Data for the table generator
     */
    public TableGeneratorMetaData getTableGenerator(String name)
    {
        Iterator iter = tableGenerators.iterator();
        while (iter.hasNext())
        {
            TableGeneratorMetaData tgmd = (TableGeneratorMetaData)iter.next();
            if (tgmd.getName().equals(name))
            {
                return tgmd;
            }
        }
        return null;
    }

    // -------------------------------- Mutators -------------------------------

    /**
     * Method to add a class Meta-Data to the package.
     * @param cmd Meta-Data for the class
     * @return The class metadata that was added (or already existing)
     */
    public ClassMetaData addClass(ClassMetaData cmd)
    {
        if (cmd == null)
        {
            return null;
        }
        if (classes == null)
        {
            classes = new ArrayList();
        }
        else
        {
            // Check if already exists and return the current one if so
            Iterator iter = classes.iterator();
            while (iter.hasNext())
            {
                AbstractClassMetaData c = (AbstractClassMetaData)iter.next();
                if (cmd.getName().equals(c.getName()) && c instanceof ClassMetaData)
                {
                    return (ClassMetaData)c;
                }
            }
        }

        classes.add(cmd);
        cmd.parent = this;
        return cmd;
    }

    /**
     * Method to remove a class from this metadata definition.
     * This is of use where we read in metadata only to find that the class that it pertains to is not in the CLASSPATH.
     * @param cmd Metadata for the class to remove
     */
    public void removeClass(AbstractClassMetaData cmd)
    {
        if (classes != null)
        {
            classes.remove(cmd);
        }
    }

    /**
     * Method to create a new class metadata, add it, and return it.
     * @param className Name of the class (in this package)
     * @return The class metadata
     */
    public ClassMetaData newClassMetadata(String className)
    {
        if (StringUtils.isWhitespace(className))
        {
            throw new InvalidClassMetaDataException("044061", name);
        }
        ClassMetaData cmd = new ClassMetaData(this, className);
        return addClass(cmd);
    }

    /**
     * Method to add a interface Meta-Data to the package.
     * @param imd Meta-Data for the interface
     * @return The interface metadata that was added (or already existing)
     */
    public InterfaceMetaData addInterface(InterfaceMetaData imd)
    {
        if (imd == null)
        {
            return null;
        }
        if (interfaces == null)
        {
            interfaces = new ArrayList();
        }
        else
        {
            // Check if already exists and return the current one if so
            Iterator iter = interfaces.iterator();
            while (iter.hasNext())
            {
                AbstractClassMetaData c = (AbstractClassMetaData)iter.next();
                if (imd.getName().equals(c.getName()) && c instanceof InterfaceMetaData)
                {
                    return (InterfaceMetaData)c;
                }
            }
        }

        interfaces.add(imd);
        imd.parent = this;
        return imd;
    }

    /**
     * Method to create a new interface metadata, add it, and return it.
     * @param intfName Name of the interface (in this package)
     * @return The interface metadata
     */
    public InterfaceMetaData newInterfaceMetadata(String intfName)
    {
        InterfaceMetaData imd = new InterfaceMetaData(this, intfName);
        return addInterface(imd);
    }

    /**
     * Method to add a sequence Meta-Data to the package.
     * @param seqmd Meta-Data for the sequence
     */
    public void addSequence(SequenceMetaData seqmd)
    {
        if (seqmd == null)
        {
            return;
        }

        if (sequences == null)
        {
            sequences = new HashSet();
        }
        sequences.add(seqmd);
        seqmd.parent = this;
    }

    /**
     * Method to create a new Sequence metadata, add it, and return it.
     * @param seqName Name of the sequence
     * @param seqStrategy Strategy for the sequence
     * @return The sequence metadata
     */
    public SequenceMetaData newSequenceMetadata(String seqName, String seqStrategy)
    {
        SequenceMetaData seqmd = new SequenceMetaData(seqName, seqStrategy);
        addSequence(seqmd);
        return seqmd;
    }

    /**
     * Method to add a TableGenerator Meta-Data to the package.
     * @param tabmd Meta-Data for the TableGenerator
     */
    public void addTableGenerator(TableGeneratorMetaData tabmd)
    {
        if (tabmd == null)
        {
            return;
        }

        if (tableGenerators == null)
        {
            tableGenerators = new HashSet();
        }
        tableGenerators.add(tabmd);
        tabmd.parent = this;
    }

    /**
     * Method to create a new TableGenerator metadata, add it and return it.
     * @param name Name of the table generator
     * @return The metadata
     */
    public TableGeneratorMetaData newTableGeneratorMetadata(String name)
    {
        TableGeneratorMetaData tgmd = new TableGeneratorMetaData(name);

        if (tableGenerators == null)
        {
            tableGenerators = new HashSet();
        }
        tableGenerators.add(tgmd);
        tgmd.parent = this;

        return tgmd;
    }

    public PackageMetaData setCatalog(String catalog)
    {
        this.catalog = StringUtils.isWhitespace(catalog) ? null : catalog;
        return this;
    }

    public PackageMetaData setSchema(String schema)
    {
        this.schema = StringUtils.isWhitespace(schema) ? null : schema;
        return this;
    }

    // ------------------------------ Utilities --------------------------------

    /**
     * Returns a string representation of the object.
     * @param prefix prefix string
     * @param indent indent string
     * @return a string representation of the object.
     */
    public String toString(String prefix,String indent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<package name=\"" + name + "\"");
        if (catalog != null)
        {
            sb.append(" catalog=\"" + catalog + "\"");
        }
        if (schema != null)
        {
            sb.append(" schema=\"" + schema + "\"");
        }
        sb.append(">\n");

        // Add interfaces
        if (interfaces != null)
        {
            Iterator int_iter = interfaces.iterator();
            while (int_iter.hasNext())
            {
                sb.append(((InterfaceMetaData)int_iter.next()).toString(prefix + indent,indent));
            }
        }

        // Add classes
        if (classes != null)
        {
            Iterator cls_iter = classes.iterator();
            while (cls_iter.hasNext())
            {
                sb.append(((ClassMetaData)cls_iter.next()).toString(prefix + indent,indent));
            }
        }

        // Add sequences
        if (sequences != null)
        {
            Iterator seq_iter = sequences.iterator();
            while (seq_iter.hasNext())
            {
                sb.append(((SequenceMetaData)seq_iter.next()).toString(prefix + indent,indent));
            }
        }

        // Add extensions
        sb.append(super.toString(prefix + indent,indent));

        sb.append(prefix).append("</package>\n");
        return sb.toString();
    }
}