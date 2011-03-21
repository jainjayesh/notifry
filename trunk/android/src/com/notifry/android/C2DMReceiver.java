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

import org.json.JSONException;

import com.google.android.c2dm.C2DMBaseReceiver;

import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifryMessage;
import com.notifry.android.database.NotifrySource;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver
{
	private static final String TAG = "Notifry";

	public C2DMReceiver()
	{
		super("notifry@gmail.com");
	}

	public void onRegistrered( Context context, String registration ) throws Exception
	{
		Log.i("Notifry", "registered and got key: " + registration);
		
		// Get a list of accounts. We need to send it to any enabled ones on the backend.
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		ArrayList<NotifryAccount> accounts = database.listAccounts();
		database.close();
		
		// TODO: This needs to have better error handling - it just blindly assumes it worked!
		for( NotifryAccount account: accounts )
		{
			if( account.getEnabled() )
			{
				account.registerWithBackend(context, registration, true, null, null, null);
			}
		}
	}

	protected void onMessage( Context context, Intent intent )
	{
		Bundle extras = intent.getExtras();
		
		// The server would have sent a message type.
		String type = extras.getString("type");
		
		if( type.equals("message") )
		{
			// Fetch the message out into a NotifryMessage object.
			NotifryMessage message = NotifryMessage.fromC2DM(context, extras);
			
			Log.d("Notifry", "Got message! " + message.getMessage());
			
			// Persist this message to the database.
			NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
			database.open();
			database.saveMessage(message);
			database.close();
			
			// Send a notification to the notification service, which will then
			// dispatch and handle everything else.
			Intent intentData = new Intent(getBaseContext(), NotificationService.class);
			intentData.putExtra("messageId", message.getId());
			startService(intentData);
		}
		else if( type.equals("refreshall") )
		{
			// Server says to refresh our list when we can. Typically means that
			// a source has been deleted. Make a note of it.
			Long serverAccountId = Long.parseLong(extras.getString("device_id"));
			NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
			database.open();
			NotifryAccount account = database.getAccountByServerId(serverAccountId);
			account.setRequiresSync(true);
			database.saveAccount(account);
			database.close();
			
			Log.d(TAG, "Server just asked us to refresh sources list - usually due to deletion.");
		}
		else if( type.equals("sourcechange") )
		{
			// Server says that a source has been created or updated.
			// We should pull a copy of it locally.
			Long serverSourceId = Long.parseLong(extras.getString("id"));
			Long serverDeviceId = Long.parseLong(extras.getString("device_id"));
			
			// TODO: Should this really be here?
			BackendRequest request = new BackendRequest("/sources/get");
			request.add("id", serverSourceId.toString());
			request.addMeta("operation", "updatedSource");
			request.addMeta("context", context);
			request.addMeta("source_id", serverSourceId);
			request.addMeta("account_id", serverDeviceId);
			
			Log.d(TAG, "Server just asked us to update/create server source ID " + serverSourceId + " for server account ID " + serverDeviceId);
		
			// Where to come back when we're done.
			request.setHandler(handler);
			
			NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
			database.open();
			NotifryAccount account = database.getAccountByServerId(serverDeviceId);
			database.close();

			// Start a thread to make the request.
			// But if there was no account to match that device, don't bother.
			if( account != null )
			{
				request.startInThread(this, null, account.getAccountName());
			}
			
		}
		else if( type.equals("devicedelete") )
		{
			// Server says we've been deregistered. We should now clear our registration.
			Long deviceId = Long.parseLong(extras.getString("device_id"));
			NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
			database.open();
			NotifryAccount account = database.getAccountByServerId(deviceId);
			
			// Disable it, and clear the registration ID.
			account.setEnabled(false);
			account.setServerRegistrationId(null);
			account.setRequiresSync(true);
			
			// Save it back to the database.
			database.saveAccount(account);
			database.close();
			
			Log.d(TAG, "Server just asked us to deregister! And should be done now.");
		}
		
		// TODO: Handle other types of messages.
	}

	public void onError( Context context, String errorId )
	{
		Log.e("Notifry", "Error: " + errorId);
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
			// TODO: I get the feeling this is the wrong place to do all this.
			BackendResponse response = (BackendResponse) msg.obj;
			Context context = (Context) response.getRequest().getMeta("context");

			// Was it successful?
			if( response.isError() )
			{
				// No, not successful.
				// Er... now what?
				Log.e(TAG, "Error getting remote request via C2DMReciever class - I told you this was a bad idea. " + response.getError());
			}
			else
			{
				try
				{
					// Fetch out metadata.
					BackendRequest request = response.getRequest();
					String operation = (String) request.getMeta("operation");

					// Determine our operation.
					if( operation.equals("updatedSource") )
					{
						// We were fetching a new or updated source from the server.
						// Open the database and save it.
						NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
						database.open();
						
						Long accountId = (Long) request.getMeta("account_id");
						NotifryAccount account = database.getAccountByServerId(accountId);
						
						Long sourceId = (Long) request.getMeta("source_id");
						
						// Try and get an existing source from our database.
						NotifrySource source = database.getSourceByServerId(sourceId);
						if( source == null )
						{
							// New object!
							source = new NotifrySource();
						}
						
						// The server would have given us a complete source object.
						source.fromJSONObject(response.getJSON().getJSONObject("source"));
						source.setAccountName(account.getAccountName());
						source.setLocalEnabled(true); // Enabled by default.

						database.saveSource(source);
						database.close();
						
						Log.d(TAG, "Created/updated source based on server request: local " + source.getId() + " remote: " + sourceId);
					}
				}
				catch( JSONException e )
				{
					// The response doesn't look like we expected.
					Log.d(TAG, "Invalid response from server: " + e.getMessage());
					// And now we've failed. Now what?
				}
			}
		}
	};	
}
