/**********************************************************************
Copyright (c) 2013 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.flush;

import org.datanucleus.state.ObjectProvider;

/**
 * Flush operation for a persist of the specified object.
 */
public class PersistOperation implements Operation
{
    ObjectProvider sm;

    public PersistOperation(ObjectProvider sm)
    {
        this.sm = sm;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.Operation#getObjectProvider()
     */
    public ObjectProvider getObjectProvider()
    {
        return sm;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.flush.Operation#perform()
     */
    public void perform()
    {
        // TODO Call persist of this object. Currently handled by FlushProcess
    }

    public String toString()
    {
        return "PERSIST : " + sm;
    }
}
