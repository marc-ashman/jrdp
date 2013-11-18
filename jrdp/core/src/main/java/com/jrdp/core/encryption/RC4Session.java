package com.jrdp.core.encryption;

import com.jrdp.core.encryption.RC4.KeyUpdateRequestListener;
import com.jrdp.core.remote.rdp.Constants;
import com.jrdp.core.remote.rdp.Rdp;
import com.jrdp.core.util.BitManip;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.RC4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

public class RC4Session implements EncryptionSession, KeyUpdateRequestListener
{
	static final int MAX_RC4_USES = 4096;
	
	private byte[] originalEncryptKey;
	private byte[] originalDecryptKey;
	private byte[] encryptionKey;
	private byte[] decryptionKey;
	private byte[] macKey;
	private int encryptionMethod;
	
	protected RC4 encryptor;
	protected RC4 decryptor;
	
	public RC4Session(byte[] encryptionKey, byte[] decryptionKey, byte[] macKey, int encryptionMethod)
	{
		this.encryptionKey = originalEncryptKey = encryptionKey;
		this.decryptionKey = originalDecryptKey = decryptionKey;
		this.encryptionMethod = encryptionMethod;
		this.macKey = macKey;

		encryptor = new RC4(encryptionKey, MAX_RC4_USES, this);
		decryptor = new RC4(decryptionKey, MAX_RC4_USES, this);
	}

	@Override
	public byte[] decrypt(byte[] data, int offset, int length) {
		byte[] decrypted = decryptor.decrypt(data, offset, length);
		decryptor.warnOfUse();
		return decrypted;
	}

	@Override
	public byte[] encrypt(byte[] data, int offset, int length) {
		byte[] encrypted = encryptor.encrypt(data, offset, length);
		encryptor.warnOfUse();
		return encrypted;
	}

	/**
	 * Generates a Salted MAC for the current packet
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240789(v=PROT.10).aspx>
	 * 5.3.6.1.1 Salted MAC Generation</a>
	 */
	@Override
	public byte[] createMac(byte[] data) {
		SHA1Digest sha1 = new SHA1Digest();
		sha1.update(macKey, 0, macKey.length);
		sha1.update(Constants.pad36, 0, Constants.pad36.length);
		byte[] length = new byte[4];
		BitManip.setLittleEndian(length, 0, data.length);
		sha1.update(length, 0, length.length);
		sha1.update(data, 0, data.length);
		byte[] sha1Digest = new byte[Constants.SHA1_DIGEST_LENGTH];
		sha1.doFinal(sha1Digest, 0);
		
		MD5Digest md5 = new MD5Digest();
		md5.update(macKey, 0, macKey.length);
		md5.update(Constants.pad5c, 0, Constants.pad5c.length);
		md5.update(sha1Digest, 0, sha1Digest.length);
		byte[] md5Digest = new byte[Constants.MD5_DIGEST_LENGTH];
		md5.doFinal(md5Digest, 0);
		
		byte[] mac = new byte[8];
		System.arraycopy(md5Digest, 0, mac, 0, 8);
		
		return mac;
	}
	
	public byte[] getMacKey()
	{
		return macKey;
	}


	/**
	 * @see <a href=http://msdn.microsoft.com/en-us/library/cc240792(v=PROT.10).aspx>
	 * 5.3.7.1 Non-FIPS</a>
	 */
	@Override
	public void onKeyUpdateRequest(RC4 source) {
		byte[] originalKey = source == encryptor ? originalEncryptKey : originalDecryptKey;
		byte[] currentKey = source == decryptor ? encryptionKey : decryptionKey;
		((RC4)source).updateKey(updateRc4Key(originalKey, currentKey));
	}
	
	private byte[] updateRc4Key(byte[] originalKey, byte[] currentKey)
	{
		int keySize = 0;
		switch(encryptionMethod)
		{
			case Constants.ENCRYPTION_128BIT:
				keySize = 16;
				break;
			case Constants.ENCRYPTION_40BIT:
			case Constants.ENCRYPTION_56BIT:
				keySize = 8;
				break;
			case Constants.ENCRYPTION_NONE:
			case Constants.ENCRYPTION_FIPS:
				//Should never happen...
				return null;
		}
		
		SHA1Digest sha1 = new SHA1Digest();
		sha1.update(originalKey, 0, keySize);
		sha1.update(Constants.pad36, 0, Constants.pad36.length);
		sha1.update(currentKey, 0, currentKey.length);
		byte[] shaComponent = new byte[Rdp.SHA1_DIGEST_LENGTH];
		sha1.doFinal(shaComponent, 0);
		
		//StringManip.print(shaComponent, "SHA1:");
		
		MD5Digest md5 = new MD5Digest();
		md5.update(originalKey, 0, keySize);
		md5.update(Constants.pad5c, 0, Constants.pad5c.length);
		md5.update(shaComponent, 0, shaComponent.length);
		byte[] tempKey = new byte[Rdp.MD5_DIGEST_LENGTH];
		md5.doFinal(tempKey, 0);

		//StringManip.print(tempKey, "MD5:");
		
		RC4Engine rc4 = new RC4Engine();
		if(keySize == 16)
		{
			byte[] newKey = new byte[tempKey.length];
			rc4.init(true, new KeyParameter(tempKey));
			rc4.processBytes(tempKey, 0, tempKey.length, newKey, 0);
			return newKey;
		}
		else
		{
			byte[] newKey = new byte[8];
			byte[] smallerTmpKey = new byte[8];
			System.arraycopy(tempKey, 0, smallerTmpKey, 0, 8);
			rc4.init(true, new KeyParameter(smallerTmpKey));
			rc4.processBytes(smallerTmpKey, 0, 8, newKey, 0);
			newKey[0] = (byte) 0xd1;
			if(encryptionMethod == Constants.ENCRYPTION_40BIT)
			{
				newKey[1] = 0x26;
				newKey[2] = (byte) 0x9e;
			}
			return newKey;
		}
	}
}
