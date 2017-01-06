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
package org.datanucleus.enhancement;

/**
 * A class that can be managed by DataNucleus must implement this interface.
 * <P>
 * This interface defines methods that allow the implementation to manage the instances. It also defines
 * methods that allow a DataNucleus aware application to examine the runtime state of instances. For example, an
 * application can discover whether the instance is persistent, transactional, dirty, new, deleted, or
 * detached; and to get its associated ExecutionContext, object identity, and version if it has one.
 * <P>
 * The Persistable interface is designed to avoid name conflicts in the scope of user-defined classes.
 * All of its declared method names are prefixed with 'dn'.
 */
public interface Persistable
{
    /** dnFlags == READ_WRITE_OK, then the fields in the default fetch group can be accessed for read or write without notifying the StateManager. */
    static final byte READ_WRITE_OK = 0;

    /** dnFlags == LOAD_REQUIRED, then the fields in the default fetch group cannot be accessed for read or write without notifying the StateManager. */
    static final byte LOAD_REQUIRED = 1;

    /** dnFlags == READ_OK, then the fields in the default fetch group can be accessed for read without notifying the StateManager */
    static final byte READ_OK = -1;

    /** dnFieldFlags for a field includes CHECK_READ, then the field has been enhanced to call the StateManager on read if the dnFlags setting is not READ_OK or READ_WRITE_OK */
    static final byte CHECK_READ = 1;

    /** dnFieldFlags for a field includes MEDIATE_READ, then the field has been enhanced to always call the StateManager on all reads */
    static final byte MEDIATE_READ = 2;

    /** dnFieldFlags for a field includes CHECK_WRITE, then the field has been enhanced to call the StateManager on write if the dnFlags setting is not READ_WRITE_OK */
    static final byte CHECK_WRITE = 4;

    /** dnFieldFlags for a field includes MEDIATE_WRITE, then the field has been enhanced to always call the StateManager on all writes */
    static final byte MEDIATE_WRITE = 8;

    /** dnFieldFlags for a field includes SERIALIZABLE, then the field is not declared as TRANSIENT. */
    static final byte SERIALIZABLE = 16;

    /**
     * Return the associated ExecutionContext if there is one. 
     * Transactional and persistent instances return the associated ExecutionContext.
     * Transient non-transactional instances return null.
     * This method always delegates to the StateManager if it is non-null.
     * @return the ExecutionContext associated with this instance.
     */
    ExecutionContextReference dnGetExecutionContext();

    /**
     * Return the associated StateManager if there is one.
     * @return The StateManager managing this object
     */
    StateManager dnGetStateManager();

    /**
     * This method sets the StateManager instance that manages the state of this instance. This method is
     * normally used by the StateManager during the process of making an instance persistent, transient, or
     * transactional. The caller of this method must have JDOPermission for the instance, if the instance is
     * not already owned by a StateManager. If the parameter is null, and the StateManager approves the
     * change, then the dnFlags field will be reset to READ_WRITE_OK. If the parameter is not null, and the
     * security manager approves the change, then the dnFlags field will be reset to LOAD_REQUIRED.
     * @param sm The StateManager which will own this instance, or null to reset the instance to transient state
     * @throws SecurityException if the caller does not have JDOPermission
     */
    void dnReplaceStateManager(StateManager sm) throws SecurityException;

    /**
     * The owning StateManager uses this method to ask the instance to provide the value of the single field identified by fieldNumber.
     * @param fieldNumber the field whose value is to be provided by a callback to the StateManager's providedXXXField method
     */
    void dnProvideField(int fieldNumber);

    /**
     * The owning StateManager uses this method to ask the instance to provide the values of the multiple fields identified by fieldNumbers.
     * @param fieldNumbers the fields whose values are to be provided by multiple callbacks to the StateManager's providedXXXField method
     */
    void dnProvideFields(int[] fieldNumbers);

    /**
     * The owning StateManager uses this method to ask the instance to replace the value of the single field identified by number.
     * @param fieldNumber the field whose value is to be replaced by a callback to the StateManager's replacingXXXField method
     */
    void dnReplaceField(int fieldNumber);

    /**
     * The owning StateManager uses this method to ask the instance to replace the values of the multiple fields identified by number.
     * @param fieldNumbers the fields whose values are to be replaced by multiple callbacks to the StateManager's replacingXXXField method
     */
    void dnReplaceFields(int[] fieldNumbers);

    /**
     * The owning StateManager uses this method to ask the instance to replace the value of the flags by
     * calling back the StateManager replacingFlags method.
     */
    void dnReplaceFlags();

    /**
     * Copy field values from another instance of the same class to this instance.
     * <P>
     * This method will throw an exception if the other instance is not managed by the same StateManager as
     * this instance.
     * @param other the PC instance from which field values are to be copied
     * @param fieldNumbers the field numbers to be copied into this instance
     */
    void dnCopyFields(Object other, int[] fieldNumbers);

