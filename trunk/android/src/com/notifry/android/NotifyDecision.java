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

public class NotifyDecision
{
	private Boolean shouldNotify;
	private NotifryMessage message;

	public Boolean getShouldNotify()
	{
		return shouldNotify;
	}

	public void setShouldNotify( Boolean shouldNotify )
	{
		this.shouldNotify = shouldNotify;
	}

	public NotifryMessage getMessage()
	{
		return message;
	}

	public void setMessage( NotifryMessage message )
	{
		this.message = message;
	}
	
	public String getSpokenMessage()
	{
		return this.message.getTitle() + ". " + this.message.getMessage();
	}

	/**
	 * Determine if we should notify about this message or now.
	 * 
	 * @param context
	 * @param message
	 * @return
	 */
	public static NotifyDecision shouldNotify( Context context, NotifryMessage message )
	{
		NotifyDecision decision = new NotifyDecision();
		decision.setShouldNotify(message.getSource().getLocalEnabled());
		decision.setMessage(message);

		return decision;
	}
}
