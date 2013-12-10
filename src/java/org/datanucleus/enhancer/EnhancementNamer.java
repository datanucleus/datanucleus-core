/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

/**
 * Interface defining the naming of fields/classes used in enhancement.
 */
public interface EnhancementNamer
{
    /** Field name of StateManager */
    String getStateManagerFieldName();

    /** Field name of Flags */
    String getFlagsFieldName();

    /** Field name of FieldNames */
    String getFieldNamesFieldName();

    /** Field name of FieldTypes */
    String getFieldTypesFieldName();

    /** Field name of FieldFlags */
    String getFieldFlagsFieldName();

    /** Field name of PersistenceCapableSuperclass */
    String getPersistableSuperclassFieldName();

    /** Field name of FieldCount */
    String getInheritedFieldCountFieldName();

    /** Field name of DetachedState */
    String getDetachedStateFieldName();

    /** Field name of serialVersionUID */
    String getSerialVersionUidFieldName();

    /** Method name for initialising FieldNames  */
    String getFieldNamesInitMethodName();

    /** Method name for initialising FieldTypes */
    String getFieldTypesInitMethodName();

    /** Method name for initialising FieldFlags */
    String getFieldFlagsInitMethodName();

    /** Method name for object id accessor  */
    String getGetObjectIdMethodName();

    /** Method name for transactional object id accessor  */
    String getGetTransactionalObjectIdMethodName();

    /** Method name for version accessor  */
    String getGetVersionMethodName();

    /** Method name of IsDetached */
    String getIsDetachedMethodName();

    /** Method name of IsDetachedInternal */
    String getIsDetachedInternalMethodName();

    /** Method name of IsDeleted */
    String getIsDeletedMethodName();

    /** Method name of IsDirty */
    String getIsDirtyMethodName();

    /** Method name of IsNew */
    String getIsNewMethodName();

    /** Method name of IsPersistent */
    String getIsPersistentMethodName();

    /** Method name of IsTransactional */
    String getIsTransactionalMethodName();

    /** Method name of GetPersistenceManager */
    String getGetPersistenceManagerMethodName();

    /** Method name of PreSerialize */
    String getPreSerializeMethodName();

    /** Method name for GetInheritedFieldCount */
    String getGetInheritedFieldCountMethodName();

    /** Method name for SuperClone */
    String getSuperCloneMethodName();

    /** Method name for GetManagedFieldCount */
    String getGetManagedFieldCountMethodName();

    /** Method name for PersistableSuperclassInit */
    String getPersistableSuperclassInitMethodName();

    /** Method name of LoadClass */
    String getLoadClassMethodName();

    /** Method name of CopyField */
    String getCopyFieldMethodName();

    /** Method name of CopyFields */
    String getCopyFieldsMethodName();

    /** Method name of CopyFieldsFromObjectId */
    String getCopyKeyFieldsFromObjectIdMethodName();

    /** Method name of CopyFieldsToObjectId */
    String getCopyKeyFieldsToObjectIdMethodName();

    /** Method name of ProvideField */
    String getProvideFieldMethodName();

    /** Method name of ProvideFields */
    String getProvideFieldsMethodName();

    /** Method name of ReplaceField */
    String getReplaceFieldMethodName();

    /** Method name of ReplaceFields. */
    String getReplaceFieldsMethodName();

    /** Method name of ReplaceFlags. */
    String getReplaceFlagsMethodName();

    /** Method name of ReplaceStateManager. */
    String getReplaceStateManagerMethodName();

    /** Method name of ReplaceDetachedState. */
    String getReplaceDetachedStateMethodName();

    /** Method name of MakeDirty. */
    String getMakeDirtyMethodName();

    /** Method name of MakeDirtyDetached. */
    String getMakeDirtyDetachedMethodName();

    /** Method name of NewInstance. */
    String getNewInstanceMethodName();

    /** Method name of NewObjectIdInstance. */
    String getNewObjectIdInstanceMethodName();

    /** Prefix for method names for getXXX. */
    String getGetMethodPrefixMethodName();

    /** Prefix for method names for setXXX. */
    String getSetMethodPrefixMethodName();

