/**********************************************************************
Copyright (c) 2002 Mike Martin (TJDO) and others. All rights reserved.
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
2002 Kelly Grizzle (TJDO)
2003 Andy Jefferson - commented
    ...
**********************************************************************/
package org.datanucleus.state;

import org.datanucleus.exceptions.NucleusException;
import org.datanucleus.util.Localiser;

/**
 * A <i>IllegalStateTransitionException</i> is thrown if a life-cycle
 * state attempts a transition that is illegal.  This indicates a coding
 * bug in the JDO implementation.
 */
public class IllegalStateTransitionException extends NucleusException
{
    private static final long serialVersionUID = -1686259899799936448L;

    /**
     * Constructs an illegal state transition exception.
     * @param state The object's current life-cycle state.
     * @param transition A string describing the type of transition.
     * @param op ObjectProvider for the object.
     */
    public IllegalStateTransitionException(LifeCycleState state, String transition, ObjectProvider op)
    {
        super(Localiser.msg("026027", transition, state, op));
        setFatal();
    }
}