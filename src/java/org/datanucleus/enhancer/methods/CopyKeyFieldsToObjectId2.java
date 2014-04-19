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
package org.datanucleus.enhancer.methods;

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

/**
 * Method to generate the method "jdoCopyKeyFieldsToObjectId" using ASM.
 * For datastore/nondurable identity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(PersistenceCapable.ObjectIdFieldSupplier objectidfieldsupplier,
 *        Object object)
 * {
 * }
 * </pre>
 * and for SingleFieldIdentity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(PersistenceCapable.ObjectIdFieldSupplier fs, Object oid)
 * {
 *     throw new JDOFatalInternalException("It's illegal to call ...");
 * }
 * </pre>
 * and for user-supplied app identity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(PersistenceCapable.ObjectIdFieldSupplier fs, Object oid)
 * {
 *     if (fs == null)
 *         throw new IllegalArgumentException("ObjectIdFieldSupplier is null");
 *     if (oid instanceof UserPrimaryKey != true)
 *         throw new ClassCastException("oid is not instanceof mydomain.UserPrimaryKey");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     o.zzz1 = fs.fetchYYY1Field(1);
 *     o.zzz2 = fs.fetchYYY2Field(2);
 * }
 * </pre>
 * and for CompoundIdentity
 * <pre>
 * public void jdoCopyKeyFieldsToObjectId(PersistenceCapable.ObjectIdFieldSupplier fs, Object oid)
 * {
 *     if (fs == null)
 *         throw new IllegalArgumentException("ObjectIdFieldSupplier is null");
 *     if (oid instanceof UserPrimaryKey != true)
 *         throw new ClassCastException("oid is not instanceof mydomain.UserPrimaryKey");
 *     UserPrimaryKey o = (UserPrimaryKey) oid;
 *     o.zzz1 = fs.fetchYYYField(1);
 *     o.zzz2 = ((YYY2.Key)JDOHelper.getObjectId((YYY2)fs.fetchObjectField(2)));
 * }
 * </pre>
 */
