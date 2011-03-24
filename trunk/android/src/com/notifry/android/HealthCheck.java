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

import java.util.ArrayList;
import java.util.HashMap;

import com.google.android.c2dm.C2DMessaging;
import com.notifry.android.database.NotifryAccount;
import com.notifry.android.database.NotifrySource;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class HealthCheck
{
	public ArrayList<String> errors = new ArrayList<String>();
	public ArrayList<String> warnings = new ArrayList<String>();
	
	protected void addError( String error )
	{
		this.errors.add(error);
	}
	
	protected void addWarning( String warning )
	{
		this.warnings.add(warning);
	}
	
	public ArrayList<String> getErrors()
	{
		return this.errors;
	}
	
	public ArrayList<String> getWarnings()
	{
		return this.warnings;
	}
	
	public boolean healthy()
	{
		return (this.errors.size() == 0);
	}
	
	public static HealthCheck performHealthcheck( Context context )
	{
		// Get ready...
		HealthCheck check = new HealthCheck();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		
		// Do we have a C2DM ID?
		final String registrationId = C2DMessaging.getRegistrationId(context);
		if( registrationId == null || "".equals(registrationId) )
		{
			check.addError(context.getString(R.string.health_error_no_c2dm_id));
		}
		
		// Was there an error getting the C2DM ID?
		String c2dmError = settings.getString("dm_register_error", "");
		if( !c2dmError.equals("") )
		{
			check.addError(context.getString(R.string.health_error_c2dm_error) + c2dmError);
		}
		
		// Are we registered to any backends?
		boolean hasBackend = false;
		ArrayList<NotifryAccount> accounts = NotifryAccount.FACTORY.listAll(context);
		HashMap<String, Integer> sourceCounts = new HashMap<String, Integer>();
		for( NotifryAccount account: accounts )
		{
			if( account.getEnabled() )
			{
				hasBackend = true;
				sourceCounts.put(account.getAccountName(), NotifrySource.FACTORY.countSources(context, account.getAccountName()));
				
				// Does the key differ from what we last told the server?
				if( !account.getLastC2DMId().equals(registrationId) )
				{
					check.addError(String.format(context.getString(R.string.health_error_account_wrong_id), account.getAccountName()));
				}
			}
		}
		
		if( !hasBackend )
		{
			check.addError(context.getString(R.string.health_error_no_accounts));
		}
		else
		{
			// For any enabled accounts, if there are no sources, let the user know.
			// This is a warning though.
			for( String accountName: sourceCounts.keySet() )
			{
				if( sourceCounts.get(accountName) == 0 )
				{
					String rawString = context.getString(R.string.health_error_no_sources);
					check.addWarning(String.format(rawString, accountName));
				}
			}
		}

		return check;
	}
}
