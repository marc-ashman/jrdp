package com.jrdp.core.remote.rdp;

import com.jrdp.core.encryption.EncryptionSession;
import com.jrdp.core.encryption.RC4Session;
import com.jrdp.core.encryption.RSA;
import com.jrdp.core.remote.rdp.replies.RdpReply;
import com.jrdp.core.remote.rdp.replies.SecureReply;
import com.jrdp.core.remote.rdp.requests.RdpRequest;
import com.jrdp.core.util.BitManip;
import com.jrdp.core.util.InputByteStream;
import com.jrdp.core.util.Logger;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

import java.security.SecureRandom;

class Secure extends MCS
{
	/**
	 * RDP 4.0 bulk compression
	 */
	static final int COMPRESSION_8K = 0 << 9;
	/**
	 * RDP 5.0 bulk compression
	 */
	static final int COMPRESSION_64K = 1 << 9;
	/**
	 * RDP 6.0 bulk compression
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc241682(v=PROT.10).aspx>
	 * [MS-RDPEGDI]: Remote Desktop Protocol: Graphics Device Interface (GDI) Acceleration Extensions<br>
	 * Section 3.1.8.1: RDP 6.0</a>
	 */
	static final int COMPRESSION_TYPE_RDP6 = 2 << 9;
	/**
	 * RDP 6.1 bulk compression
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc432762(v=PROT.10).aspx>
	 * [MS-RDPEGDI]: Remote Desktop Protocol: Graphics Device Interface (GDI) Acceleration Extensions<br>
	 * Section 3.1.8.2: RDP 6.1</a>
	 */
	static final int COMPRESSION_TYPE_RDP61 = 3 << 9;
	/**
	 * Indicates the client has a mouse
	 */
	static final int INFO_MOUSE = 0x00000001;
	/**
	 * Tells server to not require Ctrl+Alt+Del on logon prompt
	 */
	static final int INFO_DIABLECTRLALTDEL = 0x00000002;
	/**
	 * Requests that client logs on with given username and password
	 */
	static final int INFO_AUTOLOGON = 0x00000008;
	static final int INFO_UNICODE = 0x00000010;
	static final int INFO_MAXIMIZE_SHELL = 0x00000020;
	static final int INFO_LOGON_NOTIFY = 0x00000040;
	/**
	 * Indicates that compression information sent in is valid. Also implies that client
	 * supports RDP 4.0 8k bulk compression
	 */
	static final int INFO_COMPRESSION_VALID = 0x00000080;
	/**
	 * Indicates that the client supports RDP 5.0 64K bulk compression
	 */
	static final int INFO_COMPRESSION_TYPE_64K = 0x00000200;
	/**
	 * Requests that audio played on server be played on client
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240933(v=PROT.10).aspx>
	 * [MS-RDPEA]: Remote Desktop Protocol: Audio Output Virtual Channel Extension</a>
	 */
	static final int INFO_REMOTECONSOLEAUDIO = 0x00002000;
	/**
	 * Indicates all client to server info is encrypted
	 */
	static final int INFO_FORCE_ENCRYPTED_CS_PDU = 0x00004000;
	//public static final int INFO_LOGONERRORS = 0x00010000;
	/**
	 * Forces audio redirection/playback to be disabled
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240933(v=PROT.10).aspx>
	 * [MS-RDPEA]: Remote Desktop Protocol: Audio Output Virtual Channel Extension</a>
	 */
	static final int INFO_NOAUDIOPLAYBACK = 0x00080000;
	/**
	 * Indicates redirection of client audio to server is supported
	 * @see <a href=http://msdn.microsoft.com/en-us/library/dd342521(v=PROT.10).aspx>
	 * [MS-RDPEAI]: Remote Desktop Protocol: Audio Input Redirection Virtual Channel Extension</a>
	 */
	static final int INFO_AUDIOCAPTURE = 0x00200000;
	/**
	 * Forces video redirection/playback to be disabled
	 * @see <a href=http://msdn.microsoft.com/en-us/library/dd342975(v=PROT.10).aspx>
	 * [MS-RDPEV]: Remote Desktop Protocol: Video Redirection Virtual Channel Extension</a>
	 */
	static final int INFO_VIDEO_DISABLE = 0x00400000;
	static final int PERF_DISABLE_WALLPAPER = 0x00000001;
	static final int PERF_DISABLE_FULL_WINDOW_DRAG = 0x00000002;
	static final int PERF_DISABLE_MENU_ANIMATIONS = 0x00000004;
	static final int PERF_DISABLE_THEMING = 0x00000008;
	static final int PERF_DISABLE_CURSOR_SHADOW = 0x00000020;
	static final int PERF_DISABLE_CURSOR_BLINKING = 0x00000040;
	static final int PERF_ENABLE_FONT_SMOOTHING = 0x00000080;
	static final int PERF_ENABLE_DESKTOP_COMPOSITION = 0x00000100;
	static final short SEC_ENCRYPT = 0x0008;
	
