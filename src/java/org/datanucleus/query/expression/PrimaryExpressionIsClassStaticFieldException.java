/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.query.expression;

import java.lang.reflect.Field;

import org.datanucleus.exceptions.NucleusException;

/**
 * Exception thrown when compiling a PrimaryExpression and we find that it really represents
 * a static field of a Class (literal), and so should be swapped in the Node tree.
 */
public class PrimaryExpressionIsClassStaticFieldException extends NucleusException
{
    /** The class that the PrimaryExpression represents. */
    Field field;

    public PrimaryExpressionIsClassStaticFieldException(Field fld)
    {
        super("PrimaryExpression should be a Literal representing field " + fld.getName());
        this.field = fld;
    }

    /**
     * Accessor for the field that this primary expression represents.
     * @return The field
     */
    public Field getLiteralField()
    {
        return field;
    }
}