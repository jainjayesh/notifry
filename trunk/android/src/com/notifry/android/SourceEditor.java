package com.notifry.android;

import org.json.JSONException;

import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifrySource;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class SourceEditor extends Activity
{
	private static final String TAG = "Notifry";
	private final SourceEditor thisActivity = this;
	private NotifrySource source = null;

	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		// Set the layout, and allow text filtering.
		setContentView(R.layout.screen_sourcedetail);
	}
	
	public void onResume()
	{
		super.onResume();
		
		// Reload our source data.
		this.source = null;
		this.loadFromSource(this.getSource());
	}
	
	/**
	 * Load this activity from the given source.
	 * @param source
	 */
	public void loadFromSource( NotifrySource source )
	{
		EditText title = (EditText) findViewById(R.id.detail_title);
		title.setText(source.getTitle());
		
		CheckBox serverEnable = (CheckBox) findViewById(R.id.detail_serverenable);
		serverEnable.setChecked(source.getServerEnabled());
		CheckBox localEnable = (CheckBox) findViewById(R.id.detail_localenable);
		localEnable.setChecked(source.getLocalEnabled());
	}
	
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
		
		request.setHandler(handler);
		
		request.startInThread(this, getString(R.string.source_saving_to_server), source.getAccountName());
	}
	
	/**
	 * Email the source key to someone.
	 */
	public void emailKey( View view )
	{
		// User wants to email the key to someone.
	}
	
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
					// Load the source from the server information. We assume the server is correct.
					source.fromJSONObject(response.getJSON().getJSONObject("source"));

					// Open the database and save it.
					NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(thisActivity);
					database.open();
					database.saveSource(source);
					database.close();
					
					Toast.makeText(thisActivity, "Source saved successfully.", Toast.LENGTH_SHORT).show();
					
					// "Exit" our activity and go back to the list.
					thisActivity.finish();
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
			NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
			database.open();
			this.source = database.getSourceById(sourceIntent.getLongExtra("sourceId", 0));
			database.close();
		}

		return this.source;
	}
}
