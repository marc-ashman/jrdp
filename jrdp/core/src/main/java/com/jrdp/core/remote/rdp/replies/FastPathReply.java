package com.jrdp.core.remote.rdp.replies;

public class FastPathReply extends RdpReply
{
	private boolean encrypted;
	
	public FastPathReply()
	{
		
	}
	
	public boolean isEncrypted()
	{
		return encrypted;
	}
	
	public void setEncrypted(boolean encrypted)
	{
		this.encrypted = encrypted;
	}
}
