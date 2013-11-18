package com.jrdp.core.remote.rdp;

import com.jrdp.core.compression.MCCP;
import com.jrdp.core.compression.RLE;
import com.jrdp.core.remote.Canvas;
import com.jrdp.core.remote.rdp.replies.FastPathReply;
import com.jrdp.core.remote.rdp.replies.RdpReply;
import com.jrdp.core.remote.rdp.requests.ChannelJoinRequest;
import com.jrdp.core.remote.rdp.requests.InputEvent;
import com.jrdp.core.remote.rdp.requests.RdpRequest;
import com.jrdp.core.util.BitManip;
import com.jrdp.core.util.InputByteStream;
import com.jrdp.core.util.Logger;

import java.util.Hashtable;

public class Rdp extends Secure
{
	private final static boolean supportedUnicodeKeyboardEvents = false;
	//////////////////////
	//General Capabilities
	//////////////////////
	//extra flags (0x0001 = fastpath output supported, 0x0400 = supports excluding compression header,
	// 0x0004 = supports long length creds, 0x0008 = auto reconnect supported, 0x0010 salted mac generation)
	private final static short CLIENT_SUPPORT_GENERAL_CAPABILITIES = 0x0001;
	//supports refresh rect PDU
	private final static byte CLIENT_SUPPORT_REFRESH_RECT_PDU = 0x00;
	//supports supress output pdu
	private final static byte CLIENT_SUPPORT_SUPRESS_OUTPUT_PDU = 0x00;
	
	////////////////
	//Bitmap support
	////////////////
	//Desktop resize (0x0000 = false, 0x0001 = true)
	private final static short CLIENT_SUPPORT_DESKTOP_SIZE = 0x0000;
	
	///////////////
	//Order Support
	///////////////
	//0x00 = extra flags flag is invalid, 0x01 = is valid 
	private final static short CLIENT_SUPPORT_ORDER_SUPPORT_EX_IS_VALID = 0x0000;
	//extra flags (0x0002 = bitmap cache rev 3 supported, 0x0004 = frame marker alt sec drawing order supported)
	private final static short CLIENT_SUPPORT_ORDER_SUPPORT_EXTRA_FLAGS = 0x0000;
	
	//////////////
	//Bitmap Cache
	//////////////
	//bitmap cache sizes
	private final static short CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_1_ENTRIES = 0;
	private final static short CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_2_ENTRIES = 0;
	private final static short CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_3_ENTRIES = 0;
	private final static short CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_1_CELL_SIZE = 0;
	private final static short CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_2_CELL_SIZE = 0;
	private final static short CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_3_CELL_SIZE = 0;
	private final static int CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_1_NUM_ENTRIES = 0;
	private final static int CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_2_NUM_ENTRIES = 0;
	private final static int CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_3_NUM_ENTRIES = 0;
	private final static int CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_4_NUM_ENTRIES = 0;
	private final static int CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_5_NUM_ENTRIES = 0;
	
	///////////////
	//Input Cap Set
	///////////////
	//Input flags (0x0001 = scancodes, 0x0004 = extended mouse event notifications, 0x0008 = fastpath input,
	// 0x0010 = unicode keyboard event notifications, 0x0020 = fastpath input 2)
	private final static short CLIENT_SUPPORT_INPUT_SUPPORT_FLAGS = 0x0001 |
			(supportedUnicodeKeyboardEvents ? 0x0010 : 0x0000);
	
	///////////////
	//Sound Cap Set
	///////////////
	//sound flags (0x0001 = playing a beep is supported)
	private final static short CLIENT_SUPPORT_SOUND_FLAGS = 0;
	
	/////////////////////
	//Glyph Cache Cap Set
	/////////////////////
	//glyph cache sizes
	private final static short CLIENT_SUPPORT_GLYPH_CACHE_ENTRIES[] = {
		254, 254, 254, 254, 254, 254, 254, 254, 254, 254 };
	private final static short CLIENT_SUPPORT_GLYPH_MAX_CELL_SIZE[] = {
		4, 4, 8, 8, 16, 32, 64, 128, 256, 256 };
	private final static short CLIENT_SUPPORT_GLYPH_FRAG_ENTRIES = 256;
	private final static short CLIENT_SUPPORT_GLYPH_FRAG_MAX_CELL_SIZE = 256;
	//glyph support level (0x0001 = partial support (rev 1 drawing orders), 0x0002 = full support
	// (rev 1 drawing orders), 0x0003 = encode support (rev 2))
	private final static short CLIENT_SUPPORT_GLYPH_SUPPORT_LEVEL = 0x0000;
	
	private final static int SHARE_DATA_HEADER_SIZE = 18;
	private final static int SHARE_CONTROL_HEADER_SIZE = 6;
	private static final byte PDU_TYPE_DEMAND_ACTIVE = 0x01;
	private static final byte PDU_TYPE_CONFIRM_ACTIVE = 0x03;
	private static final byte PDU_TYPE_DEACTIVATE_ALL = 0x06;
	private static final byte PDU_TYPE_DATA = 0x07;
	private static final byte PDU_TYPE_SERVER_REDIR = 0x0A;

	private static final byte PDU_TYPE2_UPDATE = 0x02;
	private static final byte PDU_TYPE2_SYNCHRONIZE = 0x1f;
	private static final byte PDU_TYPE2_CONTROL = 0x14;
	private static final byte PDU_TYPE2_BITMAPCACHE_PERSISTENT_LIST = 0x2b;
	private static final byte PDU_TYPE2_FONT_LIST = 0x27;
	private static final byte PDU_TYPE2_FONT_MAP = 0x28;
	private static final byte PDU_TYPE2_POINTER = 0x1b;
	private static final byte PDU_TYPE2_INPUT = 0x1c;
	private static final byte PDU_TYPE2_ERROR_INFO = 0x2F;

	private static final byte FASTPATH_CODE_ORDERS = 0x00;
	private static final byte FASTPATH_CODE_BITMAP = 0x01;
	private static final byte FASTPATH_CODE_PALETTE = 0x02;
	private static final byte FASTPATH_CODE_SYNCHRONIZE = 0x03;
	private static final byte FASTPATH_CODE_SURFACE_COMMANDS = 0x04;
	private static final byte FASTPATH_CODE_POINTER_HIDDEN_UPDATE = 0x05;
	private static final byte FASTPATH_CODE_POINTER_DEFAULT_UPDATE = 0x06;
	private static final byte FASTPATH_CODE_POINTER_POSITION_UPDATE = 0x08;
	private static final byte FASTPATH_CODE_COLOR_POINTER_UPDATE = 0x09;
	private static final byte FASTPATH_CODE_CACHED_POINTER_UPDATE = 0x0A;
	private static final byte FASTPATH_CODE_NEW_POINTER_UPDATE = 0x0B;

	private static final short CONTROL_ACTION_COOPERATE = 0x0004;
	private static final short CONTROL_ACTION_REQUEST_CONTROL = 0x0001;
	
	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240549(v=PROT.10).aspx>
	 * 2.2.7.1.1 General Capability Set (TS_GENERAL_CAPABILITYSET)</a>
	 */
	private static final short CAPABILITY_TYPE_GENERAL = 0x0001;
	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240554(v=PROT.10).aspx>
	 * 2.2.7.1.2 Bitmap Capability Set (TS_BITMAP_CAPABILITYSET)</a>
	 */
	private static final short CAPABILITY_TYPE_BITMAP = 0x0002;

