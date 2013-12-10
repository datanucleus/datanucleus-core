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
package org.datanucleus;

import org.datanucleus.state.CallbackHandler;

/**
 * Callback handler that does nothing. Provided for the case where the user wants to do bulk operations
 * and isn't interested in callbacks.
 */
public class NullCallbackHandler implements CallbackHandler
{
    public void setValidationListener(CallbackHandler handler)
    {
    }

    public void postCreate(Object pc)
    {
    }

    public void prePersist(Object pc)
    {
    }

    public void preStore(Object pc)
    {
    }

    public void postStore(Object pc)
    {
    }

    public void preClear(Object pc)
    {
    }

    public void postClear(Object pc)
    {
    }

    public void preDelete(Object pc)
    {
    }

    public void postDelete(Object pc)
    {
    }

    public void preDirty(Object pc)
    {
    }

    public void postDirty(Object pc)
    {
    }

    public void postLoad(Object pc)
    {
    }

    public void postRefresh(Object pc)
    {
    }

    public void preDetach(Object pc)
    {
    }

    public void postDetach(Object pc, Object detachedPC)
    {
    }

    public void preAttach(Object detachedPC)
    {
    }

    public void postAttach(Object pc, Object detachedPC)
    {
    }

    public void addListener(Object listener, Class[] classes)
    {
    }

    public void removeListener(Object listener)
    {
    }

    public void close()
    {
    }
}