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

package com.jrdp.core.util;

public class BitManip
{
	/**
	 * merges the bytes to an int. the first argument is set at the least
	 * significant position, second is next, and so on
	 */
	public final static int mergeToIntLeastSigFirst(byte a)
	{
		return a & 0xFF;
	}
	
	/**
	 * merges the bytes to an int. the first argument is set at the least
	 * significant position, second is next, and so on
	 */
	public final static int mergeToIntLeastSigFirst(byte a, byte b)
	{
		return a & 0xFF | ((b & 0xFF) << 8);
	}
	
	/**
	 * merges the bytes to an int. the first argument is set at the least
	 * significant position, second is next, and so on
	 */
	public final static int mergeToIntLeastSigFirst(byte a, byte b, byte c)
	{
		return (a & 0xFF) | ((b & 0xFF) << 8) | ((c & 0xFF) << 16);
	}
	
	/**
	 * merges the bytes to an int. the first argument is set at the least
	 * significant position, second is next, and so on
	 */
	public final static int mergeToIntLeastSigFirst(byte a, byte b, byte c, byte d)
	{
		return (a & 0xFF) | ((b & 0xFF) << 8) | ((c & 0xFF) << 16) | ((d & 0xFF) << 24);
	}
	
	public final static int mergeToInt(byte a, byte b, byte c, byte d)
	{
		return ((a & 0xff) << 24) | ((b & 0xff) << 16) | 
				((c & 0xff) << 8) | (d & 0xff);
	}
	
	public final static int mergeToInt(short a, short b)
	{
		return ((a & 0xffff) << 24) | ((b & 0xffff) << 8);
	}
	
	public final static short mergeToShort(byte a, byte b)
	{
		return (short) (((a & 0xff) << 8) | (b & 0xff));
	}
	
	public final static short mergeToShortLittleEngian(byte a, byte b)
	{
		return (short) ((a & 0xff) | (b & 0xff) << 8);
	}
	
	public final static byte splitToByte(int octet, int val)
	{
		switch(octet)
		{
			case 1:
				return (byte) (val >>> 24);
			case 2:
				return (byte) (val >>> 16);
			case 3:
				return (byte) (val >>> 8);
			case 4:
				return (byte) val;
		}
		throw new IllegalArgumentException("Invalid octet");
	}
		
	public final static byte splitToByte(int octet, short val)
	{
		switch(octet)
		{
			case 1:
				return (byte) (val >>> 8);
			case 2:
				return (byte) val;
		}
		throw new IllegalArgumentException("Invalid octet");
	}
	
	public final static byte[] toByteArray(short val)
	{
		return new byte[] { (byte) ((val & 0xff00) >>> 8), (byte) (val & 0x00ff) };
	}
	
	public final static byte[] toByteArray(int val)
	{
		return new byte[] { (byte) ((val & 0xff000000) >>> 24), (byte) ((val & 0x00ff0000) >>> 16),
				(byte) ((val & 0x0000ff00) >>> 8), (byte) (val & 0x000000ff) };
	}
	
	public final static int set(byte[] array, int index, int[] val)
	{
		if(array.length < index + (val.length * 4))
			throw new IllegalArgumentException("Array to large to set");
		int n = index;
		for(int i=0; i < val.length; i++)
		{
			array[n++] = first(val[i]);
			array[n++] = second(val[i]);
			array[n++] = third(val[i]);
			array[n++] = fourth(val[i]);
		}
		return n - index;
	}
	
	public final static int set(byte[] array, int index, short[] val)
	{
		if(array.length < index + (val.length * 2))
			throw new IllegalArgumentException("Array to large to set");
		int n = index;
		for(int i=0; i < val.length; i++)
		{
			array[n++] = first(val[i]);
			array[n++] = second(val[i]);
		}
		return n - index;
	}
	
	public final static int set(byte[] array, int index, int val)
	{
		array[index] = first(val);
		array[index + 1] = second(val);
		array[index + 2] = third(val);
		array[index + 3] = fourth(val);
		return 4;
	}
	
	public final static int set(byte[] array, int index, short val)
	{
		array[index] = first(val);
		array[index + 1] = second(val);
		return 2;
	}

	public final static int setAsLittleEndianInt(byte[] array, int index, byte[] val)
	{
		if(val.length % 4 != 0)
			throw new IllegalArgumentException("val array size must be multiple of 4");
		int arraySize = val.length / 4;
		for(int i=0; i < arraySize; i++)
		{
			int n = i * 4;
			array[index + n] = val[n + 3];
			array[index + n + 1] = val[n + 2];
			array[index + n + 2] = val[n + 1];
			array[index + n + 3] = val[n];
		}
		return arraySize * 4;
	}
	
	public final static int setLittleEndian(byte[] array, int index, int[] val)
	{
		if(array.length < index + (val.length * 4))
			throw new IllegalArgumentException("Array to large to set");
		int n = index;
		for(int i=0; i < val.length; i++)
		{
			array[n++] = fourth(val[i]);
			array[n++] = third(val[i]);
			array[n++] = second(val[i]);
			array[n++] = first(val[i]);
		}
		return n - index;
	}
	
