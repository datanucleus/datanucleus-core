/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved. 
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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.api.ApiAdapter;
import org.datanucleus.exceptions.NoPersistenceInformationException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.annotations.AnnotationManager;

/**
 * Manager for metadata in DataNucleus.
 * The <i>MetaDataManager</i> manages the metadata for classes/members.
 * MetaData can be derived from annotations, or XML, or via an API, or a mixture of all.
 * MetaData from different sources is merged using <i>MetaDataMerger</i>
 * 
 * <h3>persistence.xml</h3>
 * This class does not handle the parsing of a persistence-unit metadata from a "persistence.xml" file.
 * That is handled by <i>MetaDataUtils.getMetaDataForPersistenceUnit</i> which returns the <i>PersistenceUnitMetaData</i> and
 * by <i>MetaDataUtils.parsePersistenceFiles</i> which parses all <i>PersistenceFileMetaData</i> from the available "persistence.xml" file(s).
 */
public interface MetaDataManager
{
    void close();

    NucleusContext getNucleusContext();

    ApiAdapter getApiAdapter();

    AnnotationManager getAnnotationManager();

    /**
     * Method to return the prefix applied to all enhancer method names
     * @return The enhancer method name prefix (e.g "dn")
     */
    String getEnhancedMethodNamePrefix();

    /**
     * Method to return whether the specified member is an enhancer-provided member.
     * @param memberName Name of the member
     * @return Whether it was added by the enhancer (e.g prefix "dn")
     */
    boolean isEnhancerField(String memberName);

    /**
     * Method to register a listener to be notified when metadata for a class/interface is initialised.
     * @param listener The listener
     */
    void registerListener(MetaDataListener listener);

    /**
     * Method to deregister a listener from being notified when metadata for a class/interface is initialised.
     * @param listener The listener
     */
    void deregisterListener(MetaDataListener listener);

    void setAllowMetaDataLoad(boolean allow);

    void setAllowXML(boolean allow);
    
    void setDefaultNullable(boolean nullable);

    void setAllowAnnotations(boolean allow);

    /**
     * Mutator for whether to validate the MetaData files for XML compliance.
     * @param validate Whether to validate
     */
    void setValidate(boolean validate);

    /**
     * Mutator for whether to support XML namespaces.
     * @param aware Whether to be XML namespace aware
     */
    void setXmlNamespaceAware(boolean aware);

    /**
     * Accessor for whether the MetaData manager supports ORM concepts and metadata.
     * With object datastores this will return false.
     * @return Whether we support ORM
     */
    boolean supportsORM();
    
    /**
     * Acessor for the default nullability of fields.
     * @return true if fields should be null by default and false whether it should be not-null.
     */
    boolean isDefaultNullable();

    /**
     * Accessor for whether we are managing the enhancement process.
     * @return Whether we are enhancing
     */
    boolean isEnhancing();

    /**
     * Initialisation method to load up all metadata defined by the specified metadata files.
     * Metadata files can be absolute/relative filenames, or can be resources in the CLASSPATH.
     * @param metadataFiles The metadata files
     * @param loader ClassLoader to use in loading the metadata (if any)
     * @return Array of the FileMetaData that is managed
     * @throws NucleusUserException (with nested exceptions) if an error occurs parsing the files
     */
    FileMetaData[] loadMetaDataFiles(String[] metadataFiles, ClassLoader loader);

    /**
     * Initialisation method to load up all metadata for the specified classes.
     * @param classNames The class names
     * @param loader ClassLoader to use in loading the classes (if any)
     * @return Array of the FileMetaData that is managed
     * @throws NucleusUserException (with nested exceptions) if an error occurs parsing the files
     */
    FileMetaData[] loadClasses(String[] classNames, ClassLoader loader);

    /**
     * Initialisation method to load the metadata provided by the specified jar.
     * @param jarFileName Name of the jar file
     * @param loader ClassLoader to use in loading of the jar (if any)
     * @return Array of the FileMetaData that is managed
     * @throws NucleusUserException if an error occurs parsing the jar info
     */
    FileMetaData[] loadJar(String jarFileName, ClassLoader loader);

    /**
     * Initialisation method to to load all class metadata defined by the "persistence-unit".
     * @param pumd The MetaData for this "persistence-unit"
     * @param loader ClassLoader to use in loading of the persistence unit (if any)
     * @return Array of the FileMetaData that is managed
     * @throws NucleusUserException if an error occurs parsing the persistence-unit info
     */
    FileMetaData[] loadPersistenceUnit(PersistenceUnitMetaData pumd, ClassLoader loader);

