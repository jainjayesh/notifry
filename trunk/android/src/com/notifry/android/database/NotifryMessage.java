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
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class NotifryMessage
{
	private static final String TAG = "Notifry";
	private Long id;
	private Long serverId;
	private NotifrySource source;
	private String title;
	private String timestamp;
	private String message;
	private String url;
	private Boolean seen;

	public Long getId()
	{
		return id;
	}

	public void setId( Long id )
	{
		this.id = id;
	}

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
	
	public String getDisplayTimestamp()
	{
		try
		{	
			// Parse it, and display in LOCAL timezone.
			SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US);
			ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
			Date date = ISO8601DATEFORMAT.parse(this.timestamp);
			DateFormat formatter = DateFormat.getDateTimeInstance();
			formatter.setTimeZone(TimeZone.getDefault());
			return formatter.format(date);
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

	public static NotifryMessage fromC2DM( Context context, Bundle extras )
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
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
		database.open();
		NotifrySource source = database.getSourceByServerId(sourceId);
		database.close();
		
		if( source == null )
		{
			// No such source... now what?
			// TODO: Deal with this.
			Log.e(TAG, "No such source " + sourceId);
		}
		else
		{
			incoming.setSource(source);
		}
		
		return incoming;
	}
}
