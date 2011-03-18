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
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.notifry.android.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BackendRequest
{
	private static final String TAG = "Notifry";

	final private List<NameValuePair> params = new ArrayList<NameValuePair>();
	final private String uri;
	final private BackendRequest thisRequest = this;
	final private HashMap<String, Object> requestMeta = new HashMap<String, Object>();
	private ProgressDialog dialog = null;
	private Handler responseHandler = null;

	/**
	 * Construct a BackendRequest object for the given URI.
	 * @param uri
	 */
	public BackendRequest( String uri )
	{
		this.uri = uri;
	}
	
	/**
	 * Set the optional handler to post to when the request is complete.
	 * @param completeHandler
	 */
	public void setHandler( Handler completeHandler )
	{
		this.responseHandler = completeHandler;
	}

	/**
	 * Add a parameter to this request.
	 * @param name
	 * @param value
	 */
	public void add( String name, String value )
	{
		this.params.add(new BasicNameValuePair(name, value));
	}

	/**
	 * Add metadata to this request - this is for the final handler
	 * to be able to get the context and deal with the request.
	 * @param name
	 * @param object
	 */
	public void addMeta( String name, Object object )
	{
		this.requestMeta.put(name, object);
	}

	/**
	 * For debugging, dump the name/value pairs for this request.
	 */
	public void dumpRequest()
	{
		Log.d(TAG, "URI: " + this.uri);
		for( NameValuePair param : this.params )
		{
			Log.d(TAG, "Param: " + param.getName() + " = " + param.getValue());
		}
	}

	/**
	 * Get the URI for this request.
	 * @return
	 */
	public String getUri()
	{
		return this.uri;
	}

	/**
	 * Get all the name value pairs for this request.
	 * @return
	 */
	public List<NameValuePair> getParams()
	{
		return this.params;
	}

	/**
	 * Get a piece of metadata. Returns NULL if not found.
	 * @param key
	 * @return
	 */
	public Object getMeta( String key )
	{
		return this.requestMeta.get(key);
	}

	/**
	 * The intermediary handler to turn off the progress dialog, if it exists.
	 */
	private Handler handler = new Handler()
	{
		@Override
		public void handleMessage( Message msg )
		{
			// Turn off the dialog, if it was on.
			if( dialog != null )
			{
				dialog.dismiss();
				dialog = null;
			}
			
			// Now forward this message to the result handler, if required.
			if( responseHandler != null )
			{
				Message newMessage = Message.obtain();
				newMessage.obj = msg.obj;
				responseHandler.sendMessage(newMessage);
			}
		}
	};

	/**
	 * Fire off this request in a thread in the background.
	 * @param context The context for this request.
	 * @param statusMessage If provided, show a progress dialog with this message. If NULL, no progress dialog is shown.
	 * @param accountName The account name to perform the request under.
	 */
	public void startInThread( final Context context, final String statusMessage, final String accountName )
	{
		// Set up the dialog.
		if( statusMessage != null )
		{
			this.dialog = ProgressDialog.show(context, context.getString(R.string.app_name), statusMessage, true);
		}

		// Create a new thread.
		Thread thread = new Thread()
		{
			public void run()
			{
				// Create a client.
				BackendClient client = new BackendClient(context, accountName);

				BackendResponse result;
				try
				{
					//Log.i(TAG, "Beginning request...");
					result = client.request(thisRequest);

					// Was it successful?
					/*if( result.isError() )
					{
						Log.e(TAG, "Error: " + result.getError());
					}
					else
					{
						Log.e(TAG, "Success! Server returned: " + result.getJSON().toString());
					}*/

					// Prepare the message to send back.
					Message message = Message.obtain();
					message.obj = result;
					
					// Post it to ourselves - it will then post it back to the real thread.
					handler.sendMessage(message);
				}
				catch( Exception e )
				{
					Log.e(TAG, "Generic exception: " + e.getMessage() + " of type " + e.getClass().toString());
				}
			}
		};

		// And start the request.
		thread.start();
	}
}