    /**
     * Method to load user-provided (dynamic) metadata (from the JDO MetaData API).
     * @param fileMetaData FileMetaData to register/populate/initialise
     * @param loader ClassLoader to use in loading the metadata (if any)
     */
    void loadUserMetaData(FileMetaData fileMetaData, ClassLoader loader);

    /**
     * Method to load the metadata from the specified files.
     * Supports absolute/relative file names, or CLASSPATH resources.
     * @param metadataFiles array of MetaData files
     * @param clr ClassLoader resolver
     * @return List of FileMetaData
     */
    Collection<FileMetaData> loadFiles(String[] metadataFiles, ClassLoaderResolver clr);

    /**
     * Convenience method to allow the unloading of metadata, for example where the user wants to reload a class definition
     * and that class maybe has different metadata with the new definition.
     * @param className Name of the class
     */
    void unloadMetaDataForClass(String className);

    /**
     * Convenience method to return if the specified class is a known persistable class.
     * @param className Name of the class
     * @return Whether it is persistable
     */
    boolean isClassPersistable(String className);

    /**
     * Accessor for all FileMetaData currently managed here.
     * @return FileMetaData managed here currently
     */
    FileMetaData[] getFileMetaData();

    /**
     * Accessor for the names of the classes with MetaData currently registered with this manager.
     * @return Names of classes with MetaData
     */
    Collection<String> getClassesWithMetaData();

    /**
     * Convenience method to check if we have metadata present for the specified class.
     * @param className The name of the class to check
     * @return Whether the metadata is already registered for this class
     */
    boolean hasMetaDataForClass(String className);

    /**
     * Accessor for the metadata for the class(es) with the specified object-id class name as PK.
     * This only works for user-provided object-id classes (not SingleFieldIdentity).
     * @param objectIdClassName The object-id class name
     * @return Collection of AbstractClassMetaData for the classes using this PK
     */
    Collection<AbstractClassMetaData> getClassMetaDataWithApplicationId(String objectIdClassName);

    /**
     * Accessor for the MetaData for a class given the name and a loader.
     * All MetaData returned from this method will be initialised and ready for full use.
     * If the class can't be loaded, null will be returned. 
     * @param className Name of the class to find MetaData for
     * @param clr ClassLoaderResolver resolver for use in loading the class.
     * @return The ClassMetaData for this class (or null if not found)
     **/
    AbstractClassMetaData getMetaDataForClass(String className, ClassLoaderResolver clr);

    /**
     * Primary accessor for the MetaData for a class.
     * All MetaData returned from this method will be initialised and ready for full use.
     * @param c The class to find MetaData for
     * @param clr the ClassLoaderResolver
     * @return The ClassMetaData for this class (or null if not found)
     */
    AbstractClassMetaData getMetaDataForClass(Class c, ClassLoaderResolver clr);

    /**
     * Accessor for the MetaData for a class given the "entity-name".
     * @param entityName The entity name to find MetaData for
     * @return The ClassMetaData for this entity name (or null if not found)
     */
    AbstractClassMetaData getMetaDataForEntityName(String entityName);

    /**
     * Accessor for the MetaData for a class given the "discriminator".
     * @param discriminator The discriminator name to find MetaData for
     * @return The ClassMetaData for this discriminator (or null if not found)
     */
    AbstractClassMetaData getMetaDataForDiscriminator(String discriminator);

    /**
     * Method to access the (already known) metadata for the specified class.
     * If the class is not yet known about it returns null.
     * Only used by org.datanucleus.metadata classes.
     * @param className Name of the class
     * @return MetaData for the class
     */
    AbstractClassMetaData readMetaDataForClass(String className);

    /**
     * Method to access the (already known) metadata for the field/property of the specified class.
     * If the class (or this field/property) is not yet known about it returns null.
     * Only used by org.datanucleus.metadata classes.
     * @param className Name of the class
     * @param memberName Name of the field/property
     * @return MetaData for the field/property
     */
    AbstractMemberMetaData readMetaDataForMember(String className, String memberName);

    /**
     * Internal convenience method for accessing the MetaData for a class.
     * MetaData returned by this method may be uninitialised so should only really
     * be used in initialisation processes.
     * To be implemented by the implementing class.
     * @param c The class to find MetaData for
     * @param clr ClassLoader resolver
     * @return The ClassMetaData for this class (or null if not found)
     **/
    AbstractClassMetaData getMetaDataForClassInternal(Class c, ClassLoaderResolver clr);

    /**
     * Accessor for the subclasses of a particular class
     * @param className Name of the class that we want the known subclasses for.
     * @param includeDescendents Whether to include subclasses of subclasses etc
     * @return Names of the subclasses. return null if there are no subclasses
     */
    String[] getSubclassesForClass(String className, boolean includeDescendents);

