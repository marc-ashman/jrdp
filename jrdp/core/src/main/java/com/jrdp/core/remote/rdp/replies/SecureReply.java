/*
 * Copyright (C) 2013 JRDP Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jrdp.core.remote.rdp.replies;

public class SecureReply extends RdpReply
{
	public static final short SEC_ENCRYPT = 0x0008;
	public static final short SEC_LICENSE_PKT = 0x0080;
	
	private int secureUserDataLength;
	private byte[] mac;
	private short securityFlags;

	public SecureReply()
	{
		super();
	}
	
	public SecureReply(RdpReply reply)
	{
		super(reply);
	}
	
	public void setSecureUserDataLength(int length)
	{
		secureUserDataLength = length;
	}
	
	public int getSecureUserDataLength()
	{
		return secureUserDataLength;
	}
	
	public void setMac(byte[] mac)
	{
		this.mac = mac;
	}
	
	public byte[] getMac()
	{
		return mac;
	}
	
	public void setSecurityFlags(short securityFlags)
	{
		this.securityFlags = securityFlags;
	}
	
	public short getSecurityFlags()
	{
		return securityFlags;
	}
	
	public boolean isEncypted()
	{
		return (securityFlags & SEC_ENCRYPT) == SEC_ENCRYPT;
	}
	
	public boolean isLicensingPdu()
	{
		return (securityFlags & SEC_LICENSE_PKT) == SEC_LICENSE_PKT;
	}
	
	public void setSecurityHeaderLength(int securityHeaderLength) {
		secureUserDataLength -= securityHeaderLength;
	}
}
