/**********************************************************************
Copyright (c) 2008 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.query.compiler;

import org.datanucleus.query.node.Node;

/**
 * Interface for a parser of a query.
 * To be implemented for each particular query language.
 * Responsible for taking a String clause of a query and converting it into a Node tree.
 */
public interface Parser
{
    public abstract Node parse(String expression);

    public abstract Node[] parseFrom(String expression);

    public abstract Node[] parseUpdate(String expression);

    public abstract Node[] parseOrder(String expression);

    public abstract Node[] parseResult(String expression);

    public abstract Node[] parseTupple(String expression);

    public abstract Node[][] parseVariables(String expression);

    public abstract Node parseVariable(String expression);

    public abstract Node[][] parseParameters(String expression);
}