/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
2005 Andy Jefferson - changed to extend JDOFatalUserException
    ...
**********************************************************************/
package org.datanucleus.metadata;

import org.datanucleus.exceptions.NucleusFatalUserException;
import org.datanucleus.util.Localiser;

/**
 * Representation of an exception thrown when an error occurs in Meta-Data definition.
 * All constructors take in a Localiser, a key into the localisation messages, and optional
 * parameters to use in the construction of the message.
 */
public class InvalidMetaDataException extends NucleusFatalUserException
{
    /** Message resources key */
    protected String messageKey;

    protected InvalidMetaDataException(String key, String message)
    {
        super(message);
        this.messageKey = key;
    }

    public InvalidMetaDataException(String key, Object... params)
    {
        this(key, Localiser.msg(key, params));
    }

    /**
     * Accessor for the message key into the localisation system of messages.
     * This is used in tests to validate the correct message is reported.
     * @return Message resource key
     */
    public String getMessageKey()
    {
        return messageKey;
    }
}