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
