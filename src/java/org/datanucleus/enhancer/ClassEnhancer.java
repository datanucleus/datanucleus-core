/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.asm.Opcodes;
import org.datanucleus.metadata.ClassMetaData;
import org.datanucleus.metadata.MetaDataManager;

/**
 * Interface representation of an enhancer of a class.
 */
public interface ClassEnhancer
{
    /** Version of the ASM API to use (introduced in ASM v4 to aid backward compatibility). */
    public static final int ASM_API_VERSION = Opcodes.ASM5;

    /** Option for generating the default constructor. */
    public static final String OPTION_GENERATE_DEFAULT_CONSTRUCTOR = "generate-default-constructor";

    /** Option for generating the default constructor. */
    public static final String OPTION_GENERATE_PK = "generate-primary-key";
    
    /** Option for use the detach listener. */
    public static final String OPTION_GENERATE_DETACH_LISTENER = "generate-detach-listener";

    /**
     * Method to set the options controlling the enhancement.
     * @param options The options
     */
    void setOptions(Collection<String> options);

    /**
     * Accessor for whether a particular option is enabled.
     * @param name Name of the option
     * @return Whether it has this option
     */
    boolean hasOption(String name);

    /**
     * Validate whether the class is enhanced.
     * @return Return true if already enhanced class.
     */
    boolean validate();

    /**
     * Method to enhance the class definition internally.
     * @return Whether the class was enhanced successfully
     */
    boolean enhance();

    /**
     * Method to save the (current) class definition bytecode into a class file.
     * Only has effect if the bytecode has been modified (by enhance()).
     * If directoryName is specified it will be written to $directoryName/className.class
     * else will overwrite the existing class.
     * @param directoryName Name of a directory (or null to overwrite the class)
     * @throws IOException If an I/O error occurs in the write.
     */
    void save(String directoryName) throws IOException;

    /**
     * Access the class bytecode.
     * @return the class in byte array format
     */
    byte[] getClassBytes();

    /**
     * Access the generated primary-key class bytecode.
     * @return the primary-key class in byte array format
     */
    byte[] getPrimaryKeyClassBytes();

    /**
     * Accessor for the MetaData manager in use.
     * @return MetaData manager
     */
    MetaDataManager getMetaDataManager();

    /**
     * Accessor for the ClassLoaderResolver in use.
     * @return ClassLoader resolver
     */
    ClassLoaderResolver getClassLoaderResolver();

    /**
     * Accessor for the ClassMetaData for the class.
     * @return MetaData for the class
     */
    ClassMetaData getClassMetaData();

    void setNamer(EnhancementNamer namer);

    EnhancementNamer getNamer();

    /**
     * Accessor for the class being enhanced.
     * @return Class being enhanced
     */
    Class getClassBeingEnhanced();

    /**
     * Accessor for the name of the class being enhanced.
     * @return Class name
     */
    String getClassName();

    /**
     * Accessor for the ASM class name for the class being enhanced.
     * @return ASM class name
     */
    public String getASMClassName();

    /**
     * Accessor for the class descriptor for the class being enhanced
     * @return class descriptor
     */
    public String getClassDescriptor();

    /**
     * Accessor for the methods required.
     * @return List of methods required for enhancement
     */
    List<ClassMethod> getMethodsList();

    /**
     * Accessor for the fields required.
     * @return List of fields required for enhancement
     */
    List<ClassField> getFieldsList();

    /**
     * Check if the class is Persistable or is going to be enhanced based on the metadata
     * @param className the class name
     * @return true if Persistable
     */
    public boolean isPersistable(String className);
}