    /** ASM class name for DetachListener. */
    String getDetachListenerAsmClassName();

    /** ASM class name for StateManager. */
    String getStateManagerAsmClassName();

    /** ASM class name for PersistenceManager. */
    String getPersistenceManagerAsmClassName();

    /** ASM class name for Persistable. */
    String getPersistableAsmClassName();

    /** ASM class name for Detachable. */
    String getDetachableAsmClassName();

    /** ASM class name for ObjectIdFieldConsumer. */
    String getObjectIdFieldConsumerAsmClassName();

    /** ASM class name for ObjectIdFieldSupplier. */
    String getObjectIdFieldSupplierAsmClassName();

    /** ASM class name for DetachedFieldAccessException. */
    String getDetachedFieldAccessExceptionAsmClassName();

    /** ASM class name for FatalInternalException. */
    String getFatalInternalExceptionAsmClassName();

    /** ASM class name for Helper. */
    String getHelperAsmClassName();

    /** ASM class name for ImplHelper. */
    String getImplHelperAsmClassName();

    /** Descriptor for ByteIdentity. */
    String getByteIdentityDescriptor();

    /** Descriptor for CharIdentity. */
    String getCharIdentityDescriptor();

    /** Descriptor for IntIdentity. */
    String getIntIdentityDescriptor();

    /** Descriptor for LongIdentity. */
    String getLongIdentityDescriptor();

    /** Descriptor for ShortIdentity. */
    String getShortIdentityDescriptor();

    /** Descriptor for StringIdentity. */
    String getStringIdentityDescriptor();

    /** Descriptor for ObjectIdentity. */
    String getObjectIdentityDescriptor();

    /**
     * Accessor for the descriptor for a SingleFieldIdentity type.
     * @param oidClassName Name of the SingleFieldIdentity class
     * @return The descriptor of the SingleFieldIdentity type
     */
    String getSingleFieldIdentityDescriptor(String oidClassName);

    /**
     * Method to return the type descriptor for the key of the provided single-field identity class name.
     * @param oidClassName Single-field identity class name
     * @return The type descriptor for the key
     */
    String getTypeDescriptorForSingleFieldIdentityGetKey(String oidClassName);

    /**
     * Convenience method to give the method type name for a singleFieldIdentity class name.
     * Used for aaaCopyKeyFields[To/From]ObjectId and defines the "type name" used for things like storeXXXField.
     * <ul>
     * <li>Byte, byte : returns "Byte"</li>
     * <li>Character, char : returns "Char"</li>
     * <li>Integer, int : returns "Int"</li>
     * <li>Long, long : returns "Long"</li>
     * <li>Short, short : returns "Short"</li>
     * <li>String : returns "String"</li>
     * <li>all others : returns "Object"</li>
     * </ul>
     * @param oidClassName Name of the single field identity class
     * @return Name for the method
     */
    String getTypeNameForUseWithSingleFieldIdentity(String oidClassName);

    /** Descriptor for StateManager. */
    String getStateManagerDescriptor();

    /** Descriptor for PersistenceManager. */
    String getPersistenceManagerDescriptor();

    /** Descriptor for PersistenceCapable. */
    String getPersistableDescriptor();

    /** Descriptor for Detachable. */
    String getDetachableDescriptor();

    /** Descriptor for ObjectIdFieldConsumer. */
    String getObjectIdFieldConsumerDescriptor();

    /** Descriptor for ObjectIdFieldSupplier. */
    String getObjectIdFieldSupplierDescriptor();

    /** Class for the PersistenceManager interface. */
    Class getPersistenceManagerClass();

    /** Class for the StateManager interface. */
    Class getStateManagerClass();

    /** Class for the Persistable interface. */
    Class getPersistableClass();

    /** Class for the Detachable interface. */
    Class getDetachableClass();

    /** Class for ObjectIdFieldSupplier. */
    Class getObjectIdFieldSupplierClass();

    /** Class for ObjectIdFieldConsumer. */
    Class getObjectIdFieldConsumerClass();

    /** Class for ObjectIdentity. */
    Class getObjectIdentityClass();
}