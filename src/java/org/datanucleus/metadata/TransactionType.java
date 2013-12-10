/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.metadata;

/**
 * Representation of a transaction type.
 */
public enum TransactionType
{
    JTA,
    RESOURCE_LOCAL;

    /**
     * Return Sequence strategy from String.
     * @param value sequence strategy
     * @return Instance of SequenceStrategy. If parse failed, return null.
     */
    public static TransactionType getValue(final String value)
    {
        if (value == null)
        {
            return null;
        }
        else if (TransactionType.JTA.toString().equalsIgnoreCase(value))
        {
            return TransactionType.JTA;
        }
        else if (TransactionType.RESOURCE_LOCAL.toString().equalsIgnoreCase(value))
        {
            return TransactionType.RESOURCE_LOCAL;
        }
        return null;
    }
}