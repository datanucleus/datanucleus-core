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

/**
 * Create instances of persistable objects. Instantiation of interfaces or
 * abstract classes is impossible, and for this reason concrete classes are generated and 
 * and enhanced at runtime by the ImplementationCreator. The generated classes
 * are loaded/defined by an internal ClassLoader to the ImplementationCreator. The internal
 * ClassLoader delegates to the ClassLoaderResolver (loader) the load of user classes.
 */
public interface ImplementationCreator
{
    /**
     * Constructs an implementation for an interface and instantiates it
     * @param pc The class of the interface or abstract class, or concrete class defined in MetaData
     * @param loader The ClassLoaderResolver for the interface
     * @return The instance implementing the interface
     */
    <T> T newInstance(Class<T> pc, ClassLoaderResolver loader);

    /**
     * Accessor for the ClassLoader loading classes created at runtime
     * @return The ClassLoader
     */
    ClassLoader getClassLoader();
}