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
package org.datanucleus.enhancer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.EventListener;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.state.StateManager;

/**
 * Helper class for the DN bytecode enhancement contract. It contains methods to register metadata for
 * persistable classes and to perform common operations needed by implementations, not by end users.
 * <P>
 * It allows construction of instances of persistable classes without using reflection.
 * <P>
 * Persistable classes register themselves via a static method at class load time. There is no security restriction on this access. 
 */
public class EnhancementHelper extends java.lang.Object
{
    private static EnhancementHelper singletonHelper = new EnhancementHelper();

    /**
     * This synchronized <code>HashMap</code> contains a static mapping of <code>Persistable</code> class to
     * metadata for the class used for constructing new instances. New entries are added by the static method
     * in each <code>Persistable</code> class. Entries are never removed.
     */
    private static Map<Class, Meta> registeredClasses = Collections.synchronizedMap(new HashMap<Class, Meta>());

    /**
     * This Set contains all classes that have registered for setStateManager permissions via
     * authorizeStateManagerClass. Only the key is used in order to maintain a weak set of classes.
     */
    private static final Map<Class, Class> authorizedStateManagerClasses = new WeakHashMap<Class, Class>();

    /**
     * This list contains the registered listeners for <code>RegisterClassEvent</code>s.
     */
    private static final List<RegisterClassListener> listeners = new ArrayList<RegisterClassListener>();

    /** The DateFormat pattern. */
    private static String dateFormatPattern;

    /** The default DateFormat instance. */
    private static DateFormat dateFormat;

    static
    {
        singletonHelper.registerDateFormat(getDateTimeInstance());
    }

    private EnhancementHelper()
    {
    }

    public static EnhancementHelper getInstance()/* throws SecurityException*/
    {
        /*SecurityManager sec = System.getSecurityManager();
        if (sec != null)
        {
            // throws exception if caller is not authorized
            sec.checkPermission(JDOPermission.GET_METADATA);
        }*/
        return singletonHelper;
    }

    /**
     * Get the field names for a <code>Persistable</code> class. The order of fields is the natural ordering
     * of the <code>String</code> class (without considering localization).
     * @param pcClass the <code>Persistable</code> class.
     * @return the field names for the class.
     */
    public String[] getFieldNames(Class pcClass)
    {
        Meta meta = getMeta(pcClass);
        return meta.getFieldNames();
    }

    /**
     * Get the field types for a <code>Persistable</code> class. The order of fields is the same as for field
     * names.
     * @param pcClass the <code>Persistable</code> class.
     * @return the field types for the class.
     */
    public Class[] getFieldTypes(Class pcClass)
    {
        Meta meta = getMeta(pcClass);
        return meta.getFieldTypes();
    }

    /**
     * Get the field flags for a <code>Persistable</code> class. The order of fields is the same as for field
     * names.
     * @param pcClass the <code>Persistable</code> class.
     * @return the field types for the class.
     */
    public byte[] getFieldFlags(Class pcClass)
    {
        Meta meta = getMeta(pcClass);
        return meta.getFieldFlags();
    }

    /**
     * Get the persistence-capable superclass for a <code>Persistable</code> class.
     * @param pcClass the <code>Persistable</code> class.
     * @return The <code>Persistable</code> superclass for this class, or <code>null</code> if there isn't
     * one.
     */
    public Class getPersistableSuperclass(Class pcClass)
    {
        Meta meta = getMeta(pcClass);
        return meta.getPersistableSuperclass();
    }

    /**
     * Create a new instance of the class and assign its StateManager. The new instance has its <code>dnFlags</code> set to <code>LOAD_REQUIRED</code>.
     * @see Persistable#dnNewInstance(StateManager sm)
     * @param pcClass the <code>Persistable</code> class.
     * @param sm the <code>StateManager</code> which will own the new instance.
     * @return the new instance, or <code>null</code> if the class is not registered.
     */
    public Persistable newInstance(Class pcClass, StateManager sm)
    {
        Meta meta = getMeta(pcClass);
        Persistable pcInstance = meta.getPC();
        return pcInstance == null ? null : pcInstance.dnNewInstance(sm);
    }

