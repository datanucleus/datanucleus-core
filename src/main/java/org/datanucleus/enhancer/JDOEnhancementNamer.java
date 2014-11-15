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

import org.datanucleus.ClassConstants;
import org.datanucleus.ClassNameConstants;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancement.Detachable;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.util.DetachListener;

/**
 * Definition of enhancement naming, for use with the JDO API.
 * Note that this does not provide "binary compatibility" since we need to provide something that works for JDO and JPA and cannot rely
 * on having jdo-api.jar present.
 */
public class JDOEnhancementNamer implements EnhancementNamer
{
    private static JDOEnhancementNamer instance = null;

    public static JDOEnhancementNamer getInstance()
    {
        if (instance == null)
        {
            instance = new JDOEnhancementNamer();
        }
        return instance;
    }

    protected JDOEnhancementNamer()
    {
    }

    private static final Class CL_Detachable = Detachable.class;
    private static final Class CL_Persistable = ClassConstants.PERSISTABLE;
    private static final Class CL_ObjectIdFieldConsumer = Persistable.ObjectIdFieldConsumer.class;
    private static final Class CL_ObjectIdFieldSupplier = Persistable.ObjectIdFieldSupplier.class;
    private static final Class CL_ExecutionContextRef = ClassConstants.EXECUTION_CONTEXT_REFERENCE;
    private static final Class CL_StateManager = ClassConstants.STATE_MANAGER;

    private final static String ACN_DetachListener = DetachListener.class.getName().replace('.', '/');
    private final static String ACN_StateManager = CL_StateManager.getName().replace('.', '/');
    private final static String ACN_ExecutionContext = CL_ExecutionContextRef.getName().replace('.', '/');
    private final static String ACN_Persistable = CL_Persistable.getName().replace('.', '/');
    private final static String ACN_Detachable = CL_Detachable.getName().replace('.', '/');
    private final static String ACN_ObjectIdFieldConsumer = CL_ObjectIdFieldConsumer.getName().replace('.', '/');
    private final static String ACN_ObjectIdFieldSupplier = CL_ObjectIdFieldSupplier.getName().replace('.', '/');
    private final static String ACN_DetachedFieldAccessException = "javax/jdo/JDODetachedFieldAccessException";
    private final static String ACN_FatalInternalException = "javax/jdo/JDOFatalInternalException";
    private final static String ACN_ImplHelper = EnhancementHelper.class.getName().replace('.', '/');