	static final short KEYBOARD_FLAG_EXTENDED = 0x0100;
	static final short KEYBOARD_FLAG_DOWN = 0x0000;
	static final short KEYBOARD_FLAG_UP = (short) 0x8000;
	
	static final Cursor DEFAULT_CURSOR = new Cursor(new int[] 
	        { -16644841, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, -16644841, -16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -1710360, -16645101, 261, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -1, 
			-1710360, -16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, -16644841, -1, -1, -1710360, -16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -1, -1, -1, -1710360, 
			-16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			-16644841, -1, -1, -1, -1, -1710360, -16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -1, -1, -1, -1, -131587, -1907996, 
			-16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			-16644841, -1, -1, -1, -131587, -263173, -460552, -2302497, -16645101, 261, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -1, -1, -131587, -263173, 
			-460552, -657931, -921103, -2697512, -16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -1, -131587, -263173, -460552, -657931, -921103, 
			-1118482, -1381654, -3158063, -16645101, 261, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, -16644841, -131587, -263173, -460552, -657931, -921103, -1118482, -1381654, 
			-1644826, -1907998, -3552821, -16645101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, -16644841, -263173, -460552, -657931, -2500133, -1118482, -1381654, -16645101, 
			-16645101, -16645101, -16645101, -16645101, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, -16644841, -460552, -657931, -2500133, -16644840, -5986901, -1644826, 
			-16645101, 263, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			-16644841, -657931, -2500133, -16644840, 261, -16645099, -1907998, -3552821, -16645099, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16644841, -2500133, 
			-16644840, 261, 0, -16645102, -5197643, -2368549, -16645102, 263, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -16645101, -16644840, 261, 0, 0, 261, -16644843, 
			-2565928, -4144958, -16645099, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, -16645102, -4144958, -2894893, -16645100, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, -16644843, -16710896, 260, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
			0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, 
			(short) 32, (short) 32, (short) 0, (short) 0);
	
	private boolean supportsDesktopResize = false;
	private int shareId;
	private short bitsPerPixel;
	private short desktopWidth;
	private short desktopHeight;
	private int drawingFlags;
	private boolean supportsFastPath = false;
	private boolean supportsNoBitmapCompressionHeader = false;
	private boolean supportsLongCredentials = false;
	private boolean supportsAutoReconnect = false;
	private boolean supportsSaltedChecksum = false;
	private boolean supportsRefreshRectPDU = false;
	private boolean supportsSupressOutputPDU = false;
	
	private MCCP mccp;
	private boolean running;
	private boolean connected = false;
	private Canvas canvas;
	private Hashtable<Integer, Cursor> cursors = new Hashtable<Integer, Cursor>();
	//private Orders orders;
	
	
	public Rdp(NetworkManager net, Canvas canvas, String username, String password,
			String domain, String ip, int requestedEncryptionLevel, int timeZoneBias,
			int performanceFlags, short width, short height, short requestedColorDepth,
			byte connectionType, boolean isFrenchLocale)
	{
		this(net, new RdpConnectionInfo(username, password, domain, ip,
			timeZoneBias, performanceFlags, width, height, requestedColorDepth,
			connectionType, isFrenchLocale), canvas);
	}
	
	public Rdp(NetworkManager net, RdpConnectionInfo info, Canvas canvas) {
		super(net, info);
		running = true;
		this.canvas = canvas;
		mccp = new MCCP();
		canvas.cursorPositionChanged(info.getWidth() / 2, info.getHeight() / 2);
		
		//Setup a default pointer so that we have a pointer from the beginning, guaranteed
		cursors.put(1, DEFAULT_CURSOR);
		canvas.setCursor(cursors.get(1));
		canvas.showCursor();
	}
	
	protected RdpReply readPacket(int type, InputByteStream packet)
	{
		RdpReply reply = super.readPacket(type, packet);
		switch(type)
		{
			case SERVER_DEMAND_ACTIVE_PDU:
				if(readShareControlHeader(packet) != PDU_TYPE_DEMAND_ACTIVE)
					return null;
				readDemandActivePDU(packet);
				break;
			case UNKNOWN_PDU:
				if(reply.isFastPath())
					return reply;
				processShareDataHeader(packet);
				break;
		}
		
		return reply;
	}
	
	private void processShareDataHeader(InputByteStream packet)
	{
		byte masterType = readShareControlHeader(packet);
		if(masterType == PDU_TYPE_SERVER_REDIR)
		{
			Logger.log(Logger.INFO, "SERVER REDIR PDU RECEIVED");
		}
		else if(masterType == PDU_TYPE_DATA)
		{
			packet.skip(6);
			short uncompressedLength = packet.getShortLittleEndian();
			//for some reason, compressed length is always 18 more than is should be, maybe because
			// of header size?
			byte packetType = (byte) (packet.getByte() & 0xff);
			byte compressedType = (byte) (packet.getByte() & 0xff);
			short compressedLength = (short) ((packet.getShortLittleEndian() & 0xffff) - 18);
			Logger.log(Logger.DEBUG, "uncompressed length: " + uncompressedLength + " compressed length: " + compressedLength + " left: " + packet.left() +
					"\npacket type: " + packetType + " compressed type: " + compressedType);
			processUnknownPDU(packet, packetType, compressedType, compressedLength);
		}
		else if(masterType == PDU_TYPE_DEACTIVATE_ALL)
		{
			Logger.log(Logger.INFO, "DEACTIVATE ALL PDU RECEIVED");
			packet.skip(4);
			int length = packet.getShortLittleEndian();
			Logger.log(Logger.DEBUG, "length: " + length);
		}
		else if(masterType == PDU_TYPE_DEMAND_ACTIVE)
		{
			readDemandActivePDU(packet);
			sendPacket(CONFIRM_ACTIVE_PDU);
			sendPacket(SYNCHRONIZE_PDU);
			sendPacket(CONTROL_PDU_COOPERATE);
			sendPacket(CONTROL_PDU_REQUEST_CONTROL);
			readPacket(UNKNOWN_PDU);		//Synchronize
			readPacket(UNKNOWN_PDU);		//Control - Cooperate
			readPacket(UNKNOWN_PDU);		//Control - Grant Control
			InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_SYNCHRONIZE,
					(short) 0, (short)0, (short)0);
			sendPacket(INPUT_EVENT, event);
			if(serverVersion == SERVER_VERSION_5_0_AND_HIGHER)
			{
				sendPacket(PERSISTENT_KEY_LIST_PDU);
				sendPacket(FONT_LIST_PDU);
			}
			else
			{
				sendPacket(FONT_LIST_PDU);
			}
			//TODO: reset order state?
			//readPacket(UNKNOWN_PDU);
			//resetOrderState();
		}
		else
		{
			Logger.log(Logger.DEBUG, "UNKNOWN PDU RECEIVED: " + Integer.toHexString(masterType & 0xff));
		}
	}
	
