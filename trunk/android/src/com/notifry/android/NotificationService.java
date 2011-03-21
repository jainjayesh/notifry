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

import com.notifry.android.database.NotifryDatabaseAdapter;
import com.notifry.android.database.NotifryMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Audio;
import android.util.Log;

public class NotificationService extends Service
{
	private static final String TAG = "Notifry";
	private NotificationManager notificationManager;

	@Override
	public IBinder onBind( Intent arg0 )
	{
		return null;
	}

	public void onCreate()
	{
		super.onCreate();
		
		// Fetch out our notification service.
		this.notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	public void onStart( Intent intent, int startId )
	{
		super.onStart(intent, startId);
		
		// Is the master enable off? Then don't bother doing anything.
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if( settings.getBoolean(getString(R.string.masterEnable), true) == false )
		{
			Log.d(TAG, "Master enable is off, so not doing anything.");
			return;
		}
		
		// We were provided with a message ID. Load it and then handle it.
		Long messageId = intent.getLongExtra("messageId", 0);
		int unreadMessagesOfType = 0;
		
		NotifryDatabaseAdapter database = new NotifryDatabaseAdapter(this);
		database.open();
		NotifryMessage message = database.getMessageById(messageId);
		if( message != null )
		{
			unreadMessagesOfType = database.countUnreadMessages(message.getSource());
		}
		database.close();
		
		// If the message is NULL, then we've been passed an invalid message - return.
		if( message == null )
		{
			Log.d(TAG, "Message " + messageId + " not found, so not doing anything.");
			return;
		}
		
		// Make a decision on the message.
		NotifyDecision decision = NotifyDecision.shouldNotify(this, message);
		
		if( decision.getShouldNotify() )
		{
			// Ok, let's start notifying!
			int icon = R.drawable.icon_statusbar;
			long when = System.currentTimeMillis(); // TODO - make this the timestamp on the message.
			Notification notification = new Notification(icon, getString(R.string.app_name), when);
			
			Context context = getApplicationContext();
			String contentTitle = "";
			String contentText = "";
			
			if( unreadMessagesOfType == 1 )
			{
				// Only one message of this type. Set the title to be the message's title, and then
				// content to be the message itself.
				contentTitle = message.getTitle();
				contentText = message.getMessage();
			}
			else
			{
				// More than one message. Instead, the title is the source name,
				// and the content is the number of unseen messages.
				contentTitle = message.getSource().getTitle();
				contentText = String.format("%d unseen messages", unreadMessagesOfType);
			}

			// Generate the intent to go to that message list.
			Intent notificationIntent = new Intent(this, MessageList.class);
			notificationIntent.putExtra("sourceId", message.getSource().getId());
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

			// Set the notification data.
			notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
			
			// Now, other notification methods.
			if( settings.getBoolean(getString(R.string.playRingtone), true) )
			{
				String tone = settings.getString(getString(R.string.choosenNotification), "");
				Log.d(TAG, "Notification selected by user: " + tone);
				if( tone.equals("") )
				{
					// Set the default notification tone.
					notification.defaults |= Notification.DEFAULT_SOUND;
				}
				else
				{
					// Load the notification and add it.
					notification.sound = Uri.withAppendedPath(Audio.Media.INTERNAL_CONTENT_URI, tone);
				}
			}
			if( settings.getBoolean(getString(R.string.vibrateNotify), true) )
			{
				notification.defaults |= Notification.DEFAULT_VIBRATE;
			}
			if( settings.getBoolean(getString(R.string.ledFlash), true) )
			{
				if( settings.getBoolean(getString(R.string.fastLedFlash), false) )
				{
					// Special "fast flash" mode for phones with poor notification LEDs.
					// Ie, my G2 that flashes very slowly so it's hard to notice.
					notification.ledARGB = 0xff00ff00;
					notification.ledOnMS = 300;
					notification.ledOffMS = 1000;
					notification.flags |= Notification.FLAG_SHOW_LIGHTS;					
				}
				else
				{
					// Use the default device flash notifications.
					notification.defaults |= Notification.DEFAULT_LIGHTS;
				}
			}
			
			// Put the notification in the tray. Use the source's ID to identify it.
			// And this confusion below is because the ID's are Longs. The loss of precision here should not
			// be an issue, unless you're a particularly keen notifrier.
			Long sourceId = message.getSource().getId();
			int notifyId = (int)(sourceId % Integer.MAX_VALUE);
			this.notificationManager.notify(notifyId, notification);

			// If we're speaking, dispatch the message to the speaking service.
			if( settings.getBoolean(getString(R.string.speakMessage), false) )
			{
				Intent intentData = new Intent(getBaseContext(), SpeakService.class);
				Log.d(TAG, "Speaking text: " + decision.getSpokenMessage());
				intentData.putExtra("text", decision.getSpokenMessage());
				startService(intentData);
			}
		}
		
		return;
	}
}
