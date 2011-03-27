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

import java.text.ParseException;

import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifryMessage;
import com.notifry.android.database.NotifrySource;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class MessageList extends ListActivity
{
	private final static int DELETE_ALL = 1;
	private final static int DELETE_SEEN = 2;
	private final static int MARK_ALL_AS_SEEN = 3;
	private NotifrySource source = null;

	/** Called when the activity is first created. */
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);

		// Set the layout.
		setContentView(R.layout.screen_recent_messages);
		
		// Set up our cursor and list adapter. This automatically updates
		// as messages are updated and changed.
		Cursor cursor = NotifryMessage.FACTORY.cursorList(this, this.getSource());
		SimpleCursorAdapter adapter = new SimpleCursorAdapter(
				this,
				R.layout.message_list_row,
				cursor,
				new String[] { NotifryDatabaseAdapter.KEY_TITLE, NotifryDatabaseAdapter.KEY_TIMESTAMP, NotifryDatabaseAdapter.KEY_SEEN },
				new int[] { R.id.message_row_title, R.id.message_row_timestamp });
		
		adapter.setViewBinder(new MessageViewBinder());
		
		this.setListAdapter(adapter);
	}

	public void onResume()
	{
		super.onResume();

		// And tell the notification service to clear the notification.
		if( this.getSource() != null )
		{
			Intent intentData = new Intent(getBaseContext(), NotificationService.class);
			intentData.putExtra("operation", "update");
			intentData.putExtra("sourceId", this.getSource().getId());
			startService(intentData);
			
			// Set the title of this activity.
			setTitle(String.format(getString(R.string.messages_source_title), this.getSource().getTitle()));
		}
		else
		{
			// Set the title of this activity.
			setTitle(getString(R.string.messages_all_title));
		}
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

	@Override
	public boolean onCreateOptionsMenu( Menu menu )
	{
		boolean result = super.onCreateOptionsMenu(menu);
		menu.add(0, DELETE_ALL, 0, R.string.delete_all).setIcon(android.R.drawable.ic_menu_delete);
		menu.add(0, DELETE_SEEN, 0, R.string.delete_read).setIcon(android.R.drawable.ic_menu_delete);
		menu.add(0, MARK_ALL_AS_SEEN, 0, R.string.mark_all_as_seen).setIcon(android.R.drawable.ic_media_play);
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
			case MARK_ALL_AS_SEEN:
				markAllAsSeen();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}
	
	public void deleteAll( boolean onlySeen )
	{
		// Delete all messages. Optionally, those matching the given source.
		NotifryMessage.FACTORY.deleteMessagesBySource(this, source, onlySeen);
	}
	
	public void markAllAsSeen()
	{
		NotifryMessage.FACTORY.markAllAsSeen(this, this.getSource());
	}

	/**
	 * When an item in the listview is clicked...
	 */
	protected void onListItemClick( ListView l, View v, int position, long id )
	{
		// Launch the message detail.
		Intent intent = new Intent(getBaseContext(), MessageDetail.class);
		intent.putExtra("messageId", id);
		startActivity(intent);
	}
	
	/**
	 * List item view binding class - used to format dates and make the title bold.
	 * @author daniel
	 */
	private class MessageViewBinder implements SimpleCursorAdapter.ViewBinder
	{
		public boolean setViewValue( View view, Cursor cursor, int columnIndex )
		{
			// Format the timestamp as local time.
			if( columnIndex == cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TIMESTAMP) )
			{
				TextView timestamp = (TextView) view;
				try
				{
					timestamp.setText(NotifryMessage.formatUTCAsLocal(NotifryMessage.parseISO8601String(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TIMESTAMP)))));
				}
				catch( ParseException ex )
				{
					timestamp.setText("UNKNOWN");
				}
				return true;
			}
			// Make the title bold if it's unseen.
			if( columnIndex == cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TITLE) )
			{
				TextView title = (TextView) view;
				title.setText(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TITLE)));
				
				if( cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SEEN)) == 0 )
				{
					title.setTypeface(Typeface.DEFAULT_BOLD);
				}
				return true;
			}

			return false;
		}
	}
}