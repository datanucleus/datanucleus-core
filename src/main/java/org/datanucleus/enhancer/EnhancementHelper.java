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
import java.util.concurrent.ConcurrentHashMap;

import org.datanucleus.enhancement.Persistable;
import org.datanucleus.enhancement.StateManager;
import org.datanucleus.exceptions.NucleusUserException;

/**
 * Helper class for the DN bytecode enhancement contract. 
 * It contains methods to register metadata for persistable classes and to perform common operations needed by implementations, not by end users.
 * <P>
 * It allows construction of instances of persistable classes without using reflection.
 * <P>
 * Persistable classes register themselves via a static method at class load time. There is no security restriction on this access. 
 */
public class EnhancementHelper extends java.lang.Object
{
    private static EnhancementHelper singletonHelper = new EnhancementHelper();

    /**
     * This synchronized <code>Map</code> contains a static mapping of <code>Persistable</code> class to
     * metadata for the class used for constructing new instances. New entries are added by the static method
     * in each <code>Persistable</code> class. Entries are never removed.
     */
    private static Map<Class, Meta> registeredClasses = new ConcurrentHashMap<>();

    /**
     * This Set contains all classes that have registered for setStateManager permissions via
     * authorizeStateManagerClass. Only the key is used in order to maintain a weak set of classes.
     */
    private static final Map<Class, Class> authorizedStateManagerClasses = new WeakHashMap<>();

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
     * Create a new instance of the class and assign its StateManager.
     * @see Persistable#dnNewInstance(StateManager sm)
     * @param pcClass the <code>Persistable</code> class.
     * @param sm the <code>StateManager</code> which will own the new instance.
     * @return the new instance, or <code>null</code> if the class is not registered.
     */
    public Persistable newInstance(Class pcClass, StateManager sm)
    {
        Persistable pc = getPersistableForClass(pcClass);
        return pc == null ? null : pc.dnNewInstance(sm);
    }

    /**
     * Create a new instance of the class and assign its StateManager and key values from the ObjectId. If the oid parameter is <code>null</code>, no key values are copied. 
     * The new instance has its <code>dnFlags</code> set to <code>LOAD_REQUIRED</code>.
     * @see Persistable#dnNewInstance(StateManager sm, Object oid)
     * @param pcClass the <code>Persistable</code> class.
     * @param sm the <code>StateManager</code> which will own the new instance.
     * @return the new instance, or <code>null</code> if the class is not registered.
     * @param oid the ObjectId instance from which to copy key field values.
     */
    public Persistable newInstance(Class pcClass, StateManager sm, Object oid)
    {
        Persistable pc = getPersistableForClass(pcClass);
        return pc == null ? null : pc.dnNewInstance(sm, oid);
    }

    /**
     * Create a new instance of the ObjectId class of this <code>Persistable</code> class. 
     * It is intended only for application identity. 
     * This method should not be called for classes that use single field identity; newObjectIdInstance(Class, Object) should be used instead.
     * If the class has been enhanced for datastore identity, or if the class is abstract, null is returned.
     * @param pcClass the <code>Persistable</code> class.
     * @return the new ObjectId instance, or <code>null</code> if the class is not registered.
     */
    public Object newObjectIdInstance(Class pcClass)
    {
        Persistable pc = getPersistableForClass(pcClass);
        return pc == null ? null : pc.dnNewObjectIdInstance();
    }

