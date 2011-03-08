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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class C2DMReceiver extends C2DMBaseReceiver
{
	public C2DMReceiver()
	{
		super("notifry@gmail.com");
	}

	public void onRegistrered( Context context, String registration )
	{
		Log.i("Notifry", "registered and got key: " + registration);
		// Send the key to the server.
	}

	protected void onMessage( Context context, Intent intent )
	{
		Bundle extras = intent.getExtras();

		// Fetch the message out into a PushMessage object.
		PushMessage message = new PushMessage();

		message.setMessage(extras.getString("message"));
		message.setTitle(extras.getString("title"));
		message.setUrl(extras.getString("url"));

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
