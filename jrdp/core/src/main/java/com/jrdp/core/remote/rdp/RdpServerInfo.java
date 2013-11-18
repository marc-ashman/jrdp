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
