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