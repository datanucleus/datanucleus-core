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

/**
 * Representation of a Meta-Data file. 
 * Contains a list of package meta-data, together with any named queries and fetch plans etc.
 */
public class FileMetaData extends MetaData
{
    // TODO Remove this, and pass in to populate/initialise as required
    /** Manager for this MetaData object. Used in AbstractMemberMetaData.setRelation process. */
    transient protected MetaDataManager metaDataManager;

    /** Type of file (JDO, ORM, JDOQUERY, etc) */
    protected MetadataFileType type;

    /** Name of file */
    protected String filename;

    /** Catalog name for all classes in this file */
    protected String catalog;

    /** Schema name for all classes in this file */
    protected String schema;

    /** Named queries defined in this file. */
    protected Collection<QueryMetaData> queries = null;

    /** Named stored procedures defined in this file. */
    protected Collection<StoredProcQueryMetaData> storedProcs = null;

    /** List of query result MetaData defined for this file. */
    protected Collection<QueryResultMetaData> queryResultMetaData = null;

    /** Named FetchPlans in this file. */
    protected Collection<FetchPlanMetaData> fetchPlans = null;

    /** List of packages in this file (uses List to retain file positioning) */
    protected List<PackageMetaData> packages = null;

    /** List of event listeners defined for this file. */
    protected List<EventListenerMetaData> listeners = null;

    /**
     * Constructor.
     */
    public FileMetaData()
    {
    }

    /**
     * Method to set the MetaDataManager in use.
     * TODO Remove this
     * @param mmgr MetaDataManager to use
     */
    public void setMetaDataManager(MetaDataManager mmgr)
    {
        this.metaDataManager = mmgr;
    }

    public String getFilename()
    {
        return filename;
    }

