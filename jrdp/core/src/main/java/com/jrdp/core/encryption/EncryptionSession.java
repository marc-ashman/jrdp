package com.jrdp.core.encryption;

public interface EncryptionSession
{	
	public abstract byte[] encrypt(byte[] data, int offset, int length);
	public abstract byte[] decrypt(byte[] data, int offset, int length);
	public abstract byte[] createMac(byte[] data);
}
