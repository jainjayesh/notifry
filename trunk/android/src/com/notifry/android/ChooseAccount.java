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
import java.util.HashMap;

import org.json.JSONException;

import com.google.android.c2dm.C2DMessaging;
import com.notifry.android.database.NotifryAccount;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAccount extends ListActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener 
{
	private static final int REFRESH_IDS = 1;
	private static final String TAG = "Notifry";
	private final ChooseAccount thisActivity = this;

	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		AccountManager accountManager = AccountManager.get(getApplicationContext());

		// Sync our database. Only on create.
		NotifryAccount.FACTORY.syncAccountList(this, accountManager);

		// Set the layout, and allow text filtering.
		setContentView(R.layout.screen_accounts);
		getListView().setTextFilterEnabled(true);
	}

	public void onResume()
	{
		super.onResume();

		// When coming back, refresh our list of accounts.
		refreshView();
		
		// Show the help.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if( settings.getBoolean("chooseAccountFirstTime", true) )
		{
			// Show first time help.
			new AlertDialog.Builder(this).
				setTitle(getString(R.string.accounts_and_sources)).
				setMessage(getString(R.string.accounts_help)).
				setPositiveButton(getString(R.string.ok), null).
				show();
			
			// And don't show again.
			Editor editor = settings.edit();
			editor.putBoolean("chooseAccountFirstTime", false);
			editor.commit();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, REFRESH_IDS, 0, R.string.refresh_ids).setIcon(android.R.drawable.ic_menu_rotate);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case REFRESH_IDS:
				// Dispatch this to the updater service.
				Intent intentData = new Intent(getBaseContext(), UpdaterService.class);
				intentData.putExtra("type", "registration");
				intentData.putExtra("registration", C2DMessaging.getRegistrationId(this));
				startService(intentData);
				
				Toast.makeText(thisActivity, getString(R.string.background_refreshing), Toast.LENGTH_SHORT).show();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}	
	
	/**
	 * Refresh the list of accounts viewed by this activity.
	 */
	public void refreshView()
	{
		// Refresh our list of accounts.
		ArrayList<NotifryAccount> accounts = NotifryAccount.FACTORY.listAll(this);

		this.setListAdapter(new AccountArrayAdapter(this, this, R.layout.account_list_row, accounts));		
	}

	/**
	 * Handler for when you click an account name.
	 * @param account
	 */
	public void clickAccountName( NotifryAccount account )
	{
		// If enabled, launch the sources list for this account.
		if( account.getEnabled() )
		{
			Intent intent = new Intent(getBaseContext(), SourceList.class);
			intent.putExtra("account", account.getAccountName());
			startActivity(intent);
		}
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
		NotifryAccount refreshedAccount = NotifryAccount.FACTORY.get(this, account.getId()); 

		// Add some metadata to the request so we know how to deal with it
		// afterwards.
		HashMap<String, Object> metadata = new HashMap<String, Object>();
		metadata.put("account", refreshedAccount);

		String statusMessage = "Busy...";
		
		if( state )
		{
			// Enable the account.
			metadata.put("operation", "register");
			metadata.put("registration", C2DMessaging.getRegistrationId(this));
			statusMessage = getString(R.string.registering_with_server);
		}
		else
		{
			// Disable the account.
			metadata.put("operation", "deregister");
			statusMessage = getString(R.string.deregistering_with_server);
		}

		// And send off the request.
		refreshedAccount.registerWithBackend(this, C2DMessaging.getRegistrationId(this), state, statusMessage, handler, metadata);
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
				Toast.makeText(thisActivity, response.getError() + " - Please try again.", Toast.LENGTH_LONG).show();
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
						
						// We need a refresh.
						account.setRequiresSync(true);
						
						// Store the registration ID.
						account.setLastC2DMId((String) request.getMeta("registration")); 
						
						// Persist it.
						account.save(thisActivity);
						
						refreshView();
						
						Toast.makeText(thisActivity, String.format(getString(R.string.registering_with_server_complete, account.getAccountName())), Toast.LENGTH_SHORT).show();
					}
					else if( operation.equals("deregister") )
					{
						// We've deregistered the account.
						account.setServerRegistrationId(null);
						account.setEnabled(false);
						account.setLastC2DMId(null);
						
						// Persist it to the database.
						account.save(thisActivity);
						
						refreshView();
						
						Toast.makeText(thisActivity, String.format(getString(R.string.deregistering_with_server_complete, account.getAccountName())), Toast.LENGTH_SHORT).show();
					}
				}
				catch( JSONException e )
				{
					// The response doesn't look like we expected.
					Log.d(TAG, "Invalid response from server: " + e.getMessage());
					Toast.makeText(thisActivity, "Invalid response from the server.", Toast.LENGTH_LONG).show();
					refreshView();
				}
			}
		}
	};
	
	@Override
	public void onCheckedChanged( CompoundButton checkbox, boolean checked )
	{
		Long id = (Long) checkbox.getTag();
		NotifryAccount account = NotifryAccount.FACTORY.get(this, id);
		
		this.checkedAccount(account, checked);
	}

	@Override
	public void onClick( View textview )
	{
		Long id = (Long) textview.getTag();
		NotifryAccount account = NotifryAccount.FACTORY.get(this, id);
		
		this.clickAccountName(account);
	}	

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
			
			convertView.setClickable(true);
			convertView.setLongClickable(true);

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
					title.setTag(account.getId());
					title.setClickable(true);
					title.setOnClickListener(parentActivity);
				}
				if( enabled != null )
				{
					enabled.setChecked(account.getEnabled());
					enabled.setTag(account.getId());
					enabled.setOnCheckedChangeListener(parentActivity);
				}
			}

			return convertView;
		}
	}
}
