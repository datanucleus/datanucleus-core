/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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

import java.io.Serializable;

import org.datanucleus.ExecutionContext;

/**
 * Translator for object identities where the user wants to input identities that are not strict
 * JDO standard toString() forms, so this converts them.
 */
public interface IdentityStringTranslator extends Serializable
{
    /**
     * Method to translate the object into the identity.
     * @param ec ExecutionContext
     * @param stringId String form of the identity
     * @return The identity
     */
    Object getIdentity(ExecutionContext ec, String stringId);
}