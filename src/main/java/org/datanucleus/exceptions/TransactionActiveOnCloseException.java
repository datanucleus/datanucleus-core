/**********************************************************************
Copyright (c) 2007 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.exceptions;

import org.datanucleus.util.Localiser;

/**
 * An exception thrown when close is invoked on an ExecutionContext yet the transaction is still active.
 */
public class TransactionActiveOnCloseException extends NucleusUserException
{
    private static final long serialVersionUID = 8801501994814961125L;

    /**
     * Constructs a transaction is still active exception with the specified detail message.
     * @param failedObject ExecutionContext object that failed to close
     */
    public TransactionActiveOnCloseException(Object failedObject)
    {
        super(Localiser.msg("015034"), failedObject);
    }
}