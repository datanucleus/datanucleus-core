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
package org.datanucleus.query.node;

/**
 * Node representing a parameter. 
 * This is sub-classed so that we can store the parameter position at compile.
 */
public class ParameterNode extends Node
{
    int position;

    /**
     * Constructor for parameter node without a defined value.
     * @param nodeType
     * @param position Position
     */
    public ParameterNode(NodeType nodeType, int position)
    {
        super(nodeType);
        this.position = position;
    }

    /**
     * @param nodeType
     * @param nodeValue
     * @param position 
     */
    public ParameterNode(NodeType nodeType, Object nodeValue, int position)
    {
        super(nodeType, nodeValue);
        this.position = position;
    }

    public int getPosition()
    {
        return position;
    }
}