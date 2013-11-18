package com.jrdp.core.remote.rdp;

import com.jrdp.core.ServerInfo;

public class RdpServerInfo extends ServerInfo
{
	private boolean supportsExtendedClientDataBlocks = false;
	private int securityProtocol;

	public RdpServerInfo(String server, int port)
	{
		super(server, port);
	}
	
	public boolean isSupportsExtendedClientDataBlocks()
	{
		return supportsExtendedClientDataBlocks;
	}

	public void setSupportsExtendedClientDataBlocks(
			boolean supportsExtendedClientDataBlocks)
	{
		this.supportsExtendedClientDataBlocks = supportsExtendedClientDataBlocks;
	}
	
	public void setSecurityProtocol(int protocol)
	{
		securityProtocol = protocol;
	}
	
	public int getSecrityProtocol()
	{
		return securityProtocol;
	}
}
