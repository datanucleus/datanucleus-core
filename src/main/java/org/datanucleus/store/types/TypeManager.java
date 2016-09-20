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
package org.datanucleus.store.types;

import java.util.Collection;
import java.util.Set;

import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Registry of java type support.
 * Provides information applicable to all datastores for how a field of a class is treated; 
 * whether it is by default persistent, whether it is by default embedded, whether it is in the DFG, 
 * and if it has a wrapper for SCO operations. Also stores whether the type can be converted to/from
 * a String (for datastores that don't provide storage natively).
 * Uses the plugin mechanism extension-point "org.datanucleus.java_type".
 */
public interface TypeManager
{
    void close();

    /**
     * Accessor for the supported second-class Types.
     * This may or may not be a complete list, just that it provides the principal ones.
     * @return Set of supported types (fully qualified class names).
     **/
    Set<String> getSupportedSecondClassTypes();

    /**
     * Accessor for whether a class is supported as being second class.
     * @param className The class name
     * @return Whether the class is supported (to some degree)
     */
    boolean isSupportedSecondClassType(String className);

    /**
     * Convenience method to filter out any supported classes from a list.
     * @param inputClassNames Names of the classes
     * @return Names of the classes (omitting supported types)
     */
    String[] filterOutSupportedSecondClassNames(String[] inputClassNames);

    /**
     * Accessor for whether the type is by default persistent.
     * TODO Support use of apiAdapter.isMemberDefaultPersistent to get strict JDO/JPA behaviour.
     * @param c The type
     * @return Whether persistent
     */
    boolean isDefaultPersistent(Class c);

    /**
     * Accessor for whether the type is by default in the DFG.
     * @param c The type
     * @return Whether in the DFG
     */
    boolean isDefaultFetchGroup(Class c);

    /**
     * Accessor for whether the generic collection type is by default in the DFG.
     * @param c The type
     * @param genericType The element generic type
     * @return Whether in the DFG
     */
    boolean isDefaultFetchGroupForCollection(Class c, Class genericType);

    /**
     * Accessor for whether the type is by default embedded.
     * @param c The type
     * @return Whether embedded
     */
    boolean isDefaultEmbeddedType(Class c);

    /**
     * Accessor for whether the type is SCO mutable.
     * @param className The type
     * @return Whether SCO mutable
     */
    boolean isSecondClassMutableType(String className);

    /**
     * Accessor for the SCO wrapper for the type
     * @param className The type
     * @return SCO wrapper
     */
    Class getWrapperTypeForType(String className);

    /**
     * Accessor for the backing-store Second Class Wrapper class for the supplied class.
     * A type will have a SCO wrapper if it is SCO supported and is mutable.
     * If there is no backed wrapper provided returns the simple wrapper.
     * @param className The class name
     * @return The second class wrapper
     */
    Class getWrappedTypeBackedForType(String className);

    /**
     * Accessor for whether the type is a SCO wrapper itself.
     * @param className The type
     * @return Whether is SCO wrapper
     */
    boolean isSecondClassWrapper(String className);

    /**
     * Accessor for a java type that the supplied class is a SCO wrapper for.
     * If the supplied class is not a SCO wrapper for anything then returns null.
     * @param className Name of the class
     * @return The java class that this is a wrapper for (or null)
     */
    Class getTypeForSecondClassWrapper(String className);

    /**
     * Obtains the registered ContainerHandler for the given containerClass. ContainerHandler are specified via the plugin mechanism using the
     * container-handler attribute of the java-type element.  
     * @param containerClass The class of the container.
     * @return The respective ContainerHandler if registered or null if no ContainerHandler is found for the containerClass.
     * @param <H> Handler type
     */
     <H extends ContainerHandler> H getContainerHandler(Class containerClass);
    
     /**
     * Convenience method to obtain the ContainerAdapter using the container object instance
     * @param container The container instance
     * @return The ContainerAdapter for the respective container or null if it's not a supported container
     */
    ContainerAdapter getContainerAdapter(Object container);

    /**
     * Accessor for the type converter with the provided name.
     * This is used when the user has specified metadata for a field to use a particular named converter.
     * @param converterName Name of the converter
     * @return The converter
     */
    TypeConverter getTypeConverterForName(String converterName);

    /**
     * Register a TypeConverter with the TypeManager process for specific attribute/db types.
     * TypeConverters are registered either from the contents of "plugin.xml" (i.e the builtin types) where the name is of the form "dn.*",
     * or from user-registered metadata (e.g JPA Annotations) where the name is the class name of the converter or a user supplied name.
     * @param name The name to register the converter under
     * @param converter The converter
     * @param memberType Type of the java member
     * @param dbType Type of the database column
     * @param autoApply Whether this should be used as an auto-apply converter
     * @param autoApplyType Java type to auto apply this for
     */
    void registerConverter(String name, TypeConverter converter, Class memberType, Class dbType, boolean autoApply, String autoApplyType);

    /**
     * Method to return a TypeConverter that should be applied by default for the specified java (member) type.
     * Will return null if the java type has no autoApply type defined for it (the default).
     * @param memberType The java (member) type
     * @return The converter to use by default
     */
    TypeConverter getAutoApplyTypeConverterForType(Class memberType);

    /**
     * Accessor for the default type converter for the provided Java type.
     * @param memberType Java type for the member
     * @return The default converter (if any)
     */
    TypeConverter getDefaultTypeConverterForType(Class memberType);

    /**
     * Method providing the ability for a datastore plugin to override the default converter type for the specified java type.
     * @param memberType Member type
     * @param converterName The converter to use by default. This is assumed to exist.
     */
    void setDefaultTypeConverterForType(Class memberType, String converterName);

    /**
     * Accessor for the type converter for the provided Java type and its datastore type.
     * @param memberType Java type for the member
     * @param datastoreType Java type for the datastore
     * @return The converter (if any)
     */
    TypeConverter getTypeConverterForType(Class memberType, Class datastoreType);

    /**
     * Accessor for the available type converters for the provided Java type.
     * @param memberType The java type
     * @return The available Type Converters
     */
    Collection<TypeConverter> getTypeConvertersForType(Class memberType);

    /**
     * Method to return the datastore type for the specified TypeConverter.
     * @param conv The converter
     * @param memberType The member type
     * @return The datastore type
     */
    Class getDatastoreTypeForTypeConverter(TypeConverter conv, Class memberType);

    /**
     * Method to return the member type for the specified TypeConverter.
     * @param conv The converter
     * @param datastoreType The datastore type for this converter
     * @return The member type
     */
    Class getMemberTypeForTypeConverter(TypeConverter conv, Class datastoreType);
}