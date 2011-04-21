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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class NotifrySource extends ORM<NotifrySource>
{
	private static final String TAG = "Notifry";
	
	public static final NotifrySource FACTORY = new NotifrySource();

	private String accountName = null;
	private String changeTimestamp = null;
	private String title = null;
	private Long serverId = null;
	private String sourceKey = null;
	private Boolean serverEnabled = null;
	private Boolean localEnabled = null;
	private Boolean useGlobalNotification = true;
	private Boolean vibrate = false;
	private Boolean ringtone = false;
	private String customRingtone = "";
	private Boolean ledFlash = false;
	private Boolean speakMessage = false;
	
	/**
	 * Get the notification ID.
	 * This is the local source ID as an integer.
	 * @return
	 */
	public int getNotificationId()
	{
		// Yes, this casting will potentially lose precision. But unless
		// you've created a lot of local sources, you're unlikely to run
		// into it. If you run into this in production, please let me know.
		Long sourceId = this.getId();
		int notifyId = (int)(sourceId % Integer.MAX_VALUE);
		return notifyId;
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

	public Boolean getUseGlobalNotification()
	{
		return useGlobalNotification;
	}

	public void setUseGlobalNotification( Boolean useGlobalNotification )
	{
		this.useGlobalNotification = useGlobalNotification;
	}

	public Boolean getVibrate()
	{
		return vibrate;
	}

	public void setVibrate( Boolean vibrate )
	{
		this.vibrate = vibrate;
	}

	public Boolean getRingtone()
	{
		return ringtone;
	}

	public void setRingtone( Boolean ringtone )
	{
		this.ringtone = ringtone;
	}

	public String getCustomRingtone()
	{
		return customRingtone;
	}

	public void setCustomRingtone( String customRingtone )
	{
		this.customRingtone = customRingtone;
	}

	public Boolean getLedFlash()
	{
		return ledFlash;
	}

	public void setLedFlash( Boolean ledFlash )
	{
		this.ledFlash = ledFlash;
	}

	public Boolean getSpeakMessage()
	{
		return speakMessage;
	}

	public void setSpeakMessage( Boolean speakMessage )
	{
		this.speakMessage = speakMessage;
	}

	public void fromJSONObject( JSONObject source ) throws JSONException
	{
		this.changeTimestamp = source.getString("updated");
		this.title = source.getString("title");
		this.serverEnabled = source.getBoolean("enabled");
		this.sourceKey = source.getString("key");
		this.serverId = source.getLong("id");
	}
	
	public ArrayList<NotifrySource> listAll( Context context, String accountName )
	{
		return NotifrySource.FACTORY.genericList(context, NotifryDatabaseAdapter.KEY_ACCOUNT_NAME + "= ?", new String[] { accountName }, NotifryDatabaseAdapter.KEY_TITLE + " ASC");
	}
	
	public int countSources( Context context, String accountName )
	{
		String query = null;
		String[] queryParams = null;
		if( accountName != null )
		{
			query = NotifryDatabaseAdapter.KEY_ACCOUNT_NAME + "= ?";
			queryParams = new String[] { accountName };
		}
		return this.genericCount(context, query, queryParams);
	}	
	
	public NotifrySource getByServerId( Context context, Long serverId )
	{
		return NotifrySource.FACTORY.getOne(context, NotifryDatabaseAdapter.KEY_SERVER_ID + "=" + serverId, null);
	}
	
	public ArrayList<NotifrySource> syncFromJSONArray( Context context, JSONArray sourceList, String accountName ) throws JSONException
	{
		ArrayList<NotifrySource> result = new ArrayList<NotifrySource>();
		HashSet<Long> seenIds = new HashSet<Long>();
		
		for( int i = 0; i < sourceList.length(); i++ )
		{
			// See if we can find a local object with that ID.
			JSONObject object = sourceList.getJSONObject(i);
			Long serverId = object.getLong("id");
			
			NotifrySource source = NotifrySource.FACTORY.getByServerId(context, serverId);
			
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
			source.save(context);
			
			seenIds.add(source.getId());
		}
		
		// Now, find out the IDs that exist in our database but were not in our list.
		// Those have been deleted.
		ArrayList<NotifrySource> allSources = NotifrySource.FACTORY.listAll(context, accountName);
		HashSet<Long> allIds = new HashSet<Long>();
		for( NotifrySource source: allSources )
		{
			allIds.add(source.getId());
		}
		
		allIds.removeAll(seenIds);

		for( Long sourceId: allIds )
		{
			NotifrySource source = NotifrySource.FACTORY.get(context, sourceId);
			NotifryMessage.FACTORY.deleteMessagesBySource(context, source, false);
			source.delete(context);
		}

		return result;
	}

	@Override
	public Uri getContentUri()
	{
		return NotifryDatabaseAdapter.CONTENT_URI_SOURCES;
	}

	@Override
	protected ContentValues flatten()
	{
		ContentValues values = new ContentValues();
		values.put(NotifryDatabaseAdapter.KEY_ACCOUNT_NAME, this.getAccountName());
		values.put(NotifryDatabaseAdapter.KEY_SERVER_ENABLED, this.getServerEnabled() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_LOCAL_ENABLED, this.getLocalEnabled() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_TITLE, this.getTitle());
		values.put(NotifryDatabaseAdapter.KEY_SERVER_ID, this.getServerId());
		values.put(NotifryDatabaseAdapter.KEY_CHANGE_TIMESTAMP, this.getChangeTimestamp());
		values.put(NotifryDatabaseAdapter.KEY_SOURCE_KEY, this.getSourceKey());
		values.put(NotifryDatabaseAdapter.KEY_USE_GLOBAL_NOTIFICATION, this.getUseGlobalNotification() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_VIBRATE, this.getVibrate() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_RINGTONE, this.getRingtone() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_CUSTOM_RINGTONE, this.getCustomRingtone());
		values.put(NotifryDatabaseAdapter.KEY_LED_FLASH, this.getLedFlash() ? 1 : 0);
		values.put(NotifryDatabaseAdapter.KEY_SPEAK_MESSAGE, this.getSpeakMessage() ? 1 : 0);
		return values;
	}

	@Override
	protected NotifrySource inflate( Context context, Cursor cursor )
	{
		NotifrySource source = new NotifrySource();
		source.setAccountName(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_ACCOUNT_NAME)));
		source.setId(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_ID)));
		source.setServerEnabled(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SERVER_ENABLED)) == 0 ? false : true);
		source.setLocalEnabled(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_LOCAL_ENABLED)) == 0 ? false : true);
		source.setServerId(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SERVER_ID)));
		source.setTitle(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_TITLE)));
		source.setChangeTimestamp(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_CHANGE_TIMESTAMP)));
		source.setSourceKey(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SOURCE_KEY)));
		
		source.setUseGlobalNotification(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_USE_GLOBAL_NOTIFICATION)) == 0 ? false : true);
		source.setVibrate(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_VIBRATE)) == 0 ? false : true);
		source.setRingtone(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_RINGTONE)) == 0 ? false : true);
		source.setLedFlash(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_LED_FLASH)) == 0 ? false : true);
		source.setCustomRingtone(cursor.getString(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_CUSTOM_RINGTONE)));
		source.setSpeakMessage(cursor.getLong(cursor.getColumnIndex(NotifryDatabaseAdapter.KEY_SPEAK_MESSAGE)) == 0 ? false : true);
		
		return source;
	}

	@Override
	protected String[] getProjection()
	{
		return NotifryDatabaseAdapter.SOURCE_PROJECTION;
	}
}
