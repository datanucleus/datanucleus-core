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
2003 Andy Jefferson - commented
2007 Andy Jefferson - changed to extend NucleusUserException
    ...
**********************************************************************/
package org.datanucleus.store.types;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * A <i>IncompatibleFieldTypeException</i> is thrown if an incompatible field
 * type is specified in the construction of a second-class object instance.
 */
public class IncompatibleFieldTypeException extends NucleusUserException
{
    private static final long serialVersionUID = 6864005515921540632L;

    /**
     * Constructs an incompatible field type exception.
     * @param classAndFieldName The name of the class and SCO field.
     * @param requiredTypeName Name of required type of the field.
     * @param requestedTypeName Name of requested type of the field.
     */
    public IncompatibleFieldTypeException(String classAndFieldName, String requiredTypeName, String requestedTypeName)
    {
        super(Localiser.msg("023000", classAndFieldName, requiredTypeName, requestedTypeName));
    }
}