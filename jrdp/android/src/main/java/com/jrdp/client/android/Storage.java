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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import com.jrdp.core.util.Logger;

import java.util.Vector;

public class Storage
{
	public static final String TABLE_CONNECTION_INFO = "connections";
	public static final String ROW_ID = "_id";
	public static final String ROW_NICKNAME = "nick";
	public static final String ROW_IP = "ip";
	public static final String ROW_PORT = "port";
	public static final String ROW_USERNAME = "user";
	public static final String ROW_PASSWORD = "pass";
	public static final String ROW_DOMAIN = "domain";
	public static final String ROW_ENCRYPTION_LEVEL = "enc";
	public static final String ROW_PERFORMANCE_FLAGS = "perf";
	public static final String ROW_RESOLUTION_INDEX = "resolution";
	public static final String ROW_COLOR = "color";
	public static final String ROW_INPUT_TYPE = "input";
	public static final String ROW_LIST_ORDER = "listorder";
	
	private static final String DATABASE_NAME = "RDPStorage.db";
	private static final int DATABASE_VERSION = 1;
	private DatabaseHelper helper;
	private Vector<StorageListener> listeners;
	
	public Storage(Context context)
	{
		listeners = new Vector<StorageListener>();
		helper = new DatabaseHelper(context, DATABASE_NAME, null, DATABASE_VERSION);
	}
	
	public synchronized Cursor getData(String table, String[] columns, String where, String orderBy, String limit)
	{
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor result = db.query(table, columns, where, null, null, null, orderBy, limit);
		return result;
	}
	
	public synchronized int updateData(String table, ContentValues updatedData, String where)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		int affectedRows = db.update(table, updatedData, where, null);
		db.close();
		if(affectedRows > 0)
			notifyListeners();
		return affectedRows;
	}
	
	public synchronized long insert(String table, ContentValues values)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		long affectedRows = db.insert(table, null, values);
		db.close();
		if(affectedRows != -1)
			notifyListeners();
		return affectedRows;
	}
	
	public synchronized int delete(String table, String where)
	{
		SQLiteDatabase db = helper.getWritableDatabase();
		int affectedRows = db.delete(table, where, null);
		db.close();
		if(affectedRows > 0)
			notifyListeners();
		return affectedRows;
	}
	
	public synchronized void addListener(StorageListener listener)
	{
		listeners.add(listener);
	}
	
	public synchronized void removeListener(StorageListener listener)
	{
		listeners.remove(listener);
	}
	
	private synchronized void notifyListeners()
	{
		int size = listeners.size();
		for(int i=0; i < size; i++)
		{
			listeners.elementAt(i).onStoredDataChangedListener(this);
		}
	}
	
	private class DatabaseHelper extends SQLiteOpenHelper
	{
		public DatabaseHelper(Context context, String name, CursorFactory factory, int version)
		{
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db)
		{
			db.execSQL("CREATE TABLE " + TABLE_CONNECTION_INFO + " (" +
					ROW_ID + " INTEGER PRIMARY KEY," +
					ROW_NICKNAME + " TEXT," +
					ROW_IP + " TEXT," + 
					ROW_PORT + " INTEGER," +
					ROW_USERNAME + " TEXT," +
					ROW_PASSWORD + " TEXT," +
					ROW_DOMAIN + " TEXT," +
					ROW_ENCRYPTION_LEVEL + " INTEGER," +
					ROW_PERFORMANCE_FLAGS + " INTEGER," +
					ROW_RESOLUTION_INDEX + " INTEGER," +
					ROW_COLOR + " INTEGER," +
					ROW_INPUT_TYPE + " INTEGER," +
					ROW_LIST_ORDER + " INTEGER );");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
		{
			Logger.log(Logger.INFO, "Upgrading database from " + oldVersion + " to " + newVersion);
			//TODO: Store all db data in variables, move it into newly-created database
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONNECTION_INFO + ";");
			onCreate(db);
		}
	}
	
	public interface StorageListener
	{
		public void onStoredDataChangedListener(Storage storage);
	}
}
