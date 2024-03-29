/**********************************************************************
Copyright (c) 2024 kraendavid and others. All rights reserved.
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
package org.datanucleus.cache;

/**
 * Marker interface for collections where it's possible to make modifications while iterating.
 * When used for Level 1 cache implementations there is a considerable performance benefit
 * when closing an execution context with many objects in Level 1 cache.
 */
public interface SupportsConcurrentModificationsIteration
{
}