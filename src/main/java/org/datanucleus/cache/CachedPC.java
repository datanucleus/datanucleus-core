/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.cache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.datanucleus.util.ClassUtils;
import org.datanucleus.util.StringUtils;

/**
 * An object that is stored in the Level2 Cache keyed by the identity of the persistable object.
 * Comprises a map of the field values keyed by the absolute field number in the class, the loaded fields array, and the version of the object that is represented with these values.
 * <ul>
 * <li>Where the field is a relation field (PC, Map, Collection, array) we store the id of any referenced persistable object. 
 * This is used when regenerating the object, and recreating its relations. 
 * Note that the "id" is the DatastoreId or SingleFieldId etc where applicable otherwise is CachedId (ensuring that the class of the related object is stored).</li>
 * <li>Where the field contains an embedded/serialised persistable object, we store a nested CachedPC object representing that object (since it doesn't exist in its own right).</li>
 * </ul>
 */
public class CachedPC<T> implements Serializable
{
    private static final long serialVersionUID = 1326244752228266953L;

    /** Class of the object being cached. */
    private Class<T> cls;

    /** Identity of the object being cached. This is to allow recreation of the object when using uniqueKey lookup. */
    private Object id;

    /** Values for the fields, keyed by the absolute field number. */
    private Map<Integer, Object> fieldValues = null;

    /** Version of the cached object (if any) - Long, Timestamp etc. */
    private Object version;

    /** The loaded fields array. TODO Note that this could be interpreted from the keys of fieldValues. */
    private boolean[] loadedFields;

    /**
     * Constructor.
     * @param cls The class of the object
     * @param loadedFields The loaded fields
     * @param vers Version of the object (optional)
     * @param id Identity of the object
     */
    public CachedPC(Class<T> cls, boolean[] loadedFields, Object vers, Object id)
    {
        this.cls = cls;
        this.id = id;
        this.loadedFields = new boolean[loadedFields.length];
        for (int i = 0; i < loadedFields.length; i++)
        {
            this.loadedFields[i] = loadedFields[i];
        }
        this.version = vers;
    }

    public Class<T> getObjectClass()
    {
        return cls;
    }

    public Object getId()
    {
        return id;
    }

    public void setFieldValue(Integer fieldNumber, Object value)
    {
        if (fieldValues == null)
        {
            fieldValues = new HashMap<>();
        }
        fieldValues.put(fieldNumber, value);
    }

    public Object getFieldValue(Integer fieldNumber)
    {
        return (fieldValues == null) ? null : fieldValues.get(fieldNumber);
    }

    public void setVersion(Object ver)
    {
        this.version = ver;
    }

    public Object getVersion()
    {
        return version;
    }

    /**
     * Accessor for the loaded fields of this object. Use setLoadedField() if you want to update a flag.
     * @return The loaded fields flags
     */
    public boolean[] getLoadedFields()
    {
        return loadedFields;
    }

    public int[] getLoadedFieldNumbers()
    {
        return ClassUtils.getFlagsSetTo(loadedFields, true);
    }

    public void setLoadedField(int fieldNumber, boolean loaded)
    {
        loadedFields[fieldNumber] = loaded;
    }

    public synchronized CachedPC<T> getCopy()
    {
        CachedPC<T> copy = new CachedPC(cls, loadedFields, version, id);
        if (fieldValues != null)
        {
            // TODO Some (mutable) field values may need copying
            copy.fieldValues = new HashMap<Integer, Object>(fieldValues.size());
            Iterator<Map.Entry<Integer, Object>> entryIter = fieldValues.entrySet().iterator();
            while (entryIter.hasNext())
            {
                Map.Entry<Integer, Object> entry = entryIter.next();
                Integer key = entry.getKey();
                Object val = entry.getValue();
                if (val != null && val instanceof CachedPC)
                {
                    val = ((CachedPC) val).getCopy();
                }
                copy.fieldValues.put(key, val);
            }
        }
        return copy;
    }

    /**
     * Method to return a sting form of the cached object. Returns something like
     * <pre>
     * CachedPC : cls=mydomain.MyClass version=1 loadedFlags=[YY]
     * </pre>
     * @return The string form
     */
    public String toString()
    {
        return toString("", false);
    }

    /**
     * Method to return a string form of the cached object. Returns something like
     * <pre>
     * CachedPC : cls=mydomain.MyClass version=1 loadedFlags=[YY] numValues=2
     *     field=0 value=101 type=class java.lang.Long
     *     field=1 value=Home type=class java.lang.String
     * </pre>
     * when debug is enabled, and omits the "field=..." parts when not using debug.
     * @param indent Indent for this CachedPC
     * @param debug Whether to include the field values in the return
     * @return The string form
     */
    public String toString(String indent, boolean debug)
    {
        StringBuilder str = new StringBuilder();
        str.append(indent).append("CachedPC : cls=").append(cls.getName()).append(" version=").append(version).append(" loadedFlags=").append(StringUtils.booleanArrayToString(loadedFields));
        if (debug && fieldValues != null)
        {
            str.append(" numValues=").append(fieldValues != null ? fieldValues.size() : 0).append("\n");

            Iterator<Map.Entry<Integer, Object>> fieldValuesIter = fieldValues.entrySet().iterator();
            while (fieldValuesIter.hasNext())
            {
                Map.Entry<Integer, Object> fieldValuesEntry = fieldValuesIter.next();
                str.append(indent).append("  ").append("field=").append(fieldValuesEntry.getKey()).append(" value=");
                if (fieldValuesEntry.getValue() instanceof CachedPC)
                {
                    str.append("\n");
                    str.append(((CachedPC)fieldValuesEntry.getValue()).toString(indent + "  ", debug));
                }
                else
                {
                    str.append(fieldValuesEntry.getValue());
                    if (fieldValuesEntry.getValue() != null)
                    {
                        str.append(" type=" + fieldValuesEntry.getValue().getClass().getName());
                    }
                }
                if (fieldValuesIter.hasNext())
                {
                    str.append("\n");
                }
            }
        }
        return str.toString();
    }

    public static class CachedId implements Serializable, Comparable<CachedId>
    {
        private static final long serialVersionUID = -2806783207184913323L;

        String className;

        Object id;

        public CachedId(String className, Object id)
        {
            this.className = className;
            this.id = id;
        }

        public String getClassName()
        {
            return className;
        }

        public Object getId()
        {
            return id;
        }

        public boolean equals(Object obj)
        {
            if (obj == null || !(obj instanceof CachedId))
            {
                return false;
            }
            CachedId other = (CachedId) obj;
            return other.className.equals(className) && other.id.equals(id);
        }

        public int hashCode()
        {
            return className.hashCode() ^ id.hashCode();
        }

        public int compareTo(CachedId obj)
        {
            if (obj == null)
            {
                return 1;
            }
            return hashCode() - obj.hashCode();
        }
    }
}