/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.datanucleus.enhancer.asm.ClassWriter;
import org.datanucleus.enhancer.asm.FieldVisitor;
import org.datanucleus.enhancer.asm.Label;
import org.datanucleus.enhancer.asm.MethodVisitor;
import org.datanucleus.enhancer.asm.Opcodes;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.IdentityType;
import org.datanucleus.util.Localiser;

/**
 * Class to handle the generation of a PK class for a persistable class.
 * The primary key class is generated as its own class, rather than as an inner class of the original class.
 * NOTE: This will only currently handle persistable fields. If you require persistable properties then get the code and extend this.
 */
public class PrimaryKeyGenerator
{
    /** Metadata for the class that needs a primary key class. */
    final AbstractClassMetaData cmd;

    /** Name of the primary key class ("mydomain.MyClass_PK"). */
    final String pkClassName;

    /** ASM name of the PK class ("mydomain/MyClass_PK"). */
    final String className_ASM;

    /** ASM type descriptor name of the PK class ("Lmydomain/MyClass_PK;"). */
    final String className_DescName;

    /** The enhancer being used. */
    final ClassEnhancer classEnhancer;

    String stringSeparator = ":";

    /**
     * Constructor for a PK generator for the specified class.
     * @param cmd Metadata for the class that needs a primary key class
     * @param enhancer The enhancer being used
     */
    public PrimaryKeyGenerator(AbstractClassMetaData cmd, ClassEnhancer enhancer)
    {
        this.cmd = cmd;
        this.classEnhancer = enhancer;
        pkClassName = cmd.getFullClassName() + AbstractClassMetaData.GENERATED_PK_SUFFIX;
        className_ASM = pkClassName.replace('.', '/');
        className_DescName = "L" + className_ASM + ";";
        // TODO If this is compound identity then we need to change the separator to be different to the
        // related class so it is possible to separate them
    }

    /**
     * Method to generate the primary key class.
     * @return The bytes for this pk class
     */
    public byte[] generate()
    {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

        // TODO Parameterise the first argument to allow any JDK
        cw.visit(EnhanceUtils.getAsmVersionForJRE(), Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className_ASM, null, "java/lang/Object", new String[] {"java/io/Serializable"});

        // Add fields
        addFields(cw);

        // Add constructors
        addDefaultConstructor(cw);
        addStringConstructor(cw);

        // Add methods
        addMethodToString(cw);
        addMethodEquals(cw);
        addMethodHashCode(cw);

        cw.visitEnd();
        return cw.toByteArray();
    }

    /**
     * Method to add fields to match the PK fields of the persistable class
     * @param cw The ClassWriter to use
     */
    protected void addFields(ClassWriter cw)
    {
        int[] pkPositions = cmd.getPKMemberPositions();
        for (int i=0;i<pkPositions.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtRelativePosition(pkPositions[i]);
            String fieldTypeName = getTypeNameForField(mmd);
            if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
            {
                DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005021", fieldTypeName + " " + pkClassName + " " + mmd.getName()));
            }

            FieldVisitor fv = cw.visitField(Opcodes.ACC_PUBLIC, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(fieldTypeName), null, null);
            fv.visitEnd();
        }
    }

    protected String getTypeNameForField(AbstractMemberMetaData mmd)
    {
        AbstractClassMetaData fieldCmd = classEnhancer.getMetaDataManager().getMetaDataForClass(mmd.getType(), classEnhancer.getClassLoaderResolver());
        String fieldTypeName = mmd.getTypeName();
        if (fieldCmd != null && fieldCmd.getIdentityType() == IdentityType.APPLICATION)
        {
            fieldTypeName = fieldCmd.getObjectidClass();
        }
        return fieldTypeName;
    }

    /**
     * Method to add an empty default constructor.
     * @param cw The ClassWriter to use
     */
    protected void addDefaultConstructor(ClassWriter cw)
    {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005020", pkClassName + "()"));
        }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();

        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        mv.visitInsn(Opcodes.RETURN);

        Label endLabel = new Label();
        mv.visitLabel(endLabel);

        mv.visitLocalVariable("this", className_DescName, null, startLabel, endLabel, 0);
        mv.visitMaxs(1, 1);

