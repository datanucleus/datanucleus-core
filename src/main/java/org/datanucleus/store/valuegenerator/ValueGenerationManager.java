/**********************************************************************
Copyright (c) 2017 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.valuegenerator;

import java.util.Properties;

import org.datanucleus.metadata.AbstractClassMetaData;
import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Manager for the creation of ValueGenerators.
 * ValueGenerators are of two primary types.
 * <ul>
 * <li><b>unique</b> : apply to any datastore, and generate unique values. For example, UUID, which generates the values in Java space.</li>
 * <li><b>datastore</b> : apply to a particular datastore, and member. For example, an RDBMS SEQUENCE.</li>
 * </ul>
 * Any unique generators are loaded at initialisation. Any datastore generators are loaded when required.
 * All generators are cached once created, and can be looked up by the member "key" that they are for.
 * 
 * <h3>Member Key</h3>
 * The member "key" is either the fully-qualified member name (e.g "mydomain.MyClass.myField") that is having its values generated, or
 * is for a (surrogate) datastore id member (e.g "mydomain.MyClass (datastore-id)").
 * All unique generators can also be looked up by the strategy name (since there is one instance of that generator per strategy.
 */
public interface ValueGenerationManager
{
    /**
     * Method to clear out the generators managed by this manager.
     */
    void clear();

    /**
     * Method to access the currently defined ValueGenerator for the specified member "key" (if any).
     * @param memberKey The member "key"
     * @return Its ValueGenerator
     */
    ValueGenerator getValueGeneratorForMemberKey(String memberKey);

    /**
     * Method to store a ValueGenerator for the specified member "key".
     * @param memberKey The member "key"
     * @param generator The ValueGenerator to use for that member key
     */
    void registerValueGeneratorForMemberKey(String memberKey, ValueGenerator generator);

    /**
     * Accessor for the "unique" ValueGenerator for the specified name (if any).
     * @param name The (strategy) name.
     * @return The ValueGenerator for that name
     */
    ValueGenerator getUniqueValueGeneratorByName(String name);

    /**
     * Simple way of generating a member "key" for use in lookups for datastore-identity.
     * @param cmd Metadata for the class using datastore-identity
     * @return The member "key" to use
     */
    String getMemberKey(AbstractClassMetaData cmd);

    /**
     * Simple way of generating a member "key" for use in lookups.
     * @param mmd Metadata for the member
     * @return The member "key" to use
     */
    String getMemberKey(AbstractMemberMetaData mmd);

    /**
     * Method to create and register a generator of the specified strategy, for the specified memberKey.
     * @param memberKey The member key
     * @param strategyName Strategy for the generator
     * @param props The properties to use
     * @return The ValueGenerator
     */
    ValueGenerator createAndRegisterValueGenerator(String memberKey, String strategyName, Properties props);

    /**
     * Accessor for the type of value that is generated by the ValueGenerator for the specified strategy, for the member "key".
     * @param strategyName The value generation strategy
     * @param memberKey The member "key"
     * @return The type of value generated
     */
    Class getTypeForValueGeneratorForMember(String strategyName, String memberKey);

    /**
     * Convenience accessor for whether the specified strategy is supported for this datastore.
     * @param strategy The strategy name
     * @return Whether it is supported
     */
    boolean supportsStrategy(String strategy);

    /**
     * Method to create a ValueGenerator when the generator is datastore based.
     * This is used solely by the NucleusSequence API to create a generator, but not to register it here for further use.
     * @param strategyName Strategy name
     * @param seqName Symbolic name of the generator
     * @param props Properties to control the generator
     * @param connectionProvider Provider for connections
     * @return The ValueGenerator
     */
    ValueGenerator createValueGenerator(String strategyName, String seqName, Properties props, ValueGenerationConnectionProvider connectionProvider);
}