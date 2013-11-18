package com.jrdp.core.remote.rdp;


import com.jrdp.core.encryption.RSA;
import com.jrdp.core.remote.rdp.replies.ChannelJoinConfirm;
import com.jrdp.core.remote.rdp.replies.RdpReply;
import com.jrdp.core.remote.rdp.requests.ChannelJoinRequest;
import com.jrdp.core.remote.rdp.requests.RdpRequest;
import com.jrdp.core.util.BitManip;
import com.jrdp.core.util.InputByteStream;
import com.jrdp.core.util.Logger;
import com.jrdp.core.util.StringManip;

public class MCS extends X224
{
	//40 bit = 0x01; 128 bit = 0x02; 56 bit = 0x08; FIPS = 0x10;
	private static final int SUPPORTED_ENCRYPTION_METHODS = 0x0000000B;
	//supports errinfo pdu = 0x0001; want 32 bpp session = 0x0002; supports statusinfo pdu = 0x0004;
	// supports assymetric keys larger than 512 bits = 0x0008; is valid connection type = 0x0020;
	// supports monitor layout pdu = 0x0040;
	private static final short EARLY_COMPATIBILITY_FLAGS = 0x0021;
	static final int KEYBOARD_TYPE = 0x00000001; 		//IBM-83
	static final int KEYBOARD_LAYOUT = 0;
	static final int KEYBOARD_FUNCTION_KEY = 0;
	static final int KEYBOARD_SUB_TYPE = 0;
	static final short[] IME_FILE_NAME = StringManip.toUnicode("", 32);
	
	static final int SERVER_VERSION_4_0 = 1;
	static final int SERVER_VERSION_5_0_AND_HIGHER = 2;
    public static final int ENCRYPTION_LEVEL_NONE = 0x00000000;
    public static final int ENCRYPTION_LEVEL_LOW = 0x00000001;
    public static final int ENCRYPTION_LEVEL_CLIENT_COMPATIBLE = 0x00000002;
    public static final int ENCRYPTION_LEVEL_HIGH = 0x00000003;
    public static final int ENCRYPTION_LEVEL_FIPS = 0x00000004;
    public static final byte CONNECTION_TYPE_MODEM = 0x01;
	public static final byte CONNECTION_TYPE_BROADBAND_LOW = 0x02;
    public static final byte CONNECTION_TYPE_SATELLITE = 0x03;
    public static final byte CONNECTION_TYPE_BROADBAND_HIGH = 0x04;
    public static final byte CONNECTION_TYPE_WAN = 0x05;
    public static final byte CONNECTION_TYPE_LAN = 0x06;

    public static final short COLOR_DEPTH_4BPP = (short) 0xca00;
    public static final short COLOR_DEPTH_8BPP = (short) 0xca01;
    public static final short POST_BETA_COLOR_DEPTH_4BPP = (short) 0xca00;
    public static final short POST_BETA_COLOR_DEPTH_8BPP = (short) 0xca01;
    public static final short POST_BETA_COLOR_DEPTH_16BPP_555_MASK = (short) 0xca02;
    public static final short POST_BETA_COLOR_DEPTH_16BPP_565_MASK = (short) 0xca03;
    public static final short POST_BETA_COLOR_DEPTH_24BPP = (short) 0xca04;
    public static final short HIGH_COLOR_DEPTH_4BPP = (short) 0x0004;
    public static final short HIGH_COLOR_DEPTH_8BPP = (short) 0x0008;
    public static final short HIGH_COLOR_DEPTH_15BPP = (short) 0x000f;
    public static final short HIGH_COLOR_DEPTH_16BPP = (short) 0x0010;
    public static final short HIGH_COLOR_DEPTH_24BPP = (short) 0x0018;
	static final short SUPPORTED_COLOR_DEPTH_24BPP = (short) 0x0001;
	static final short SUPPORTED_COLOR_DEPTH_16BPP = (short) 0x0002;
	static final short SUPPORTED_COLOR_DEPTH_15BPP = (short) 0x0004;
	static final short SUPPORTED_COLOR_DEPTH_32BPP = (short) 0x0008;
	
