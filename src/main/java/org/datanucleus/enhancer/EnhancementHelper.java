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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
 * 
 * TODO Provide a mechanism to automatically deregister classes when their class loader exits? or register against the NucleusContext?
 */
public class EnhancementHelper extends java.lang.Object
{
    private static EnhancementHelper singletonHelper = new EnhancementHelper();

    /**
     * Static mapping of <code>Persistable</code> class to an instance of that class.
     * New entries are added by the static method in each <code>Persistable</code> class initialisation. Entries are never removed.
     */
    private static Map<Class, Meta> registeredClasses = new ConcurrentHashMap<>();

    /**
     * This list contains the registered listeners for <code>RegisterClassEvent</code>s.
     */
    private static final List<RegisterClassListener> listeners = new ArrayList<RegisterClassListener>();

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
         * @param registeredClass The class that has just been initialised/registered
         */
        public void registerClass(Class registeredClass);
    }

    /**
     * Register metadata by class.
     * This is called by the enhanced constructor of the <code>Persistable</code> class.
     * @param pcClass the <code>Persistable</code> class used as the key for lookup.
     * @param pc an instance of the <code>Persistable</code> class. TODO We can just do pcClass.newInstance() to get one!
     */
    public static void registerClass(Class pcClass, Persistable pc)
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
                for (Iterator i = listeners.iterator(); i.hasNext();)
                {
                    RegisterClassListener crl = (RegisterClassListener) i.next();
                    if (crl != null)
                    {
                        crl.registerClass(pcClass);
                    }
                }
            }
        }
    }

    /**
     * Unregister all classes for the specified class loader.
     * @param cl ClassLoader
     */
    public void unregisterClasses (ClassLoader cl)
    {
        synchronized(registeredClasses) 
        {
            for (Iterator i = registeredClasses.keySet().iterator(); i.hasNext();)
            {
                Class pcClass = (Class)i.next();
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
            crl.registerClass(pcClass);
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
            // Commented out code that we could enable, particularly if we update registerClass to not pass an instance
            /*if (Persistable.class.isAssignableFrom(pcClass))
            {
                // Not yet registered but this is Persistable, so create an instance
                Persistable pc;
                try
                {
                    pc = (Persistable) pcClass.newInstance();
                    registeredClasses.put(pcClass, new Meta(pc));
                    return pc;
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                }
            }*/
            throw new NucleusUserException("Cannot lookup meta info for " + pcClass + " - nothing found").setFatal();
        }
        return ret.getPC();
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
            return "Meta-" + pc.getClass().getName();
        }
    }

    // TODO core-264 Remove this field and 2 methods
    /**
     * This Set contains all classes that have registered for setStateManager permissions via authorizeStateManagerClass. 
     * Only the key is used in order to maintain a weak set of classes.
     */
    private static final Map<Class, Class> authorizedStateManagerClasses = new WeakHashMap<>();

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
}