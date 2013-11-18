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