	public static final int MD5_DIGEST_LENGTH = 16;
	public static final int SHA1_DIGEST_LENGTH = 20;
	private static final int CLIENT_RANDOM_SIZE = 32;
	private static final byte[][] masterSecretValues = {	{ 0x41 },
															{ 0x42, 0x42 },
															{ 0x43, 0x43, 0x43 } };
	private static final byte[][] sessionKeyBlobValues = {	{ 0x58 },
															{ 0x59, 0x59 },
															{ 0x5a, 0x5a, 0x5a } };
	
	private byte[] clientRandom = new byte[CLIENT_RANDOM_SIZE];

	protected EncryptionSession encryption;
	protected boolean encrypt;

	public Secure(NetworkManager net, RdpConnectionInfo info)
	{
		super(net, info);
		new SecureRandom().nextBytes(clientRandom);
		if(encryptionMethod != Constants.ENCRYPTION_NONE && encryptionLevel != ENCRYPTION_LEVEL_NONE)
			encrypt = false;
		else
			encrypt = true;
	}
	
	protected void sendPacket(int type, RdpRequest request, RdpPacket packet)
	{
		boolean addSecurityHeader = true;
		switch(type)
		{
			case CLIENT_SECURITY_EXCHANGE:
				byte[] ex = createClientSecurityExchangeRequest();
				packet.addPacketPeiceAtStart(ex);
				break;
			case CLIENT_INFO_DATA:
				int size = -1;
				switch(encryptionMethod)
				{
					case Constants.ENCRYPTION_NONE:
						size = 0;
						break;
					case Constants.ENCRYPTION_128BIT:
						size = 128;
						break;
					case Constants.ENCRYPTION_40BIT:
						size = 40;
						break;
					case Constants.ENCRYPTION_56BIT:
						size = 56;
						break;
					case Constants.ENCRYPTION_FIPS:
						size = -1;
						break;
				}
				if(size > 0 && encrypt)
				{
					setupForNonFipsEncryption(this.clientRandom, serverRandom, size);
				}
				packet.addPacketPeiceAtStart(createClientInfoPDU(), encryption);
				break;
			case CONFIRM_ACTIVE_PDU:		case SYNCHRONIZE_PDU:
			case CONTROL_PDU_COOPERATE:		case CONTROL_PDU_REQUEST_CONTROL:
			case PERSISTENT_KEY_LIST_PDU:	case FONT_LIST_PDU:
			case UNKNOWN_PDU:				case INPUT_EVENT:
				//do nothing... just avoiding the default case to happen
				break;
			default:
				addSecurityHeader = false;
				break;
		}
		if(addSecurityHeader)
		{
			packet.addPacketPeiceAtStart(createSecurityHeader(type, packet));
			packet.addPacketPeiceAtStart(createSendDataRequestPDU(type, packet.getLength()));
		}
		super.sendPacket(type, request, packet);
	}

