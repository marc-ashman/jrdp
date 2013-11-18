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

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

import java.util.List;

public class WindowsKeyboardView extends KeyboardView
{
	public static final int KEY_DELETE = 1;
	public static final int KEY_UP = 2;
	public static final int KEY_HOME = 3;
	public static final int KEY_END = 4;
	public static final int KEY_PAGE_UP = 5;
	public static final int KEY_PAGE_DOWN = 6;
	public static final int KEY_ALT = 7;
	public static final int KEY_CTRL = 8;
	public static final int KEY_LEFT = 9;
	public static final int KEY_DOWN = 10;
	public static final int KEY_RIGHT = 11;
	public static final int KEY_ESC = 12;
	public static final int KEY_TAB = 13;
	public static final int KEY_WINDOWS = 14;
	public static final int KEY_SHIFT = 15;
	public static final int KEY_HIDE = 16;

	public static final int MODIFIER_ALT = 0x00000001;
	public static final int MODIFIER_CTRL = 0x00000002;
	public static final int MODIFIER_SHIFT = 0x00000004;
	
	public interface OnKeyboardKeyPressedListener
	{
		public void onKeyPressed(int key);
		public void onModifiersChanged(int modifierMask);
		public void onHideRequested();
	}
	
	private int modifierMask;
	private OnKeyboardKeyPressedListener listener;
	
	public WindowsKeyboardView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setOnKeyboardActionListener(new OnKeyboardActionListener()
		{
			@Override
			public void onKey(int code, int[] codes)
			{
				//Update modifierMask
				final int oldMask = modifierMask;
				modifierMask = 0;
				List<Keyboard.Key> keys = getKeyboard().getKeys();
				final int length = keys.size();
				for(int i=0; i < length; i++)
				{
					Keyboard.Key key = keys.get(i);
					if(key.on)
					{
						switch(key.codes[0])
						{
						case KEY_ALT:
							modifierMask |= MODIFIER_ALT;
							break;
						case KEY_CTRL:
							modifierMask |= MODIFIER_CTRL;
							break;
						case KEY_SHIFT:
							modifierMask |= MODIFIER_SHIFT;
							break;
						}
					}
				}
				if(oldMask != modifierMask)
					listener.onModifiersChanged(modifierMask);
				
				listener.onKeyPressed(code);
				
				if(code == KEY_HIDE)
					listener.onHideRequested();
			}

			@Override
			public void onPress(int code)
			{
			}

			@Override
			public void onRelease(int code)
			{
			}

			@Override
			public void onText(CharSequence text)
			{
			}

			@Override
			public void swipeDown()
			{
			}

			@Override
			public void swipeLeft()
			{
			}

			@Override
			public void swipeRight()
			{
			}

			@Override
			public void swipeUp()
			{
			}
		});
	}
	
	public void setOnKeyboardKeyPressedListener(OnKeyboardKeyPressedListener listener)
	{
		this.listener = listener;
	}
}