    /**
     * Create a new instance of the class and assign its StateManager and key values from the
     * ObjectId. If the oid parameter is <code>null</code>, no key values are copied. The new instance has its
     * <code>dnFlags</code> set to <code>LOAD_REQUIRED</code>.
     * @see Persistable#dnNewInstance(StateManager sm, Object oid)
     * @param pcClass the <code>Persistable</code> class.
     * @param sm the <code>StateManager</code> which will own the new instance.
     * @return the new instance, or <code>null</code> if the class is not registered.
     * @param oid the ObjectId instance from which to copy key field values.
     */
    public Persistable newInstance(Class pcClass, StateManager sm, Object oid)
    {
        Meta meta = getMeta(pcClass);
        Persistable pcInstance = meta.getPC();
        return pcInstance == null ? null : pcInstance.dnNewInstance(sm, oid);
    }

    /**
     * Create a new instance of the ObjectId class of this <code>Persistable</code> class. It is intended only
     * for application identity. This method should not be called for classes that use single field identity;
     * newObjectIdInstance(Class, Object) should be used instead. If the class has been enhanced for datastore
     * identity, or if the class is abstract, null is returned.
     * @param pcClass the <code>Persistable</code> class.
     * @return the new ObjectId instance, or <code>null</code> if the class is not registered.
     */
    public Object newObjectIdInstance(Class pcClass)
    {
        Meta meta = getMeta(pcClass);
        Persistable pcInstance = meta.getPC();
        return pcInstance == null ? null : pcInstance.dnNewObjectIdInstance();
    }

    /**
     * Create a new instance of the class used by the parameter Class for JDO identity, using the key
     * constructor of the object id class. It is intended for single field identity. The identity instance
     * returned has no relationship with the values of the primary key fields of the persistence-capable
     * instance on which the method is called. If the key is the wrong class for the object id class, null is
     * returned.
     * <P>
     * For classes that use single field identity, if the parameter is of one of the following types, the
     * behavior must be as specified:
     * <ul>
     * <li><code>Number</code> or <code>Character</code>: the parameter must be the single field type or the
     * wrapper class of the primitive field type; the parameter is passed to the single field identity
     * constructor</li>
     * <li><code>ObjectIdFieldSupplier</code>: the field value is fetched from the
     * <code>ObjectIdFieldSupplier</code> and passed to the single field identity constructor</li>
     * <li><code>String</code>: the String is passed to the single field identity constructor</li>
     * </ul>
     * @return the new ObjectId instance, or <code>null</code> if the class is not registered.
     * @param obj the <code>Object</code> form of the object id
     * @param pcClass the <code>Persistable</code> class.
     */
    public Object newObjectIdInstance(Class pcClass, Object obj)
    {
        Meta meta = getMeta(pcClass);
        Persistable pcInstance = meta.getPC();
        return (pcInstance == null) ? null : pcInstance.dnNewObjectIdInstance(obj);
    }

    /**
     * Copy fields from an outside source to the key fields in the ObjectId. This method is generated in the
     * <code>Persistable</code> class to generate a call to the field manager for each key field in the
     * ObjectId.
     * <P>
     * For example, an ObjectId class that has three key fields (<code>int id</code>, <code>String name</code>
     * , and <code>Float salary</code>) would have the method generated:
     * <code>
     * void dnCopyKeyFieldsToObjectId (Object oid, ObjectIdFieldSupplier fm) 
     * {
     *     oid.id = fm.fetchIntField (0);
     *     oid.name = fm.fetchStringField (1);
     *     oid.salary = fm.fetchObjectField (2);
     * }</code>
     * <P>
     * The implementation is responsible for implementing the <code>ObjectIdFieldSupplier</code> to provide
     * the values for the key fields.
     * @param pcClass the <code>Persistable Class</code>.
     * @param oid the ObjectId target of the copy.
     * @param fm the field manager that supplies the field values.
     */
    public void copyKeyFieldsToObjectId(Class pcClass, Persistable.ObjectIdFieldSupplier fm, Object oid)
    {
        Meta meta = getMeta(pcClass);
        Persistable pcInstance = meta.getPC();
        if (pcInstance == null)
        {
            throw new NucleusException("Class " + pcClass.getName() + " has no identity!").setFatal();
        }
        pcInstance.dnCopyKeyFieldsToObjectId(fm, oid);
    }

