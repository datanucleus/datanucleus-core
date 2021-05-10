/**********************************************************************
Copyright (c) 2016 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.query.compiler;

import java.util.ArrayDeque;
import java.util.Deque;

import org.datanucleus.exceptions.NucleusException;

/**
 * Abstract query parser. To be extended for the particular query language.
 */
public abstract class AbstractParser implements Parser
{
    /** Whether to impose strict syntax for the query language. */
    protected boolean strict = false;

    protected Lexer lexer;

    protected Deque<Node> stack = new ArrayDeque<Node>();

    public AbstractParser()
    {
    }

    public void setStrict(boolean flag)
    {
        this.strict = flag;
    }

    public void setExplicitParameters(boolean flag)
    {
        // Override if you want to allow explicit parameters
        throw new NucleusException("Explicit parameters are not supported by this query parser");
    }

    /**
     * Convenience method to navigate down through descendants to find the last one.
     * Uses the first child node each time, so doesn't cope if there are multiple.
     * @param node The node
     * @return The last descendant
     */
    protected static Node getLastDescendantNodeForNode(Node node)
    {
        if (node == null)
        {
            return null;
        }
        if (node.getChildNodes() == null)
        {
            return node;
        }
        else if (node.getChildNodes().size() > 1)
        {
            return null;
        }
        if (!node.hasNextChild())
        {
            return node;
        }
        return getLastDescendantNodeForNode(node.getChildNode(0));
    }
}