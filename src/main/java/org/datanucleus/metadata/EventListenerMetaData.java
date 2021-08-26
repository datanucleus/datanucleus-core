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

import java.util.HashMap;
import java.util.Map;

import org.datanucleus.util.NucleusLogger;

/**
 * Listener for events, following the JPA/Jakarta model.
 */
public class EventListenerMetaData extends MetaData
{
    private static final long serialVersionUID = 6816110137508487523L;

    /** Name of the EventListener class. */
    String className;

    /** Method names in the EventListener class keyed by their callback name (e.g javax.persistence.PrePersist, etc). */
    Map<String, String> methodNamesByCallbackName = new HashMap();

    /**
     * Constructor for an EventListener MetaData
     * @param className Name of the EventListener class
     */
    public EventListenerMetaData(String className)
    {
        this.className = className;
    }

    /**
     * Accessor for the name of the EventListener class
     * @return Name of the EventListener
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * Method to add a method name for this EventListener.
     * @param callbackClassName Name of the callback type (javax.persistence.PrePersist etc)
     * @param methodName The method in the EventListener class that handles it
     */
    public void addCallback(String callbackClassName, String methodName)
    {
        addCallback(callbackClassName, this.className, methodName);
    }

    /**
     * Method to add a method name for this EventListener.
     * @param callbackClassName Name of the callback class
     * @param className Name of the class declaring the method
     * @param methodName The method in the className class that handles it
     */
    public void addCallback(String callbackClassName, String className, String methodName)
    {
        if (methodNamesByCallbackName == null)
        {
            methodNamesByCallbackName = new HashMap();
        }
        if (methodNamesByCallbackName.get(callbackClassName) != null)
        {
            // Only accept the first encountered method of a callback type
            // TODO Do we want to allow multiple callbacks of a particular type e.g PreStore?
            NucleusLogger.METADATA.debug("Attempt to register a callback " + callbackClassName + " for " + className + "." + methodName + 
                " but callback already registered for this callback : " + methodNamesByCallbackName.get(callbackClassName), new Exception());
            return;
        }

        methodNamesByCallbackName.put(callbackClassName, className + '.' + methodName);
    }

    /**
     * Accessor for the method name in the EventListener class that handles the specified callback event
     * @param callbackClassName Name of the callback class
     * @return The method name (if any). Fully-qualified
     */
    public String getMethodNameForCallbackClass(String callbackClassName)
    {
        return methodNamesByCallbackName.get(callbackClassName);
    }
}