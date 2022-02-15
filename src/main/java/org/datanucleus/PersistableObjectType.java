/**********************************************************************
Copyright (c) 2022 Andy Jefferson and others. All rights reserved.
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
 * Definition of the type of a persistable object.
 * This can be a plain persistable object (PC) or a persistable object embedded/serialised, possibily in a collection/array element or in a map key/value.
 */
public enum PersistableObjectType
{
    /** Persisted in own right with an identity */
    PC,
    /** Persistable type persisted into owner */
    EMBEDDED_PC,
    /** Persistable collection element persisted into owner */
    EMBEDDED_COLLECTION_ELEMENT_PC,
    /** Persistable array element persisted into owner */
    EMBEDDED_ARRAY_ELEMENT_PC,
    /** Persistable map key persisted into owner */
    EMBEDDED_MAP_KEY_PC,
    /** Persistable map value persisted into owner */
    EMBEDDED_MAP_VALUE_PC
}