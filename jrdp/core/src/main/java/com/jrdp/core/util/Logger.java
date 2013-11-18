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
