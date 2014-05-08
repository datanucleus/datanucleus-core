/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved. 
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
2003 Andy Jefferson - coding standards
2007 Andy Jefferson - converted to true "nondurable" identity class
    ...
**********************************************************************/
package org.datanucleus.identity;

/**
 * Object identifier for use with nondurable objects to guarantee uniqueness in the JVM (but not in datastore).
 * They can also be used as identifiers for classes that have no database extent.
 * Refer to JDO 2 [5.4.4]. An Object id class must have the following :-
 * <ul>
 * <li>Must be public</li>
 * <li>Must have a public constructor, which may be the default (no-arg) constructor</li>
 * <li>All fields must be public</li>
 * <li>All fields must be of Serializable types.</li>
 * </ul>
 */
public final class SCOID
{
    public final String objClass;

    /**
     * Constructs a new SCOID to identify an object of the given class.
     * @param objClass The class of the instance being identified.
     */
    public SCOID(String objClass)
    {
        this.objClass = objClass;
    }

    /**
     * Returns the class of the object identified by this SCOID.
     * @return  The class of the object identified by this SCOID.
     */
    public String getSCOClass()
    {
        return objClass;
    }
}