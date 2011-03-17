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

public class NotifryAccount
{
	private Long id = null;
	private String accountName = null;
	private Long serverRegistrationId = null;
	private Boolean enabled = null;

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
}