    /**
     * Create a new instance of the class used by the parameter Class for JDO identity, using the key constructor of the object id class. 
     * It is intended for single field identity. 
     * The identity instance returned has no relationship with the values of the primary key fields of the persistence-capable instance on which the method is called. 
     * If the key is the wrong class for the object id class, null is returned.
     * <P>
     * For classes that use single field identity, if the parameter is of one of the following types, the behavior must be as specified:
     * <ul>
     * <li><code>Number</code> or <code>Character</code>: the parameter must be the single field type or the
     * wrapper class of the primitive field type; the parameter is passed to the single field identity constructor</li>
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
        Persistable pc = getPersistableForClass(pcClass);
        return (pc == null) ? null : pc.dnNewObjectIdInstance(obj);
    }

    public static interface RegisterClassListener extends EventListener
    {
        /**
         * This method gets called when a Persistable class is registered.
         * @param event a <code>RegisterClassEvent</code> instance describing the registered class plus metadata.
         */
        public void registerClass(RegisterClassEvent event);
    }

    public static class RegisterClassEvent extends EventObject
    {
        private static final long serialVersionUID = -8336171250765467347L;

        /** The class object of the registered Persistable class */
        protected Class pcClass;

        public RegisterClassEvent(EnhancementHelper helper, Class registeredClass)
        {
            super(helper);
            this.pcClass = registeredClass;
        }

        public Class getRegisteredClass()
        {
            return pcClass;
        }
    }

    /**
     * Register metadata by class.
     * This is called by the enhanced constructor of the <code>Persistable</code> class.
     * TODO Remove the unused arguments when the enhancement contract is updated to not use them.
     * @param pcClass the <code>Persistable</code> class used as the key for lookup.
     * @param fieldNames Not used
     * @param fieldTypes Not used
     * @param fieldFlags Not used
     * @param pc an instance of the <code>Persistable</code> class
     * @param persistableSuperclass Not used
     */
    public static void registerClass(Class pcClass, String[] fieldNames, Class[] fieldTypes, byte[] fieldFlags, Class persistableSuperclass, Persistable pc)
    {
        if (pcClass == null)
        {
            throw new NullPointerException("Attempt to register class with null class type");
        }

        registeredClasses.put(pcClass, new Meta(pc));

        // Notify all listeners
        synchronized (listeners)
        {
            if (!listeners.isEmpty())
            {
                RegisterClassEvent event = new RegisterClassEvent(singletonHelper, pcClass);
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
        Set<Class> alreadyRegisteredClasses = null;
        synchronized (listeners)
        {
            listeners.add(crl);

            // Make a copy of the existing set of registered classes.
            // Between these two lines of code, any number of new class registrations might occur, and will then all wait until this synchronized block completes. 
            // Some of the class registrations might be delivered twice to the newly registered listener.
            alreadyRegisteredClasses = new HashSet<Class>(registeredClasses.keySet());
        }

        // new registrations will call the new listener while the following occurs notify the new listener about already-registered classes
        for (Class pcClass : alreadyRegisteredClasses)
        {
            RegisterClassEvent event = new RegisterClassEvent(this, pcClass);
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
     * Look up the instance for a Persistable class.
     * @param pcClass the <code>Class</code>.
     * @return the Persistable instance for the <code>Class</code>.
     */
    private static Persistable getPersistableForClass(Class pcClass)
    {
        Meta ret = registeredClasses.get(pcClass);
        if (ret == null)
        {
            throw new NucleusUserException("Cannot lookup meta info for " + pcClass + " - nothing found").setFatal();
        }
        return ret.getPC();
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
     * Check that the parameter instance is of a class that is authorized for JDOPermission("setStateManager"). 
     * This method is called by the <code>replaceStateManager</code> method in Persistable classes. 
     * A class that is passed as the parameter to replaceStateManager must be authorized for JDOPermission("setStateManager"). 
     * To improve performance, first the set of authorized classes is checked, and if not present, a regular permission check is made. 
     * The regular permission check requires that all callers on the stack, including the persistence-capable class itself, must be authorized for JDOPermission("setStateManager").
     * @param sm an instance of StateManager whose class is to be checked.
     */
    public static void checkAuthorizedStateManager(StateManager sm)
    {
        /*final SecurityManager scm = System.getSecurityManager();
        if (scm == null)
        {
            // if no security manager, no checking.
            return;
        }*/
        synchronized (authorizedStateManagerClasses)
        {
            if (authorizedStateManagerClasses.containsKey(sm.getClass()))
            {
                return;
            }
        }

        // if not already authorized, perform "long" security checking.
        /*scm.checkPermission(JDOPermission.SET_STATE_MANAGER);*/
    }

    /**
     * Construct an instance of a key class using a String as input. 
     * This is a helper interface for use with ObjectIdentity. 
     * Classes without a String constructor (such as those in java.lang and java.util) will use this interface for constructing new instances. 
     * The result might be a singleton or use some other strategy.
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
                        throw new NucleusUserException("Exception in Date identity String constructor", new Object[] {s, Integer.valueOf(pp.getErrorIndex()), dateFormatPattern});
                    }
                    return result;
                }
            }
        });
    }

    /**
     * Construct an instance of the parameter class, using the keyString as an argument to the constructor. 
     * If the class has a StringConstructor instance registered, use it. 
     * If not, try to find a constructor for the class with a single String argument. Otherwise, throw a JDOUserException.
     * TODO Consider moving this to ObjectId or IdentityManager
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
     * Helper class to manage persistable classes. 
     */
    static class Meta
    {
        /** Instance of <code>Persistable</code>, used at runtime to create new instances. */
        Persistable pc;

        /**
         * Construct an instance of <code>Meta</code>.
         * @param pc An instance of the <code>Persistable</code> class
         */
        Meta(Persistable pc)
        {
            this.pc = pc;
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