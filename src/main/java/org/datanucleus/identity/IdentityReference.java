/**********************************************************************
Copyright (c) 2007 Xuan Baldorf and others. All rights reserved.
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
package org.datanucleus.identity;

import java.io.Serializable;

import org.datanucleus.util.StringUtils;

/**
 * Simple identity being a reference to the object itself.
 */
public class IdentityReference implements Serializable
{
    private static final long serialVersionUID = 2472281096825989665L;

    /** The object we are the identity for. */
    protected Object client;

    public IdentityReference(Object client)
    {
        this.client = client;
    }

    public int hashCode()
    {
        return System.identityHashCode(client);
    }

    public boolean equals(Object o)
    {
        if (o instanceof IdentityReference)
        {
            return client == ((IdentityReference)o).client;
        }
        return false;
    }

    public String toString()
    {
        return StringUtils.toJVMIDString(client);
    }
}