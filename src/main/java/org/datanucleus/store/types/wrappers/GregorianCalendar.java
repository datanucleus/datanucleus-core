/**********************************************************************
Copyright (c) 2005 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.types.wrappers;

import java.io.ObjectStreamException;
import java.util.Date;
import java.util.TimeZone;

import org.datanucleus.FetchPlanState;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.types.SCO;

/**
 * A mutable second-class GregorianCalendar object.
 */
public class GregorianCalendar extends java.util.GregorianCalendar implements SCO<java.util.GregorianCalendar>
{
    protected transient ObjectProvider ownerOP;
    protected transient AbstractMemberMetaData ownerMmd;

    /**
     * Creates a <tt>GregorianCalendar</tt> object that represents the time at which it was allocated.
     * Assigns owning object and field name.
     * @param op ObjectProvider for the owning object
     * @param mmd Metadata for the member
     */
    public GregorianCalendar(ObjectProvider op, AbstractMemberMetaData mmd)
    {
        super();
        this.ownerOP = op;
        this.ownerMmd = mmd;
    }

    public void initialise()
    {
    }

    public void initialise(java.util.GregorianCalendar newValue, Object oldValue)
    {
        initialise(newValue);
    }

    public void initialise(java.util.GregorianCalendar cal)
    {
        super.setTimeInMillis(cal.getTime().getTime());
        super.setTimeZone(cal.getTimeZone());
    }

    /**
     * Accessor for the unwrapped value that we are wrapping.
     * @return The unwrapped value
     */
    public java.util.GregorianCalendar getValue()
    {
        java.util.GregorianCalendar cal = new java.util.GregorianCalendar(getTimeZone());
        cal.setTime(getTime());
        return cal;
    }

    /**
     * Utility to unset the owner.
     **/
    public void unsetOwner()
    {
        ownerOP = null;
        ownerMmd = null;
    }

    /**
     * Accessor for the owner.
     * @return The owner 
     **/
    public Object getOwner()
    {
        return ownerOP != null ? ownerOP.getObject() : null;
    }

    /**
     * Accessor for the field name
     * @return The field name
     */
    public String getFieldName()
    {
        return ownerMmd.getName();
    }

    /**
     * Utility to mark the object as dirty
     */
    public void makeDirty()
    {
        if (ownerOP != null)
        {
            ownerOP.makeDirty(ownerMmd.getAbsoluteFieldNumber());
            if (!ownerOP.getExecutionContext().getTransaction().isActive())
            {
                ownerOP.getExecutionContext().processNontransactionalUpdate();
            }
        }
    }

    /**
     * Method to return a detached copy of the value object.
     * @param state State for detachment process
     * @return The detached copy
     */
    public java.util.GregorianCalendar detachCopy(FetchPlanState state)
    {
        // Return a java.util.GregorianCalendar
        java.util.GregorianCalendar cal = new java.util.GregorianCalendar(getTimeZone());
        cal.setTime(getTime());
        return cal;
    }

    /**
     * Method to return an attached version for the passed ObjectProvider and
     * field, using the passed value.
     * @param value The new value
     */
    public void attachCopy(java.util.GregorianCalendar value)
    {
        long oldValue = getTimeInMillis();
        initialise(value);

        // Check if the field has changed, and set the owner field as dirty if
        // necessary
        long newValue = ((java.util.Calendar) value).getTime().getTime();
        if (oldValue != newValue)
        {
            makeDirty();
        }
    }

    /**
     * Creates and returns a copy of this object.
     * <P>Mutable second-class Objects are required to provide a public clone method in order to allow for copying persistable objects.
     * In contrast to Object.clone(), this method must not throw a CloneNotSupportedException.
     * @return A clone of the object
     */
    public Object clone()
    {
        Object obj = super.clone();

        ((GregorianCalendar)obj).unsetOwner();

        return obj;
    }

    /**
     * The writeReplace method is called when ObjectOutputStream is preparing to
     * write the object to the stream. The ObjectOutputStream checks whether the
     * class defines the writeReplace method. If the method is defined, the
     * writeReplace method is called to allow the object to designate its
     * replacement in the stream. The object returned should be either of the
     * same type as the object passed in or an object that when read and
     * resolved will result in an object of a type that is compatible with all
     * references to the object.
     * @return the replaced object
     * @throws ObjectStreamException if an error occurs
     */
    protected Object writeReplace()
    throws ObjectStreamException
    {
        java.util.GregorianCalendar cal = new java.util.GregorianCalendar(this.getTimeZone());
        cal.setTime(this.getTime());
        return cal;
    }

    // ------------------------- Implementation of the methods -----------------------------

    /**
     * Method to add an amount to a field
     * @param field The field
     * @param amount The amount to add
     */
    public void add(int field, int amount)
    {
        super.add(field, amount);
        makeDirty();
    }

    /**
     * Method to roll a field by 1.
     * @param field The field
     * @param up The whether to move it up
     */
    public void roll(int field, boolean up)
    {
        super.roll(field, up);
        makeDirty();
    }

    /**
     * Method to roll the value of a field
     * @param field The field
     * @param amount The amount to roll by
     */
    public void roll(int field, int amount)
    {
        super.roll(field, amount);
        makeDirty();
    }

    /**
     * Method to set a field
     * @param field The field
     * @param value The new value
     */
    public void set(int field, int value)
    {
        super.set(field, value);
        makeDirty();
    }

    /**
     * Method to set the gregorian cal change date
     * @param date The new change date
     */
    public void setGregorianChange(Date date)
    {
        super.setGregorianChange(date);
        makeDirty();
    }

    /**
     * Method to set the first day of the week
     * @param value The first day of the week
     */
    public void setFirstDayOfWeek(int value)
    {
        super.setFirstDayOfWeek(value);
        makeDirty();
    }

    /**
     * Method to set the lenient setting
     * @param lenient Whether it is lenient
     */
    public void setLenient(boolean lenient)
    {
        super.setLenient(lenient);
        makeDirty();
    }

    /**
     * Method to set the minimal days in the week
     * @param value The minimal days in the week
     */
    public void setMinimalDaysInFirstWeek(int value)
    {
        super.setMinimalDaysInFirstWeek(value);
        makeDirty();
    }

    /**
     * Method to set the time in milliseconds
     * @param millis The new time in millisecs
     */
    public void setTimeInMillis(long millis)
    {
        super.setTimeInMillis(millis);
        makeDirty();
    }

    /**
     * Method to set the timezone
     * @param value The new timezone
     */
    public void setTimeZone(TimeZone value)
    {
        super.setTimeZone(value);
        makeDirty();
    }
}