	private static int CLIENT_CORE_DATA_BLOCK_SIZE = 216;
	private static int CLIENT_SECURITY_DATA_BLOCK_SIZE = 12;
	private static int GCC_CONNECTION_DATA_SIZE = 23;
	private static int MCS_CI_DATA_BLOCK_SIZE = 102;
	private static byte[] MCS_CONNECT_INITIAL_DATA_START;
	
	static
	{
		int clientInfoSize = (CLIENT_CORE_DATA_BLOCK_SIZE +	CLIENT_SECURITY_DATA_BLOCK_SIZE) | 0x8000;
		int gccSize = (CLIENT_CORE_DATA_BLOCK_SIZE + CLIENT_SECURITY_DATA_BLOCK_SIZE + 14) | 0x8000;
		int userSize = GCC_CONNECTION_DATA_SIZE + CLIENT_CORE_DATA_BLOCK_SIZE +	CLIENT_SECURITY_DATA_BLOCK_SIZE;
		int mcsciSize = MCS_CI_DATA_BLOCK_SIZE + GCC_CONNECTION_DATA_SIZE +
				CLIENT_CORE_DATA_BLOCK_SIZE +	CLIENT_SECURITY_DATA_BLOCK_SIZE - 5;
		
		// values taken from http://msdn.microsoft.com/en-us/library/cc240836(v=PROT.10).aspx
		MCS_CONNECT_INITIAL_DATA_START = new byte[]
		{
				0x7f, 0x65,					//BER: Application-Defined Type = APPLICATION 101 =	Connect-Initial
											//BER: Type Length [Need to replace the 0x00's with the actual length]
				(byte) 0x82, BitManip.third(mcsciSize), BitManip.fourth(mcsciSize),
				0x04, 0x01, 0x01,			//Connect-Initial::callingDomainSelector
				0x04, 0x01, 0x01,			//Connect-Initial::calledDomainSelector
				0x01, 0x01, (byte) 0xff,	//Connect-Initial::upwardFlag = TRUE
				0x30, 0x19,					//Connect-Initial::targetParameters (25 bytes)
				0x02, 0x01, 0x22,			//DomainParameters::maxChannelIds
				0x02, 0x01, 0x02,			//DomainParameters::maxUserIds = 2
				0x02, 0x01, 0x00,			//DomainParameters::maxTokenIds = 0
				0x02, 0x01, 0x01,			//DomainParameters::numPriorities = 1
				0x02, 0x01, 0x00,			//DomainParameters::minThroughput = 0
				0x02, 0x01, 0x01,			//DomainParameters::maxHeight = 1
				0x02, 0x02, (byte) 0xff, (byte) 0xff,		//DomainParameters::maxMCSPDUsize = 65535
				0x02, 0x01, 0x02,			//DomainParameters::protocolVersion = 2
				0x30, 0x19,					//Connect-Initial::minimumParameters (25 bytes)
				0x02, 0x01, 0x01,			//DomainParameters::maxChannelIds = 1
				0x02, 0x01, 0x01,			//DomainParameters::maxUserIds = 1
				0x02, 0x01, 0x01,			//DomainParameters::maxTokenIds = 1
				0x02, 0x01, 0x01,			//DomainParameters::numPriorities = 1
				0x02, 0x01, 0x00,			//DomainParameters::minThroughput = 0
				0x02, 0x01, 0x01,			//DomainParameters::maxHeight = 1
				0x02, 0x02, 0x04, 0x20,		//DomainParameters::maxMCSPDUsize = 1056
				0x02, 0x01, 0x02,			//DomainParameters::protocolVersion = 2
				0x30, 0x1c,					//Connect-Initial::maximumParameters (28 bytes)
				0x02, 0x02, (byte) 0xff, (byte) 0xff,		//DomainParameters::maxChannelIds = 65535
				0x02, 0x02, (byte) 0xfc, 0x17,				//DomainParameters::maxUserIds = 64535
				0x02, 0x02, (byte) 0xff, (byte) 0xff,		//DomainParameters::maxTokenIds = 65535
				0x02, 0x01, 0x01,			//DomainParameters::numPriorities = 1
				0x02, 0x01, 0x00,			//DomainParameters::minThroughput = 0
				0x02, 0x01, 0x01,			//DomainParameters::maxHeight = 1
				0x02, 0x02, (byte) 0xff, (byte) 0xff,		//DomainParameters::maxMCSPDUsize = 65535
				0x02, 0x01, 0x02,			//DomainParameters::protocolVersion = 2
											//Connect-Initial::userData
				0x04, (byte) 0x82, BitManip.third(userSize), BitManip.fourth(userSize),
				///// GCC PORTION START /////
				0x00,						//CHOICE: From Key select object (0) of type OBJECT IDENTIFIER
				0x05,						//object length = 5 bytes
				0x00, 0x14, 0x7c, 0x00, 0x01,		//object
				BitManip.third(gccSize), BitManip.fourth(gccSize),		//ConnectData::connectPDU length
				0x00, 0x08, 0x00, 0x10,		//PER encoded (basic aligned variant) GCC
				0x00, 0x01, (byte) 0xc0,	// Conference Create Request PDU
				0x00,
				0x44, 0x75, 0x63, 0x61,		//h221NonStandard (client-to-server H.221 key) = "Duca"
											//size of remaining data structs that are coming up
				BitManip.third(clientInfoSize), BitManip.fourth(clientInfoSize)
		};
	}
	
