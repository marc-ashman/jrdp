package com.jrdp.core.remote.rdp;

public class Cursor
{
	private int [] img;
	private short width;
	private short height;
	private short hotspotX;
	private short hotspotY;
	
	public Cursor(int[] img, short width, short height, short hotspotX, short hotspotY)
	{
		this.img = img;
		this.width = width;
		this.height = height;
		this.hotspotX = hotspotX;
		this.hotspotY = hotspotY;
	}
	
	public int[] getCursorBitmap()
	{
		return img;
	}
	
	public short getWidth()
	{
		return width;
	}
	
	public short getHeight()
	{
		return height;
	}
	
	public short getHotspotX()
	{
		return hotspotX;
	}
	
	public short getHotspotY()
	{
		return hotspotY;
	}
}