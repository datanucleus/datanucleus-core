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
package org.datanucleus.enhancer;

import org.datanucleus.enhancer.asm.Label;
import org.datanucleus.enhancer.asm.MethodVisitor;
import org.datanucleus.enhancer.asm.Opcodes;
import org.datanucleus.enhancer.methods.InitClass;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ClassPersistenceModifier;
import org.datanucleus.metadata.FieldMetaData;
import org.datanucleus.metadata.FieldPersistenceModifier;
import org.datanucleus.util.Localiser;

/**
 * Adapter for methods in persistence-enabled classes allowing enhancement of direct access to user fields.
 * Currently performs the following updates
 * <ul>
 * <li>Any GETFIELD on a field of a Persistable class is replaced by a call to dnGetXXX()</li>
 * <li>Any PUTFIELD on a field of a Persistable class is replaced by a call to dnSetXXX()</li>
 * <li>Any clone() method that has no superclass but calls clone() is changed to call dnSuperClone()</li>
 * <li>Any static class initialisation adds on the "InitClass" instructions</li>
 * <li>Any user-provided "writeObject" method will have "dnPreSerialize" added before the user method code.
 * </ul>
 */
public class EnhancerMethodAdapter extends MethodVisitor
{
    /** The enhancer for this class. */
    protected ClassEnhancer enhancer;

    /** Name for the method being adapted. */
    protected String methodName;

    /** Descriptor for the method being adapted. */
    protected String methodDescriptor;

    /**
     * Constructor for the method adapter.
     * @param mv MethodVisitor
     * @param enhancer ClassEnhancer for the class with the method
     * @param methodName Name of the method
     * @param methodDesc descriptor for the method
     */
    public EnhancerMethodAdapter(MethodVisitor mv, ClassEnhancer enhancer, String methodName, String methodDesc)
    {
        super(ClassEnhancer.ASM_API_VERSION, mv);
        this.enhancer = enhancer;
        this.methodName = methodName;
        this.methodDescriptor = methodDesc;
    }

    boolean firstLabel = true;

    /* (non-Javadoc)
     * @see org.datanucleus.asm.MethodVisitor#visitLabel(org.datanucleus.asm.Label)
     */
    @Override
    public void visitLabel(Label label)
    {
        super.visitLabel(label);

        if (firstLabel)
        {
            if (methodName.equals("writeObject"))
            {
                // User has provided a "writeObject" method so enhance it by adding "dnPreSerialize" before user code (so after the first "label")

                // "dnPreSerialize();"
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, enhancer.getASMClassName(), enhancer.getNamer().getPreSerializeMethodName(), "()V", false);

                if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                {
                    DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005033", enhancer.getClassName() + "." + methodName));
                }
            }
        }
        firstLabel = false;
    }

    /**
     * Method to intercept any calls to fields.
     * @param opcode Operation
     * @param owner Owner class
     * @param name Name of the field
     * @param desc Descriptor for the field
     */
    public void visitFieldInsn(int opcode, String owner, String name, String desc)
    {
        String ownerName = owner.replace('/', '.');
        if (enhancer.isPersistable(ownerName))
        {
            AbstractClassMetaData cmd = null;
            boolean fieldInThisClass = true;
            if (enhancer.getClassMetaData().getFullClassName().equals(ownerName))
            {
                // Same class so use the input MetaData
                cmd = enhancer.getClassMetaData();
            }
            else
            {
                fieldInThisClass = false;
                cmd = enhancer.getMetaDataManager().getMetaDataForClass(ownerName, enhancer.getClassLoaderResolver());
            }

            // If the field access is in this class and this is the constructor then don't enhance it.
            // This is because this object is not connected to a StateManager nor is it detached.
            // Also languages like Scala don't necessarily initialise superclasses first and so
            // enhancing here would cause issues
            if (!fieldInThisClass || !(methodName.equals("<init>")))
            {
                AbstractMemberMetaData fmd = cmd.getMetaDataForMember(name);
                if (fmd != null && !fmd.isStatic() && !fmd.isFinal() &&
                    fmd.getPersistenceModifier() != FieldPersistenceModifier.NONE &&
                    fmd.getPersistenceFlags() != 0 && fmd instanceof FieldMetaData)
                {
                    // Field being accessed has its access mediated by the enhancer, so intercept it
                    // Make sure we address the field being in the class it is actually in
                    String fieldOwner = fmd.getClassName(true).replace('.', '/');
                    if (opcode == Opcodes.GETFIELD)
                    {
                        // Read of a field of a PC class, so replace with dnGetXXX() call
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, fieldOwner, enhancer.getNamer().getGetMethodPrefixMethodName() + name, "(L" + fieldOwner + ";)" + desc);
                        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                        {
                            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005023",
                                enhancer.getClassName() + "." + methodName, fmd.getClassName(true) + "." + name, 
                                enhancer.getNamer().getGetMethodPrefixMethodName() + name + "()"));
                        }
                        return;
                    }
                    else if (opcode == Opcodes.PUTFIELD)
                    {
                        // Write of a field of a PC class, so replace with dnSetXXX() call
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, fieldOwner, enhancer.getNamer().getSetMethodPrefixMethodName() + name, "(L" + fieldOwner + ";" + desc + ")V");
                        if (DataNucleusEnhancer.LOGGER.isDebugEnabled())
                        {
                            DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005023",
                                enhancer.getClassName() + "." + methodName, fmd.getClassName(true) + "." + name,
                                enhancer.getNamer().getSetMethodPrefixMethodName() + name + "()"));
                        }
                        return;
                    }
                }
            }
            else
            {
                DataNucleusEnhancer.LOGGER.debug(Localiser.msg("005024",
                    enhancer.getClassName() + "." + methodName, opcode == Opcodes.GETFIELD ? "get" : "set", ownerName + "." + name));
            }
        }

        super.visitFieldInsn(opcode, owner, name, desc);
    }

    /**
     * Method to intercept any general instructions.
     * We use it to intercept any RETURN on a static initialisation block so we can append to it.
     * @param opcode Operation
     */
    public void visitInsn(int opcode)
    {
        if (enhancer.getClassMetaData().getPersistenceModifier() == ClassPersistenceModifier.PERSISTENCE_CAPABLE &&
            methodName.equals("<clinit>") && methodDescriptor.equals("()V") && opcode == Opcodes.RETURN)
        {
            // Add the initialise instructions to the existing block
            InitClass initMethod = InitClass.getInstance(enhancer);
            initMethod.addInitialiseInstructions(mv);

            // Add the RETURN
            mv.visitInsn(Opcodes.RETURN);
            return;
        }

        super.visitInsn(opcode);
    }
}
