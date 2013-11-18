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

import com.jrdp.core.remote.rdp.replies.FastPathReply;
import com.jrdp.core.remote.rdp.replies.RdpReply;
import com.jrdp.core.remote.rdp.requests.RdpRequest;
import com.jrdp.core.util.BitManip;
import com.jrdp.core.util.InputByteStream;
import com.jrdp.core.util.Logger;


class X224
{
	static final int PROTOCOL_RDP = 0x00000000;
	static final int PROTOCOL_SSL = 0x00000001;
	static final int PROTOCOL_HYBRID = 0x00000002;
	private static final byte TPKT_VERSION = 0x03;
	static final int TPKT_HEADER_SIZE = 4;
	/**
	 * Make initial connection (Step 1)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240470(PROT.10).aspx>
	 * 2.2.1.1 Client X.224 Connection Request PDUM</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240842(PROT.10).aspx>
	 * 4.1.1 Client X.224 Connection Request PDU</a>
	 */
	static final int CONNECTION_REQUEST = 1;
	/**
	 * Read connection confirmation (Step 2)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240501(PROT.10).aspx>
	 * 2.2.1.2 Server X.224 Connection Confirm PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240922(PROT.10).aspx>
	 * 4.1.2 Server X.224 Connection Confirm PDU</a>
	 */
	static final int CONNECTION_CONFIRM = 2;
	/**
	 * MCS Initial Connection (Step 3)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240508(v=PROT.10).aspx>
	 * 2.2.1.3 Client MCS Connect Initial PDU with GCC Conference Create Request</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240836(v=PROT.10).aspx>
	 * 4.1.3 Client MCS Connect Initial PDU with GCC Conference Create Request</a>
	 */
	static final int MCS_CONNECT_INITIAL = 3;
	/**
	 * MCS Initial Connection Response (Step 4)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240515(v=PROT.10).aspx>
	 * 2.2.1.4 Server MCS Connect Response PDU with GCC Conference Create Response<a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240919(v=PROT.10).aspx>
	 * 4.1.4 Server MCS Connect Response PDU with GCC Conference Create Response</a>
	 */
	static final int MCS_CONNECT_RESPONSE = 4;
	/**
	 * MCS Erect Domain Request (Step 5)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240523(v=PROT.10).aspx>
	 * 2.2.1.5 Client MCS Erect Domain Request PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240837(v=PROT.10).aspx>
	 * 4.1.5 Client MCS Erect Domain Request PDU</a>
	 */
	static final int ERECT_DOMAIN_REQUEST = 5;
	/**
	 * MCS Attach User Request (Step 6)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240524(v=PROT.10).aspx>
	 * 2.2.1.6 Client MCS Attach User Request PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240835(v=PROT.10).aspx>
	 * 4.1.6 Client MCS Attach User Request PDU</a>
	 */
	static final int ATTACH_USER_REQUEST = 6;
	/**
	 * MCS Attach User Confirm (Step 7)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240525(v=PROT.10).aspx>
	 * 2.2.1.7 Server MCS Attach User Confirm PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240918(v=PROT.10).aspx>
	 * 4.1.7 Server MCS Attach-User Confirm PDU</a>
	 */
	static final int ATTACH_USER_CONFIRM = 7;
	/**
	 * MCS Channel Join Request (Step 8)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240526(v=PROT.10).aspx>
	 * 2.2.1.8 Client MCS Channel Join Request PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240834(v=PROT.10).aspx>
	 * 4.1.8.1.1 Client Join Request PDU for Channel 1007 (User Channel)</a>
	 */
	static final int CHANNEL_JOIN_REQUEST = 8;
	/**
	 * MCS Channel Join Confirm (Step 9)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240527(v=PROT.10).aspx>
	 * 2.2.1.9 Server MCS Channel Join Confirm PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240915(v=PROT.10).aspx>
	 * 4.1.8.1.2 Server Join Confirm PDU for Channel 1007 (User Channel)</a>
	 */
	static final int CHANNEL_JOIN_CONFIRM = 9;
	/**
	 * Client Security Echange (Step 10)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240471(v=PROT.10).aspx>
	 * 2.2.1.10 Client Security Exchange PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240839(v=PROT.10).aspx>
	 * 4.1.9 Client Security Exchange PDU</a>
	 */
	static final int CLIENT_SECURITY_EXCHANGE = 10;
	/**
	 * Client Info Data (Step 11)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240473(v=PROT.10).aspx>
	 * 2.2.1.11 Client Info PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240829(v=PROT.10).aspx>
	 * 4.1.10 Client Info PDU</a>
	 */
	static final int CLIENT_INFO_DATA = 11;
	/**
	 * Server License Error PDU (Step 12)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240479(v=PROT.10).aspx>
	 * 2.2.1.12 Server License Error PDU - Valid Client</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240916(v=PROT.10).aspx>
	 * 4.1.11 Server License Error PDU - Valid Client</a>
	 */
	static final int SERVER_LICENSE_ERROR_PDU = 12;
	/**
	 * Server Demand Active PDU (Step 13)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240484(v=PROT.10).aspx>
	 * 2.2.1.13.1 Server Demand Active PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240909(v=PROT.10).aspx>
	 * 4.1.12 Server Demand Active PDU</a>
	 */
	static final int SERVER_DEMAND_ACTIVE_PDU = 13;
	/**
	 * Client Confirm Active PDU (Step 14)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240487(v=PROT.10).aspx>
	 * 2.2.1.13.2 Client Confirm Active PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240825(v=PROT.10).aspx>
	 * 4.1.13 Client Confirm Active PDU</a>
	 */
	static final int CONFIRM_ACTIVE_PDU = 14;
	/**
	 * Client Synchronize PDU (Step 15)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240489(v=PROT.10).aspx>
	 * 2.2.1.14 Client Synchronize PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240841(v=PROT.10).aspx>
	 * 4.1.14 Client Synchronize PDU</a>
	 */
	static final int SYNCHRONIZE_PDU = 15;
	/**
	 * Client Control PDU - Cooperate (Step 16)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240491(v=PROT.10).aspx>
	 * 2.2.1.15 Client Control PDU - Cooperate</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240826(v=PROT.10).aspx>
	 * 4.1.15 Client Control PDU - Cooperate</a>
	 */
	static final int CONTROL_PDU_COOPERATE = 16;
	/**
	 * Client Control PDU - Request Control (Step 17)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240493(v=PROT.10).aspx>
	 * 2.2.1.16 Client Control PDU - Request Control</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240827(v=PROT.10).aspx>
	 * 4.1.16 Client Control PDU - Request Control</a>
	 */
	static final int CONTROL_PDU_REQUEST_CONTROL = 17;
	/**
	 * Client Persistent Key List PDU (Step 18)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240494(v=PROT.10).aspx>
	 * 2.2.1.17 Client Persistent Key List PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240838(v=PROT.10).aspx>
	 * 4.1.17 Client Persistent Key List PDU</a>
	 */
	static final int PERSISTENT_KEY_LIST_PDU = 18;
	/**
	 * Client Persistent Key List PDU (Step 19)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240452(PROT.10).aspx>
	 * 1.3.1.1 Connection Sequence</a>
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240497(v=PROT.10).aspx>
	 * 2.2.1.18 Client Font List PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240828(v=PROT.10).aspx>
	 * 4.1.18 Client Font List PDU</a>
	 */
	static final int FONT_LIST_PDU = 19;
	static final int UNKNOWN_PDU = 20;
	static final int INPUT_EVENT = 21;
	
