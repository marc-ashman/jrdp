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

import android.util.Log;

import com.jrdp.core.util.Logger;


public class AndroidLogger implements Logger.LoggerInterface
{
	public static final int LOG_LEVEL_DEBUG = 0x00000001;
	public static final int LOG_LEVEL_INFO = 0x00000010;
	public static final int LOG_LEVEL_ERROR = 0x00000100;
	private static final String LOG_LABEL = "RDP";
	
	private int logLevel;
	
	public AndroidLogger(int logLevel)
	{
		this.logLevel = logLevel;
	}
	
	@Override
	public void log(int type, String message)
	{
		switch(type)
		{
		case Logger.DEBUG:
			if((logLevel & LOG_LEVEL_DEBUG) != 0)
				Log.d(LOG_LABEL, message);
			break;
		case Logger.ERROR:
			if((logLevel & LOG_LEVEL_ERROR) != 0)
				Log.e(LOG_LABEL, message);
			break;
		case Logger.INFO:
			if((logLevel & LOG_LEVEL_INFO) != 0)
				Log.i(LOG_LABEL, message);
			break;
		}
	}
	
	@Override
	public void log(String message) {
		Log.i(LOG_LABEL, message);
	}

	@Override
	public void log(Exception e) {
		if(e != null)
			Log.e(LOG_LABEL, e.getClass() + e.getMessage());
		Log.e(LOG_LABEL, "Unknown exception occured");
	}

	@Override
	public void log(Exception e, String message) {
		if(e != null)
			Log.e(LOG_LABEL, message + " :: " + e.getClass() + e.getMessage());
		else
			Log.e(LOG_LABEL, message);
	}
}
