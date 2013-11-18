package com.jrdp.core.encryption;


import com.jrdp.core.util.BitManip;
import com.jrdp.core.util.Logger;

import java.math.BigInteger;

public class RSA
{
	private int modulusLen;
	private int modulusBitLen;
	private int maxBytesLen;
	private byte[] exponent;
	private byte[] modulus;
	private byte[] signature;
	
	public RSA(int modulusLen, int modulusBitLen, int maxBytesLen, byte[] exponent, byte[] modulus, byte[] signature)
	{
		this.modulusLen = modulusLen;
		this.modulusBitLen = modulusBitLen;
		this.maxBytesLen = maxBytesLen;
		this.exponent = exponent;
		this.modulus = modulus;
		this.signature = signature;
	}

    public byte[] encrypt(byte[] dataBytes) {
        return encrypt(modulus.clone(), exponent.clone(), dataBytes);
    }

    public static byte[] encrypt(byte[] modulusBytes, byte[] exponentBytes, byte[] dataBytes)
    {
        ReverseByteArray(modulusBytes);
        ReverseByteArray(exponentBytes);
        ReverseByteArray(dataBytes);

        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger( 1, exponentBytes);
        BigInteger data = new BigInteger(1, dataBytes);

        BigInteger cipherText = data.modPow(exponent, modulus);

        byte[] cipherTextBytes = cipherText.toByteArray();
        ReverseByteArray(cipherTextBytes);

        ReverseByteArray(modulusBytes);
        ReverseByteArray(exponentBytes);
        ReverseByteArray(dataBytes);

        return cipherTextBytes;
    }

    public static void ReverseByteArray(byte[] array)
    {
        int i, j;
        byte temp;

        for (i = 0, j = array.length - 1; i < j; i++, j--) {
            temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
	
	public int getModulusLen() {
		return modulusLen;
	}

	public int getModulusBitLen() {
		return modulusBitLen;
	}

	public int getMacBytesLen() {
		return maxBytesLen;
	}

	public byte[] getExponent() {
		return exponent;
	}

	public byte[] getModulus() {
		return modulus;
	}

	public byte[] getSignature() {
		return signature;
	}

	public static RSA createKeyFromCertificate(byte[] data)
	{
		if(BitManip.getIntLittleEndian(data, 0) != 0x00000001)
		{
			Logger.log(Logger.ERROR, "Received unexpected certificate version");
			return null;
		}
		if(BitManip.getIntLittleEndian(data, 4) != 0x00000001)
		{
			Logger.log(Logger.ERROR, "Received unexpected signature algorithm identifier");
			return null;
		}
		if(BitManip.getIntLittleEndian(data, 8) != 0x00000001)
		{
			Logger.log(Logger.ERROR, "Received unexpected key algorithm identifier");
			return null;
		}
		if(BitManip.getShortLittleEndian(data, 12) != (short) 0x0006)
		{
			Logger.log(Logger.ERROR, "Received unexpected key blob field type");
			return null;
		}
		short keyLength = BitManip.getShortLittleEndian(data, 14);
		if(BitManip.getIntLittleEndian(data, 16) != 0x31415352)
		{
			Logger.log(Logger.ERROR, "Received unexpected magic value (RSA1 in ANSI)");
			return null;
		}
		int modulusLen = BitManip.getIntLittleEndian(data, 20);
		int modulusBitLen = BitManip.getIntLittleEndian(data, 24);
		int maxBytesLen = BitManip.getIntLittleEndian(data, 28);
		
		/*********************TESTING*********************/
		modulusLen -= 8;
		
		byte[] exponent = BitManip.getByteArray(data, 32, 4);//(data, 32, exponent, 0, 4);
		byte[] modulus = BitManip.getByteArray(data, 36, modulusLen);
		/*********************TESTING*********************/
		modulusLen += 8;
		if(BitManip.getShortLittleEndian(data, 36 + modulusLen) != (short) 0x0008)
		{
			Logger.log(Logger.ERROR, "Received unexpected signature blob field type");
			return null;
		}
		short signatureLength = BitManip.getShortLittleEndian(data, 38 + modulusLen);
		byte[] signature = BitManip.getByteArray(data, 40 + modulusLen, signatureLength);
		return new RSA(modulusLen, modulusBitLen, maxBytesLen, exponent, modulus, signature);
	}
}
