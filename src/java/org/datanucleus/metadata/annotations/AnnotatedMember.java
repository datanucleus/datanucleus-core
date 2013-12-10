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

/**
 * Representation of a field/method that is annotated.
 * Used by AbstractAnnotationReader to hold the annotations for a field/method when extracting them 
 * for the class.
 */
class AnnotatedMember
{
    /** The Field/Method */
    Member member;

    /** Annotations for field/method */
    AnnotationObject[] annotations;

    /**
     * Constructor.
     * @param field The field
     * @param annotations The annotation objects
     */
    public AnnotatedMember(Member field, AnnotationObject[] annotations)
    {
        this.member = field;
        this.annotations = annotations;
    }
    
    /**
     * Accessor for the field name
     * @return The name
     */
    public String getName()
    {
        return member.getName();
    }

    /**
     * Accessor for the field / method.
     * @return field/method information
     */
    public Member getMember()
    {
        return member;
    }

    /**
     * Accessor for the annotations
     * @return The annotation objects
     */
    public AnnotationObject[] getAnnotations()
    {
        return annotations;
    }

    /**
     * Method to add more annotations for this field.
     * @param annotations Annotations to add
     */
    public void addAnnotations(AnnotationObject[] annotations)
    {
        if (this.annotations == null)
        {
            this.annotations = annotations;
        }
        else
        {
            AnnotationObject[] newAnnotations = new AnnotationObject[this.annotations.length + annotations.length];
            int pos = 0;
            for (int i=0;i<this.annotations.length;i++)
            {
                newAnnotations[pos++] = this.annotations[i];
            }
            for (int i=0;i<annotations.length;i++)
            {
                newAnnotations[pos++] = annotations[i];
            }
            this.annotations = newAnnotations;
        }
    }

    @Override
    public String toString()
    {
        return member.getName();
    }
}