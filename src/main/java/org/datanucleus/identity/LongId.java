/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.identity;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * This class is for identity with a single long field.
 */
public class LongId extends SingleFieldId<Long>
{
    private long key;

    public LongId(Class pcClass, long key)
    {
        super(pcClass);
        this.key = key;
        this.hashCode = targetClassName.hashCode() ^ (int) key;
    }

    public LongId(Class pcClass, Long key)
    {
        this(pcClass, key != null ? key.longValue() : -1);
        assertKeyNotNull(key);
    }

    public LongId(Class pcClass, String str)
    {
        this(pcClass, Long.parseLong(str));
        assertKeyNotNull(str);
    }

    public LongId()
    {
    }

    public long getKey()
    {
        return key;
    }

    public Long getKeyAsObject()
    {
        return Long.valueOf(key);
    }

    /**
     * Return the String form of the key.
     * @return the String form of the key
     */
    public String toString()
    {
        return Long.toString(key);
    }

    /* (non-Javadoc)
     * @see org.datanucleus.identity.SingleFieldId#keyEquals(org.datanucleus.identity.SingleFieldId)
     */
    @Override
    protected boolean keyEquals(SingleFieldId obj)
    {
        if (obj instanceof LongId)
        {
            return key == ((LongId)obj).key;
        }
        return false;
    }

    public int compareTo(Object o)
    {
        if (o instanceof LongId)
        {
            LongId other = (LongId) o;
            int result = super.compare(other);
            if (result == 0)
            {
                long diff = key - other.key;
                if (diff == 0)
                {
                    return 0;
                }

                if (diff < 0)
                {
                    return -1;
                }
                return 1;
            }
            return result;
        }
        else if (o == null)
        {
            throw new ClassCastException("object is null");
        }
        throw new ClassCastException(this.getClass().getName() + " != " + o.getClass().getName());
    }

    /**
     * Write this object. Write the superclass first.
     * @param out the output
     */
    public void writeExternal(ObjectOutput out) throws IOException
    {
        super.writeExternal(out);
        out.writeLong(key);
    }

    /**
     * Read this object. Read the superclass first.
     * @param in the input
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        super.readExternal(in);
        key = in.readLong();
    }
}