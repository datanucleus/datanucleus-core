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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.PersistenceNucleusContext;
import org.datanucleus.PersistenceNucleusContextImpl;
import org.datanucleus.PropertyNames;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.enhancer.EnhancementNucleusContextImpl;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NoPersistenceInformationException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.annotations.AnnotationManager;
import org.datanucleus.metadata.annotations.AnnotationManagerImpl;
import org.datanucleus.metadata.xml.MetaDataParser;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.MultiMap;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Manager of metadata information in DataNucleus having scope of an NucleusContext.
 * Each PMF/EMF will effectively have a single MetaDataManager handling all XML/Annotations metadata.
 * <p>
 * Metadata can be loaded into the MetaDataManager in two ways
 * <ul>
 * <li>Load when required. When the persistence process needs a class it will ask for metadata, and we can
 * go and find its metadata from XML/annotations.</li>
 * <li>Load explicitly via API calls. This happens when handling persistence for a persistence-unit for
 * example since we know what classes/mapping is involved. It is also the case with the enhancer where
 * we know what classes to enhance so we load the metadata first</li>
 * </ul>
 * <P>
 * Acts as a registry of metadata so that metadata files don't need to be 
 * parsed multiple times. MetaData is stored as a FileMetaData, which contains
 * PackageMetaData, which contains ClassMetaData, and so on. This maps exactly
 * to the users model of their metadata. The users access point is 
 * <B>getMetaDataForClass()</B> which will check the known classes without metadata,
 * then check the existing registered metdata, then check the valid locations for 
 * metdata files. This way, the metadata is managed from this single point.
 * </P>
 * <P>
 * Maintains a list of all classes that have been checked for MetaData and
 * don't have any available. This avoids the needs to look up MetaData multiple
 * times finding the same result. Currently this list is for all ClassMetaData 
 * objects keyed by the class name.
 * </P>
 * <P>
 * Users can register interest in knowing when metadata for classes are loaded by registering a listener
 * using the <i>addListener</i> method. This will then notify the listener when metadata for any class
 * is initialised. This provides the opportunity to reject the metadata where particular features are
 * not supported. For example a StoreManager could register a listener where it doesn't support
 * datastore identity and throw an InvalidMetaDataException. This would then filter back out to the user
 * for the operation they invoked
 * </P>
 * <P>
 * MetaDataManager is intended to be thread-safe. All maps are ConcurrentHashMap to provide basic multithread usage.
 * In addition all mutating methods make use of an update "lock" so that only one thread can update the metadata
 * definition at any time.
 * </P>
 */
public abstract class MetaDataManagerImpl implements Serializable, MetaDataManager
{
    private static final long serialVersionUID = 5223949693488111123L;

    /** The NucleusContext that this metadata manager is operating in. */
    protected final NucleusContext nucleusContext;

    /** Manager for annotations. */
    protected final AnnotationManager annotationManager;

    /** XML Parser for MetaData. */
    protected MetaDataParser metaDataParser = null;

    /** Flag whether we should validate the (XML) metadata files when parsing. */
    protected boolean validateXML = true;

    /** Flag whether we should be (XML) namespace aware when parsing. */
    protected boolean supportXMLNamespaces = true;

    /** Flag defining if we allow metadata load currently. If false then we only allow access to currently loaded metadata. */
    protected boolean allowMetaDataLoad = true;

    /** Whether we allow XML metadata. */
    protected boolean allowXML = true;

    /** Whether we allow annotations metadata. */
    protected boolean allowAnnotations = true;

    /** Whether we allow ORM XML metadata. */
    protected boolean allowORM = true;

    /** Flag defining the default nullability for fields. */
    protected boolean defaultNullable = true;
    
    protected Lock updateLock = null;

    /** Cache of class names that are known to not have MetaData/annotations. */
    protected Collection<String> classesWithoutPersistenceInfo = new HashSet();

    /** Map of ClassMetaData, keyed by the class name. */
    protected Map<String, AbstractClassMetaData> classMetaDataByClass = new ConcurrentHashMap<String, AbstractClassMetaData>();

    protected Map<String, AbstractClassMetaData> usableClassMetaDataByClass = new ConcurrentHashMap<String, AbstractClassMetaData>();

    /** Map of FileMetaData for the parsed files, keyed by the URL string. */
    protected Map<String, FileMetaData> fileMetaDataByURLString = new ConcurrentHashMap();

    /** Map of ClassMetaData, keyed by the JPA "entity name". */
    protected Map<String, AbstractClassMetaData> classMetaDataByEntityName = new ConcurrentHashMap();

    /** Map of ClassMetaData, keyed by the class discriminator name. */
    protected Map<String, AbstractClassMetaData> classMetaDataByDiscriminatorName = new ConcurrentHashMap();

    /** Cache subclass information as that is expensive to compute, keyed by class name */
    protected Map<String, Set<String>> directSubclassesByClass = new ConcurrentHashMap();

    /** Cache of names of concrete subclass for a class name. Used for fast lookups from ids. */
    protected Map<String, Set<String>> concreteSubclassNamesByClassName = new ConcurrentHashMap<String, Set<String>>();

    /** Map of QueryMetaData, keyed by the (class name + query name). */
    protected Map<String, QueryMetaData> queryMetaDataByName = null;

    /** Map of StoredProcQueryMetaData, keyed by the (class name + query name). */
    protected Map<String, StoredProcQueryMetaData> storedProcQueryMetaDataByName = null;

    /** Map of FetchPlanMetaData, keyed by the fetch plan name. */
    protected Map<String, FetchPlanMetaData> fetchPlanMetaDataByName = null;

    /** Map of SequenceMetaData, keyed by the package name and sequence name. */
    protected Map<String, SequenceMetaData> sequenceMetaDataByPackageSequence = null;

    /** Map of TableGeneratorMetaData, keyed by the package name and generator name. */
    protected Map<String, TableGeneratorMetaData> tableGeneratorMetaDataByPackageSequence = null;

    /** Map of QueryResultMetaData keyed by the name. */
    protected Map<String, QueryResultMetaData> queryResultMetaDataByName = null;

    /** Map of class metadata, keyed by the application-id object-id class name (not SingleField). */
    protected MultiMap classMetaDataByAppIdClassName = new MultiMap();

    /** Listeners for metadata load. */
    protected Set<MetaDataListener> listeners = null;

    /** Temporary list of class metadata loaded during the current call, for use with listeners. */
    private List<AbstractClassMetaData> listenersLoadedMetaData = null;

    /** Number of user metadata items. */
    protected int userMetaDataNumber = 0;

    protected Map<String, DiscriminatorLookup> discriminatorLookupByRootClassName = new ConcurrentHashMap<String, MetaDataManagerImpl.DiscriminatorLookup>();

    private class DiscriminatorLookup
    {
        Map<String, String> discrimValueByClass = new HashMap<String, String>();
        Map<String, String> discrimClassByValue = new HashMap<String, String>();
        public void addValue(String className, String value)
        {
            this.discrimValueByClass.put(className, value);
            this.discrimClassByValue.put(value, className);
        }
        public String getValueForClass(String className)
        {
            return discrimValueByClass.get(className);
        }
        public String getClassForValue(String value)
        {
            return discrimClassByValue.get(value);
        }
        public String toString()
        {
            return StringUtils.mapToString(discrimValueByClass);
        }
    }

