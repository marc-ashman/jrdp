package com.jrdp.core.remote;

public interface RemoteClient
{
	public void moveMouse(short deltaX, short deltaY);
	public void sendKey(char key);
	public void sendSpecialKey(short key);
}
