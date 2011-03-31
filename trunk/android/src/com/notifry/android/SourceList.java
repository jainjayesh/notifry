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

package com.notifry.android;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifrySource;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SourceList extends ListActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener
{
	public final static int ADD_SOURCE = 1;
	public final static int REFRESH_SERVER = 2;
	private static final String TAG = "Notifry";
	private final SourceList thisActivity = this;
	private NotifryAccount account = null;

	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		// Set the layout, and allow text filtering.
		setContentView(R.layout.screen_sources);
		getListView().setTextFilterEnabled(true);		
	}
	
	public void onConfigurationChanged( Configuration newConfig )
	{
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.screen_sources);
	}

	public void onResume()
	{
		super.onResume();

		// When coming back, refresh our list of accounts.
		refreshView();
		
		// And, are we out of sync?
		if( this.getAccount().getRequiresSync() )
		{
			this.syncWithServer();
		}
	}
	
	/**
	 * Sync the list with the server.
	 * This half starts the request to the backend.
	 */
	public void syncWithServer()
	{
		// Sync the list off the server.
		BackendRequest request = new BackendRequest("/sources/list");

		// Indicate what we're doing.
		request.addMeta("operation", "list");

		// For debugging, dump the request data.
		request.dumpRequest();
		
		// Where to come back when we're done.
		request.setHandler(handler);

		// Start a thread to make the request.
		// This will just update our view when ready.
		request.startInThread(this, getString(R.string.loading_sources_from_server), this.getAccount().getAccountName());		
	}

	/**
	 * Fetch the account that this source list is for.
	 * 
	 * @return
	 */
	public NotifryAccount getAccount()
	{
		if( this.account == null )
		{
			// Get the account from the intent.
			// We store it in a private variable to save us having to query the
			// DB each time.
			Intent sourceIntent = getIntent();
			this.account = NotifryAccount.FACTORY.getByAccountName(this, sourceIntent.getStringExtra("account")); 
		}

		return this.account;
	}

	/**
	 * Refresh the list of sources viewed by this activity.
	 */
	public void refreshView()
	{
		// Refresh our list of sources.
		ArrayList<NotifrySource> sources = NotifrySource.FACTORY.listAll(this, this.getAccount().getAccountName()); 

		this.setListAdapter(new SourceArrayAdapter(this, this, R.layout.source_list_row, sources));
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, ADD_SOURCE, 0, R.string.create_source).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, REFRESH_SERVER, 0, R.string.refresh_sources_server).setIcon(android.R.drawable.ic_menu_rotate);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case ADD_SOURCE:
				askForSourceName();
				return true;
			case REFRESH_SERVER:
				syncWithServer();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Helper function to show a dialog to ask for a source name.
	 */
	private void askForSourceName()
	{
		final EditText input = new EditText(this);

		new AlertDialog.Builder(this).
				setTitle(getString(R.string.create_source)).
				setMessage(getString(R.string.create_source_message)).
				setView(input).
				setPositiveButton(
						getString(R.string.create),
						new DialogInterface.OnClickListener()
						{
							public void onClick( DialogInterface dialog, int whichButton )
							{
								Editable value = input.getText();
								if( value.length() > 0 )
								{
									// Fire it off to the create source function.
									createSource(value.toString());
								}
							}
						}).
				setNegativeButton(
						getString(R.string.cancel),
						new DialogInterface.OnClickListener()
						{
							public void onClick( DialogInterface dialog, int whichButton )
							{
								// No need to take any action.
							}
						}).
				show();
	}
	
	/**
	 * Helper function to create a source.
	 * @param title
	 */
	public void createSource( String title )
	{
		// We need to send this request to the backend, and then it will set up everything we need.
		BackendRequest request = new BackendRequest("/sources/create");
		request.add("title", title);
		request.add("enabled", "on");
		request.add("device", this.getAccount().getServerRegistrationId().toString());

		// Indicate what we're doing.
		request.addMeta("operation", "create");

		// For debugging, dump the request data.
		//request.dumpRequest();
		
		// Where to come back when we're done.
		request.setHandler(handler);

		// Start a thread to make the request.
		request.startInThread(this, getString(R.string.create_source_server_waiting), this.getAccount().getAccountName());
	}

	/**
	 * Handler for when you click an source.
	 * 
	 * @param account
	 */
	public void clickSource( NotifrySource source )
	{
		// Launch the source editor.
		Intent intent = new Intent(getBaseContext(), SourceEditor.class);
		intent.putExtra("sourceId", source.getId());
		startActivity(intent);
	}

	public void onClick( View clickedView )
	{
		Long id = (Long) clickedView.getTag();
		NotifrySource source = NotifrySource.FACTORY.get(this, id);
		
		clickSource(source);
	}	
	
	public void onCheckedChanged( CompoundButton buttonView, boolean isChecked )
	{
		Long id = (Long) buttonView.getTag();
		NotifrySource source = NotifrySource.FACTORY.get(this, id);
		
		checkedSource(source, isChecked);
	}	
	
	/**
	 * Handler for when you change a source's enabled status.
	 * @param source
	 * @param state
	 */
	public void checkedSource( NotifrySource source, boolean state )
	{
		// All we're doing is changing the LOCAL enable flag.
		// Refresh the source.
		NotifrySource refreshedSource = NotifrySource.FACTORY.get(this, source.getId()); 
		refreshedSource.setLocalEnabled(state);
		refreshedSource.save(this);
		
		// And refresh our view.
		refreshView();
	}

	/**
	 * Private handler class that is the callback for when the external requests
	 * are complete.
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
					String operation = (String) request.getMeta("operation");

					// Determine our operation.
					if( operation.equals("create") )
					{
						// We were creating a new source.
						// The server would have given us a complete source object.
						NotifrySource source = new NotifrySource();
						source.fromJSONObject(response.getJSON().getJSONObject("source"));
						source.setAccountName(getAccount().getAccountName());
						source.setLocalEnabled(true); // Enabled by default.

						// Open the database and save it.
						source.save(thisActivity);

						refreshView();

						Toast.makeText(thisActivity, getString(R.string.create_source_server_complete), Toast.LENGTH_SHORT).show();
					}
					else if( operation.equals("list") )
					{
						// We just got a list from the server. Sync it up!
						JSONArray serverList = response.getJSON().getJSONArray("sources");
						NotifrySource.FACTORY.syncFromJSONArray(thisActivity, serverList, thisActivity.getAccount().getAccountName());
						
						// Mark the account as synced.
						NotifryAccount account = NotifryAccount.FACTORY.getByAccountName(thisActivity, thisActivity.getAccount().getAccountName()); 
						account.setRequiresSync(false);
						account.save(thisActivity);

						// Force the object to be refreshed next time.
						thisActivity.account = null;
						
						// And refresh.
						refreshView();
						
						Toast.makeText(thisActivity, getString(R.string.refresh_sources_success), Toast.LENGTH_SHORT).show();
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

	/**
	 * An array adapter to put sources into the list view.
	 * 
	 * @author daniel
	 */
	private class SourceArrayAdapter extends ArrayAdapter<NotifrySource>
	{
		final private SourceList parentActivity;
		private ArrayList<NotifrySource> sources;

		public SourceArrayAdapter( SourceList parentActivity, Context context, int textViewResourceId, ArrayList<NotifrySource> objects )
		{
			super(context, textViewResourceId, objects);
			this.parentActivity = parentActivity;
			this.sources = objects;
		}

		public View getView( int position, View convertView, ViewGroup parent )
		{
			// Inflate a view if required.
			if( convertView == null )
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.source_list_row, null);
			}

			// Find the account.
			final NotifrySource source = this.sources.get(position);

			// And set the values on our row.
			if( source != null )
			{
				TextView title = (TextView) convertView.findViewById(R.id.source_row_source_name);
				TextView serverEnabled = (TextView) convertView.findViewById(R.id.source_row_server_enabled);
				CheckBox enabled = (CheckBox) convertView.findViewById(R.id.source_row_local_enabled);

				
				if( title != null )
				{
					title.setText(source.getTitle());
					title.setClickable(true);
					title.setTag(source.getId());

					title.setOnClickListener(parentActivity);
				}
				if( serverEnabled != null )
				{
					serverEnabled.setClickable(true);
					serverEnabled.setTag(source.getId());
					if( source.getServerEnabled() == false )
					{
						serverEnabled.setText(getString(R.string.source_disabled_on_server));
						serverEnabled.setVisibility(View.VISIBLE);
					}
					else
					{
						serverEnabled.setVisibility(View.GONE);
					}
					serverEnabled.setOnClickListener(parentActivity);
				}
				if( enabled != null )
				{
					enabled.setChecked(source.getLocalEnabled());
					enabled.setTag(source.getId());

					enabled.setOnCheckedChangeListener(parentActivity);
				}
			}

			return convertView;
		}
	}
}