        mv.visitEnd();
    }

    /**
     * Method to add a constructor taking in a String.
     * @param cw The ClassWriter to use
     */
    protected void addStringConstructor(ClassWriter cw)
    {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005020", pkClassName + "(String str)"));
        }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/String;)V", null, null);
        mv.visitCode();

        int[] pkPositions = cmd.getPKMemberPositions();
        Label[] fieldLabels = new Label[pkPositions.length];

        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        // Invoke default constructor of superclass (Object)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

        // StringTokenizer tokeniser = new StringTokenizer(str, {stringSeparator});
        mv.visitTypeInsn(Opcodes.NEW, "java/util/StringTokenizer");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn(stringSeparator);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/StringTokenizer", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        Label l5 = new Label();
        mv.visitLabel(l5);

        // Get the next token and set the respective field from it
        int astorePosition = 2;
        for (int i=0;i<pkPositions.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtRelativePosition(pkPositions[i]);
            String typeName_ASM = mmd.getTypeName().replace('.', '/');
            astorePosition++;

            // String tokenX = tokeniser.nextToken();
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/StringTokenizer", "nextToken", "()Ljava/lang/String;");
            mv.visitVarInsn(Opcodes.ASTORE, astorePosition);

            fieldLabels[i] = new Label();
            mv.visitLabel(fieldLabels[i]);
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            if (mmd.getType() == long.class || mmd.getType() == int.class || mmd.getType() == short.class)
            {
                // Uses the following pattern (e.g for Integer)
                // fieldX = Integer.valueOf(tokenX).intValue();
                String type_desc = EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName());
                String wrapperClassName_ASM = "java/lang/Long";
                String wrapperConverterMethod = "longValue";
                if (mmd.getType() == int.class)
                {
                    wrapperClassName_ASM = "java/lang/Integer";
                    wrapperConverterMethod = "intValue";
                }
                else if (mmd.getType() == short.class)
                {
                    wrapperClassName_ASM = "java/lang/Short";
                    wrapperConverterMethod = "shortValue";
                }

                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, wrapperClassName_ASM, "valueOf", "(Ljava/lang/String;)L" + wrapperClassName_ASM + ";");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapperClassName_ASM, wrapperConverterMethod, "()" + type_desc);
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), type_desc);
            }
            else if (mmd.getType() == char.class)
            {
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, "field1", "C");
            }
            else if (mmd.getType() == String.class)
            {
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));
            }
            else if (mmd.getType() == Long.class || mmd.getType() == Integer.class || 
                    mmd.getType() == Short.class || mmd.getType() == BigInteger.class)
            {
                // Uses the following pattern (e.g for Long)
                // fieldX = Long.valueOf(tokenX);
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, typeName_ASM, "valueOf", "(Ljava/lang/String;)L" + typeName_ASM + ";");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), "L" + typeName_ASM + ";");
            }
            else if (mmd.getType() == Currency.class)
            {
                // "fieldX = TypeX.newInstance(tokenX)"
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Currency", "getInstance", "(Ljava/lang/String;)Ljava/util/Currency;");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), "Ljava/util/Currency;");
            }
            else if (mmd.getType() == TimeZone.class)
            {
                // "fieldX = TimeZone.getTimeZone(tokenX)"
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/TimeZone", "getTimeZone", "(Ljava/lang/String;)Ljava/util/TimeZone;");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), "Ljava/util/TimeZone;");
            }
            else if (mmd.getType() == UUID.class)
            {
                // "fieldX = UUID.fromString(tokenX)"
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/UUID", "fromString", "(Ljava/lang/String;)Ljava/util/UUID;");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), "Ljava/util/UUID;");
            }
            else if (Date.class.isAssignableFrom(mmd.getType()))
            {
                // fieldX = new Date(new Long(tokenX).longValue());
                mv.visitTypeInsn(Opcodes.NEW, typeName_ASM);
                mv.visitInsn(Opcodes.DUP);
                mv.visitTypeInsn(Opcodes.NEW, "java/lang/Long");
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Long", "<init>", "(Ljava/lang/String;)V");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, typeName_ASM, "<init>", "(J)V");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), 
                    EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));
            }
            else if (Calendar.class.isAssignableFrom(mmd.getType()))
            {
                // fieldX = Calendar.getInstance();
                // fieldX.setTimeInMillis(Long.valueOf(tokenX).longValue());
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/util/Calendar", "getInstance", "()Ljava/util/Calendar;");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), "Ljava/util/Calendar;");
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), "Ljava/util/Calendar;");
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(Ljava/lang/String;)Ljava/lang/Long;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Calendar", "setTimeInMillis", "(J)V");
            }
            else
            {
                // "fieldX = new TypeX(tokenX)"
                // TODO If this is compound identity and the related object uses SingleFieldIdentity then
                // we need to use a different constructor with this value
                String fieldTypeName = getTypeNameForField(mmd);
                String fieldTypeName_ASM = fieldTypeName.replace('.', '/');
                mv.visitTypeInsn(Opcodes.NEW, fieldTypeName_ASM);
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, astorePosition);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, fieldTypeName_ASM, "<init>", "(Ljava/lang/String;)V");
                mv.visitFieldInsn(Opcodes.PUTFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(fieldTypeName));
            }
        }

        mv.visitInsn(Opcodes.RETURN);

        Label endLabel = new Label();
        mv.visitLabel(endLabel);

        int variableNum = 0;
        mv.visitLocalVariable("this", className_DescName, null, startLabel, endLabel, variableNum++);
        mv.visitLocalVariable("str", "Ljava/lang/String;", null, startLabel, endLabel, variableNum++);
        mv.visitLocalVariable("tokeniser", "Ljava/util/StringTokenizer;", null, l5, endLabel, variableNum++);

        for (int i=0;i<pkPositions.length;i++)
        {
            mv.visitLocalVariable("token" + i, "Ljava/lang/String;", null, fieldLabels[i], endLabel, variableNum++);
        }

        mv.visitMaxs(6, variableNum);

        mv.visitEnd();
    }

    /**
     * Method to add a toString() method.
     * @param cw The ClassWriter to use
     */
    protected void addMethodToString(ClassWriter cw)
    {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005019", "String " + pkClassName + ".toString()"));
        }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        // "StringBuilder str = new StringBuilder();"
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
        mv.visitVarInsn(Opcodes.ASTORE, 1);
        Label l1 = new Label();
        mv.visitLabel(l1);

        // "str.append(field1).append(":").append(field2) ..." etc
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        int[] pkPositions = cmd.getPKMemberPositions();
        for (int i=0;i<pkPositions.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtRelativePosition(pkPositions[i]);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(),
                EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));

            // Use most representive form of "StringBuilder.append()"
            if (mmd.getType() == int.class || mmd.getType() == long.class ||
                mmd.getType() == float.class || mmd.getType() == double.class)
            {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", 
                    "(" + EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()) + ")Ljava/lang/StringBuilder;");
            }
            else if (mmd.getType() == char.class)
            {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(" + EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()) + ")Ljava/lang/StringBuilder;");
            }
            else if (mmd.getType() == String.class)
            {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            }
            else if (Date.class.isAssignableFrom(mmd.getType()))
            {
                // Use the long value of the date (millisecs)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mmd.getTypeName().replace('.', '/'), "getTime", "()J");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(J)Ljava/lang/StringBuilder;");
            }
            else if (Calendar.class.isAssignableFrom(mmd.getType()))
            {
                // Use the long value of the Calendar (millisecs)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Calendar", "getTime", "()Ljava/util/Date;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/Date", "getTime", "()J");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(J)Ljava/lang/StringBuilder;");
            }
            else if (mmd.getType() == TimeZone.class)
            {
                // Use the ID of the TimeZone
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/TimeZone", "getID", "()Ljava/lang/String;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            }
            else
            {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mmd.getTypeName().replace('.', '/'), "toString", "()Ljava/lang/String;");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            }

            if (i < (pkPositions.length-1))
            {
                // Add separator ({stringSeparator})
                mv.visitLdcInsn(stringSeparator);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
            }
        }
        mv.visitInsn(Opcodes.POP);

        // "return str.toString();"
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        mv.visitInsn(Opcodes.ARETURN);

        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", className_DescName, null, startLabel, endLabel, 0);
        mv.visitLocalVariable("str", "Ljava/lang/StringBuilder;", null, l1, endLabel, 1);
        mv.visitMaxs(pkPositions.length, 2);
        mv.visitEnd();
    }

    /**
     * Method to add an equals() method.
     * @param cw The ClassWriter to use
     */
    protected void addMethodEquals(ClassWriter cw)
    {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005019", "boolean " + pkClassName + ".equals(Object obj)"));
        }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
        mv.visitCode();

        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        // if (obj == this) {return true;}
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IF_ACMPNE, l1);
        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(l1);

        // if (!(obj instanceof ThePK)) {return false;}
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.INSTANCEOF, className_ASM);
        Label l3 = new Label();
        mv.visitJumpInsn(Opcodes.IFNE, l3);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(l3);

        // ThePK other = (ThePK)obj;
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, className_ASM);
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        Label compareStartLabel = new Label();
        mv.visitLabel(compareStartLabel);

        int[] pkPositions = cmd.getPKMemberPositions();
        Label compareSepLabel = null;
        for (int i=0;i<pkPositions.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtRelativePosition(pkPositions[i]);
            if (mmd.getType() == long.class)
            {
                // "fieldX == other.fieldX"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));

                mv.visitInsn(Opcodes.LCMP);
                if (i == 0)
                {
                    compareSepLabel = new Label();
                }
                mv.visitJumpInsn(Opcodes.IFNE, compareSepLabel);
            }
            else if (mmd.getType() == int.class || mmd.getType() == short.class || mmd.getType() == char.class)
            {
                // "fieldX == other.fieldX"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));

                if (i == 0)
                {
                    compareSepLabel = new Label();
                }
                mv.visitJumpInsn(Opcodes.IF_ICMPNE, compareSepLabel);
            }
            else
            {
                // "fieldX.equals(other.fieldX)"
                String typeName = getTypeNameForField(mmd);
                String typeName_ASM = typeName.replace('.', '/');
                String typeNameDesc = "L" + typeName_ASM + ";";
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), typeNameDesc);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), typeNameDesc);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, typeName_ASM, "equals", "(Ljava/lang/Object;)Z");

                if (i == 0)
                {
                    compareSepLabel = new Label();
                }
                mv.visitJumpInsn(Opcodes.IFEQ, compareSepLabel);
            }
        }

        mv.visitInsn(Opcodes.ICONST_1);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(compareSepLabel);
        mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {className_ASM}, 0, null);

        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        Label endLabel = new Label();
        mv.visitLabel(endLabel);
        mv.visitLocalVariable("this", className_DescName, null, startLabel, endLabel, 0);
        mv.visitLocalVariable("obj", "Ljava/lang/Object;", null, startLabel, endLabel, 1);
        mv.visitLocalVariable("other", className_DescName, null, compareStartLabel, endLabel, 2);
        mv.visitMaxs(4, 3); // TODO Can be (2, 3) in some situations, e.g if char is one of PK fields?
        mv.visitEnd();
    }

    /**
     * Method to add a hashCode() method.
     * @param cw The ClassWriter to use
     */
    protected void addMethodHashCode(ClassWriter cw)
    {
        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
        {
            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005019", "int " + pkClassName + ".hashCode()"));
        }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "hashCode", "()I", null, null);
        mv.visitCode();

        Label startLabel = new Label();
        mv.visitLabel(startLabel);

        int[] pkPositions = cmd.getPKMemberPositions();
        for (int i=0;i<pkPositions.length;i++)
        {
            AbstractMemberMetaData mmd = cmd.getMetaDataForManagedMemberAtRelativePosition(pkPositions[i]);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitFieldInsn(Opcodes.GETFIELD, className_ASM, mmd.getName(), EnhanceUtils.getTypeDescriptorForType(mmd.getTypeName()));
            if (mmd.getType() == long.class)
            {
                // "(int)fieldX"
                mv.visitInsn(Opcodes.L2I);
            }
            else if (mmd.getType() == int.class || mmd.getType() == short.class || mmd.getType() == char.class)
            {
                // "fieldX"
            }
            else
            {
                // "fieldX.hashCode"
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, mmd.getTypeName().replace('.', '/'), "hashCode", "()I");
            }

            if (i > 0)
            {
                // "^"
                mv.visitInsn(Opcodes.IXOR);
            }
        }

        mv.visitInsn(Opcodes.IRETURN);

        Label endLabel = new Label();
        mv.visitLabel(endLabel);

        mv.visitLocalVariable("this", className_DescName, null, startLabel, endLabel, 0);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }
}