package com.jrdp.core.remote.rdp.orders;

class Polyline
{
	public int x;
	public int y;
	public int flags;
	public int fgColor;
	public int lines;
	public int op;
	public int size;
	public byte[] data;
	
	public Polyline()
	{
		data = new byte[256];
	}
	
	public void reset()
	{
		x = y = flags = lines = op = 0;
		data = new byte[256];
	}
}
