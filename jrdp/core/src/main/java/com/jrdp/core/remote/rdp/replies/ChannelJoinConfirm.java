package com.jrdp.core.remote.rdp.replies;

public class ChannelJoinConfirm extends RdpReply
{
	private short channelId;
	
	public ChannelJoinConfirm(short channelId)
	{
		super();
		this.channelId = channelId;
	}
	
	public ChannelJoinConfirm(RdpReply reply)
	{
		super(reply);
	}
	
	public short getChannelId()
	{
		return channelId;
	}
}
