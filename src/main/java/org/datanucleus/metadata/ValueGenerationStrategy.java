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
    ...
**********************************************************************/
package org.datanucleus.metadata;

import java.io.Serializable;

/**
 * Value generation "strategy".
 * Would have been nice to have this as an enum, but we need to allow for CUSTOM types
 */
public class ValueGenerationStrategy implements Serializable
{
    private static final long serialVersionUID = -6851202349718961853L;

    /**
     * strategy="native" in JDO, and "auto" in JPA
     * 
     * The value "native" allows the JDO implementation to pick the most
     * suitable strategy based on the underlying database.
     */
    public static final ValueGenerationStrategy NATIVE=new ValueGenerationStrategy(1);

    /**
     * strategy="sequence" in JDO and JPA
     * 
     * The value "sequence" specifies that a named database sequence is used to
     * generate key values for the table. If sequence is used, then the
     * sequence-name attribute is required.
     */
    public static final ValueGenerationStrategy SEQUENCE=new ValueGenerationStrategy(2);

    /**
     * strategy="identity" in JDO and JPA
     * 
     * The value "identity" specifies that the column identified as the key
     * column is managed by the database as an autoincrementing identity type.
     */
    public static final ValueGenerationStrategy IDENTITY=new ValueGenerationStrategy(3);

    /**
     * strategy="increment" in JDO and "table" in JPA
     * 
     * The value "increment" specifies a strategy that simply finds the largest
     * key already in the database and increments the key value for new
     * instances. It can be used with integral column types when the JDO
     * application is the only database user inserting new instances.
     */
    public static final ValueGenerationStrategy INCREMENT=new ValueGenerationStrategy(4);

    /**
     * strategy="uuid-string"
     * 
     * The value "uuid-string" specifies a strategy that generates a 128-bit
     * UUID unique within a network (the IP address of the machine running the
     * application is part of the id) and represents the result as a
     * 16-character String.
     */
    public static final ValueGenerationStrategy UUIDSTRING=new ValueGenerationStrategy(5);

    /**
     * strategy="uuid-hex"
     * 
     * The value "uuid-hex" specifies a strategy that generates a 128-bit UUID
     * unique within a network (the IP address of the machine running the
     * application is part of the id) and represents the result as a
     * 32-character String.
     */
    public static final ValueGenerationStrategy UUIDHEX=new ValueGenerationStrategy(6);

    /**
     * An extension strategy not in the standard JDO/JPA list.
     * Will have the "customName" set to the chosen strategy.
     * This object only exists for use in the equals() method to check if something is CUSTOM.
     */
    public static final ValueGenerationStrategy CUSTOM = new ValueGenerationStrategy(7);

    /** The type id. */
    private final int typeId;

    /** The Name of the custom type (if CUSTOM). */
    private String customName;

    /**
     * constructor
     * @param i type id
     */
    private ValueGenerationStrategy(int i)
    {
        this.typeId = i;
    }

    /**
     * Accessor for the custom name (if using strategy type of CUSTOM).
     * @return Custom name
     */
    public String getCustomName()
    {
        return customName;
    }

    public int hashCode()
    {
        return typeId;
    }

    public boolean equals(Object o)
    {
        if (o instanceof ValueGenerationStrategy)
        {
            return ((ValueGenerationStrategy)o).typeId == typeId;
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
            case 1 :
                return "native";
            case 2 :
                return "sequence";
            case 3 :
                return "identity";
            case 4 :
                return "increment";
            case 5 :
                return "uuid-string";
            case 6 :
                return "uuid-hex";
            case 7 :
                return "custom";
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
     * Gets an IdentityStrategy for the given value argument.
     * @param value the String representation of IdentityStrategy
     * @return the IdentityStrategy corresponding to the value argument. NATIVE
     * IdentityStrategy is returned if the value argument is null or no
     * corresponding strategy was found
     */
    public static ValueGenerationStrategy getIdentityStrategy(final String value)
    {
        if (value == null)
        {
            return ValueGenerationStrategy.NATIVE;
        }
        else if (ValueGenerationStrategy.NATIVE.toString().equals(value))
        {
            return ValueGenerationStrategy.NATIVE;
        }
        else if (ValueGenerationStrategy.SEQUENCE.toString().equals(value))
        {
            return ValueGenerationStrategy.SEQUENCE;
        }
        else if (ValueGenerationStrategy.IDENTITY.toString().equals(value))
        {
            return ValueGenerationStrategy.IDENTITY;
        }
        else if (ValueGenerationStrategy.INCREMENT.toString().equals(value))
        {
            return ValueGenerationStrategy.INCREMENT;
        }
        else if ("TABLE".equalsIgnoreCase(value))
        {
            // JPA "table" strategy equates to JDO "increment"
            return ValueGenerationStrategy.INCREMENT;
        }
        else if (ValueGenerationStrategy.UUIDSTRING.toString().equals(value))
        {
            return ValueGenerationStrategy.UUIDSTRING;
        }
        else if (ValueGenerationStrategy.UUIDHEX.toString().equals(value))
        {
            return ValueGenerationStrategy.UUIDHEX;
        }
        else
        {
            // All other strategies have their own strategy object
            ValueGenerationStrategy strategy = new ValueGenerationStrategy(7);
            strategy.customName = value;
            return strategy;
        }
    }
}