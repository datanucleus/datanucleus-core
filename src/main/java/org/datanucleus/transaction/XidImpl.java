/**********************************************************************
Copyright (c) 2007 Erik Bengtson and others. All rights reserved. 
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
package org.datanucleus.transaction;

public class XidImpl implements javax.transaction.xa.Xid
{
    byte[] branchQualifierBytes;
    int formatId;
    byte[] globalTransactionIdBytes;

    public XidImpl(int branchQualifierBytes, int formatId, byte[] globalTransactionIdBytes)
    {
        byte[] buf = new byte[4];
        buf[0] = (byte) ((branchQualifierBytes >>> 24) & 0xFF);
        buf[1] = (byte) ((branchQualifierBytes >>> 16) & 0xFF);
        buf[2] = (byte) ((branchQualifierBytes >>> 8) & 0xFF);
        buf[3] = (byte) (branchQualifierBytes & 0xFF);
        this.branchQualifierBytes = buf;
        this.formatId = formatId;
        this.globalTransactionIdBytes = globalTransactionIdBytes;
    }    
    
    public XidImpl(int branchQualifierBytes, int formatId, int globalTransactionIdBytes)
    {
        byte[] buf = new byte[4];
        buf[0] = (byte) ((branchQualifierBytes >>> 24) & 0xFF);
        buf[1] = (byte) ((branchQualifierBytes >>> 16) & 0xFF);
        buf[2] = (byte) ((branchQualifierBytes >>> 8) & 0xFF);
        buf[3] = (byte) (branchQualifierBytes & 0xFF);
        this.branchQualifierBytes = buf;
        this.formatId = formatId;
        buf = new byte[4];
        buf[0] = (byte) ((globalTransactionIdBytes >>> 24) & 0xFF);
        buf[1] = (byte) ((globalTransactionIdBytes >>> 16) & 0xFF);
        buf[2] = (byte) ((globalTransactionIdBytes >>> 8) & 0xFF);
        buf[3] = (byte) (globalTransactionIdBytes & 0xFF);
        this.globalTransactionIdBytes = buf;
    }

    public byte[] getBranchQualifier()
    {
        return branchQualifierBytes;
    }

    public int getFormatId()
    {
        return formatId;
    }

    public byte[] getGlobalTransactionId()
    {
        return globalTransactionIdBytes;
    }

    // TODO Make this human-readable
    public String toString()
    {
        return "Xid="+new String(globalTransactionIdBytes);
    }
}
