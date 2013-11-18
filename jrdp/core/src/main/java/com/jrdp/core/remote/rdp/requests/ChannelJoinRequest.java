package com.jrdp.core.remote.rdp.requests;

public class ChannelJoinRequest extends RdpRequest
{
	private short channelId;
	
	public ChannelJoinRequest(short channelId)
	{
		this.channelId = channelId;
	}
	
	public short getChannelId()
	{
		return channelId;
	}
}