	protected RdpReply readPacket(int type, InputByteStream packet)
	{
		RdpReply reply = super.readPacket(type, packet);
		boolean readSecurityHeader = false;
		switch(type)
		{
			case UNKNOWN_PDU:
				if(reply.isFastPath())
					break;
			case SERVER_LICENSE_ERROR_PDU:		case SERVER_DEMAND_ACTIVE_PDU:
			case CONFIRM_ACTIVE_PDU:			case SYNCHRONIZE_PDU:
			case CONTROL_PDU_COOPERATE:			case CONTROL_PDU_REQUEST_CONTROL:
			case PERSISTENT_KEY_LIST_PDU:		case FONT_LIST_PDU:
				reply = new SecureReply(reply);
				readSecurityHeader = true;
				break;
		}
		
		if(readSecurityHeader)
		{
			//http://msdn.microsoft.com/en-us/library/cc240794(v=PROT.10).aspx
			if(!readSendDataIndication(packet, (SecureReply) reply))
				return null;
			if(!readSecurityHeader(packet, (SecureReply) reply))
				return null;
			if(((SecureReply) reply).isEncypted())
			{
				packet.decryptStream(encryption);
			}
		}
		
		if(type == SERVER_LICENSE_ERROR_PDU)
		{
			if(((SecureReply) reply).isLicensingPdu())
			{
				if(!readValidClientLicenseData(packet))
					return null;
			}
			else
			{
				Logger.log(Logger.ERROR, "Expected server license error PDU... didn't get one");
				return null;
			}
		}
		
		return reply;
	}
	
	/**
	 * Share Control Header
	 * @return 
	 */
	public void readShareControlHeader()
	{
		
	}
	
	/**
	 * Encrypts the client random, using the RSA key given by the server
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240781(v=PROT.10).aspx>
	 * 5.3.4 Client and Server Random Values</a>
	 */
	private byte[] encryptClientRandom()
	{
		byte[] encryptedRandom = RSA.encrypt(rsaKey.getModulus(), rsaKey.getExponent(), clientRandom);
		int keyLen = rsaKey.getModulusLen();
		if(encryptedRandom.length != keyLen)
		{
			byte[] temp = new byte[keyLen];
			System.arraycopy(encryptedRandom, 0, temp, 0, encryptedRandom.length);
			encryptedRandom = temp;
		}
		return encryptedRandom;
	}

	private boolean readSecurityHeader(InputByteStream packet, SecureReply reply)
	{
		short flags = packet.getShortLittleEndian();
		//skip the hi flags
		packet.skip(2);
		reply.setSecurityFlags(flags);
		if((flags & SEC_ENCRYPT) == SEC_ENCRYPT)
		{
			if(encryptionLevel == ENCRYPTION_LEVEL_NONE || encryptionLevel == ENCRYPTION_LEVEL_LOW)
			{
				//http://msdn.microsoft.com/en-us/library/cc240579(v=PROT.10).aspx
				// (already read)
				reply.setSecurityHeaderLength(4);
				return true;
			}
			else if(encryptionMethod == Constants.ENCRYPTION_40BIT ||
					encryptionMethod == Constants.ENCRYPTION_56BIT ||
					encryptionMethod == Constants.ENCRYPTION_128BIT)
			{
				reply.setSecurityHeaderLength(12);
				//http://msdn.microsoft.com/en-us/library/cc240580(v=PROT.10).aspx
				reply.setMac(packet.getByteArray(8));
				return true;
			}
			else if(encryptionMethod == Constants.ENCRYPTION_FIPS)
			{
				//http://msdn.microsoft.com/en-us/library/cc240581(v=PROT.10).aspx
				//TODO: FIPS
				reply.setSecurityHeaderLength(4);
				return false;
			}
		}
		//TODO: check for forced encryption
		else if(encryptionLevel == ENCRYPTION_LEVEL_CLIENT_COMPATIBLE ||
				encryptionLevel == ENCRYPTION_LEVEL_HIGH || encryptionLevel == ENCRYPTION_LEVEL_FIPS)
		{
			//http://msdn.microsoft.com/en-us/library/cc240579(v=PROT.10).aspx
			// (already read)
			reply.setSecurityHeaderLength(4);
			return true;
		}
		else
		{
			Logger.log(Logger.ERROR, "Reached invalid state when receiving security header");
			return false;
		}
		return true;
	}
	
