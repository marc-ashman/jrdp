package com.jrdp.core.remote;

import com.jrdp.core.remote.rdp.Cursor;


public interface Canvas
{
	public void setCanvasBottomUp(int[] img, int width, int height, int x, 
			int y, int clippingWidth, int clippingHeight);
	public void setCanvas(int[] img, int width, int height, int x, int y, 
			int clippingWidth, int clippingHeight);
	public void cursorPositionChanged(int x, int y);
	public void setCursor(Cursor cursor);
	public void hideCursor();
	public void showCursor();
	public int getCursorX();
	public int getCursorY();
}

//Old, but functional... why delete them - may be useful some day...
//-----------------------------------------------------------------------------
//public void setCanvasBottomUp(int[] img, int width, int height, int x, 
//		int y, int clippingWidth, int clippingHeight)
//{
//	int clippedWidth = width - (width - clippingWidth);
//	int clippedHeight = height - (height - clippingHeight);
//	int bmpIndex = pixelIndex(x, y + clippingHeight - 1);
//	int updateIndex = 0;
//	for(int i=0; i < clippedHeight; i++)
//	{
//		System.arraycopy(img, updateIndex, bmp, bmpIndex, clippedWidth);
//		updateIndex += width;
//		bmpIndex -= this.width;
//	}
//}
//-----------------------------------------------------------------------------
//public void setCanvas(int[] img, int width, int height, int x, int y, 
//		int clippingWidth, int clippingHeight)
//{
//	//System.out.println("x: " + x + " y:" + y + " width:" + width + " height:" + height + 
//	//		" clipwidth: " + clippingWidth + " clipheight: " + clippingHeight);
//	int clippedWidth = width - (width - clippingWidth);
//	int clippedHeight = height - (height - clippingHeight);
//	int bmpIndex = pixelIndex(x, y);
//	int updateIndex = 0;
//	for(int i=0; i < clippedHeight; i++)
//	{
//		System.arraycopy(img, updateIndex, bmp, bmpIndex, clippedWidth);
//		updateIndex += width;
//		bmpIndex += this.width;
//	}
//}
