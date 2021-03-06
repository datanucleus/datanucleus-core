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
    ...
**********************************************************************/
package org.datanucleus.store.query.inmemory;

public abstract class AggregateExpression
{
    public Object add(Object obj)
    {
        throw new UnsupportedOperationException();
    }

    public Object sub(Object obj)
    {
        throw new UnsupportedOperationException();
    }

    public Object div(Object obj)
    {
        throw new UnsupportedOperationException();
    }

    public Boolean gt(Object obj)
    {
        throw new UnsupportedOperationException();
    }

    public Boolean lt(Object obj)
    {
        throw new UnsupportedOperationException();
    }

    public Boolean eq(Object obj)
    {
        throw new UnsupportedOperationException();
    }
}