    /**
     * Explicitly mark this instance and this field dirty. Normally, Persistable classes are able to
     * detect changes made to their fields. However, if a reference to an array is given to a method outside
     * the class, and the array is modified, then the persistent instance is not aware of the change. This API
     * allows the application to notify the instance that a change was made to a field.
     * <P>
     * The field name should be the fully qualified name, including package name and class name of the class
     * declaring the field. This allows unambiguous identification of the field to be marked dirty. If
     * multiple classes declare the same field, and if the package and class name are not provided by the
     * parameter in this API, then the field marked dirty is the field declared by the most derived class.
     * <P>
     * Transient instances ignore this method.
     * <P>
     * @param fieldName the name of the field to be marked dirty.
     */
    void dnMakeDirty(String fieldName);

    /**
     * Return a copy of the identity associated with this instance.
     * <P>
     * Persistent instances of Persistable classes have an identity managed by the ExecutionContext. 
     * This method returns a copy of the ObjectId that represents the identity.
     * <P>
     * Transient instances return null.
     * <P>
     * The ObjectId may be serialized and later restored, and used with a ExecutionContext from the same 
     * DataNucleus instance to locate a persistent instance with the same data store identity.
     * <P>
     * If the identity is managed by the application, then the ObjectId may be used with a
     * ExecutionContext from DataNucleus that supports the Persistable class.
     * <P>
     * If the identity is not managed by the application or the data store, then the ObjectId returned is
     * only valid within the current transaction.
     * <P>
     * If the identity is being changed in the transaction, this method returns the object id as of the
     * beginning of the current transaction.
     * @return a copy of the ObjectId of this instance as of the beginning of the transaction.
     */
    Object dnGetObjectId();

    /**
     * Return a copy of the identity associated with this instance. This method is the same as
     * dnGetObjectId if the identity of the instance has not changed in the current transaction.
     * <P>
     * If the identity is being changed in the transaction, this method returns the current object id as
     * modified in the current transaction.
     * @see #dnGetObjectId()
     * @return a copy of the ObjectId of this instance as modified in the transaction.
     */
    Object dnGetTransactionalObjectId();

    /**
     * Return the version of this instance.
     * @return the version
     */
    Object dnGetVersion();

    /**
     * Tests whether this object is dirty. Instances that have been modified, deleted, or newly made
     * persistent in the current transaction return true.
     * <P>
     * Transient instances return false.
     * @return true if this instance has been modified in the current transaction.
     */
    boolean dnIsDirty();

    /**
     * Tests whether this object is transactional. Instances whose state is associated with the current transaction return true.
     * Transient instances return false.
     * @return true if this instance is transactional.
     */
    boolean dnIsTransactional();

    /**
     * Tests whether this object is persistent. Instances that represent persistent objects in the data store return true.
     * @return true if this instance is persistent.
     */
    boolean dnIsPersistent();

    /**
     * Tests whether this object has been newly made persistent. Instances that have been made persistent in
     * the current transaction return true. Transient instances return false.
     * @return true if this instance was made persistent in the current transaction.
     */
    boolean dnIsNew();

    /**
     * Tests whether this object has been deleted. Instances that have been deleted in the current transaction return true.
     * Transient instances return false.
     * @return true if this instance was deleted in the current transaction.
     */
    boolean dnIsDeleted();

    /**
     * Tests whether this object has been detached. Instances that have been detached return true.
     * Transient instances return false.
     * @return true if this instance is detached.
     */
    boolean dnIsDetached();

    /**
     * Return a new instance of this class, with the StateManager set to the parameter, and dnFlags set to LOAD_REQUIRED.
     * <P>
     * This method is used as a performance optimization as an alternative to using reflection to construct a
     * new instance. It is used by the JDOImplHelper class method newInstance.
     * @param sm the StateManager that will own the new instance.
     * @return a new instance of this class.
     */
    Persistable dnNewInstance(StateManager sm);

    /**
     * Return a new instance of this class, with the StateManager set to the parameter, key fields
     * initialized to the values in the oid, and dnFlags set to LOAD_REQUIRED.
     * <P>
     * This method is used as a performance optimization as an alternative to using reflection to construct a
     * new instance of a class that uses application identity. It is used by the JDOImplHelper class method
     * newInstance.
     * @param sm the StateManager that will own the new instance.
     * @param oid an instance of the object id class (application identity).
     * @return a new instance of this class.
     */
    Persistable dnNewInstance(StateManager sm, Object oid);

    /**
     * Create a new instance of the ObjectId class for this Persistable class and initialize the key
     * fields from the instance on which this method is called. This method creates a new instance of the
     * class used for identity. It is intended only for application identity. If the class has been
     * enhanced for datastore identity, or if the class is abstract, null is returned.
     * <P>
     * For classes using single field identity, this method must be called on an instance of a
     * persistence-capable class with its primary key field initialized (not null), or an exception is thrown.
     * <P>
     * The instance returned is initialized with the value(s) of the primary key field(s) of the instance on
     * which the method is called.
     * @return the new instance created.
     */
    Object dnNewObjectIdInstance();

