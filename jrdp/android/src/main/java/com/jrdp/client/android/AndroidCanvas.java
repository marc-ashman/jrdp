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

package com.jrdp.client.android;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;

import com.jrdp.core.remote.rdp.Cursor;
import com.jrdp.core.remote.rdp.RemoteDesktopApplication;

public class AndroidCanvas extends BitmapDrawable implements com.jrdp.core.remote.Canvas
{
	private RemoteDesktopApplication app;
	//TODO: possible leak with activity here, change to handler
	private Handler handler;
	private Cursor cursor;
	private boolean showCursor;
	private int cursorX;
	private int cursorY;
	
	public AndroidCanvas(Resources res, Bitmap bitmap, Handler handler, RemoteDesktopApplication app)
	{
		super(res, bitmap);
		this.app = app;
		this.handler = handler;
	}
	
	@Override
	public void draw(Canvas canvas)
	{
		super.draw(canvas);
		if(showCursor && cursor != null)
		{
			canvas.drawBitmap(cursor.getCursorBitmap(), 0, cursor.getWidth(), cursorX - cursor.getHotspotX(), 
					cursorY - cursor.getHotspotY(), cursor.getWidth(), cursor.getHeight(), true, null);
		}
	}
	
	@Override
	public void setCanvas(int[] img, int width, int height, int x, int y,
			int clippingWidth, int clippingHeight)
	{
		getBitmap().setPixels(img, 0, width, x, y, clippingWidth, clippingHeight);
		invalidate();
	}

	@Override
	public void setCanvasBottomUp(int[] img, int width, int height, int x,
			int y, int clippingWidth, int clippingHeight)
	{
		getBitmap().setPixels(img, width * (clippingHeight - 1), 
				-width, x, y, clippingWidth, clippingHeight);
		invalidate();
	}

	@Override
	public void setCursor(Cursor cursor) {
		this.cursor = cursor;
		app.onRequestRemoteViewRefresh();
	}

	@Override
	public void hideCursor() {
		if(!showCursor)
			return;
		showCursor = false;
		app.onRequestRemoteViewRefresh();
	}

	@Override
	public void showCursor() {
		if(showCursor)
			return;
		showCursor = true;
		app.onRequestRemoteViewRefresh();
	}

	@Override
	public void cursorPositionChanged(int x, int y) {
		cursorX = x;
		cursorY = y;
		app.onRequestRemoteViewRefresh();
	}

	@Override
	public int getCursorX() {
		return cursorX;
	}

	@Override
	public int getCursorY() {
		return cursorY;
	}
	
	public void invalidate() {
		handler.post(new Runnable(){
			public void run()
			{
				invalidateSelf();
			}
		});
	}
}
