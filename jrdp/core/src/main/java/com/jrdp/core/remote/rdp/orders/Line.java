package com.jrdp.core.remote.rdp.orders;

/**
 * @see <a href=http://msdn.microsoft.com/en-us/library/cc241589(v=PROT.10).aspx>
 * 2.2.2.2.1.1.2.11 LineTo (LINETO_ORDER)</a>
 */
class Line
{
	public int mixmode;
	public int startX;
	public int startY;
	public int endX;
	public int endY;
	public int bgColor;
	public int op;
	public Pen pen;
	
	public Line()
	{
		pen = new Pen();
	}
	
	public void reset()
	{
		mixmode = startX = startY = endX = endY = bgColor = op = 0;
		pen.reset();
	}
}
