/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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
package org.datanucleus.store.query;

import java.util.Iterator;

import org.datanucleus.ExecutionContext;
import org.datanucleus.FetchPlan;

/**
 * Extent of objects within DataNucleus.
 * Represents objects of a type, optionally including the subclasses of that type.
 * @param <T> The type of the candidate
 */
public interface Extent<T>
{
    /**
     * Accessor for candidate class of the extent.
     * @return Candidate class
     */
    Class<T> getCandidateClass();

    /**
     * Accessor for whether this extent includes subclasses.
     * @return Whether subclasses are contained
     */
    boolean hasSubclasses();

    ExecutionContext getExecutionContext();

    FetchPlan getFetchPlan();

    /**
     * Accessor for an iterator over the extent.
     * @return The iterator
     */
    Iterator<T> iterator();

    /**
     * Close all iterators and all resources for this extent.
     */
    void closeAll();

    /**
     * Close the specified iterator.
     * @param iterator The iterator
     */
    void close(Iterator<T> iterator);
}