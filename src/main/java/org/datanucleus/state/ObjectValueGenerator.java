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
package org.datanucleus.state;

import java.util.Map;

import org.datanucleus.ExecutionContext;

/**
 * Interface providing value generation based on an input (persistable) object.
 */
public interface ObjectValueGenerator
{
    /**
     * Method that takes the object being persisted by the specified ExecutionContext
     * and generates a value (based on the contents of the object). This could be used, for example,
     * to generate a unique value for the object based on some of its fields.
     * @param ec execution context
     * @param obj The object (persistent, or being persisted)
     * @param extensions Extensions on the field being generated
     * @return The value
     */
    Object generate(ExecutionContext ec, Object obj, Map<String, String> extensions);
}