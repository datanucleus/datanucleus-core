/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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

import java.sql.Types;

/**
 * Representation of the jdbc-type of a column.
 * Note that something similar to this is now present in JDK 1.8 so we could remove this in the future when that is the minimum JDK supported.
 */
public enum JdbcType 
{
    ARRAY(Types.ARRAY),
    BIGINT(Types.BIGINT),
    BINARY(Types.BINARY),
    BIT(Types.BIT),
    BLOB(Types.BLOB),
    BOOLEAN(Types.BOOLEAN),
    CHAR(Types.CHAR),
    CLOB(Types.CLOB),
    DATALINK(Types.DATALINK),
    DATE(Types.DATE),
    DECIMAL(Types.DECIMAL),
    DOUBLE(Types.DOUBLE),
    FLOAT(Types.FLOAT),
    INTEGER(Types.INTEGER),
    LONGNVARCHAR(Types.LONGNVARCHAR),
    LONGVARBINARY(Types.LONGVARBINARY),
    LONGVARCHAR(Types.LONGVARCHAR),
    NCHAR(Types.NCHAR),
    NCLOB(Types.NCLOB),
    NUMERIC(Types.NUMERIC),
    NVARCHAR(Types.NVARCHAR),
    OTHER(Types.OTHER),
    REAL(Types.REAL),
    SMALLINT(Types.SMALLINT),
    SQLXML(Types.SQLXML),
    TIME(Types.TIME),
    TIME_WITH_TIMEZONE(2013), // JDK 1.8+
    TIMESTAMP(Types.TIMESTAMP),
    TIMESTAMP_WITH_TIMEZONE(2014), // JDK 1.8+
    TINYINT(Types.TINYINT),
    VARBINARY(Types.VARBINARY),
    VARCHAR(Types.VARCHAR),
    XMLTYPE(2007); // TODO This is not strictly speaking a JDBC type (actually an SQL type for Oracle)

    private int value;

    private JdbcType(int value)
    {
        this.value = value;
    }

    public int getValue() 
    {
        return value;
    }

    public static JdbcType getEnumByValue(int value)
    {
        switch (value)
        {
            case Types.ARRAY:
                return ARRAY;
            case Types.BIGINT:
                return BIGINT;
            case Types.BINARY:
                return BINARY;
            case Types.BIT:
                return BIT;
            case Types.BLOB:
                return BLOB;
            case Types.BOOLEAN:
                return BOOLEAN;
            case Types.CHAR:
                return CHAR;
            case Types.CLOB:
                return CLOB;
            case Types.DATALINK:
                return DATALINK;
            case Types.DATE:
                return DATE;
            case Types.DECIMAL:
                return DECIMAL;
            case Types.DOUBLE:
                return DOUBLE;
            case Types.FLOAT:
                return FLOAT;
            case Types.INTEGER:
                return INTEGER;
            case Types.LONGNVARCHAR:
                return LONGNVARCHAR;
            case Types.LONGVARBINARY:
                return LONGVARBINARY;
            case Types.LONGVARCHAR:
                return LONGVARCHAR;
            case Types.NCHAR:
                return NCHAR;
            case Types.NCLOB:
                return NCLOB;
            case Types.NUMERIC:
                return NUMERIC;
            case Types.NVARCHAR:
                return NVARCHAR;
            case Types.OTHER:
                return OTHER;
            case Types.REAL:
                return REAL;
            case Types.SMALLINT:
                return SMALLINT;
            case Types.SQLXML:
                return SQLXML;
            case Types.TIME:
                return TIME;
            case 2013:
                return TIME_WITH_TIMEZONE;
            case Types.TIMESTAMP:
                return TIMESTAMP;
            case 2014:
                return TIMESTAMP_WITH_TIMEZONE;
            case Types.TINYINT:
                return TINYINT;
            case Types.VARBINARY:
                return VARBINARY;
            case Types.VARCHAR:
                return VARCHAR;
            case 2007:
                return XMLTYPE;
            default:
                return null;
        }
    }
}