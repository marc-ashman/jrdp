package com.jrdp.core.remote.rdp.requests;

public class InputEvent extends RdpRequest
{
	public static final short INPUT_TYPE_MOUSE = (short) 0x8001;
	public static final short INPUT_TYPE_KEYBOARD_SCANCODE = 0x0004;
	public static final short INPUT_TYPE_KEYBOARD_UNICODE = 0x0005;
	public static final short INPUT_TYPE_SYNCHRONIZE = 0x0000;
	
	public static final short INPUT_FLAG_MOUSE_MOVE = 0x0800;
	public static final short INPUT_FLAG_MOUSE_LEFT_CLICK = (short) 0x9000;
	
	public static final short INPUT_FLAG_KEYBOARD_KEY_DOWN = 0x0000;
	public static final short INPUT_FLAG_KEYBOARD_KEY_UP = (short) 0x8000;
	
	private short eventType;
	private short flag;
	private short param1;
	private char unicodeKey;
	private short param2;
	
	public InputEvent(short eventType, short flag, short param1, short param2)
	{
		this.eventType = eventType;
		this.flag = flag;
		this.param1 = param1;
		this.param2 = param2;
	}
	
	public InputEvent(short eventType, short flag, char unicodeKey, short param2)
	{
		this.eventType = eventType;
		this.flag = flag;
		this.unicodeKey = unicodeKey;
		this.param2 = param2;
	}

	public short getEventType()
	{
		return eventType;
	}
	
	public short getFlag()
	{
		return flag;
	}

	public short getParam1()
	{
		return param1;
	}
	
	public char getUnicodeKey()
	{
		return unicodeKey;
	}

	public short getParam2()
	{
		return param2;
	}
}