	public final static int setLittleEndian(byte[] array, int index, short[] val)
	{
		if(array.length < index + (val.length * 2))
			throw new IllegalArgumentException("Array to large to set");
		int n = index;
		for(int i=0; i < val.length; i++)
		{
			array[n++] = second(val[i]);
			array[n++] = first(val[i]);
		}
		return n - index;
	}
	
	public final static int setLittleEndian(byte[] array, int index, char val)
	{
		array[index] = second(val);
		array[index + 1] = first(val);
		return 2;
	}
	
	public final static int setLittleEndian(byte[] array, int index, int val)
	{
		array[index] = fourth(val);
		array[index + 1] = third(val);
		array[index + 2] = second(val);
		array[index + 3] = first(val);
		return 4;
	}
	
	public final static int setLittleEndian(byte[] array, int index, short val)
	{
		array[index] = second(val);
		array[index + 1] = first(val);
		return 2;
	}
	
	public final static int setLittleEndian(byte[] array, int index, byte val)
	{
		array[index] = val;
		return 1;
	}
	
	public final static int setUnicode16(byte[] array, int index, String str, int length, boolean lastIsNul)
	{
		int i = 0;
		
		for(i = 0; i < length && i < str.length(); i++)
		{
			char c = str.charAt(i);
			array[index + (i * 2)] = second(c);
			array[index + (i * 2) + 1] = first(c);
		}
		while(i < length)
		{
			array[index + (i * 2)] = 0;
			array[index + (i * 2) + 1] = 0;
			i++;
		}
		if(lastIsNul)
		{
			array[index + (length * 2) - 1] = 0;
			array[index + (length * 2) - 2] = 0;
		}
		return length * 2;
	}
	
	public final static byte first(char val)
	{
		return (byte) ((val & 0xff00) >>> 8);
	}
	
	public final static byte second(char val)
	{
		return (byte) (val & 0x00ff);
	}
	
	public final static byte first(short val)
	{
		return (byte) ((val & 0xff00) >>> 8);
	}
	
	public final static byte second(short val)
	{
		return (byte) (val & 0x00ff);
	}
	
	public final static byte first(int val)
	{
		return (byte) ((val & 0xff000000) >>> 24);
	}
	
	public final static byte second(int val)
	{
		return (byte) ((val & 0x00ff0000) >>> 16);
	}
	
	public final static byte third(int val)
	{
		return (byte) ((val & 0x0000ff00) >>> 8);
	}
	
	public final static byte fourth(int val)
	{
		return (byte) (val & 0x000000ff);
	}
	
	public final static byte[] getByteArrayLittleEndian(byte[] data, int n, int size)
	{
		return getByteArray(data, n, size);
	}
	
	public final static byte[] getByteArray(byte[] data, int n, int size)
	{
		int pos = n;
		byte[] array = new byte[size];
		for(int i=0; i < size; i++)
		{
			array[i] = data[pos++];
		}
		return array;
	}
	
	public final static int getInt(byte[] data, int n)
	{
		return (data[n++] & 0xFF) << 24 | (data[n++] & 0xFF) << 16 |
				(data[n++] & 0xFF) << 8 | (data[n] & 0xFF);
	}
	
	public final static int getIntLittleEndian(byte[] data, int n)
	{
		return (data[n++] & 0xFF) | (data[n++] & 0xFF) << 8 |
				(data[n++] & 0xFF) << 16 | (data[n] & 0xFF) << 24;
	}
	
	public final static int[] getIntArray(byte[] data, int n, int size)
	{
		int[] array = new int[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getInt(data, n);
			n += 4;
		}
		return array;
	}
	
	public final static int[] getIntArrayLittleEndian(byte[] data, int n, int size)
	{
		int[] array = new int[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getIntLittleEndian(data, n);
			n += 4;
		}
		return array;
	}
	
	public final static short getShort(byte[] data, int n)
	{
		return (short) ((data[n++] & 0xFF) << 8 | (data[n] & 0xFF));
	}
	
	public final static short getShortLittleEndian(byte[] data, int n)
	{
		return (short) ((data[n++] & 0xFF) | (data[n] & 0xFF) << 8);
	}
	
	public final static short[] getShortArray(byte[] data, int n, int size)
	{
		short[] array = new short[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getShort(data, n);
			n += 2;
		}
		return array;
	}
	
	public final static short[] getShortArrayLittleEndian(byte[] data, int n, int size)
	{
		short[] array = new short[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getShortLittleEndian(data, n);
			n += 2;
		}
		return array;
	}
	
	public final static byte[] reverse(byte[] array)
	{
		byte[] data = new byte[array.length];
		int i, j;
		byte temp;
		for(i = 0, j = array.length - 1; i < j; i++, j--)
		{
			temp = array[i];
			data[i] = array[j];
			data[j] = temp;
		}
		return array;
	}
}