	private byte[] createSecurityHeader(int packetType, RdpPacket packet)
	{
		short type = 0x0000;
		short encrypted = SEC_ENCRYPT;
		short secureChecksum = 0x0800;
		//special case to possibly include a basic security header (2.2.8.1.1.2.1) if encryption level is none (0)
		if(packetType == CLIENT_INFO_DATA)
		{
			type = 0x0040;
			if(!encrypt)
			{
				//http://msdn.microsoft.com/en-us/library/cc240579(v=PROT.10).aspx
				byte[] data = new byte[4];
				BitManip.setLittleEndian(data, 0, (short) (type));
				BitManip.setLittleEndian(data, 2, (short) 0x0000);
				return data;
			}
		}
		else if(packetType == SERVER_LICENSE_ERROR_PDU)
		{
			type = 0x0080;
			byte[] data = new byte[12];
			BitManip.setLittleEndian(data, 0, type);
			BitManip.setLittleEndian(data, 2, (short) 0x0000);
			byte[] mac = BitManip.reverse(encryption.createMac(packet.packPeices()));
			System.arraycopy(mac, 0, data, 4, mac.length);
			return data;
		}
		else if(packetType == CLIENT_SECURITY_EXCHANGE)
		{
			type = (short) 0x0001;
			//http://msdn.microsoft.com/en-us/library/cc240579(v=PROT.10).aspx
			byte[] data = new byte[4];
			BitManip.setLittleEndian(data, 0, (short) (type));
			BitManip.setLittleEndian(data, 2, (short) 0x0000);
			return data;
		}
		if(encryptionMethod == Constants.ENCRYPTION_FIPS)
		{
			//http://msdn.microsoft.com/en-us/library/cc240581(v=PROT.10).aspx
			//TODO: fips
		}
		else if(encrypt)
		{
			//http://msdn.microsoft.com/en-us/library/cc240580(v=PROT.10).aspx
			byte[] data = new byte[12];
			BitManip.setLittleEndian(data, 0, (short) (type | encrypted));
			BitManip.setLittleEndian(data, 2, (short) 0x0000);
			byte[] mac = BitManip.reverse(encryption.createMac(packet.packPeices()));
			System.arraycopy(mac, 0, data, 4, mac.length);
			return data;
		}
		return null;
	}

	/**
	 * @see [T125] Section 10.31 (Doesn't exist?)
	 * @see [T125] Section 7 Part 7 & 10
	 * @see (First use) Example: <a href=http://msdn.microsoft.com/en-us/library/cc240916(v=PROT.10).aspx>
	 * 4.1.11 Server License Error PDU - Valid Client</a>
	 */
	private boolean readSendDataIndication(InputByteStream packet, SecureReply reply)
	{
		if((packet.getByte() & 0xff) != 0x68)		//send data indication structure
		{
			Logger.log(Logger.ERROR, "Invalid choice value received when reading a security header");
			return false;
		}
		packet.skip(4);
		if((packet.getByte() & 0xff) == 0x30)
		{
			Logger.log(Logger.DEBUG, "POSSIBLE ERROR PDU");
		}
		int lengthByte1 = packet.getByte() & 0xff;
		int length = 0;
		if((lengthByte1 & 0x80) == 0x80)
		{
			int lengthByte2 = packet.getByte() & 0xff;
			length = BitManip.mergeToShort((byte) (lengthByte1 & 0x7f), (byte) lengthByte2);
		}
		else
		{
			length = lengthByte1;
		}
		reply.setSecureUserDataLength(length);
		return true;
	}

	/**
	 * @see [T125] Section 10.30 (Doesn't exist?)
	 * @see [T125] Section 7 Part 7 & 10
	 * @see (First use) Example: <a href=http://msdn.microsoft.com/en-us/library/cc240839(v=PROT.10).aspx>
	 * 4.1.9 Client Security Exchange PDU</a>
	 */
	private byte[] createSendDataRequestPDU(int packetType, int length)
	{
		//http://msdn.microsoft.com/en-us/library/cc240472(v=PROT.10).aspx
		boolean bigLength = false;
		if(length > 127)
			bigLength = true;
		byte[] data = new byte[7 + (bigLength ? 1 : 0)];
		data[0] = 0x64;
		BitManip.set(data, 1, initiator);
		BitManip.set(data, 3, channelId);
		data[5] = 0x70;
		if(bigLength)
			BitManip.set(data, 6, (short) (length | 0x8000));
		else
			data[6] = (byte) length;
		return data;
	}
    
