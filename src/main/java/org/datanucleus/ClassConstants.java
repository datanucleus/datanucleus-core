/**********************************************************************
Copyright (c) 2005 Erik Bengtson and others. All rights reserved.
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
package org.datanucleus;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.datanucleus.enhancement.ExecutionContextReference;
import org.datanucleus.enhancement.Persistable;
import org.datanucleus.enhancement.StateManager;
import org.datanucleus.identity.ByteId;
import org.datanucleus.identity.CharId;
import org.datanucleus.identity.IntId;
import org.datanucleus.identity.LongId;
import org.datanucleus.identity.DatastoreIdImpl;
import org.datanucleus.identity.ObjectId;
import org.datanucleus.identity.ShortId;
import org.datanucleus.identity.StringId;
import org.datanucleus.metadata.MetaDataManager;
import org.datanucleus.store.StoreManager;

/**
 * Constants with classes (class created to reduce overhead on calling Class.class *performance*)
 */
public class ClassConstants
{
    /** Loader for NucleusContext (represents the loader for "core"). */
    public static final ClassLoader NUCLEUS_CONTEXT_LOADER = NucleusContext.class.getClassLoader();

    public static final Class BOOLEAN = boolean.class;
    public static final Class BYTE = byte.class;
    public static final Class CHAR = char.class;
    public static final Class DOUBLE = double.class;
    public static final Class FLOAT = float.class;
    public static final Class INT = int.class;
    public static final Class LONG = long.class;
    public static final Class SHORT = short.class;

    public static final Class JAVA_LANG_BOOLEAN = Boolean.class;
    public static final Class JAVA_LANG_BYTE = Byte.class;
    public static final Class JAVA_LANG_CHARACTER = Character.class;
    public static final Class JAVA_LANG_DOUBLE = Double.class;
    public static final Class JAVA_LANG_FLOAT = Float.class;
    public static final Class JAVA_LANG_INTEGER = Integer.class;
    public static final Class JAVA_LANG_LONG = Long.class;
    public static final Class JAVA_LANG_SHORT = Short.class;

    public static final Class JAVA_LANG_STRING = String.class;
    public static final Class JAVA_MATH_BIGDECIMAL = BigDecimal.class;
    public static final Class JAVA_MATH_BIGINTEGER = BigInteger.class;
    public static final Class JAVA_SQL_DATE = java.sql.Date.class;
    public static final Class JAVA_SQL_TIME = java.sql.Time.class;
    public static final Class JAVA_SQL_TIMESTAMP = java.sql.Timestamp.class;
    public static final Class JAVA_UTIL_DATE = java.util.Date.class;
    public static final Class JAVA_IO_SERIALIZABLE = java.io.Serializable.class;

    public static final Class PERSISTENCE_NUCLEUS_CONTEXT = PersistenceNucleusContext.class;
    public static final Class NUCLEUS_CONTEXT = NucleusContext.class;
    public static final Class CLASS_LOADER_RESOLVER = ClassLoaderResolver.class;
    public static final Class STORE_MANAGER = StoreManager.class;
    public static final Class METADATA_MANAGER = MetaDataManager.class;
    public static final Class EXECUTION_CONTEXT = ExecutionContext.class;
    public static final Class EXECUTION_CONTEXT_REFERENCE = ExecutionContextReference.class;

    public static final Class PERSISTABLE = Persistable.class;
    public static final Class STATE_MANAGER = StateManager.class;

    // Identity classes
    public static final Class IDENTITY_SINGLEFIELD_LONG = LongId.class;
    public static final Class IDENTITY_SINGLEFIELD_INT = IntId.class;
    public static final Class IDENTITY_SINGLEFIELD_STRING = StringId.class;
    public static final Class IDENTITY_SINGLEFIELD_CHAR = CharId.class;
    public static final Class IDENTITY_SINGLEFIELD_BYTE = ByteId.class;
    public static final Class IDENTITY_SINGLEFIELD_OBJECT = ObjectId.class;
    public static final Class IDENTITY_SINGLEFIELD_SHORT = ShortId.class;
    public static final Class IDENTITY_OID_IMPL = DatastoreIdImpl.class;
}