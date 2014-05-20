/**********************************************************************
Copyright (c) 2003 Erik Bengtson and others. All rights reserved.
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.datanucleus.state.ObjectProvider;
import org.datanucleus.util.SoftValueMap;

/**
 * Level 1 Cache using Soft referenced objects in a Map.
 * <P>
 * If map entry value object is not actively being used, i.e. no other object has a strong reference to it, 
 * it may become garbage collected at the discretion of the garbage collector (typically if the VM is low on 
 * memory). If this happens, the entry in the <code>SoftValueMap</code> corresponding to the value object will 
 * also be removed.
 * </P>
 * @see java.lang.ref.SoftReference
 */
public class SoftRefCache implements Level1Cache
{
	private Map<Object, ObjectProvider> softCache = new SoftValueMap();

	public SoftRefCache()
	{
	}

	public ObjectProvider put(Object key, ObjectProvider value)
	{
		return softCache.put(key,value);        
	}

	public ObjectProvider get(Object key)
	{
		return softCache.get(key);
	}

	public boolean containsKey(Object key)
	{
		return softCache.containsKey(key);
	}

	public ObjectProvider remove(Object key)
	{
		return softCache.remove(key);       
	}

	public void clear()
	{
	    if (isEmpty())
	    {
	        return;
	    }
		softCache.clear();
	}
    
	/* (non-Javadoc)
	 * @see java.util.Map#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value)
	{
		return softCache.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#entrySet()
	 */
	public Set entrySet()
	{
		return softCache.entrySet();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty()
	{
		return softCache.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#keySet()
	 */
	public Set keySet()
	{
		return softCache.keySet();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#putAll(java.util.Map)
	 */
	public void putAll(Map t)
	{
		softCache.putAll(t);
	}

	/* (non-Javadoc)
	 * @see java.util.Map#size()
	 */
	public int size()
	{
		return softCache.size();
	}

	/* (non-Javadoc)
	 * @see java.util.Map#values()
	 */
	public Collection values()
	{
		return softCache.values();
	}
}