	/**
	 * @param clientRandom random generated by the client
	 * @param serverRandom random generated by the server
	 * @param keyLength length of keys being requested (40, 56 or 128)
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240785(v=PROT.10).aspx>
	 * 5.3.5.1 Non-FIPS Initial Key Generation</a>
	 */
	public void setupForNonFipsEncryption(byte[] clientRandom, byte[] serverRandom, int keyLength)
	{
		if(keyLength != 40 && keyLength != 56 && keyLength != 128)
		{
			Logger.log(Logger.ERROR, "Received unsupported key length for key generation");
			return;
		}

		//PreMasterSecret = First192Bits(ClientRandom) + First192Bits(ServerRandom)
		byte[] preMasterSecret = new byte[48];
		System.arraycopy(clientRandom, 0, preMasterSecret, 0, 24);
		System.arraycopy(serverRandom, 0, preMasterSecret, 24, 24);

		//SaltedHash(S, I) = MD5(S + SHA(I + S + ClientRandom + ServerRandom))
		//PreMasterHash(I) = SaltedHash(PremasterSecret, I)
		//MasterSecret = PreMasterHash(0x41) + PreMasterHash(0x4242) + PreMasterHash(0x434343)
		byte[] masterSecret = saltedHash(clientRandom, serverRandom, preMasterSecret, masterSecretValues);
		
		//MasterHash(I) = SaltedHash(MasterSecret, I)
		//SessionKeyBlob = MasterHash(0x58) + MasterHash(0x5959) + MasterHash(0x5A5A5A)
		byte[] sessionKeyBlob = saltedHash(clientRandom, serverRandom, masterSecret, sessionKeyBlobValues);
		
		//MACKey128 = First128Bits(SessionKeyBlob)
		//FinalHash(K) = MD5(K + ClientRandom + ServerRandom)
		//InitialClientDecryptKey128 = FinalHash(Second128Bits(SessionKeyBlob))
		//InitialClientEncryptKey128 = FinalHash(Third128Bits(SessionKeyBlob))
		byte[] macKey = new byte[16];
		byte[] encryptKey = new byte[16];
		byte[] decryptKey = new byte[16];
		System.arraycopy(sessionKeyBlob, 0, macKey, 0, 16);
		MD5Digest md5 = new MD5Digest();
		md5.update(sessionKeyBlob, 16, 16);
		md5.update(clientRandom, 0, clientRandom.length);
		md5.update(serverRandom, 0, serverRandom.length);
		md5.doFinal(decryptKey, 0);
		md5.update(sessionKeyBlob, 32, 16);
		md5.update(clientRandom, 0, clientRandom.length);
		md5.update(serverRandom, 0, serverRandom.length);
		md5.doFinal(encryptKey, 0);
		
		if(keyLength == 40 || keyLength == 56)
		{
			//MACKey56 = 0xD1 + Last56Bits(First64Bits(MACKey128))
			//FinalHash(K) = MD5(K + ClientRandom + ServerRandom)
			//InitialClientEncryptKey56 = 0xD1 + Last56Bits(First64Bits(InitialClientEncryptKey128))
			//InitialClientDecryptKey56 = 0xD1 + Last56Bits(First64Bits(InitialClientDecryptKey128))
			macKey[0] = encryptKey[0] = decryptKey[0] = (byte) 0xd1;
			if(keyLength == 40)
			{
				//MACKey40 = 0xD1269E + Last40Bits(First64Bits(MACKey128))
				//FinalHash(K) = MD5(K + ClientRandom + ServerRandom)
				//InitialClientEncryptKey40 = 0xD1269E + Last40Bits(First64Bits(InitialClientEncryptKey128))
				//InitialClientDecryptKey40 = 0xD1269E + Last40Bits(First64Bits(InitialClientDecryptKey128))
				macKey[1] = encryptKey[1] = decryptKey[1] = 0x26;
				macKey[2] = encryptKey[2] = decryptKey[2] = (byte) 0x9e;
			}
			byte[] tempMac = new byte[8];
			byte[] tempEncrypt = new byte[8];
			byte[] tempDecrypt = new byte[8];
			System.arraycopy(macKey, 0, tempMac, 0, 8);
			System.arraycopy(encryptKey, 0, tempEncrypt, 0, 8);
			System.arraycopy(decryptKey, 0, tempDecrypt, 0, 8);
			encryption = new RC4Session(tempEncrypt, tempDecrypt, tempMac, encryptionMethod);
		}
		else if(keyLength == 128)
		{
			encryption = new RC4Session(encryptKey, decryptKey, macKey, encryptionMethod);
		}
	}
	
