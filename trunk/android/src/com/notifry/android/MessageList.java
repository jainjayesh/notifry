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

import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifryMessage;
import com.notifry.android.database.NotifrySource;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MessageList extends ListActivity
{
	private final static int DELETE_ALL = 1;
	private final static int DELETE_SEEN = 2;
	private static final String TAG = "Notifry";
	private final MessageList thisActivity = this;
	private NotifrySource source = null;

	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		// Set the layout, and allow text filtering.
		setContentView(R.layout.screen_recent_messages);
		getListView().setTextFilterEnabled(true);	
	}

	public void onResume()
	{
		super.onResume();

		// When coming back, refresh our list of messages.
		refreshView();
	}

	/**
	 * Fetch the source that this message list is for (optional!)
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
			Long sourceId = sourceIntent.getLongExtra("sourceId", 0);
			
			if( sourceId > 0 )
			{
				this.source = NotifrySource.FACTORY.get(this, sourceId); 
			}
		}

		return this.source;
	}

	/**
	 * Refresh the list of messages viewed by this activity.
	 */
	public void refreshView()
	{
		// Refresh our list of messages.
		ArrayList<NotifryMessage> messages = NotifryMessage.FACTORY.list(this, this.getSource()); 

		this.setListAdapter(new MessageArrayAdapter(this, this, R.layout.message_list_row, messages));
	}
	
	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, DELETE_ALL, 0, R.string.delete_all).setIcon(android.R.drawable.ic_menu_delete);
		menu.add(0, DELETE_SEEN, 0, R.string.delete_read).setIcon(android.R.drawable.ic_menu_delete);
		return result;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item )
	{
		switch( item.getItemId() )
		{
			case DELETE_ALL:
				deleteAll(false);
				return true;
			case DELETE_SEEN:
				deleteAll(true);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	public void deleteAll( boolean onlySeen )
	{
		// Delete all messages. Optionally, those matching the given source.
		NotifryMessage.FACTORY.deleteMessagesBySource(this, source, onlySeen);
		
		// And refresh!
		refreshView();
	}

	/**
	 * Handler for when you click a message.
	 * 
	 * @param message
	 */
	public void clickMessage( NotifryMessage message )
	{
		// Launch the message detail.
		Intent intent = new Intent(getBaseContext(), MessageDetail.class);
		intent.putExtra("messageId", message.getId());
		startActivity(intent);
	}

	/**
	 * An array adapter to put messages into the list view.
	 * 
	 * @author daniel
	 */
	private class MessageArrayAdapter extends ArrayAdapter<NotifryMessage>
	{
		final private MessageList parentActivity;
		private ArrayList<NotifryMessage> messages;

		public MessageArrayAdapter( MessageList parentActivity, Context context, int textViewResourceId, ArrayList<NotifryMessage> objects )
		{
			super(context, textViewResourceId, objects);
			this.parentActivity = parentActivity;
			this.messages = objects;
		}

		public View getView( int position, View convertView, ViewGroup parent )
		{
			// Inflate a view if required.
			if( convertView == null )
			{
				LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.message_list_row, null);
			}

			// Find the message.
			final NotifryMessage message = this.messages.get(position);

			// And set the values on our row.
			if( message != null )
			{
				TextView title = (TextView) convertView.findViewById(R.id.message_row_title);
				TextView timestamp = (TextView) convertView.findViewById(R.id.message_row_timestamp);
				
				View.OnClickListener clickListener = new View.OnClickListener()
				{
					public void onClick( View v )
					{
						parentActivity.clickMessage(message);
					}
				};
				
				if( title != null )
				{
					title.setText(message.getTitle());
					title.setClickable(true);

					title.setOnClickListener(clickListener);
					
					// And if the message is unseen, make the title bold.
					if( message.getSeen() == false )
					{
						title.setTypeface(Typeface.DEFAULT_BOLD);
					}
				}
				if( timestamp != null )
				{
					timestamp.setText(message.getDisplayTimestamp());
					timestamp.setClickable(true);

					timestamp.setOnClickListener(clickListener);
				}				
			}

			return convertView;
		}
	}
}