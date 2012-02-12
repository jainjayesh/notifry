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

import java.util.Date;

import com.google.android.c2dm.C2DMessaging;
import com.notifry.android.database.NotifryMessage;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Notifry extends Activity
{	
	public final static String UPDATE_INTENT = "com.notifry.android.UpdateUI";
	
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
		
		// Register to get update actions from other threads.
		registerReceiver(healthCheckReciever, new IntentFilter(UPDATE_INTENT));

		// Register for C2DM. We'll report this to the server later.
		final String registrationId = C2DMessaging.getRegistrationId(this);
		if( registrationId != null && !"".equals(registrationId) )
		{
			Log.i("Notifry", "Already registered. registrationId is " + registrationId);
		}
		else
		{
			Log.i("Notifry", "No existing registrationId. Registering.");
			C2DMessaging.register(this, "notifry@gmail.com");
		}
		
		// Clean out old messages.
		// TODO: Do this somewhere else too.
		Date olderThan = new Date();
		olderThan.setTime(olderThan.getTime() - 86400 * 1000);
		NotifryMessage.FACTORY.deleteOlderThan(this, olderThan);
	}

	public void onResume()
	{
		super.onResume();

		// Change the master enable/disable button based on the settings.
		// This is done in onResume() so it's correct even if you go to the
		// settings and come back.
		this.changeEnabledLabelFor(findViewById(R.id.home_disableAll));
		
		// Also, do a health check and post the results.
		try
		{
			this.doHealthCheck();
		}
		catch( NameNotFoundException e )
		{
			Log.d("Notifry", "Can't find own installed package...");
		}
	}
	
	public void doHealthCheck() throws NameNotFoundException
	{
		// Peform the health check.
		HealthCheck check = HealthCheck.performHealthcheck(this);
		
		TextView healthCheckArea = (TextView) findViewById(R.id.home_healthCheck);
		
		// Format the check text.
		StringBuilder allText = new StringBuilder();
		for( String error: check.getErrors() )
		{
			allText.append("- ");
			allText.append(error);
			allText.append('\n');
		}
		for( String error: check.getWarnings() )
		{
			allText.append("- ");
			allText.append(error);
			allText.append('\n');
		}
		
		if( allText.length() == 0 )
		{
			allText.append('\n');
			allText.append('v');
			allText.append(this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
			healthCheckArea.setText(getString(R.string.health_check_all_ok) + allText.toString());
		}
		else
		{
			healthCheckArea.setText(allText.toString().trim());
		}
		
		// Enable the accounts button once we have an ID.
		final String registrationId = C2DMessaging.getRegistrationId(this);
		if( registrationId != null && !"".equals(registrationId) )
		{
			Button accountsButton = (Button) findViewById(R.id.home_accounts);
			accountsButton.setEnabled(true);
		}		
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
	 * @throws NameNotFoundException 
	 */
	public void disableEnableNotifications( View view ) throws NameNotFoundException
	{
		// Enable or disable the master enable flag, updating the button as
		// appropriate.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		editor.putBoolean(getString(R.string.masterEnable), !settings.getBoolean(getString(R.string.masterEnable), true));
		editor.commit();

		this.changeEnabledLabelFor(view);
		
		this.doHealthCheck();
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
	 * 
	 * @param view
	 */
	public void launchSettings( View view )
	{
		Intent intent = new Intent(getBaseContext(), Settings.class);
		startActivity(intent);
	}

	/**
	 * Onclick handler to launch the recent messages dialog.
	 * 
	 * @param view
	 */
	public void launchRecentMessages( View view )
	{
		Intent intent = new Intent(getBaseContext(), MessageList.class);
		startActivity(intent);
	}

	/**
	 * Onclick handler to launch the account chooser dialog.
	 * 
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
	
	private final BroadcastReceiver healthCheckReciever = new BroadcastReceiver()
	{
		@Override
		public void onReceive( Context context, Intent intent )
		{
			Log.d("Notifry", "Re-performing health check.");
			try
			{
				doHealthCheck();
			}
			catch( NameNotFoundException e )
			{
				Log.d("Notifry", "Can't find own package information...");
			}
		}
	};	
}