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
package org.datanucleus.identity;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.ExecutionContext;
import org.datanucleus.exceptions.ClassNotResolvedException;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.IdentityType;

/**
 * Identity translator that allows for some combinations that Xcalia XIC allowed.
 * This string form isn't necessarily the "id.toString()" form - it was added to allow migration from Xcalia XIC.
 * Handles the following String forms :-
 * <ul>
 * <li>{fully-qualified-class-name}:{key}</li>
 * <li>{discriminator}:{key}</li>
 * </ul>
 * The "key" is either
 * <ul>
 * <li>datastore-identity : the key of the object e.g 12345</li>
 * <li>application-identity : the toString() output from the PK</li>
 * </ul>
 */
public class XcaliaIdentityStringTranslator implements IdentityStringTranslator
{
    /* (non-Javadoc)
     * @see org.datanucleus.identity.IdentityStringTranslator#getIdentity(org.datanucleus.ExecutionContext, java.lang.String)
     */
    public Object getIdentity(ExecutionContext ec, String stringId)
    {
        ClassLoaderResolver clr = ec.getClassLoaderResolver();

        // a). find the first part before any colon, and try as class name
        // b). try the first part as discriminator
        Object id = null;
        int idStringPos = stringId.indexOf(':');
        if (idStringPos > 0)
        {
            String definer = stringId.substring(0, idStringPos);
            String idKey = stringId.substring(idStringPos+1);
            AbstractClassMetaData acmd = null;
            try
            {
                // See if this is the className
                clr.classForName(definer);
                acmd = ec.getMetaDataManager().getMetaDataForClass(definer, clr);
            }
            catch (ClassNotResolvedException cnre)
            {
                // Not a class so maybe a discriminator
                acmd = ec.getMetaDataManager().getMetaDataForDiscriminator(definer);
            }

            if (acmd != null)
            {
                if (acmd.getIdentityType() == IdentityType.DATASTORE)
                {
                    // "idKey" assumed to be the key of the datastore-identity
                    try
                    {
                        Long keyLong = Long.valueOf(idKey);
                        id = OIDFactory.getInstance(ec.getNucleusContext(), acmd.getFullClassName(), keyLong);
                    }
                    catch (NumberFormatException nfe)
                    {
                        // Maybe is String based
                        id = OIDFactory.getInstance(ec.getNucleusContext(), acmd.getFullClassName(), idKey);
                    }
                }
                else if (acmd.getIdentityType() == IdentityType.APPLICATION)
                {
                    // "idKey" assumed to be the toString() output of the application-identity
                    id = IdentityUtils.getNewApplicationIdentityObjectId(clr, acmd, idKey);
                }
            }
        }
        else
        {
            // Maybe is an OID.toString() ?
            // TODO Support this
        }
        return id;
    }
}