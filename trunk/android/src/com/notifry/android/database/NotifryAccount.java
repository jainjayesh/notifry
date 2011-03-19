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

import java.util.HashMap;

import com.notifry.android.remote.BackendRequest;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;

public class NotifryAccount
{
	private Long id = null;
	private String accountName = null;
	private Long serverRegistrationId = null;
	private Boolean enabled = null;
	private Boolean requiresSync = true;

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

	public Boolean getEnabled()
	{
		return enabled;
	}

	public void setEnabled( Boolean enabled )
	{
		this.enabled = enabled;
	}

	public void setServerRegistrationId( Long serverRegistrationId )
	{
		this.serverRegistrationId = serverRegistrationId;
	}

	public Long getServerRegistrationId()
	{
		return serverRegistrationId;
	}
	
	public Boolean getRequiresSync()
	{
		return requiresSync;
	}

	public void setRequiresSync( Boolean requiresSync )
	{
		this.requiresSync = requiresSync;
	}

	/**
	 * Register the device with the server.
	 * @param context
	 * @param key
	 * @param showStatus
	 */
	public void registerWithBackend( Context context, String key, boolean register, String statusMessage, Handler handler, HashMap<String, Object> metadata )
	{
		// Register the device with the server.
		BackendRequest request = new BackendRequest("/devices/register");
		request.add("devicekey", key);
		request.add("devicetype", "android");
		try
		{
			request.add("deviceversion", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
		}
		catch( NameNotFoundException e )
		{
			request.add("deviceversion", "Unknown");
		}
		
		// Send something so we know what the device is.
		request.add("nickname", Build.MODEL);

		// If already registered, update the same entry.
		if( this.getServerRegistrationId() != null )
		{
			request.add("id", this.getServerRegistrationId().toString());
		}

		if( register )
		{
			request.add("operation", "add");
		}
		else
		{
			request.add("operation", "remove");
		}

		// For debugging, dump the request data.
		//request.dumpRequest();
		
		// And the callback handler, if required.
		request.setHandler(handler);
		
		// Add any metadata if required.
		if( metadata != null )
		{
			for( String metaKey: metadata.keySet() )
			{
				request.addMeta(metaKey, metadata.get(metaKey));
			}
		}		
		
		// Start a thread to make the request.
		request.startInThread(context, statusMessage, this.getAccountName());
	}
}
