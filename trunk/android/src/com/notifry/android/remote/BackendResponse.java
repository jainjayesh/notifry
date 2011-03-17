package com.notifry.android.remote;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class BackendResponse
{
	private static final String TAG = "Notifry";

	private HttpResponse response;
	private String error = null;
	private JSONObject json;
	private BackendRequest request;

	public BackendResponse( BackendRequest request, HttpResponse response )
	{
		this.request = request;
		this.response = response;

		// Determine if it was an error.
		if( response.getStatusLine().getStatusCode() != 200 )
		{
			// There was an error.
			this.error = "" + response.getStatusLine().getStatusCode() + response.getStatusLine().getReasonPhrase();
		}
		else
		{
			this.parseJson();
		}
	}

	public void parseJson()
	{
		// Parse the JSON from the body.
		String jsonBody = "";

		// Step 1: read it out from the input stream.
		try
		{
            int ch;
            StringBuffer sb = new StringBuffer();
            InputStream is = this.response.getEntity().getContent();
            while ((ch = is.read()) != -1)
            {
                    sb.append((char) ch);
            }
            jsonBody = sb.toString();
            is.close();
		}
		catch( IOException ex )
		{
			// Failed to read the response.
			this.error = "Failed to read the HTTP response.";
			Log.e(BackendResponse.TAG, ex.toString());
			return;
		}

		// Step 2: parse.
		try
		{
			JSONObject jsonData = new JSONObject(jsonBody);
			
			// See if there was an error in it.
			if( jsonData.has("error") )
			{
				// Server error. Note it!
				this.error = jsonData.getString("error");
			}
			
			this.json = jsonData;
		}
		catch( JSONException ex )
		{
			// Failed to parse the JSON.
			this.error = "Failed to parse the JSON from the server.";
			Log.e(BackendResponse.TAG, "Failed to parse JSON response: " + jsonBody);
		}
	}

	public BackendResponse( String error )
	{
		// Another kind of error.
		this.error = error;
	}

	public boolean isError()
	{
		return this.error != null;
	}
	
	public JSONObject getJSON()
	{
		return this.json;
	}
	
	public HttpResponse getHttpResponse()
	{
		return this.response;
	}
	
	public String getError()
	{
		return this.error;
	}
	
	public BackendRequest getRequest()
	{
		return this.request;
	}
}