	protected short initiator;
	protected short channelId;
	protected short[] otherChannels;
	protected int serverVersion;
	protected int encryptionMethod;
	protected int encryptionLevel;
	protected byte[] serverRandom;
	protected RSA rsaKey;
	
	protected short highColorDepth;
	protected short colorDepth;
	protected short postBetaColorDepth;
	protected short supportedColorDepths;
	
	//needs to be moved to the lowest layer when SSL is implemented
	private int serverSelectedProtocol = 0;

	public MCS(NetworkManager net, RdpConnectionInfo info)
	{
		super(net, info);
		highColorDepth = 0;
		colorDepth = 0;
		postBetaColorDepth = 0;
		supportedColorDepths = SUPPORTED_COLOR_DEPTH_15BPP | SUPPORTED_COLOR_DEPTH_16BPP |
				SUPPORTED_COLOR_DEPTH_24BPP;
		switch(info.getRequestedColorDepth())
		{
		case 15:
			postBetaColorDepth = POST_BETA_COLOR_DEPTH_16BPP_555_MASK;
			highColorDepth = HIGH_COLOR_DEPTH_15BPP;
			break;
		case 16:
			postBetaColorDepth = POST_BETA_COLOR_DEPTH_16BPP_565_MASK;
			highColorDepth = HIGH_COLOR_DEPTH_16BPP;
			break;
		case 24:
			postBetaColorDepth = POST_BETA_COLOR_DEPTH_24BPP;
			highColorDepth = HIGH_COLOR_DEPTH_24BPP;
			break;
		}
	}
	
	public short getInitiator()
	{
		return initiator;
	}

	public short getChannelId()
	{
		return channelId;
	}

	public short[] getOtherChannels()
	{
		return otherChannels;
	}
	
	protected void sendPacket(int type, RdpRequest request, RdpPacket packet)
	{
		switch(type)
		{
			case MCS_CONNECT_INITIAL:
				packet.addPacketPeiceAtStart(createMcsConnectInitialStructure());
				break;
			case ERECT_DOMAIN_REQUEST:
				packet.addPacketPeiceAtStart(createMcsErectDomainRequest());
				break;
			case ATTACH_USER_REQUEST:
				packet.addPacketPeiceAtStart(createMcsAttachUserRequest());
				break;
			case CHANNEL_JOIN_REQUEST:
				if(request != null && request instanceof ChannelJoinRequest)
					packet.addPacketPeiceAtStart(createChannelJoinRequest((ChannelJoinRequest) request));
				else
					throw new IllegalArgumentException("CHANNEL_JOIN_REQUEST: Invalid RdpRequest Received");
				break;
		}
		super.sendPacket(type, request, packet);
	}
	
