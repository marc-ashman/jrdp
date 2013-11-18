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