    /**
     * Copy fields to an outside source from the key fields in the ObjectId. This method is generated in the
     * <code>Persistable</code> class to generate a call to the field manager for each key field in the
     * ObjectId. For example, an ObjectId class that has three key fields (<code>int id</code>,
     * <code>String name</code>, and <code>Float salary</code>) would have the method generated:
     * <code>void dnCopyKeyFieldsFromObjectId(Persistable oid, ObjectIdFieldConsumer fm)
     * {
     *     fm.storeIntField (0, oid.id);
     *     fm.storeStringField (1, oid.name);
     *     fm.storeObjectField (2, oid.salary);
     * }</code>
     * <P>
     * The implementation is responsible for implementing the <code>ObjectIdFieldConsumer</code> to store the
     * values for the key fields.
     * @param pcClass the <code>Persistable</code> class
     * @param oid the ObjectId source of the copy.
     * @param fm the field manager that receives the field values.
     */
    public void copyKeyFieldsFromObjectId(Class pcClass, Persistable.ObjectIdFieldConsumer fm, Object oid)
    {
        Meta meta = getMeta(pcClass);
        Persistable pcInstance = meta.getPC();
        if (pcInstance == null)
        {
            throw new NucleusException("Class " + pcClass.getName() + " has no identity!").setFatal();
        }
        pcInstance.dnCopyKeyFieldsFromObjectId(fm, oid);
    }

    public static interface RegisterClassListener extends EventListener
    {
        /**
         * This method gets called when a persistence-capable class is registered.
         * @param event a <code>RegisterClassEvent</code> instance describing the registered 
         * class plus metadata.
         */
        public void registerClass(RegisterClassEvent event);
    }

    public static class RegisterClassEvent extends EventObject
    {
        private static final long serialVersionUID = -8336171250765467347L;
        /** The class object of the registered persistence-capable class */
        protected Class pcClass;
        /** The names of managed fields of the persistence-capable class */
        protected String[] fieldNames;  
        /** The types of managed fields of the persistence-capable class */
        protected Class[] fieldTypes;
        /** The flags of managed fields of the persistence-capable class */
        protected byte[] fieldFlags;

        protected Class persistableSuperclass; 

        public RegisterClassEvent(EnhancementHelper helper, Class registeredClass, String[] fieldNames, Class[] fieldTypes, byte[] fieldFlags, Class persistableSuperclass)
        {
            super(helper);
            this.pcClass = registeredClass;
            this.fieldNames = fieldNames;
            this.fieldTypes = fieldTypes;
            this.fieldFlags = fieldFlags;
            this.persistableSuperclass = persistableSuperclass;
        }

        public Class getRegisteredClass()
        {
            return pcClass;
        }
        public String[] getFieldNames()
        {
            return fieldNames;
        }
        public Class[] getFieldTypes()
        {
            return fieldTypes;
        }
        public byte[] getFieldFlags()
        {
            return fieldFlags;
        }
        public Class getPersistableSuperclass()
        {
            return persistableSuperclass;
        }
    }

