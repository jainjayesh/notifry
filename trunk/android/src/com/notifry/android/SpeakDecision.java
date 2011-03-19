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

import com.notifry.android.database.NotifryMessage;

import android.content.Context;

public class SpeakDecision
{
	private Boolean shouldSpeak;
	private String spokenMessage;
	private NotifryMessage message;

	public Boolean getShouldSpeak()
	{
		return shouldSpeak;
	}

	public void setShouldSpeak( Boolean shouldSpeak )
	{
		this.shouldSpeak = shouldSpeak;
	}

	public String getSpokenMessage()
	{
		return spokenMessage;
	}

	public void setSpokenMessage( String spokenMessage )
	{
		this.spokenMessage = spokenMessage;
	}

	public NotifryMessage getMessage()
	{
		return message;
	}

	public void setMessage( NotifryMessage message )
	{
		this.message = message;
	}

	/**
	 * Determine if we should speak the given message or not, and also
	 * format the text in preparation for the speech.
	 * @param context
	 * @param message
	 * @return
	 */
	public static SpeakDecision shouldSpeak( Context context, NotifryMessage message )
	{
		SpeakDecision decision = new SpeakDecision();
		decision.setShouldSpeak(true);
		decision.setMessage(message);
		decision.setSpokenMessage(message.getMessage());
		
		return decision;
	}
}
