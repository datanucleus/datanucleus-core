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

import javax.jdo.JDODetachedFieldAccessException;
import javax.jdo.JDOFatalInternalException;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.identity.ByteIdentity;
import javax.jdo.identity.CharIdentity;
import javax.jdo.identity.IntIdentity;
import javax.jdo.identity.LongIdentity;
import javax.jdo.identity.ObjectIdentity;
import javax.jdo.identity.ShortIdentity;
import javax.jdo.identity.StringIdentity;
import javax.jdo.spi.Detachable;
import javax.jdo.spi.JDOImplHelper;
import javax.jdo.spi.PersistenceCapable;
import javax.jdo.spi.StateManager;

import org.datanucleus.asm.Type;
import org.datanucleus.util.DetachListener;

/**
 * Definition of enhancement naming for use with the JDO API.
 * All field/class names match those in the JDO spec.
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
    private static final Class CL_Persistable = PersistenceCapable.class;
    private static final Class CL_ObjectIdFieldConsumer = PersistenceCapable.ObjectIdFieldConsumer.class;
    private static final Class CL_ObjectIdFieldSupplier = PersistenceCapable.ObjectIdFieldSupplier.class;
    private static final Class CL_PersistenceManager = PersistenceManager.class;
    private static final Class CL_StateManager = StateManager.class;

    private final static String ACN_DetachListener = DetachListener.class.getName().replace('.', '/');
    private final static String ACN_StateManager = CL_StateManager.getName().replace('.', '/');
    private final static String ACN_PersistenceManager = CL_PersistenceManager.getName().replace('.', '/');
    private final static String ACN_Persistable = CL_Persistable.getName().replace('.', '/');
    private final static String ACN_Detachable = CL_Detachable.getName().replace('.', '/');
    private final static String ACN_ObjectIdFieldConsumer = CL_ObjectIdFieldConsumer.getName().replace('.', '/');
    private final static String ACN_ObjectIdFieldSupplier = CL_ObjectIdFieldSupplier.getName().replace('.', '/');
    private final static String ACN_DetachedFieldAccessException = JDODetachedFieldAccessException.class.getName().replace('.', '/');
    private final static String ACN_FatalInternalException = JDOFatalInternalException.class.getName().replace('.', '/');
    private final static String ACN_Helper = JDOHelper.class.getName().replace('.', '/');
    private final static String ACN_ImplHelper = JDOImplHelper.class.getName().replace('.', '/');

    private final static String CD_ByteIdentity = Type.getDescriptor(ByteIdentity.class);
    private final static String CD_CharIdentity = Type.getDescriptor(CharIdentity.class);
    private final static String CD_IntIdentity = Type.getDescriptor(IntIdentity.class);
    private final static String CD_LongIdentity = Type.getDescriptor(LongIdentity.class);
    private final static String CD_ShortIdentity = Type.getDescriptor(ShortIdentity.class);
    private final static String CD_StringIdentity = Type.getDescriptor(StringIdentity.class);
    private final static String CD_ObjectIdentity = Type.getDescriptor(ObjectIdentity.class);
    private final static String CD_StateManager = Type.getDescriptor(StateManager.class);
    private final static String CD_PersistenceManager = Type.getDescriptor(PersistenceManager.class);
    private final static String CD_PersistenceCapable = Type.getDescriptor(PersistenceCapable.class);
    private final static String CD_Detachable = Type.getDescriptor(CL_Detachable);
    private final static String CD_ObjectIdFieldConsumer = Type.getDescriptor(PersistenceCapable.ObjectIdFieldConsumer.class);
    private final static String CD_ObjectIdFieldSupplier = Type.getDescriptor(PersistenceCapable.ObjectIdFieldSupplier.class);
    private final static String CD_String = Type.getDescriptor(String.class);
    private final static String CD_Object = Type.getDescriptor(Object.class);

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStateManagerFieldName()
     */
    public String getStateManagerFieldName()
    {
        return "jdoStateManager";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFlagsFieldName()
     */
    public String getFlagsFieldName()
    {
        return "jdoFlags";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldNamesFieldName()
     */
    public String getFieldNamesFieldName()
    {
        return "jdoFieldNames";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldTypesFieldName()
     */
    public String getFieldTypesFieldName()
    {
        return "jdoFieldTypes";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldFlagsFieldName()
     */
    public String getFieldFlagsFieldName()
    {
        return "jdoFieldFlags";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableSuperclassFieldName()
     */
    public String getPersistableSuperclassFieldName()
    {
        return "jdoPersistenceCapableSuperclass";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getInheritedFieldCountFieldName()
     */
    public String getInheritedFieldCountFieldName()
    {
        return "jdoInheritedFieldCount";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getDetachedStateFieldName()
     */
    public String getDetachedStateFieldName()
    {
        return "jdoDetachedState";
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
        return "__jdoFieldNamesInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldTypesInitMethodName()
     */
    public String getFieldTypesInitMethodName()
    {
        return "__jdoFieldTypesInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getFieldFlagsInitMethodName()
     */
    public String getFieldFlagsInitMethodName()
    {
        return "__jdoFieldFlagsInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetObjectIdMethodName()
     */
    public String getGetObjectIdMethodName()
    {
        return "jdoGetObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetTransactionalObjectIdMethodName()
     */
    public String getGetTransactionalObjectIdMethodName()
    {
        return "jdoGetTransactionalObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetVersionMethodName()
     */
    public String getGetVersionMethodName()
    {
        return "jdoGetVersion";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDetachedMethodName()
     */
    public String getIsDetachedMethodName()
    {
        return "jdoIsDetached";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDetachedInternalMethodName()
     */
    public String getIsDetachedInternalMethodName()
    {
        return "jdoIsDetachedInternal";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDeletedMethodName()
     */
    public String getIsDeletedMethodName()
    {
        return "jdoIsDeleted";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsDirtyMethodName()
     */
    public String getIsDirtyMethodName()
    {
        return "jdoIsDirty";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsNewMethodName()
     */
    public String getIsNewMethodName()
    {
        return "jdoIsNew";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsPersistentMethodName()
     */
    public String getIsPersistentMethodName()
    {
        return "jdoIsPersistent";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIsTransactionalMethodName()
     */
    public String getIsTransactionalMethodName()
    {
        return "jdoIsTransactional";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetPersistenceManagerMethodName()
     */
    public String getGetPersistenceManagerMethodName()
    {
        return "jdoGetPersistenceManager";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPreSerializeMethodName()
     */
    public String getPreSerializeMethodName()
    {
        return "jdoPreSerialize";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetInheritedFieldCountMethodName()
     */
    public String getGetInheritedFieldCountMethodName()
    {
        return "__jdoGetInheritedFieldCount";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getSuperCloneMethodName()
     */
    public String getSuperCloneMethodName()
    {
        return "jdoSuperClone";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetManagedFieldCountMethodName()
     */
    public String getGetManagedFieldCountMethodName()
    {
        return "jdoGetManagedFieldCount";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableSuperclassInitMethodName()
     */
    public String getPersistableSuperclassInitMethodName()
    {
        return "__jdoPersistenceCapableSuperclassInit";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getLoadClassMethodName()
     */
    public String getLoadClassMethodName()
    {
        return "___jdo$loadClass";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyFieldMethodName()
     */
    public String getCopyFieldMethodName()
    {
        return "jdoCopyField";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyFieldsMethodName()
     */
    public String getCopyFieldsMethodName()
    {
        return "jdoCopyFields";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyKeyFieldsFromObjectIdMethodName()
     */
    public String getCopyKeyFieldsFromObjectIdMethodName()
    {
        return "jdoCopyKeyFieldsFromObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCopyKeyFieldsToObjectIdMethodName()
     */
    public String getCopyKeyFieldsToObjectIdMethodName()
    {
        return "jdoCopyKeyFieldsToObjectId";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getProvideFieldMethodName()
     */
    public String getProvideFieldMethodName()
    {
        return "jdoProvideField";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getProvideFieldsMethodName()
     */
    public String getProvideFieldsMethodName()
    {
        return "jdoProvideFields";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceFieldMethodName()
     */
    public String getReplaceFieldMethodName()
    {
        return "jdoReplaceField";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceFieldsMethodName()
     */
    public String getReplaceFieldsMethodName()
    {
        return "jdoReplaceFields";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceFlagsMethodName()
     */
    public String getReplaceFlagsMethodName()
    {
        return "jdoReplaceFlags";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceStateManagerMethodName()
     */
    public String getReplaceStateManagerMethodName()
    {
        return "jdoReplaceStateManager";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getReplaceDetachedStateMethodName()
     */
    public String getReplaceDetachedStateMethodName()
    {
        return "jdoReplaceDetachedState";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getMakeDirtyMethodName()
     */
    public String getMakeDirtyMethodName()
    {
        return "jdoMakeDirty";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getMakeDirtyDetachedMethodName()
     */
    public String getMakeDirtyDetachedMethodName()
    {
        return "jdoMakeDirtyDetached";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getNewInstanceMethodName()
     */
    public String getNewInstanceMethodName()
    {
        return "jdoNewInstance";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getNewObjectIdInstanceMethodName()
     */
    public String getNewObjectIdInstanceMethodName()
    {
        return "jdoNewObjectIdInstance";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getGetMethodPrefixMethodName()
     */
    public String getGetMethodPrefixMethodName()
    {
        return "jdoGet";
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getSetMethodPrefixMethodName()
     */
    public String getSetMethodPrefixMethodName()
    {
        return "jdoSet";
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
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistenceManagerAsmClassName()
     */
    public String getPersistenceManagerAsmClassName()
    {
        return ACN_PersistenceManager;
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
     * @see org.datanucleus.enhancer.ClassEnhancer#getHelperAsmClassName()
     */
    public String getHelperAsmClassName()
    {
        return ACN_Helper;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getImplHelperAsmClassName()
     */
    public String getImplHelperAsmClassName()
    {
        return ACN_ImplHelper;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getByteIdentityDescriptor()
     */
    public String getByteIdentityDescriptor()
    {
        return CD_ByteIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getCharIdentityDescriptor()
     */
    public String getCharIdentityDescriptor()
    {
        return CD_CharIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getIntIdentityDescriptor()
     */
    public String getIntIdentityDescriptor()
    {
        return CD_IntIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getLongIdentityDescriptor()
     */
    public String getLongIdentityDescriptor()
    {
        return CD_LongIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getShortIdentityDescriptor()
     */
    public String getShortIdentityDescriptor()
    {
        return CD_ShortIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStringIdentityDescriptor()
     */
    public String getStringIdentityDescriptor()
    {
        return CD_StringIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getObjectIdentityDescriptor()
     */
    public String getObjectIdentityDescriptor()
    {
        return CD_ObjectIdentity;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getStateManagerDescriptor()
     */
    public String getStateManagerDescriptor()
    {
        return CD_StateManager;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistenceManagerDescriptor()
     */
    public String getPersistenceManagerDescriptor()
    {
        return CD_PersistenceManager;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistableDescriptor()
     */
    public String getPersistableDescriptor()
    {
        return CD_PersistenceCapable;
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
        if (oidClassName.equals(LongIdentity.class.getName()))
        {
            return CD_LongIdentity;
        }
        else if (oidClassName.equals(IntIdentity.class.getName()))
        {
            return CD_IntIdentity;
        }
        else if (oidClassName.equals(StringIdentity.class.getName()))
        {
            return CD_StringIdentity;
        }
        else if (oidClassName.equals(ShortIdentity.class.getName()))
        {
            return CD_ShortIdentity;
        }
        else if (oidClassName.equals(CharIdentity.class.getName()))
        {
            return CD_CharIdentity;
        }
        else if (oidClassName.equals(ByteIdentity.class.getName()))
        {
            return CD_ByteIdentity;
        }
        else if (oidClassName.equals(ObjectIdentity.class.getName()))
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
        if (oidClassName.equals(LongIdentity.class.getName()))
        {
            return Type.LONG_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(IntIdentity.class.getName()))
        {
            return Type.INT_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ShortIdentity.class.getName()))
        {
            return Type.SHORT_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(CharIdentity.class.getName()))
        {
            return Type.CHAR_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(ByteIdentity.class.getName()))
        {
            return Type.BYTE_TYPE.getDescriptor();
        }
        else if (oidClassName.equals(StringIdentity.class.getName()))
        {
            return CD_String;
        }
        else if (oidClassName.equals(ObjectIdentity.class.getName()))
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
        else if (oidClassName.equals(ByteIdentity.class.getName()))
        {
            return "Byte";
        }
        else if (oidClassName.equals(CharIdentity.class.getName()))
        {
            return "Char";
        }
        else if (oidClassName.equals(IntIdentity.class.getName()))
        {
            return "Int";
        }
        else if (oidClassName.equals(LongIdentity.class.getName()))
        {
            return "Long";
        }
        else if (oidClassName.equals(ShortIdentity.class.getName()))
        {
            return "Short";
        }
        else if (oidClassName.equals(StringIdentity.class.getName()))
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
     * @see org.datanucleus.enhancer.ClassEnhancer#getPersistenceManagerClass()
     */
    public Class getPersistenceManagerClass()
    {
        return CL_PersistenceManager;
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
        return ObjectIdentity.class;
    }
}