    /**
     * Create a new instance of the class used for identity, using the key constructor of the object id
     * class. It is intended for single field identity. The identity instance returned has no relationship
     * with the values of the primary key fields of the persistence-capable instance on which the method is
     * called. If the key is the wrong class for the object id class, null is returned.
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
     * @return the new instance created.
     * @param o the object identity constructor parameter
     */
    Object dnNewObjectIdInstance(Object o);

    /**
     * Copy fields from this Persistable instance to the Object Id instance. For classes using single
     * field identity, this method always throws <code>JDOUserException</code>.
     * @param oid the ObjectId target of the key fields
     */
    void dnCopyKeyFieldsToObjectId(Object oid);

    /**
     * Copy fields from an outside source to the key fields in the ObjectId. This method is generated in the
     * <code>Persistable</code> class to generate a call to the field manager for each key field in the
     * ObjectId. For example, an ObjectId class that has three key fields <code>(int id,
     * String name, and Float salary)</code> would have the method generated: <code>
     * void dnCopyKeyFieldsToObjectId(ObjectIdFieldSupplier fm, Object objectId) 
     * {
     *     EmployeeKey oid = (EmployeeKey)objectId;
     *     oid.id = fm.fetchIntField (0);
     *     oid.name = fm.fetchStringField (1);
     *     oid.salary = fm.fetchObjectField (2);
     * }
     * </code>
     * <P>
     * The implementation is responsible for implementing the <code>ObjectIdFieldSupplier</code> to produce
     * the values for the key fields.
     * <P>
     * For classes using single field identity, this method always throws <code>JDOUserException</code>.
     * @param oid the ObjectId target of the copy.
     * @param fm the field supplier that supplies the field values.
     */
    void dnCopyKeyFieldsToObjectId(ObjectIdFieldSupplier fm, Object oid);

    /**
     * Copy fields to an outside consumer from the key fields in the ObjectId. This method is generated in the
     * <code>Persistable</code> class to generate a call to the field manager for each key field in the
     * ObjectId. For example, an ObjectId class that has three key fields <code>(int id,
     * String name, and Float salary)</code> would have the method generated: <code>
     * void copyKeyFieldsFromObjectId(ObjectIdFieldConsumer fm, Object objectId) 
     * {
     *      EmployeeKey oid = (EmployeeKey)objectId;
     *      fm.storeIntField (0, oid.id);
     *      fm.storeStringField (1, oid.name);
     *      fm.storeObjectField (2, oid.salary);
     * }
     * </code>
     * <P>
     * The implementation is responsible for implementing the <code>ObjectIdFieldConsumer</code> to store the
     * values for the key fields.
     * @param oid the ObjectId source of the copy.
     * @param fm the field manager that receives the field values.
     */
    void dnCopyKeyFieldsFromObjectId(ObjectIdFieldConsumer fm, Object oid);

    /**
     * This interface is a convenience interface that allows an instance to implement both
     * <code>ObjectIdFieldSupplier</code> and <code>ObjectIdFieldConsumer</code>.
     */
    static interface ObjectIdFieldManager extends ObjectIdFieldConsumer, ObjectIdFieldSupplier
    {
    }

    /**
     * This interface is used to provide fields to the Object id instance. It is used by the method
     * copyKeyFieldsToObjectId. When the method is called, the generated code calls the instance of
     * ObjectIdFieldManager for each field in the object id.
     */
    static interface ObjectIdFieldSupplier
    {
        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        boolean fetchBooleanField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        char fetchCharField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        byte fetchByteField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        short fetchShortField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        int fetchIntField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        long fetchLongField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        float fetchFloatField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        double fetchDoubleField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        String fetchStringField(int fieldNumber);

        /**
         * Fetch one field from the field manager. This field will be stored in the proper field of the
         * ObjectId.
         * @param fieldNumber the field number of the key field.
         * @return the value of the field to be stored into the ObjectId.
         */
        Object fetchObjectField(int fieldNumber);
    }

    /**
     * This interface is used to store fields from the Object id instance. It is used by the method
     * copyKeyFieldsFromObjectId. When the method is called, the generated code calls the instance of
     * ObjectIdFieldManager for each field in the object id.
     */
    static interface ObjectIdFieldConsumer
    {
        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeBooleanField(int fieldNumber, boolean value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeCharField(int fieldNumber, char value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeByteField(int fieldNumber, byte value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeShortField(int fieldNumber, short value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeIntField(int fieldNumber, int value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeLongField(int fieldNumber, long value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeFloatField(int fieldNumber, float value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeDoubleField(int fieldNumber, double value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeStringField(int fieldNumber, String value);

        /**
         * Store one field into the field manager. This field was retrieved from the field of the ObjectId.
         * @param fieldNumber the field number of the key field.
         * @param value the value of the field from the ObjectId.
         */
        void storeObjectField(int fieldNumber, Object value);
    }
}