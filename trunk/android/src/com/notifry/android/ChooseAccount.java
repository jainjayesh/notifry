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
 * The contents of this are heavily based on this blog post:
 * http://blog.notdot.net/2010/05/Authenticating-against-App-Engine-from-an-Android-app
 * Thanks dude for your awesome writeup!
 */

package com.notifry.android;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.android.c2dm.C2DMessaging;
import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.remote.BackendRequest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAccount extends ListActivity
{
	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		AccountManager accountManager = AccountManager.get(getApplicationContext());

		// Sync our database. Only on create.
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		database.syncAccountList(accountManager);
		database.close();
		
		setContentView(R.layout.screen_accounts);
		getListView().setTextFilterEnabled(true);
	}

	public void onResume()
	{
		super.onResume();
		
		// Refresh our list of accounts.
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		ArrayList<NotifryAccount> accounts = database.listAccounts();
		database.close();
		
		this.setListAdapter(new AccountArrayAdapter(this, this, R.layout.account_list_row, accounts));
	}
	
	public void clickAccountName( NotifryAccount account )
	{
		Toast.makeText(this, account.getAccountName(), Toast.LENGTH_SHORT).show();
	}
	
	public void checkedAccount( NotifryAccount account, boolean state )
	{
		// Refresh the account object. In case it's changed.
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		NotifryAccount refreshedAccount = database.getAccountById(account.getId());
		database.close();
		
		if( state )
		{
			// Log into the server and populate the sources list.
			BackendRequest request = new BackendRequest("/registration");
			request.add("devicekey", C2DMessaging.getRegistrationId(this));
			request.add("devicetype", "android");
			try
			{
				request.add("deviceversion", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
			}
			catch( NameNotFoundException e )
			{
				request.add("deviceversion", "Unknown");
			}
			
			// If already registered, update the same entry.
			if( refreshedAccount.getServerRegistrationId() != null )
			{
				request.add("id", refreshedAccount.getServerRegistrationId().toString());
			}
			
			request.dumpRequest();
			
			// Start a thread to make the request. Block until it's done.
			request.startInThread(this, null /*"Registering with server..."*/, refreshedAccount.getAccountName());
			
			// Now update the local database if it succeeded. TODO: Later.
		}
		else
		{
			// Deregister this account from the server. TODO: the server doesn't let you do this at the moment.
		}
	}

	private class AccountArrayAdapter extends ArrayAdapter<NotifryAccount>
	{
		final private ChooseAccount parentActivity;
		private ArrayList<NotifryAccount> accounts;

		public AccountArrayAdapter( ChooseAccount parentActivity, Context context, int textViewResourceId, ArrayList<NotifryAccount> objects )
		{
			super(context, textViewResourceId, objects);
			this.parentActivity = parentActivity;
			this.accounts = objects;
		}

		public View getView( int position, View convertView, ViewGroup parent )
		{
			// Inflate a view if required.
			if( convertView == null )
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.account_list_row, null);
			}

			// Find the account.
			final NotifryAccount account = this.accounts.get(position);

			// And set the values on our row.
			if( account != null )
			{
				TextView title = (TextView) convertView.findViewById(R.id.account_row_account_name);
				CheckBox enabled = (CheckBox) convertView.findViewById(R.id.account_row_account_enabled);
				if( title != null )
				{
					title.setText(account.getAccountName());
					title.setClickable(true);
					
					// This doesn't seem memory friendly, but we'll get away with it because
					// there won't be many registered accounts.
					title.setOnClickListener(new View.OnClickListener()
					{
						public void onClick( View v )
						{
							parentActivity.clickAccountName(account);
						}
					});
				}
				if( enabled != null )
				{
					enabled.setChecked(account.getEnabled());
					
					// This doesn't seem memory friendly, but we'll get away with it because
					// there won't be many registered accounts.
					enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
					{						
						public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
						{
							parentActivity.checkedAccount(account, isChecked);
						}
					});
				}
			}

			return convertView;
		}
	}
}
