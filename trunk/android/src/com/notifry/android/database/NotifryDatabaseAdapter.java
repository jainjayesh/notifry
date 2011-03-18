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
 */

package com.notifry.android.database;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NotifryDatabaseAdapter
{
	public static final String KEY_ID = "_id";
	public static final String KEY_ACCOUNT_NAME = "account_name";
	public static final String KEY_ENABLED = "enabled";
	public static final String KEY_SERVER_REGISTRATION_ID = "server_registration_id";
	public static final String KEY_SERVER_ENABLED = "server_enabled";
	public static final String KEY_LOCAL_ENABLED = "local_enabled";
	public static final String KEY_TITLE = "title";
	public static final String KEY_SOURCE_KEY = "source_key";
	public static final String KEY_SERVER_ID = "server_id";
	public static final String KEY_CHANGE_TIMESTAMP = "change_timestamp";

	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	private static final String DATABASE_CREATE_ACCOUNTS = "create table accounts (_id integer primary key autoincrement, " +
			"account_name text not null, " +
			"server_registration_id long, " +
			"enabled integer not null " +
			");";

	private static final String DATABASE_CREATE_SOURCES = "create table sources (_id integer primary key autoincrement, " +
			"account_name text not null, " +
			"change_timestamp text not null, " +
			"title text not null, " +
			"server_id integer not null, " +
			"source_key text not null, " +
			"server_enabled integer not null, " +
			"local_enabled integer not null " +
			");";

	private static final String DATABASE_CREATE_MESSAGES = "create table messages (_id integer primary key autoincrement, " +
			"source_id integer not null, " +
			"timestamp text not null, " +
			"title text not null, " +
			"message text not null, " +
			"url text not null " +
			");";

	private static final String DATABASE_NAME = "notifry";
	private static final String DATABASE_TABLE_ACCOUNTS = "accounts";
	private static final String DATABASE_TABLE_SOURCES = "sources";
	private static final String DATABASE_TABLE_MESSAGES = "messages";

	private static final int DATABASE_VERSION = 1;

	private final Context context;

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
			// Not implemented. Here for when needed in the future.
		}
	}

	/**
	 * Create a new adapter.
	 * 
	 * @param context
	 */
	public NotifryDatabaseAdapter( Context context )
	{
		this.context = context;
	}

	/**
	 * Open the database.
	 * 
	 * @return
	 * @throws SQLException
	 */
	public NotifryDatabaseAdapter open() throws SQLException
	{
		this.dbHelper = new DatabaseHelper(this.context);
		this.db = this.dbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * Close the database.
	 */
	public void close()
	{
		db.close();
		dbHelper.close();
	}

	/**
	 * Sync the account list with our own copy, adding new ones as needed.
	 * @param accountManager
	 */
	public void syncAccountList( AccountManager accountManager )
	{
		Account[] accounts = accountManager.getAccountsByType("com.google");
		
		for( int i = 0; i < accounts.length; i++ )
		{
			NotifryAccount account = this.getAccountByName(accounts[i].name);
			
			if( account == null )
			{
				// Can't find it. Create one.
				account = new NotifryAccount();
				account.setEnabled(false); // Disabled by default.
				account.setAccountName(accounts[i].name);
				this.saveAccount(account);
			}
		}
	}
	
	/**
	 * List all the accounts in our database. This is not especially efficient, but you're only likely
	 * to have a few accounts on the phone.
	 * @return
	 */
	public ArrayList<NotifryAccount> listAccounts()
	{
		ArrayList<NotifryAccount> result = new ArrayList<NotifryAccount>();

		Cursor cursor = db.query(DATABASE_TABLE_ACCOUNTS, new String[] { KEY_ID, KEY_ACCOUNT_NAME, KEY_ENABLED, KEY_SERVER_REGISTRATION_ID }, null, null, null, null, null);
		
		if( cursor != null )
		{
			while( cursor.moveToNext() )
			{
				result.add(this.inflateFromCursor(cursor));
			}
			
			cursor.close();
		}
		
		return result;
	}

	/**
	 * Helper function to inflate a NotifryAccount object from a database cursor.
	 * @param cursor
	 * @return
	 */
	private NotifryAccount inflateFromCursor( Cursor cursor )
	{
		NotifryAccount account = new NotifryAccount();
		account = new NotifryAccount();
		account.setAccountName(cursor.getString(cursor.getColumnIndex(KEY_ACCOUNT_NAME)));
		account.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
		account.setEnabled(cursor.getLong(cursor.getColumnIndex(KEY_ENABLED)) == 0 ? false : true);
		account.setServerRegistrationId(cursor.getLong(cursor.getColumnIndex(KEY_SERVER_REGISTRATION_ID)));
		
		if( account.getServerRegistrationId() == 0 )
		{
			account.setServerRegistrationId(null);
		}
		return account;
	}

	/**
	 * Get an account object from an account name.
	 * 
	 * @param name The account name to find.
	 * @return An inflated NotifryAccount object, or NULL if not found.
	 */
	public NotifryAccount getAccountByName( String name )
	{
		NotifryAccount account = null;

		Cursor cursor = db.query(true, DATABASE_TABLE_ACCOUNTS, new String[] { KEY_ID, KEY_ACCOUNT_NAME, KEY_ENABLED, KEY_SERVER_REGISTRATION_ID }, KEY_ACCOUNT_NAME + "= ?", new String[] { name }, null, null, null, null);

		if( cursor != null )
		{
			cursor.moveToFirst();
			if( cursor.getCount() != 0 )
			{
				account = this.inflateFromCursor(cursor);
			}
			cursor.close();
		}

		return account;
	}
	
	/**
	 * Get an account from an ID.
	 * @param id The local ID of the account to fetch.
	 * @return An inflated account object, or NULL if not found.
	 */
	public NotifryAccount getAccountById( Long id )
	{
		NotifryAccount account = null;

		if (id != null)
		{
			Cursor cursor = db.query(true, DATABASE_TABLE_ACCOUNTS, new String[] { KEY_ID, KEY_ACCOUNT_NAME, KEY_ENABLED, KEY_SERVER_REGISTRATION_ID }, KEY_ID + "=" + id, null, null, null, null, null);

			if( cursor != null )
			{
				cursor.moveToFirst();
				if( cursor.getCount() != 0 )
				{
					account = this.inflateFromCursor(cursor);
				}
				cursor.close();
			}
		}

		return account;
	}
	
	/**
	 * Save the provided account object into the database.
	 * @param account
	 * @return
	 */
	public NotifryAccount saveAccount( NotifryAccount account )
	{
		ContentValues values = new ContentValues();
		values.put(KEY_ACCOUNT_NAME, account.getAccountName());
		values.put(KEY_ENABLED, account.getEnabled() ? 1 : 0);
		values.put(KEY_SERVER_REGISTRATION_ID, account.getServerRegistrationId());
		
		if( account.getId() == null)
		{
			// New object.
			account.setId(db.insert(DATABASE_TABLE_ACCOUNTS, null, values));
		}
		else
		{
			// Update the existing object.
			db.update(DATABASE_TABLE_ACCOUNTS, values, KEY_ID + "=" + account.getId(), null);
		}
		
		return account;
	}
}