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
2009 Andy Jefferson - removed the value of the symbol so it can be thread-safe
    ...
**********************************************************************/
package org.datanucleus.query.symbol;

/**
 * A symbol in a query. Registers the name and the type of the symbol.
 */
public interface Symbol
{
    public static final int IDENTIFIER = 0;
    public static final int PARAMETER = 1;
    public static final int VARIABLE = 2;

    void setType(int type);
    int getType();

    String getQualifiedName();

    void setValueType(Class type);
    Class getValueType();
}