    /**
     * Register metadata by class. The registration will be done in the class named EnhancementHelper
     * loaded by the same or an ancestor class loader as the <code>Persistable</code> class performing the
     * registration.
     * @param pcClass the <code>Persistable</code> class used as the key for lookup.
     * @param fieldNames an array of <code>String</code> field names for persistent and transactional fields
     * @param fieldTypes an array of <code>Class</code> field types
     * @param fieldFlags the Field Flags for persistent and transactional fields
     * @param pc an instance of the <code>Persistable</code> class
     * @param persistableSuperclass the most immediate superclass that is <code>Persistable</code>
     */
    public static void registerClass(Class pcClass, String[] fieldNames, Class[] fieldTypes, byte[] fieldFlags, Class persistableSuperclass, Persistable pc)
    {
        if (pcClass == null)
        {
            throw new NullPointerException("Attempt to register class with null class type");
        }
        Meta meta = new Meta(fieldNames, fieldTypes, fieldFlags, persistableSuperclass, pc);
        registeredClasses.put(pcClass, meta);

        synchronized (listeners)
        {
            if (!listeners.isEmpty())
            {
                RegisterClassEvent event = new RegisterClassEvent(singletonHelper, pcClass, fieldNames, fieldTypes, fieldFlags, persistableSuperclass);
                for (Iterator i = listeners.iterator(); i.hasNext();)
                {
                    RegisterClassListener crl = (RegisterClassListener) i.next();
                    if (crl != null)
                    {
                        crl.registerClass(event);
                    }
                }
            }
        }
    }

    /**
     * Unregister metadata by class loader. This method unregisters all registered <code>Persistable</code>
     * classes loaded by the specified class loader. Any attempt to get metadata for unregistered classes will
     * result in a <code>JDOFatalUserException</code>.
     * @param cl the class loader.
     */
    public void unregisterClasses(ClassLoader cl)
    {
        /*SecurityManager sec = System.getSecurityManager();
        if (sec != null)
        {
            // throws exception if caller is not authorized
            sec.checkPermission(JDOPermission.MANAGE_METADATA);
        }*/
        synchronized (registeredClasses)
        {
            for (Iterator i = registeredClasses.keySet().iterator(); i.hasNext();)
            {
                Class pcClass = (Class) i.next();
                // Note, the pc class was registered by calling the static method EnhancementHelper.registerClass. 
                // This means the EnhancementHelper class loader is the same as or an ancestor of the class loader of the pc class. 
                if ((pcClass != null) && (pcClass.getClassLoader() == cl))
                {
                    // unregister pc class, if its class loader is the specified one.
                    i.remove();
                }
            }
        }
    }

    /**
     * Unregister metadata by class. This method unregisters the specified class. Any further attempt to get
     * metadata for the specified class will result in a <code>JDOFatalUserException</code>.
     * @param pcClass the <code>Persistable</code> class to be unregistered.
     */
    public void unregisterClass(Class pcClass)
    {
        if (pcClass == null)
        {
            throw new NullPointerException("Cannot unregisterClass on null");
        }
        /*SecurityManager sec = System.getSecurityManager();
        if (sec != null)
        {
            // throws exception if caller is not authorized
            sec.checkPermission(JDOPermission.MANAGE_METADATA);
        }*/
        registeredClasses.remove(pcClass);
    }

    /**
     * Add the specified <code>RegisterClassListener</code> to the listener list.
     * @param crl the listener to be added
     */
    public void addRegisterClassListener(RegisterClassListener crl)
    {
        Set alreadyRegisteredClasses = null;
        synchronized (listeners)
        {
            listeners.add(crl);
            // Make a copy of the existing set of registered classes.
            // Between these two lines of code, any number of new class
            // registrations might occur, and will then all wait until this
            // synchronized block completes. Some of the class registrations
            // might be delivered twice to the newly registered listener.
            alreadyRegisteredClasses = new HashSet<Class>(registeredClasses.keySet());
        }
        // new registrations will call the new listener while the following
        // occurs notify the new listener about already-registered classes
        for (Iterator it = alreadyRegisteredClasses.iterator(); it.hasNext();)
        {
            Class pcClass = (Class) it.next();
            Meta meta = getMeta(pcClass);
            RegisterClassEvent event = new RegisterClassEvent(this, pcClass, meta.getFieldNames(), meta.getFieldTypes(), meta.getFieldFlags(),
                    meta.getPersistableSuperclass());
            crl.registerClass(event);
        }
    }

