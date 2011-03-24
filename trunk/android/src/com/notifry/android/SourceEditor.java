package com.notifry.android;

import org.json.JSONException;

import com.notifry.android.database.NotifrySource;
import com.notifry.android.remote.BackendRequest;
import com.notifry.android.remote.BackendResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
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
		
		TextView sourceKey = (TextView) findViewById(R.id.detail_sourcekey);
		sourceKey.setText(getString(R.string.source_key_heading) + "\n" + source.getSourceKey());
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
		request.addMeta("operation", "save");
		
		request.setHandler(handler);
		
		request.startInThread(this, getString(R.string.source_saving_to_server), source.getAccountName());
	}
	
	/**
	 * Delete this source.
	 */
	public void delete( View view )
	{
		// User clicked delete button.
		// Confirm that's what they want.
		new AlertDialog.Builder(this).
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

	}
	
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
	public void test( View view )
	{
		BackendRequest request = new BackendRequest("/sources/test");
		request.add("id", getSource().getServerId().toString());
		request.addMeta("operation", "test");
		request.addMeta("source", getSource());
		request.setHandler(handler);
		
		request.startInThread(this, getString(R.string.source_testing_with_server), source.getAccountName());		
	}
	
	/**
	 * Email the source key to someone.
	 */
	public void emailKey( View view )
	{
		// User wants to email the key to someone.
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.source_key_email_subject));
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, String.format(getString(R.string.source_key_email_body), getSource().getSourceKey()));
		this.startActivity(Intent.createChooser(emailIntent, "Send key via email"));
	}
	
	/**
	 * View the messages of this source.
	 * @param view
	 */
	public void messages( View view )
	{
		Intent intent = new Intent(this, MessageList.class);
		intent.putExtra("sourceId", this.getSource().getId());
		startActivity(intent);
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
					
					String operation = (String) request.getMeta("operation");
					
					if( operation.equals("save") )
					{
						// Load the source from the server information. We assume the server is correct.
						source.fromJSONObject(response.getJSON().getJSONObject("source"));
	
						// Save it to the database.
						source.save(thisActivity);
						
						Toast.makeText(thisActivity, getString(R.string.source_save_success), Toast.LENGTH_SHORT).show();
						
						// "Exit" our activity and go back to the list.
						thisActivity.finish();
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
}