	protected void sendPacket(int type, RdpRequest request, RdpPacket packet)
	{
		switch(type)
		{
			case CONFIRM_ACTIVE_PDU:
				packet.addPacketPeiceAtStart(createConfirmActivePDU(), encryption);
				break;
			case SYNCHRONIZE_PDU:
				packet.addPacketPeiceAtStart(createSynchronizePDU(), encryption);
				break;
			case CONTROL_PDU_COOPERATE:
				packet.addPacketPeiceAtStart(createClientControlPDU(CONTROL_ACTION_COOPERATE), encryption);
				break;
			case CONTROL_PDU_REQUEST_CONTROL:
				packet.addPacketPeiceAtStart(createClientControlPDU(CONTROL_ACTION_REQUEST_CONTROL), encryption);
				break;
			case PERSISTENT_KEY_LIST_PDU:
				packet.addPacketPeiceAtStart(createClientPersistentBitmapCachePDU(), encryption);
				break;
			case FONT_LIST_PDU:
				packet.addPacketPeiceAtStart(createFontListPDU(), encryption);
				break;
			case INPUT_EVENT:
				if(!(request instanceof InputEvent))
					throw new IllegalArgumentException("Invalid request received for mouse input event");
				packet.addPacketPeiceAtStart(createInputEvent((InputEvent) request), encryption);
				break;
		}
		
		super.sendPacket(type, request, packet);
	}
	
	protected FastPathReply processFastPathPacket(FastPathReply reply, InputByteStream packet)
	{
		super.processFastPathPacket(reply, packet);
		int updateHeader = packet.getByte() & 0xff;
		byte code = (byte) (updateHeader & 0x0F);
		byte fragmentation = (byte) ((updateHeader & 0x03) >> 6);
		int compression = updateHeader & 0x80;
		if(compression != 0)
		{
			compression = packet.getByte() & 0xff;
		}
		packet.skip(2);		//skip size
		if(fragmentation != 0)
		{
			if(fragmentation == 2)
			{
				Logger.log(Logger.DEBUG, "FRAGMENTATION OCCURED!!!!!!!!!");
				return null;
			}
		}
		switch(code)
		{
		case FASTPATH_CODE_ORDERS:
		case FASTPATH_CODE_BITMAP:
			readUpdatePDU(packet);
			break;
		case FASTPATH_CODE_PALETTE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_PALETTE not implemented");
			break;
		case FASTPATH_CODE_SYNCHRONIZE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_SYNCHRONIZE not implemented");
			break;
		case FASTPATH_CODE_SURFACE_COMMANDS:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_SURFACE_COMMANDS not implemented");
			break;
		case FASTPATH_CODE_POINTER_HIDDEN_UPDATE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_POINTER_HIDDEN_UPDATE not implemented");
			break;
		case FASTPATH_CODE_POINTER_DEFAULT_UPDATE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_POINTER_DEFAULT_UPDATE not implemented");
			break;
		case FASTPATH_CODE_POINTER_POSITION_UPDATE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_POINTER_POSITION_UPDATE not implemented");
			break;
		case FASTPATH_CODE_COLOR_POINTER_UPDATE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_COLOR_POINTER_UPDATE not implemented");
			break;
		case FASTPATH_CODE_CACHED_POINTER_UPDATE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_CACHED_POINTER_UPDATE not implemented");
			break;
		case FASTPATH_CODE_NEW_POINTER_UPDATE:
			Logger.log(Logger.DEBUG, "Fastpath - got FASTPATH_CODE_NEW_POINTER_UPDATE not implemented");
			break;
		default:
			Logger.log(Logger.DEBUG, "Unknown fast path code: " + Integer.toHexString(code & 0xff));
			break;
		}
		return reply;
	}
	
	private byte[] createInputEvent(InputEvent event)
	{
		int i = 0;
		int size = SHARE_DATA_HEADER_SIZE + 16;
		byte[] data = new byte[size];
		System.arraycopy(createShareDataHeader((short) size, PDU_TYPE_DATA, PDU_TYPE2_INPUT),
				0, data, 0, SHARE_DATA_HEADER_SIZE);
		i += SHARE_DATA_HEADER_SIZE;
		/*//////////////////////////////
		System.out.println("--------------------");
		System.out.println("Numevents: " + 1);
		System.out.println("pad: " + 0);
		System.out.println("time: " + 1);
		System.out.println("event type: " + (event.getEventType() & 0xffff) + " (" + Integer.toHexString((event.getEventType() & 0xffff)) + ")");
		System.out.println("flag: " + (event.getFlag() & 0xffff) + " (" + Integer.toHexString((event.getFlag() & 0xffff)) + ")");
		System.out.println("param1: " + (event.getParam1() & 0xffff) + " (" + Integer.toHexString((event.getParam1() & 0xffff)) + ")");
		System.out.println("unicode key: " + (event.getUnicodeKey() & 0xffff) + " (" + Integer.toHexString((event.getUnicodeKey() & 0xffff)) + ")");
		System.out.println("param2: " + (event.getParam2() & 0xffff) + " (" + Integer.toHexString((event.getParam2() & 0xffff)) + ")");
		*////////////////////////////////
		i += BitManip.setLittleEndian(data, i, (short) 1);
		i += BitManip.setLittleEndian(data, i, (short) 0);
		i += BitManip.setLittleEndian(data, i, 1);
		i += BitManip.setLittleEndian(data, i, event.getEventType());
		i += BitManip.setLittleEndian(data, i, event.getFlag());
		if(event.getEventType() == InputEvent.INPUT_TYPE_KEYBOARD_UNICODE)
			i += BitManip.setLittleEndian(data, i, event.getUnicodeKey());
		else
			i += BitManip.setLittleEndian(data, i, event.getParam1());
		BitManip.setLittleEndian(data, i, event.getParam2());
		return data;
	}
	
