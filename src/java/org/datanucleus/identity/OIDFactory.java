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
package org.datanucleus.identity;

import org.datanucleus.ClassConstants;
import org.datanucleus.NucleusContext;
import org.datanucleus.util.ClassUtils;

/**
 * Factory for OID instances.
 * OIDs are not cached, due to thread sync issues and better performance in new JVMs
 */
public class OIDFactory
{
    private OIDFactory()
    {
        // Private constructor to prevent instantiation
    }

    /**
     * Factory method for OID instances using class name and key value.
     * @param nucleusCtx The Context
     * @param className the pc class name
     * @param value the id value
     * @return an OID instance
     */
    public static OID getInstance(NucleusContext nucleusCtx, String className, Object value)
    {
        // Get the OID class to use for this NucleusContext
        Class oidClass = nucleusCtx.getDatastoreIdentityClass();

        OID oid;
        if (oidClass == ClassConstants.OID_IMPL)
        {
            //we hard code OIDImpl to improve performance
            oid = new OIDImpl(className, value);
        }
        else
        {
            //others are pluggable
            oid = (OID)ClassUtils.newInstance(oidClass, new Class[] {String.class, Object.class}, 
                new Object[] {className, value});
        }
        return oid;
    }

    /**
     * Factory method for OID instances using long key value where the class of the object is not
     * important (i.e datastore-unique identity).
     * @param nucleusCtx Context
     * @param value the id value
     * @return an OID instance
     */
    public static OID getInstance(NucleusContext nucleusCtx, long value)
    {
        // Get the OID class to use for this NucleusContext
        Class oidClass = nucleusCtx.getDatastoreIdentityClass();

        OID oid;
        if (oidClass == DatastoreUniqueOID.class)
        {
            //we hard code DatastoreUniqueOID to improve performance
            oid = new DatastoreUniqueOID(value);
        }
        else
        {
            //others are pluggable
            oid = (OID)ClassUtils.newInstance(oidClass, new Class[] {Long.class}, 
                new Object[] {Long.valueOf(value)});
        }
        return oid;
    }

    /**
     * Factory method for OID instances using toString() output.
     * @param nucleusCtx Context
     * @param oidString result of toString on an OID
     * @return an OID instance
     */
    public static OID getInstance(NucleusContext nucleusCtx, String oidString)
    {
        // Get the OID class to use for this NucleusContext
        Class oidClass = nucleusCtx.getDatastoreIdentityClass();

        OID oid;
        if (oidClass == ClassConstants.OID_IMPL)
        {
            //we hard code OIDImpl to improve performance
            oid = new OIDImpl(oidString);
        }
        else
        {
            //others are pluggable
            oid = (OID)ClassUtils.newInstance(oidClass, new Class[] {String.class}, new Object[] {oidString});
        }
        return oid;
    }
}