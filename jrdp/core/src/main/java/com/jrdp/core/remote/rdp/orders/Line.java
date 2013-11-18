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
