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
package org.datanucleus.store.types.converters;

import java.lang.reflect.Method;

import org.datanucleus.util.NucleusLogger;

/**
 * Helper methods for using TypeConverter classes.
 */
public class TypeConverterHelper
{
    public static Class getMemberTypeForTypeConverter(TypeConverter conv, Class datastoreType)
    {
        try
        {
            Method m = conv.getClass().getMethod("toMemberType", new Class[] {datastoreType});
            return m.getReturnType();
        }
        catch (Exception e)
        {
            try
            {
                // Maybe is a wrapper to a converter, like for JPA
                Method m = conv.getClass().getMethod("getMemberClass");
                return (Class)m.invoke(conv);
            }
            catch (Exception e2)
            {
                NucleusLogger.GENERAL.warn(">> Converter " + conv + " didn't have adequate information from toMemberType nor from getDatastoreClass");
            }
        }
        return null;
    }

    public static Class getDatastoreTypeForTypeConverter(TypeConverter conv, Class memberType)
    {
        try
        {
            Method m = conv.getClass().getMethod("toDatastoreType", new Class[] {memberType});
            return m.getReturnType();
        }
        catch (Exception e)
        {
            // This will fail if we have a TypeConverter converting an interface, and the field is of the implementation type
        }

        try
        {
            // Maybe is a wrapper to a converter, like for JPA
            Method m = conv.getClass().getMethod("getDatastoreClass");
            return (Class)m.invoke(conv);
        }
        catch (Exception e2)
        {
            // Not a JPA wrapper type
        }

        // Maybe there is a toDatastoreType but not precise member type so just find the toDatastoreType method
        try
        {
            Method[] methods = conv.getClass().getMethods();
            if (methods != null)
            {
                // Note that with reflection we get duplicated methods here, so if we have a method "String toDatastoreType(Serializable)" then
                // reflection returns 1 method as "String toDatastoreType(Serializable)" and another as "Object toDatastoreType(Object)"
                for (int i=0;i<methods.length;i++)
                {
                    Class[] paramTypes = methods[i].getParameterTypes();
                    if (methods[i].getName().equals("toDatastoreType") && methods[i].getReturnType() != Object.class && paramTypes != null && paramTypes.length == 1)
                    {
                        return methods[i].getReturnType();
                    }
                }
            }
        }
        catch (Exception e3)
        {
            NucleusLogger.GENERAL.warn(">> Converter " + conv + " didn't have adequate information from toDatastoreType nor from getDatastoreClass");
        }

        return null;
    }
}