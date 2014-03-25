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
package org.datanucleus.store.schema.table;

import java.util.List;

import org.datanucleus.metadata.AbstractMemberMetaData;
import org.datanucleus.store.types.converters.TypeConverter;

/**
 * Representation of a verifier for schema information.
 * Provides a means for a store plugin to override the default TypeConverter usage, as well as allowing it to set 
 */
public interface SchemaVerifier
{
    /**
     * Method to allow the verifier to approve, override, or null out the TypeConverter to be used for a member.
     * @param mmd Metadata for the member
     * @param conv The default TypeConverter
     * @return The TypeConverter that the verifier wishes to use for this member
     */
    TypeConverter verifyTypeConverterForMember(AbstractMemberMetaData mmd, TypeConverter conv);

    /**
     * Method called when the specified member has its column(s) created.
     * @param mapping Member-column mapping
     * @param mmd The member metadata that this relates to, or null if this represents a surrogate (datastore id, version etc).
     */
    void attributeMember(MemberColumnMapping mapping, AbstractMemberMetaData mmd);

    /**
     * Method called when the specified surrogate member has its column(s) created.
     * @param mapping Member-column mapping
     */
    void attributeMember(MemberColumnMapping mapping);

    /**
     * Method called when the specified embedded member has its column(s) created.
     * @param mapping The column
     * @param mmds The member metadata(s), allowing navigation
     */
    void attributeEmbeddedMember(MemberColumnMapping mapping, List<AbstractMemberMetaData> mmds);
}