    public FileMetaData setFilename(String filename)
    {
        this.filename = filename;
        return this;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public FileMetaData setCatalog(String catalog)
    {
        this.catalog = catalog;
        return this;
    }

    public String getSchema()
    {
        return schema;
    }

    public FileMetaData setSchema(String schema)
    {
        this.schema = schema;
        return this;
    }

    public MetadataFileType getType()
    {
        return type;
    }

    public FileMetaData setType(MetadataFileType type)
    {
        this.type = type;
        return this;
    }

    /**
     * Accessor for the number of named queries.
     * @return no of named queries
     */
    public int getNoOfQueries()
    {
        return queries != null ? queries.size() : 0;
    }

    /**
     * Accessor for the metadata of the named queries.
     * @return Meta-Data for the named queries.
     */
    public QueryMetaData[] getQueries()
    {
        return (queries == null ? null : 
            (QueryMetaData[])queries.toArray(new QueryMetaData[queries.size()]));
    }

    /**
     * Accessor for the number of named queries.
     * @return no of named queries
     */
    public int getNoOfStoredProcQueries()
    {
        return storedProcs != null ? storedProcs.size() : 0;
    }

    /**
     * Accessor for the metadata of the named stored procedure queries.
     * @return Meta-Data for the named stored proc queries.
     */
    public StoredProcQueryMetaData[] getStoredProcQueries()
    {
        return (storedProcs == null ? null : 
            (StoredProcQueryMetaData[])storedProcs.toArray(new StoredProcQueryMetaData[storedProcs.size()]));
    }

    /**
     * Accessor for the number of named fetch plans.
     * @return no of named fetch plans
     */
    public int getNoOfFetchPlans()
    {
        return fetchPlans != null ? fetchPlans.size() : 0;
    }

    /**
     * Accessor for the metadata of the named fetch plans.
     * @return Meta-Data for the named fetch plans.
     */
    public FetchPlanMetaData[] getFetchPlans()
    {
        return (fetchPlans == null ? null : 
            (FetchPlanMetaData[])fetchPlans.toArray(new FetchPlanMetaData[fetchPlans.size()]));
    }

    /**
     * Accessor for the number of packages.
     * @return no of packages.
     */
    public int getNoOfPackages()
    {
        return packages != null ? packages.size() : 0;
    }

    /**
     * Accessor for the meta-data of a package.
     * @param i index number
     * @return Meta-Data for a package.
     */
    public PackageMetaData getPackage(int i)
    {
        if (packages == null)
        {
            return null;
        }

        return packages.get(i);
    }

    /**
     * Accessor for the Meta-Data of a package with a given name.
     * @param name Name of the package
     * @return Meta-Data for the package
     */
    public PackageMetaData getPackage(String name)
    {
        if (packages == null)
        {
            return null;
        }

        Iterator<PackageMetaData> iter = packages.iterator();
        while (iter.hasNext())
        {
            PackageMetaData p = iter.next();
            if (p.name.equals(name))
            {
                return p;
            }
        }
        return null;
    }

    /**
     * Utility method to check if the MetaData for a class is contained in this file.
     * @param pkg_name Name of package
     * @param class_name Name of class
     * @return The MetaData for the class
     */
    public ClassMetaData getClass(String pkg_name, String class_name)
    {
        if (pkg_name == null || class_name == null)
        {
            return null;
        }

        PackageMetaData pmd=getPackage(pkg_name);
        if (pmd != null)
        {
            return pmd.getClass(class_name);
        }

        return null;
    }

    /**
     * Method to create a new QueryMetadata, add it to the registered queries and return it.
     * @param queryName Name of the query
     * @return The Query metadata
     */
    public QueryMetaData newQueryMetadata(String queryName)
    {
        QueryMetaData qmd = new QueryMetaData(queryName);

        if (queries == null)
        {
            queries = new HashSet();
        }
        queries.add(qmd);
        qmd.parent = this;

        return qmd;
    }

    /**
     * Method to create a new StoredProcQueryMetadata, add it to the registered queries and return it.
     * @param queryName Name of the query
     * @return The Query metadata
     */
    public StoredProcQueryMetaData newStoredProcQueryMetaData(String queryName)
    {
        StoredProcQueryMetaData qmd = new StoredProcQueryMetaData(queryName);

        if (storedProcs == null)
        {
            storedProcs = new HashSet();
        }
        storedProcs.add(qmd);
        qmd.parent = this;

        return qmd;
    }

    public FetchPlanMetaData newFetchPlanMetadata(String name)
    {
        FetchPlanMetaData fpmd = new FetchPlanMetaData(name);

        if (fetchPlans == null)
        {
            fetchPlans = new HashSet();
        }
        fetchPlans.add(fpmd);
        fpmd.parent = this;

        return fpmd;
    }

    /**
     * Method to create and return a package metadata for the specified package name.
     * @param name Name of the package
     * @return The PackageMetadata
     */
    public PackageMetaData newPackageMetadata(String name)
    {
        PackageMetaData pmd = new PackageMetaData(name);
        if (packages == null)
        {
            packages = new ArrayList();
        }
        else
        {
            // Check if already exists and return the current one if so
            Iterator<PackageMetaData> iter = packages.iterator();
            while (iter.hasNext())
            {
                PackageMetaData p = iter.next();
                if (pmd.getName().equals(p.getName()))
                {
                    return p;
                }
            }
        }

        packages.add(pmd);
        pmd.parent = this;
        return pmd;
    }

    /**
     * Add a listener class name
     * @param listener the listener metadata. Duplicated classes are ignored
     */
    public void addListener(EventListenerMetaData listener)
    {
        if (listeners == null)
        {
            listeners = new ArrayList();
        }
        if (!listeners.contains(listener))
        {
            listeners.add(listener);
            listener.parent = this;
        }
    }

    /**
     * Get the event listeners registered against the file.
     * @return the event listeners
     */
    public List getListeners()
    {
        return listeners;
    }

    /**
     * Method to register a query-result MetaData.
     * @param resultMetaData Query-Result MetaData to register
     */
    public void addQueryResultMetaData(QueryResultMetaData resultMetaData)
    {
        if (queryResultMetaData == null)
        {
            queryResultMetaData = new HashSet();
        }
        if (!queryResultMetaData.contains(resultMetaData))
        {
            queryResultMetaData.add(resultMetaData);
            resultMetaData.parent = this;
        }
    }

    /**
     * Method to create a new query result metadata, add it, and return it.
     * @param name Name of the result
     * @return The query result metadata
     */
    public QueryResultMetaData newQueryResultMetadata(String name)
    {
        QueryResultMetaData qrmd = new QueryResultMetaData(name);
        addQueryResultMetaData(qrmd);
        return qrmd;
    }

    /**
     * Get the query result MetaData.
     * @return Query Result MetaData
     */
    public QueryResultMetaData[] getQueryResultMetaData()
    {
        if (queryResultMetaData == null)
        {
            return null;
        }
        return queryResultMetaData.toArray(new QueryResultMetaData[queryResultMetaData.size()]);
    }

    // -------------------------------- Utilities ------------------------------

    /**
     * Returns a string representation of the object.
     * @param prefix Any prefix for the output
     * @param indent The indent
     * @return a string representation of the object.
     */
    public String toString(String prefix, String indent)
    {
        if (indent == null)
        {
            indent = "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix).append("<jdo");
        if (catalog != null)
        {
            sb.append(" catalog=\"" + catalog + "\"");
        }
        if (schema != null)
        {
            sb.append(" schema=\"" + schema + "\"");
        }
        sb.append(">\n");

        // Add packages
        if (packages != null)
        {
            Iterator<PackageMetaData> iter = packages.iterator();
            while (iter.hasNext())
            {
                sb.append(iter.next().toString(indent,indent));
            }
        }

        // Add queries
        if (queries != null)
        {
            Iterator iter = queries.iterator();
            while (iter.hasNext())
            {
                sb.append(((QueryMetaData)iter.next()).toString(indent, indent));
            }
        }

        // Add fetch plans
        if (fetchPlans != null)
        {
            Iterator iter = fetchPlans.iterator();
            while (iter.hasNext())
            {
                sb.append(((FetchPlanMetaData)iter.next()).toString(indent, indent));
            }
        }

        // Add extensions
        sb.append(super.toString(indent,indent)); 

        sb.append("</jdo>");
        return sb.toString();
    }
}