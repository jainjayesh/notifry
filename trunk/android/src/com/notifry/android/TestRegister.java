package com.notifry.android;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.android.c2dm.C2DMessaging;
import com.notifry.android.remote.BackendClient;
import com.notifry.android.remote.BackendResponse;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class TestRegister extends Activity
{
	private static final String TAG = "Notifry";

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		
		// Just put something on the screen.
		setContentView(R.layout.test_register_screen);
		
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = settings.edit();
		
		Intent sourceIntent = getIntent();
		Account selectedAccount = (Account) sourceIntent.getExtras().get("account");
		
		editor.putString("test_account_name", selectedAccount.name);
		editor.commit();
		final Context closure = this;
		
		// Register for C2DM.
		final String registrationId = C2DMessaging.getRegistrationId(this);
		if( registrationId != null && !"".equals(registrationId) )
		{
			Log.i("Notifry", "Already registered. registrationId is " + registrationId);
			// C2DMessaging.unregister(this);
			
			Log.i("Notifry", "Sending to backend...");
			Thread background = new Thread() {
				public void run()
				{
					// Send the key to the server.
					BackendClient client = new BackendClient(closure, settings.getString("test_account_name", ""));
					
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair("devicekey", registrationId));
					params.add(new BasicNameValuePair("devicetype", "android"));
					params.add(new BasicNameValuePair("deviceversion", "0.1"));
					
					BackendResponse result;
					try
					{
						Log.i("Notifry", "Beginning request...");
						result = client.request("/registration", params);
						// Was it successful?
						if( result.isError() )
						{
							Log.e(TAG, "Error: " + result.getError());
						}
						else
						{
							Log.e(TAG, "Success! Server returned: " + result.getJSON().toString());
						}
					}
					catch( Exception e )
					{
						Log.e(TAG, "Generic exception: " + e.getMessage() + " of type " + e.getClass().toString());
					}
				}
			};
			background.start();
		}
		else
		{
			Log.i("Notifry", "No existing registrationId. Registering..");
			C2DMessaging.register(this, "notifry@gmail.com");
		}
	}
}
