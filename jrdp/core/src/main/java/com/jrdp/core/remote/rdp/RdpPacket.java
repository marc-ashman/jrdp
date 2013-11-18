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

import com.jrdp.core.encryption.EncryptionSession;

import java.util.Vector;

class RdpPacket
{
	private Vector<PacketPeice> peices;
	private int length;
	
	public RdpPacket()
	{
		peices = new Vector<PacketPeice>();
	}

	public void addPacketPeiceAtEnd(byte[] peice)
	{
		addPacketPeiceAtEnd(peice, null);
	}

	public void addPacketPeiceAtEnd(byte[] peice, EncryptionSession crypto)
	{
		peices.add(new PacketPeice(peice, crypto));
		length += peice.length;
	}

	public void addPacketPeiceAtStart(byte[] peice)
	{
		addPacketPeiceAtStart(peice, null);
	}
	
	public void addPacketPeiceAtStart(byte[] peice, EncryptionSession crypto)
	{
		if(peice.length == 0)
			return;
		peices.add(0, new PacketPeice(peice, crypto));
		length += peice.length;
	}
	
	/**
	 * returns all peices packed together into a single array. Doesn't perform any encryption.
	 */
	public byte[] packPeices()
	{
		if(peices.size() == 1)
		{
			return peices.elementAt(0).getPeice();
		}
		else
		{
			byte[] data = new byte[length];
			int n = 0;
			for(int i=0; i < peices.size(); i++)
			{
				byte[] peice = peices.elementAt(i).getPeice();
				System.arraycopy(peice, 0, data, n, peice.length);
				n += peice.length;
			}
			return data;
		}
	}
	
	/**
	 * Returns all peices packed together into a single array. Performs all encryption.<br>
	 * Also warns all used CryptoEngines that they have been used for a single packet.
	 */
	public byte[] finalizePacket()
	{
		Vector<EncryptionSession> encryptionsToPerform = new Vector<EncryptionSession>();
		int numPeices = peices.size();
		byte[] data = new byte[length];
		int k = 0;
		for(int i=0; i < numPeices; i++)
		{
			PacketPeice peice = peices.elementAt(i);
			EncryptionSession engine = peice.getEncryptionSession();
			if(engine != null)
				encryptionsToPerform.add(engine);
			int numEncryptions = encryptionsToPerform.size();
			if(numEncryptions == 0)
			{
				byte[] peiceData = peice.getPeice();
				System.arraycopy(peiceData, 0, data, k, peiceData.length);
				k += peiceData.length;
			}
			else
			{
				byte[] peiceData = peice.getPeice();
	//System.out.println("******************************************");
	//StringManip.print(peiceData, "Peice: ");
				for(int n=0; n < numEncryptions; n++)
				{
					engine = encryptionsToPerform.elementAt(n);
					peiceData = engine.encrypt(peiceData, 0, peiceData.length);
				}
	//if(numEncryptions > 0)
	//	StringManip.print(peiceData, "Encrypted: ");
				System.arraycopy(peiceData, 0, data, k, peiceData.length);
				k += peiceData.length;
			}
		}
		return data;
	}
	
	public void reset()
	{
		peices.clear();
		length = 0;
	}
	
	public int getPeiceCount()
	{
		return peices.size();
	}
	
	public byte[] getPeice(int index)
	{
		return peices.elementAt(index).getPeice();
	}
	
	public int getLength()
	{
		return length;
	}
	
	private class PacketPeice
	{
		private byte[] peice;
		private EncryptionSession crypto;
		
		PacketPeice(byte[] peice, EncryptionSession crypto)
		{
			this.peice = peice;
			this.crypto = crypto;
		}
		
		public byte[] getPeice()
		{
			return peice;
		}
		
		public EncryptionSession getEncryptionSession()
		{
			return crypto;
		}
	}
}