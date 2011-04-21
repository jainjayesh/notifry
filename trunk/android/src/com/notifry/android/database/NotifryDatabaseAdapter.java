/**
 * Notifry for Android.
 * 
 * Copyright 2011 Daniel Foote
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Excellent writeup of content providers:
 * http://www.devx.com/wireless/Article/41133/1763/page/1
 */

package com.notifry.android.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class NotifryDatabaseAdapter extends ContentProvider
{
	public static final String PROVIDER_NAME_ACCOUNTS = "com.notifry.android.provider.NotifryAccounts";
	public static final String PROVIDER_NAME_SOURCES = "com.notifry.android.provider.NotifrySources";
	public static final String PROVIDER_NAME_MESSAGES = "com.notifry.android.provider.NotifryMessages";
	
	public static final Uri CONTENT_URI_ACCOUNTS = Uri.parse("content://"+ PROVIDER_NAME_ACCOUNTS + "/accounts");
    public static final Uri CONTENT_URI_SOURCES = Uri.parse("content://"+ PROVIDER_NAME_SOURCES + "/sources");
    public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://"+ PROVIDER_NAME_MESSAGES + "/messages");
    
    private static final int ACCOUNTS = 1;
    private static final int ACCOUNT_ID = 2;
    private static final int SOURCES = 3;
    private static final int SOURCE_ID = 4;
    private static final int MESSAGES = 5;
    private static final int MESSAGE_ID = 6;    
    
    private static final UriMatcher uriMatcher;
    static
    {
    	uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	uriMatcher.addURI(PROVIDER_NAME_ACCOUNTS, "accounts", ACCOUNTS);
    	uriMatcher.addURI(PROVIDER_NAME_ACCOUNTS, "accounts/#", ACCOUNT_ID);
    	uriMatcher.addURI(PROVIDER_NAME_SOURCES, "sources", SOURCES);
    	uriMatcher.addURI(PROVIDER_NAME_SOURCES, "sources/#", SOURCE_ID);
    	uriMatcher.addURI(PROVIDER_NAME_MESSAGES, "messages", MESSAGES);
    	uriMatcher.addURI(PROVIDER_NAME_MESSAGES, "messages/#", MESSAGE_ID);
    }
	
	private static final String TAG = "Notifry";
	public static final String KEY_ID = "_id";
	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ENABLED = "enabled";
	public static final String KEY_SERVER_REGISTRATION_ID = "server_registration_id";
	public static final String KEY_SERVER_ENABLED = "server_enabled";
	public static final String KEY_LOCAL_ENABLED = "local_enabled";
	public static final String KEY_LAST_C2DM_ID = "last_c2dm_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_SOURCE_KEY = "source_key";
	public static final String KEY_SERVER_ID = "server_id";
	public static final String KEY_CHANGE_TIMESTAMP = "change_timestamp";
	public static final String KEY_SOURCE_ID = "source_id";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_URL = "url";
	public static final String KEY_SEEN = "seen";
	public static final String KEY_REQUIRES_SYNC = "requires_sync";
	public static final String KEY_USE_GLOBAL_NOTIFICATION = "use_global_notification";
	public static final String KEY_VIBRATE = "vibrate";
	public static final String KEY_RINGTONE = "ringtone";
	public static final String KEY_CUSTOM_RINGTONE = "custom_ringtone";
	public static final String KEY_LED_FLASH = "led_flash";
	public static final String KEY_SPEAK_MESSAGE = "speak_message";
	
	public static final String[] ACCOUNT_PROJECTION = new String[] { KEY_ID, KEY_ACCOUNT_NAME, KEY_ENABLED, KEY_SERVER_REGISTRATION_ID, KEY_REQUIRES_SYNC, KEY_LAST_C2DM_ID };
	public static final String[] SOURCE_PROJECTION = new String[] { KEY_ID, KEY_ACCOUNT_NAME, KEY_CHANGE_TIMESTAMP, KEY_TITLE, KEY_SERVER_ID, KEY_SOURCE_KEY, KEY_SERVER_ENABLED, KEY_LOCAL_ENABLED, KEY_USE_GLOBAL_NOTIFICATION, KEY_VIBRATE, KEY_RINGTONE, KEY_CUSTOM_RINGTONE, KEY_LED_FLASH, KEY_SPEAK_MESSAGE };
	public static final String[] MESSAGE_PROJECTION = new String[] { KEY_ID, KEY_SOURCE_ID, KEY_TIMESTAMP, KEY_TITLE, KEY_MESSAGE, KEY_URL, KEY_SERVER_ID, KEY_SEEN };	

	private SQLiteDatabase db;

	private static final String DATABASE_CREATE_ACCOUNTS = "create table accounts (_id integer primary key autoincrement, " +
			"account_name text not null, " +
			"server_registration_id long, " +
			"enabled integer not null, " +
			"requires_sync integer not null, " +
			"last_c2dm_id text " +
			");";

	private static final String DATABASE_CREATE_SOURCES = "create table sources (_id integer primary key autoincrement, " +
			"account_name text not null, " +
			"change_timestamp text not null, " +
			"title text not null, " +
			"server_id integer not null, " +
			"source_key text not null, " +
			"server_enabled integer not null, " +
			"local_enabled integer not null, " +
			"use_global_notification integer not null, " +
			"vibrate integer not null, " +
			"ringtone integer not null, " +
			"custom_ringtone text not null, " +
			"led_flash integer not null, " +
			"speak_message integer not null " +
			");";

	private static final String DATABASE_CREATE_MESSAGES = "create table messages (_id integer primary key autoincrement, " +
			"source_id integer not null, " +
			"server_id integer not null, " +
			"timestamp text not null, " +
			"title text not null, " +
			"message text not null, " +
			"url text, " +
			"seen integer not null " +
			");";

	private static final String DATABASE_NAME = "notifry";
	private static final String DATABASE_TABLE_ACCOUNTS = "accounts";
	private static final String DATABASE_TABLE_SOURCES = "sources";
	private static final String DATABASE_TABLE_MESSAGES = "messages";

	private static final int DATABASE_VERSION = 3;

	/**
	 * Database helper class to create and manage the schema.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper
	{
		DatabaseHelper( Context context )
		{
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate( SQLiteDatabase db )
		{
			db.execSQL(DATABASE_CREATE_ACCOUNTS);
			db.execSQL(DATABASE_CREATE_SOURCES);
			db.execSQL(DATABASE_CREATE_MESSAGES);
		}

		@Override
		public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion )
		{
			// Upgrading from v1 (during BETA) to v3.
			if( oldVersion < 3 )
			{
				db.execSQL("ALTER TABLE sources ADD COLUMN speak_message integer not null default 0;");
				db.execSQL("ALTER TABLE sources ADD COLUMN use_global_notification integer not null default 1;");
				db.execSQL("ALTER TABLE sources ADD COLUMN vibrate integer not null default 0;");
				db.execSQL("ALTER TABLE sources ADD COLUMN ringtone integer not null default 0;");
				db.execSQL("ALTER TABLE sources ADD COLUMN custom_ringtone text not null default '';");
				db.execSQL("ALTER TABLE sources ADD COLUMN led_flash integer not null default 0;");
			}
		}
	}
	
	@Override
	public String getType( Uri uri )
	{
		switch( uriMatcher.match(uri) )
		{
			// Get all accounts.
			case ACCOUNTS:
				return "vnd.android.cursor.dir/vnd.notifry.accounts";
			// Get a single account.
			case ACCOUNT_ID:
				return "vnd.android.cursor.item/vnd.notifry.accounts";
			// Get all accounts.
			case SOURCES:
				return "vnd.android.cursor.dir/vnd.notifry.sources";
			// Get a single account.
			case SOURCE_ID:
				return "vnd.android.cursor.item/vnd.notifry.sources";
				// Get all accounts.
			case MESSAGES:
				return "vnd.android.cursor.dir/vnd.notifry.messages";
			// Get a single account.
			case MESSAGE_ID:
				return "vnd.android.cursor.item/vnd.notifry.messages";				
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	@Override
	public boolean onCreate()
	{
		Context context = getContext();
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
		return (db == null) ? false : true;
	}
	
	private String getTableFor( Uri uri )
	{
		switch( uriMatcher.match(uri) )
		{
			case ACCOUNTS:
			case ACCOUNT_ID:
				return DATABASE_TABLE_ACCOUNTS;
			case SOURCES:
			case SOURCE_ID:
				return DATABASE_TABLE_SOURCES;
			case MESSAGES:
			case MESSAGE_ID:
				return DATABASE_TABLE_MESSAGES;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	private Uri getContentUriFor( Uri uri )
	{
		switch( uriMatcher.match(uri) )
		{
			case ACCOUNTS:
			case ACCOUNT_ID:
				return CONTENT_URI_ACCOUNTS;
			case SOURCES:
			case SOURCE_ID:
				return CONTENT_URI_SOURCES;
			case MESSAGES:
			case MESSAGE_ID:
				return CONTENT_URI_MESSAGES;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}	
	
	@Override
	public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder )
	{
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		
		// Choose the table.
		sqlBuilder.setTables(this.getTableFor(uri));
		
		// If it's a single ID matcher, limit to a single 
		switch( uriMatcher.match(uri) )
		{
			case ACCOUNT_ID:
			case SOURCE_ID:
			case MESSAGE_ID:
				sqlBuilder.appendWhere(KEY_ID + " = " + uri.getPathSegments().get(1));
				break;
		}

		// Perform the query.
		Cursor cursor = sqlBuilder.query(
				this.db,
				projection,
				selection,
				selectionArgs,
				null,
				null,
				sortOrder);

		// Tell the cursor to listen for changes.
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}
	
	@Override
	public Uri insert( Uri uri, ContentValues values )
	{
		// Insert into the database...
		long rowID = this.db.insert(this.getTableFor(uri), "", values);

		// And on success...
		if( rowID > 0 )
		{
			// Create our return URI.
			Uri _uri = ContentUris.withAppendedId(this.getContentUriFor(uri), rowID);
			// And notify anyone watching that it's changed.
			getContext().getContentResolver().notifyChange(this.getContentUriFor(_uri), null);
			getContext().getContentResolver().notifyChange(_uri, null);
			return _uri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}
	
	@Override
	public int update( Uri uri, ContentValues values, String selection, String[] selectionArgs )
	{
		int count = 0;
		// Determine the table.
		String table = this.getTableFor(uri);
		// Perform the update.
		count = this.db.update(table, values, selection, selectionArgs);
		// Notify anyone that we've changed things.
		getContext().getContentResolver().notifyChange(this.getContentUriFor(uri), null);
		getContext().getContentResolver().notifyChange(uri, null);
		// And return the number of changed rows.
		return count;
	}	
	
	@Override
	public int delete( Uri uri, String selection, String[] selectionArgs )
	{
		int count = 0;
		// Determine the table.
		String table = this.getTableFor(uri);
		// Do the deletion.
		count = this.db.delete(table, selection, selectionArgs);
		
		// And notify anyone that we've changed things.
		getContext().getContentResolver().notifyChange(this.getContentUriFor(uri), null);
		getContext().getContentResolver().notifyChange(uri, null);
		
		// Return the number of deleted entries.
		return count;
	}
}