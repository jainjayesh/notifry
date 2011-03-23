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
import java.util.HashMap;
import java.util.HashSet;

import com.notifry.android.remote.BackendRequest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

public class NotifryAccount extends ORM<NotifryAccount>
{
	public final static NotifryAccount FACTORY = new NotifryAccount();
	
	private String accountName = null;
	private Long serverRegistrationId = null;
	private Boolean enabled = null;
	private Boolean requiresSync = true;

	public String getAccountName()
	{
		return accountName;
	}

	public void setAccountName( String accountName )
	{
		this.accountName = accountName;
	}

	public Boolean getEnabled()
	{
		return enabled;
	}

	public void setEnabled( Boolean enabled )
	{
		this.enabled = enabled;
	}

	public void setServerRegistrationId( Long serverRegistrationId )
	{
		this.serverRegistrationId = serverRegistrationId;
	}

	public Long getServerRegistrationId()
	{
		return serverRegistrationId;
	}
	
	public Boolean getRequiresSync()
	{
		return requiresSync;
	}

	public void setRequiresSync( Boolean requiresSync )
	{
		this.requiresSync = requiresSync;
	}

	/**
	 * Register the device with the server.
	 * @param context
	 * @param key
	 * @param showStatus
	 */
	public void registerWithBackend( Context context, String key, boolean register, String statusMessage, Handler handler, HashMap<String, Object> metadata )
	{
		// Register the device with the server.
		BackendRequest request = new BackendRequest("/devices/register");
		request.add("devicekey", key);
		request.add("devicetype", "android");
		try
		{
			request.add("deviceversion", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
		}
		catch( NameNotFoundException e )
		{
			request.add("deviceversion", "Unknown");
		}
		
		// Send something so we know what the device is.
		request.add("nickname", Build.MODEL);

		// If already registered, update the same entry.
		if( this.getServerRegistrationId() != null )
		{
			request.add("id", this.getServerRegistrationId().toString());
		}

		if( register )
		{
			request.add("operation", "add");
		}
		else
		{
			request.add("operation", "remove");
		}

		// For debugging, dump the request data.
		//request.dumpRequest();
		
		// And the callback handler, if required.
		request.setHandler(handler);
		
		// Add any metadata if required.
		if( metadata != null )
		{
			for( String metaKey: metadata.keySet() )
			{
				request.addMeta(metaKey, metadata.get(metaKey));
			}
		}		
		
		// Start a thread to make the request.
		request.startInThread(context, statusMessage, this.getAccountName());
	}
	
	public ArrayList<NotifryAccount> listAll( Context context )
	{
		return NotifryAccount.FACTORY.genericList(context, null, null, NotifryDatabaseAdapter.KEY_ACCOUNT_NAME + " ASC");
	}	
	
	public NotifryAccount getByAccountName( Context context, String accountName )
	{
		return this.getOne(context, NotifryDatabaseAdapter.KEY_ACCOUNT_NAME + "=?", new String[] { accountName });
	}
	
	public NotifryAccount getByServerId( Context context, Long serverId )
	{
		return this.getOne(context, NotifryDatabaseAdapter.KEY_SERVER_REGISTRATION_ID + "=" + serverId, null);
	}
	
	public void deleteByAccountName( Context context, String accountName )
	{
		this.genericDelete(context, NotifryDatabaseAdapter.KEY_ACCOUNT_NAME + "= ?", new String[] { accountName });
	}
	
	/**
	 * Sync the account list with our own copy, adding new ones as needed.
	 * @param accountManager
	 */
	public void syncAccountList( Context context, AccountManager accountManager )
	{
		Account[] accounts = accountManager.getAccountsByType("com.google");
		
		HashSet<String> seenAccounts = new HashSet<String>();
		
		for( int i = 0; i < accounts.length; i++ )
		{
			NotifryAccount account = NotifryAccount.FACTORY.getByAccountName(context, accounts[i].name);
			
			if( account == null )
			{
				// Can't find it. Create one.
				account = new NotifryAccount();
				account.setEnabled(false); // Disabled by default.
				account.setAccountName(accounts[i].name);
				account.save(context);
			}
			
			seenAccounts.add(accounts[i].name);
		}
		
		// List all accounts, and add them to a list of accounts in the database.
		ArrayList<NotifryAccount> allAccounts = NotifryAccount.FACTORY.listAll(context);
		HashSet<String> localAccounts = new HashSet<String>();
		
		for( NotifryAccount account: allAccounts )
		{
			localAccounts.add(account.getAccountName());
		}
		
		// Intersect the sets, and remove any accounts as appropriate.
		localAccounts.removeAll(seenAccounts);
		
		// Now remove anything in local accounts that should not be there.
		for( String accountName: localAccounts )
		{
			NotifryAccount.FACTORY.deleteByAccountName(context, accountName);
		}
		
		// And we're finally complete!
	}	

	@Override
	public Uri getContentUri()
	{
		return NotifryDatabaseAdapter.CONTENT_URI_ACCOUNTS;
	}

	@Override
	protected ContentValues flatten()
	{
		ContentValues values = new ContentValues();
		values.put(NotifryDatabaseAdapter.KEY_ACCOUNT_NAME, this.getAccountName());
		values.put(NotifryDatabaseAdapter.KEY_ENABLED, this.getEnabled() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_SERVER_REGISTRATION_ID, this.getServerRegistrationId());
		values.put(NotifryDatabaseAdapter.KEY_REQUIRES_SYNC, this.getRequiresSync() ? 1 : 0);
		
		return values;
	}

	@Override
	protected NotifryAccount inflate( Context context, Cursor cursor )
	{
		NotifryAccount account = new NotifryAccount();
		account = new NotifryAccount();
		account.setAccountName(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_ACCOUNT_NAME)));
		account.setId(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_ID)));
		account.setEnabled(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_ENABLED)) == 0 ? false : true);
		account.setServerRegistrationId(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SERVER_REGISTRATION_ID)));
		account.setRequiresSync(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_REQUIRES_SYNC)) == 0 ? false : true);
		
		if( account.getServerRegistrationId() == 0 )
		{
			account.setServerRegistrationId(null);
		}
		return account;
	}

	@Override
	protected String[] getProjection()
	{
		return NotifryDatabaseAdapter.ACCOUNT_PROJECTION;
	}
}
