package com.jrdp.core.remote.rdp.replies;

public class RdpReply
{
	private short length;
	private int X224PacketLength;
	private boolean isFastPath = false;

	public RdpReply()
	{
		
	}
	
	public RdpReply(RdpReply reply)
	{
		length = reply.length;
		X224PacketLength = reply.X224PacketLength;
	}
	
	public void setX224Length(int MCSPacketLength)
	{
		this.X224PacketLength = MCSPacketLength;
	}
	
	public int getX224PacketLength()
	{
		return X224PacketLength;
	}
	
	public void setPacketLength(short length)
	{
		this.length = length;
	}
	
	public short getPacketLength()
	{
		return length;
	}
	
	public void setIsFastPath(boolean isFastPath)
	{
		this.isFastPath = isFastPath;
	}
	
	public boolean isFastPath()
	{
		return isFastPath;
	}
}
