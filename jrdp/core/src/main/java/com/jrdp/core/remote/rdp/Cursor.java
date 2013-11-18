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