	protected RdpReply readPacket(int type, InputByteStream packet)
	{
		RdpReply reply = super.readPacket(type, packet);
		switch(type)
		{
			case MCS_CONNECT_RESPONSE:
				readMcsConnectResponse(packet, reply);
				break;
			case ATTACH_USER_CONFIRM:
				readMcdAttachUserConfirm(packet, reply);
				break;
			case CHANNEL_JOIN_CONFIRM:
				reply = new ChannelJoinConfirm(reply);
				if(!readChannelJoinConfirm(packet, (ChannelJoinConfirm) reply))
					return null;
				break;
		}
		return reply;
	}
	
	/**
	 * @see [T125] Section 10.1
	 * @see [T125] Section 7 Part 2
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240508(v=PROT.10).aspx>
	 * 2.2.1.3 Client MCS Connect Initial PDU with GCC Conference Create Request</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240836(v=PROT.10).aspx>
	 * 4.1.3 Client MCS Connect Initial PDU with GCC Conference Create Request</a>
	 */
	private byte[] createMcsConnectInitialStructure()
	{
		
		int size = MCS_CI_DATA_BLOCK_SIZE + GCC_CONNECTION_DATA_SIZE +
				CLIENT_CORE_DATA_BLOCK_SIZE + CLIENT_SECURITY_DATA_BLOCK_SIZE;
		byte[] data = new byte[size];
		int i = MCS_CONNECT_INITIAL_DATA_START.length;
		System.arraycopy(MCS_CONNECT_INITIAL_DATA_START, 0, data, 0, i);
		i += BitManip.setLittleEndian(data, i, (short) 0xc001);					//header - type
		i += BitManip.setLittleEndian(data, i, (short) CLIENT_CORE_DATA_BLOCK_SIZE);//header - length
		i += BitManip.setLittleEndian(data, i, 0x00080004);						//version
		i += BitManip.setLittleEndian(data, i, info.getWidth());				//width
		i += BitManip.setLittleEndian(data, i, info.getHeight());				//height
		i += BitManip.setLittleEndian(data, i, colorDepth);						//color depth
		i += BitManip.setLittleEndian(data, i, (short) 0xaa03);					//SAS Sequence
		i += BitManip.setLittleEndian(data, i, KEYBOARD_LAYOUT);				//keyboard layout
		i += BitManip.setLittleEndian(data, i, Constants.CLIENT_BUILD);			//client build
		i += BitManip.setUnicode16(data, i, Constants.PRODUCT_NAME, 16, true);	//client name
		i += BitManip.setLittleEndian(data, i, KEYBOARD_TYPE);					//keyboard type
		i += BitManip.setLittleEndian(data, i, KEYBOARD_SUB_TYPE);				//keyboard sub type
		i += BitManip.setLittleEndian(data, i, KEYBOARD_FUNCTION_KEY);			//keyboard function key count
		i += BitManip.setLittleEndian(data, i, IME_FILE_NAME);					//Input Method Editor file name
		i += BitManip.setLittleEndian(data, i, postBetaColorDepth);				//best beta 2 color depth
		i += BitManip.setLittleEndian(data, i, (short) 0x0001);					//client product id
		i += BitManip.setLittleEndian(data, i, 0x00000000);						//serial number
		i += BitManip.setLittleEndian(data, i, highColorDepth);					//high color depth
		i += BitManip.setLittleEndian(data, i, supportedColorDepths);			//supported color depths
		i += BitManip.setLittleEndian(data, i, EARLY_COMPATIBILITY_FLAGS);		//early capability flags
		i += BitManip.setUnicode16(data, i, Constants.PRODUCT_ID, 32, true);	//client dig product id
		i += BitManip.setLittleEndian(data, i, info.getConnectionType());		//connection type
		i += BitManip.setLittleEndian(data, i, (byte) 0x00);					//1 octet pad
		i += BitManip.setLittleEndian(data, i, serverSelectedProtocol);			//server selected protocol
		i += BitManip.setLittleEndian(data, i, (short) 0xc002);					//header - type
		i += BitManip.setLittleEndian(data, i, (short) CLIENT_SECURITY_DATA_BLOCK_SIZE);		//header - length
		//encryption methods and ext encryption methods
		if(info.isFrenchLocale())
		{
			i += BitManip.setLittleEndian(data, i, 0x00000000);
			i += BitManip.setLittleEndian(data, i, SUPPORTED_ENCRYPTION_METHODS);
		}
		else
		{
			i += BitManip.setLittleEndian(data, i, SUPPORTED_ENCRYPTION_METHODS);
			i += BitManip.setLittleEndian(data, i, 0x00000000);
		}
		return data;
	}
	
