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

import com.google.android.c2dm.C2DMBaseReceiver;

import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.PushMessage;
import com.notifry.android.remote.BackendRequest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
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
				// Register or de-register the device with the server.
				BackendRequest request = new BackendRequest("/registration");
				request.add("devicekey", registration);
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
				if( account.getServerRegistrationId() != null )
				{
					request.add("id", account.getServerRegistrationId().toString());
				}

				request.add("operation", "add");

				// For debugging, dump the request data.
				//request.dumpRequest();
				
				// Start a thread to make the request.
				request.startInThread(this, null, account.getAccountName());	
			}
		}
	}

	protected void onMessage( Context context, Intent intent )
	{
		Bundle extras = intent.getExtras();

		// Fetch the message out into a PushMessage object.
		PushMessage message = new PushMessage();

		message.setMessage(extras.getString("message"));
		message.setTitle(extras.getString("title"));
		message.setUrl(extras.getString("url"));
		
		Log.d("Notifry", "Got message! " + message.getMessage());

		// Determine if the message should be spoken.
		if( SpeakDecision.shouldSpeak(context, message) )
		{
			// Start talking.
			Intent intentData = new Intent(getBaseContext(), SpeakService.class);
			intentData.putExtra("text", message.getMessage());
			startService(intentData);
		}

		// Store this message locally.
		// Notify the user in other ways too?
	}

	public void onError( Context context, String errorId )
	{
		Log.e("Notifry", "Error: " + errorId);
	}
}
