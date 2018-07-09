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
    String getStateManagerFieldName();

    String getFlagsFieldName();

    String getFieldNamesFieldName();

    String getInheritedFieldCountFieldName();

    String getDetachedStateFieldName();

    String getSerialVersionUidFieldName();

    String getFieldNamesInitMethodName();

    String getGetObjectIdMethodName();

    String getGetTransactionalObjectIdMethodName();

    String getGetVersionMethodName();

    String getIsDetachedMethodName();

    String getIsDetachedInternalMethodName();

    String getIsDeletedMethodName();

    String getIsDirtyMethodName();

    String getIsNewMethodName();

    String getIsPersistentMethodName();

    String getIsTransactionalMethodName();

    String getGetExecutionContextMethodName();

    String getGetStateManagerMethodName();

    String getPreSerializeMethodName();

    String getGetInheritedFieldCountMethodName();

    String getCloneMethodName();

    String getGetManagedFieldCountMethodName();

    String getLoadClassMethodName();

    String getCopyFieldMethodName();

    String getCopyFieldsMethodName();

    String getCopyKeyFieldsFromObjectIdMethodName();

    String getCopyKeyFieldsToObjectIdMethodName();

    String getProvideFieldMethodName();

    String getProvideFieldsMethodName();

    String getReplaceFieldMethodName();

    String getReplaceFieldsMethodName();

    String getReplaceFlagsMethodName();

    String getReplaceStateManagerMethodName();

    String getReplaceDetachedStateMethodName();

    String getMakeDirtyMethodName();

    String getMakeDirtyDetachedMethodName();

    String getNewInstanceMethodName();

    String getNewObjectIdInstanceMethodName();

    String getGetMethodPrefixMethodName();

    String getSetMethodPrefixMethodName();

    String getDetachListenerAsmClassName();

    String getStateManagerAsmClassName();

    String getExecutionContextAsmClassName();

    String getPersistableAsmClassName();

    String getDetachableAsmClassName();

    String getObjectIdFieldConsumerAsmClassName();

    String getObjectIdFieldSupplierAsmClassName();

    String getDetachedFieldAccessExceptionAsmClassName();

    String getFatalInternalExceptionAsmClassName();

    String getImplHelperAsmClassName();

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

    String getStateManagerDescriptor();

    String getExecutionContextDescriptor();

    String getPersistableDescriptor();

    String getDetachableDescriptor();

    String getObjectIdFieldConsumerDescriptor();

    String getObjectIdFieldSupplierDescriptor();

    Class getExecutionContextClass();

    Class getStateManagerClass();

    Class getPersistableClass();

    Class getDetachableClass();

    Class getObjectIdFieldSupplierClass();

    Class getObjectIdFieldConsumerClass();

    Class getObjectIdentityClass();
}