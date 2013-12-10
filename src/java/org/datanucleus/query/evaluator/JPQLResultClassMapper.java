/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.query.evaluator;

/**
 * Class to map the results of a JPQL query to the result class.
 */
public class JPQLResultClassMapper extends AbstractResultClassMapper
{
    /**
     * Constructor for the ResultClassMapper which handles setResultClass
     * @param resultClass The required result class
     */
    public JPQLResultClassMapper(Class resultClass)
    {
        super(resultClass);
    }
}