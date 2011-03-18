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

import org.json.JSONException;

import com.google.android.c2dm.C2DMessaging;
import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.accounts.AccountManager;
import android.app.ListActivity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAccount extends ListActivity
{
	private static final String TAG = "Notifry";
	private final ChooseAccount thisActivity = this;

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

		// Set the layout, and allow text filtering.
		setContentView(R.layout.screen_accounts);
		getListView().setTextFilterEnabled(true);
	}

	public void onResume()
	{
		super.onResume();

		// When coming back, refresh our list of accounts.
		refreshView();
	}
	
	/**
	 * Refresh the list of accounts viewed by this activity.
	 */
	public void refreshView()
	{
		// Refresh our list of accounts.
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		ArrayList<NotifryAccount> accounts = database.listAccounts();
		database.close();

		this.setListAdapter(new AccountArrayAdapter(this, this, R.layout.account_list_row, accounts));		
	}

	/**
	 * Handler for when you click an account name.
	 * @param account
	 */
	public void clickAccountName( NotifryAccount account )
	{
		Toast.makeText(this, account.getAccountName(), Toast.LENGTH_SHORT).show();
	}

	/**
	 * Handler for when you check or uncheck an account, which fires off a request
	 * to the server.
	 * @param account
	 * @param state
	 */
	public void checkedAccount( NotifryAccount account, boolean state )
	{
		// Refresh the account object. In case it's changed.
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		NotifryAccount refreshedAccount = database.getAccountById(account.getId());
		database.close();

		// Register or de-register the device with the server.
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

		// Add some metadata to the request so we know how to deal with it
		// afterwards.
		request.addMeta("account", refreshedAccount);
		
		String statusMessage = "Busy...";
		
		if( state )
		{
			// Enable the account.
			request.addMeta("operation", "register");
			request.add("operation", "add");
			statusMessage = getString(R.string.registering_with_server);
		}
		else
		{
			// Disable the account.
			request.addMeta("operation", "deregister");
			request.add("operation", "remove");
			statusMessage = getString(R.string.deregistering_with_server);
		}

		// For debugging, dump the request data.
		//request.dumpRequest();
		
		// Where to come back when we're done.
		request.setHandler(handler);

		// Start a thread to make the request.
		request.startInThread(this, statusMessage, refreshedAccount.getAccountName());
	}

	/**
	 * Private handler class that is the callback for when the external requests are complete.
	 */
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage( Message msg )
		{
			// Fetch out the response.
			BackendResponse response = (BackendResponse) msg.obj;

			// Was it successful?
			if( response.isError() )
			{
				// No, not successful.
				Toast.makeText(thisActivity, response.getError() + " - Please try again.", Toast.LENGTH_LONG);
			}
			else
			{
				try
				{
					// Fetch out metadata.
					BackendRequest request = response.getRequest();
					NotifryAccount account = (NotifryAccount) request.getMeta("account");
					String operation = (String) request.getMeta("operation");

					// Determine our operation.
					if( operation.equals("register") )
					{
						// We were registering the account.
						// The server would have given us a registration ID.
						account.setServerRegistrationId(Long.parseLong(response.getJSON().getJSONObject("device").getString("id")));
						
						// Enable the account.
						account.setEnabled(true);
						
						// Open the database and save it.
						NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(thisActivity);
						database.open();
						database.saveAccount(account);
						database.close();
						
						refreshView();
						
						Toast.makeText(thisActivity, String.format(getString(R.string.registering_with_server_complete, account.getAccountName())), Toast.LENGTH_SHORT).show();
					}
					else if( operation.equals("deregister") )
					{
						// We've deregistered the account.
						account.setServerRegistrationId(null);
						account.setEnabled(false);
						
						// Open the database and save it.
						NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(thisActivity);
						database.open();
						database.saveAccount(account);
						database.close();
						
						refreshView();
						
						Toast.makeText(thisActivity, String.format(getString(R.string.deregistering_with_server_complete, account.getAccountName())), Toast.LENGTH_SHORT).show();
					}
				}
				catch( JSONException e )
				{
					// The response doesn't look like we expected.
					Log.d(TAG, "Invalid response from server: " + e.getMessage());
					Toast.makeText(thisActivity, "Invalid response from the server.", Toast.LENGTH_LONG);
					refreshView();
				}
			}
		}
	};

	/**
	 * An array adapter to put accounts into the list view.
	 * @author daniel
	 */
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

					// This doesn't seem memory friendly, but we'll get away
					// with it because
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

					// This doesn't seem memory friendly, but we'll get away
					// with it because
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