    /**
     * Remove the specified <code>RegisterClassListener</code> from the listener list.
     * @param crl the listener to be removed
     */
    public void removeRegisterClassListener(RegisterClassListener crl)
    {
        synchronized (listeners)
        {
            listeners.remove(crl);
        }
    }

    /**
     * Returns a collection of class objects of the registered persistable classes.
     * @return registered persistable classes
     */
    public Collection<Class> getRegisteredClasses()
    {
        return Collections.unmodifiableCollection(registeredClasses.keySet());
    }

    /**
     * Look up the metadata for a <code>Persistable</code> class.
     * @param pcClass the <code>Class</code>.
     * @return the <code>Meta</code> for the <code>Class</code>.
     */
    private static Meta getMeta(Class pcClass)
    {
        Meta ret = registeredClasses.get(pcClass);
        if (ret == null)
        {
            throw new NucleusUserException("Cannot lookup meta info for " + pcClass + " - nothing found").setFatal();
        }
        return ret;
    }

    /**
     * Register a class authorized to replaceStateManager. During replaceStateManager, a persistable class will call the
     * corresponding checkAuthorizedStateManager and the class of the instance of the parameter must have been registered.
     * @param smClass a Class that is authorized for JDOPermission("setStateManager").
     */
    public static void registerAuthorizedStateManagerClass(Class smClass) /*throws SecurityException*/
    {
        if (smClass == null)
        {
            throw new NullPointerException("Cannot register StateManager class with null input!");
        }
        /*SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(JDOPermission.SET_STATE_MANAGER);
        }*/
        synchronized (authorizedStateManagerClasses)
        {
            authorizedStateManagerClasses.put(smClass, null);
        }
    }

    /**
     * Register classes authorized to replaceStateManager. During replaceStateManager, a persistable class will call the
     * corresponding checkAuthorizedStateManager and the class of the instance of the parameter must have been registered.
     * @param smClasses a Collection of Classes
     */
    public static void registerAuthorizedStateManagerClasses(Collection smClasses) /*throws SecurityException*/
    {
        /*SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(JDOPermission.SET_STATE_MANAGER);*/
            synchronized (authorizedStateManagerClasses)
            {
                for (Iterator it = smClasses.iterator(); it.hasNext();)
                {
                    Object smClass = it.next();
                    if (!(smClass instanceof Class))
                    {
                        throw new ClassCastException("Cannot register StateManager class passing in object of type " + smClass.getClass().getName());
                    }
                    registerAuthorizedStateManagerClass((Class) it.next());
                }
            }
        /*}*/
    }

    /**
     * Check that the parameter instance is of a class that is authorized for
     * JDOPermission("setStateManager"). This method is called by the replaceStateManager method in
     * persistable classes. A class that is passed as the parameter to replaceStateManager must be
     * authorized for JDOPermission("setStateManager"). To improve performance, first the set of authorized
     * classes is checked, and if not present, a regular permission check is made. The regular permission
     * check requires that all callers on the stack, including the persistence-capable class itself, must be
     * authorized for JDOPermission("setStateManager").
     * @param sm an instance of StateManager whose class is to be checked.
     */
    public static void checkAuthorizedStateManager(StateManager sm)
    {
        checkAuthorizedStateManagerClass(sm.getClass());
    }

    /**
     * Check that the parameter instance is a class that is authorized for JDOPermission("setStateManager").
     * This method is called by the constructors of JDO Reference Implementation classes.
     * @param smClass a Class to be checked for JDOPermission("setStateManager")
     */
    public static void checkAuthorizedStateManagerClass(Class smClass)
    {
        /*final SecurityManager scm = System.getSecurityManager();
        if (scm == null)
        {
            // if no security manager, no checking.
            return;
        }*/
        synchronized (authorizedStateManagerClasses)
        {
            if (authorizedStateManagerClasses.containsKey(smClass))
            {
                return;
            }
        }
        // if not already authorized, perform "long" security checking.
        /*scm.checkPermission(JDOPermission.SET_STATE_MANAGER);*/
    }