	/**
	 * @see [T125] Section 10.2
	 * @see [T125] Section 7.2
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240515(v=PROT.10).aspx>
	 * 2.2.1.4 Server MCS Connect Response PDU with GCC Conference Create Response<a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240919(v=PROT.10).aspx>
	 * 4.1.4 Server MCS Connect Response PDU with GCC Conference Create Response</a>
	 */
	private boolean readMcsConnectResponse(InputByteStream packet, RdpReply reply)
	{
		int size = reply.getPacketLength() - reply.getX224PacketLength() - TPKT_HEADER_SIZE;
		if(packet.getShort() != 0x7f66)
		{
			Logger.log(Logger.ERROR, "Invalid Application Type in Connect-Response");
			return false;
		}
		//check to see if length is in short or long form
		int b = packet.getByte() & 0xff;
		if((b & 0x80) == 0x80)
		{
			//long form
			packet.skip(b & 0x7f);
		}
		packet.skip(2);		//skip next 2
		if((packet.getByte() & 0xff) != 00)
		{
			Logger.log(Logger.ERROR, "Connection was not successful");
			return false;
		}
		packet.skip((packet.getByte() & 0xff) + 1);	//go to next length indicator
		packet.skip((packet.getByte() & 0xff) + 1);
		//check to see if length is in short or long form
		b = (packet.getByte() & 0xff);
		if((b & 0x80) == 0x80)
		{
			//long form
			packet.skip(b & 0x7f);
		}
		packet.skip(23);		//should now be pointing passed the PER encoded bullshit
		while(packet.getPos() < size - 1)
		{
			short type = packet.getShortLittleEndian();
			short length = packet.getShortLittleEndian();
			if(type == (short) 0x0c03)
			{
				//2.2.1.4.4 Server Network Data (TS_UD_SC_NET)
				//http://msdn.microsoft.com/en-us/library/cc240522(v=PROT.10).aspx
				channelId = packet.getShortLittleEndian();
				short count = packet.getShortLittleEndian();
				short[] channels = new short[count];
				for(int i=0; i < count; i++)
				{
					channels[i] = packet.getShortLittleEndian();
				}
				if(count % 2 == 1)		//pad
					packet.skip(2);
				otherChannels = channels;
			}
			else if(type == (short) 0x0c02)
			{
				//2.2.1.4.3 Server Security Data (TS_UD_SC_SEC1)
				//http://msdn.microsoft.com/en-us/library/cc240518(v=PROT.10).aspx
				encryptionMethod = packet.getIntLittleEndian();
				switch(encryptionMethod)
				{
				case Constants.ENCRYPTION_NONE:
					Logger.log(Logger.INFO, "NO encryption chosen by server");
					break;
				case Constants.ENCRYPTION_40BIT:
					Logger.log(Logger.INFO, "40 bit encryption chosen by server");
					break;
				case Constants.ENCRYPTION_56BIT:
					Logger.log(Logger.INFO, "56 bit encryption chosen by server");
					break;
				case Constants.ENCRYPTION_128BIT:
					Logger.log(Logger.INFO, "128 bit encryption chosen by server");
					break;
				case Constants.ENCRYPTION_FIPS:
					Logger.log(Logger.INFO, "FIPS encryption chosen by server");
					break;
				}
				encryptionLevel = packet.getIntLittleEndian();
				if(encryptionMethod != Constants.ENCRYPTION_NONE && encryptionLevel != ENCRYPTION_LEVEL_NONE){
					int randomLen = packet.getIntLittleEndian();
					int certLen = packet.getIntLittleEndian();
					serverRandom = packet.getByteArrayLittleEndian(randomLen);
					rsaKey = RSA.createKeyFromCertificate(packet.getByteArray(certLen));
				}
			}
			else if(type == (short) 0x0c01)
			{
				int version = packet.getIntLittleEndian();
				if(version == 0x00080001)
				{
					serverVersion = SERVER_VERSION_4_0;
					Logger.log(Logger.INFO, "Server is running RDP 4.0");
				}
				else if(version == 0x00080004)
				{
					serverVersion = SERVER_VERSION_5_0_AND_HIGHER;
					Logger.log(Logger.INFO, "Server is running RDP 5.0+");
				}
				else
				{
					serverVersion = -1;
					Logger.log(Logger.INFO, "Server is running unknown RDP version");
				}
				//TODO: compare this to info.getServerSelectedProtocol()
				//packet.getIntLittleEndian();
				if(length == 12)
					packet.skip(4);		//Windows XP's length is only 8, so this int is skipped
			}
		}
		
		return true;
	}
	