    private final static String CD_ByteIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_BYTE);
    private final static String CD_CharIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_CHAR);
    private final static String CD_IntIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_INT);
    private final static String CD_LongIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_LONG);
    private final static String CD_ShortIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_SHORT);
    private final static String CD_StringIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_STRING);
    private final static String CD_ObjectIdentity = Type.getDescriptor(ClassConstants.IDENTITY_SINGLEFIELD_OBJECT);
    private final static String CD_StateManager = Type.getDescriptor(ClassConstants.STATE_MANAGER);
    private final static String CD_ExecutionContextRef = Type.getDescriptor(ClassConstants.EXECUTION_CONTEXT_REFERENCE);
    private final static String CD_Persistable = Type.getDescriptor(ClassConstants.PERSISTABLE);
    private final static String CD_Detachable = Type.getDescriptor(CL_Detachable);
    private final static String CD_ObjectIdFieldConsumer = Type.getDescriptor(Persistable.ObjectIdFieldConsumer.class);
    private final static String CD_ObjectIdFieldSupplier = Type.getDescriptor(Persistable.ObjectIdFieldSupplier.class);
    private final static String CD_String = Type.getDescriptor(String.class);
    private final static String CD_Object = Type.getDescriptor(Object.class);

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStateManagerFieldName()
     */
    public String getStateManagerFieldName()
    {
        return "dnStateManager";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFlagsFieldName()
     */
    public String getFlagsFieldName()
    {
        return "dnFlags";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldNamesFieldName()
     */
    public String getFieldNamesFieldName()
    {
        return "dnFieldNames";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldTypesFieldName()
     */
    public String getFieldTypesFieldName()
    {
        return "dnFieldTypes";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldFlagsFieldName()
     */
    public String getFieldFlagsFieldName()
    {
        return "dnFieldFlags";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableSuperclassFieldName()
     */
    public String getPersistableSuperclassFieldName()
    {
        return "dnPersistableSuperclass";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getInheritedFieldCountFieldName()
     */
    public String getInheritedFieldCountFieldName()
    {
        return "dnInheritedFieldCount";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getDetachedStateFieldName()
     */
    public String getDetachedStateFieldName()
    {
        return "dnDetachedState";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getSerialVersionUidFieldName()
     */
    public String getSerialVersionUidFieldName()
    {
        return "serialVersionUID";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldNamesInitMethodName()
     */
    public String getFieldNamesInitMethodName()
    {
        return "__dnFieldNamesInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldTypesInitMethodName()
     */
    public String getFieldTypesInitMethodName()
    {
        return "__dnFieldTypesInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldFlagsInitMethodName()
     */
    public String getFieldFlagsInitMethodName()
    {
        return "__dnFieldFlagsInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetObjectIdMethodName()
     */
    public String getGetObjectIdMethodName()
    {
        return "dnGetObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetTransactionalObjectIdMethodName()
     */
    public String getGetTransactionalObjectIdMethodName()
    {
        return "dnGetTransactionalObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetVersionMethodName()
     */
    public String getGetVersionMethodName()
    {
        return "dnGetVersion";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDetachedMethodName()
     */
    public String getIsDetachedMethodName()
    {
        return "dnIsDetached";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDetachedInternalMethodName()
     */
    public String getIsDetachedInternalMethodName()
    {
        return "dnIsDetachedInternal";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDeletedMethodName()
     */
    public String getIsDeletedMethodName()
    {
        return "dnIsDeleted";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDirtyMethodName()
     */
    public String getIsDirtyMethodName()
    {
        return "dnIsDirty";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsNewMethodName()
     */
    public String getIsNewMethodName()
    {
        return "dnIsNew";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsPersistentMethodName()
     */
    public String getIsPersistentMethodName()
    {
        return "dnIsPersistent";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsTransactionalMethodName()
     */
    public String getIsTransactionalMethodName()
    {
        return "dnIsTransactional";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetExecutionContextMethodName()
     */
    public String getGetExecutionContextMethodName()
    {
        return "dnGetExecutionContext";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPreSerializeMethodName()
     */
    public String getPreSerializeMethodName()
    {
        return "dnPreSerialize";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetInheritedFieldCountMethodName()
     */
    public String getGetInheritedFieldCountMethodName()
    {
        return "__dnGetInheritedFieldCount";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getSuperCloneMethodName()
     */
    public String getSuperCloneMethodName()
    {
        return "dnSuperClone";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetManagedFieldCountMethodName()
     */
    public String getGetManagedFieldCountMethodName()
    {
        return "dnGetManagedFieldCount";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableSuperclassInitMethodName()
     */
    public String getPersistableSuperclassInitMethodName()
    {
        return "__dnPersistableSuperclassInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getLoadClassMethodName()
     */
    public String getLoadClassMethodName()
    {
        return "___dn$loadClass";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyFieldMethodName()
     */
    public String getCopyFieldMethodName()
    {
        return "dnCopyField";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyFieldsMethodName()
     */
    public String getCopyFieldsMethodName()
    {
        return "dnCopyFields";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyKeyFieldsFromObjectIdMethodName()
     */
    public String getCopyKeyFieldsFromObjectIdMethodName()
    {
        return "dnCopyKeyFieldsFromObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyKeyFieldsToObjectIdMethodName()
     */
    public String getCopyKeyFieldsToObjectIdMethodName()
    {
        return "dnCopyKeyFieldsToObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getProvideFieldMethodName()
     */
    public String getProvideFieldMethodName()
    {
        return "dnProvideField";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getProvideFieldsMethodName()
     */
    public String getProvideFieldsMethodName()
    {
        return "dnProvideFields";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceFieldMethodName()
     */
    public String getReplaceFieldMethodName()
    {
        return "dnReplaceField";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceFieldsMethodName()
     */
    public String getReplaceFieldsMethodName()
    {
        return "dnReplaceFields";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceFlagsMethodName()
     */
    public String getReplaceFlagsMethodName()
    {
        return "dnReplaceFlags";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceStateManagerMethodName()
     */
    public String getReplaceStateManagerMethodName()
    {
        return "dnReplaceStateManager";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceDetachedStateMethodName()
     */
    public String getReplaceDetachedStateMethodName()
    {
        return "dnReplaceDetachedState";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getMakeDirtyMethodName()
     */
    public String getMakeDirtyMethodName()
    {
        return "dnMakeDirty";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getMakeDirtyDetachedMethodName()
     */
    public String getMakeDirtyDetachedMethodName()
    {
        return "dnMakeDirtyDetached";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getNewInstanceMethodName()
     */
    public String getNewInstanceMethodName()
    {
        return "dnNewInstance";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getNewObjectIdInstanceMethodName()
     */
    public String getNewObjectIdInstanceMethodName()
    {
        return "dnNewObjectIdInstance";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetMethodPrefixMethodName()
     */
    public String getGetMethodPrefixMethodName()
    {
        return "dnGet";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getSetMethodPrefixMethodName()
     */
    public String getSetMethodPrefixMethodName()
    {
        return "dnSet";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.EnhancementNamer#getDetachListenerAsmClassName()
     */
    public String getDetachListenerAsmClassName()
    {
        return ACN_DetachListener;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStateManagerAsmClassName()
     */
    public String getStateManagerAsmClassName()
    {
        return ACN_StateManager;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getExecutionContextAsmClassName()
     */
    public String getExecutionContextAsmClassName()
    {
        return ACN_ExecutionContext;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableAsmClassName()
     */
    public String getPersistableAsmClassName()
    {
        return ACN_Persistable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getDetachableAsmClassName()
     */
    public String getDetachableAsmClassName()
    {
        return ACN_Detachable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdFieldConsumerAsmClassName()
     */
    public String getObjectIdFieldConsumerAsmClassName()
    {
        return ACN_ObjectIdFieldConsumer;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdFieldSupplierAsmClassName()
     */
    public String getObjectIdFieldSupplierAsmClassName()
    {
        return ACN_ObjectIdFieldSupplier;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getDetachedFieldAccessExceptionAsmClassName()
     */
    public String getDetachedFieldAccessExceptionAsmClassName()
    {
        return ACN_DetachedFieldAccessException;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFatalInternalExceptionAsmClassName()
     */
    public String getFatalInternalExceptionAsmClassName()
    {
        return ACN_FatalInternalException;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getImplHelperAsmClassName()
     */
    public String getImplHelperAsmClassName()
    {
        return ACN_ImplHelper;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStateManagerDescriptor()
     */
    public String getStateManagerDescriptor()
    {
        return CD_StateManager;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getExecutionContextDescriptor()
     */
    public String getExecutionContextDescriptor()
    {
        return CD_ExecutionContextRef;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableDescriptor()
     */
    public String getPersistableDescriptor()
    {
        return CD_Persistable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getDetachableDescriptor()
     */
    public String getDetachableDescriptor()
    {
        return CD_Detachable;
    }

    /**
     * Accessor for the descriptor for a SingleFieldIdentity type.
     * @param oidClassName Name of the SingleFieldIdentity class
     * @return The descriptor of the SingleFieldIdentity type
     */
    public String getSingleFieldIdentityDescriptor(String oidClassName)
    {
        if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_LONG))
        {
            return CD_LongIdentity;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_INT))
        {
            return CD_IntIdentity;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_STRING))
        {
            return CD_StringIdentity;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_SHORT))
        {
            return CD_ShortIdentity;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_CHAR))
        {
            return CD_CharIdentity;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_BYTE))
        {
            return CD_ByteIdentity;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_OBJECT))
        {
            return CD_ObjectIdentity;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getTypeDescriptorForSingleFieldIdentityGetKey(java.lang.String)
     */
    public String getTypeDescriptorForSingleFieldIdentityGetKey(String oidClassName)
    {
        if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_LONG))
        {
            return Type.LONG_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_INT))
        {
            return Type.INT_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_SHORT))
        {
            return Type.SHORT_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_CHAR))
        {
            return Type.CHAR_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_BYTE))
        {
            return Type.BYTE_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_STRING))
        {
            return CD_String;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_OBJECT))
        {
            return CD_Object;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getTypeNameForUseWithSingleFieldIdentity(java.lang.String)
     */
    public String getTypeNameForUseWithSingleFieldIdentity(String oidClassName)
    {
        if (oidClassName == null)
        {
            return null;
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_BYTE))
        {
            return "Byte";
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_CHAR))
        {
            return "Char";
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_INT))
        {
            return "Int";
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_LONG))
        {
            return "Long";
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_SHORT))
        {
            return "Short";
        }
        else if (oidClassName.equals(ClassNameConstants.IDENTITY_SINGLEFIELD_STRING))
        {
            return "String";
        }
        return "Object";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdFieldConsumerDescriptor()
     */
    public String getObjectIdFieldConsumerDescriptor()
    {
        return CD_ObjectIdFieldConsumer;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdFieldSupplierDescriptor()
     */
    public String getObjectIdFieldSupplierDescriptor()
    {
        return CD_ObjectIdFieldSupplier;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getExecutionContextClass()
     */
    public Class getExecutionContextClass()
    {
        return CL_ExecutionContextRef;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStateManagerClass()
     */
    public Class getStateManagerClass()
    {
        return CL_StateManager;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableClass()
     */
    public Class getPersistableClass()
    {
        return CL_Persistable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getDetachableClass()
     */
    public Class getDetachableClass()
    {
        return CL_Detachable;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdFieldSupplierClass()
     */
    public Class getObjectIdFieldSupplierClass()
    {
        return CL_ObjectIdFieldSupplier;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdFieldConsumerClass()
     */
    public Class getObjectIdFieldConsumerClass()
    {
        return CL_ObjectIdFieldConsumer;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdentityClass()
     */
    public Class getObjectIdentityClass()
    {
        return ClassConstants.IDENTITY_SINGLEFIELD_OBJECT;
    }
}