/******************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
*****************************************************************/
package org.datanucleus.util;

import org.datanucleus.exceptions.NucleusUserException;

/**
 * Listener hook for detachment events, as an alternative to the JDO standard process of throwing a NucleusUserException.
 * Call setInstance() with your DetachListener and you will pick up all events.
 */
public abstract class DetachListener
{
    private static DetachListener instance;

    public static DetachListener getInstance()
    {
        if (instance == null)
        {
            // Fallback instance providing JDO-bytecode-contract compliant behaviour
            synchronized (DetachListener.class)
            {
                if (instance == null)
                {
                    instance = new DetachListener()
                    {
                        @Override
                        public void undetachedFieldAccess(Object instance, String fieldName)
                        {
                            throw new NucleusUserException("You have just attempted to access field \"" + fieldName + 
                                "\" yet this field was not detached when you detached the object." +
                                " Either dont access this field, or detach it when detaching the object.");
                        }
                    };
                }
            }
        }
        return instance;
    }

    public static void setInstance(DetachListener instance)
    {
        synchronized (DetachListener.class)
        {
            DetachListener.instance = instance;
        }
    }

    /**
     * Invoked when a user tries to get a non-loaded field on a detached object.
     * @param instance of object detached
     * @param memberName name of field/property that has been read.
     */
    public abstract void undetachedFieldAccess(Object instance, String memberName);
}