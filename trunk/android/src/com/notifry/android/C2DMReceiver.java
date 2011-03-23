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

import com.google.android.c2dm.C2DMBaseReceiver;

import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifryMessage;

import android.content.Context;
import android.content.Intent;
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

		// Dispatch this to the updater service.
		Intent intentData = new Intent(getBaseContext(), UpdaterService.class);
		intentData.putExtra("type", "registration");
		intentData.putExtra("registration", registration);
		startService(intentData);
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
			
			Log.d("Notifry", "We've been notifried! " + message.getMessage());
			
			// Persist this message to the database.
			message.save(context);
			
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
			NotifryAccount account = NotifryAccount.FACTORY.getByServerId(context, serverAccountId);
			account.setRequiresSync(true);
			account.save(context);
			
			Log.d(TAG, "Server just asked us to refresh sources list - usually due to deletion.");
		}
		else if( type.equals("sourcechange") )
		{
			// Server says that a source has been created or updated.
			// We should pull a copy of it locally.
			Long serverSourceId = Long.parseLong(extras.getString("id"));
			Long serverDeviceId = Long.parseLong(extras.getString("device_id"));
			
			Intent intentData = new Intent(getBaseContext(), UpdaterService.class);
			intentData.putExtra("type", "sourcechange");
			intentData.putExtra("sourceId", serverSourceId);
			intentData.putExtra("deviceId", serverDeviceId);
			startService(intentData);
			
			Log.d(TAG, "Server just asked us to update/create server source ID " + serverSourceId + " for server account ID " + serverDeviceId);
		}
		else if( type.equals("devicedelete") )
		{
			// Server says we've been deregistered. We should now clear our registration.
			Long deviceId = Long.parseLong(extras.getString("device_id"));
			NotifryAccount account = NotifryAccount.FACTORY.getByServerId(context, deviceId);
			
			// Check if it's NULL - it's possible we have a desync!
			if( account != null )
			{
				// Disable it, and clear the registration ID.
				account.setEnabled(false);
				account.setServerRegistrationId(null);
				account.setRequiresSync(true);
				
				// Save it back to the database.
				account.save(context);
			}
			
			Log.d(TAG, "Server just asked us to deregister! And should be done now.");
		}
	}

	public void onError( Context context, String errorId )
	{
		// TODO: Handle this better.
		Log.e("Notifry", "Error: " + errorId);
	}
}