public class CopyKeyFieldsToObjectId2 extends ClassMethod
{
    public static CopyKeyFieldsToObjectId2 getInstance(ClassEnhancer enhancer)
    {
        return new CopyKeyFieldsToObjectId2(enhancer, enhancer.getNamer().getCopyKeyFieldsToObjectIdMethodName(),
            Opcodes.ACC_PUBLIC/* | Opcodes.ACC_FINAL*/,
            null, new Class[] {enhancer.getNamer().getObjectIdFieldSupplierClass(), Object.class}, new String[] { "fs", "oid" });
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
    public CopyKeyFieldsToObjectId2(ClassEnhancer enhancer, String name, int access, 
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
                visitor.visitLocalVariable(argNames[0], getNamer().getObjectIdFieldSupplierDescriptor(), null, startLabel, endLabel, 1);
                visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
                visitor.visitMaxs(0, 3);
            }
            else
            {
                String objectIdClass = cmd.getObjectidClass();
                String ACN_objectIdClass = objectIdClass.replace('.', '/');
                if (enhancer.getMetaDataManager().getApiAdapter().isSingleFieldIdentityClass(objectIdClass))
                {
                    // SingleFieldIdentity
                    Label startLabel = new Label();
                    visitor.visitLabel(startLabel);

                    visitor.visitTypeInsn(Opcodes.NEW, getNamer().getFatalInternalExceptionAsmClassName());
                    visitor.visitInsn(Opcodes.DUP);
                    visitor.visitLdcInsn("It's illegal to call jdoCopyKeyFieldsToObjectId for a class with SingleFieldIdentity.");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, getNamer().getFatalInternalExceptionAsmClassName(), "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], EnhanceUtils.CD_Object, null, startLabel, endLabel, 1);
                    visitor.visitLocalVariable("paramObject", "Ljava/lang/Object;", null, startLabel, endLabel, 2);
                    visitor.visitMaxs(3, 3);
                }
                else
                {
                    // User-provided app identity, and compound identity
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
                    visitor.visitLdcInsn("ObjectIdFieldSupplier is null");
                    visitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/IllegalArgumentException",
                        "<init>", "(Ljava/lang/String;)V");
                    visitor.visitInsn(Opcodes.ATHROW);

                    visitor.visitLabel(l4);
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

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
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    visitor.visitVarInsn(Opcodes.ALOAD, 2);
                    visitor.visitTypeInsn(Opcodes.CHECKCAST, ACN_objectIdClass);
                    visitor.visitVarInsn(Opcodes.ASTORE, 3);

                    visitor.visitLabel(l0);

                    // Copy the PK members using the appropriate method for each field/property
                    int[] pkFieldNums = enhancer.getClassMetaData().getPKMemberPositions();
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
                            visitor.visitVarInsn(Opcodes.ALOAD, 3);
                            visitor.visitVarInsn(Opcodes.ALOAD, 1);
                            EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                            visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                getNamer().getObjectIdFieldSupplierAsmClassName(),
                                "fetch" + typeMethodName + "Field", "(I)" + EnhanceUtils.getTypeDescriptorForJDOMethod(fmd.getType()));
                            if (typeMethodName.equals("Object"))
                            {
                                visitor.visitTypeInsn(Opcodes.CHECKCAST, fmd.getTypeName().replace('.', '/'));
                            }

                            visitor.visitMethodInsn(Opcodes.INVOKESTATIC, getNamer().getHelperAsmClassName(),
                                "getObjectId", "(Ljava/lang/Object;)Ljava/lang/Object;");
                            visitor.visitTypeInsn(Opcodes.CHECKCAST, acmd.getObjectidClass().replace('.', '/'));
                            // TODO Cater for property, and private field cases
                            visitor.visitFieldInsn(Opcodes.PUTFIELD, ACN_objectIdClass,
                                fmd.getName(), "L" + acmd.getObjectidClass().replace('.', '/') + ";");
                        }
                        else
                        {
                            // Standard application-identity
                            if (fmd instanceof PropertyMetaData)
                            {
                                // Field in PK is property, hence use setXXX in PK
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                                visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    getNamer().getObjectIdFieldSupplierAsmClassName(),
                                    "fetch" + typeMethodName + "Field", "(I)" + EnhanceUtils.getTypeDescriptorForJDOMethod(fmd.getType()));
                                if (typeMethodName.equals("Object"))
                                {
                                    visitor.visitTypeInsn(Opcodes.CHECKCAST, fmd.getTypeName().replace('.', '/'));
                                }
                                visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ACN_objectIdClass, 
                                    ClassUtils.getJavaBeanSetterName(fmd.getName()), "(" + fieldTypeDesc + ")V");
                            }
                            else if (Modifier.isPublic(pkFieldModifiers))
                            {
                                // Field in PK is public so update directly
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                                visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                    getNamer().getObjectIdFieldSupplierAsmClassName(),
                                    "fetch" + typeMethodName + "Field", "(I)" + EnhanceUtils.getTypeDescriptorForJDOMethod(fmd.getType()));
                                if (typeMethodName.equals("Object"))
                                {
                                    visitor.visitTypeInsn(Opcodes.CHECKCAST, fmd.getTypeName().replace('.', '/'));
                                }
                                visitor.visitFieldInsn(Opcodes.PUTFIELD, ACN_objectIdClass,
                                    fmd.getName(), fieldTypeDesc);
                            }
                            else
                            {
                                // Field in PK is protected/private so use reflection, generating
                                // "Field field = o.getClass().getDeclaredField("pmIDFloat");"
                                // "field.setAccessible(true);"
                                // "field.set(o, fs.fetchObjectField(1));"

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
                                visitor.visitVarInsn(Opcodes.ALOAD, 4);
                                visitor.visitVarInsn(Opcodes.ALOAD, 3);
                                visitor.visitVarInsn(Opcodes.ALOAD, 1);
                                EnhanceUtils.addBIPUSHToMethod(visitor, fmd.getFieldId());
                                if (fmd.getTypeName().equals("boolean"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, 
                                        getNamer().getObjectIdFieldSupplierAsmClassName(), 
                                        "fetchBooleanField", "(I)Z");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setBoolean", "(Ljava/lang/Object;Z)V");
                                }
                                else if (fmd.getTypeName().equals("byte"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                        getNamer().getObjectIdFieldSupplierAsmClassName(),
                                        "fetchByteField", "(I)B");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setByte", "(Ljava/lang/Object;B)V");
                                }
                                else if (fmd.getTypeName().equals("char"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, 
                                        getNamer().getObjectIdFieldSupplierAsmClassName(), 
                                        "fetchCharField", "(I)C");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setChar", "(Ljava/lang/Object;C)V");
                                }
                                else if (fmd.getTypeName().equals("double"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, 
                                        getNamer().getObjectIdFieldSupplierAsmClassName(), 
                                        "fetchDoubleField", "(I)D");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setDouble", "(Ljava/lang/Object;D)V");
                                }
                                else if (fmd.getTypeName().equals("float"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, 
                                        getNamer().getObjectIdFieldSupplierAsmClassName(),
                                        "fetchFloatField", "(I)F");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setFloat", "(Ljava/lang/Object;F)V");
                                }
                                else if (fmd.getTypeName().equals("int"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, 
                                        getNamer().getObjectIdFieldSupplierAsmClassName(), 
                                        "fetchIntField", "(I)I");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", 
                                        "setInt", "(Ljava/lang/Object;I)V");
                                }
                                else if (fmd.getTypeName().equals("long"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, 
                                        getNamer().getObjectIdFieldSupplierAsmClassName(),
                                        "fetchLongField", "(I)J");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "setLong", "(Ljava/lang/Object;J)V");
                                }
                                else if (fmd.getTypeName().equals("short"))
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                        getNamer().getObjectIdFieldSupplierAsmClassName(),
                                        "fetchShortField", "(I)S");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "setShort", "(Ljava/lang/Object;S)V");
                                }
                                else
                                {
                                    visitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                        getNamer().getObjectIdFieldSupplierAsmClassName(),
                                        "fetchObjectField", "(I)Ljava/lang/Object;");
                                    visitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field",
                                        "set", "(Ljava/lang/Object;Ljava/lang/Object;)V");
                                }
                            }
                        }
                    }

                    visitor.visitLabel(l1);
                    Label l20 = new Label();
                    visitor.visitJumpInsn(Opcodes.GOTO, l20);
                    visitor.visitLabel(l2);
                    visitor.visitFrame(Opcodes.F_FULL, 4, 
                        new Object[] {getClassEnhancer().getASMClassName(), 
                            getClassEnhancer().getNamer().getObjectIdFieldSupplierAsmClassName(),
                            "java/lang/Object", ACN_objectIdClass}, 1, new Object[] {"java/lang/Exception"});

                    visitor.visitVarInsn(Opcodes.ASTORE, 4);
                    visitor.visitLabel(l20);
                    visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

                    visitor.visitInsn(Opcodes.RETURN);

                    Label endLabel = new Label();
                    visitor.visitLabel(endLabel);
                    visitor.visitLocalVariable("this", getClassEnhancer().getClassDescriptor(), null, startLabel, endLabel, 0);
                    visitor.visitLocalVariable(argNames[0], getNamer().getObjectIdFieldSupplierDescriptor(), null, startLabel, endLabel, 1);
                    visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
                    visitor.visitLocalVariable("o", "L" + ACN_objectIdClass + ";", null, l0, endLabel, 3);
                    if (reflectionFieldStart != null)
                    {
                        visitor.visitLocalVariable("field", "Ljava/lang/reflect/Field;", null, reflectionFieldStart, l2, 4);
                        visitor.visitMaxs(4, 5);
                    }
                    else
                    {
                        visitor.visitMaxs(3, 4);
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
            visitor.visitLocalVariable(argNames[0], "L" + getNamer().getObjectIdFieldSupplierAsmClassName() + ";", null, startLabel, endLabel, 1);
            visitor.visitLocalVariable(argNames[1], EnhanceUtils.CD_Object, null, startLabel, endLabel, 2);
            visitor.visitMaxs(0, 3);
        }

        visitor.visitEnd();
    }
}
