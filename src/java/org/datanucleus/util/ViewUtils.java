/**********************************************************************
Copyright (c) 2004 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.datanucleus.exceptions.NucleusUserException;

/**
 * Utilities for handling Views.
 * TODO Move to RDBMS
 */
public class ViewUtils
{
    protected static final Localiser LOCALISER=Localiser.getInstance("org.datanucleus.Localisation",
        org.datanucleus.ClassConstants.NUCLEUS_CONTEXT_LOADER);

    /**
     * Check for any circular view references between referencer and referencee.
     * If one is found, throw a NucleusUserException with the chain of references.
     * @param viewReferences The Map of view references to check.
     * @param referencer_name Name of the class that has the reference.
     * @param referencee_name Name of the class that is being referenced.
     * @param referenceChain The List of class names that have been referenced
     * @throws NucleusUserException If a circular reference is found in the view definitions.
     */
    public static void checkForCircularViewReferences(Map viewReferences,
                                                      String referencer_name,
                                                      String referencee_name,
                                                      List referenceChain)
    {
        Set class_names = (Set)viewReferences.get(referencee_name);
        if (class_names != null)
        {
            // Initialize the chain of references if needed.  Add the referencee
            // to the chain.
            if (referenceChain == null)
            {
                referenceChain = new ArrayList();
                referenceChain.add(referencer_name);
            }
            referenceChain.add(referencee_name);

            // Iterate through all referenced classes from the referencee.  If
            // any reference the referencer, throw an exception.
            for (Iterator it=class_names.iterator(); it.hasNext(); )
            {
                String current_name=(String)it.next();
                if (current_name.equals(referencer_name))
                {
                    StringBuilder error=new StringBuilder(LOCALISER.msg("031003"));

                    for (Iterator chainIter=referenceChain.iterator(); chainIter.hasNext(); )
                    {
                        error.append(chainIter.next());
                        if (chainIter.hasNext())
                        {
                            error.append(" -> ");
                        }
                    }

                    throw new NucleusUserException(error.toString()).setFatal();
                }
                else
                {
                    // Make recursive call to check for any nested dependencies.
                    // e.g A references B, B references C, C references A.
                    checkForCircularViewReferences(viewReferences,
                                                   referencer_name,
                                                   current_name,
                                                   referenceChain);
                }
            }
        }
    }
}