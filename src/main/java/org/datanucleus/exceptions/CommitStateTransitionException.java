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
package org.datanucleus.exceptions;

import org.datanucleus.util.Localiser;

/**
 * A <i>CommitStateTransitionException</i> is thrown when an error occurs
 * transitioning from one lifecycle state to another.
 */
public class CommitStateTransitionException extends NucleusException
{
    private static final long serialVersionUID = 5977558567821991933L;

    /**
     * Constructor.
     * @param nested Nested exceptions
     **/
    public CommitStateTransitionException(java.lang.Exception[] nested)
    {
        super(Localiser.msg("015037"),nested);
    }
}