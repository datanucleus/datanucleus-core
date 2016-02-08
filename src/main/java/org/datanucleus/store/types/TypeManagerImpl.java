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
2004 Erik Bengtson - added interfaces methods
2008 Andy Jefferson - added java type handling separate from mapped types
    ...
**********************************************************************/
package org.datanucleus.store.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.NucleusContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.plugin.ConfigurationElement;
import org.datanucleus.plugin.PluginManager;
import org.datanucleus.store.types.converters.ClassStringConverter;
import org.datanucleus.store.types.converters.TypeConverter;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Implementation of registry of java type support.
 * Provides information applicable to all datastores for how a field of a class is treated; 
 * whether it is by default persistent, whether it is by default embedded, whether it is in the DFG, 
 * and if it has a wrapper for SCO operations. Also stores whether the type can be converted to/from
 * a String (for datastores that don't provide storage natively).
 * Uses the plugin mechanism extension-point "org.datanucleus.java_type".
 */
public class TypeManagerImpl implements TypeManager, Serializable
{
    private static final long serialVersionUID = 8217508318434539002L;

    protected NucleusContext nucCtx;

    protected transient ClassLoaderResolver clr;

    /** Map of java types, keyed by the class name. */
    protected Map<String, JavaType> javaTypes = new ConcurrentHashMap();
    
    /** Map of ContainerHandlers, keyed by the container type class name. */
    protected Map<Class, ? super ContainerHandler> containerHandlersByClass = new ConcurrentHashMap();

    /** Map of TypeConverter keyed by their symbolic name. */
    protected Map<String, TypeConverter> convertersByName = null;

    /** Map of TypeConverter keyed by type name that we should default to for this type (user-defined). */
    protected Map<String, TypeConverter> autoApplyConvertersByType = null;

    /** Map of (Map of TypeConverter keyed by the datastore type), keyed by the member type. */
    protected Map<Class, Map<Class, TypeConverter>> typeConverterMap = null;
    

    /**
     * Constructor, loading support for type mappings using the plugin mechanism.
     * @param nucCtx NucleusContext
     */
    public TypeManagerImpl(NucleusContext nucCtx)
    {
        this.nucCtx = nucCtx;
        loadJavaTypes(nucCtx.getPluginManager());
        loadTypeConverters(nucCtx.getPluginManager());
    }

    protected ClassLoaderResolver getClassLoaderResolver()
    {
        if (clr == null)
        {
            clr = nucCtx.getClassLoaderResolver(null);
        }
        return clr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getSupportedSecondClassTypes()
     */
    @Override
    public Set<String> getSupportedSecondClassTypes()
    {
        return new HashSet(javaTypes.keySet());
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isSupportedSecondClassType(java.lang.String)
     */
    @Override
    public boolean isSupportedSecondClassType(String className)
    {
        if (className == null)
        {
            return false;
        }
        JavaType type = javaTypes.get(className);
        if (type == null)
        {
            try
            {
                Class cls = getClassLoaderResolver().classForName(className);
                type = findJavaTypeForClass(cls);
                return (type != null);
            }
            catch (Exception e)
            {
            }
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#filterOutSupportedSecondClassNames(java.lang.String[])
     */
    @Override
    public String[] filterOutSupportedSecondClassNames(String[] inputClassNames)
    {
        // Filter out any "simple" type classes
        int filteredClasses = 0;
        for (int i = 0; i < inputClassNames.length; ++i)
        {
            if (isSupportedSecondClassType(inputClassNames[i]))
            {
                inputClassNames[i] = null;
                ++filteredClasses;
            }
        }
        if (filteredClasses == 0)
        {
            return inputClassNames;
        }
        String[] restClasses = new String[inputClassNames.length - filteredClasses];
        int m = 0;
        for (int i = 0; i < inputClassNames.length; ++i)
        {
            if (inputClassNames[i] != null)
            {
                restClasses[m++] = inputClassNames[i];
            }
        }
        return restClasses;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isDefaultPersistent(java.lang.Class)
     */
    @Override
    public boolean isDefaultPersistent(Class c)
    {
        if (c == null)
        {
            return false;
        }

        JavaType type = javaTypes.get(c.getName());
        if (type != null)
        {
            return true;
        }

        // Try to find a class that this class extends that is supported
        type = findJavaTypeForClass(c);
        if (type != null)
        {
            return true;
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isDefaultFetchGroup(java.lang.Class)
     */
    @Override
    public boolean isDefaultFetchGroup(Class c)
    {
        if (c == null)
        {
            return false;
        }

        if (nucCtx.getApiAdapter().isPersistable(c))
        {
            // 1-1/N-1 (persistable field), so return what the API default is
            return nucCtx.getApiAdapter().getDefaultDFGForPersistableField();
        }

        JavaType type = javaTypes.get(c.getName());
        if (type != null)
        {
            // Field type defined in plugins, so return the setting
            return type.dfg;
        }

        // Try to find a class that this class extends that is supported
        type = findJavaTypeForClass(c);
        if (type != null)
        {
            return type.dfg;
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isDefaultFetchGroupForCollection(java.lang.Class, java.lang.Class)
     */
    @Override
    public boolean isDefaultFetchGroupForCollection(Class c, Class genericType)
    {
        if (c != null && genericType == null)
        {
            return isDefaultFetchGroup(c);
        }
        else if (c == null)
        {
            return false;
        }

        String name = c.getName() + "<" + genericType.getName() + ">";
        JavaType type = javaTypes.get(name);
        if (type != null)
        {
            return type.dfg;
        }

        // Try to find a class that this class extends that is supported
        type = findJavaTypeForCollectionClass(c, genericType);
        if (type != null)
        {
            return type.dfg;
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isDefaultEmbeddedType(java.lang.Class)
     */
    @Override
    public boolean isDefaultEmbeddedType(Class c)
    {
        if (c == null)
        {
            return false;
        }

        JavaType type = javaTypes.get(c.getName());
        if (type != null)
        {
            return type.embedded;
        }

        // Try to find a class that this class extends that is supported
        type = findJavaTypeForClass(c);
        if (type != null)
        {
            return type.embedded;
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isSecondClassMutableType(java.lang.String)
     */
    @Override
    public boolean isSecondClassMutableType(String className)
    {
        return (getWrapperTypeForType(className) != null);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getWrapperTypeForType(java.lang.String)
     */
    @Override
    public Class getWrapperTypeForType(String className)
    {
        if (className == null)
        {
            return null;
        }

        JavaType type = javaTypes.get(className);
        return type == null ? null : type.wrapperType;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getWrappedTypeBackedForType(java.lang.String)
     */
    @Override
    public Class getWrappedTypeBackedForType(String className)
    {
        if (className == null)
        {
            return null;
        }

        JavaType type = javaTypes.get(className);
        return type == null ? null : type.wrapperTypeBacked;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#isSecondClassWrapper(java.lang.String)
     */
    @Override
    public boolean isSecondClassWrapper(String className)
    {
        if (className == null)
        {
            return false;
        }

        // Check java types with wrappers
        Iterator iter = javaTypes.values().iterator();
        while (iter.hasNext())
        {
            JavaType type = (JavaType)iter.next();
            if (type.wrapperType != null && type.wrapperType.getName().equals(className))
            {
                return true;
            }
            if (type.wrapperTypeBacked != null && type.wrapperTypeBacked.getName().equals(className))
            {
                return true;
            }
        }

        return false;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getTypeForSecondClassWrapper(java.lang.String)
     */
    @Override
    public Class getTypeForSecondClassWrapper(String className)
    {
        Iterator iter = javaTypes.values().iterator();
        while (iter.hasNext())
        {
            JavaType type = (JavaType)iter.next();
            if (type.wrapperType != null && type.wrapperType.getName().equals(className))
            {
                return type.cls;
            }
            if (type.wrapperTypeBacked != null && type.wrapperTypeBacked.getName().equals(className))
            {
                return type.cls;
            }
        }
        return null;
    }
    
    @Override
    public ContainerAdapter getContainerAdapter(Object container)
    {
        ContainerHandler containerHandler = getContainerHandler(container.getClass());
        return containerHandler == null ? null : containerHandler.getAdapter(container);
    }
    
    @Override
    public <H extends ContainerHandler> H getContainerHandler(Class containerClass)
    {
        H containerHandler = (H) containerHandlersByClass.get(containerClass);
        if (containerHandler == null)
        {
            // Try to find the container handler using the registered type
            JavaType type = findJavaTypeForClass(containerClass);
            if (type != null && type.containerHandlerType != null)
            {
                Class[] parameterTypes = null;
                Object[] parameters = null;
                
                Class[] classParameterTypes = new Class[]{Class.class};

                // Allow ContainerHandlers that receive the container type on the constructor. e.g. ArrayHandler
                if (ClassUtils.getConstructorWithArguments(type.containerHandlerType, classParameterTypes) != null )
                {
                    parameterTypes = classParameterTypes;
                    parameters = new Object[] {containerClass};
                }
                
                containerHandler = (H) ClassUtils.newInstance(type.containerHandlerType, parameterTypes, parameters);
                
                containerHandlersByClass.put(containerClass, containerHandler);
            }
        }

        return containerHandler;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getTypeConverterForName(java.lang.String)
     */
    @Override
    public TypeConverter getTypeConverterForName(String converterName)
    {
        return (convertersByName == null || converterName == null) ? null : convertersByName.get(converterName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#registerConverter(java.lang.String, org.datanucleus.store.types.converters.TypeConverter, boolean, java.lang.String)
     */
    @Override
    public void registerConverter(String name, TypeConverter converter, boolean autoApply, String autoApplyType)
    {
        if (convertersByName == null)
        {
            convertersByName = new ConcurrentHashMap<String, TypeConverter>();
        }
        convertersByName.put(name, converter);
        if (converter instanceof ClassStringConverter)
        {
            ((ClassStringConverter)converter).setClassLoaderResolver(getClassLoaderResolver());
        }
        if (autoApply)
        {
            if (autoApplyConvertersByType == null)
            {
                autoApplyConvertersByType = new ConcurrentHashMap<String, TypeConverter>();
            }
            autoApplyConvertersByType.put(autoApplyType, converter);
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#registerConverter(java.lang.String, org.datanucleus.store.types.converters.TypeConverter)
     */
    @Override
    public void registerConverter(String name, TypeConverter converter)
    {
        registerConverter(name, converter, false, null);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getAutoApplyTypeConverterForType(java.lang.Class)
     */
    @Override
    public TypeConverter getAutoApplyTypeConverterForType(Class memberType)
    {
        return autoApplyConvertersByType == null ? null : autoApplyConvertersByType.get(memberType.getName());
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#setDefaultTypeConverterForType(java.lang.Class, java.lang.String)
     */
    @Override
    public void setDefaultTypeConverterForType(Class memberType, String converterName)
    {
        JavaType javaType = javaTypes.get(memberType.getName());
        if (javaType == null)
        {
            return;
        }

        String typeConverterName = javaType.typeConverterName;
        if (typeConverterName == null || !typeConverterName.equals(converterName))
        {
            javaType.typeConverterName = converterName;
        }
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getDefaultTypeConverterForType(java.lang.Class)
     */
    @Override
    public TypeConverter getDefaultTypeConverterForType(Class memberType)
    {
        JavaType javaType = javaTypes.get(memberType.getName());
        if (javaType == null)
        {
            return null;
        }
        String typeConverterName = javaType.typeConverterName;
        if (typeConverterName == null)
        {
            return null;
        }
        return getTypeConverterForName(typeConverterName);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getTypeConverterForType(java.lang.Class, java.lang.Class)
     */
    @Override
    public TypeConverter getTypeConverterForType(Class memberType, Class datastoreType)
    {
        if (typeConverterMap == null || memberType == null)
        {
            return null;
        }

        Map<Class, TypeConverter> convertersForMember = typeConverterMap.get(memberType);
        if (convertersForMember == null)
        {
            return null;
        }
        return convertersForMember.get(datastoreType);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.types.TypeManager#getTypeConvertersForType(java.lang.Class)
     */
    @Override
    public Collection<TypeConverter> getTypeConvertersForType(Class memberType)
    {
        if (typeConverterMap == null || memberType == null)
        {
            return null;
        }

        Map<Class, TypeConverter> convertersForMember = typeConverterMap.get(memberType);
        if (convertersForMember == null)
        {
            return null;
        }
        return convertersForMember.values();
    }

    /**
     * Convenience method to return the JavaType for the specified class. If this class has a defined
     * JavaType then returns it. If not then tries to find a superclass that is castable to the specified type.
     * @param cls The class required
     * @return The JavaType
     */
    protected JavaType findJavaTypeForClass(Class cls)
    {
        if (cls == null)
        {
            return null;
        }
        JavaType type = javaTypes.get(cls.getName());
        if (type != null)
        {
            return type;
        }

        // Not supported so try to find one that is supported that this class derives from
        Collection supportedTypes = new HashSet(javaTypes.values());
        Iterator iter = supportedTypes.iterator();
        while (iter.hasNext())
        {
            type = (JavaType)iter.next();
            if (type.cls == cls && type.genericType == null)
            {
                return type;
            }
            if (!type.cls.getName().equals("java.lang.Object") && !type.cls.getName().equals("java.io.Serializable"))
            {
                Class componentCls = (cls.isArray() ? cls.getComponentType() : null);
                if (componentCls != null)
                {
                    // Array type
                    if (type.cls.isArray() && type.cls.getComponentType().isAssignableFrom(componentCls))
                    {
                        javaTypes.put(cls.getName(), type); // Register this subtype for reference
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug(Localiser.msg("016001", cls.getName(), type.cls.getName()));
                        }
                        return type;
                    }
                }
                else
                {
                    // Basic type
                    if (type.cls.isAssignableFrom(cls) && type.genericType == null)
                    {
                        javaTypes.put(cls.getName(), type); // Register this subtype for reference
                        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                        {
                            NucleusLogger.PERSISTENCE.debug(Localiser.msg("016001", cls.getName(), type.cls.getName()));
                        }
                        return type;
                    }
                }
            }
        }

        // Not supported
        return null;
    }

    /**
     * Convenience method to return the JavaType for the specified class. If this class has a defined
     * JavaType then returns it. If not then tries to find a superclass that is castable to the specified
     * type.
     * @param cls The class required
     * @param genericType Any generic type specified for the element
     * @return The JavaType
     */
    protected JavaType findJavaTypeForCollectionClass(Class cls, Class genericType)
    {
        if (cls == null)
        {
            return null;
        }
        else if (genericType == null)
        {
            return findJavaTypeForClass(cls);
        }

        String typeName = cls.getName() + "<" + genericType.getName() + ">";
        JavaType type = javaTypes.get(typeName);
        if (type != null)
        {
            return type;
        }

        // Not supported so try to find one that is supported that this class derives from
        Collection supportedTypes = new HashSet(javaTypes.values());
        Iterator iter = supportedTypes.iterator();
        while (iter.hasNext())
        {
            type = (JavaType)iter.next();
            if (type.cls.isAssignableFrom(cls))
            {
                if (type.genericType != null && type.genericType.isAssignableFrom(genericType))
                {
                    javaTypes.put(typeName, type); // Register this subtype for reference
                    return type;
                }
            }
        }

        // Fallback to just matching the collection type and forget the generic detail
        return findJavaTypeForClass(cls);
    }

    static class JavaType implements Serializable
    {
        private static final long serialVersionUID = -811442140006259453L;
        final Class cls;
        final Class genericType;
        final boolean embedded;
        final boolean dfg;
        final Class wrapperType;
        final Class wrapperTypeBacked;
        String typeConverterName;
        final Class containerHandlerType;

        public JavaType(Class cls, Class genericType, boolean embedded, boolean dfg, 
                Class wrapperType, Class wrapperTypeBacked, Class containerHandlerType, String typeConverterName)
        {
            this.cls = cls;
            this.genericType = genericType;
            this.embedded = embedded;
            this.dfg = dfg;
            this.wrapperType = wrapperType;
            this.wrapperTypeBacked = (wrapperTypeBacked != null ? wrapperTypeBacked : wrapperType);
            this.containerHandlerType = containerHandlerType;
            this.typeConverterName = typeConverterName;
        }
    }

    /**
     * Method to load the java type that are currently registered in the PluginManager.
     * @param mgr the PluginManager
     */
    private void loadJavaTypes(PluginManager mgr)
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("016003"));
        }
        ClassLoaderResolver clr = getClassLoaderResolver();
        ConfigurationElement[] elems = mgr.getConfigurationElementsForExtension("org.datanucleus.java_type", null, null);
        if (elems != null)
        {
            for (int i=0; i<elems.length; i++)
            {
                String javaName = elems[i].getAttribute("name").trim();
                String genericTypeName = elems[i].getAttribute("generic-type");
                String embeddedString = elems[i].getAttribute("embedded");
                String dfgString = elems[i].getAttribute("dfg");
                String wrapperType = elems[i].getAttribute("wrapper-type");
                String wrapperTypeBacked = elems[i].getAttribute("wrapper-type-backed");
                String typeConverterName = elems[i].getAttribute("converter-name");
                String containerHandlerType = elems[i].getAttribute("container-handler");

                boolean embedded = false;
                if (embeddedString != null && embeddedString.equalsIgnoreCase("true"))
                {
                    embedded = true;
                }
                boolean dfg = false;
                if (dfgString != null && dfgString.equalsIgnoreCase("true"))
                {
                    dfg = true;
                }
                if (!StringUtils.isWhitespace(wrapperType))
                {
                    wrapperType = wrapperType.trim();
                }
                else
                {
                    wrapperType = null;
                }
                if (!StringUtils.isWhitespace(wrapperTypeBacked))
                {
                    wrapperTypeBacked = wrapperTypeBacked.trim();
                }
                else
                {
                    wrapperTypeBacked = null;
                }
                if (!StringUtils.isWhitespace(containerHandlerType))
                {
                    containerHandlerType = containerHandlerType.trim();
                }
                else
                {
                    containerHandlerType = null;
                }

                try
                {
                    Class cls = clr.classForName(javaName);
                    Class genericType = null;
                    String javaTypeName = cls.getName();
                    if (!StringUtils.isWhitespace(genericTypeName))
                    {
                        genericType = clr.classForName(genericTypeName);
                        javaTypeName += "<" + genericTypeName + ">";
                    }

                    if (!javaTypes.containsKey(javaTypeName))
                    {
                        // Only add first entry for a java type (ordered by the "priority" flag)

                        Class wrapperClass = loadClass(mgr, elems, i, wrapperType, "016005");
                        Class wrapperClassBacked = loadClass(mgr, elems, i, wrapperTypeBacked, "016005");
                        Class containerHandlerClass = loadClass(mgr, elems, i, containerHandlerType, "016009");

                        String typeName = cls.getName();
                        if (genericType != null)
                        {
                            // "Collection<String>"
                            typeName += "<" + genericType.getName() + ">";
                        }
                        javaTypes.put(typeName, new JavaType(cls, genericType, embedded, dfg, wrapperClass, wrapperClassBacked, containerHandlerClass, typeConverterName));
                    }
                }
                catch (ClassNotResolvedException cnre)
                {
                    NucleusLogger.PERSISTENCE.debug("Not enabling java type support for " + javaName + " : java type not present in CLASSPATH");
                }
                catch (Exception e)
                {
                    NucleusLogger.PERSISTENCE.debug("Not enabling java type support for " + javaName + " : " + e.getMessage());
                }
            }
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            List<String> typesList = new ArrayList<String>(javaTypes.keySet());
            Collections.sort(typesList, ALPHABETICAL_ORDER_STRING);
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("016006", StringUtils.collectionToString(typesList)));
        }
    }
    
    private Class loadClass(PluginManager mgr, ConfigurationElement[] elems, int i, String className, String messageKey)
    {
        Class result = null;
        if (className != null)
        {
            try
            {
                result = mgr.loadClass(elems[i].getExtension().getPlugin().getSymbolicName(), className);
            }
            catch (NucleusException jpe)
            {
                // Impossible to load the class implementation from this plugin
                NucleusLogger.PERSISTENCE.error(Localiser.msg(messageKey, className));
                throw new NucleusException(Localiser.msg(messageKey, className));
            }
        }
        return result;
    }

    /**
     * Method to load the java type that are currently registered in the PluginManager.
     * @param mgr the PluginManager
     * @param clr the ClassLoaderResolver
     */
    private void loadTypeConverters(PluginManager mgr)
    {
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("016007"));
        }

        ClassLoaderResolver clr = getClassLoaderResolver();
        ConfigurationElement[] elems = mgr.getConfigurationElementsForExtension("org.datanucleus.type_converter", null, null);
        if (elems != null)
        {
            for (int i=0; i<elems.length; i++)
            {
                String name = elems[i].getAttribute("name").trim();
                String memberTypeName = elems[i].getAttribute("member-type").trim();
                String datastoreTypeName = elems[i].getAttribute("datastore-type").trim();
                String converterClsName = elems[i].getAttribute("converter-class").trim();
                Class memberType = null;
                try
                {
                    // Use plugin manager to instantiate the converter in case its in separate plugin
                    TypeConverter conv = (TypeConverter) mgr.createExecutableExtension("org.datanucleus.type_converter", 
                        "name", name, "converter-class", null, null);
                    registerConverter(name, conv);
                    if (typeConverterMap == null)
                    {
                        typeConverterMap = new ConcurrentHashMap<Class, Map<Class,TypeConverter>>();
                    }
                    memberType = clr.classForName(memberTypeName);
                    Class datastoreType = clr.classForName(datastoreTypeName);
                    Map<Class, TypeConverter> convertersForMember = typeConverterMap.get(memberType);
                    if (convertersForMember == null)
                    {
                        convertersForMember = new ConcurrentHashMap<Class, TypeConverter>();
                        typeConverterMap.put(memberType, convertersForMember);
                    }
                    convertersForMember.put(datastoreType, conv);
                }
                catch (Exception e)
                {
                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        if (memberType != null)
                        {
                            NucleusLogger.PERSISTENCE.debug("TypeConverter for " + memberTypeName + "<->" +
                                datastoreTypeName + " using " + converterClsName + " not instantiable (missing dependencies?) so ignoring");
                        }
                        else
                        {
                            NucleusLogger.PERSISTENCE.debug("TypeConverter for " + memberTypeName + "<->" +
                                datastoreTypeName + " ignored since java type not present in CLASSPATH");
                        }
                    }
                }
            }
        }
        if (NucleusLogger.PERSISTENCE.isDebugEnabled())
        {
            NucleusLogger.PERSISTENCE.debug(Localiser.msg("016008"));
            if (typeConverterMap != null)
            {
                List<Class> typesList = new ArrayList<Class>(typeConverterMap.keySet());
                Collections.sort(typesList, ALPHABETICAL_ORDER);
                for (Class javaType : typesList)
                {
                    Set<Class> datastoreTypes = typeConverterMap.get(javaType).keySet();
                    StringBuilder str = new StringBuilder();
                    for (Class datastoreCls : datastoreTypes)
                    {
                        if (str.length() > 0)
                        {
                            str.append(',');
                        }
                        str.append(StringUtils.getNameOfClass(datastoreCls));
                    }
                    NucleusLogger.PERSISTENCE.debug("TypeConverter(s) available for " + StringUtils.getNameOfClass(javaType) + " to : " + str.toString());
                }
            }
        }
    }

    private static Comparator<Class> ALPHABETICAL_ORDER = new Comparator<Class>() 
    {
        public int compare(Class cls1, Class cls2) 
        {
            int res = String.CASE_INSENSITIVE_ORDER.compare(cls1.getName(), cls2.getName());
            if (res == 0) 
            {
                res = cls1.getName().compareTo(cls2.getName());
            }
            return res;
        }
    };

    private static Comparator<String> ALPHABETICAL_ORDER_STRING = new Comparator<String>() 
    {
        public int compare(String cls1, String cls2) 
        {
            int res = String.CASE_INSENSITIVE_ORDER.compare(cls1, cls2);
            if (res == 0) 
            {
                res = cls1.compareTo(cls2);
            }
            return res;
        }
    };
}