    /**
     * Accessor for the names of all concrete subclasses of the provided class.
     * @param className Name of the class that we want the known concrete subclasses for.
     * @return Names of the subclasses. Returns null if there are no subclasses
     */
    String[] getConcreteSubclassesForClass(String className);

    /**
     * Accessor for the list of names of classes that are declared to implement the specified interface
     * (using &lt;implements&gt; in the MetaData). This will include subclasses of declared classes. Ignore abstract classes.
     * The array of implementation class names will have the initial implementations first followed by
     * the subclass implementations etc. So for example if we look for all implementations of I and A implements I
     * and B extends A, then it will return [A, B] in that order.
     * @param interfaceName Name of the interface
     * @param clr The ClassLoaderResolver
     * @return The names of the classes declared as implementing that interface. return null if no classes
     */
    String[] getClassesImplementingInterface(String interfaceName, ClassLoaderResolver clr);

    /**
     * Accessor for the MetaData for an implementation of a reference type.
     * Finds the metadata for the implementation of this reference.
     * @param referenceClass The reference class to find MetaData for
     * @param implValue Object of an implementation class, to return if possible (null=ignore)
     * @param clr ClassLoader resolver
     * @return The ClassMetaData for an implementation of a reference type
     */
    ClassMetaData getMetaDataForImplementationOfReference(Class referenceClass, Object implValue, ClassLoaderResolver clr);

    /**
     * Accessor for the MetaData for a named query for a class.
     * If the class is not specified, searches for the query with this name for any class.
     * Will only return metadata for queries already registered in this implementation.
     * @param cls The class which has the query defined for it
     * @param clr the ClassLoaderResolver
     * @param queryName Name of the query
     * @return The QueryMetaData for the query for this class
     **/
    QueryMetaData getMetaDataForQuery(Class cls, ClassLoaderResolver clr, String queryName);

    /**
     * Convenience method to access the names of named queries that are registered with this manager.
     * @return Names of the named queries
     */
    Set<String> getNamedQueryNames();

    /**
     * Method to register a named query.
     * @param qmd The definition of the query, with its name
     */
    void registerNamedQuery(QueryMetaData qmd);

    /**
     * Accessor for the MetaData for a named stored procedure query for a class.
     * If the class is not specified, searches for the query with this name for any class.
     * Will only return metadata for queries already registered in this implementation.
     * @param cls The class which has the query defined for it
     * @param clr the ClassLoaderResolver
     * @param queryName Name of the (stored proc) query
     * @return The StoredProcQueryMetaData for the query for this class
     **/
    StoredProcQueryMetaData getMetaDataForStoredProcQuery(Class cls, ClassLoaderResolver clr, String queryName);

    /**
     * Accessor for the MetaData for a named fetch plan.
     * @param name Name of the fetch plan
     * @return The FetchPlanMetaData for this name (if any)
     **/
    FetchPlanMetaData getMetaDataForFetchPlan(String name);

    /**
     * Accessor for the MetaData for a Sequence in a package.
     * This implementation simply checks what is already loaded and returns if found
     * @param clr the ClassLoaderResolver
     * @param seqName Name of the package (fully qualified if necessary)
     * @return The SequenceMetaData for this named sequence
     **/
    SequenceMetaData getMetaDataForSequence(ClassLoaderResolver clr, String seqName);

    /**
     * Accessor for the MetaData for a TableGenerator in a package.
     * This implementation simply checks what is already loaded and returns if found
     * @param clr the ClassLoaderResolver
     * @param genName Name of the package (fully qualified if necessary)
     * @return The TableGenerator for this named generator
     **/
    TableGeneratorMetaData getMetaDataForTableGenerator(ClassLoaderResolver clr, String genName);

    /**
     * Accessor for the MetaData for a QueryResult.
     * @param name Name of the query result
     * @return The QueryResultMetaData under this name
     **/
    QueryResultMetaData getMetaDataForQueryResult(String name);

    /**
     * Accessor for the MetaData for an interface.
     * Part of the support for "persistent-interface".
     * This defaults to returning null since interfaces are only supported by JDO.
     * @param c The interface to find MetaData for
     * @param clr the ClassLoaderResolver
     * @return The InterfaceMetaData for this interface (or null if not found)
     */
    InterfaceMetaData getMetaDataForInterface(Class c, ClassLoaderResolver clr);

    /**
     * Convenience method to return if the passed class name is a "persistent-interface".
     * @param name Name if the interface
     * @return Whether it is a "persistent-interface"
     */
    boolean isPersistentInterface(String name);

    /**
     * Convenience method to return if the passed class name is an implementation of the passed "persistent-interface".
     * @param interfaceName Name of the persistent interface
     * @param implName The implementation name
     * @return Whether it is a (DataNucleus-generated) impl of the persistent interface
     */
    boolean isPersistentInterfaceImplementation(String interfaceName, String implName);

