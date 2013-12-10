/**********************************************************************
Copyright (c) 2011 Andy Jefferson and others. All rights reserved.
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
package org.datanucleus.store.encryption;

import org.datanucleus.metadata.AbstractMemberMetaData;

/**
 * Interface to be implemented by any persistence encryption provider.
 */
public interface PersistenceEncryptionProvider
{
    /**
     * Method to encrypt the provided value for persistence.
     * @param mmd Metadata for the member (field/property).
     * @param value Its value to encrypt.
     * @return The encrypted value
     */
    Object encryptValue(AbstractMemberMetaData mmd, Object value);

    /**
     * Method to decrypt the provided value from persistence.
     * @param mmd Metadata for the member (field/property).
     * @param value Its value to decrypt.
     * @return The decrypted value
     */
    Object decryptValue(AbstractMemberMetaData mmd, Object value);
}