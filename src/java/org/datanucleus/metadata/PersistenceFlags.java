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
package org.datanucleus.metadata;

/**
 * Series of flag settings used in the persistence process.
 * Copied from javax.jdo.spi.PersistenceCapable for when we don't need jdo.jar present.
 */
public class PersistenceFlags
{
    /**
     * If StateManager persistenceFlags is set to READ_WRITE_OK, then the fields in the default fetch group
     * can be accessed for read or write without notifying the StateManager.
     */
    public static final byte READ_WRITE_OK = 0;

    /**
     * If StateManager persistenceFlags is set to LOAD_REQUIRED, then the fields in the default fetch group
     * cannot be accessed for read or write without notifying the StateManager.
     */
    public static final byte LOAD_REQUIRED = 1;

    /**
     * If StateManager persistenceFlags is set to READ_OK, then the fields in the default fetch group
     * can be accessed for read without notifying the StateManager.
     */
    public static final byte READ_OK = -1;

    
    /**
     * If the flags for a field includes CHECK_READ, then the field has been enhanced to call the 
     * StateManager on read if the jdoFlags setting is not READ_OK or READ_WRITE_OK.
     */
    public static final byte CHECK_READ = 1;

    /**
     * If the flags for a field includes MEDIATE_READ, then the field has been enhanced to always 
     * call the StateManager on all reads.
     */
    public static final byte MEDIATE_READ = 2;

    /**
     * If the flags for a field includes CHECK_WRITE, then the field has been enhanced to call the
     * StateManager on write if the jpoxFlags setting is not READ_WRITE_OK;.
     */
    public static final byte CHECK_WRITE = 4;

    /**
     * If the flags for a field includes MEDIATE_WRITE, then the field has been enhanced to always 
     * call the StateManager on all writes.
     */
    public static final byte MEDIATE_WRITE = 8;

    /**
     * If the flags for a field includes SERIALIZABLE, then the field is not declared as TRANSIENT.
     */
    public static final byte SERIALIZABLE = 16;

    /**
     * Utility to convert JDO specific flags to a String.
     * @param flags The JDO flags
     * @return String version 
     */
    public static String persistenceFlagsToString(byte flags)
    {
        switch (flags)
        {
            case LOAD_REQUIRED:
                return "LOAD_REQUIRED";
            case READ_OK:
                return "READ_OK";
            case READ_WRITE_OK:
                return "READ_WRITE_OK";
            default:
                return "???";
        }
    }
}