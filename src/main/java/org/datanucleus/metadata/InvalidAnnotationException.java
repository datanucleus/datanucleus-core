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
package org.datanucleus.metadata;

import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.util.Localiser;

/**
 * Exception thrown when an annotation has been specified that is invalid in the circumstances.
 */
public class InvalidAnnotationException extends NucleusUserException
{
    private static final long serialVersionUID = -8436370607632552044L;
    /** Message resources key */
    protected String messageKey;

    /**
     * Constructor with message resource, message param and cause exception
     * @param key message resources key
     * @param cause cause exception
     * @param params parameters
     */
    public InvalidAnnotationException(String key, Throwable cause, Object... params)
    {
        super(Localiser.msg(key, params), cause);
        setFatal();
    }

    /**
     * Constructor with message resource, message params
     * @param key message resources key
     * @param params parameters to the message
     */
    public InvalidAnnotationException(String key, Object... params)
    {
        super(Localiser.msg(key, params));
        setFatal();
    }

    /**
     * Return message resource key
     * @return Message resource key
     */
    public String getMessageKey()
    {
        return messageKey;
    }
}