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

package com.notifry.android.remote;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.util.Log;

public class BackendRequest
{
	private static final String TAG = "Notifry";
	
	final private List<NameValuePair> params = new ArrayList<NameValuePair>();
	final private String uri;
	final private BackendRequest thisRequest = this;
	
	public BackendRequest( String uri )
	{
		this.uri = uri;
	}
	
	public void add( String name, String value )
	{
		this.params.add(new BasicNameValuePair(name, value));
	}
	
	public String getUri()
	{
		return this.uri;
	}
	
	public List<NameValuePair> getParams()
	{
		return this.params;
	}
	
	public void startInThread( final Context context, final String statusMessage, final String accountName )
	{
		Thread thread = new Thread(){
			public void run()
			{
				BackendClient client = new BackendClient(context, accountName);
				
				BackendResponse result;
				try
				{
					Log.i(TAG, "Beginning request...");
					result = client.request(thisRequest, statusMessage);

					// Was it successful?
					if( result.isError() )
					{
						Log.e(TAG, "Error: " + result.getError());
					}
					else
					{
						Log.e(TAG, "Success! Server returned: " + result.getJSON().toString());
					}
				}
				catch( Exception e )
				{
					Log.e(TAG, "Generic exception: " + e.getMessage() + " of type " + e.getClass().toString());
				}
			}
		};
		
		thread.start();
	}
}
