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

import com.jrdp.core.encryption.EncryptionSession;

public class InputByteStream
{
	private int pos = 0;
	private byte[] data;
	
	public InputByteStream(byte[] stream)
	{
		data = stream;
	}
	
	public int getPos()
	{
		return pos;
	}
	
	public void skip(int positions)
	{
		pos += positions;
	}
	
	public int available()
	{
		return data.length - pos;
	}
	
	public int getByte()
	{
		return data[pos++];
	}
	
	public byte[] getByteArrayLittleEndian(int size)
	{
		return getByteArray(size);
	}
	
	public byte[] getByteArray(int size)
	{
		byte[] array = new byte[size];
		for(int i=0; i < size; i++)
		{
			array[i] = data[pos++];
		}
		return array;
	}
	
	public int getInt()
	{
		return (data[pos++] & 0xFF) << 24 | (data[pos++] & 0xFF) << 16 |
				(data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF);
	}
	
	public int getIntLittleEndian()
	{
		return (data[pos++] & 0xFF) | (data[pos++] & 0xFF) << 8 |
				(data[pos++] & 0xFF) << 16 | (data[pos++] & 0xFF) << 24;
	}
	
	public int[] getIntArray(int size)
	{
		int[] array = new int[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getInt();
		}
		return array;
	}
	
	public int[] getIntArrayLittleEndian(int size)
	{
		int[] array = new int[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getIntLittleEndian();
		}
		return array;
	}
	
	public short getShort()
	{
		return (short) ((data[pos++] & 0xFF) << 8 | (data[pos++] & 0xFF));
	}
	
	public short getShortLittleEndian()
	{
		return (short) ((data[pos++] & 0xFF) | (data[pos++] & 0xFF) << 8);
	}
	
	public short[] getShortArray(int size)
	{
		short[] array = new short[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getShort();
		}
		return array;
	}
	
	public short[] getShortArrayLittleEndian(int size)
	{
		short[] array = new short[size];
		for(int i=0; i < size; i++)
		{
			array[i] = getShortLittleEndian();
		}
		return array;
	}
	
	public int left()
	{
		return data.length - pos;
	}
	
	public void decryptStream(EncryptionSession encryption)
	{
		decryptStream(encryption, data.length - left());
	}
	
	public void decryptStream(EncryptionSession encryption, int pos)
	{
		if(encryption == null)
			return;
		int len = data.length - pos;
		encryption.decrypt(data, pos, len);
	}
	
	public byte getAt(int index)
	{
		return data[index];
	}
	
	public int peek()
	{
		return data[pos] & 0xff;
	}
}