    /**
     * Construct an instance of a key class using a String as input. This is a helper interface for use with
     * ObjectIdentity. Classes without a String constructor (such as those in java.lang and java.util) will
     * use this interface for constructing new instances. The result might be a singleton or use some other
     * strategy.
     */
    public interface StringConstructor
    {
        /**
         * Construct an instance of the class for which this instance is registered.
         * @param s the parameter for construction
         * @return the constructed object
         */
        public Object construct(String s);
    }

    /**
     * Special StringConstructor instances for use with specific classes that have no public String
     * constructor. The Map is keyed on class instance and the value is an instance of StringConstructor.
     */
    static final Map<Class, StringConstructor> stringConstructorMap = new HashMap<Class, StringConstructor>();

    /**
     * Register special StringConstructor instances. These instances are for constructing instances from
     * String parameters where there is no String constructor for them.
     * @param cls the class to register a StringConstructor for
     * @param sc the StringConstructor instance
     * @return the previous StringConstructor registered for this class
     */
    public Object registerStringConstructor(Class cls, StringConstructor sc)
    {
        synchronized (stringConstructorMap)
        {
            return stringConstructorMap.put(cls, sc);
        }
    }

    /**
     * Register the default special StringConstructor instances.
     */
    static
    {
        // TODO Add other possible types
        singletonHelper.registerStringConstructor(Currency.class, new StringConstructor()
        {
            public Object construct(String s)
            {
                try
                {
                    return Currency.getInstance(s);
                }
                catch (Exception ex)
                {
                    throw new NucleusUserException("Exception in Currency identity String constructor", ex);
                }
            }
        });
        singletonHelper.registerStringConstructor(Locale.class, new StringConstructor()
        {
            public Object construct(String s)
            {
                try
                {
                    String lang = s;
                    int firstUnderbar = s.indexOf('_');
                    if (firstUnderbar == -1)
                    {
                        // nothing but language
                        return new Locale(lang);
                    }
                    lang = s.substring(0, firstUnderbar);
                    String country;
                    int secondUnderbar = s.indexOf('_', firstUnderbar + 1);
                    if (secondUnderbar == -1)
                    {
                        // nothing but language, country
                        country = s.substring(firstUnderbar + 1);
                        return new Locale(lang, country);
                    }
                    country = s.substring(firstUnderbar + 1, secondUnderbar);
                    String variant = s.substring(secondUnderbar + 1);
                    return new Locale(lang, country, variant);
                }
                catch (Exception ex)
                {
                    throw new NucleusUserException("Exception in Locale identity String constructor", ex);
                }
            }
        });
        singletonHelper.registerStringConstructor(Date.class, new StringConstructor()
        {
            public synchronized Object construct(String s)
            {
                try
                {
                    // first, try the String as a Long
                    return new Date(Long.parseLong(s));
                }
                catch (NumberFormatException ex)
                {
                    // not a Long; try the formatted date
                    ParsePosition pp = new ParsePosition(0);
                    Date result = dateFormat.parse(s, pp);
                    if (result == null)
                    {
                        throw new NucleusUserException("Exception in Date identity String constructor", 
                            new Object[] {s, Integer.valueOf(pp.getErrorIndex()), dateFormatPattern});
                    }
                    return result;
                }
            }
        });
    }

