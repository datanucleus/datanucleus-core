package org.datanucleus.store.types;

import java.util.Set;

import org.datanucleus.store.types.converters.TypeConverter;

public interface TypeManager
{

    /**
     * Accessor for the supported second-class Types.
     * This may or may not be a complete list, just that it provides the principal ones.
     * @return Set of supported types (fully qualified class names).
     **/
    public abstract Set<String> getSupportedSecondClassTypes();

    /**
     * Accessor for whether a class is supported as being second class.
     * @param className The class name
     * @return Whether the class is supported (to some degree)
     */
    public abstract boolean isSupportedSecondClassType(String className);

    /**
     * Convenience method to filter out any supported classes from a list.
     * @param inputClassNames Names of the classes
     * @return Names of the classes (omitting supported types)
     */
    public abstract String[] filterOutSupportedSecondClassNames(String[] inputClassNames);

    /**
     * Accessor for whether the type is by default persistent.
     * TODO Support use of apiAdapter.isMemberDefaultPersistent to get strict JDO/JPA behaviour.
     * @param c The type
     * @return Whether persistent
     */
    public abstract boolean isDefaultPersistent(Class c);

    /**
     * Accessor for whether the type is by default in the DFG.
     * @param c The type
     * @return Whether in the DFG
     */
    public abstract boolean isDefaultFetchGroup(Class c);

    /**
     * Accessor for whether the generic collection type is by default in the DFG.
     * @param c The type
     * @param genericType The element generic type
     * @return Whether in the DFG
     */
    public abstract boolean isDefaultFetchGroupForCollection(Class c, Class genericType);

    /**
     * Accessor for whether the type is by default embedded.
     * @param c The type
     * @return Whether embedded
     */
    public abstract boolean isDefaultEmbeddedType(Class c);

    /**
     * Accessor for whether the type is SCO mutable.
     * @param className The type
     * @return Whether SCO mutable
     */
    public abstract boolean isSecondClassMutableType(String className);

    /**
     * Accessor for the SCO wrapper for the type
     * @param className The type
     * @return SCO wrapper
     */
    public abstract Class getWrapperTypeForType(String className);

    /**
     * Accessor for the backing-store Second Class Wrapper class for the supplied class.
     * A type will have a SCO wrapper if it is SCO supported and is mutable.
     * If there is no backed wrapper provided returns the simple wrapper.
     * @param className The class name
     * @return The second class wrapper
     */
    public abstract Class getWrappedTypeBackedForType(String className);

    /**
     * Accessor for whether the type is a SCO wrapper itself.
     * @param className The type
     * @return Whether is SCO wrapper
     */
    public abstract boolean isSecondClassWrapper(String className);

    /**
     * Accessor for a java type that the supplied class is a SCO wrapper for.
     * If the supplied class is not a SCO wrapper for anything then returns null.
     * @param className Name of the class
     * @return The java class that this is a wrapper for (or null)
     */
    public abstract Class getTypeForSecondClassWrapper(String className);

    /**
     * Accessor for the type converter with the provided name.
     * This is used when the user has specified metadata for a field to use a particular named converter.
     * @param converterName Name of the converter
     * @return The converter
     */
    public abstract TypeConverter getTypeConverterForName(String converterName);

    /**
     * Register a TypeConverter with the TypeManager process.
     * @param name The name to register the converter under
     * @param converter The converter
     * @param autoApply Whether this should be used as an auto-apply converter
     */
    public abstract void registerConverter(String name, TypeConverter converter, boolean autoApply, String autoApplyType);

    /**
     * TypeConverters are registered either from the contents of "plugin.xml" (i.e the builtin types) where the
     * name is of the form "dn.*", or from user-registered metadata (e.g JPA Annotations) where the name is
     * the class name of the converter.
     * @param name The name to register the converter under
     * @param converter The converter
     */
    public abstract void registerConverter(String name, TypeConverter converter);

    /**
     * Method to return a TypeConverter that should be applied by default for the specified java (member) type.
     * Will return null if the java type has no autoApply type defined for it (the default).
     * @param memberType The java (member) type
     * @return The converter to use by default
     */
    public abstract TypeConverter getAutoApplyTypeConverterForType(Class memberType);

    /**
     * Accessor for the default type converter for the provided Java type.
     * @param memberType Java type for the member
     * @return The default converter (if any)
     */
    public abstract TypeConverter getDefaultTypeConverterForType(Class memberType);

    /**
     * Accessor for the type converter for the provided Java type and its datastore type.
     * @param memberType Java type for the member
     * @param datastoreType Java type for the datastore
     * @return The converter (if any)
     */
    public abstract TypeConverter getTypeConverterForType(Class memberType, Class datastoreType);

}