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

import java.util.ArrayList;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class NotifrySource
{
	private static final String TAG = "Notifry";
	
	private Long id = null;
	private String accountName = null;
	private String changeTimestamp = null;
	private String title = null;
	private Long serverId = null;
	private String sourceKey = null;
	private Boolean serverEnabled = null;
	private Boolean localEnabled = null;

	public Long getId()
	{
		return id;
	}

	public void setId( Long id )
	{
		this.id = id;
	}

	public String getAccountName()
	{
		return accountName;
	}

	public void setAccountName( String accountName )
	{
		this.accountName = accountName;
	}

	public String getChangeTimestamp()
	{
		return changeTimestamp;
	}

	public void setChangeTimestamp( String changeTimestamp )
	{
		this.changeTimestamp = changeTimestamp;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle( String title )
	{
		this.title = title;
	}

	public Long getServerId()
	{
		return serverId;
	}

	public void setServerId( Long serverId )
	{
		this.serverId = serverId;
	}

	public String getSourceKey()
	{
		return sourceKey;
	}

	public void setSourceKey( String sourceKey )
	{
		this.sourceKey = sourceKey;
	}

	public Boolean getServerEnabled()
	{
		return serverEnabled;
	}

	public void setServerEnabled( Boolean serverEnabled )
	{
		this.serverEnabled = serverEnabled;
	}

	public Boolean getLocalEnabled()
	{
		return localEnabled;
	}

	public void setLocalEnabled( Boolean localEnabled )
	{
		this.localEnabled = localEnabled;
	}

	public void fromJSONObject( JSONObject source ) throws JSONException
	{
		this.changeTimestamp = source.getString("updated");
		this.title = source.getString("title");
		this.serverEnabled = source.getBoolean("enabled");
		this.sourceKey = source.getString("key");
		this.serverId = source.getLong("id");
	}
	
	public static ArrayList<NotifrySource> syncFromJSONArray( Context context, JSONArray sourceList, String accountName ) throws JSONException
	{
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(context);
		database.open();
		ArrayList<NotifrySource> result = new ArrayList<NotifrySource>();
		HashSet<Long> seenIds = new HashSet<Long>();
		
		for( int i = 0; i < sourceList.length(); i++ )
		{
			// See if we can find a local object with that ID.
			JSONObject object = sourceList.getJSONObject(i);
			Long serverId = object.getLong("id");
			
			NotifrySource source = database.getSourceByServerId(serverId);
			
			if( source == null )
			{
				// We don't have that source locally. Create it.
				source = new NotifrySource();
				source.fromJSONObject(object);
				// It's only locally enabled if the server has it enabled.
				source.setLocalEnabled(source.getServerEnabled());
				source.setAccountName(accountName);
			}
			else
			{
				// Server already has it. Assume the server is the most up to date version.
				source.fromJSONObject(object);
			}
			
			// Save it in the database.
			database.saveSource(source);
			
			seenIds.add(source.getId());
		}
		
		// Now, find out the IDs that exist in our database but were not in our list.
		// Those have been deleted.
		seenIds.removeAll(database.sourceIdSet(accountName));
		
		// tempSource is a hack to get around having to instantiate the objects.
		NotifrySource tempSource = new NotifrySource();
		for( Long sourceId: seenIds )
		{
			tempSource.setId(sourceId);
			database.deleteSource(tempSource);
		}
		
		database.close();
		return result;
	}
}