	protected NetworkManager net;
	protected RdpConnectionInfo info;
	protected int securityProtocol;
	
	public X224(NetworkManager net, RdpConnectionInfo info)
	{
		this.net = net;
		this.info = info;
	}
	
	/**
	 * Creates a packet defined by the RDP protocol
	 * @param type - packet type
	 * @return the resulting packet, or null on failure
	 */
	public final void sendPacket(int type)
	{
		sendPacket(type, null, new RdpPacket());
	}

	/**
	 * Creates a packet defined by the RDP protocol
	 * @param type - packet type
	 * @param request - contains extra information that the certain packet type may need
	 * @return the resulting packet, or null on failure
	 */
	public final void sendPacket(int type, RdpRequest request)
	{
		//TODO: get some kind of success/fail to the following call
		sendPacket(type, request, new RdpPacket());
	}
	
	protected void sendPacket(int type, RdpRequest request, RdpPacket packet)
	{
		//create connection request PDU
		switch(type)
		{
			case CONNECTION_REQUEST:
				packet.addPacketPeiceAtStart(createx224ConnectionRequestPDU());
				break;
			default:
				packet.addPacketPeiceAtStart(createx224DataPDU());
				break;
		}
		//attach tptk header to all packets
		packet.addPacketPeiceAtStart(createTpktHeader(packet.getLength()));
		net.send(packet);
	}
	
	public synchronized final RdpReply readPacket(int type)
	{
		return readPacket(type, net.getPacket());
	}
	
	public final RdpReply processPacket(InputByteStream packet)
	{
		return readPacket(UNKNOWN_PDU, packet);
	}
	
	protected FastPathReply processFastPathPacket(FastPathReply reply, InputByteStream packet)
	{
		reply.setIsFastPath(true);
		int inputHeader = (packet.getByte() & 0xff);
		boolean encrypted = (inputHeader & 0x80) == 0x80;
		reply.setEncrypted(encrypted);
		int lengthByte1 = (packet.getByte() & 0xff);
		short length = 0;
		if((lengthByte1 & 0x80) == 0x80)
		{
			int lengthByte2 = (packet.getByte() & 0xff);
			length = BitManip.mergeToShort((byte) (lengthByte1 & 0x7f), (byte) lengthByte2);
		}
		else
		{
			length = (short) lengthByte1;
		}
		reply.setPacketLength(length);
		//TODO: FIPS here
		if(encrypted)
			packet.skip(8);
		return reply;
	}
	
