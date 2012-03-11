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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;

import com.actionbarsherlock.view.MenuItem;
import com.notifry.android.database.NotifrySource;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class SourceEditor extends PreferenceActivity
{
	private static final String TAG = "Notifry";
	private final SourceEditor thisActivity = this;
	private NotifrySource source = null;
	private ORMPreferencesMapper preferenceMapper;
	
	static List<String> togglePreferences = new ArrayList<String>();
	{
		togglePreferences.add("source_ringtone");
		togglePreferences.add("source_vibrate");
		togglePreferences.add("source_ledflash");
		togglePreferences.add("source_speakmessage");
		togglePreferences.add("source_customringtone");
	}
	
	// This is heavily inspired by ConnectBot's host editor:
	// https://github.com/kruton/connectbot/blob/master/src/org/connectbot/HostEditorActivity.java
	public class ORMPreferencesMapper implements SharedPreferences
	{
		protected List<OnSharedPreferenceChangeListener> listeners = new LinkedList<OnSharedPreferenceChangeListener>();

		public boolean contains( String key )
		{
			Map<String, ?> values = this.getAll();
			return values.containsKey(key);
		}

		public Editor edit()
		{
			return new Editor();
		}

		public Map<String, ?> getAll()
		{
			Map<String, Object> values = new HashMap<String, Object>();
			values.put("source_server_enable", thisActivity.source.getServerEnabled());
			values.put("source_local_enable", thisActivity.source.getLocalEnabled());
			values.put("source_ringtone", thisActivity.source.getRingtone());
			values.put("source_vibrate", thisActivity.source.getVibrate());
			values.put("source_ledflash", thisActivity.source.getLedFlash());
			values.put("source_speakmessage", thisActivity.source.getSpeakMessage());
			values.put("source_global", thisActivity.source.getUseGlobalNotification());
			
			values.put("source_customringtone", thisActivity.source.getCustomRingtone());
			values.put("source_title", thisActivity.source.getTitle());
			return values;
		}

		public boolean getBoolean( String key, boolean defValue )
		{
			// source_server_enable
			// source_local_enable
			// source_statusbar
			// source_ringtone
			// source_vibrate
			// source_ledflash
			// source_speakmessage
			if( key.equals("source_server_enable") )
			{
				return thisActivity.source.getServerEnabled();
			}
			else if( key.equals("source_local_enable") )
			{
				return thisActivity.source.getLocalEnabled();
			}
			else if( key.equals("source_ringtone") )
			{
				return thisActivity.source.getRingtone();
			}
			else if( key.equals("source_vibrate") )
			{
				return thisActivity.source.getVibrate();
			}
			else if( key.equals("source_ledflash") )
			{
				return thisActivity.source.getLedFlash();
			}
			else if( key.equals("source_speakmessage") )
			{
				return thisActivity.source.getSpeakMessage();
			}
			else if( key.equals("source_global") )
			{
				return thisActivity.source.getUseGlobalNotification();
			}

			return false;
		}

		public float getFloat( String key, float defValue )
		{
			// No floats available.
			return 0;
		}

		public int getInt( String key, int defValue )
		{
			// No int's available.
			return 0;
		}

		public long getLong( String key, long defValue )
		{
			// No longs available.
			return 0;
		}

		public String getString( String key, String defValue )
		{
			// source_customringtone
			// source_title
			if( key.equals("source_title") )
			{
				return thisActivity.source.getTitle();
			}
			else if( key.equals("source_customringtone") )
			{
				return thisActivity.source.getCustomRingtone();
			}

			return null;
		}

		public void registerOnSharedPreferenceChangeListener( OnSharedPreferenceChangeListener listener )
		{
			this.listeners.add(listener);
		}

		public void unregisterOnSharedPreferenceChangeListener( OnSharedPreferenceChangeListener listener )
		{
			this.listeners.remove(listener);
		}
		
		public class Editor implements SharedPreferences.Editor
		{

			public android.content.SharedPreferences.Editor clear()
			{
				// Not applicable for this object.
				return null;
			}

			public boolean commit()
			{
				thisActivity.source.save(thisActivity);
				return true;
			}

			public android.content.SharedPreferences.Editor putBoolean( String key, boolean value )
			{
				// NOTE: This is not transactionally safe as it modifies the parent's object.
				// source_server_enable
				// source_local_enable
				// source_statusbar
				// source_ringtone
				// source_vibrate
				// source_ledflash
				// source_speakmessage
				// source_global
				if( key.equals("source_server_enable") )
				{
					thisActivity.source.setServerEnabled(value);
				}
				else if( key.equals("source_local_enable") )
				{
					thisActivity.source.setLocalEnabled(value);
				}
				else if( key.equals("source_ringtone") )
				{
					thisActivity.source.setRingtone(value);
				}
				else if( key.equals("source_vibrate") )
				{
					thisActivity.source.setVibrate(value);
				}
				else if( key.equals("source_ledflash") )
				{
					thisActivity.source.setLedFlash(value);
				}
				else if( key.equals("source_speakmessage") )
				{
					thisActivity.source.setSpeakMessage(value);
				}
				else if( key.equals("source_global") )
				{
					thisActivity.source.setUseGlobalNotification(value);
				}

				return this;
			}

			public android.content.SharedPreferences.Editor putFloat( String key, float value )
			{
				// No floats to set.
				return this;
			}

			public android.content.SharedPreferences.Editor putInt( String key, int value )
			{
				// No ints to set.
				return this;
			}

			public android.content.SharedPreferences.Editor putLong( String key, long value )
			{
				// No longs to set.
				return this;
			}

			public android.content.SharedPreferences.Editor putString( String key, String value )
			{
				// NOTE: This is not transactionally safe as it modifies the parent's object.				
				// source_customringtone
				// source_title
				if( key.equals("source_title") )
				{
					thisActivity.source.setTitle(value);
				}
				else if( key.equals("source_customringtone") )
				{
					thisActivity.source.setCustomRingtone(value);
				}				

				return this;
			}

			public android.content.SharedPreferences.Editor remove( String key )
			{
				// Nothing to do here - this doesn't make sense.
				return this;
			}

			public void apply()
			{
				// TODO Auto-generated method stub
				
			}

			public android.content.SharedPreferences.Editor putStringSet( String arg0, Set<String> arg1 )
			{
				// TODO Auto-generated method stub
				return null;
			}
		}

		public Set<String> getStringSet( String arg0, Set<String> arg1 )
		{
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	public SharedPreferences getSharedPreferences(String name, int mode)
	{
		return this.preferenceMapper;
	}

	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		this.getSource();
		this.preferenceMapper = new ORMPreferencesMapper();

		this.addPreferencesFromResource(R.xml.source_preference_editor);
		
		// Find and attach the onclick handlers.
		Preference titlePreference = this.findPreference("source_title");
		titlePreference.setOnPreferenceChangeListener(immediateServerChangeListener);
		Preference serverEnabledPreference = this.findPreference("source_server_enable");
		serverEnabledPreference.setOnPreferenceChangeListener(immediateServerChangeListener);
		
		Preference sourceKeyPreference = this.findPreference("source_key");
		sourceKeyPreference.setOnPreferenceClickListener(emailClickHandler);
		Preference sourceEmailKeyPreference = this.findPreference("source_email");
		sourceEmailKeyPreference.setOnPreferenceClickListener(emailClickHandler);
		
		Preference messagesPreference = this.findPreference("source_messages");
		messagesPreference.setOnPreferenceClickListener(messagesClickHandler);
		Preference testPreference = this.findPreference("source_test");
		testPreference.setOnPreferenceClickListener(testClickHandler);
		Preference deletePreference = this.findPreference("source_delete");
		deletePreference.setOnPreferenceClickListener(deleteClickHandler);
		Preference globalNotificationPreference = this.findPreference("source_global");
		globalNotificationPreference.setOnPreferenceChangeListener(globalNofificationCheckListener);
	}
	
	public void onConfigurationChanged( Configuration newConfig )
	{
		super.onConfigurationChanged(newConfig);
	}

	public void onResume()
	{
		super.onResume();
		
		// Reload our source data.
		this.source = null;
		// And finally, update the screen.
		this.loadFromSource(this.getSource());
	}
	
	/**
	 * Load this activity from the given source.
	 * @param source
	 */
	public void loadFromSource( NotifrySource source )
	{
		// Create a new preferences mapper with updated source data.
		this.preferenceMapper = new ORMPreferencesMapper();		
		
		Preference titlePreference = this.findPreference("source_title");
		titlePreference.setSummary(source.getTitle());
		
		Preference sourceKey = this.findPreference("source_key");
		sourceKey.setSummary(source.getSourceKey());
		
		// Force update the server enabled preference.
		CheckBoxPreference serverEnabled = (CheckBoxPreference) this.findPreference("source_server_enable");
		serverEnabled.setChecked(source.getServerEnabled());
		
		this.toggleNotificationPreferences(source.getUseGlobalNotification());
	}
	
	/**
	 * A listener for changes that must occur immediately on the server.
	 */
	OnPreferenceChangeListener immediateServerChangeListener = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange( Preference preference, Object newValue )
		{
			// Crude: determine what thing we changed based on the object's type.
			// CAUTION: This won't work if we need to send more than a string or boolean to the server.
			BackendRequest request = new BackendRequest("/sources/edit");
			request.add("id", thisActivity.getSource().getServerId().toString());
			
			if( newValue instanceof String )
			{
				// Set the title.
				request.add("title", (String) newValue);
				if( thisActivity.getSource().getServerEnabled() )
				{
					request.add("enabled", "on");
				}
			}
			else if( newValue instanceof Boolean )
			{
				request.add("title", thisActivity.getSource().getTitle());
				// Set the server enabled value.
				Boolean boolValue = (Boolean) newValue;
				if( boolValue )
				{
					request.add("enabled", "on");
				}
			}

			request.addMeta("source", thisActivity.getSource());
			request.addMeta("operation", "save");
			
			request.setHandler(handler);
			
			request.startInThread(thisActivity, getString(R.string.source_saving_to_server), thisActivity.getSource().getAccountName());			
			
			// Prevent the change from occuring here.
			return false;
		}
	};
	
	
	/**
	 * Save this source.
	 */
	public void save( View view )
	{
		// User clicked save button.
		// Prepare the new local object.
		this.source = null;
		NotifrySource source = this.getSource();
		
		EditText title = (EditText) findViewById(R.id.detail_title);
		source.setTitle(title.getText().toString());
		
		CheckBox serverEnable = (CheckBox) findViewById(R.id.detail_serverenable);
		source.setServerEnabled(serverEnable.isChecked());
		CheckBox localEnable = (CheckBox) findViewById(R.id.detail_localenable);
		source.setLocalEnabled(localEnable.isChecked());
		
		// Now, send the updates to the server. On success, save the changes locally.
		BackendRequest request = new BackendRequest("/sources/edit");
		request.add("id", source.getServerId().toString());
		request.add("title", source.getTitle());
		if( source.getServerEnabled() )
		{
			request.add("enabled", "on");
		}
		
		request.addMeta("source", source);
		request.addMeta("operation", "save");
		
		request.setHandler(handler);
		
		request.startInThread(this, getString(R.string.source_saving_to_server), source.getAccountName());
	}
	
	public void toggleNotificationPreferences( boolean globalOn )
	{
		for( String preferenceName: SourceEditor.togglePreferences )
		{
			Preference preference = this.findPreference(preferenceName);
			preference.setEnabled(!globalOn);
		}
	}
	
	/**
	 * Toggle the global notifications enabled.
	 */
	OnPreferenceChangeListener globalNofificationCheckListener = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange( Preference preference, Object newValue )
		{
			thisActivity.toggleNotificationPreferences((Boolean) newValue);
			return true;
		}
	};

	/**
	 * Delete this source.
	 */
	OnPreferenceClickListener deleteClickHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			// User clicked delete button.
			// Confirm that's what they want.
			new AlertDialog.Builder(thisActivity).
				setTitle(getString(R.string.delete_source)).
				setMessage(getString(R.string.delete_source_message)).
				setPositiveButton(
						getString(R.string.delete),
						new DialogInterface.OnClickListener()
						{
							public void onClick( DialogInterface dialog, int whichButton )
							{
								// Fire it off to the delete source function.
								deleteSource(thisActivity.getSource());
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
			
			return true;
		}
	};
	
	/**
	 * Send a request to the backend to delete the source.
	 * @param source
	 */
	public void deleteSource( NotifrySource source )
	{
		// Now, send the updates to the server. On success, save the changes locally.
		BackendRequest request = new BackendRequest("/sources/delete");
		request.add("id", source.getServerId().toString());
		request.addMeta("operation", "delete");
		request.addMeta("source", getSource());
		request.setHandler(handler);
		
		request.startInThread(this, getString(R.string.source_deleting_from_server), source.getAccountName());		
	}
	
	/**
	 * Send a request to the backend to test this source.
	 * @param view
	 */
	OnPreferenceClickListener testClickHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			BackendRequest request = new BackendRequest("/sources/test");
			request.add("id", getSource().getServerId().toString());
			request.addMeta("operation", "test");
			request.addMeta("source", getSource());
			request.setHandler(handler);
			
			request.startInThread(thisActivity, getString(R.string.source_testing_with_server), source.getAccountName());
			
			return true;
		}
	};
	
	/**
	 * Email the source key to someone.
	 */
	OnPreferenceClickListener emailClickHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			// User wants to email the key to someone.
			final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.source_key_email_subject));
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, String.format(getString(R.string.source_key_email_body), getSource().getSourceKey()));
			thisActivity.startActivity(Intent.createChooser(emailIntent, "Send key via email"));
			
			return true;
		}
	};
	
	/**
	 * View the messages of this source.
	 */
	OnPreferenceClickListener messagesClickHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			Intent intent = new Intent(thisActivity, MessageList.class);
			intent.putExtra("sourceId", thisActivity.getSource().getId());
			startActivity(intent);
			return true;
		}
	};
	
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
					NotifrySource source = (NotifrySource) request.getMeta("source");
					
					String operation = (String) request.getMeta("operation");
					
					if( operation.equals("save") )
					{
						// Load the source from the server information. We assume the server is correct.
						source.fromJSONObject(response.getJSON().getJSONObject("source"));
	
						// Save it to the database.
						source.save(thisActivity);
						
						Toast.makeText(thisActivity, getString(R.string.source_save_success), Toast.LENGTH_SHORT).show();
						
						// Reload the parent activity.
						thisActivity.source = null;
						thisActivity.loadFromSource(thisActivity.getSource());
					}
					else if( operation.equals("delete") )
					{
						// Delete from local.
						source.delete(thisActivity);

						// Let the user know we're done.
						Toast.makeText(thisActivity, getString(R.string.source_delete_success), Toast.LENGTH_SHORT).show();
						
						// And exit this activity.
						thisActivity.finish();
					}
					else if( operation.equals("test") )
					{
						// The server has been asked to test us.
						Toast.makeText(thisActivity, getString(R.string.source_test_success), Toast.LENGTH_SHORT).show();
					}
				}
				catch( JSONException e )
				{
					// The response doesn't look like we expected.
					Log.d(TAG, "Invalid response from server: " + e.getMessage());
					Toast.makeText(thisActivity, "Invalid response from the server.", Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	/**
	 * Fetch the account that this source list is for.
	 * 
	 * @return
	 */
	public NotifrySource getSource()
	{
		if( this.source == null )
		{
			// Get the source from the intent.
			// We store it in a private variable to save us having to query the
			// DB each time.
			Intent sourceIntent = getIntent();
			this.source = NotifrySource.FACTORY.get(this, sourceIntent.getLongExtra("sourceId", 0)); 
		}

		return this.source;
	}
	
	/**
	 * When the home button is hit...
	 */
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case android.R.id.home:
				Intent intent = new Intent(getBaseContext(), SourceList.class);
				intent.putExtra("account",this.getSource().getAccountName());
				startActivity(intent);
				return true;
		}

		return super.onOptionsItemSelected((android.view.MenuItem) item);
	}	
}
