/**********************************************************************
Copyright (c) 2006 Andy Jefferson and others. All rights reserved.
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

import org.datanucleus.ClassLoaderResolver;
import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.PackageMetaData;

/**
 * Interface defining the access to MetaData derived from Annotations.
 */
public interface AnnotationManager
{
    /**
     * Method to get the ClassMetaData for a class from its annotations.
     * @param cls The class
     * @param pmd MetaData for the owning package (that this will be a child of)
     * @param clr Resolver for ClassLoader issues
     * @return The ClassMetaData (unpopulated and unitialised)
     */
    public AbstractClassMetaData getMetaDataForClass(Class cls, PackageMetaData pmd, ClassLoaderResolver clr);

    public boolean getClassAnnotationHasHandler(String annotationName);

    public boolean getMemberAnnotationHasHandler(String annotationName);

    /**
     * Accessor for a handler for the specified class annotation.
     * @param annotationName Annotation to be handled.
     * @return The handler (if any). Takes the first one, so only register one handler
     */
    public ClassAnnotationHandler getHandlerForClassAnnotation(String annotationName);

    /**
     * Accessor for a handler for the specified member annotation.
     * @param annotationName Annotation to be handled
     * @return The handler (if any). Takes the first one, so only register one handler
     */
    public MemberAnnotationHandler getHandlerForMemberAnnotation(String annotationName);
}