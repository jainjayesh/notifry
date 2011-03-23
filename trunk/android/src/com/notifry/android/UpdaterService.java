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
import java.util.HashMap;

import org.json.JSONException;

import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifrySource;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

public class UpdaterService extends Service
{
	private static final String TAG = "Notifry";
	private UpdaterService thisService = this;
	
	@Override
	public IBinder onBind( Intent arg0 )
	{
		return null;
	}
	
	public void onCreate()
	{
		super.onCreate();
	}

	public void onStart( Intent intent, int startId )
	{
		super.onStart(intent, startId);
		
		// We need to make some kind of backend request.
		String type = intent.getExtras().getString("type");
		
		if( type.equals("registration") )
		{
			// We want to update our registration key with the server.
			// Get a list of accounts. We need to send it to any enabled ones on the backend.
			ArrayList<NotifryAccount> accounts = NotifryAccount.FACTORY.listAll(this); 
			
			String newRegistration = intent.getExtras().getString("registration");

			for( NotifryAccount account: accounts )
			{
				if( account.getEnabled() )
				{
					HashMap<String, Object> metadata = new HashMap<String, Object>();
					metadata.put("account", account);
					metadata.put("operation", "register");
					account.registerWithBackend(this, newRegistration, true, null, handler, metadata);
				}
			}
		}
		else if( type.equals("sourcechange") )
		{
			// Somewhere, a source has changed or been added. We should pull down a local one.
			Long serverSourceId = intent.getLongExtra("sourceId", 0);
			Long serverDeviceId = intent.getLongExtra("deviceId", 0);
			
			BackendRequest request = new BackendRequest("/sources/get");
			request.add("id", serverSourceId.toString());
			request.addMeta("operation", "updatedSource");
			request.addMeta("context", this);
			request.addMeta("source_id", serverSourceId);
			request.addMeta("account_id", serverDeviceId);
		
			// Where to come back when we're done.
			request.setHandler(handler);
			
			NotifryAccount account = NotifryAccount.FACTORY.getByServerId(this, serverDeviceId); 

			// Start a thread to make the request.
			// But if there was no account to match that device, don't bother.
			if( account != null )
			{
				request.startInThread(this, null, account.getAccountName());
			}			
		}
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
				Log.e(TAG, "Error getting remote request: " + response.getError());
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
						
						Long accountId = (Long) request.getMeta("account_id");
						NotifryAccount account = NotifryAccount.FACTORY.getByServerId(thisService, accountId);
						
						Long sourceId = (Long) request.getMeta("source_id");
						
						// Try and get an existing source from our database.
						NotifrySource source = NotifrySource.FACTORY.getByServerId(thisService, sourceId);
						if( source == null )
						{
							// New object!
							source = new NotifrySource();
							source.setLocalEnabled(true); // Enabled by default.
						}
						
						// The server would have given us a complete source object.
						source.fromJSONObject(response.getJSON().getJSONObject("source"));
						source.setAccountName(account.getAccountName());

						source.save(thisService);
						
						Log.d(TAG, "Created/updated source based on server request: local " + source.getId() + " remote: " + sourceId);
					}
					else if( operation.equals("register") )
					{
						// Register complete. No action required here.
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
