package com.jrdp.core.util;

public class StringManip
{
	public static short[] toUnicode(String str)
	{
		return toUnicode(str, str.length());
	}
	
	public static short[] toUnicode(String str, int size)
	{
		short[] unicode = new short[size];
		for(int i=0; i < size; i++)
		{
			if(i >= str.length())
				unicode[i] = 0;
			else
				unicode[i] = (short) str.charAt(i);
		}
		return unicode;
	}

	public static void print(byte[] data, String str)
	{
		Logger.log(Logger.DEBUG, str + " size(" + data.length + ")");
		print(data);
	}
	
	public static void print(byte[] data)
	{
		int i;
		for(i=0; i < data.length; i++)
		{
			if(data[i] > 31 && data[i] < 127)
				System.out.print((char) data[i]);
			else
				System.out.print('.');
			if((i+1) % 16 == 0)
			{
				System.out.print("\t\t");
				String result = "";
				int n;
				for (n=0; n < 16; n++)
				{
					int j = i+1 - 16 + n;
					if(j < 0 || j >= data.length)
						continue;
					result += Integer.toString( ( data[j] & 0xff ) + 0x100, 16).substring( 1 ).toUpperCase() + " ";
				}
				System.out.print(result + "\n");
			}
		}
		if((i % 16) == 0)
			return;
		System.out.print("\t\t");
		String result = "";
		for (int n=0; n < 16; n++)
		{
			int j = i+1 - 16 + n;
			if(j < 0 || j >= data.length)
				continue;
			result += Integer.toString( ( data[j] & 0xff ) + 0x100, 16).substring( 1 ) + " ";
		}
		System.out.print(result + "\n");
	}
}
