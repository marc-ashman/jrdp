package com.jrdp.core.util;

public class Logger
{
	public static final int INFO = 0;
	public static final int DEBUG = 1;
	public static final int ERROR = 2;
	private static LoggerInterface logger;
	
	public static void setLogger(LoggerInterface newLogger)
	{
		logger = newLogger;
	}
	
	public static void log(int type, String message)
	{
		if(logger != null)
			logger.log(type, message);
	}
	
	public static void log(String message)
	{
		if(logger != null)
			logger.log(message);
	}
	
	public static void log(Exception e)
	{
		if(logger != null)
			logger.log(e);
	}
	
	public static void log(Exception e, String message)
	{
		if(logger != null)
			logger.log(e, message);
	}
	
	public interface LoggerInterface
	{
		public void log(int type, String message);
		public void log(String message);
		public void log(Exception e);
		public void log(Exception e, String message);
	}
}
