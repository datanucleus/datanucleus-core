/**********************************************************************
Copyright (c) 2002 TJDO and others. All rights reserved.
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
2004 Erik Bengtson - extends ObjectIdFieldManager
2007 Andy Jefferson - removed dependence on JDO so can be used with JPA
    ...
**********************************************************************/
package org.datanucleus.store.fieldmanager;

/**
 * Provide methods to fetch from/to a persistable object to/from the ObjectProvider/DataStore.
 */
public interface FieldManager extends FieldConsumer, FieldSupplier
{
}