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
 * The contents of this are heavily based on this blog post:
 * http://blog.notdot.net/2010/05/Authenticating-against-App-Engine-from-an-Android-app
 * Thanks dude for your excellent writeup!
 */

package com.notifry.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

public class AppEngineAuthenticator extends Activity
{
	DefaultHttpClient httpClient = new DefaultHttpClient();
	
	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.app_info);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Intent intent = getIntent();
		AccountManager accountManager = AccountManager.get(getApplicationContext());
		Account account = (Account) intent.getExtras().get("account");
		accountManager.getAuthToken(account, "ah", false, new GetAuthTokenCallback(), null);
	}	

	private class GetAuthTokenCallback implements AccountManagerCallback
	{
		public void run( AccountManagerFuture result )
		{
			Bundle bundle;
			try
			{
				bundle = (Bundle) result.getResult();
				Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
				if( intent != null )
				{
					// User input required
					startActivity(intent);
				}
				else
				{
					onGetAuthToken(bundle);
				}
			}
			catch( OperationCanceledException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch( AuthenticatorException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	protected void onGetAuthToken( Bundle bundle )
	{
		String authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
		new GetCookieTask().execute(authToken);
	}

	private class GetCookieTask extends AsyncTask<String, String, Boolean>
	{
		protected Boolean doInBackground( String... tokens )
		{
			try
			{
				// Don't follow redirects
				httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);

				HttpGet httpGet = new HttpGet("https://" + getString(R.string.backend_url) + "/_ah/login?continue=http://localhost/&auth=" + tokens[0]);
				HttpResponse response;
				response = httpClient.execute(httpGet);
				if( response.getStatusLine().getStatusCode() != 302 )
				{
					// Response should be a redirect
					return false;
				}

				for( Cookie cookie : httpClient.getCookieStore().getCookies() )
				{
					if( cookie.getName().equals("ACSID") )
					{
						return true;
					}
				}
			}
			catch( ClientProtocolException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				httpClient.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
			}
			return false;
		}

		protected void onPostExecute( Boolean result )
		{
			new AuthenticatedRequestTask().execute("http://" + getString(R.string.backend_url) + "/admin/");
		}
	}

	private class AuthenticatedRequestTask extends AsyncTask<String, String, HttpResponse>
	{
		protected HttpResponse doInBackground( String... urls )
		{
			try
			{
				HttpGet http_get = new HttpGet(urls[0]);
				return httpClient.execute(http_get);
			}
			catch( ClientProtocolException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute( HttpResponse result )
		{
			try
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(result.getEntity().getContent()));
				String first_line = reader.readLine();
				Toast.makeText(getApplicationContext(), first_line, Toast.LENGTH_LONG).show();
			}
			catch( IllegalStateException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch( IOException e )
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
