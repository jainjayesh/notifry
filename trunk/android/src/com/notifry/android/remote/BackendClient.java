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
 * 
 * This file is based off Google's Chrome to Phone code, although is quite
 * different.
 * 
 * See http://code.google.com/p/chrometophone/source/browse/trunk/android/src/com/google/android/apps/chrometophone/AppEngineClient.java
 */

package com.notifry.android.remote;

import com.notifry.android.R;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class BackendClient
{
	private static final String TAG = "Notifry";

	private Context context;
	private String accountName;
	private String backendName;

	public BackendClient( Context context, String accountName )
	{
		this.context = context;
		this.accountName = accountName;
		this.backendName = "https://" + context.getString(R.string.backend_url);
	}

	public BackendResponse request( BackendRequest request, String statusMessage ) throws Exception
	{
		// Add a 'format=json' to the params.
		request.add("format", "json");
		
		ProgressDialog dialog = null;
		
		if( statusMessage != null )
		{
			dialog = ProgressDialog.show(this.context, this.context.getString(R.string.app_name), statusMessage, true);    
		}
		
		HttpResponse res = requestNoRetry(request.getUri(), request.getParams(), false);
		if( res.getStatusLine().getStatusCode() == 500 )
		{
			res = requestNoRetry(request.getUri(), request.getParams(), true);
		}
		
		// Parse the response.
		BackendResponse response = new BackendResponse(request, res);
		
		if( statusMessage != null )
		{
			dialog.dismiss();
		}
		
		return response;
	}

	private HttpResponse requestNoRetry( String urlPath, List<NameValuePair> params, boolean newToken ) throws Exception
	{
		// Get auth token for account
		Account account = new Account(this.accountName, "com.google");
		String authToken = getAuthToken(this.context, account);
		if( authToken == null )
		{
			throw new PendingAuthException(this.accountName);
		}
		if( newToken )
		{
			// invalidate the cached token
			AccountManager accountManager = AccountManager.get(this.context);
			accountManager.invalidateAuthToken(account.type, authToken);
			authToken = this.getAuthToken(this.context, account);
		}

		// Get ACSID cookie
		DefaultHttpClient client = new DefaultHttpClient();
		String continueURL = this.backendName;
		URI uri = new URI(this.backendName + "/_ah/login?continue=" +
				URLEncoder.encode(continueURL, "UTF-8") +
				"&auth=" + authToken);
		HttpGet method = new HttpGet(uri);
		final HttpParams getParams = new BasicHttpParams();
		HttpClientParams.setRedirecting(getParams, false); // continue is not
															// used
		method.setParams(getParams);

		HttpResponse res = client.execute(method);
		Header[] headers = res.getHeaders("Set-Cookie");
		if( res.getStatusLine().getStatusCode() != 302 ||
				headers.length == 0 )
		{
			return res;
		}

		String ascidCookie = null;
		for( Header header : headers )
		{
			if( header.getValue().indexOf("ACSID=") >= 0 )
			{
				// let's parse it
				String value = header.getValue();
				String[] pairs = value.split(";");
				ascidCookie = pairs[0];
			}
		}

		// Make POST request
		uri = new URI(this.backendName + urlPath);
		HttpPost post = new HttpPost(uri);
		UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
		post.setEntity(entity);
		post.setHeader("Cookie", ascidCookie);
		post.setHeader("X-Same-Domain", "1"); // XSRF
		res = client.execute(post);
		return res;
	}

	private String getAuthToken( Context context, Account account )
	{
		String authToken = null;
		AccountManager accountManager = AccountManager.get(context);
		try
		{
			AccountManagerFuture<Bundle> future = accountManager.getAuthToken(account, "ah", false, null, null);
			Bundle bundle = future.getResult();
			authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
			// User will be asked for "App Engine" permission.
			if( authToken == null )
			{
				// No auth token - will need to ask permission from user.
				//Intent intent = new Intent(SetupActivity.AUTH_PERMISSION_ACTION);
				//intent.putExtra("AccountManagerBundle", bundle);
				//context.sendBroadcast(intent);

				Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
				if( intent != null )
				{
					// User input required
					context.startActivity(intent);
				}
			}
		}
		catch( OperationCanceledException e )
		{
			Log.w(TAG, e.getMessage());
		}
		catch( AuthenticatorException e )
		{
			Log.w(TAG, e.getMessage());
		}
		catch( IOException e )
		{
			Log.w(TAG, e.getMessage());
		}
		return authToken;
	}

	public class PendingAuthException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public PendingAuthException( String message )
		{
			super(message);
		}
	}
}