    /**
     * Accessor for the implementation name for the specified "persistent-interface".
     * @param interfaceName The name of the persistent interface
     * @return The name of the implementation class
     */
    String getImplementationNameForPersistentInterface(String interfaceName);

    /**
     * Method to take the FileMetaData and register the relevant parts of it with the assorted caches provided.
     * Note : this is only public to allow enhancer tests to load up metadata manually.
     * @param fileURLString URL of the metadata file
     * @param filemd The File MetaData
     * @param clr ClassLoader resolver
     */
    void registerFile(String fileURLString, FileMetaData filemd, ClassLoaderResolver clr);

    /**
     * Method to return the class name that uses the provided discriminator value using the specified root class to search from.
     * @param rootCmd The root class
     * @param discrimValue The discriminator value
     * @return The class using this value
     */
    String getClassNameForDiscriminatorValueWithRoot(AbstractClassMetaData rootCmd, String discrimValue);

    /**
     * Method to return the discriminator value used by the specified class.
     * @param cmd Class to search for
     * @return The discriminator value used by this class
     */
    String getDiscriminatorValueForClass(AbstractClassMetaData cmd);

    /**
     * Method to return the class name that uses the specified discriminator value for the specified discriminator.
     * @param discrimValue Discriminator value
     * @param dismd The discriminator metadata
     * @return The class name (or null if not found)
     */
    String getClassNameFromDiscriminatorValue(String discrimValue, DiscriminatorMetaData dismd);

    /**
     * Convenience method to get the MetaData for all referenced classes with the passed set of classes as root.
     * @param classNames Names of the root classes
     * @param clr ClassLoader resolver
     * @return List of AbstractClassMetaData objects for the referenced classes
     * @throws NoPersistenceInformationException thrown when one of the classes has no metadata.
     */
    List<AbstractClassMetaData> getReferencedClasses(String[] classNames, ClassLoaderResolver clr);

    /**
     * Utility to return if this field is of a persistable type.
     * @param type Type of the field (for when "type" is not yet set)
     * @return Whether the field type is persistable.
     */
    boolean isFieldTypePersistable(Class type);

    /**
     * Method to register a persistent interface and its implementation with the MetaData system.
     * This is called by the JDO ImplementationCreator.
     * @param imd MetaData for the interface
     * @param implClass The implementation class
     * @param clr ClassLoader Resolver to use
     */
    void registerPersistentInterface(InterfaceMetaData imd, Class implClass, ClassLoaderResolver clr);

    /**
     * Method to register the metadata for an implementation of a persistent abstract class.
     * This is called by the JDO ImplementationCreator.
     * @param cmd MetaData for the abstract class
     * @param implClass The implementation class
     * @param clr ClassLoader resolver
     */
    void registerImplementationOfAbstractClass(ClassMetaData cmd, Class implClass, ClassLoaderResolver clr);

    // These methods are part of the internal load process so ought to be protected/private

    /**
     * Load up and add any O/R mapping info for the specified class to the stored ClassMetaData (if supported).
     * Only to be invoked by ClassMetaData, InterfaceMetaData.
     * @param c The class
     * @param clr ClassLoader resolver
     */
    void addORMDataToClass(Class c, ClassLoaderResolver clr);

    /**
     * Load up and add any annotations mapping info for the specified class to the stored ClassMetaData.
     * Only to be invoked by ClassMetaData, InterfaceMetaData.
     * @param c The class
     * @param cmd the metadata to add annotation to
     * @param clr ClassLoader resolver
     */
    void addAnnotationsDataToClass(Class c, AbstractClassMetaData cmd, ClassLoaderResolver clr);

    /**
     * Method called (by AbstractClassMetaData.initialise()) when a class/interface has its metadata initialised.
     * Only to be invoked by ClassMetaData, InterfaceMetaData.
     * @param cmd Metadata that has been initialised
     */
    void abstractClassMetaDataInitialised(AbstractClassMetaData cmd);

    /**
     * Convenience method to register all sequences found in the passed file.
     * @param filemd MetaData for the file
     */
    void registerSequencesForFile(FileMetaData filemd);

    /**
     * Convenience method to register all table generators found in the passed file.
     * @param filemd MetaData for the file
     */
    void registerTableGeneratorsForFile(FileMetaData filemd);

    /**
     * Convenience method to register the discriminator value used by the specified class for easy lookup.
     * @param cmd Metadata for the class
     * @param discrimValue The discriminator value
     */
    void registerDiscriminatorValueForClass(AbstractClassMetaData cmd, String discrimValue);
}