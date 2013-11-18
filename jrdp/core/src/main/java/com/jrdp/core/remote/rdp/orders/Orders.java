//package fusion.remote.rdp.orders;
//
//import fusion.remote.rdp.RdpCanvas;
//import util.InputByteStream;
//
//public class Orders
//{
//	private static final byte ORDER_STANDARD = 0x01;
//	private static final byte ORDER_SECONDARY = 0x02;
//	private static final byte ORDER_CHANGE = 0x08;
//
//    private static final int ORDER_ZERO_FILLED_BYTE_BIT0 = 0x40;
//    private static final int ORDER_ZERO_FILLED_BYTE_BIT1 = 0x80;
//	//Standard Orders
//    private static final int ORDER_DESTBLT = 0;
//    private static final int ORDER_PATBLT = 1;
//    private static final int ORDER_SCREENBLT = 2;
//    private static final int ORDER_LINE = 9;
//    private static final int ORDER_RECT = 10;
//    private static final int ORDER_DESKSAVE = 11;
//    private static final int ORDER_MEMBLT = 13;
//    private static final int ORDER_TRIBLT = 14;
//    private static final int ORDER_POLYLINE = 22;
//    private static final int ORDER_TEXT2 = 27;
//    //Secondary Orders
//    private static final int ORDER_RAW_BMPCACHE = 0;
//    private static final int ORDER_COLCACHE = 1;
//    private static final int ORDER_BMPCACHE = 2;
//    private static final int ORDER_FONTCACHE = 3;
//    private static final int ORDER_RAW_BMPCACHE2 = 4;
//    private static final int ORDER_BMPCACHE2 = 5;
//    
//    private static final int ORDER_BOUNDS = 0x04;
//    private static final int ORDER_DELTA = 0x10;
//    private static final int ORDER_ZERO_BOUNDS_DELTAS = 0x20;
//    
//    private static final int MIX_TRANSPARENT = 0;
//    private static final int MIX_OPAQUE = 1;
//    private static final int TEXT2_VERTICAL = 0x04;
//    private static final int TEXT2_IMPLICIT_X = 0x20;
//	
//	private State state;
//	private RdpCanvas canvas;
//	private int bitsPerPixel;
//	
//	public Orders(RdpCanvas canvas, int bitsPerPixel)
//	{
//		this.canvas = canvas;
//		this.bitsPerPixel = bitsPerPixel;
//		state = new State();
//	}
//	
//	public boolean processOrder(InputByteStream packet)
//	{
//		int controlFlags = packet.getByte() & 0xff;
//		
//		if((controlFlags & ORDER_STANDARD) == 0)
//			return false;
//		if((controlFlags & ORDER_SECONDARY) != 0)
//			return processSecondaryOrder(packet);
//		if(false)
//			return false;
//		else
//		{
//			if((controlFlags & ORDER_CHANGE) != 0)
//				state.orderType = packet.getByte() & 0xff;
//			int size;
//			switch(state.orderType)
//			{
//			case ORDER_TEXT2:
//			case ORDER_TRIBLT:
//				size = 3;
//				break;
//			case ORDER_MEMBLT:
//			case ORDER_PATBLT:
//			case ORDER_LINE:
//				size = 2;
//				break;
//			default:
//				size = 1;
//				break;
//			}
//			
//			int flags = 0;
//			if((controlFlags & ORDER_ZERO_FILLED_BYTE_BIT0) != 0)
//				size--;
//			if((controlFlags & ORDER_ZERO_FILLED_BYTE_BIT1) != 0)
//			{
//				if(size < 2)
//					size = 0;
//				else
//					size -= 2;
//			}
//			for(int i=0; i < size; i++)
//				flags |= ((packet.getByte() & 0xff) << (i * 8));
//			
//			boolean useBounds = false;
//			if((controlFlags & ORDER_BOUNDS) != 0)
//			{
//				if((controlFlags & ORDER_ZERO_BOUNDS_DELTAS) == 0)
//				{
//					int boundsFlags = packet.getByte() & 0xff;
//					//store in temp var for faster access
//					Bounds bounds = state.bounds;
//					
//					if((boundsFlags & 0x01) != 0)
//						bounds.left = getCoordinate(packet, bounds.left, false);
//					else if((boundsFlags & 0x10) != 0)
//						bounds.left = getCoordinate(packet, bounds.left, true);
//	
//					if((boundsFlags & 0x02) != 0)
//						bounds.top = getCoordinate(packet, bounds.top, false);
//					else if((boundsFlags & 0x20) != 0)
//						bounds.top = getCoordinate(packet, bounds.top, true);
//					
//					if((boundsFlags & 0x04) != 0)
//						bounds.right = getCoordinate(packet, bounds.right, false);
//					else if((boundsFlags & 0x40) != 0)
//						bounds.right = getCoordinate(packet, bounds.right, true);
//					
//					if((boundsFlags & 0x08) != 0)
//						bounds.bottom = getCoordinate(packet, bounds.bottom, false);
//					else if((boundsFlags & 0x80) != 0)
//						bounds.bottom = getCoordinate(packet, bounds.bottom, true);
//					
//				}
//				useBounds = true;
//			}
//			
//			boolean isDelta = ((controlFlags & ORDER_DELTA) != 0);
//			switch(state.orderType)
//			{
//			case ORDER_LINE:
//				System.out.println("Line");
//				processLineOrder(packet, state.lineTo, flags, isDelta, useBounds);
//				break;
//			case ORDER_POLYLINE:
//				System.out.println("Polyline");
//				processPolylineOrder(packet, state.polyline, flags, isDelta, useBounds);
//				break;
//			case ORDER_RECT:
//				System.out.println("Rectangle");
//				processRectangleOrder(packet, state.rect, flags, isDelta, useBounds);
//				break;
//			default:
//				System.out.print("Unknown order type: " + Integer.toHexString(state.orderType & 0xff));
//				break;
//			}
//			
//			return true;
//		}
//	}
//	
//	private void processRectangleOrder(InputByteStream packet,
//			Rectangle rect, int flags, boolean delta, boolean useBounds)
//	{
//		if((flags & 0x01) != 0)
//			rect.x = getCoordinate(packet, rect.x, delta);
//		if((flags & 0x02) != 0)
//			rect.y = getCoordinate(packet, rect.y, delta);
//		if((flags & 0x04) != 0)
//			rect.width = getCoordinate(packet, rect.width, delta);
//		if((flags & 0x08) != 0)
//			rect.height = getCoordinate(packet, rect.height, delta);
//		if((flags & 0x10) != 0)
//			rect.color = (rect.color & 0xffffff00) | (packet.getByte() & 0xff);
//		if((flags & 0x20) != 0)
//			rect.color = (rect.color & 0xffff00ff) | ((packet.getByte() & 0xff) << 8);
//		if((flags & 0x40) != 0)
//			rect.color = (rect.color & 0xff00ffff) | ((packet.getByte() & 0xff) << 16);
//		rect.color = to24BitColor(rect.color, bitsPerPixel);
//		
//		canvas.drawRect(rect, state.bounds);
//	}
//	
//	private void processPolylineOrder(InputByteStream packet,
//			Polyline polyline, int flags, boolean delta, boolean useBounds)
//	{
//		if((flags & 0x01) != 0)
//			polyline.x = getCoordinate(packet, polyline.x, delta);
//		if((flags & 0x02) != 0)
//			polyline.y = getCoordinate(packet, polyline.y, delta);
//		if((flags & 0x04) != 0)
//			polyline.op = packet.getByte() & 0xff;
//		if((flags & 0x10) != 0)
//			polyline.fgColor = getColor(packet);
//		if((flags & 0x20) != 0)
//			polyline.lines = packet.getByte() & 0xff;
//		if((flags & 0x40) != 0)
//		{
//			int size = packet.getByte() & 0xff;
//			polyline.size = size;
//			for(int i=0; i < size; i++)
//				polyline.data[i] = (byte) (packet.getByte() & 0xff);
//		}
//		
//		polyline.fgColor = to24BitColor(polyline.fgColor, bitsPerPixel);
//		Bounds bounds = useBounds ? state.bounds : null;
//		
//		canvas.drawPolyline(polyline, bounds);
//	}
//	
//	private void processLineOrder(InputByteStream packet,
//			Line line, int flags, boolean delta, boolean useBounds)
//	{
//		if((flags & 0x01) != 0)
//			line.mixmode = packet.getShortLittleEndian();
//		if((flags & 0x02) != 0)
//			line.startX = getCoordinate(packet, line.startX, delta);
//		if((flags & 0x04) != 0)
//			line.startY = getCoordinate(packet, line.startY, delta);
//		if((flags & 0x08) != 0)
//			line.endX = getCoordinate(packet, line.endX, delta);
//		if((flags & 0x10) != 0)
//			line.endY = getCoordinate(packet, line.endY, delta);
//		if((flags & 0x20) != 0)
//			line.bgColor = getColor(packet);
//		if((flags & 0x40) != 0)
//			line.op = packet.getByte() & 0xff;
//		
//		setPen(packet, line.pen, flags >> 7);
//		
//		line.bgColor = to24BitColor(line.bgColor, bitsPerPixel);
//		Bounds bounds = useBounds ? state.bounds : null;
//		
//		canvas.drawLine(line, bounds);
//	}
//	
//	private static int getCoordinate(InputByteStream packet, int coordinate, boolean isDelta)
//	{
//		if(isDelta)
//		{
//			//get byte, but need to keep the sign bit
//			byte delta = (byte) packet.getByte();
//			int change = coordinate + (int) delta;
//			return change;
//		}
//		else
//			return packet.getShortLittleEndian();
//	}
//	
//	private static int getColor(InputByteStream packet)
//	{
//		return (packet.getByte() & 0xff) | ((packet.getByte() & 0xff) << 8) | ((packet.getByte() & 0xff) << 16);
//	}
//	
//	private static void setPen(InputByteStream packet, Pen pen, int flags)
//	{
//		if((flags & 0x01) != 0)
//			pen.style = packet.getByte() & 0xff;
//		if((flags & 0x02) != 0)
//			pen.width = packet.getByte() & 0xff;
//		if((flags & 0x04) != 0)
//			pen.color = getColor(packet);
//	}
//	
//	public boolean processSecondaryOrder(InputByteStream packet)
//	{
//		System.out.println("Secondary order bla bla bla");
//		int length = packet.getShortLittleEndian();
//		int flags = packet.getShortLittleEndian();
//		int type = packet.getByte() & 0xff;
//		switch(type)
//		{
//		default:
//			System.out.println("Unknown secondary order: " + Integer.toHexString(type & 0xff));
//			break;
//		}
//		return true;
//	}
//	
//	private static int to24BitColor(int color, int bitsPerPixel)
//	{
//		int red, green, blue;
//		switch(bitsPerPixel)
//		{
//		case 15:
//			red = (color >> 7) & 0xF8;
//	        green = (color >> 2) & 0xF8;
//	        blue = (color << 3) & 0xFF;
//	        
//	        red |= red >> 5;
//	        green |= green >> 5;
//	        blue |= blue >> 5;
//	        
//	        return (red << 16) | (green << 8) | blue;
//		case 16:
//			red = (color >> 8) & 0xF8;
//	        green = (color >> 3) & 0xFC;
//	        blue = (color << 3) & 0xFF;
//	        
//	        red |= red >> 5;
//	        green |= green >> 6;
//	        blue |= blue >> 5;
//	        
//	        return (red << 16) | (green << 8) | blue;
//		case 24:
//			return color;
//		case 32:
//			return color;
//		default:
//			return color;
//		}
//	}
//	
//	private class State
//	{
//		public int orderType = 0;
//		public Line lineTo;
//		public Bounds bounds;
//		public Polyline polyline;
//		public Rectangle rect;
//		
//		public State()
//		{
//			lineTo = new Line();
//			bounds = new Bounds();
//			polyline = new Polyline();
//			rect = new Rectangle();
//		}
//		
//		public void reset()
//		{
//			lineTo.reset();
//			bounds.reset();
//			polyline.reset();
//			rect.reset();
//		}
//	}
//}
