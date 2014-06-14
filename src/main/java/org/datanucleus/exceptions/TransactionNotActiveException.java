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
 * An exception thrown when commit/rollback is invoked on an ExecutionContext yet the transaction is not active.
 */
public class TransactionNotActiveException extends NucleusUserException
{
    private static final long serialVersionUID = -3462236079972766332L;

    /**
     * Constructs an exception with the specified detail message.
     */
    public TransactionNotActiveException()
    {
        super(Localiser.msg("015035"));
    }

    /**
     * Constructor.
     * @param message the localized error message
     * @param failedObject the failed object
     */
    public TransactionNotActiveException(String message, Object failedObject)
    {
        super(message,failedObject);
    }
}