	public void setMouse(short x, short y)
	{
		InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_MOUSE, InputEvent.INPUT_FLAG_MOUSE_MOVE, x, y);
		sendPacket(INPUT_EVENT, event);
		canvas.cursorPositionChanged(x, y);
	}
	
	public void moveMouse(short deltaX, short deltaY)
	{
		short x = (short) (canvas.getCursorX() + deltaX);
		short y = (short) (canvas.getCursorY() + deltaY);
		InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_MOUSE, InputEvent.INPUT_FLAG_MOUSE_MOVE, x, y);
		sendPacket(INPUT_EVENT, event);
		canvas.cursorPositionChanged(x, y);
	}
	
	public void clickMouseLeft()
	{
		short x = (short) canvas.getCursorX();
		short y = (short) canvas.getCursorY();
		InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_MOUSE, 
				InputEvent.INPUT_FLAG_MOUSE_LEFT_CLICK, x, y);
		sendPacket(INPUT_EVENT, event);
		event = new InputEvent(InputEvent.INPUT_TYPE_MOUSE, 
				(short) 0x1000, x, y);
		sendPacket(INPUT_EVENT, event);
	}
	
	public void sendSpecialKeyboardKey(short key)
	{
		boolean isExtended = false;
		switch(key)
		{
		case 0:
			isExtended = true;
			break;
		}
		short flag = InputEvent.INPUT_FLAG_KEYBOARD_KEY_DOWN;
		if(isExtended)
			flag |= 0x0100;
		InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_KEYBOARD_SCANCODE, flag, key, (short) 0);
		sendPacket(INPUT_EVENT, event);

		flag = InputEvent.INPUT_FLAG_KEYBOARD_KEY_UP;
		if(isExtended)
			flag |= 0x0100;
		event = new InputEvent(InputEvent.INPUT_TYPE_KEYBOARD_SCANCODE, flag, key, (short) 0);
		sendPacket(INPUT_EVENT, event);
	}
	
	public void sendKeyboardKey(char key)
	{
		InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_KEYBOARD_UNICODE, 
				(short) 0, key, (short) 0);
		sendPacket(INPUT_EVENT, event);
	}
	
	/*public void sendKeyboardKey(String key)
	{
		byte[] keyBytes;
		try {
			keyBytes = key.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			Logger.log(e, "Unable to convert string to byte");
			return;
		}
		short k = 0;
		if(keyBytes.length == 1)
			k = keyBytes[0];
		else if(keyBytes.length == 2)
			k = BitManip.mergeToShort(keyBytes[0], keyBytes[1]);
		else
			System.out.println("FFFFFFFFFFFFFFFF");
		InputEvent event = new InputEvent(InputEvent.INPUT_TYPE_KEYBOARD_UNICODE, 
				(short) 0, k, (short) 0);
		sendPacket(INPUT_EVENT, event);
	}*/
	
	private void startMainReceiveLoop()
	{
		Logger.log(Logger.INFO, "--- MAIN LOOP STARTED ---");
		new Thread(new Runnable()
		{
			public void run()
			{
				while(running)
				{
					readPacket(UNKNOWN_PDU);
				}
			}
		}).start();
	}
	
	public void disconnect()
	{
		running = connected = false;
		//TODO: more?
	}
	
	private void processUnknownPDU(InputByteStream packet, byte type,
			byte compressedType, short compressedLength)
	{
		if((compressedType & MCCP.MCCP_COMPRESSED) != 0)
		{
			byte[] b = packet.getByteArray(compressedLength);
			//if(!mccp.decompress(packet, compressedType, compressedLength))
			//System.out.println("Length: " + compressedLength + " compressedType: " + compressedType);
			//StringManip.print(b, "PACKET: ");
			if(mccp.mppc_expand(b, compressedLength, compressedType) != 0)
			{
				Logger.log(Logger.ERROR, "Failed to decompress mccp stream");
				return;
			}
			
			byte[] data = new byte[mccp.getResultLength()];
			System.arraycopy(mccp.getHistoryBuffer(), mccp.getResultOffset(), data, 0, mccp.getResultLength());
			//StringManip.print(data, "RESULT: ");
			
			packet = new InputByteStream(data);
		}
		//Logger.log("received packet of type: " + Integer.toHexString(type & 0xff));
		switch(type)
		{
		case PDU_TYPE2_UPDATE:
			readUpdatePDU(packet);
			break;
		case PDU_TYPE2_SYNCHRONIZE:
			readSynchronizePDU(packet);
			break;
		case PDU_TYPE2_CONTROL:
			readControlPDU(packet);
			break;
		case PDU_TYPE2_FONT_LIST:
			readFontListPDU(packet);
			break;
		case PDU_TYPE2_FONT_MAP:
			readFontMapPDU(packet);
			break;
		case PDU_TYPE2_POINTER:
			readPointerUpdatePDU(packet);
			break;
		case PDU_TYPE2_ERROR_INFO:
			Logger.log(Logger.ERROR, "Error info pdu: " + Integer.toHexString(packet.getIntLittleEndian()));
		default:
			Logger.log(Logger.DEBUG, "Unknown pdu received: " + Integer.toHexString(type & 0x000000FF));
//			disconnect();
			break;
		}
		
		
		if(packet.left() > 0)
		{
			//StringManip.print(packet.getByteArray(packet.left()), "PACKET NOT EMPTY. left: " + packet.left());
		}
	}
	
	private byte[] createShareDataHeader(short packetLength, byte pduType, byte pduType2)
	{
		byte[] data = new byte[18];
		System.arraycopy(createShareControlHeader(packetLength, pduType), 0, data, 0, SHARE_CONTROL_HEADER_SIZE);
		BitManip.setLittleEndian(data, 6, shareId);
		//1 byte padding
		data[11] = 0x01;
		//TODO: packet compression
		BitManip.setLittleEndian(data, 12, (short) 0);//(short) (packetLength - 14));
		data[14] = pduType2;
		data[15] = 0;
		BitManip.setLittleEndian(data, 16, (short) 0);
		return data;
	}
	
	private byte[] createShareControlHeader(short packetLength, byte pduType)
	{
		byte[] data = new byte[6];
		BitManip.setLittleEndian(data, 0, packetLength);
		data[2] = pduType;
		data[2] |= 0x01 << 4;
		data[3] = 0x00;
		BitManip.setLittleEndian(data, 4, (short) channelId);
		return data;
	}
	
	private byte readShareControlHeader(InputByteStream packet)
	{
		packet.skip(2);
		byte pduType = (byte) (packet.getShortLittleEndian() & 0x000F);
		packet.skip(2);
		return pduType;
	}
	
	private byte[] createConfirmActivePDU()
	{
		byte[] general = createGeneralCapabilitySet();
		byte[] bitmap = createBitmapCapabilitySet();
		byte[] orders = createOderCapabilitySet();
		byte[] bitmapCache;
		if(serverVersion == SERVER_VERSION_4_0)
		{
			bitmapCache = createRev1BitmapCapabilitySet();
		}
		else
		{
			bitmapCache = createRev2BitmapCapabilitySet();
		}
		//TODO: bmp cache
		byte[] colorTable = createColorTableCacheCabilititySet();
		byte[] windowActivation = createWindowActivationCapabilitySet();
		byte[] control = createControlCapabilitySet();
		byte[] pointer = createPointerCapabilitySet();
		byte[] share = createShareCapabilitySet();
		byte[] input = createInputCapabilitySet();
		byte[] sound = createSoundCapabilitySet();
		byte[] font = createFontCapabilitySet();
		byte[] glyphCache = createGlyphCacheCapabilitySet();
		byte[] multiFragmentUpdate = createMultiFragmentUpdateCapabilitySet();//new byte[] { 26, 0, 8, 0, 1, 0, 0, 0 };
		
		int capabilityLength = general.length + bitmap.length + bitmapCache.length + colorTable.length +
				windowActivation.length + control.length + pointer.length + share.length + input.length +
				sound.length + glyphCache.length + font.length + orders.length + multiFragmentUpdate.length;
		int length = 26 + capabilityLength;
		byte[] data = new byte[length];
		int i = 6;
		System.arraycopy(createShareControlHeader((short) length, PDU_TYPE_CONFIRM_ACTIVE),
				0, data, 0, SHARE_CONTROL_HEADER_SIZE);
		i += BitManip.setLittleEndian(data, i, shareId);
		i += BitManip.setLittleEndian(data, i, (short) 0x03ea);
		i += BitManip.setLittleEndian(data, i, (short) 6);
		i += BitManip.setLittleEndian(data, i, (short) (capabilityLength + 4));
		data[i++] = 'M';
		data[i++] = 'S';
		data[i++] = 'T';
		data[i++] = 'S';
		data[i++] = 'C';
		data[i++] = 0x00;
		i += BitManip.setLittleEndian(data, i, (short) 13);
		i += BitManip.setLittleEndian(data, i, (short) 0);
		System.arraycopy(general, 0, data, i, general.length);
		i += general.length;
		System.arraycopy(bitmap, 0, data, i, bitmap.length);
		i += bitmap.length;
		System.arraycopy(orders, 0, data, i, orders.length);
		i += orders.length;
		System.arraycopy(bitmapCache, 0, data, i, bitmapCache.length);
		i += bitmapCache.length;
		System.arraycopy(colorTable, 0, data, i, colorTable.length);
		i += colorTable.length;
		System.arraycopy(windowActivation, 0, data, i, windowActivation.length);
		i += windowActivation.length;
		System.arraycopy(control, 0, data, i, control.length);
		i += control.length;
		System.arraycopy(pointer, 0, data, i, pointer.length);
		i += pointer.length;
		System.arraycopy(share, 0, data, i, share.length);
		i += share.length;
		System.arraycopy(input, 0, data, i, input.length);
		i += input.length;
		System.arraycopy(sound, 0, data, i, sound.length);
		i += sound.length;
		System.arraycopy(font, 0, data, i, font.length);
		i += font.length;
		System.arraycopy(glyphCache, 0, data, i, glyphCache.length);
		i += glyphCache.length;
		System.arraycopy(multiFragmentUpdate, 0, data, i, multiFragmentUpdate.length);
		i += multiFragmentUpdate.length;
		
		return data;
	}
	
	private byte[] createMultiFragmentUpdateCapabilitySet()
	{
		byte[] data = new byte[8];
		BitManip.setLittleEndian(data, 0, (short) 26);
		BitManip.setLittleEndian(data, 2, (short) 8);
		BitManip.setLittleEndian(data, 4, 0);
		return data;
	}
	
	private void readDemandActivePDU(InputByteStream packet)
	{
		shareId = packet.getIntLittleEndian();
		short lengthDescriptor = packet.getShortLittleEndian();
		short lengthCapabilities = (short) (packet.getShortLittleEndian() - 4);
		packet.skip(lengthDescriptor);
		short numberCapabilities = packet.getShortLittleEndian();
		packet.skip(2);
		for(int i=0; i < numberCapabilities && lengthCapabilities > 0; i++)
		{
			short type = packet.getShortLittleEndian();
			short length = (short) (packet.getShortLittleEndian() - 4);
			switch(type)
			{
				case CAPABILITY_TYPE_GENERAL:
					readGeneralCapabilitySet(packet);
					break;
				case CAPABILITY_TYPE_BITMAP:
					readBitmapCompabilitySet(packet);
					break;
				default:
					packet.skip(length);
					break;
			}
			lengthCapabilities -= (length + 4);
		}
		packet.skip(4);
	}
	
	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240549(v=PROT.10).aspx>
	 * 2.2.7.1.1 General Capability Set (TS_GENERAL_CAPABILITYSET)</a>
	 */
	private byte[] createGeneralCapabilitySet()
	{
		byte[] data = new byte[24];
		BitManip.setLittleEndian(data, 0, (short) 1);
		BitManip.setLittleEndian(data, 2, (short) 24);
		BitManip.setLittleEndian(data, 4, (short) 0);
		BitManip.setLittleEndian(data, 6, (short) 0);
		BitManip.setLittleEndian(data, 8, (short) 0x0200);
		//pad 2
		BitManip.setLittleEndian(data, 12, (short) 0);
		BitManip.setLittleEndian(data, 14, CLIENT_SUPPORT_GENERAL_CAPABILITIES);
		BitManip.setLittleEndian(data, 16, (short) 0);
		BitManip.setLittleEndian(data, 18, (short) 0);
		BitManip.setLittleEndian(data, 20, (short) 0);
		data[22] = CLIENT_SUPPORT_REFRESH_RECT_PDU;
		data[23] = CLIENT_SUPPORT_SUPRESS_OUTPUT_PDU;
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240554(v=PROT.10).aspx>
	 * 2.2.7.1.2 Bitmap Capability Set (TS_BITMAP_CAPABILITYSET)</a>
	 */
	private byte[] createBitmapCapabilitySet()
	{
		byte[] data = new byte[28];
		BitManip.setLittleEndian(data, 0, (short) 2);
		BitManip.setLittleEndian(data, 2, (short) 28);
		short depth = 0;
		switch(highColorDepth)
		{
			case HIGH_COLOR_DEPTH_4BPP:
				depth = 4;
				break;
			case HIGH_COLOR_DEPTH_8BPP:
				depth = 8;
				break;
			case HIGH_COLOR_DEPTH_15BPP:
				depth = 15;
				break;
			case HIGH_COLOR_DEPTH_16BPP:
				depth = 16;
				break;
			case HIGH_COLOR_DEPTH_24BPP:
				depth = 24;
				break;
			default:
				depth = 8;
				break;
		}
		BitManip.setLittleEndian(data, 4, depth);
		BitManip.setLittleEndian(data, 6, (short) 0x0001);
		BitManip.setLittleEndian(data, 8, (short) 0x0001);
		BitManip.setLittleEndian(data, 10, (short) 0x0001);
		BitManip.setLittleEndian(data, 12, (short) desktopWidth);
		BitManip.setLittleEndian(data, 14, (short) desktopHeight);
		//pad 2
		BitManip.setLittleEndian(data, 18, (short) CLIENT_SUPPORT_DESKTOP_SIZE);
		BitManip.setLittleEndian(data, 20, (short) 0x0001);
		BitManip.setLittleEndian(data, 22, (byte) 0);
		BitManip.setLittleEndian(data, 23, (byte) 0);	//TODO: 32bit bitmap support stuffs
		BitManip.setLittleEndian(data, 24, (short) 0x0001);
		BitManip.setLittleEndian(data, 26, (short) 0);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240549(v=PROT.10).aspx>
	 * 2.2.7.1.1 General Capability Set (TS_GENERAL_CAPABILITYSET)</a>
	 */
	private boolean readGeneralCapabilitySet(InputByteStream packet)
	{
		packet.skip(10);	//skip useless stuff
		short flags = packet.getShortLittleEndian();
		if((flags & 0x0001) == 0x0001)
		{
			supportsFastPath = true;
			Logger.log(Logger.INFO, "Server supports fast path output");
		}
		if((flags & 0x0400) == 0x0400)
			supportsNoBitmapCompressionHeader = true;
		if((flags & 0x0004) == 0x0004)
			supportsLongCredentials = true;
		if((flags & 0x0008) == 0x0008)
			supportsAutoReconnect = true;
		if((flags & 0x0010) == 0x0010)
			supportsSaltedChecksum = true;
		packet.skip(6);
		if((packet.getByte() & 0xff) == 0x01)
			supportsRefreshRectPDU = true;
		if((packet.getByte() & 0xff) == 0x01)
			supportsSupressOutputPDU = true;
		return true;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240554(v=PROT.10).aspx>
	 * 2.2.7.1.2 Bitmap Capability Set (TS_BITMAP_CAPABILITYSET)</a>
	 */
	private boolean readBitmapCompabilitySet(InputByteStream packet)
	{
		bitsPerPixel = packet.getShortLittleEndian();
		Logger.log(Logger.INFO, "Session bitmap quality: " + bitsPerPixel + "bpp");
		packet.skip(6);
		desktopWidth = packet.getShortLittleEndian();
		desktopHeight = packet.getShortLittleEndian();
		packet.skip(2);
		if(packet.getShortLittleEndian() == 0x0001)
			supportsDesktopResize = true;
		packet.skip(3);
		drawingFlags = packet.getByte() & 0xff;
		packet.skip(4);
		return true;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240556(v=PROT.10).aspx>
	 * 2.2.7.1.3 Order Capability Set (TS_ORDER_CAPABILITYSET)</a>
	 */
	private byte[] createOderCapabilitySet()
	{
		byte[] data = new byte[88];
		BitManip.setLittleEndian(data, 0, (short) 0x0003);
		BitManip.setLittleEndian(data, 2, (short) 88);
		byte[] terminalDesc = new byte[16];
		System.arraycopy(terminalDesc, 0, data, 4, 16);
		//pad 4
		BitManip.setLittleEndian(data, 24, (short) 1);
		BitManip.setLittleEndian(data, 26, (short) 20);
		//pad 2
		BitManip.setLittleEndian(data, 30, (short) 1);
		BitManip.setLittleEndian(data, 32, (short) 0);
		BitManip.setLittleEndian(data, 34, (short) 0x0002 | 0x0008 | 0x0020
				| CLIENT_SUPPORT_ORDER_SUPPORT_EX_IS_VALID);
		byte[] orderSupport = /*new byte[] {
				0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x01, 0x00, 
				0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00
		};
			
			/*/new byte[32];
			 /* *
			new byte[] {
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
		};*/
		System.arraycopy(orderSupport, 0, data, 36, 32);
		BitManip.setLittleEndian(data, 68, (short) 0);
		BitManip.setLittleEndian(data, 70, (short) CLIENT_SUPPORT_ORDER_SUPPORT_EXTRA_FLAGS);
		//pad 4
		BitManip.setLittleEndian(data, 76, 230400);
		//pad 2
		//pad 2
		BitManip.setLittleEndian(data, 84, (short) 0x04e4);
		//pad 2
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc241564(v=PROT.10).aspx>
	 * 2.2.1.1 Color Table Cache Capability Set (TS_COLORTABLE_CAPABILITYSET)</a>
	 */
	private byte[] createColorTableCacheCabilititySet()
	{
		byte[] data = new byte[8];
		BitManip.setLittleEndian(data, 0, (short) 0x000A);
		BitManip.setLittleEndian(data, 2, (short) 8);
		BitManip.setLittleEndian(data, 4, (short) 6);
		BitManip.setLittleEndian(data, 6, (short) 0);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240569(v=PROT.10).aspx>
	 * 2.2.7.2.3 Window Activation Capability Set (TS_WINDOWACTIVATION_CAPABILITYSET)</a>
	 */
	private byte[] createWindowActivationCapabilitySet()
	{
		byte[] data = new byte[12];
		BitManip.setLittleEndian(data, 0, (short) 7);
		BitManip.setLittleEndian(data, 2, (short) 12);
		BitManip.setLittleEndian(data, 4, (short) 0);
		BitManip.setLittleEndian(data, 6, (short) 0);
		BitManip.setLittleEndian(data, 8, (short) 0);
		BitManip.setLittleEndian(data, 10, (short) 0);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240568(v=PROT.10).aspx>
	 * 2.2.7.2.2 Control Capability Set (TS_CONTROL_CAPABILITYSET)</a>
	 */
	private byte[] createControlCapabilitySet()
	{
		byte[] data = new byte[12];
		BitManip.setLittleEndian(data, 0, (short) 5);
		BitManip.setLittleEndian(data, 2, (short) 12);
		BitManip.setLittleEndian(data, 4, (short) 0);
		BitManip.setLittleEndian(data, 6, (short) 0);
		BitManip.setLittleEndian(data, 8, (short) 2);
		BitManip.setLittleEndian(data, 10, (short) 2);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240562(v=PROT.10).aspx>
	 * 2.2.7.1.5 Pointer Capability Set (TS_POINTER_CAPABILITYSET)</a>
	 */
	private byte[] createPointerCapabilitySet()
	{
		byte[] data = new byte[10];
		BitManip.setLittleEndian(data, 0, (short) 8);
		BitManip.setLittleEndian(data, 2, (short) 10);
		BitManip.setLittleEndian(data, 4, (short) 0x0001);
		BitManip.setLittleEndian(data, 6, (short) 20);
		BitManip.setLittleEndian(data, 8, (short) 21);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240570(v=PROT.10).aspx>
	 * 2.2.7.2.4 Share Capability Set (TS_SHARE_CAPABILITYSET)</a>
	 */
	private byte[] createShareCapabilitySet()
	{
		byte[] data = new byte[8];
		BitManip.setLittleEndian(data, 0, (short) 9);
		BitManip.setLittleEndian(data, 2, (short) 8);
		BitManip.setLittleEndian(data, 4, (short) 0);
		BitManip.setLittleEndian(data, 6, (short) 0);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240563(v=PROT.10).aspx>
	 * 2.2.7.1.6 Input Capability Set (TS_INPUT_CAPABILITYSET)</a>
	 */
	private byte[] createInputCapabilitySet()
	{
		byte[] data = new byte[88];
		BitManip.setLittleEndian(data, 0, (short) 13);
		BitManip.setLittleEndian(data, 2, (short) 88);
		BitManip.setLittleEndian(data, 4, (short) CLIENT_SUPPORT_INPUT_SUPPORT_FLAGS);
		//pad 2
		BitManip.setLittleEndian(data, 8, KEYBOARD_LAYOUT);
		BitManip.setLittleEndian(data, 12, KEYBOARD_TYPE);
		BitManip.setLittleEndian(data, 16, KEYBOARD_SUB_TYPE);
		BitManip.setLittleEndian(data, 20, KEYBOARD_FUNCTION_KEY);
		int n = 24;
		short[] imeFileName = IME_FILE_NAME;
		for(int i=0; i < 32; i++)
		{
			byte[] val = BitManip.toByteArray(imeFileName[i]);
			data[n++] = val[0];
			data[n++] = val[1];
		}
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240565(v=PROT.10).aspx>
	 * 2.2.7.1.8 Glyph Cache Capability Set (TS_GLYPHCACHE_CAPABILITYSET)</a>
	 */
	private byte[] createGlyphCacheCapabilitySet()
	{
		byte[] data = new byte[52];
		BitManip.setLittleEndian(data, 0, (short) 16);
		BitManip.setLittleEndian(data, 2, (short) 52);
		for(int i=0; i < 10; i++)
		{
			BitManip.setLittleEndian(data, 4 + (i * 4), CLIENT_SUPPORT_GLYPH_CACHE_ENTRIES[i]);
			BitManip.setLittleEndian(data, 4 + (i * 4) + 2, CLIENT_SUPPORT_GLYPH_MAX_CELL_SIZE[i]);
		}
		BitManip.setLittleEndian(data, 44, (short) CLIENT_SUPPORT_GLYPH_FRAG_ENTRIES);
		BitManip.setLittleEndian(data, 46, (short) CLIENT_SUPPORT_GLYPH_FRAG_MAX_CELL_SIZE);
		BitManip.setLittleEndian(data, 48, (short) CLIENT_SUPPORT_GLYPH_SUPPORT_LEVEL);
		//pad 2
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240571(v=PROT.10).aspx>
	 * 2.2.7.2.5 Font Capability Set (TS_FONT_CAPABILITYSET)</a>
	 */
	private byte[] createFontCapabilitySet()
	{
		byte[] data = new byte[8];
		BitManip.setLittleEndian(data, 0, (short) 14);
		BitManip.setLittleEndian(data, 2, (short) 8);
		BitManip.setLittleEndian(data, 4, (short) 0x0001);
		//pad 2
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240552(v=PROT.10).aspx>
	 * 2.2.7.1.11 Sound Capability Set (TS_SOUND_CAPABILITYSET)</a>
	 */
	private byte[] createSoundCapabilitySet()
	{
		byte[] data = new byte[8];
		BitManip.setLittleEndian(data, 0, (short) 12);
		BitManip.setLittleEndian(data, 2, (short) 8);
		BitManip.setLittleEndian(data, 4, (short) CLIENT_SUPPORT_SOUND_FLAGS);
		BitManip.setLittleEndian(data, 6, (short) 0);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240559(v=PROT.10).aspx>
	 * 2.2.7.1.4.1 Revision 1 (TS_BITMAPCACHE_CAPABILITYSET)</a>
	 */
	private byte[] createRev1BitmapCapabilitySet()
	{
		byte[] data = new byte[40];
		BitManip.setLittleEndian(data, 0, (short) 4);
		BitManip.setLittleEndian(data, 2, (short) 40);
		//pad 24
		BitManip.setLittleEndian(data, 28, (short) CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_1_ENTRIES);
		BitManip.setLittleEndian(data, 30, (short) CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_1_CELL_SIZE);
		BitManip.setLittleEndian(data, 32, (short) CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_2_ENTRIES);
		BitManip.setLittleEndian(data, 34, (short) CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_2_CELL_SIZE);
		BitManip.setLittleEndian(data, 36, (short) CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_3_ENTRIES);
		BitManip.setLittleEndian(data, 38, (short) CLIENT_SUPPORT_BITMAP_CACHE_REV1_CACHE_3_CELL_SIZE);
		return data;
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240560(v=PROT.10).aspx>
	 * 2.2.7.1.4.2 Revision 2 (TS_BITMAPCACHE_CAPABILITYSET_REV2)</a>
	 */
	private byte[] createRev2BitmapCapabilitySet()
	{
		byte[] data = new byte[40];
		BitManip.setLittleEndian(data, 0, (short) 19);
		BitManip.setLittleEndian(data, 2, (short) 40);
		BitManip.setLittleEndian(data, 4, (short) 0);
		//BitManip.set(data, 6, (short) 0);
		data[6] = 0;
		data[7] = 3;
		int cellInfo = CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_1_NUM_ENTRIES & 0xfffffffe;
		BitManip.setLittleEndian(data, 8, cellInfo);
		cellInfo = CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_2_NUM_ENTRIES & 0xfffffffe;
		BitManip.setLittleEndian(data, 12, cellInfo);
		cellInfo = CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_3_NUM_ENTRIES & 0xfffffffe;
		BitManip.setLittleEndian(data, 16, cellInfo);
		cellInfo = CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_4_NUM_ENTRIES & 0xfffffffe;
		BitManip.setLittleEndian(data, 20, cellInfo);
		cellInfo = CLIENT_SUPPORT_BITMAP_CACHE_REV2_CACHE_5_NUM_ENTRIES & 0xfffffffe;
		BitManip.setLittleEndian(data, 24, cellInfo);
		//last 12 bytes are padding
		return data;
	}
	
	private byte[] createSynchronizePDU()
	{
		byte[] data = new byte[22];
		System.arraycopy(createShareDataHeader((short) 22, PDU_TYPE_DATA , PDU_TYPE2_SYNCHRONIZE),
				0, data, 0, SHARE_DATA_HEADER_SIZE);
		BitManip.setLittleEndian(data, 18, (short) 1);
		BitManip.setLittleEndian(data, 20, channelId);
		return data;
	}
	
	private byte[] createClientControlPDU(short action)
	{
		byte[] data = new byte[26];
		System.arraycopy(createShareDataHeader((short) 26, PDU_TYPE_DATA, PDU_TYPE2_CONTROL),
				0, data, 0, SHARE_DATA_HEADER_SIZE);
		BitManip.setLittleEndian(data, 18, action);
		BitManip.setLittleEndian(data, 20, (short) 0);
		BitManip.setLittleEndian(data, 22, 0);
		return data;
	}
	
	private byte[] createClientPersistentBitmapCachePDU()
	{
		byte[] data = new byte[42];
		System.arraycopy(createShareDataHeader((short) 42, PDU_TYPE_DATA,
				PDU_TYPE2_BITMAPCACHE_PERSISTENT_LIST), 0, data, 0, SHARE_DATA_HEADER_SIZE);
		BitManip.setLittleEndian(data, 18, (short) 0);
		BitManip.setLittleEndian(data, 20, (short) 0);
		BitManip.setLittleEndian(data, 22, (short) 0);
		BitManip.setLittleEndian(data, 24, (short) 0);
		BitManip.setLittleEndian(data, 26, (short) 0);

		BitManip.setLittleEndian(data, 28, (short) 0);
		BitManip.setLittleEndian(data, 30, (short) 0);
		BitManip.setLittleEndian(data, 32, (short) 0);
		BitManip.setLittleEndian(data, 34, (short) 0);
		BitManip.setLittleEndian(data, 36, (short) 0);
		BitManip.setLittleEndian(data, 38, (byte) 0x03);
		BitManip.setLittleEndian(data, 39, (byte) 0);		//pad
		BitManip.setLittleEndian(data, 40, (short) 0);		//pad
		//no entries... for now
		return data;
	}
	
	//TODO: possible bug on 4.0 servers: rdesktop sends this twice, once with 0x0001 and then with 0x0002,
	// not just with 0x0003.
	private byte[] createFontListPDU()
	{
		byte[] data = new byte[26];
		System.arraycopy(createShareDataHeader((short) 26, PDU_TYPE_DATA,
				PDU_TYPE2_FONT_LIST), 0, data, 0, SHARE_DATA_HEADER_SIZE);
		BitManip.setLittleEndian(data, 18, (short) 0);
		BitManip.setLittleEndian(data, 20, (short) 0);
		BitManip.setLittleEndian(data, 22, (short) 0x0003);
		BitManip.setLittleEndian(data, 24, (short) 0x0032);
		return data;
	}
	
	private void readSynchronizePDU(InputByteStream packet)
	{
		//Nothing interesting here
		packet.skip(4);
	}
	
	private short readControlPDU(InputByteStream packet)
	{
		short action = packet.getShortLittleEndian();
		packet.skip(6);
		return action;
	}
	
	private void readFontListPDU(InputByteStream packet)
	{
		//Nothing interesting here
		packet.skip(8);
	}
	
	private void readFontMapPDU(InputByteStream packet)
	{
		//Nothing interesting here
		packet.skip(8);
	}
	
	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240614(v=PROT.10).aspx>
	 * 2.2.9.1.1.4 Server Pointer Update PDU (TS_POINTER_PDU)</a>
	 */
	private void readPointerUpdatePDU(InputByteStream packet)
	{
		short type = packet.getShortLittleEndian();
		packet.skip(2);			//pad 2
		switch(type)
		{
		case 0x0001:
			int pointerType = packet.getIntLittleEndian();
			if(pointerType == 0)
				//Logger.log("Hidden Pointer Received");
				canvas.hideCursor();
			else if(pointerType == 0x7F00)
				Logger.log(Logger.INFO, "Pointer System Default Received");
			else
				Logger.log(Logger.INFO, "Unknown Pointer Received");
			break;
		case 0x0003:
			canvas.cursorPositionChanged(packet.getShortLittleEndian(),
					packet.getShortLittleEndian());
			break;
		case 0x0006:
			readColorPointerUpdate(packet);
			break;
		case 0x0007:
			int index = packet.getShortLittleEndian();
			Cursor cursor = cursors.get(new Integer(index));
			if(cursor != null)
			{
				Logger.log(Logger.INFO, "Switching to pointer at index:" + index);
				canvas.setCursor(cursor);
				canvas.showCursor();
			}
			break;
		case 0x0008:
			short xorBitsPerPixel = packet.getShortLittleEndian();
			if(xorBitsPerPixel == 1)
				return;
			readColorPointerUpdate(packet);
			break;
		default:
			Logger.log(Logger.DEBUG, "Unsupported Pointer Update PDU Type Received: " + Integer.toHexString(type));
			break;
		}
	}

	/**
	 * <a href=http://msdn.microsoft.com/en-us/library/cc240607(v=PROT.10).aspx>
	 * 2.2.9.1.1.3 Server Graphics Update PDU (TS_GRAPHICS_PDU)</a>
	 */
	private void readUpdatePDU(InputByteStream packet)
	{
		short type = packet.getShortLittleEndian();
		switch(type)
		{
		case 0x00000000:		//Orders
			//Logger.log("Update PDU type 0x0001 received, don't know what to do with it");
			packet.skip(2);
			int orderCount = packet.getShortLittleEndian();
			packet.skip(2);
			for(int i=0; i < orderCount; i++)
			{
				//orders.processOrder(packet);
			}
			break;
		case 0x00000001:		//Bitmap
			updateCanvas(packet);
			break;
		case 0x00000002:		//Palette
			Logger.log(Logger.DEBUG, "Update PDU type 0x0002 received, don't know what to do with it");
			break;
		case 0x00000003:		//Synchronize
			//pad 2
			packet.skip(2);
			break;
		default:
			Logger.log(Logger.DEBUG, "Unsupported Update PDU Type Received: " + Integer.toHexString(type));
			break;
		}
	}
	
	private void updateCanvas(InputByteStream packet)
	{
		short numRect = packet.getShortLittleEndian();
		for(int p=0; p < numRect; p++)
		{
			short left = packet.getShortLittleEndian();
			short top = packet.getShortLittleEndian();
			short right = packet.getShortLittleEndian();
			short bottom = packet.getShortLittleEndian();
			short width = packet.getShortLittleEndian();
			short height = packet.getShortLittleEndian();
			short bitsPerPixel = packet.getShortLittleEndian();
			short flags = packet.getShortLittleEndian();
			short length = packet.getShortLittleEndian();
			
			if(bottom == top && left == right && bottom == 0 && left == 0)
			{
				packet.getByteArray(length);
				continue;
			}
			
            int clippedWidth = right - left + 1;
            int clippedHeight = bottom - top + 1;
	
			int bytesPerPixel = (bitsPerPixel + 7) / 8;

			if(flags == 0)
			{
				int[] img = RLE.convertToInt(packet.getByteArray(length), bytesPerPixel, bitsPerPixel);
				canvas.setCanvasBottomUp(img, width, height, left, top, clippedWidth, clippedHeight);
				continue;
			}
			else if((flags & 0x0400) == 0x0400)
			{
				
			}
			else
			{
				packet.skip(2);
				length = packet.getShortLittleEndian();
				packet.skip(4);
			}
			int[] img;
			try{
				img = RLE.decompressInt(width, height, length, packet.getByteArray(length), bytesPerPixel, bitsPerPixel);
			}catch(Exception e){
				e.printStackTrace();
				packet.getByteArray(packet.available());
				return;
			}
			if(img != null)
			{
				canvas.setCanvas(img, width, height, left, top, clippedWidth, clippedHeight);
			}
			else
			{
				Logger.log(Logger.DEBUG, "Got NULL img in updateCanvas");
			}
			continue;
		}
	}
	
	private void readColorPointerUpdate(InputByteStream packet)
	{
		short cursorIndex = packet.getShortLittleEndian();
		short hotspotX = packet.getShortLittleEndian();
		short hotspotY = packet.getShortLittleEndian();
		short width = packet.getShortLittleEndian();
		short height = packet.getShortLittleEndian();
		short andMaskLength = packet.getShortLittleEndian();
		short xorMaskLength = packet.getShortLittleEndian();

		int bytesPerPixel = xorMaskLength / (width * height);

		byte[] xorMask = packet.getByteArray(xorMaskLength);
		byte[] andMask = packet.getByteArray(andMaskLength);

		int size = width * height;
		int[] img = new int[size];
		int index = (size - width);

		int andIndex = 0;
		int orIndex = 0;
		int mask;
		for(int i=0; i < height; i++){
			mask = 0x80;
			for(int n=0; n < width; n++){
				img[index] = 0x00000000;
				img[index] |= xorMask[orIndex++] & 0x000000ff;
				img[index] |= (xorMask[orIndex++] << 8) & 0x0000ff00;
				img[index] |= (xorMask[orIndex++] << 16) & 0x00ff0000;
				if(bytesPerPixel == 4)
					orIndex++;
				if((andMask[andIndex] & mask) == 0)
					img[index] |= 0xff000000;
				index++;
				mask >>= 1;
				if(mask == 0){
					mask = 0x80;
					andIndex++;
				}
			}
			if(andIndex % 2 != 0)
				andIndex++;
			if(orIndex % 2 != 0)
				orIndex++;
			index -= 2 * width;
		}
		Cursor cursor = new Cursor(img, width, height, hotspotX, hotspotY);
		cursors.put(new Integer(cursorIndex), cursor);
	}
	
	public short getDesktopWidth()
	{
		return desktopWidth;
	}
	
	public short getDesktopHeight()
	{
		return desktopHeight;
	}
	
	public boolean connect()
	{
		try
		{
			net.connect();
		}
		catch(Exception e)
		{
			Logger.log(e, "Could not connect");
			return false;
		}
		connected = true;
		//Step 1
		sendPacket(CONNECTION_REQUEST);
		//Step 2
		readPacket(CONNECTION_CONFIRM);
		//Step 3
		sendPacket(MCS_CONNECT_INITIAL);
		//Step 4
		readPacket(MCS_CONNECT_RESPONSE);
		//Step 5
		sendPacket(ERECT_DOMAIN_REQUEST);
		//Step 6
		sendPacket(ATTACH_USER_REQUEST);
		//Step 7
		readPacket(ATTACH_USER_CONFIRM);
		//Step 8
		ChannelJoinRequest channelRequest = new ChannelJoinRequest(channelId);
		sendPacket(CHANNEL_JOIN_REQUEST, channelRequest);
		if(readPacket(CHANNEL_JOIN_CONFIRM) == null)
			return false;
		//Step 9
		for(int i=0; i < otherChannels.length; i++)
		{
			channelRequest = new ChannelJoinRequest(otherChannels[i]);
			sendPacket(CHANNEL_JOIN_REQUEST, channelRequest);
			if(readPacket(CHANNEL_JOIN_CONFIRM) == null)
				return false;
		}
		//Step 10
		//TODO: check for security or encryption being 0
		sendPacket(CLIENT_SECURITY_EXCHANGE);
		//Step 11
		sendPacket(CLIENT_INFO_DATA);
		//Step 12
		readPacket(SERVER_LICENSE_ERROR_PDU);
		//Step 13
		readPacket(SERVER_DEMAND_ACTIVE_PDU);
		//Step 14
		sendPacket(CONFIRM_ACTIVE_PDU);
		//Step 15
		sendPacket(SYNCHRONIZE_PDU);
		//Step 16
		sendPacket(CONTROL_PDU_COOPERATE);
		//Step 17
		sendPacket(CONTROL_PDU_REQUEST_CONTROL);
		//Step 18
		sendPacket(PERSISTENT_KEY_LIST_PDU);
		//Step 19
		sendPacket(FONT_LIST_PDU);
		
		startMainReceiveLoop();
		
		return true;
	}
	
	public boolean isConnected()
	{
		return connected;
	}
}
