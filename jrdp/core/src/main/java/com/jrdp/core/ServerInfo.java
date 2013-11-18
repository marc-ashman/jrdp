package com.jrdp.core;

public class ServerInfo
{
	private String server;
	private int port;
	
	public ServerInfo(String server, int port)
	{
		this.server = server;
		this.port = port;
	}
	
	public String getServer()
	{
		return server;
	}
	
	public int getPort()
	{
		return port;
	}
}
