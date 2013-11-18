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