	/**
	 * @see [T125] Section 10.6 (Doesn't exist?)
	 * @see [T125] Section 7 Part 3 & 10
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240523(v=PROT.10).aspx>
	 * 2.2.1.5 Client MCS Erect Domain Request PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240837(v=PROT.10).aspx>
	 * 4.1.5 Client MCS Erect Domain Request PDU</a>
	 */
	private byte[] createMcsErectDomainRequest()
	{
		return new byte[] { 0x04, 0x01, 0x00, 0x01, 0x00 };
	}
	
	/**
	 * @see [T125] Section 10.15 (Doesn't exist?)
	 * @see [T125] Section 7 Part 5 & 10
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240524(v=PROT.10).aspx>
	 * 2.2.1.6 Client MCS Attach User Request PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240835(v=PROT.10).aspx>
	 * 4.1.6 Client MCS Attach User Request PDU</a>
	 */
	private byte[] createMcsAttachUserRequest()
	{
		return new byte[] { 0x28 };
	}
	
	/**
	 * @see [T125] Section 10.16 (Doesn't exist?)
	 * @see [T125] Section 7 Part 5 & 10
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240525(v=PROT.10).aspx>
	 * 2.2.1.7 Server MCS Attach User Confirm PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240918(v=PROT.10).aspx>
	 * 4.1.7 Server MCS Attach-User Confirm PDU</a>
	 */
	private boolean readMcdAttachUserConfirm(InputByteStream packet, RdpReply reply)
	{
		//TODO: check for success
		packet.skip(packet.available() - 2);
		initiator = packet.getShort();
		return true;
	}
	
	/**
	 * @see [T125] Section 10.19 (Doesn't exist?)
	 * @see [T125] Section 7 Part 6 & 10
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240526(v=PROT.10).aspx>
	 * 2.2.1.8 Client MCS Channel Join Request PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240834(v=PROT.10).aspx>
	 * 4.1.8.1.1 Client Join Request PDU for Channel 1007 (User Channel)</a>
	 */
	private byte[] createChannelJoinRequest(ChannelJoinRequest request)
	{
		byte[] data = new byte[5];
		data[0] = 0x38;
		BitManip.set(data, 1, (short) (initiator));
		BitManip.set(data, 3, request.getChannelId());
		return data;
	}

	/**
	 * @see [T125] Section 10.20 (Doesn't exist?)
	 * @see [T125] Section 7 Part 6 & 10
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240527(v=PROT.10).aspx>
	 * 2.2.1.9 Server MCS Channel Join Confirm PDU</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240915(v=PROT.10).aspx>
	 * 4.1.8.1.2 Server Join Confirm PDU for Channel 1007 (User Channel)</a>
	 */
	private boolean readChannelJoinConfirm(InputByteStream packet, ChannelJoinConfirm reply)
	{
		//TODO: check for success
		packet.skip(2);
		if(packet.getShort() != initiator)
		{
			Logger.log(Logger.ERROR, "Invalid initiator received from server");
			return false;
		}
		//TODO: this
//		if(packet.getShort() != reply.getChannelId() || packet.getShort() != reply.getChannelId())
//		{
//			Logger.log("Invalid channel id received from server");
//			return false;
//		}
		return true;
	}
}