    /**
     * Constructor, specifying the context used.
     * @param ctx context that this metadata manager operates in
     */
    public MetaDataManagerImpl(NucleusContext ctx)
    {
        nucleusContext = ctx;
        updateLock = new ReentrantLock();

        validateXML = nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_VALIDATE);
        supportXMLNamespaces = nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_XML_NAMESPACE_AWARE);
        allowXML = nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_XML);
        allowAnnotations = nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_ALLOW_ANNOTATIONS);
        defaultNullable = nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_DEFAULT_NULLABLE);
        
        annotationManager = new AnnotationManagerImpl(this);

        // Register all of the types managed by the TypeManager as known second-class types (no metadata).
        Set supportedClasses = nucleusContext.getTypeManager().getSupportedSecondClassTypes();
        Iterator<String> iter = supportedClasses.iterator();
        while (iter.hasNext())
        {
            classesWithoutPersistenceInfo.add(iter.next());
        }

        allowORM = nucleusContext.supportsORMMetaData();
        if (allowORM)
        {
            Boolean configOrm = 
                nucleusContext.getConfiguration().getBooleanObjectProperty(PropertyNames.PROPERTY_METADATA_SUPPORT_ORM);
            if (configOrm != null && !configOrm.booleanValue())
            {
                // User has turned it off
                allowORM = false;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#close()
     */
    @Override
    public void close()
    {
        classMetaDataByClass.clear();
        classMetaDataByClass = null;

        usableClassMetaDataByClass.clear();
        usableClassMetaDataByClass = null;

        fileMetaDataByURLString.clear();
        fileMetaDataByURLString = null;

        classesWithoutPersistenceInfo.clear();
        classesWithoutPersistenceInfo = null;

        directSubclassesByClass.clear();
        directSubclassesByClass = null;
        concreteSubclassNamesByClassName.clear();
        concreteSubclassNamesByClassName = null;

        if (classMetaDataByEntityName != null)
        {
            classMetaDataByEntityName.clear();
            classMetaDataByEntityName = null;
        }

        if (classMetaDataByDiscriminatorName != null)
        {
            classMetaDataByDiscriminatorName.clear();
            classMetaDataByDiscriminatorName = null;
        }

        if (queryMetaDataByName != null)
        {
            queryMetaDataByName.clear();
            queryMetaDataByName = null;
        }
        if (storedProcQueryMetaDataByName != null)
        {
            storedProcQueryMetaDataByName.clear();
            storedProcQueryMetaDataByName = null;
        }
        if (fetchPlanMetaDataByName != null)
        {
            fetchPlanMetaDataByName.clear();
            fetchPlanMetaDataByName = null;
        }

        if (sequenceMetaDataByPackageSequence != null)
        {
            sequenceMetaDataByPackageSequence.clear();
            sequenceMetaDataByPackageSequence = null;
        }

        if (tableGeneratorMetaDataByPackageSequence != null)
        {
            tableGeneratorMetaDataByPackageSequence.clear();
            tableGeneratorMetaDataByPackageSequence = null;
        }

        if (queryResultMetaDataByName != null)
        {
            queryResultMetaDataByName.clear();
            queryResultMetaDataByName = null;
        }

        if (classMetaDataByAppIdClassName != null)
        {
            classMetaDataByAppIdClassName.clear();
            classMetaDataByAppIdClassName = null;
        }

        if (listeners != null)
        {
            listeners.clear();
            listeners = null;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#registerListener(org.datanucleus.metadata.MetaDataListener)
     */
    @Override
    public void registerListener(MetaDataListener listener)
    {
        if (listeners == null)
        {
            listeners = new HashSet<MetaDataListener>();
        }
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#deregisterListener(org.datanucleus.metadata.MetaDataListener)
     */
    @Override
    public void deregisterListener(MetaDataListener listener)
    {
        if (listeners == null)
        {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty())
        {
            listeners = null;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getEnhancedMethodNamePrefix()
     */
    @Override
    public String getEnhancedMethodNamePrefix()
    {
        return "dn";
    }

    public boolean isEnhancerField(String fieldName)
    {
        String prefix = "dn";
        if (!fieldName.startsWith(prefix))
        {
            return false;
        }

        // TODO Take these from org.datanucleus.enhancer class at some point to avoid hardcoding
        if (fieldName.equals("dnStateManager") || fieldName.equals("dnFlags") || fieldName.equals("dnDetachedState"))
        {
            return true;
        }

        // Static fields - commented out. Uncomment if we ever support persisting static fields
        /*if (fieldName.equals("dnFieldFlags") ||
            fieldName.equals("dnFieldNames") ||
            fieldName.equals("dnFieldTypes") ||
            fieldName.equals("dnPersistableSuperclass") ||
            fieldName.equals("dnInheritedFieldCount"))
        {
            return true;
        }*/
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#setAllowMetaDataLoad(boolean)
     */
    @Override
    public void setAllowMetaDataLoad(boolean allow)
    {
        allowMetaDataLoad = allow;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#setAllowXML(boolean)
     */
    @Override
    public void setAllowXML(boolean allow)
    {
        this.allowXML = allow;
    }
    
    @Override
    public void setDefaultNullable(boolean defaultNullable)
    {
        this.defaultNullable = defaultNullable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#setAllowAnnotations(boolean)
     */
    @Override
    public void setAllowAnnotations(boolean allow)
    {
        this.allowAnnotations = allow;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#supportsORM()
     */
    @Override
    public boolean supportsORM()
    {
        return allowORM;
    }
    
    @Override
    public boolean isDefaultNullable()
    {
        return defaultNullable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#isEnhancing()
     */
    @Override
    public boolean isEnhancing()
    {
        return getNucleusContext() instanceof EnhancementNucleusContextImpl;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#setValidate(boolean)
     */
    @Override
    public void setValidate(boolean validate)
    {
        validateXML = validate;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#setXmlNamespaceAware(boolean)
     */
    @Override
    public void setXmlNamespaceAware(boolean aware)
    {
        supportXMLNamespaces = aware;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getNucleusContext()
     */
    @Override
    public NucleusContext getNucleusContext()
    {
        return nucleusContext;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getApiAdapter()
     */
    @Override
    public ApiAdapter getApiAdapter()
    {
        return nucleusContext.getApiAdapter();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getAnnotationManager()
     */
    @Override
    public AnnotationManager getAnnotationManager()
    {
        return annotationManager;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#loadMetadataFiles(java.lang.String[], java.lang.ClassLoader)
     */
    @Override
    public FileMetaData[] loadMetadataFiles(String[] metadataFiles, ClassLoader loader)
    {
        if (!allowMetaDataLoad)
        {
            return null;
        }
        boolean originatingLoadCall = false;
        if (listenersLoadedMetaData == null && listeners != null)
        {
            originatingLoadCall = true;
            listenersLoadedMetaData = new ArrayList<AbstractClassMetaData>();
        }

        try
        {
            if (originatingLoadCall)
            {
                updateLock.lock();
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044005", StringUtils.objectArrayToString(metadataFiles)));
            }

            // Load MetaData files - will throw NucleusUserException if problems found
            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(loader);
            Collection fileMetaData = loadFiles(metadataFiles, clr);
            if (!fileMetaData.isEmpty())
            {
                // Populate/Initialise all loaded FileMetaData
                initialiseFileMetaDataForUse(fileMetaData, clr);
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044010"));
            }
            if (originatingLoadCall)
            {
                processListenerLoadingCall();
            }

            return (FileMetaData[])fileMetaData.toArray(new FileMetaData[fileMetaData.size()]);
        }
        finally
        {
            if (originatingLoadCall)
            {
                updateLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#loadClasses(java.lang.String[], java.lang.ClassLoader)
     */
    @Override
    public FileMetaData[] loadClasses(String[] classNames, ClassLoader loader)
    {
        if (!allowMetaDataLoad)
        {
            return null;
        }
        boolean originatingLoadCall = false;
        if (listenersLoadedMetaData == null && listeners != null)
        {
            originatingLoadCall = true;
            listenersLoadedMetaData = new ArrayList<AbstractClassMetaData>();
        }

        try
        {
            if (originatingLoadCall)
            {
                updateLock.lock();
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044006", StringUtils.objectArrayToString(classNames)));
            }

            // Load classes
            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(loader);
            Collection fileMetaData = new ArrayList();
            Set<Exception> exceptions = new HashSet();
            for (int i=0;i<classNames.length;i++)
            {
                try
                {
                    Class cls = clr.classForName(classNames[i]);
                    // Check for MetaData for this class (take precedence over annotations if they exist)
                    AbstractClassMetaData cmd = classMetaDataByClass.get(classNames[i]);
                    if (cmd == null)
                    {
                        // No MetaData so try annotations
                        FileMetaData filemd = loadAnnotationsForClass(cls, clr, true, false);
                        if (filemd != null)
                        {
                            // Store file against an annotations specific "URL"
                            registerFile("annotations:" + classNames[i], filemd, clr);
                            fileMetaData.add(filemd);
                        }
                        else
                        {
                            cmd = getMetaDataForClass(cls, clr);
                            if (cmd == null)
                            {
                                // Class has no metadata or annotations so warn the user
                                NucleusLogger.METADATA.debug(Localiser.msg("044017", classNames[i]));
                            }
                            else
                            {
                                fileMetaData.add(cmd.getPackageMetaData().getFileMetaData());
                            }
                        }
                    }
                    else
                    {
                        fileMetaData.add(cmd.getPackageMetaData().getFileMetaData());
                        // We have MetaData, and any annotations will be merged in during the populate process
                    }
                }
                catch (ClassNotResolvedException e)
                {
                    // log and ignore this exception
                    NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(e));
                }
                catch (Exception e)
                {
                    exceptions.add(e);
                }
            }
            if (!exceptions.isEmpty())
            {
                // Exceptions while loading annotations
                throw new NucleusUserException(Localiser.msg("044016"),
                    exceptions.toArray(new Throwable[exceptions.size()]),null);
            }

            if (!fileMetaData.isEmpty())
            {
                // Populate/Initialise all loaded FileMetaData
                initialiseFileMetaDataForUse(fileMetaData, clr);
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044010"));
            }
            if (originatingLoadCall)
            {
                processListenerLoadingCall();
            }

            return (FileMetaData[])fileMetaData.toArray(new FileMetaData[fileMetaData.size()]);
        }
        finally
        {
            if (originatingLoadCall)
            {
                updateLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#loadJar(java.lang.String, java.lang.ClassLoader)
     */
    @Override
    public FileMetaData[] loadJar(String jarFileName, ClassLoader loader)
    {
        if (!allowMetaDataLoad)
        {
            return null;
        }
        boolean originatingLoadCall = false;
        if (listenersLoadedMetaData == null && listeners != null)
        {
            originatingLoadCall = true;
            listenersLoadedMetaData = new ArrayList<AbstractClassMetaData>();
        }

        try
        {
            if (originatingLoadCall)
            {
                updateLock.lock();
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044009", jarFileName));
            }

            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(loader);
            List<FileMetaData> fileMetaData = new ArrayList();

            // Generate list of package.jdo and classes present in the jar
            Set mappingFiles = new HashSet();
            if (allowXML)
            {
                String[] packageJdoFiles = ClassUtils.getPackageJdoFilesForJarFile(jarFileName);
                if (packageJdoFiles != null)
                {
                    for (int i=0;i<packageJdoFiles.length;i++)
                    {
                        mappingFiles.add(packageJdoFiles[i]);
                    }
                }
            }

            Set classNames = new HashSet();
            if (allowAnnotations)
            {
                String[] jarClassNames = ClassUtils.getClassNamesForJarFile(jarFileName);
                if (jarClassNames != null)
                {
                    for (int i=0;i<jarClassNames.length;i++)
                    {
                        classNames.add(jarClassNames[i]);
                    }
                }
            }

            Set<Throwable> exceptions = new HashSet();

            if (allowXML && !mappingFiles.isEmpty())
            {
                // Load XML metadata
                Iterator iter = mappingFiles.iterator();
                while (iter.hasNext())
                {
                    String mappingFileName = (String)iter.next();
                    try
                    {
                        Enumeration files = clr.getResources(mappingFileName, Thread.currentThread().getContextClassLoader());
                        while (files.hasMoreElements())
                        {
                            URL url = (URL)files.nextElement();
                            if (url != null && fileMetaDataByURLString.get(url.toString()) == null)
                            {
                                FileMetaData filemd = parseFile(url);
                                if (filemd != null)
                                {
                                    // Register the file
                                    registerFile(url.toString(), filemd, clr);
                                    fileMetaData.add(filemd);
                                }
                            }
                        }
                    }
                    catch (InvalidMetaDataException imde)
                    {
                        // Error in the metadata for this file
                        NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(imde));
                        exceptions.add(imde);
                    }
                    catch (IOException ioe)
                    {
                        NucleusLogger.METADATA.error(Localiser.msg("044027",
                            jarFileName, mappingFileName, ioe.getMessage()), ioe);
                    }
                }
            }

            if (allowAnnotations && !classNames.isEmpty())
            {
                // Load annotation metadata for all classes
                Iterator iter = classNames.iterator();
                while (iter.hasNext())
                {
                    // Check for MetaData for this class (take precedence over annotations if they exist)
                    String className = (String)iter.next();
                    AbstractClassMetaData cmd = classMetaDataByClass.get(className);
                    if (cmd == null)
                    {
                        // No MetaData so try annotations
                        try
                        {
                            Class cls = clr.classForName(className);
                            FileMetaData filemd = loadAnnotationsForClass(cls, clr, true, false);
                            if (filemd != null)
                            {
                                fileMetaData.add(filemd);
                            }
                        }
                        catch (ClassNotResolvedException e)
                        {
                            // log and ignore this exception
                            NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(e));
                        }
                        catch (Throwable e)
                        {
                            exceptions.add(e);
                        }
                    }
                    else
                    {
                        // We have MetaData, and any annotations will be merged in during the populate process
                    }
                }
            }
            if (!exceptions.isEmpty())
            {
                throw new NucleusUserException(Localiser.msg("044024", jarFileName), 
                    exceptions.toArray(new Throwable[exceptions.size()]));
            }

            if (!fileMetaData.isEmpty())
            {
                // Populate/Initialise all loaded FileMetaData
                initialiseFileMetaDataForUse(fileMetaData, clr);
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044010"));
            }
            if (originatingLoadCall)
            {
                processListenerLoadingCall();
            }

            return fileMetaData.toArray(new FileMetaData[fileMetaData.size()]);
        }
        finally
        {
            if (originatingLoadCall)
            {
                updateLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#loadPersistenceUnit(org.datanucleus.metadata.PersistenceUnitMetaData, java.lang.ClassLoader)
     */
    @Override
    public FileMetaData[] loadPersistenceUnit(PersistenceUnitMetaData pumd, ClassLoader loader)
    {
        if (!allowMetaDataLoad)
        {
            return null;
        }
        boolean originatingLoadCall = false;
        if (listenersLoadedMetaData == null && listeners != null)
        {
            originatingLoadCall = true;
            listenersLoadedMetaData = new ArrayList<AbstractClassMetaData>();
        }

        try
        {
            if (originatingLoadCall)
            {
                updateLock.lock();
            }
            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044007", pumd.getName()));
            }

            Properties puProps = pumd.getProperties();
            if (puProps != null)
            {
                // Apply any properties from this persistence unit to the metadata manager
                if (puProps.containsKey(PropertyNames.PROPERTY_METADATA_XML_VALIDATE))
                {
                    Boolean val = Boolean.valueOf((String) puProps.get(PropertyNames.PROPERTY_METADATA_XML_VALIDATE));
                    if (val != null)
                    {
                        validateXML = val;
                    }
                }
                if (puProps.containsKey(PropertyNames.PROPERTY_METADATA_XML_NAMESPACE_AWARE))
                {
                    Boolean val = Boolean.valueOf((String) puProps.get(PropertyNames.PROPERTY_METADATA_XML_NAMESPACE_AWARE));
                    if (val != null)
                    {
                        supportXMLNamespaces = val;
                    }
                }
            }

            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(loader);
            Set<Throwable> exceptions = new HashSet();
            List<FileMetaData> fileMetaData = new ArrayList();

            // Generate list of XML files
            Set mappingFiles = new HashSet();
            if (allowXML)
            {
                if (nucleusContext.getApiName().equalsIgnoreCase("JPA"))
                {
                    mappingFiles.add("META-INF/orm.xml"); // Default location for JPA
                }
                if (pumd.getMappingFiles() != null)
                {
                    // <mapping-file>
                    mappingFiles.addAll(pumd.getMappingFiles());
                }
                if (nucleusContext.getApiName().equalsIgnoreCase("JDO")) // When in JDO mode grab any package.jdo
                {
                    // <jar-file>
                    Set jarFileNames = pumd.getJarFiles();
                    if (jarFileNames != null)
                    {
                        Iterator iter = jarFileNames.iterator();
                        while (iter.hasNext())
                        {
                            Object jarFile = iter.next();
                            if (jarFile instanceof String)
                            {
                                String[] packageJdoFiles = ClassUtils.getPackageJdoFilesForJarFile((String)jarFile);
                                if (packageJdoFiles != null)
                                {
                                    for (int i=0;i<packageJdoFiles.length;i++)
                                    {
                                        mappingFiles.add(packageJdoFiles[i]);
                                    }
                                }
                            }
                            else if (jarFile instanceof URL)
                            {
                                String[] packageJdoFiles = ClassUtils.getPackageJdoFilesForJarFile((URL)jarFile);
                                if (packageJdoFiles != null)
                                {
                                    for (int i=0;i<packageJdoFiles.length;i++)
                                    {
                                        mappingFiles.add(packageJdoFiles[i]);
                                    }
                                }
                            }
                            else if (jarFile instanceof URI)
                            {
                                String[] packageJdoFiles = ClassUtils.getPackageJdoFilesForJarFile((URI)jarFile);
                                if (packageJdoFiles != null)
                                {
                                    for (int i=0;i<packageJdoFiles.length;i++)
                                    {
                                        mappingFiles.add(packageJdoFiles[i]);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generate list of (possibly annotated) class names
            Set classNames = new HashSet();
            if (allowAnnotations)
            {
                if (pumd.getClassNames() != null)
                {
                    classNames.addAll(pumd.getClassNames());
                }
                if (getNucleusContext() instanceof PersistenceNucleusContextImpl) // TODO Why not when enhancing? document it
                {
                    Set jarFileNames = pumd.getJarFiles();
                    if (jarFileNames != null)
                    {
                        Iterator iter = jarFileNames.iterator();
                        while (iter.hasNext())
                        {
                            Object jarFile = iter.next();
                            if (jarFile instanceof String)
                            {
                                String[] jarClassNames = ClassUtils.getClassNamesForJarFile((String)jarFile);
                                if (jarClassNames != null)
                                {
                                    for (int i=0;i<jarClassNames.length;i++)
                                    {
                                        classNames.add(jarClassNames[i]);
                                    }
                                }
                            }
                            else if (jarFile instanceof URL)
                            {
                                String[] jarClassNames = ClassUtils.getClassNamesForJarFile((URL)jarFile);
                                if (jarClassNames != null)
                                {
                                    for (int i=0;i<jarClassNames.length;i++)
                                    {
                                        classNames.add(jarClassNames[i]);
                                    }
                                }
                            }
                            else if (jarFile instanceof URI)
                            {
                                String[] jarClassNames = ClassUtils.getClassNamesForJarFile((URI)jarFile);
                                if (jarClassNames != null)
                                {
                                    for (int i=0;i<jarClassNames.length;i++)
                                    {
                                        classNames.add(jarClassNames[i]);
                                    }
                                }
                            }
                        }
                    }
                }

                if (!pumd.getExcludeUnlistedClasses())
                {
                    MetaDataScanner scanner = getScanner(clr);
                    if (scanner != null)
                    {
                        Set<String> scannedClassNames = scanner.scanForPersistableClasses(pumd);
                        if (scannedClassNames != null)
                        {
                            classNames.addAll(scannedClassNames);
                        }
                    }
                    else
                    {
                        // Classpath scan for other classes
                        try
                        {
                            if (pumd.getRootURI() != null && pumd.getRootURI().getScheme().equals("file"))
                            {
                                // File-based root so load all classes under the root URL of the persistence-unit
                                File rootDir = new File(pumd.getRootURI());
                                String[] scannedClassNames = ClassUtils.getClassNamesForDirectoryAndBelow(rootDir);
                                if (scannedClassNames != null)
                                {
                                    for (int i=0;i<scannedClassNames.length;i++)
                                    {
                                        NucleusLogger.METADATA.debug(Localiser.msg("044026", scannedClassNames[i], pumd.getName()));
                                        classNames.add(scannedClassNames[i]);
                                    }
                                }
                            }
                        }
                        catch (IllegalArgumentException iae)
                        {
                            NucleusLogger.METADATA.debug("Ignoring scan of classes for this persistence-unit since the URI root is " + pumd.getRootURI() + " and is not hierarchical");
                            // Ignore the scan for classes
                        }
                    }
                }
            }

            if (allowXML && !mappingFiles.isEmpty())
            {
                // Load XML metadata for all <mapping-file> specifications
                Iterator iter = mappingFiles.iterator();
                while (iter.hasNext())
                {
                    String mappingFileName = (String)iter.next();
                    try
                    {
                        Enumeration files = clr.getResources(mappingFileName, Thread.currentThread().getContextClassLoader());
                        if (!files.hasMoreElements())
                        {
                            NucleusLogger.METADATA.debug("Not found any metadata mapping files for resource name " + mappingFileName + " in CLASSPATH");
                        }
                        else
                        {
                            while (files.hasMoreElements())
                            {
                                URL url = (URL)files.nextElement();
                                if (url != null && fileMetaDataByURLString.get(url.toString()) == null)
                                {
                                    FileMetaData filemd = parseFile(url);
                                    if (filemd != null)
                                    {
                                        // Register the file
                                        registerFile(url.toString(), filemd, clr);
                                        fileMetaData.add(filemd);
                                    }
                                }
                            }
                        }
                    }
                    catch (InvalidMetaDataException imde)
                    {
                        // Error in the metadata for this file
                        NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(imde));
                        exceptions.add(imde);
                    }
                    catch (IOException ioe)
                    {
                        NucleusLogger.METADATA.error(Localiser.msg("044027",
                            pumd.getName(), mappingFileName, ioe.getMessage()), ioe);
                    }
                }
            }

            if (allowAnnotations && !classNames.isEmpty())
            {
                // Load annotation metadata for all classes
                Iterator iter = classNames.iterator();
                while (iter.hasNext())
                {
                    // Check for MetaData for this class (take precedence over annotations if they exist)
                    String className = (String)iter.next();
                    AbstractClassMetaData cmd = classMetaDataByClass.get(className);
                    if (cmd == null)
                    {
                        // No MetaData so try annotations
                        try
                        {
                            Class cls = clr.classForName(className);
                            FileMetaData filemd = loadAnnotationsForClass(cls, clr, true, false);
                            if (filemd != null)
                            {
                                fileMetaData.add(filemd);
                            }
                            else
                            {
                                NucleusLogger.METADATA.debug("Class " + className + " was specified in persistence-unit (maybe by not putting exclude-unlisted-classes) " +
                                        pumd.getName() + " but not annotated, so ignoring");
                            }
                        }
                        catch (ClassNotResolvedException e)
                        {
                            // log and ignore this exception
                            NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(e));
                        }
                        catch (Throwable e)
                        {
                            exceptions.add(e);
                        }
                    }
                    else
                    {
                        // We have MetaData, and any annotations will be merged in during the populate process
                    }
                }
            }
            if (!exceptions.isEmpty())
            {
                throw new NucleusUserException(Localiser.msg("044023", pumd.getName()), exceptions.toArray(new Throwable[exceptions.size()]));
            }

            if (!fileMetaData.isEmpty())
            {
                // Populate/Initialise all loaded FileMetaData
                initialiseFileMetaDataForUse(fileMetaData, clr);
            }

            // Some other (inner) classes may have been brought in the populate of the above so check
            // TODO Really need a Set of unpopulated/uninitialised metadata and continue til all done
            for (AbstractClassMetaData cmd : classMetaDataByClass.values())
            {
                if (!cmd.isPopulated())
                {
                    populateAbstractClassMetaData(cmd, clr, loader);
                }
                if (!cmd.isInitialised())
                {
                    initialiseAbstractClassMetaData(cmd, clr);
                }
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044010"));
            }
            if (originatingLoadCall)
            {
                processListenerLoadingCall();
            }

            return fileMetaData.toArray(new FileMetaData[fileMetaData.size()]);
        }
        finally
        {
            if (originatingLoadCall)
            {
                updateLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#loadUserMetaData(org.datanucleus.metadata.FileMetaData, java.lang.ClassLoader)
     */
    @Override
    public void loadUserMetaData(FileMetaData fileMetaData, ClassLoader loader)
    {
        if (fileMetaData == null)
        {
            return;
        }

        if (!allowMetaDataLoad)
        {
            return;
        }
        boolean originatingLoadCall = false;
        if (listenersLoadedMetaData == null && listeners != null)
        {
            originatingLoadCall = true;
            listenersLoadedMetaData = new ArrayList<AbstractClassMetaData>();
        }

        try
        {
            if (originatingLoadCall)
            {
                updateLock.lock();
            }

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044008"));
            }

            ClassLoaderResolver clr = nucleusContext.getClassLoaderResolver(loader);
            fileMetaData.setFilename("User_Metadata_" + userMetaDataNumber);
            userMetaDataNumber++;

            registerFile(fileMetaData.getFilename(), fileMetaData, clr);
            Collection filemds = new ArrayList();
            filemds.add(fileMetaData);
            initialiseFileMetaDataForUse(filemds, clr);

            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044010"));
            }
            if (originatingLoadCall)
            {
                processListenerLoadingCall();
            }
        }
        finally
        {
            if (originatingLoadCall)
            {
                updateLock.unlock();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#unloadMetaDataForClass(java.lang.String)
     */
    @Override
    public void unloadMetaDataForClass(String className)
    {
        try
        {
            updateLock.lock();

            // Remove any reference to the AbstractClassMetaData
            usableClassMetaDataByClass.remove(className);

            AbstractClassMetaData cmd = classMetaDataByClass.remove(className);
            Iterator<Map.Entry<String, AbstractClassMetaData>> iter = classMetaDataByDiscriminatorName.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry<String, AbstractClassMetaData> entry = iter.next();
                if (entry.getValue() == cmd)
                {
                    iter.remove();
                }
            }
            iter = classMetaDataByEntityName.entrySet().iterator();
            while (iter.hasNext())
            {
                Map.Entry<String, AbstractClassMetaData> entry = iter.next();
                if (entry.getValue() == cmd)
                {
                    iter.remove();
                }
            }
            Iterator<Map.Entry> entryIter = classMetaDataByAppIdClassName.entrySet().iterator();
            while (entryIter.hasNext())
            {
                Map.Entry entry = entryIter.next();
                Collection<AbstractClassMetaData> collCmds = (Collection<AbstractClassMetaData>) entry.getValue();
                if (!collCmds.isEmpty())
                {
                    collCmds.remove(cmd);
                }
            }

            // TODO Remove this class from subclass lookups of other classes
            concreteSubclassNamesByClassName.remove(className);
            directSubclassesByClass.remove(className);
            discriminatorLookupByRootClassName.remove(className);
            classesWithoutPersistenceInfo.remove(className);
        }
        finally
        {
            updateLock.unlock();
        }
    }

    /**
     * Accessor for any scanner for metadata classes (optional).
     * Looks for the persistence property "datanucleus.metadata.scanner" (if defined)
     * @param clr The classloader resolver
     * @return scanner instance or null if it doesn't exist or cannot be instantiated
     */
    protected MetaDataScanner getScanner(ClassLoaderResolver clr)
    {
        Object so = nucleusContext.getConfiguration().getProperty(PropertyNames.PROPERTY_METADATA_SCANNER);
        if (so == null)
        {
            return null;
        }
        if (so instanceof MetaDataScanner)
        {
            return (MetaDataScanner)so;
        }
        else if (so instanceof String)
        {
            try 
            {
                Class clazz = clr.classForName((String) so);
                return (MetaDataScanner) clazz.newInstance();
            }
            catch (Throwable t)
            {
                throw new NucleusUserException(Localiser.msg("044012", so), t);
            }
        }
        else
        {
            if (NucleusLogger.METADATA.isDebugEnabled())
            {
                NucleusLogger.METADATA.debug(Localiser.msg("044011", so));
            }
            return null;
        }
    }

    /**
     * Method to initialise the provided FileMetaData, ready for use.
     * @param fileMetaData Collection of FileMetaData
     * @param clr ClassLoader resolver
     * @throws NucleusUserException thrown if an error occurs during the populate/initialise
     *     of the supplied metadata.
     */
    protected void initialiseFileMetaDataForUse(Collection fileMetaData, ClassLoaderResolver clr)
    {
        Set<Exception> exceptions = new HashSet();

        // a). Populate MetaData
        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(Localiser.msg("044018"));
        }
        Iterator iter = fileMetaData.iterator();
        while (iter.hasNext())
        {
            FileMetaData filemd = (FileMetaData)iter.next();
            if (!filemd.isInitialised())
            {
                populateFileMetaData(filemd, clr, null);
            }
        }

        // b). Initialise MetaData
        if (NucleusLogger.METADATA.isDebugEnabled())
        {
            NucleusLogger.METADATA.debug(Localiser.msg("044019"));
        }
        iter = fileMetaData.iterator();
        while (iter.hasNext())
        {
            FileMetaData filemd = (FileMetaData)iter.next();
            if (!filemd.isInitialised())
            {
                try
                {
                    initialiseFileMetaData(filemd, clr, null);
                }
                catch (Exception e)
                {
                    NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(e));
                    exceptions.add(e);
                }
            }
        }
        if (!exceptions.isEmpty())
        {
            throw new NucleusUserException(Localiser.msg("044020"), exceptions.toArray(new Throwable[exceptions.size()]));
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#loadFiles(java.lang.String[], org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public Collection<FileMetaData> loadFiles(String[] metadataFiles, ClassLoaderResolver clr) 
    {
        List<FileMetaData> fileMetaData = new ArrayList();

        Set<Throwable> exceptions = new HashSet();
        if (allowXML)
        {
            for (int i = 0; i < metadataFiles.length; i++) 
            {
                try 
                {
                    URL fileURL = null;
                    try
                    {
                        // Try as file
                        File file = new File(metadataFiles[i]);
                        fileURL = file.toURI().toURL();
                        if (!file.exists())
                        {
                            // Try as CLASSPATH resource
                            fileURL = clr.getResource(metadataFiles[i], null);        
                        }
                    }
                    catch (Exception mue)
                    {
                        // Try as CLASSPATH resource
                        fileURL = clr.getResource(metadataFiles[i], null);
                    }
                    if (fileURL == null)
                    {
                        // User provided a filename which doesn't exist
                        NucleusLogger.METADATA.warn("Metadata file " + metadataFiles[i] + " not found in CLASSPATH");
                        continue;
                    }

                    FileMetaData filemd = fileMetaDataByURLString.get(fileURL.toString());
                    if (filemd == null)
                    {
                        // Valid metadata, and not already loaded
                        filemd = parseFile(fileURL);
                        if (filemd != null)
                        {
                            registerFile(fileURL.toString(), filemd, clr);
                            fileMetaData.add(filemd);
                        }
                        else
                        {
                            throw new NucleusUserException(Localiser.msg("044015", metadataFiles[i]));
                        }
                    }
                    else
                    {
                        fileMetaData.add(filemd);
                    }
                }
                catch (Exception e)
                {
                    NucleusLogger.METADATA.error(StringUtils.getStringFromStackTrace(e));
                    exceptions.add(e);
                }
            }
        }

        if (!exceptions.isEmpty())
        {
            // Exceptions while loading MetaData
            throw new NucleusUserException(Localiser.msg("044016"), exceptions.toArray(new Throwable[exceptions.size()]), null);
        }

        return fileMetaData;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#isClassPersistable(java.lang.String)
     */
    @Override
    public boolean isClassPersistable(String className)
    {
        AbstractClassMetaData acmd = readMetaDataForClass(className);
        if (acmd == null)
        {
            return false;
        }
        return (acmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getFileMetaData()
     */
    @Override
    public FileMetaData[] getFileMetaData()
    {
        Collection filemds = fileMetaDataByURLString.values();
        return (FileMetaData[])filemds.toArray(new FileMetaData[filemds.size()]);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getClassesWithMetaData()
     */
    @Override
    public Collection<String> getClassesWithMetaData()
    {
        return Collections.unmodifiableCollection(classMetaDataByClass.keySet());
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#hasMetaDataForClass(java.lang.String)
     */
    @Override
    public boolean hasMetaDataForClass(String className)
    {
        if (className == null)
        {
            return false;
        }

        // Check if this class has no MetaData before instantiating its class
        if (isClassWithoutPersistenceInfo(className))
        {
            return false;
        }
        
        return (classMetaDataByClass.get(className) != null);
    }


    /**
     * Accessor for whether a class doesn't have MetaData or annotations.
     * @param className Name of the class
     * @return Whether it has no metadata and annotations
     */
    protected boolean isClassWithoutPersistenceInfo(String className)
    {
        if (className == null)
        {
            return true;
        }

        // Standard Java classes have no MetaData
        if (className.startsWith("java.") || className.startsWith("javax."))
        {
            return true;
        }

        // Use the cache to determine if it has metadata
        return classesWithoutPersistenceInfo.contains(className);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getClassMetaDataWithApplicationId(java.lang.String)
     */
    @Override
    public Collection<AbstractClassMetaData> getClassMetaDataWithApplicationId(String objectIdClassName)
    {
        return (Collection<AbstractClassMetaData>)classMetaDataByAppIdClassName.get(objectIdClassName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForClass(java.lang.String, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public AbstractClassMetaData getMetaDataForClass(String className, ClassLoaderResolver clr)
    {
        if (className == null)
        {
            return null;
        }

        AbstractClassMetaData cmd = usableClassMetaDataByClass.get(className);
        if (cmd != null)
        {
            return cmd;
        }

        // Check if this class has no MetaData/annotations before instantiating its class
        if (isClassWithoutPersistenceInfo(className))
        {
            return null;
        }

        synchronized(this)
        {
            // Check if we have the MetaData already
            cmd = classMetaDataByClass.get(className);
            if (cmd != null && cmd.isPopulated() && cmd.isInitialised() && cmd instanceof ClassMetaData)
            {
                // We explicitly don't return metadata for persistent interfaces here since they should return the impl CMD
                return cmd;
            }

            // Resolve the class
            Class c = null;
            try
            {
                if (clr == null)
                {
                    c = Class.forName(className);
                }
                else
                {
                    c = clr.classForName(className, null, false);
                }
            }
            catch (ClassNotFoundException cnfe)
            {
            }
            catch (ClassNotResolvedException cnre)
            {
            }
            if (c == null)
            {
                if (cmd != null && cmd.isPopulated() && cmd.isInitialised())
                {
                    // Return any previously loaded metadata
                    return cmd;
                }

                return null;
            }

            cmd = getMetaDataForClass(c, clr);
            if (cmd != null)
            {
                usableClassMetaDataByClass.put(className, cmd);
            }
            return cmd;
        }
    }

    /** Temporary list of the FileMetaData objects utilised in this call for metadata. */
    protected ArrayList<FileMetaData> utilisedFileMetaData = new ArrayList();

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForClass(java.lang.Class, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public AbstractClassMetaData getMetaDataForClass(Class c, ClassLoaderResolver clr)
    {
        if (c == null)
        {
            return null;
        }

        AbstractClassMetaData cmd = usableClassMetaDataByClass.get(c.getName());
        if (cmd != null)
        {
            return cmd;
        }

        // Check if this class has no MetaData/annotations before instantiating its class
        if (isClassWithoutPersistenceInfo(c.getName()))
        {
            return null;
        }

        synchronized(this)
        {
            boolean originatingLoadCall = false;
            if (listenersLoadedMetaData == null && listeners != null)
            {
                originatingLoadCall = true;
                listenersLoadedMetaData = new ArrayList<AbstractClassMetaData>();
            }

            cmd = null;
            if (c.isInterface())
            {
                // "persistent-interface" - check if it has class built at runtime and return the MetaData for it 
                cmd = getClassMetaDataForImplementationOfPersistentInterface(c.getName());
            }
            else
            {
                // "persistent-class"
                cmd = getMetaDataForClassInternal(c, clr);
            }

            if (cmd != null)
            {
                // Make sure that anything returned is initialised
                populateAbstractClassMetaData(cmd, clr, c.getClassLoader());
                initialiseAbstractClassMetaData(cmd, clr);

                // Make sure all FileMetaData that were subsequently loaded as a result of this call are
                // all initialised before return
                if (!utilisedFileMetaData.isEmpty())
                {
                    // Pass 1 - initialise anything loaded during the initialise of the requested class
                    ArrayList utilisedFileMetaData1 = (ArrayList)utilisedFileMetaData.clone();
                    utilisedFileMetaData.clear();
                    Iterator iter1 = utilisedFileMetaData1.iterator();
                    while (iter1.hasNext())
                    {
                        FileMetaData filemd = (FileMetaData)iter1.next();
                        initialiseFileMetaData(filemd, clr,c.getClassLoader());
                    }

                    if (!utilisedFileMetaData.isEmpty())
                    {
                        // Pass 2 - initialise anything loaded during the initialise of pass 1
                        ArrayList utilisedFileMetaData2 = (ArrayList)utilisedFileMetaData.clone();
                        utilisedFileMetaData.clear();
                        Iterator iter2 = utilisedFileMetaData2.iterator();
                        while (iter2.hasNext())
                        {
                            FileMetaData filemd = (FileMetaData)iter2.next();
                            initialiseFileMetaData(filemd, clr,c.getClassLoader());
                        }
                    }
                }
            }
            else
            {
                if (!c.isInterface())
                {
                    classesWithoutPersistenceInfo.add(c.getName());
                }
            }
            utilisedFileMetaData.clear();

            if (originatingLoadCall)
            {
                processListenerLoadingCall();
            }

            if (cmd != null)
            {
                usableClassMetaDataByClass.put(c.getName(), cmd);
            }
            return cmd;
        }
    }

    protected void processListenerLoadingCall()
    {
        if (!listenersLoadedMetaData.isEmpty() && listeners != null)
        {
            // Notify any listeners of the metadata loaded during this call
            Iterator<AbstractClassMetaData> loadedIter = new ArrayList(listenersLoadedMetaData).iterator();
            while (loadedIter.hasNext())
            {
                AbstractClassMetaData acmd = loadedIter.next();
                Iterator<MetaDataListener> iter = listeners.iterator();
                while (iter.hasNext())
                {
                    MetaDataListener listener = iter.next();
                    listener.loaded(acmd);
                }
            }
        }

        listenersLoadedMetaData = null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForEntityName(java.lang.String)
     */
    @Override
    public AbstractClassMetaData getMetaDataForEntityName(String entityName)
    {
        return classMetaDataByEntityName.get(entityName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForDiscriminator(java.lang.String)
     */
    @Override
    public AbstractClassMetaData getMetaDataForDiscriminator(String discriminator)
    {
        return classMetaDataByDiscriminatorName.get(discriminator);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#readMetaDataForClass(java.lang.String)
     */
    @Override
    public AbstractClassMetaData readMetaDataForClass(String className)
    {
        return classMetaDataByClass.get(className);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#readMetaDataForMember(java.lang.String, java.lang.String)
     */
    @Override
    public AbstractMemberMetaData readMetaDataForMember(String className, String memberName)
    {
        AbstractClassMetaData cmd = readMetaDataForClass(className);
        return (cmd != null ? cmd.getMetaDataForMember(memberName) : null);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForClassInternal(java.lang.Class, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public abstract AbstractClassMetaData getMetaDataForClassInternal(Class c, ClassLoaderResolver clr);

    /**
     * Internal method called when we want to register the metadata for a class/interface.
     * @param fullClassName Name of the class
     * @param cmd The metadata
     */
    protected void registerMetaDataForClass(String fullClassName, AbstractClassMetaData cmd)
    {
        classMetaDataByClass.put(fullClassName, cmd);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getClassesImplementingInterface(java.lang.String, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public String[] getClassesImplementingInterface(String interfaceName, ClassLoaderResolver clr)
    {
        Collection classes = new HashSet();
        Class intfClass = clr.classForName(interfaceName);
        Collection generatedClassNames = new HashSet();

        // Loop through all known classes and find the implementations
        Collection cmds = classMetaDataByClass.values();
        Iterator cmdIter = cmds.iterator();
        boolean isPersistentInterface = false;
        while (cmdIter.hasNext())
        {
            AbstractClassMetaData acmd = (AbstractClassMetaData)cmdIter.next();
            Class implClass = null;
            try
            {
                implClass = clr.classForName(acmd.getFullClassName());
            }
            catch (ClassNotResolvedException cnre)
            {
                // Implementation class not yet generated
            }
            if (implClass != null)
            {
                if (acmd instanceof ClassMetaData)
                {
                    // Make sure that we are initialised since implementsMetaData wont be set
                    initialiseAbstractClassMetaData(acmd, clr);
                    if (intfClass.isAssignableFrom(implClass))
                    {
                        if (!((ClassMetaData)acmd).isAbstract())
                        {
                            classes.add(implClass);
                        }
                    }
                }
                else if (acmd instanceof InterfaceMetaData)
                {
                    if (intfClass.isAssignableFrom(implClass))
                    {
                        isPersistentInterface = true;
                    }
                }
            }
            else
            {
                if (isPersistentInterfaceImplementation(interfaceName, acmd.getFullClassName()))
                {
                    isPersistentInterface = true;
                    generatedClassNames.add(acmd.getFullClassName());
                }
            }
        }

        if (isPersistentInterface && nucleusContext instanceof PersistenceNucleusContext && ((PersistenceNucleusContext)nucleusContext).getImplementationCreator() != null)
        {
            // JDO2 "persistent interfaces" - deliberately kept separate from normal persistence since it is 
            // largely undocumented and best left alone TODO this is very time consuming. got to do some cache
            classes.add(((PersistenceNucleusContext)nucleusContext).getImplementationCreator().newInstance(intfClass, clr).getClass());

            int numClasses = classes.size() + generatedClassNames.size();
            String[] classNames = new String[numClasses];
            Iterator iter = classes.iterator();
            int i = 0;
            while (iter.hasNext())
            {
                classNames[i++] = ((Class)iter.next()).getName();
            }
            iter = generatedClassNames.iterator();
            while (iter.hasNext())
            {
                classNames[i++] = (String)iter.next();
            }
            return classNames;
        }
        else if (!classes.isEmpty())
        {
            // Normal persistence
            // Put the classes into a sorter so we make sure we get the initial implementations first followed
            // by any subclasses of these implementations. This is needed because when generating the schema we require
            // the subclass implementations to already have their datastore column created
            Collection classesSorted = new TreeSet(new InterfaceClassComparator());
            Iterator classesIter = classes.iterator();
            while (classesIter.hasNext())
            {
                classesSorted.add(classesIter.next());
            }

            // Return the class names (in the same order)
            String[] classNames = new String[classesSorted.size()];
            Iterator iter = classesSorted.iterator();
            int i = 0;
            while (iter.hasNext())
            {
                classNames[i++] = ((Class)iter.next()).getName();
            }
            return classNames;
        }
        return null;
    }

    /**
     * Simple comparator that orders the implementations of an interface so that the initial implementations
     * are first, and the subclasses later.
     */
    private static class InterfaceClassComparator implements Comparator, Serializable
    {
        private static final long serialVersionUID = -8114305773358090763L;

        /**
         * Default constructor.
         */
        public InterfaceClassComparator()
        {
            // Nothing to do
        }

        /**
         * Method defining the ordering of objects.
         * Places all nulls at the end.
         * @param o1 First object
         * @param o2 Second object
         * @return The comparison result
         */
        public int compare(Object o1, Object o2)
        {
            if (o1 == null && o2 == null)
            {
                return 0;
            }
            else if (o1 == null || o2 == null)
            {
                return Integer.MIN_VALUE;
            }

            // Just order based on hashcode
            Class cls1 = (Class)o1;
            Class cls2 = (Class)o2;
            return cls1.hashCode() - cls2.hashCode();
        }
    }

    /**
     * Load up and add any O/R mapping info for the specified class to the stored ClassMetaData (if supported).
     * This implementation does nothing so if ORM files are supported then this should be overridden by subclasses.
     * Is package-access so that is only accessable by MetaData classes
     * @param c The class
     * @param clr ClassLoader resolver
     */
    public void addORMDataToClass(Class c, ClassLoaderResolver clr)
    {
        // Default to doing nothing. Specified in subclasses if they support it
        return;
    }

    /**
     * Load up and add any annotations mapping info for the specified class to the stored ClassMetaData.
     * Is package-access so that is only accessable by MetaData classes
     * @param c The class
     * @param cmd the metadata to add annotation to
     * @param clr ClassLoader resolver
     */
    public void addAnnotationsDataToClass(Class c, AbstractClassMetaData cmd, ClassLoaderResolver clr)
    {
        if (allowAnnotations)
        {
            // Get the MetaData for this class/interface
            if (cmd.getPackageMetaData() != null && cmd.getPackageMetaData().getFileMetaData() != null &&
                    cmd.getPackageMetaData().getFileMetaData().getType() == MetadataFileType.ANNOTATIONS)
            {
                // Our MetaData is derived from the Annotations so nothing to merge!
                return;
            }

            // Find if there is any annotations metadata available
            FileMetaData filemd = loadAnnotationsForClass(c, clr, false, false);
            if (filemd != null)
            {
                AbstractClassMetaData annotCmd = filemd.getPackage(0).getClass(0);
                if (annotCmd != null)
                {
                    postProcessClassMetaData(annotCmd, clr);
                    // Merge the annotations MetaData into the class MetaData
                    MetaDataMerger.mergeClassAnnotationsData(cmd, annotCmd, this);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForImplementationOfReference(java.lang.Class, java.lang.Object, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public ClassMetaData getMetaDataForImplementationOfReference(Class referenceClass, Object implValue, ClassLoaderResolver clr)
    {
        if (referenceClass == null || (!referenceClass.isInterface() && referenceClass != java.lang.Object.class))
        {
            return null;
        }

        // Check if this is a "persistent interface"
        Object intfMetaData = getClassMetaDataForImplementationOfPersistentInterface(referenceClass.getName());
        if (intfMetaData != null)
        {
            return (ClassMetaData)intfMetaData;
        }

        // Search for the class required
        ClassMetaData cmd = null;
        Iterator<Map.Entry<String, AbstractClassMetaData>> cmdByClassEntryIter = classMetaDataByClass.entrySet().iterator();
        while (cmdByClassEntryIter.hasNext())
        {
            Map.Entry<String, AbstractClassMetaData> cmdByClassEntry = cmdByClassEntryIter.next();
            String className = cmdByClassEntry.getKey();
            AbstractClassMetaData cmdCls = cmdByClassEntry.getValue();

            if (cmdCls instanceof ClassMetaData)
            {
                try
                {
                    // Check if class is implementation of "implValue" (in the case of java.lang.Object, all will be!)
                    // Class cls = referenceClass.getClassLoader().loadClass(class_name);
                    // if (referenceClass.isAssignableFrom(cls))
                    if (referenceClass == Object.class || clr.isAssignableFrom(referenceClass, className))
                    {
                        // Find the base class that is an implementation
                        cmd = (ClassMetaData) cmdCls;
                        if (implValue != null && cmd.getFullClassName().equals(implValue.getClass().getName()))
                        {
                            return cmd;
                        }

                        cmdCls = cmd.getSuperAbstractClassMetaData();
                        while (cmdCls != null)
                        {
//                          if (!referenceClass.isAssignableFrom(clr.classForName(((ClassMetaData)cmd_superclass).getFullClassName())))
                            if (referenceClass != Object.class && !clr.isAssignableFrom(referenceClass, ((ClassMetaData)cmdCls).getFullClassName()))
                            {
                                // No point going further up since no longer an implementation
                                break;
                            }

                            cmd = (ClassMetaData) cmdCls;
                            if (implValue != null && cmd.getFullClassName().equals(implValue.getClass().getName()))
                            {
                                break;
                            }

                            // Go to next superclass
                            cmdCls = cmdCls.getSuperAbstractClassMetaData();
                            if (cmdCls == null)
                            {
                                break;
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                }
            }
        }

        return cmd;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForQuery(java.lang.Class, org.datanucleus.ClassLoaderResolver, java.lang.String)
     */
    @Override
    public QueryMetaData getMetaDataForQuery(Class cls, ClassLoaderResolver clr, String queryName)
    {
        if (queryName == null || queryMetaDataByName == null)
        {
            return null;
        }

        String query_key = queryName;
        if (cls != null)
        {
            query_key = cls.getName() + "_" + queryName;
        }
        return queryMetaDataByName.get(query_key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getNamedQueryNames()
     */
    @Override
    public Set<String> getNamedQueryNames()
    {
        if (queryMetaDataByName == null || queryMetaDataByName.isEmpty())
        {
            return null;
        }
        return queryMetaDataByName.keySet();
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForStoredProcQuery(java.lang.Class, org.datanucleus.ClassLoaderResolver, java.lang.String)
     */
    @Override
    public StoredProcQueryMetaData getMetaDataForStoredProcQuery(Class cls, ClassLoaderResolver clr, String queryName)
    {
        if (queryName == null || storedProcQueryMetaDataByName == null)
        {
            return null;
        }

        String query_key = queryName;
        if (cls != null)
        {
            query_key = cls.getName() + "_" + queryName;
        }
        return storedProcQueryMetaDataByName.get(query_key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForFetchPlan(java.lang.String)
     */
    @Override
    public FetchPlanMetaData getMetaDataForFetchPlan(String name)
    {
        if (name == null || fetchPlanMetaDataByName == null)
        {
            return null;
        }

        return fetchPlanMetaDataByName.get(name);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForSequence(org.datanucleus.ClassLoaderResolver, java.lang.String)
     */
    @Override
    public SequenceMetaData getMetaDataForSequence(ClassLoaderResolver clr, String seqName)
    {
        if (seqName == null || sequenceMetaDataByPackageSequence == null)
        {
            return null;
        }

        return sequenceMetaDataByPackageSequence.get(seqName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForTableGenerator(org.datanucleus.ClassLoaderResolver, java.lang.String)
     */
    @Override
    public TableGeneratorMetaData getMetaDataForTableGenerator(ClassLoaderResolver clr, String genName)
    {
        if (genName == null || tableGeneratorMetaDataByPackageSequence == null)
        {
            return null;
        }

        return tableGeneratorMetaDataByPackageSequence.get(genName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForQueryResult(java.lang.String)
     */
    @Override
    public QueryResultMetaData getMetaDataForQueryResult(String name)
    {
        if (name == null || queryResultMetaDataByName == null)
        {
            return null;
        }

        return queryResultMetaDataByName.get(name);
    }

    // ------------------------------- Persistent Interfaces ---------------------------------------

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForInterface(java.lang.Class, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public InterfaceMetaData getMetaDataForInterface(Class c, ClassLoaderResolver clr)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#isPersistentInterface(java.lang.String)
     */
    @Override
    public boolean isPersistentInterface(String name)
    {
        // Default to not supporting "persistent-interface"s
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#isPersistentInterfaceImplementation(java.lang.String, java.lang.String)
     */
    @Override
    public boolean isPersistentInterfaceImplementation(String interfaceName, String implName)
    {
        // Default to not supporting "persistent-interface"s
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#isPersistentDefinitionImplementation(java.lang.String)
     */
    @Override
    public boolean isPersistentDefinitionImplementation(String implName)
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getImplementationNameForPersistentInterface(java.lang.String)
     */
    @Override
    public String getImplementationNameForPersistentInterface(String interfaceName)
    {
        // Default to not supporting "persistent-interface"s
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getClassMetaDataForImplementationOfPersistentInterface(java.lang.String)
     */
    @Override
    public ClassMetaData getClassMetaDataForImplementationOfPersistentInterface(String interfaceName)
    {
        // Default to not supporting "persistent-interface"s
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#registerPersistentInterface(org.datanucleus.metadata.InterfaceMetaData, java.lang.Class, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public void registerPersistentInterface(InterfaceMetaData imd, Class implClass, ClassLoaderResolver clr)
    {
        // Default to not supporting "persistent-interface"s
        return;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#registerImplementationOfAbstractClass(org.datanucleus.metadata.ClassMetaData, java.lang.Class, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public void registerImplementationOfAbstractClass(ClassMetaData cmd, Class implClass, ClassLoaderResolver clr)
    {
        // Default to not supporting "persistent-abstract-classes"
        return;
    }

    // ------------------------------- Utilities -------------------------------

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getMetaDataForPersistenceUnit(java.lang.String)
     */
    @Override
    public PersistenceUnitMetaData getMetaDataForPersistenceUnit(String unitName)
    {
        String filename = nucleusContext.getConfiguration().getStringProperty("datanucleus.persistenceXmlFilename");
        PersistenceFileMetaData[] files = MetaDataUtils.parsePersistenceFiles(nucleusContext.getPluginManager(),
            filename, validateXML, nucleusContext.getClassLoaderResolver(null));
        if (files == null)
        {
            // No "persistence.xml" files found
            throw new NucleusUserException(Localiser.msg("044046"));
        }

        for (int i=0;i<files.length;i++)
        {
            PersistenceUnitMetaData[] unitmds = files[i].getPersistenceUnits();
            if (unitmds != null)
            {
                for (int j=0;j<unitmds.length;j++)
                {
                    if (unitmds[j].getName().equals(unitName))
                    {
                        // Found the required unit
                        return unitmds[j];
                    }
                }
            }
        }
        return null;
    }

    /**
     * Utility to parse an XML metadata file.
     * @param file_url URL of the file
     * @return The FileMetaData for this file
     */
    protected abstract FileMetaData parseFile(URL file_url);

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#registerFile(java.lang.String, org.datanucleus.metadata.FileMetaData, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public abstract void registerFile(String fileURLString, FileMetaData filemd, ClassLoaderResolver clr);

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#registerDiscriminatorValueForClass(org.datanucleus.metadata.AbstractClassMetaData, java.lang.String)
     */
    @Override
    public void registerDiscriminatorValueForClass(AbstractClassMetaData cmd, String discrimValue)
    {
        AbstractClassMetaData rootCmd = cmd.getBaseAbstractClassMetaData();
        DiscriminatorLookup lookup = discriminatorLookupByRootClassName.get(rootCmd.getFullClassName());
        if (lookup == null)
        {
            lookup = new DiscriminatorLookup();
            discriminatorLookupByRootClassName.put(rootCmd.getFullClassName(), lookup);
        }
        lookup.addValue(cmd.getFullClassName(), discrimValue);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getClassNameForDiscriminatorValueWithRoot(org.datanucleus.metadata.AbstractClassMetaData, java.lang.String)
     */
    @Override
    public String getClassNameForDiscriminatorValueWithRoot(AbstractClassMetaData rootCmd, String discrimValue)
    {
        DiscriminatorLookup lookup = discriminatorLookupByRootClassName.get(rootCmd.getFullClassName());
        if (lookup != null)
        {
            return lookup.getClassForValue(discrimValue);
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getDiscriminatorValueForClass(org.datanucleus.metadata.AbstractClassMetaData, java.lang.String)
     */
    @Override
    public String getDiscriminatorValueForClass(AbstractClassMetaData cmd, String discrimValue)
    {
        AbstractClassMetaData rootCmd = cmd.getBaseAbstractClassMetaData();
        DiscriminatorLookup lookup = discriminatorLookupByRootClassName.get(rootCmd.getFullClassName());
        if (lookup != null)
        {
            return lookup.getValueForClass(cmd.getFullClassName());
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getClassNameFromDiscriminatorValue(java.lang.String, org.datanucleus.metadata.DiscriminatorMetaData)
     */
    @Override
    public String getClassNameFromDiscriminatorValue(String discrimValue, DiscriminatorMetaData dismd)
    {
        if (discrimValue == null)
        {
            return null;
        }

        if (dismd.getStrategy() == DiscriminatorStrategy.CLASS_NAME)
        {
            // TODO If classMetaData not known load it?
            return discrimValue;
        }
        else if (dismd.getStrategy() == DiscriminatorStrategy.VALUE_MAP)
        {
            AbstractClassMetaData baseCmd = (AbstractClassMetaData)((InheritanceMetaData)dismd.getParent()).getParent();
            AbstractClassMetaData rootCmd = baseCmd.getBaseAbstractClassMetaData();
            return getClassNameForDiscriminatorValueWithRoot(rootCmd, discrimValue);
        }
        return null;
    }

    /**
     * Convenience method to register all sequences found in the passed file.
     * @param filemd MetaData for the file
     */
    public void registerSequencesForFile(FileMetaData filemd)
    {
        // Register all sequences for the packages in this file
        for (int i=0;i<filemd.getNoOfPackages();i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);
            SequenceMetaData[] seqmds = pmd.getSequences();
            if (seqmds != null)
            {
                if (sequenceMetaDataByPackageSequence == null)
                {
                    sequenceMetaDataByPackageSequence = new ConcurrentHashMap();
                }

                // The problem here is that with JDO we want the sequence to be fully-qualified
                // yet JPA wants the sequence name itself. Also we could be using JPA annotations
                // with JDO persistence, or mixed mode, so need to cater for both ways
                for (int j=0;j<seqmds.length;j++)
                {
                    // Register using its fully qualified name (JDO)
                    sequenceMetaDataByPackageSequence.put(seqmds[j].getFullyQualifiedName(), seqmds[j]);

                    // Register using its basic name (JPA)
                    sequenceMetaDataByPackageSequence.put(seqmds[j].getName(), seqmds[j]);
                }
            }
        }
    }

    /**
     * Convenience method to register all table generators found in the passed file.
     * @param filemd MetaData for the file
     */
    public void registerTableGeneratorsForFile(FileMetaData filemd)
    {
        // Register all table generators for the packages in this file
        for (int i=0;i<filemd.getNoOfPackages();i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);
            TableGeneratorMetaData[] tgmds = pmd.getTableGenerators();
            if (tgmds != null)
            {
                if (tableGeneratorMetaDataByPackageSequence == null)
                {
                    tableGeneratorMetaDataByPackageSequence = new ConcurrentHashMap();
                }

                // The problem here is that with JDO we want the generator to be fully-qualified
                // yet JPA wants the generator name itself. Also we could be using JPA annotations
                // with JDO persistence, or mixed mode, so need to cater for both ways
                for (int j=0;j<tgmds.length;j++)
                {
                    // Register using its fully qualified name (JDO)
                    tableGeneratorMetaDataByPackageSequence.put(tgmds[j].getFullyQualifiedName(), tgmds[j]);

                    // Register using its basic sequence name
                    tableGeneratorMetaDataByPackageSequence.put(tgmds[j].getName(), tgmds[j]);
                }
            }
        }
    }

    /**
     * Convenience method to register all table generators found in the passed file.
     * @param filemd MetaData for the file
     */
    protected void registerQueryResultMetaDataForFile(FileMetaData filemd)
    {
        // Register all query result mappings for the file
        QueryResultMetaData[] fqrmds = filemd.getQueryResultMetaData();
        if (fqrmds != null)
        {
            if (queryResultMetaDataByName == null)
            {
                queryResultMetaDataByName = new ConcurrentHashMap();
            }
            for (int i=0;i<fqrmds.length;i++)
            {
                queryResultMetaDataByName.put(fqrmds[i].getName(), fqrmds[i]);
            }
        }

        // Register all query result mappings for the classes in the file
        for (int i=0;i<filemd.getNoOfPackages();i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);
            for (int j=0;j<pmd.getNoOfClasses();j++)
            {
                AbstractClassMetaData cmd = pmd.getClass(j);
                QueryResultMetaData[] qrmds = cmd.getQueryResultMetaData();
                if (qrmds != null)
                {
                    if (queryResultMetaDataByName == null)
                    {
                        queryResultMetaDataByName = new ConcurrentHashMap();
                    }
                    for (int k=0;k<qrmds.length;k++)
                    {
                        queryResultMetaDataByName.put(qrmds[k].getName(), qrmds[k]);
                    }
                }
            }
        }
    }

    /**
     * Convenience method to register a query under a name.
     * @param qmd Query metadata
     */
    public void registerNamedQuery(QueryMetaData qmd)
    {
        if (queryMetaDataByName == null)
        {
            queryMetaDataByName = new ConcurrentHashMap();
        }

        String scope = qmd.getScope();
        String key = qmd.getName();
        if (scope != null)
        {
            key = scope + "_" + key;
        }
        queryMetaDataByName.put(key, qmd);
    }

    /**
     * Convenience method to register all queries found in the passed file.
     * @param filemd MetaData for the file
     */
    protected void registerQueriesForFile(FileMetaData filemd)
    {
        // Register all queries for this file
        // Store queries against "queryname"
        QueryMetaData[] queries = filemd.getQueries();
        if (queries != null)
        {
            if (queryMetaDataByName == null)
            {
                queryMetaDataByName = new ConcurrentHashMap();
            }
            for (int i=0;i<queries.length;i++)
            {
                String scope = queries[i].getScope();
                String key = queries[i].getName();
                if (scope != null)
                {
                    key = scope + "_" + key;
                }
                queryMetaDataByName.put(key, queries[i]);
            }
        }

        for (int i = 0; i < filemd.getNoOfPackages(); i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);

            // Register all classes (and their queries) into the respective lookup maps
            for (int j = 0; j < pmd.getNoOfClasses(); j++)
            {
                // Store queries against "classname_queryname"
                ClassMetaData cmd = pmd.getClass(j);
                QueryMetaData[] classQueries = cmd.getQueries();
                if (classQueries != null)
                {
                    if (queryMetaDataByName == null)
                    {
                        queryMetaDataByName = new ConcurrentHashMap();
                    }
                    for (int k = 0; k < classQueries.length; k++)
                    {
                        String scope = classQueries[k].getScope();
                        String key = classQueries[k].getName();
                        if (scope != null)
                        {
                            key = scope + "_" + key;
                        }
                        queryMetaDataByName.put(key, classQueries[k]);
                    }
                }
            }

            // Register all interfaces (and their queries) into the respective lookup maps
            for (int j = 0; j < pmd.getNoOfInterfaces(); j++)
            {
                // Store queries against "classname_queryname"
                InterfaceMetaData intfmd = pmd.getInterface(j);
                QueryMetaData[] interfaceQueries = intfmd.getQueries();
                if (interfaceQueries != null)
                {
                    if (queryMetaDataByName == null)
                    {
                        queryMetaDataByName = new ConcurrentHashMap();
                    }
                    for (int k = 0; k < interfaceQueries.length; k++)
                    {
                        String scope = interfaceQueries[k].getScope();
                        String key = interfaceQueries[k].getName();
                        if (scope != null)
                        {
                            key = scope + "_" + key;
                        }
                        queryMetaDataByName.put(key, interfaceQueries[k]);
                    }
                }
            }
        }
    }

    /**
     * Convenience method to register all stored proc queries found in the passed file.
     * @param filemd MetaData for the file
     */
    protected void registerStoredProcQueriesForFile(FileMetaData filemd)
    {
        // Register all queries for this file
        // Store queries against "queryname"
        StoredProcQueryMetaData[] queries = filemd.getStoredProcQueries();
        if (queries != null)
        {
            if (storedProcQueryMetaDataByName == null)
            {
                storedProcQueryMetaDataByName = new ConcurrentHashMap();
            }
            for (int i=0;i<queries.length;i++)
            {
                String key = queries[i].getName();
                storedProcQueryMetaDataByName.put(key, queries[i]);
            }
        }

        for (int i = 0; i < filemd.getNoOfPackages(); i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);

            // Register all classes (and their queries) into the respective lookup maps
            for (int j = 0; j < pmd.getNoOfClasses(); j++)
            {
                // Store queries against "classname_queryname"
                ClassMetaData cmd = pmd.getClass(j);
                StoredProcQueryMetaData[] classStoredProcQueries = cmd.getStoredProcQueries();
                if (classStoredProcQueries != null)
                {
                    if (storedProcQueryMetaDataByName == null)
                    {
                        storedProcQueryMetaDataByName = new ConcurrentHashMap();
                    }
                    for (int k = 0; k < classStoredProcQueries.length; k++)
                    {
                        String key = classStoredProcQueries[k].getName();
                        storedProcQueryMetaDataByName.put(key, classStoredProcQueries[k]);
                    }
                }
            }

            // Register all interfaces (and their queries) into the respective lookup maps
            for (int j = 0; j < pmd.getNoOfInterfaces(); j++)
            {
                // Store queries against "classname_queryname"
                InterfaceMetaData intfmd = pmd.getInterface(j);
                StoredProcQueryMetaData[] interfaceStoredProcQueries = intfmd.getStoredProcQueries();
                if (interfaceStoredProcQueries != null)
                {
                    if (storedProcQueryMetaDataByName == null)
                    {
                        storedProcQueryMetaDataByName = new ConcurrentHashMap();
                    }
                    for (int k = 0; k < interfaceStoredProcQueries.length; k++)
                    {
                        String key = interfaceStoredProcQueries[k].getName();
                        storedProcQueryMetaDataByName.put(key, interfaceStoredProcQueries[k]);
                    }
                }
            }
        }
    }

    /**
     * Convenience method to register all FetchPlans found in the passed file.
     * @param filemd MetaData for the file
     */
    protected void registerFetchPlansForFile(FileMetaData filemd)
    {
        // Register all queries for this file
        // Store queries against "queryname"
        FetchPlanMetaData[] fetchPlans = filemd.getFetchPlans();
        if (fetchPlans != null)
        {
            if (fetchPlanMetaDataByName == null)
            {
                fetchPlanMetaDataByName = new ConcurrentHashMap();
            }
            for (int i=0;i<fetchPlans.length;i++)
            {
                fetchPlanMetaDataByName.put(fetchPlans[i].getName(), fetchPlans[i]);
            }
        }
    }

    /**
     * Convenience method to populate all classes/interfaces in a Meta-Data file.
     * @param filemd The MetaData file
     * @param clr Class Loader to use in population
     * @param primary the primary ClassLoader to use (or null)
     */
    protected void populateFileMetaData(FileMetaData filemd, ClassLoaderResolver clr, ClassLoader primary)
    {
        filemd.setMetaDataManager(this);
        for (int i=0;i<filemd.getNoOfPackages();i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);
            for (int j=0;j<pmd.getNoOfClasses();j++)
            {
                AbstractClassMetaData cmd = pmd.getClass(j);
                populateAbstractClassMetaData(cmd, clr, primary);
            }
            for (int j=0;j<pmd.getNoOfInterfaces();j++)
            {
                AbstractClassMetaData cmd = pmd.getInterface(j);
                populateAbstractClassMetaData(cmd, clr, primary);
            }
        }
    }

    /**
     * Initialise all classes/interfaces in a Meta-Data file.
     * @param filemd the FileMetaData
     * @param clr ClassLoader resolver to use
     * @param primary the primary ClassLoader to use (or null)
     */
    protected void initialiseFileMetaData(FileMetaData filemd, ClassLoaderResolver clr, ClassLoader primary)
    {
        for (int i=0;i<filemd.getNoOfPackages();i++)
        {
            PackageMetaData pmd = filemd.getPackage(i);
            pmd.initialise(clr, this);
            
            for (int j=0;j<pmd.getNoOfClasses();j++)
            {
                ClassMetaData cmd = pmd.getClass(j);
                try
                {
                    initialiseClassMetaData(cmd, clr.classForName(cmd.getFullClassName(),primary), clr);
                }
                catch (NucleusException ne)
                {
                    if (nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_IGNORE_MISSING_PERSISTABLE_CLASSES, false))
                    {
                        NucleusLogger.METADATA.warn("Attempt to load metadata for class=" + cmd.getFullClassName() + " but an exception was thrown", ne);
                    }
                    else
                    {
                        throw ne;
                    }
                }
                catch (RuntimeException re)
                {
                    // Do nothing
                }
            }

            for (int j=0;j<pmd.getNoOfInterfaces();j++)
            {
                InterfaceMetaData imd = pmd.getInterface(j);
                try
                {
                    initialiseInterfaceMetaData(imd, clr, primary);
                }
                catch(NucleusException ne)
                {
                    if (nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_IGNORE_MISSING_PERSISTABLE_CLASSES, false))
                    {
                        NucleusLogger.METADATA.warn("Attempt to load metadata for class=" + imd.getFullClassName() + " but an exception was thrown", ne);
                    }
                    else
                    {
                        throw ne;
                    }
                }
                catch (RuntimeException re)
                {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Utility to initialise the MetaData for a class, using the specified
     * class. This assigns defaults to tags that haven't been assigned.
     * If the class that is being used to populate the MetaData is not
     * enhanced, this will throw a NucleusUserException informing them of this. 
     * @param cmd The classes metadata
     * @param cls The class to use as a basis for initialisation
     * @param clr ClassLoader resolver to use
     * @throws NucleusUserException if the class is not enhanced
     */
    protected void initialiseClassMetaData(ClassMetaData cmd, Class cls, ClassLoaderResolver clr)
    {
        synchronized(cmd)
        {
            if (getNucleusContext() instanceof PersistenceNucleusContextImpl && 
                cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE &&
                !getNucleusContext().getApiAdapter().isPersistable(cls))
            {
                throw new NucleusUserException(Localiser.msg("044059", cls.getName()));
            }
            populateAbstractClassMetaData(cmd, clr, cls.getClassLoader());
            initialiseAbstractClassMetaData(cmd, clr);
        }
    }

    /**
     * Utility to initialise the MetaData for a interface, using the specified
     * class. This assigns defaults to tags that haven't been assigned.
     * If the class that is being used to populate the MetaData is not
     * enhanced, this will throw a NucleusUserException informing them of this. 
     * @param imd The interface metadata
     * @param clr The loader of the interface
     * @param primary the primary ClassLoader to use (or null)
     */
    protected void initialiseInterfaceMetaData(InterfaceMetaData imd, ClassLoaderResolver clr, ClassLoader primary)
    {
        synchronized (imd)
        {
            populateAbstractClassMetaData(imd, clr, primary);
            initialiseAbstractClassMetaData(imd, clr);
        }
    }

    /**
     * Method to load the annotations for the specified class and return the FileMetaData containing
     * the class. The FileMetaData, PackageMetaData will be dummy records.
     * @param cls The class
     * @param clr ClassLoader resolver
     * @param register Whether to register the data
     * @param populate Whether to populate the data
     * @return The FileMetaData
     */
    protected FileMetaData loadAnnotationsForClass(Class cls, ClassLoaderResolver clr, boolean register, boolean populate)
    {
        if (!allowAnnotations)
        {
            return null;
        }
        if (isClassWithoutPersistenceInfo(cls.getName()))
        {
            return null;
        }

        String clsPackageName = ClassUtils.getPackageNameForClass(cls);
        if (clsPackageName == null)
        {
            // No package info, so either some primitive, or using root package. Assume it's the latter
            clsPackageName = "";
        }

        // Check for annotations (use dummy file/package so we have a place for it)
        FileMetaData filemd = new FileMetaData();
        filemd.setType(MetadataFileType.ANNOTATIONS);
        filemd.setMetaDataManager(this);
        PackageMetaData pmd = filemd.newPackageMetadata(clsPackageName);
        AbstractClassMetaData cmd = annotationManager.getMetaDataForClass(cls, pmd, clr);
        if (cmd != null)
        {
            if (register)
            {
                // register before populating to avoid recursive loops when loading referenced classes
                registerFile("annotations:" + cls.getName(), filemd, clr);

                if (populate)
                {
                    // Populate all classes in this file we've just parsed (i.e only 1!)
                    populateFileMetaData(filemd, clr, cls.getClassLoader());
                }
            }
            return filemd;
        }
        return null;
    }

    /**
     * Method that will perform any necessary post-processing on metadata.
     * @param cmd Metadata for the class
     * @param clr ClassLoader resolver
     */
    protected void postProcessClassMetaData(AbstractClassMetaData cmd, ClassLoaderResolver clr)
    {
        // This implementation does nothing - override if needed by the API
    }

    // ------------------------------ Utilities --------------------------------

    /**
     * Convenience method to populate the MetaData for the specified class/interface.
     * @param cmd MetaData
     * @param clr ClassLoader resolver
     * @param loader The primary class loader
     */
    protected void populateAbstractClassMetaData(final AbstractClassMetaData cmd, final ClassLoaderResolver clr, 
            final ClassLoader loader)
    {
        if (!cmd.isPopulated() && !cmd.isInitialised())
        {
            // Do as PrivilegedAction since populate() uses reflection to get additional fields
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        cmd.populate(clr, loader, MetaDataManagerImpl.this);
                    }
                    catch (NucleusException ne)
                    {
                        // Catch and rethrow exception since AccessController.doPriveleged swallows it!
                        if (nucleusContext.getConfiguration().getBooleanProperty(PropertyNames.PROPERTY_METADATA_IGNORE_MISSING_PERSISTABLE_CLASSES, false))
                        {
                            NucleusLogger.METADATA.warn("Attempt to load metadata for class=" + cmd.getFullClassName() + " but an exception was thrown", ne);
                        }
                        else
                        {
                            throw ne;
                        }
                    }
                    catch (Exception e)
                    {
                        throw new NucleusUserException("Exception during population of metadata for " + cmd.getFullClassName(), e);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Convenience method to initialise the MetaData for the specified class/interface.
     * @param cmd MetaData
     * @param clr ClassLoaderResolver
     */
    protected void initialiseAbstractClassMetaData(final AbstractClassMetaData cmd, final ClassLoaderResolver clr)
    {
        if (!cmd.isInitialised())
        {
            // Do as PrivilegedAction since uses reflection
            // [JDOAdapter.isValidPrimaryKeyClass calls reflective methods]
            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    try
                    {
                        cmd.initialise(clr, MetaDataManagerImpl.this);
                    }
                    // Catch and rethrow exception since AccessController.doPrivileged swallows it!
                    catch (NucleusException ne)
                    {
                        throw ne;
                    }
                    catch (Exception e)
                    {
                        throw new NucleusUserException("Exception during initialisation of metadata for " + cmd.getFullClassName(), e);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Method called (by AbstractClassMetaData.initialise()) when a class has its metadata initialised.
     * @param cmd Metadata that has been initialised
     */
    public void abstractClassMetaDataInitialised(AbstractClassMetaData cmd)
    {
        if (cmd.getIdentityType() == IdentityType.APPLICATION && !cmd.usesSingleFieldIdentityClass())
        {
            // Register the app-id object-id class lookup
            classMetaDataByAppIdClassName.put(cmd.getObjectidClass(), cmd);
        }

        if (cmd instanceof ClassMetaData)
        {
            if (cmd.getPersistableSuperclass() != null)
            {
                // Add to direct subclasses for the superclass
                Set<String> directSubclasses = directSubclassesByClass.get(cmd.getPersistableSuperclass());
                if (directSubclasses == null)
                {
                    directSubclasses = new HashSet<String>();
                    directSubclassesByClass.put(cmd.getPersistableSuperclass(), directSubclasses);
                }
                directSubclasses.add(cmd.getFullClassName());

                if (!((ClassMetaData)cmd).isAbstract())
                {
                    // Concrete class, so add to cache of concrete subclasses for all superclasses
                    AbstractClassMetaData theCmd = cmd;
                    while (theCmd.getPersistableSuperclass() != null)
                    {
                        theCmd = theCmd.getSuperAbstractClassMetaData();
                        Set<String> subclassNames = concreteSubclassNamesByClassName.get(theCmd.getFullClassName());
                        if (subclassNames == null)
                        {
                            subclassNames = new HashSet();
                            concreteSubclassNamesByClassName.put(theCmd.getFullClassName(), subclassNames);
                        }
                        subclassNames.add(cmd.getFullClassName());
                    }
                }
            }
        }

        if (listeners != null && listenersLoadedMetaData != null)
        {
            listenersLoadedMetaData.add(cmd);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getConcreteSubclassesForClass(java.lang.String)
     */
    @Override
    public String[] getConcreteSubclassesForClass(String className)
    {
        Set<String> concreteSubclasses = concreteSubclassNamesByClassName.get(className);
        return (concreteSubclasses == null ? null : concreteSubclasses.toArray(new String[concreteSubclasses.size()]));
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getSubclassesForClass(java.lang.String, boolean)
     */
    @Override
    public String[] getSubclassesForClass(String className, boolean includeDescendents)
    {
        Collection subclassNames2 = new HashSet();
        provideSubclassesForClass(className, includeDescendents, subclassNames2);
        if (!subclassNames2.isEmpty())
        {
            return (String[])subclassNames2.toArray(new String[subclassNames2.size()]);
        }

        return null;
    }

    /**
     * Provide the subclasses of a particular class to a given <code>consumer</code>
     * @param className Name of the class that we want the known subclasses for.
     * @param includeDescendents Whether to include subclasses of subclasses etc
     * @param consumer the Collection (Set) where discovered subclasses are added
     */
    private void provideSubclassesForClass(String className, boolean includeDescendents, Collection consumer)
    {
        Set<String> subclasses = directSubclassesByClass.get(className);
        if (subclasses != null)
        {
            consumer.addAll(subclasses);

            if (includeDescendents) 
            {
                Iterator subClassNameIter = subclasses.iterator();
                while (subClassNameIter.hasNext())
                {
                    //go deeper in subclasses
                    provideSubclassesForClass((String)subClassNameIter.next(), includeDescendents, consumer);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#getReferencedClasses(java.lang.String[], org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public List<AbstractClassMetaData> getReferencedClasses(String[] classNames, ClassLoaderResolver clr)
    {
        List<AbstractClassMetaData> cmds = new ArrayList();
        for (int i = 0; i < classNames.length; ++i)
        {
            Class cls = null;
            try
            {
                cls = clr.classForName(classNames[i]);
                if (!cls.isInterface())
                {
                    AbstractClassMetaData cmd = getMetaDataForClass(classNames[i], clr);
                    if (cmd == null)
                    {
                        NucleusLogger.DATASTORE.warn("Class Invalid " + classNames[i]);
                        throw new NoPersistenceInformationException(classNames[i]);
                    }
                    cmds.addAll(getReferencedClassMetaData(cmd, clr));
                }
            }
            catch (ClassNotResolvedException cnre)
            {
                // Class not found so ignore it
                NucleusLogger.DATASTORE.warn("Class " + classNames[i] + " not found so being ignored");
            }
        }
        return cmds;
    }

    /**
     * Utility to return all ClassMetaData that is referenced from the supplier class.
     * @param cmd The origin class's MetaData.
     * @param clr ClassLoaderResolver resolver for loading any classes.
     * @return List of ClassMetaData referenced by the origin
     */
    protected List<AbstractClassMetaData> getReferencedClassMetaData(AbstractClassMetaData cmd,
        ClassLoaderResolver clr)
    {
        if (cmd == null)
        {
            return null;
        }

        List<AbstractClassMetaData> orderedCMDs = new ArrayList();
        Set referencedCMDs = new HashSet();

        // Use the ClassMetaData to tell us about its classes
        cmd.getReferencedClassMetaData(orderedCMDs, referencedCMDs, clr, this);

        return orderedCMDs;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.metadata.MetaDataManager#isFieldTypePersistable(java.lang.Class)
     */
    @Override
    public boolean isFieldTypePersistable(Class type)
    {
        if (isEnhancing())
        {
            // Enhancing so return if we have MetaData that is persistable
            AbstractClassMetaData cmd = readMetaDataForClass(type.getName());
            if (cmd != null && cmd instanceof ClassMetaData &&
                cmd.getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE)
            {
                return true;
            }
        }
        return getApiAdapter().isPersistable(type);
    }
}