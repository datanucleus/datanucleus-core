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

/**
 * Representation of the jdbc-type of a column.
 * Note that something similar to this is now present in JDK 1.8 so we could remove this in the future when that is the minimum JDK supported.
 */
public enum JdbcType 
{
    BIGINT,
    BINARY,
    BIT,
    BLOB,
    BOOLEAN,
    CHAR,
    CLOB,
    DATALINK,
    DATE,
    DECIMAL,
    DOUBLE,
    FLOAT,
    INTEGER,
    LONGNVARCHAR,
    LONGVARBINARY,
    LONGVARCHAR,
    NCHAR,
    NCLOB,
    NUMERIC,
    NVARCHAR,
    OTHER,
    REAL,
    SMALLINT,
    SQLXML,
    TIME,
    TIME_WITH_TIMEZONE, // JDK 1.8+
    TIMESTAMP,
    TIMESTAMP_WITH_TIMEZONE, // JDK 1.8+
    TINYINT,
    VARBINARY,
    VARCHAR,
    XMLTYPE; // TODO This is not strictly speaking a JDBC type (actually an SQL type for Oracle)
}