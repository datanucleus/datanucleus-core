/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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

import java.util.Set;

/**
 * Scanner for persistable classes, typically provided by a JEE environment to locate classes
 * not easily/efficiently locatable using the builtin file scanner.
 */
public interface MetaDataScanner
{
    /**
     * Scan for persistable classes.
     * @param pumd The persistence unit meta data
     * @return Names of persistable classes that were found
     */
    Set<String> scanForPersistableClasses(PersistenceUnitMetaData pumd);
}
