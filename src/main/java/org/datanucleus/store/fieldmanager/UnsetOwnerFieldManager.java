/**********************************************************************
Copyright (c) 2012 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.fieldmanager;

import org.datanucleus.store.types.SCO;

/**
 * FieldManager to unset the owner fields of any SCO wrapped fields.
 */
public class UnsetOwnerFieldManager extends AbstractFieldManager
{
    public void storeObjectField(int fieldNumber, Object value)
    {
        if (value instanceof SCO)
        {
            ((SCO)value).unsetOwner();
        }
    }
}