	private static byte[] saltedHash(byte[] clientRandom, byte[] serverRandom, byte[] salt, byte[][] in)
	{
		byte[] saltedHash = new byte[MD5_DIGEST_LENGTH * in.length];
		for(int i=0; i < in.length; i++)
		{
			SHA1Digest sha1 = new SHA1Digest();
			sha1.update(in[i], 0, in[i].length);
			sha1.update(salt, 0, salt.length);
			sha1.update(clientRandom, 0, clientRandom.length);
			sha1.update(serverRandom, 0, serverRandom.length);
			byte[] sha1Digest = new byte[SHA1_DIGEST_LENGTH];
			sha1.doFinal(sha1Digest, 0);
			
			MD5Digest md5 = new MD5Digest();
			md5.update(salt, 0, salt.length);
			md5.update(sha1Digest, 0, sha1Digest.length);
			byte[] md5Digest = new byte[MD5_DIGEST_LENGTH];
			md5.doFinal(md5Digest, 0);
			
			System.arraycopy(md5Digest, 0, saltedHash, i * MD5_DIGEST_LENGTH, MD5_DIGEST_LENGTH);
		}
		return saltedHash;
	}

	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240472(v=PROT.10).aspx>
	 * 2.2.1.10.1 Security Exchange PDU Data (TS_SECURITY_PACKET)</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240839(v=PROT.10).aspx>
	 * 4.1.9 Client Security Exchange PDU</a>
	 */
	private byte[] createClientSecurityExchangeRequest()
	{
		byte[] encryptedRandom = encryptClientRandom();
		byte[] data = new byte[encryptedRandom.length + 4];
		BitManip.setLittleEndian(data, 0, encryptedRandom.length);
		System.arraycopy(encryptedRandom, 0, data, 4, encryptedRandom.length);
		return data;
	}
	
	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240475(v=PROT.10).aspx>
	 * 2.2.1.11.1.1 Info Packet (TS_INFO_PACKET)</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240829(v=PROT.10).aspx>
	 * 4.1.10 Client Info PDU</a>
	 */
	private byte[] createClientInfoPDU()
	{
		//TODO: support for 4.0 RDP servers (unicode string lengths)
		//TODO: extended packet for RDP 5.0+ servers
		byte[] domain = new byte[0];
		byte[] username = new byte[0];
		byte[] password = new byte[0];
		try
		{
			domain = info.getDomain().getBytes("UTF-16LE");
			username = info.getUsername().getBytes("UTF-16LE");
			password = info.getPassword().getBytes("UTF-16LE");
		}
		catch(Exception e)
		{
			Logger.log(e, "UTF-16LE encoding not supported");
		}
		byte[] altShell = new byte[0];
		byte[] workingDir = new byte[0];
		short domainLen = (short) (domain.length);
		short usernameLen = (short) (username.length);
		short passwordLen = (short) (password.length);
		short altShellLen = (short) (altShell.length);
		short workingDirLen = (short) (workingDir.length);
		int length = 28 + domainLen + usernameLen + passwordLen + altShellLen + workingDirLen;
//		if(serverVersion == SERVER_VERSION_5_0_AND_HIGHER)
//			length += 192;
		byte[] data = new byte[length];
		int i=0;
		i += BitManip.setLittleEndian(data, i, 0);
		//TODO: find a better way to handle the flags here...
		i += BitManip.setLittleEndian(data, i, INFO_MOUSE | INFO_DIABLECTRLALTDEL |
				INFO_MAXIMIZE_SHELL | INFO_UNICODE | INFO_LOGON_NOTIFY/* | INFO_COMPRESSION_VALID | 
				INFO_COMPRESSION_TYPE_64K*/);
		i += BitManip.setLittleEndian(data, i, domainLen);
		i += BitManip.setLittleEndian(data, i, usernameLen);
		i += BitManip.setLittleEndian(data, i, passwordLen);
		i += BitManip.setLittleEndian(data, i, altShellLen);
		i += BitManip.setLittleEndian(data, i, workingDirLen);
		System.arraycopy(domain, 0, data, i, domain.length);
		i += domain.length;
		i += BitManip.setLittleEndian(data, i, (short) 0);
		System.arraycopy(username, 0, data, i, username.length);
		i += username.length;
		i += BitManip.setLittleEndian(data, i, (short) 0);
		System.arraycopy(password, 0, data, i, password.length);
		i += password.length;
		i += BitManip.setLittleEndian(data, i, (short) 0);
		System.arraycopy(altShell, 0, data, i, altShell.length);
		i += altShell.length;
		i += BitManip.setLittleEndian(data, i, (short) 0);
		System.arraycopy(workingDir, 0, data, i, workingDir.length);
		i += workingDir.length;
		i += BitManip.setLittleEndian(data, i, (short) 0);
		
//		if(serverVersion == SERVER_VERSION_5_0_AND_HIGHER)
//		{
//			String clientAddress = "";
//			String clientDir = "";
//			byte[] standard = { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x05, 0x00,
//								0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
//			byte[] daylight = { 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01, 0x00,
//								0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
//			byte[] empty = new byte[64];
//			i += BitManip.setLittleEndian(data, i, (short) 0x0002);
//			i += BitManip.setLittleEndian(data, i, (short) 1);
//			i += BitManip.setUnicode(data, i, clientAddress);
//			i += BitManip.setLittleEndian(data, i, (short) 1);
//			i += BitManip.setUnicode(data, i, clientDir);
//			i += BitManip.setLittleEndian(data, i, info.getTimeZoneBias());
//			System.arraycopy(empty, 0, data, i, 64);
//			i += 64;
//			System.arraycopy(standard, 0, data, i, standard.length);
//			i += standard.length;
//			i += BitManip.setLittleEndian(data, i, 0);
//			System.arraycopy(empty, 0, data, i, 64);
//			i += 64;
//			System.arraycopy(daylight, 0, data, i, daylight.length);
//			i += daylight.length;
//			i += BitManip.setLittleEndian(data, i, 0xffffffc4);
//			i += BitManip.setLittleEndian(data, i, 0);
//			i += BitManip.setLittleEndian(data, i, info.getPerformanceFlags());
//			i += BitManip.setLittleEndian(data, i, (short) 0);
//		}
		return data;
	}

	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc746161(v=PROT.10).aspx>
	 * 2.2.1.12.1 Valid Client License Data (LICENSE_VALID_CLIENT_DATA)</a>
	 * @see Example: <a href=http://msdn.microsoft.com/en-us/library/cc240916(v=PROT.10).aspx>
	 * 4.1.11 Server License Error PDU - Valid Client</a>
	 */
	private boolean readValidClientLicenseData(InputByteStream packet)
	{
		if((packet.getByte() & 0xff) != (byte) 0xff)
		{
			Logger.log(Logger.ERROR, "invalid message type in valid client license data pdu");
			return false;
		}
		//skip version
		packet.skip(1);
		short length = packet.getShortLittleEndian();
		if(packet.getIntLittleEndian() != 0x00000007)
		{
			Logger.log(Logger.ERROR, "didn't receive status_valid_client in client license data");
			return false;
		}
		if(packet.getIntLittleEndian() != 0x00000002)
		{
			Logger.log(Logger.ERROR, "didn't receive st_no_transition in client license data");
			return false;
		}
		if(packet.getShortLittleEndian() != 0x0004)
		{
			Logger.log(Logger.ERROR, "didn't receive bb_error_blob in client license data");
			return false;
		}
		length = packet.getShortLittleEndian();
		if(length != 0x0000)
		{
			Logger.log(Logger.ERROR, "unexpected blob length of " + length + " received, expected 0");
			return false;
		}
		return true;
	}
}
