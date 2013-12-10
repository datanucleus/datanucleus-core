/**********************************************************************
Copyright (c) 2004 Erik Bengtson and others. All rights reserved.
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
2007 Andy Jefferson - added deleting state
    ...
**********************************************************************/
package org.datanucleus.state;

/**
 * Definition of the activity states of a StateManager.
 * Each activity is mutually exclusive so, for example, a StateManager cannot be inserting
 * and then start deleting (without finishing the insert process first).
 */
public class ActivityState
{
    /** No current activity. **/
    public static final ActivityState NONE = new ActivityState(0);

    /** Inserting the object. **/
    public static final ActivityState INSERTING = new ActivityState(1);

    /** Running callbacks after the insert of the object. **/
    public static final ActivityState INSERTING_CALLBACKS = new ActivityState(2);

    /** Deleting the object. **/
    public static final ActivityState DELETING = new ActivityState(3);

    /** The type id */
    private final int typeId;

    /**
     * Constructor.
     * @param i type id
     */
    private ActivityState(int i)
    {
        this.typeId = i;
    }

    public int hashCode()
    {
        return typeId;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param o the reference object with which to compare. 
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    public boolean equals(Object o)
    {
        if (o instanceof ActivityState)
        {
            return ((ActivityState)o).typeId == typeId;
        }
        return false;
    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     */
    public String toString()
    {
        switch (typeId)
        {
            case 0 :
                return "none";
            case 1 :
                return "inserting";
            case 2 :
                return "inserting-callback";
            case 3 :
                return "deleting";
        }
        return "";
    }

    /**
     * Accessor for the type.
     * @return Type
     **/
    public int getType()
    {
        return typeId;
    }

    /**
     * Obtain the ActivityState for the given name by <code>value</code>
     * @param value the search name
     * @return the ActivityState for the value or ActivityState.NONE if not found
     */
    public static ActivityState getActivityState(final String value)
    {
        if (value == null)
        {
            return ActivityState.NONE;
        }
        else if (ActivityState.NONE.toString().equals(value))
        {
            return ActivityState.NONE;
        }
        else if (ActivityState.INSERTING.toString().equals(value))
        {
            return ActivityState.INSERTING;
        }
        else if (ActivityState.INSERTING_CALLBACKS.toString().equals(value))
        {
            return ActivityState.INSERTING_CALLBACKS;
        }
        else if (ActivityState.DELETING.toString().equals(value))
        {
            return ActivityState.DELETING;
        }
        return ActivityState.NONE;
    }
}