    /**
     * Construct an instance of the parameter class, using the keyString as an argument to the constructor. If
     * the class has a StringConstructor instance registered, use it. If not, try to find a constructor for
     * the class with a single String argument. Otherwise, throw a JDOUserException.
     * @param className the name of the class
     * @param keyString the String parameter for the constructor
     * @return the result of construction
     */
    public static Object construct(String className, String keyString)
    {
        StringConstructor stringConstructor;
        try
        {
            Class<?> keyClass = Class.forName(className);
            synchronized (stringConstructorMap)
            {
                stringConstructor = stringConstructorMap.get(keyClass);
            }
            if (stringConstructor != null)
            {
                return stringConstructor.construct(keyString);
            }
            return keyClass.getConstructor(new Class[]{String.class}).newInstance(new Object[]{keyString});
        }
        catch (Exception ex)
        {
            // ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
            throw new NucleusUserException("Exception in Object identity String constructor", ex);
        }
    }

    /**
     * Get the DateFormat instance for the default locale from the VM. This requires the following privileges
     * for EnhancementHelper in the security permissions file: permission java.util.PropertyPermission
     * "user.country", "read"; permission java.util.PropertyPermission "user.timezone", "read,write";
     * permission java.util.PropertyPermission "java.home", "read"; If these permissions are not present, or
     * there is some other problem getting the default date format, a simple formatter is returned.
     * @return the default date-time formatter
     */
    static DateFormat getDateTimeInstance()
    {
        DateFormat result = null;
        try
        {
            result = AccessController.doPrivileged(new PrivilegedAction<DateFormat>()
            {
                public DateFormat run()
                {
                    return DateFormat.getDateTimeInstance();
                }
            });
        }
        catch (Exception ex)
        {
            result = DateFormat.getInstance();
        }
        return result;
    }

    /**
     * Register a DateFormat instance for use with constructing Date instances. The default is the default
     * DateFormat instance. If the new instance implements SimpleDateFormat, get its pattern for error messages.
     * @param df the DateFormat instance to use
     */
    public synchronized void registerDateFormat(DateFormat df)
    {
        dateFormat = df;
        if (df instanceof SimpleDateFormat)
        {
            dateFormatPattern = ((SimpleDateFormat) df).toPattern();
        }
        else
        {
            dateFormatPattern = "Unknown message";
        }
    }

    /**
     * This is a helper class to manage metadata per persistable class. 
     * The information is used at runtime to provide field names and field types to the JDO Model. 
     * This is the value of the <code>HashMap</code> which relates the <code>Persistable Class</code> as a key to the metadata.
     */
    static class Meta
    {
        /** Instance of <code>Persistable</code>, used at runtime to create new instances. */
        Persistable pc;

        /** This is an array of field names used for the Model at runtime. The field is passed by the static class initialization. */
        String[] fieldNames;

        /** This is an array of field types used for the Model at runtime. The field is passed by the static class initialization. */
        Class[] fieldTypes;

        /** This is an array of field flags used for the Model at runtime. The field is passed by the static class initialization. */
        byte[] fieldFlags;

        /** This is the <code>Class</code> instance of the <code>Persistable</code> superclass. */
        Class persistableSuperclass;

        /**
         * Construct an instance of <code>Meta</code>.
         * @param fieldNames An array of <code>String</code>
         * @param fieldTypes An array of <code>Class</code>
         * @param fieldFlags an array of <code>int</code>
         * @param persistableSuperclass the most immediate <code>Persistable</code> superclass
         * @param pc An instance of the <code>Persistable</code> class
         */
        Meta(String[] fieldNames, Class[] fieldTypes, byte[] fieldFlags, Class persistableSuperclass, Persistable pc)
        {
            this.fieldNames = fieldNames;
            this.fieldTypes = fieldTypes;
            this.fieldFlags = fieldFlags;
            this.persistableSuperclass = persistableSuperclass;
            this.pc = pc;
        }

        String[] getFieldNames()
        {
            return fieldNames;
        }
        Class[] getFieldTypes()
        {
            return fieldTypes;
        }
        byte[] getFieldFlags()
        {
            return fieldFlags;
        }
        Class getPersistableSuperclass()
        {
            return persistableSuperclass;
        }
        Persistable getPC()
        {
            return pc;
        }

        public String toString()
        {
            return "Meta-" + pc.getClass().getName(); // NOI18N
        }
    }
}
