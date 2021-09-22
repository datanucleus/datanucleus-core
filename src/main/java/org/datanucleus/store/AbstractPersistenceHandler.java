/**********************************************************************
Copyright (c) 2009 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datanucleus.ExecutionContext;
import org.datanucleus.PropertyNames;
import org.datanucleus.exceptions.DatastoreReadOnlyException;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import org.datanucleus.exceptions.NucleusUserException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.MetaData;
import org.datanucleus.state.ObjectProvider;
import org.datanucleus.store.query.Query;
import org.datanucleus.util.Localiser;
import org.datanucleus.util.NucleusLogger;
import org.datanucleus.util.StringUtils;

/**
 * Abstract representation of a persistence handler, to be extended by datastores own variant.
 */
public abstract class AbstractPersistenceHandler implements StorePersistenceHandler
{
    protected StoreManager storeMgr;

    public AbstractPersistenceHandler(StoreManager storeMgr)
    {
        this.storeMgr = storeMgr;
    }

    /* (non-Javadoc)
     * @see org.datanucleus.store.StorePersistenceHandler#findObjectForUnique(org.datanucleus.ExecutionContext, org.datanucleus.metadata.AbstractClassMetaData, java.lang.String[], java.lang.Object[])
     */
    @Override
    public Object findObjectForUnique(ExecutionContext ec, AbstractClassMetaData cmd, String[] memberNames, Object[] values)
    {
        if (memberNames.length != values.length)
        {
            throw new NucleusUserException("findObjectForUnique should have same number of member names and values");
        }

        // Fallback to using a simple JDOQL query (which is what would be performed for the majority of datastores anyway)
        StringBuilder jdoqlStr = new StringBuilder("SELECT FROM ").append(cmd.getFullClassName()).append(" WHERE ");
        Map<String, Object> paramValueMap = new HashMap<>();
        for (int i=0;i<memberNames.length;i++)
        {
            jdoqlStr.append("this.").append(memberNames[i]).append(" == :val").append(i);
            paramValueMap.put("val" + i, values[i]);
            if (i != memberNames.length-1)
            {
                jdoqlStr.append(" && ");
            }
        }
        Query q = storeMgr.newQuery(Query.LANGUAGE_JDOQL, ec, jdoqlStr.toString());
        List results = (List)q.executeWithMap(paramValueMap);
        if (results == null || results.size() == 0)
        {
            throw new NucleusObjectNotFoundException("No object found for specified members and values of type " + cmd.getFullClassName());
        }
        else if (results.size() == 1)
        {
            return results.get(0);
        }
        throw new NucleusUserException("Specified members for class " + cmd.getFullClassName() + " finds multiple objects!");
    }

    /**
     * Convenience method to assert when this StoreManager is read-only and the specified object is attempting to be updated.
     * @param op StateManager for the object
     */
    public void assertReadOnlyForUpdateOfObject(ObjectProvider op)
    {
        if (op.getExecutionContext().getBooleanProperty(PropertyNames.PROPERTY_DATASTORE_READONLY))
        {
            if (op.getExecutionContext().getStringProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION).equalsIgnoreCase("EXCEPTION"))
            {
                throw new DatastoreReadOnlyException(Localiser.msg("032004", op.getObjectAsPrintable()), op.getExecutionContext().getClassLoaderResolver());
            }

            if (NucleusLogger.PERSISTENCE.isDebugEnabled())
            {
                NucleusLogger.PERSISTENCE.debug(Localiser.msg("032005", op.getObjectAsPrintable()));
            }
            return;
        }

        AbstractClassMetaData cmd = op.getClassMetaData();
        if (cmd.hasExtension(MetaData.EXTENSION_CLASS_READ_ONLY))
        {
            String value = cmd.getValueForExtension(MetaData.EXTENSION_CLASS_READ_ONLY);
            if (!StringUtils.isWhitespace(value))
            {
                boolean readonly = Boolean.valueOf(value).booleanValue();
                if (readonly)
                {
                    if (op.getExecutionContext().getStringProperty(PropertyNames.PROPERTY_DATASTORE_READONLY_ACTION).equalsIgnoreCase("EXCEPTION"))
                    {
                        throw new DatastoreReadOnlyException(Localiser.msg("032006", op.getObjectAsPrintable()), op.getExecutionContext().getClassLoaderResolver());
                    }

                    if (NucleusLogger.PERSISTENCE.isDebugEnabled())
                    {
                        NucleusLogger.PERSISTENCE.debug(Localiser.msg("032007", op.getObjectAsPrintable()));
                    }
                    return;
                }
            }
        }
    }
}