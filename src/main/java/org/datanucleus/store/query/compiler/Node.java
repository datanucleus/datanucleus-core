/**********************************************************************
Copyright (c) 2008 Erik Bengtson and others. All rights reserved.
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
2008 Andy Jefferson - add properties. Add CLASS, PARAMETER types. Javadocs
    ...
**********************************************************************/
package org.datanucleus.store.query.compiler;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a node in a tree of nodes.
 * Has a parent, and a list of children. Each node has a type and a value.
 * Optionally has a set of properties; these represent arguments when part of a method call.
 */
public class Node
{
    /** Type of node. */
    protected NodeType nodeType;

    /** Value of the node. */
    protected Object nodeValue;

    /** Working variable used for iterating between child nodes. */
    private int cursorPos = -1;

    /** Parent of this node. */
    protected Node parent;

    /** List of child nodes in the tree below here. */
    protected List<Node> childNodes = new ArrayList();

    /** List of properties for the node. Used for invocation of methods, representing the arguments. */
    protected List<Node> properties = null;

    public Node(NodeType nodeType)
    {
        this.nodeType = nodeType;
    }
    
    public Node(NodeType nodeType, Object nodeValue)
    {
        this.nodeType = nodeType;
        this.nodeValue = nodeValue;
    }

    public NodeType getNodeType()
    {
        return nodeType;
    }

    public void setNodeValue(Object val)
    {
        this.nodeValue = val;
    }

    public Object getNodeValue()
    {
        return nodeValue;
    }

    public boolean hasProperties()
    {
        return properties != null;
    }

    public List<Node> getProperties()
    {
        return properties;
    }

    public void addProperty(Node node)
    {
        if (properties == null)
        {
            this.properties = new ArrayList();
        }
        this.properties.add(node);
    }

    public void setPropertyAtPosition(int position, Node node)
    {
        if (properties == null)
        {
            return;
        }
        if (position >= properties.size())
        {
            return;
        }
        properties.set(position, node);
    }

    public List<Node> getChildNodes()
    {
        return childNodes;
    }

    public void removeChildNode(Node node)
    {
        childNodes.remove(node);
    }

    public Node insertChildNode(Node node)
    {
        childNodes.add(0, node);
        return node;
    }

    public Node insertChildNode(Node node, int position)
    {
        childNodes.add(position, node);
        return node;
    }

    public Node appendChildNode(Node node)
    {
        childNodes.add(node);
        return node;
    }

    /*public Node[] appendChildNode(Node[] node)
    {
        childNodes.add(node);
        return node;
    }

    public Node[][] appendChildNode(Node[][] node)
    {
        childNodes.add(node);
        return node;
    }*/

    public Node getChildNode(int index)
    {
        return childNodes.get(index);
    }

    /**
     * Access the first child node.
     * @return The first node, or null if no children present
     */
    public Node getFirstChild()
    {
        cursorPos = 0;
        if (childNodes.size() < 1)
        {
            return null;
        }
        return childNodes.get(0);
    }

    /**
     * Access the next node. This asssumes that the method <pre>getFirstChild</pre> has been called before.
     * @return The next child
     */
    public Node getNextChild()
    {
        cursorPos++;
        if (childNodes.size() <= cursorPos)
        {
            return null;
        }
        return childNodes.get(cursorPos);
    }

    /**
     * Return whether there is a "next" child node. Assumes that the method <pre>getFirstChild</pre> has been called before.
     * @return Whether there is a next child node.
     */
    public boolean hasNextChild()
    {
        return cursorPos+1<childNodes.size();
    }

    public void setParent(Node parent)
    {
        this.parent = parent;
    }
    
    public Node getParent()
    {
        return parent;
    }
    
    public String getNodeId()
    {
        Node node = this;
        StringBuilder sb = new StringBuilder();
        while (node != null && node.getNodeType() == NodeType.IDENTIFIER)
        {
            if (sb.length() > 0)
            {
                sb.insert(0, ".");
            }
            sb.insert(0, node.getNodeValue());
            node = node.getParent();
        }
        return sb.toString();
    }

    public String getNodeChildId()
    {
        Node node = this;
        StringBuilder sb = new StringBuilder();
        while (node != null && node.getNodeType() == NodeType.IDENTIFIER)
        {
            if (sb.length() > 0)
            {
                sb.append(".");
            }
            sb.append(node.getNodeValue());
            node = node.getFirstChild();
        }
        return sb.toString();
    }

    /**
     * Method to print out the Node as a tree.
     * @return the node tree as a string
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(printTree(0));
        return sb.toString();
    }

    public Node clone(Node parent)
    {
        Node n = new Node(nodeType, nodeValue);
        n.parent = parent;
        if (!childNodes.isEmpty())
        {
            for (Node child : childNodes)
            {
                Node c = child.clone(n);
                n.appendChildNode(c);
            }
        }
        if (properties != null && !properties.isEmpty())
        {
            for (Node prop : properties)
            {
                Node p = prop.clone(n);
                n.addProperty(p);
            }
        }
        return n;
    }

    /**
     * Utility method to print out the node tree.
     * @param indentation What indent to use
     * @return The tree string
     */
    private String printTree(int indentation)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indentation));
        String nodeTypeStr = nodeType.toString();

        sb.append("[" + nodeTypeStr + " : " + nodeValue);
        if (properties != null)
        {
            sb.append(indent(indentation)).append("(");
            for (int i=0; i<properties.size(); i++)
            {
                sb.append(properties.get(i).printTree(indentation+1));
                if (i < properties.size()-1)
                {
                    sb.append(",");
                }
            }
            sb.append(indent(indentation)).append(")");
        }

        if (!childNodes.isEmpty())
        {
            sb.append(".");

            for (int i=0; i<childNodes.size(); i++)
            {
                sb.append((childNodes.get(i)).printTree(indentation+1));
                if (i < childNodes.size()-1)
                {
                    sb.append(",");
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Utility to add on indenting when printing out the node tree.
     * @param indentation The indent size (number of spaces)
     * @return value indented
     */
    private String indent(int indentation)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int i=0; i<4*indentation; i++)
        {
            sb.append(" ");
        }
        return sb.toString();
    }
}