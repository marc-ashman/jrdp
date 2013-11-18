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

public interface RemoteDesktopApplication
{
	public void onScreenDimensionsChanged(int width, int height);
	public void onRemoteDimensionsChanged(int width, int height);
	public void onCursorChanged(int deltaX, int deltaY);
	public void onCursorSet(int x, int y);
	public void onCursorLeftClick();
	public void onSpecialKeyboardKey(short key);
	public void onKeyboardKey(char key);
	public void onRequestRemoteViewRefresh();
	public void requestDisconnect(boolean askUser);
}
