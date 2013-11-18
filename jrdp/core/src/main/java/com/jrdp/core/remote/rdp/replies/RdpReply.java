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
