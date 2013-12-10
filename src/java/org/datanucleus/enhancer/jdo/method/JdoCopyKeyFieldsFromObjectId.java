/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.enhancer.jdo.method;

import java.lang.reflect.Modifier;

import org.datanucleus.asm.Label;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.asm.Type;
import org.datanucleus.enhancer.ClassEnhancer;
import org.datanucleus.enhancer.ClassMethod;
import org.datanucleus.enhancer.EnhanceUtils;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.metadata.PropertyMetaData;
import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.JavaUtils;

/**
 * Method to generate the method "jdoCopyKeyFieldsFromObjectId" using ASM.
 * For datastore/nondurable identity this is
 * <pre>
 * public void jdoCopyKeyFieldsFromObjectId(PersistenceCapable.ObjectIdFieldConsumer fc, Object oid)
 * {
 * }
 * </pre>
 * and for SingleFieldIdentity it is
 * <pre>
 * public void jdoCopyKeyFieldsFromObjectId(PersistenceCapable.ObjectIdFieldConsumer fc, Object oid)
 * {
 *     if (fc == null)
 *         throw new IllegalArgumentException("ObjectIdFieldConsumer is null");
 *     if (!(oid instanceof YYYIdentity))
 *         throw new ClassCastException("oid is not instanceof YYYIdentity");
 *     YYYIdentity o = (YYYIdentity) oid;
 *     fc.storeYYYField(1, o.getKey());
 * }
 * </pre>
 * and for user-defined primary keys
 * <pre>
 * public void jdoCopyKeyFieldsFromObjectId(PersistenceCapable.ObjectIdFieldConsumer fc, Object oid)
 * {
 *     if (fc == null)
 *         throw new IllegalArgumentException("ObjectIdFieldConsumer is null");
 *     if (!(oid instanceof UserPrimaryKey))
 *         throw new ClassCastException("oid is not instanceof mydomain.UserPrimaryKey");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     try
 *     {
 *         fc.storeYYYField(1, o.id);
 *         fc.storeZZZField(2, o.name);
 *     }
 *     catch(Exception e) {}
 * }
 * </pre>
 * and for CompoundIdentity
 * <pre>
 * public void jdoCopyKeyFieldsFromObjectId(PersistenceCapable.ObjectIdFieldConsumer fc, Object oid)
 * {
 *     if (fc == null)
 *         throw new IllegalArgumentException("ObjectIdFieldConsumer is null");
 *     if (!(oid instanceof UserPrimaryKey))
 *         throw new ClassCastException("oid is not instanceof mydomain.UserPrimaryKey");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     try
 *     {
 *         fc.storeYYYField(1, o.id);
 *         fc.storeZZZField(2, jdoGetPersistenceManager().getObjectById(o.zzz, false));
 *     }
 *     catch (Exception e) {}
 * }
 * </pre>
 * (the try-catch is for cases where we set the fields with reflection and it can throw an exception).
 * There are some differences for fields .v. properties and also if fields in the PK are private.
 */
public class JdoCopyKeyFieldsFromObjectId extends ClassMethod
{
    public static JdoCopyKeyFieldsFromObjectId getInstance(ClassEnhancer enhancer)
    {
        return new JdoCopyKeyFieldsFromObjectId(enhancer, enhancer.getNamer().getCopyKeyFieldsFromObjectIdMethodName(),
            Opcodes.ACC_PUBLIC,
            null, new Class[] {enhancer.getNamer().getObjectIdFieldConsumerClass(), Object.class}, new String[] { "fc", "oid" });
    }

    /**
     * Constructor.
     * @param enhancer ClassEnhancer
     * @param name Name of method
     * @param access Access type
     * @param returnType Return type
     * @param argTypes Argument types
     * @param argNames Argument names
     */
    public JdoCopyKeyFieldsFromObjectId(ClassEnhancer enhancer, String name, int access, 
        Object returnType, Object[] argTypes, String[] argNames)
    {
        super(enhancer, name, access, returnType, argTypes, argNames);
    }

