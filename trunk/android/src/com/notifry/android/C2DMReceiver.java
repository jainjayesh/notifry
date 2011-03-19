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
import com.notifry.android.database.NotifryMessage;
import com.notifry.android.database.NotifrySource;
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

			// Determine if the message should be spoken.			
			SpeakDecision decision = SpeakDecision.shouldSpeak(context, message);

			if( decision.getShouldSpeak() )
			{
				// Start talking.
				Intent intentData = new Intent(getBaseContext(), SpeakService.class);
				intentData.putExtra("text", decision.getSpokenMessage());
				startService(intentData);
			}

			// TODO: Notify the user in other ways too?			
		}
		
		// TODO: Handle other types of messages.
	}

	public void onError( Context context, String errorId )
	{
		Log.e("Notifry", "Error: " + errorId);
	}
}
