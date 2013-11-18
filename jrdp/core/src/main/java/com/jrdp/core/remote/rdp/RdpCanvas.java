package com.jrdp.core.remote.rdp;

/*import fusion.remote.Canvas;
import fusion.remote.rdp.orders.Bounds;
import fusion.remote.rdp.orders.Line;
import fusion.remote.rdp.orders.Polyline;
import fusion.remote.rdp.orders.Rectangle;

public class RdpCanvas extends Canvas
{
	private int SET_OP = 0x0c;
	
	public RdpCanvas(int width, int height) {
		super(width, height);
	}
	
	private void setPixel(int x, int y, int color, int op)
	{
		int mask = 0x00ffffff;
		int index = pixelIndex(x, y);
		int pixel = bmp[index];
		
		switch(op)
		{
		case 0x00:
			bmp[index] = 0;
			break;
		case 0x01:
			bmp[index] = (~(pixel | color) & mask);
			break;
		case 0x02:
			bmp[index] = (pixel & ((~color) & mask));
			break;
		case 0x03:
			bmp[index] = (~color) & mask;
			break;
		case 0x04:
			bmp[index] = (~pixel & color) * mask;
			break;
		case 0x05:
			bmp[index] = (~pixel) & mask;
			break;
		case 0x06:
			bmp[index] = pixel ^ (color & mask);
			break;
		case 0x07:
			bmp[index] = (~pixel & color) & mask;
			break;
		case 0x08:
			bmp[index] = pixel & (color & mask);
			break;
		case 0x09:
			bmp[index] = pixel ^ (~color & mask);
			break;
		case 0x0b:
			bmp[index] = pixel | (~color & mask);
			break;
		case 0x0c:
			bmp[index] = color;
			break;
		case 0x0d:
			bmp[index] = (~pixel | color) & mask;
			break;
		case 0x0e:
			bmp[index] = pixel | (color & mask);
			break;
		case 0x0f:
			bmp[index] = mask;
			break;
		}
		bmp[index] |= 0xff000000;
	}
	
	public void drawRect(Rectangle rect, Bounds bounds)
	{
		int x = rect.x;
		int y = rect.y;
		int width = rect.width;
		int height = rect.height;
		int color = ((rect.color & 0xff) << 16) | (rect.color & 0xff00) | ((rect.color & 0xff0000) >> 16);
		if(rect.x < bounds.left);
		{
			width -= (bounds.left - rect.x);
			x = bounds.left;
		}
		if((rect.width + rect.x) > bounds.right)
		{
			width -= ((rect.width) + rect.x) - bounds.right;
		}
		if(rect.y < bounds.bottom)
		{
			height -= (bounds.bottom - rect.y);
			y = bounds.bottom;
		}
		if((rect.height + rect.y) > bounds.top)
		{
			height -= (rect.height + rect.y) - bounds.top;
		}
		for(int i=y; i < y + height; i++)
		{
			for(int n=x; n < x + width; n++)
			{
				setPixel(n, i, color, SET_OP);
			}
		}
	}
	
	public void drawPolyline(Polyline polyline, Bounds bounds)
	{
		int[] data = new int[1];
		data[0] = ((polyline.lines - 1) / 4) + 1;
		int flags = 0;
		int index = 0;
		int op = polyline.op - 1;
		
		for(int line = 0; line < polyline.lines && data[0] < polyline.size; line++)
		{
			int lastX = polyline.x;
			int lastY = polyline.y;
			
			if(line % 4 == 0)
				flags = polyline.data[index++];
			if((flags & 0xc0) == 0)
				flags |= 0xc0;
			if((flags & 0x40) != 0)
				polyline.x += getDelta(polyline.data, data);
			if((flags & 0x80) != 0)
				polyline.y += getDelta(polyline.data, data);
			
			drawLine(lastX, lastY, polyline.x, polyline.y, polyline.fgColor, polyline.op, bounds);
		}
	}
	
	private static int getDelta(byte[] data, int[] offset)
	{
		int val = (data[0] + 1) & 0xff;
		int two = val & 0x80;
		
		if((val & 0x40) != 0)
			val |= ~0x3f;
		else
			val &= 0x3f;
		if(two != 0)
			val = (val << 8) | (data[(offset[0] + 1)] & 0xff);
		return val;
	}

	public void drawLine(Line line, Bounds bounds)
	{
		drawLine(line.startX, line.startY, line.endX, line.endY, line.bgColor, line.op, bounds);
	}
	
	public void drawLine(int startX, int startY, int endX, int endY, int bgColor, int op, Bounds bounds)
	{
		if(startX == endX)
		{
			if(startX > bounds.right || startX < bounds.left)
				return;
			boolean add = startY < endY;
			for(int i=startY; i <= endY; i++)
			{
				if(i < bounds.top && i > bounds.bottom)
				{
					setPixel(startX, i, bgColor, op);
				}
				if(add)
					i++;
				else
					i--;
			}
			return;
		}
		else if(startY == endY)
		{
			if(startY > bounds.top || startY < bounds.bottom)
				return;
			boolean add = startX < endX;
			for(int i=startX; i <= endX;)
			{
				if(i >= bounds.left && i <= bounds.right)
				{
					setPixel(startY, i, bgColor, op);
				}
				if(add)
					i++;
				else
					i--;
			}
			return;
		}

		int deltaX = endX - startX;
		if(deltaX < 0)
			deltaX = -deltaX;
		int deltaY = endY - startY;
		if(deltaY < 0)
			deltaY = -deltaY;
		
		int incrementX1, incrementX2, incrementY1, incrementY2;
		if(endX >= startX)
			incrementX1 = incrementX2 = 1;
		else
			incrementX1 = incrementX2 = -1;
		if(endY >= startY)
			incrementY1 = incrementY2 = 1;
		else
			incrementY1 = incrementY2 = -1;
		
		int denominator, numerator, pixelsToAdd, numberOfPixels;
		
		if(deltaX >= deltaY)
		{
			incrementX1 = incrementY2 = 0;
			denominator = deltaX;
			numerator = deltaX / 2;
			pixelsToAdd = deltaY;
			numberOfPixels = deltaX;
		}
		else
		{
			incrementX2 = incrementY1 = 0;
			denominator = deltaY;
			numerator = deltaY / 2;
			pixelsToAdd = deltaX;
			numberOfPixels = deltaY;
		}
		
		int x = startX;
		int y = startY;
		
		for(int i=0; i < numberOfPixels; i++)
		{
			if((bounds != null &&
					(x >= bounds.left && x <= bounds.right) &&
					(y >= bounds.bottom && y <= bounds.top))
				|| bounds == null)
			{
				if(x >= 0 && x <= width && y >= 0 && y <= height)
					setPixel(x, y, bgColor, op);
			}
			numerator += pixelsToAdd;
			if(numerator >= denominator)
			{
				numerator -= denominator;
				x += incrementX1;
				y += incrementY1;
			}
			x += incrementX2;
			y += incrementY2;
		}
	}
}*/