    /**
     * Method to add the contents of the class method.
     */
    public void execute()
    {
        visitor.visitCode();

        ClassMetaData cmd = enhancer.getClassMetaData();
        if (cmd.getIdentityType() == IdentityType.APPLICATION)
        {
            // application identity
            if (!cmd.isInstantiable())
            {
                // Application identity but mapped-superclass with no PK defined, so just "return"
                Label startLabel = new Label();
                visitor.visitLabel(startLabel);
                visitor.visitInsn(Opcodes.RETURN);
                Label endLabel = new Label();
                visitor.visitLabel(endLabel);
                visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                visitor.visitLocalVariable(argNames[0], getNamer().getObjectIdFieldConsumerDescriptor(), null, startLabel, endLabel, 1);
                visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
                visitor.visitMaxs(0, 3);
            }
            else
            {
                int[] pkFieldNums = cmd.getPKMemberPositions();
                String objectIdClass = cmd.getObjectidClass();
                String ACN_objectIdClass = objectIdClass.replace('.', '/');
                if (enhancer.getMetaDataManager().getApiAdapter().isSingleFieldIdentityClass(objectIdClass))
                {
                    // SingleFieldIdentity
                    Label startLabel = new Label();
                    visitor.visitLabel(startLabel);

                    // if (fc == null) throw new IllegalArgumentException("...");
                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    Label l1 = new Label();
                    visitor.visitJumpInsn(Opcodes.IFNONNULL, l1);
                    visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("ObjectIdFieldConsumer is null");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    // if (!(oid instanceof LongIdentity)) throw new ClassCastException("...");
                    visitor.visitLabel(l1);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitTypeInsn(Opcodes.INSTANCEOF, ACN_objectIdClass);
                    Label l5 = new Label();
                    visitor.visitJumpInsn(Opcodes.IFNE, l5);
                    visitor.visitTypeInsn(Opcodes.NEW, "java/lang/ClassCastException");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("oid is not instanceof " + objectIdClass);
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ClassCastException", "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    // XXXIdentity o = (XXXIdentity) oid;
                    visitor.visitLabel(l5);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, ACN_objectIdClass);
                    visitor.visitVarInsn(Opcodes.ASTORE, 3);

                    // fc.storeXXXField(1, o.getKey());
                    Label l9 = new Label();
                    visitor.visitLabel(l9);

                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    EnhanceUtils.addBIPUSHToMethod(visitor, pkFieldNums[0]);

                    AbstractMemberMetaData fmd = cmd.getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[0]);
                    Class primitiveType = ClassUtils.getPrimitiveTypeForType(fmd.getType());
                    if (primitiveType != null)
                    {
                        // The PK field is a primitive wrapper so create wrapper from getKey()
                        String ACN_fieldType = fmd.getTypeName().replace('.', '/');
                        String getKeyReturnDesc = Type.getDescriptor(primitiveType);

                        visitor.visitVarInsn(Opcodes.ALOAD, 3);
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, 
                            "getKey", "()" + getKeyReturnDesc);
                        visitor.visitMethodInsn(Opcodes.INVOKESTATIC, ACN_fieldType, 
                            "valueOf", "(" + getKeyReturnDesc + ")L" + ACN_fieldType + ";");

                        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                            getNamer().getObjectIdFieldConsumerAsmClassName(),
                            "storeObjectField", "(I" + EnhanceUtils.CD_Object + ")V");
                    }
                    else
                    {
                        // PK field isn't a primitive wrapper
                        visitor.visitVarInsn(Opcodes.ALOAD, 3);
                        visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, "getKey",
                            "()" + getNamer().getTypeDescriptorForSingleFieldIdentityGetKey(objectIdClass));
                        visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                            getNamer().getObjectIdFieldConsumerAsmClassName(),
                            "store" + getNamer().getTypeNameForUseWithSingleFieldIdentity(objectIdClass) + "Field",
                            "(I" + getNamer().getTypeDescriptorForSingleFieldIdentityGetKey(objectIdClass) + ")V");
                    }

                    visitor.visitInsn(Opcodes.RETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], getNamer().getObjectIdFieldConsumerDescriptor(), null, startLabel, endLabel, 1);
                    visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
                    visitor.visitLocalVariable("o", getNamer().getSingleFieldIdentityDescriptor(objectIdClass), null, l9, endLabel, 3);
                    visitor.visitMaxs(3, 4);
                }
                else
                {
                    // User-provided app identity, and compound identity
                    // Put try-catch around the field setting (for reflection cases)
                    Label l0 = new Label();
                    Label l1 = new Label();
                    Label l2 = new Label();
                    visitor.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");

                    Label startLabel = new Label();
                    visitor.visitLabel(startLabel);

                    visitor.visitVarInsn(Opcodes.ALOAD, 1);
                    Label l4 = new Label();
                    visitor.visitJumpInsn(Opcodes.IFNONNULL, l4);
                    visitor.visitTypeInsn(Opcodes.NEW, "java/lang/IllegalArgumentException");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("ObjectIdFieldConsumer is null");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException",
                        "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    visitor.visitLabel(l4);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitTypeInsn(Opcodes.INSTANCEOF, ACN_objectIdClass);
                    Label l5 = new Label();
                    visitor.visitJumpInsn(Opcodes.IFNE, l5);
                    visitor.visitTypeInsn(Opcodes.NEW, "java/lang/ClassCastException");
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("oid is not instanceof " + objectIdClass);
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/ClassCastException",
                        "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    visitor.visitLabel(l5);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, ACN_objectIdClass);
                    visitor.visitVarInsn(Opcodes.ASTORE, 3);

                    visitor.visitLabel(l0);

                    // Copy the PK members using the appropriate method for each field/property
                    Label reflectionFieldStart = null;
                    for (int i=0;i<pkFieldNums.length;i++)
                    {
                        AbstractMemberMetaData fmd = enhancer.getClassMetaData().getMetaDataForManagedMemberAtAbsolutePosition(pkFieldNums[i]);
                        String fieldTypeDesc = Type.getDescriptor(fmd.getType());
                        String typeMethodName = EnhanceUtils.getTypeNameForJDOMethod(fmd.getType());
                        int pkFieldModifiers = ClassUtils.getModifiersForFieldOfClass(enhancer.getClassLoaderResolver(), 
                            objectIdClass, fmd.getName());

                        // Check if the PK field type is a PC (CompoundIdentity)
                        AbstractClassMetaData acmd = enhancer.getMetaDataManager().getMetaDataForClass(
                            fmd.getType(), enhancer.getClassLoaderResolver());
                        if (acmd != null && acmd.getIdentityType() != IdentityType.NONDURABLE)
                        {
                            // CompoundIdentity, this field of the PK is a PC
                            visitor.visitVarInsn(Opcodes.ALOAD, 1);
                            EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                            visitor.visitVarInsn(Opcodes.ALOAD, 0);
                            visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, getClassEnhancer().getASMClassName(),
                                getNamer().getGetPersistenceManagerMethodName(), "()L" + getNamer().getPersistenceManagerAsmClassName() + ";");
                            visitor.visitVarInsn(Opcodes.ALOAD, 3);

                            // TODO Cater for property, or private field cases
                            visitor.visitFieldInsn(Opcodes.GETFIELD, ACN_objectIdClass,
                                fmd.getName(), "L" + acmd.getObjectidClass().replace('.', '/') + ";");

                            visitor.visitInsn(Opcodes.ICONST_0);
                            visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, getNamer().getPersistenceManagerAsmClassName(),
                                "getObjectById", "(Ljava/lang/Object;Z)Ljava/lang/Object;");
                            visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                getNamer().getObjectIdFieldConsumerAsmClassName(),
                                "storeObjectField", "(ILjava/lang/Object;)V");
                        }
                        else
                        {
                            // Standard application-identity
                            if (fmd instanceof PropertyMetaData)
                            {
                                // Field in PK is property, hence use getXXX in PK
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass,
                                    ClassUtils.getJavaBeanGetterName(fmd.getName(), fmd.getTypeName().equals("boolean")),
                                    "()" + Type.getDescriptor(fmd.getType()));
                                visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    getNamer().getObjectIdFieldConsumerAsmClassName(),
                                    "store" + typeMethodName + "Field",
                                    "(I" + EnhanceUtils.getTypeDescriptorForJDOMethod(fmd.getType()) + ")V");
                            }
                            else if (Modifier.isPublic(pkFieldModifiers))
                            {
                                // Field in PK is public so access directly
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitFieldInsn(Opcodes.GETFIELD, ACN_objectIdClass,
                                    fmd.getName(), fieldTypeDesc);
                                visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    getNamer().getObjectIdFieldConsumerAsmClassName(),
                                    "store" + typeMethodName + "Field",
                                    "(I" + EnhanceUtils.getTypeDescriptorForJDOMethod(fmd.getType()) + ")V");
                            }
                            else
                            {
                                // Field in PK is protected/private so use reflection, generating
                                // "Field field = o.getClass().getDeclaredField("pmIDFloat");"
                                // "field.setAccessible(true);"
                                // "fc.storeObjectField(1, field.get(o));"

                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", 
                                    "getClass", "()Ljava/lang/Class;");
                                visitor.visitLdcInsn(fmd.getName());
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", 
                                    "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
                                visitor.visitVarInsn(Opcodes.ASTORE, 4);
                                if (reflectionFieldStart == null)
                                {
                                    reflectionFieldStart = new Label();
                                    visitor.visitLabel(reflectionFieldStart);
                                }
                                visitor.visitVarInsn(Opcodes.ALOAD, 4);
                                visitor.visitInsn(Opcodes.ICONST_1);
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                    "setAccessible", "(Z)V");
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                                visitor.visitVarInsn(Opcodes.ALOAD, 4);
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                if (fmd.getTypeName().equals("boolean"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getBoolean", "(Ljava/lang/Object;)Z");
                                }
                                else if (fmd.getTypeName().equals("byte"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getByte", "(Ljava/lang/Object;)B");
                                }
                                else if (fmd.getTypeName().equals("char"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getChar", "(Ljava/lang/Object;)C");
                                }
                                else if (fmd.getTypeName().equals("double"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getDouble", "(Ljava/lang/Object;)D");
                                }
                                else if (fmd.getTypeName().equals("float"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getFloat", "(Ljava/lang/Object;)F");
                                }
                                else if (fmd.getTypeName().equals("int"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getInt", "(Ljava/lang/Object;)I");
                                }
                                else if (fmd.getTypeName().equals("long"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getLong", "(Ljava/lang/Object;)L");
                                }
                                else if (fmd.getTypeName().equals("short"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "getShort", "(Ljava/lang/Object;)S");
                                }
                                else if (fmd.getTypeName().equals("java.lang.String"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                    visitor.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
                                }
                                else
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
                                }
                                visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    getNamer().getObjectIdFieldConsumerAsmClassName(),
                                    "store" + typeMethodName + "Field",
                                    "(I" + EnhanceUtils.getTypeDescriptorForJDOMethod(fmd.getType()) + ")V");
                            }
                        }
                    }

                    // catch of the try-catch
                    visitor.visitLabel(l1);
                    Label l20 = new Label();
                    visitor.visitJumpInsn(Opcodes.GOTO, l20);
                    visitor.visitLabel(l2);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_FULL, 4, new Object[] {getClassEnhancer().getASMClassName(),
                                getClassEnhancer().getNamer().getObjectIdFieldConsumerAsmClassName(), 
                                "java/lang/Object", ACN_objectIdClass}, 1, new Object[] {"java/lang/Exception"});
                    }

                    visitor.visitVarInsn(Opcodes.ASTORE, 4);
                    visitor.visitLabel(l20);
                    if (JavaUtils.useStackMapFrames())
                    {
                        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
                    }

                    visitor.visitInsn(Opcodes.RETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], getNamer().getObjectIdFieldConsumerDescriptor(), null, startLabel, endLabel, 1);
                    visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
                    visitor.visitLocalVariable("o", "L" + ACN_objectIdClass + ";", null, l0, endLabel, 3);
                    if (reflectionFieldStart != null)
                    {
                        visitor.visitLocalVariable("field", "Ljava/lang/reflect/Field;", null, reflectionFieldStart, l2, 4);
                        visitor.visitMaxs(4, 5);
                    }
                    else
                    {
                        visitor.visitMaxs(4, 4);
                    }
                }
            }
        }
        else
        {
            // datastore/nondurable identity
            Label startLabel = new Label();
            visitor.visitLabel(startLabel);
            visitor.visitInsn(Opcodes.RETURN);
            Label endLabel = new Label();
            visitor.visitLabel(endLabel);
            visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
            visitor.visitLocalVariable(argNames[0], "L" + getNamer().getObjectIdFieldConsumerAsmClassName() + ";", null, 
                startLabel, endLabel, 1);
            visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
            visitor.visitMaxs(0, 3);
        }

        visitor.visitEnd();
    }
}
