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

package com.jrdp.core.remote.rdp;

public class RdpConnectionInfo
{
	//Core
	private String username;
	private short width;
	private short height;
	private short requestedColorDepth;
	private byte connectionType;
	//Security
	private boolean isFrenchLocale;
	//Client Info
	private String domain;
	private String password;
	private String ipAddress;
	private int timeZoneBias;
	private int performanceFlags;

	/**
	 * @param username the username of the user logging in
	 * @param password password of user's account
	 * @param domain domain where user's account is located
	 * @param ipAddress string representation of the client's ip address
	 * @param requestedEncryptionLevel requested encryption level of the client
	 * @param timeZoneBias number of minutes difference from UTC for the client's timezone 
	 * (UTC = local time + bias)
	 * @param performanceFlags bitmask defining performance related flags.
	 * @param width screen width of client
	 * @param height screen height of client
	 * @param requestedColorDepth requested color depth of the client
	 * @param compressionType type of compression for the server to send data to the client. 
	 * Can be 15, 16, or 24
	 * @param connectionType connection type that the client is running under
	 * @param isFrenchLocale specifies if client is located in France (crypto laws)
	 */
	public RdpConnectionInfo(String username, String password, String domain, String ip,
			int timeZoneBias, int performanceFlags, short width, short height, 
			short requestedColorDepth, byte connectionType, boolean isFrenchLocale)
	{
		this.username = username;
		this.password = password;
		this.domain = domain;
		this.ipAddress = ip;
		this.timeZoneBias = timeZoneBias;
		this.performanceFlags = performanceFlags;
		this.width = width;
		this.height = height;
		this.requestedColorDepth = requestedColorDepth;
		this.connectionType = connectionType;
		this.isFrenchLocale = isFrenchLocale;
	}
	
	public String getUsername()
	{
		return username;
	}

	public short getWidth()
	{
		return width;
	}

	public short getHeight()
	{
		return height;
	}

	public short getRequestedColorDepth()
	{
		return requestedColorDepth;
	}

	public byte getConnectionType()
	{
		return connectionType;
	}

	public boolean isFrenchLocale()
	{
		return isFrenchLocale;
	}
	
	public String getDomain()
	{
		return domain;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public String getIpAddress()
	{
		return ipAddress;
	}
	
	public int getTimeZoneBias()
	{
		return timeZoneBias;
	}
	
	public int getPerformanceFlags()
	{
		return performanceFlags;
	}
}
