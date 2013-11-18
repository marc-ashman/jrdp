package com.jrdp.core.util;

public class ArrayManip
{
	public final static byte[] merge(byte[][] array)
	{
		int size = 0;
		for(int i=0; i < array.length; i++)
		{
			size += array[i].length;
		}
		byte[] merged = new byte[size];
		int j = 0;
		for(int i=0; i < array.length; i++)
		{
			for(int n=0; n < array[i].length; n++)
			{
				merged[j++] = array[i][n];
			}
		}
		return merged;
	}
}