	/**
	 * Read a packet from the server
	 * @param type packet type to read
	 * @param reply reply structure to store data.
	 */
	protected RdpReply readPacket(int type, InputByteStream packet)
	{
		if(packet.peek() != TPKT_VERSION)
		{
			return processFastPathPacket(new FastPathReply(), packet);
		}
		short length = readTpktHeader(packet.getByteArray(TPKT_HEADER_SIZE));
		if(length == -1)
		{
			Logger.log(Logger.ERROR, "Invalid tpkt header received");
			return null;
		}
		//set entire packet length (including 4 bytes for tpkt header)
		RdpReply reply = new RdpReply();
		reply.setPacketLength(length);
		if(type != CONNECTION_CONFIRM)
			readx224DataTPDU(packet, reply);
		if(type == CONNECTION_CONFIRM)
		{
			if(!receivex224ConnectionConfirmPDU(packet))
				return null;
			//-1 because reply's length doesn't take in account the LI field (see [x224] Sec 13.2.1)
			if(!receiveRdpNegotiationResponse(packet))
				return null;
		}
		return reply;
	}
	
	/**
	 * @see [X224] Section 13.3
	 */
	private byte[] createx224ConnectionRequestPDU()
	{
		String username = info.getUsername();
		if(username.length() > 9)
			username = username.substring(0, 9);
		String cookie = "Cookie: mstshash=" + username + "\r\n";
		int size = 7 + cookie.length();
		byte[] value = new byte[7 + cookie.length()];
		value[0] = (byte) (size - 1);	//LI - length indicator (excluding the LI's length)
		value[1] = (byte) 0xe0;			//CR_CDT
		value[2] = 0x00;				//DST_REF
		value[3] = 0x00;
		value[4] = 0x00;				//SRC_REF
		value[5] = 0x00;				//SRC_REF
		value[6] = 0x00;				//CLASS_OPTION
		System.arraycopy(cookie.getBytes(), 0, value, 7, cookie.length());
		return value;
	}
	
	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240501(PROT.10).aspx>
	 * 2.2.1.2 Server X.224 Connection Confirm PDU</a>
	 */
	private boolean receivex224ConnectionConfirmPDU(InputByteStream packet)
	{
		int length = packet.getByte() & 0xff;
		byte[] data = packet.getByteArray(length);
		if(data[0] != (byte) 0xd0)
		{
			Logger.log(Logger.ERROR, "Server did not send valid confirm code");
			return false;
		}
		return true;
	}
	
	/**
	 * @see [T123] Section 8
	 */
	private byte[] createTpktHeader(int length)
	{
		return new byte[] { TPKT_VERSION, 0x00, BitManip.third(length + TPKT_HEADER_SIZE),
				BitManip.fourth(length + TPKT_HEADER_SIZE) };
	}

	/**
	 * @see [T123] Section 8
	 */
	private short readTpktHeader(byte[] data)
	{
		if(data[0] != TPKT_VERSION)
			return -1;
		return BitManip.mergeToShort(data[2], data[3]);
	}
	
	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240506(PROT.10).aspx>
	 * 2.2.1.2.1 RDP Negotiation Response (RDP_NEG_RSP)</a>
	 */
	private boolean receiveRdpNegotiationResponse(InputByteStream packet)
	{
		if(packet.left() == 0)
		{
			Logger.log(Logger.DEBUG, "No negotiation response to read, skipping...");
			return true;
		}
		if(packet.left() != 8)
		{
			Logger.log(Logger.ERROR, "receiveRdpNegotiationResponse - invalid size received: " + packet.left());
			return true;
		}
		int b = packet.getByte() & 0xff;
		if(b != 0x02)
		{
			Logger.log(Logger.ERROR, "receiveRdpNegotiationResponse - invalid type received: " + b);
			return false;
		}
		RdpServerInfo info = net.getServerInfo();
		if((packet.getByte() & 0xff) == 0x01)
			info.setSupportsExtendedClientDataBlocks(true);
		short length = packet.getShort();
		if(length != 0x08)
		{
			Logger.log(Logger.ERROR, "receiveRdpNegotiationResponse - invalid length received: " + length);
			return false;
		}
		securityProtocol = packet.getInt();
		return true;
	}
	
	/**
	 * @see [X224] Section 13.7
	 */
	private byte[] createx224DataPDU() {
		byte[] value = new byte[3];
		value[0] = 2;
		value[1] = (byte) 0xf0;
		value[2] = (byte) 0x80;
		return value;
	}
	
	/**
	 * @see [x224] Section 10.7
	 */
	private void readx224DataTPDU(InputByteStream packet, RdpReply reply)
	{
		int size = packet.getByte() & 0xff;
		byte[] x224TPDU = packet.getByteArray(size);
		if(x224TPDU[0] != 0xf0)
		reply.setX224Length(size + 1);		//+1 for length indicator
	}
}
