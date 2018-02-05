/**********************************************************************
Copyright (c) 2014 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.metadata.annotations;

import java.util.Map;

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.metadata.ColumnMetaData;

/**
 * Handler for the javax.validation @Size annotation.
 */
public class ValidationSizeAnnotationHandler implements MemberAnnotationHandler
{
    /* (non-Javadoc)
     * @see org.datanucleus.metadata.annotations.MemberAnnotationHandler#processMemberAnnotation(org.datanucleus.metadata.annotations.AnnotationObject, org.datanucleus.metadata.AbstractMemberMetaData, org.datanucleus.ClassLoaderResolver)
     */
    @Override
    public void processMemberAnnotation(AnnotationObject annotation, AbstractMemberMetaData mmd, ClassLoaderResolver clr)
    {
        Map<String, Object> annotationValues = annotation.getNameValueMap();
        int max = (Integer)annotationValues.get("max");
        ColumnMetaData[] colmds = mmd.getColumnMetaData();
        // TODO This should only be processed when the member is STRING. Currently dont have access to that info
        if (colmds == null || colmds.length == 0)
        {
            ColumnMetaData colmd = new ColumnMetaData();
            colmd.setLength(max);
            mmd.addColumn(colmd);
            return;
        }

        if (colmds[0].getLength() == null)
        {
            // No length set, so 
            colmds[0].setLength(max);
        }
    }
}