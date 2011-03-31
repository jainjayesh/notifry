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

package com.notifry.android.database;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.notifry.android.UpdaterService;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class NotifryMessage extends ORM<NotifryMessage>
{
	public final static NotifryMessage FACTORY = new NotifryMessage(); 
	
	private static final String TAG = "Notifry";
	private Long serverId;
	private NotifrySource source;
	private String title;
	private String timestamp;
	private String message;
	private String url;
	private Boolean seen;

	public Long getServerId()
	{
		return serverId;
	}

	public void setServerId( Long serverId )
	{
		this.serverId = serverId;
	}

	public NotifrySource getSource()
	{
		return source;
	}

	public void setSource( NotifrySource source )
	{
		this.source = source;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle( String title )
	{
		this.title = title;
	}

	public String getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp( String timestamp )
	{
		this.timestamp = timestamp;
	}

	public static Date parseISO8601String( String isoString ) throws ParseException
	{
		SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
		ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		return ISO8601DATEFORMAT.parse(isoString);
	}

	public static String formatUTCAsLocal( Date date )
	{
		DateFormat formatter = DateFormat.getDateTimeInstance();
		formatter.setTimeZone(TimeZone.getDefault());
		return formatter.format(date);		
	}
	
	public String getDisplayTimestamp()
	{
		try
		{	
			return NotifryMessage.formatUTCAsLocal(NotifryMessage.parseISO8601String(this.timestamp));
		}
		catch( ParseException e )
		{
			return "Parse error";
		}
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage( String message )
	{
		this.message = message;
	}

	public String getUrl()
	{
		return url;
	}

	public void setUrl( String url )
	{
		this.url = url;
	}
	
	public Boolean getSeen()
	{
		return seen;
	}

	public void setSeen( Boolean seen )
	{
		this.seen = seen;
	}

	public static NotifryMessage fromC2DM( Context context, Bundle extras ) throws UnsourceableMessage
	{
		NotifryMessage incoming = new NotifryMessage();
		incoming.setMessage(extras.getString("message"));
		incoming.setTitle(extras.getString("title"));
		incoming.setUrl(extras.getString("url"));	
		incoming.setServerId(Long.parseLong(extras.getString("server_id")));
		incoming.setTimestamp(extras.getString("timestamp"));
		incoming.setSeen(false);
		
		// Look up the source.
		Long sourceId = Long.parseLong(extras.getString("source_id"));
		NotifrySource source = NotifrySource.FACTORY.getByServerId(context, sourceId);
		
		if( source == null )
		{
			// No such source... now what?
			// Create a dummy source as a placeholder, then ask the updater service to fetch the rest
			// when it can.
			NotifrySource unknownSource = new NotifrySource();
			unknownSource.setTitle("Unknown Source");
			unknownSource.setLocalEnabled(true);
			unknownSource.setServerEnabled(true);
			unknownSource.setServerId(sourceId);
			
			// Find the account for this unknown source.
			Long accountId = Long.parseLong(extras.getString("device_id"));
			NotifryAccount account = NotifryAccount.FACTORY.getByServerId(context, accountId);
			
			if( account == null )
			{
				// Ok, not even that account is known here. We've done all we can - abort!
				throw FACTORY.new UnsourceableMessage();
			}
			
			unknownSource.setAccountName(account.getAccountName());
			unknownSource.setSourceKey("UNKNOWN - Refresh for correct key");
			unknownSource.setChangeTimestamp("");
			
			unknownSource.save(context);
			
			incoming.setSource(unknownSource);
			
			// Fire off the background request to update the source information.
			Intent intentData = new Intent(context, UpdaterService.class);
			intentData.putExtra("type", "sourcechange");
			intentData.putExtra("sourceId", sourceId);
			intentData.putExtra("deviceId", accountId);
			context.startService(intentData);
		}
		else
		{
			incoming.setSource(source);
		}
		
		return incoming;
	}
	
	public ArrayList<NotifryMessage> list( Context context, NotifrySource source )
	{
		String query = "";
		if( source != null )
		{
			query = NotifryDatabaseAdapter.KEY_SOURCE_ID + "=" + source.getId();
		}
		
		return this.genericList(context, query, null, NotifryDatabaseAdapter.KEY_TIMESTAMP + " DESC");
	}
	
	public Cursor cursorList( Context context, NotifrySource source )
	{
		String query = "";
		if( source != null )
		{
			query = NotifryDatabaseAdapter.KEY_SOURCE_ID + "=" + source.getId();
		}		
		
		return context.getContentResolver().query(
				this.getContentUri(),
				new String[] { NotifryDatabaseAdapter.KEY_ID, NotifryDatabaseAdapter.KEY_TITLE, NotifryDatabaseAdapter.KEY_TIMESTAMP, NotifryDatabaseAdapter.KEY_SEEN },
				query,
				null,
				NotifryDatabaseAdapter.KEY_TIMESTAMP + " DESC");
	}

	public int countUnread( Context context, NotifrySource source )
	{
		String query = NotifryDatabaseAdapter.KEY_SEEN + " = 0 ";
		if( source != null )
		{
			query += " AND " + NotifryDatabaseAdapter.KEY_SOURCE_ID + "=" + source.getId();
		}
		return this.genericCount(context, query, null);
	}
	
	public void markAllAsSeen( Context context, NotifrySource source )
	{
		ContentValues values = new ContentValues();
		values.put(NotifryDatabaseAdapter.KEY_SEEN, 1);
		
		String query = null;
		if( source != null )
		{
			query = NotifryDatabaseAdapter.KEY_SOURCE_ID + " = " + source.getId();
		}
		
		context.getContentResolver().update(this.getContentUri(), values, query, null);
	}
	
	public void deleteMessagesBySource( Context context, NotifrySource source, boolean onlyRead )
	{
		String query = null;
		if( source != null )
		{
			query = NotifryDatabaseAdapter.KEY_SOURCE_ID + "=" + source.getId();
		}
		if( onlyRead )
		{
			if( query != null )
			{
				query += " AND ";
			}
			else
			{
				query = "";
			}
			
			query += NotifryDatabaseAdapter.KEY_SEEN + "= 1";
		}
		
		this.genericDelete(context, query, null);
	}
	
	public void deleteOlderThan( Context context, Date date )
	{
		// Parse it, and display in LOCAL timezone.
		SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
		ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
		String formattedDate = ISO8601DATEFORMAT.format(date);
		
		// So, everything older than formattedDate should be removed.
		this.genericDelete(context, NotifryDatabaseAdapter.KEY_TIMESTAMP + " < ?", new String[] { formattedDate });
	}	

	@Override
	public Uri getContentUri()
	{
		return NotifryDatabaseAdapter.CONTENT_URI_MESSAGES;
	}

	@Override
	protected ContentValues flatten()
	{
		ContentValues values = new ContentValues();
		values.put(NotifryDatabaseAdapter.KEY_TITLE, this.getTitle());
		values.put(NotifryDatabaseAdapter.KEY_SOURCE_ID, this.getSource().getId());
		values.put(NotifryDatabaseAdapter.KEY_SERVER_ID, this.getServerId());
		values.put(NotifryDatabaseAdapter.KEY_MESSAGE, this.getMessage());
		values.put(NotifryDatabaseAdapter.KEY_URL, this.getUrl());
		values.put(NotifryDatabaseAdapter.KEY_TIMESTAMP, this.getTimestamp());
		values.put(NotifryDatabaseAdapter.KEY_SEEN, this.getSeen() ? 1 : 0);
		
		return values;
	}

	@Override
	protected NotifryMessage inflate( Context context, Cursor cursor )
	{
		NotifryMessage message = new NotifryMessage();
		message.setId(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_ID)));
		message.setTitle(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TITLE)));
		message.setMessage(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_MESSAGE)));
		message.setUrl(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_URL)));
		message.setSource(NotifrySource.FACTORY.get(context, cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SOURCE_ID))));
		message.setServerId(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SERVER_ID)));
		message.setSeen(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SEEN)) == 0 ? false : true);
		message.setTimestamp(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TIMESTAMP)));

		return message;
	}

	@Override
	protected String[] getProjection()
	{
		return NotifryDatabaseAdapter.MESSAGE_PROJECTION;
	}
	
	public class UnsourceableMessage extends Exception
	{
		private static final long serialVersionUID = 1L;	
	}
}
