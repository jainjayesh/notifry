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

import com.google.android.c2dm.C2DMessaging;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Notifry extends Activity
{
	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		// Prepare the view.
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_home);

		// Figure out if we have the TTS installed.
		Intent checkIntent = new Intent();
		checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		startActivityForResult(checkIntent, 0x1010);

        // Register for C2DM. We'll report this to the server later.
        final String registrationId = C2DMessaging.getRegistrationId(this);
        if( registrationId != null && !"".equals(registrationId) )
        {
                Log.i("Notifry", "Already registered. registrationId is " + registrationId);
        }
        else
        {
                Log.i("Notifry", "No existing registrationId. Registering..");
                C2DMessaging.register(this, "notifry@gmail.com");
        }
	}
	
	public void onResume()
	{
		super.onResume();
		
		// Change the master enable/disable button based on the settings.
		// This is done in onResume() so it's correct even if you go to the settings and come back.
		this.changeEnabledLabelFor(findViewById(R.id.home_disableAll));		
	}

	/**
	 * Onclick handler to stop reading now.
	 * 
	 * @param view
	 */
	public void stopReadingNow( View view )
	{
		// Inform our service to stop reading now.
		Intent intentData = new Intent(getBaseContext(), SpeakService.class);
		intentData.putExtra("stopNow", true);
		startService(intentData);
	}

	/**
	 * Onclick handler to toggle the master enable.
	 * 
	 * @param view
	 */
	public void disableEnableNotifications( View view )
	{
		// Enable or disable the master enable flag, updating the button as
		// appropriate.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		editor.putBoolean(getString(R.string.masterEnable), !settings.getBoolean(getString(R.string.masterEnable), true));
		editor.commit();

		this.changeEnabledLabelFor(view);
	}

	/**
	 * Based on the settings, change the text on the given view to match.
	 * 
	 * @param view
	 */
	public void changeEnabledLabelFor( View view )
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if( settings.getBoolean(getString(R.string.masterEnable), true) )
		{
			// It is enabled. Give the button the enabled text.
			Button button = (Button) view;
			button.setText(R.string.disable_all_notifications);
		}
		else
		{
			Button button = (Button) view;
			button.setText(R.string.enable_all_notifications);
		}
	}

	/**
	 * Onclick handler to launch the settings dialog.
	 * @param view
	 */
	public void launchSettings( View view )
	{
		Intent intent = new Intent(getBaseContext(), Settings.class);
		startActivity(intent);
	}

	public void launchRecentMessages( View view )
	{

	}

	/**
	 * Onclick handler to launch the account chooser dialog.
	 * @param view
	 */
	public void launchAccounts( View view )
	{
		Intent intent = new Intent(getBaseContext(), ChooseAccount.class);
		startActivity(intent);
	}

	/**
	 * Callback function for checking if the Text to Speech is installed. If
	 * not, it will redirect the user to download the text data.
	 */
	protected void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if( requestCode == 0x1010 )
		{
			if( resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS )
			{
				// All systems are go.
				Log.d("Notifry", "All systems are go.");
			}
			else
			{
				// TTS data missing. Go get it.
				Toast.makeText(getApplicationContext(), R.string.need_tts_data_installed, Toast.LENGTH_LONG).show();
				Log.d("Notifry", "Redirecting to get data.");
				Intent installIntent = new Intent();
				installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
				startActivity(installIntent);
			}
		}
	}
}