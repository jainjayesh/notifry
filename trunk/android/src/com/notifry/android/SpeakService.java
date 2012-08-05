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

import java.util.HashMap;
import java.util.Vector;

import android.speech.tts.TextToSpeech;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SpeakService extends Service implements SensorEventListener, TextToSpeech.OnInitListener
{
	private static final String TAG = "Notifry";
	private TextToSpeech tts = null;
	private Vector<String> queue = new Vector<String>();
	private boolean initialized = false;
	private boolean temporaryDisable = false;
	private SensorManager sensorMgr = null;
	private long lastUpdate = -1;
	private float x, y, z;
	private float last_x, last_y, last_z;
	private boolean shakeSensingOn = false;
	private SpeakService alternateThis = this;
	private int shakeThreshold = 1500;
	private HashMap<String, String> parameters = new HashMap<String, String>();

	@Override
	public IBinder onBind( Intent arg0 )
	{
		return null;
	}

	public void onInit( int status )
	{
		// Prepare parameters.
		this.parameters.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(this.getAudioStream()));
		
		// Ready.
		// Log.d("Notifry", "TTS init complete...");
		if( status == TextToSpeech.SUCCESS )
		{
			// Empty out the queue.
			synchronized( queue )
			{
				initialized = true;

				for( String message : queue )
				{
					// Log.d("Notifry", "Speaking queued message: " + message);
					tts.speak(message, TextToSpeech.QUEUE_ADD, this.parameters);
				}

				queue.clear();
			}
		}
		else
		{
			// Log.d("Notifry", "Init failure - probably missing data.");
		}
	}

	private Handler handler = new Handler()
	{
		public void handleMessage( Message msg )
		{
			// Log.d("Notifry", "Time delay message: " + msg.obj);

			synchronized( queue )
			{
				if( false == initialized )
				{
					queue.add((String) msg.obj);
				}
				else
				{
					tts.speak((String) msg.obj, TextToSpeech.QUEUE_ADD, parameters);
				}
			}
		}
	};

	private Handler sensorOffHandler = new Handler()
	{
		public void handleMessage( Message msg )
		{
			if( sensorMgr != null )
			{
				sensorMgr.unregisterListener(alternateThis);
				sensorMgr = null;
			}
			shakeSensingOn = false;
		}
	};

	@Override
	public void onCreate()
	{
		super.onCreate();

		// Listen to the phone call state.
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

		// Create the TTS object.
		this.tts = new TextToSpeech(this, this);
	}

	private int getAudioStream()
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		String desiredStream = settings.getString(getString(R.string.ttsAudioStream), "NOTIFICATION");
		int stream = AudioManager.STREAM_NOTIFICATION;
		if( desiredStream.equals("ALARM") )
		{
			stream = AudioManager.STREAM_ALARM;
		}
		else if( desiredStream.equals("MUSIC") )
		{
			stream = AudioManager.STREAM_MUSIC;
		}
		
		return stream;
	}

	@Override
	public void onStart( final Intent intent, int startId )
	{
		super.onStart(intent, startId);

		// Deal with the weird null intent issue.
		if( intent == null )
		{
			return;
		}
		
		if( temporaryDisable )
		{
			// Temporarily disabled - don't speak.
			// Log.d("Notifry", "IN CALL - NOT READING OUT");
			return;
		}

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

		boolean stopInstead = intent.getExtras().getBoolean("stopNow");
		int delaySend = intent.getExtras().getInt("delay");

		if( settings.getBoolean(getString(R.string.shakeToStop), false) )
		{
			// Shake to stop is on - kick off the listener for N seconds,
			// if not already running.
			if( !this.shakeSensingOn )
			{
				try
				{
					this.shakeThreshold = Integer.parseInt(settings.getString(getString(R.string.shakeThreshold), "1500"));
				}
				catch( NumberFormatException ex )
				{
					this.shakeThreshold = 1500;
				}
				Integer shakeWaitTime = 60;
				try
				{
					shakeWaitTime = Integer.parseInt(settings.getString(getString(R.string.shakeWaitTime), "60"));
				}
				catch( NumberFormatException ex )
				{
					// Invalid - ignore.
				}
				this.sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
				boolean accelSupported = this.sensorMgr.registerListener(
						this,
						this.sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_GAME,
						null);

				if( !accelSupported )
				{
					// no accelerometer on this device
					this.sensorMgr.unregisterListener(this);
					// Log.d("Notifry", "No acceleromters on this device.");
				}
				else
				{
					// Register a task to stop us in N seconds.
					this.shakeSensingOn = true;
					sensorOffHandler.sendMessageDelayed(Message.obtain(), shakeWaitTime * 1000);
				}
			}
		}

		if( stopInstead )
		{
			// Stop reading now.
			// Log.d("Notifry", "Got stop request... asking TTS to stop.");
			this.tts.stop();
		}
		else if( delaySend > 0 )
		{
			// Delay reading for a while.
			Message msg = Message.obtain();
			msg.obj = intent.getExtras().get("text");

			// Log.d("Notifry", "Delaying message for " + delaySend + " seconds.");
			handler.sendMessageDelayed(msg, delaySend * 1000);
		}
		else
		{
			// Send along the text.
			String text = intent.getExtras().getString("text");
			// Log.d("Notifry", "Got intent to read message: " + text);
			
			// Also change the audio stream if needed.
			this.parameters.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(this.getAudioStream()));

			// Why do we do this weird queue thing for the onInit call to work with?
			// That's because if we call tts.speak() before it's initialized nothing
			// happens. So the first message would 'disappear' and not be spoken.
			// So, we queue those until the onInit() has run, and onInit() then
			// clears the queue out.
			// Once it's initialized, we then speak it normally without adding
			// it to the queue.
			synchronized( this.queue )
			{
				if( false == this.initialized )
				{
					this.queue.add(text);
					// Log.d("Notifry", "Not initialised, so queueing.");
				}
				else
				{
					// Log.d("Notifry", "Initialized, reading out...");
					this.tts.speak(text, TextToSpeech.QUEUE_ADD, this.parameters);
				}
			}
		}
	}

	@Override
	public void onDestroy()
	{
		synchronized( this.queue )
		{
			this.tts.shutdown();
			this.initialized = false;
		}
	}

	public void onAccuracyChanged( Sensor sensor, int accuracy )
	{
		// Oh well!
	}

	public void onSensorChanged( SensorEvent event )
	{
		// This detection routine is heavily based on that found at:
		// http://www.codeshogun.com/blog/2009/04/17/how-to-detect-shake-motion-in-android-part-i/
		// Log.d("Notifry", "onSensorChanged: " + event);
		// Log.d("Notifry", "type " + event.sensor.getType() + " wanted " +
		// Sensor.TYPE_ACCELEROMETER);
		// Log.d("Notifry", "data " + event.values[SensorManager.DATA_X] + " " +
		// event.values[SensorManager.DATA_Y] + " " +
		// event.values[SensorManager.DATA_Z]);
		if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER )
		{
			long curTime = System.currentTimeMillis();
			// only allow one update every 100ms.
			if( (curTime - lastUpdate) > 100 )
			{
				long diffTime = (curTime - lastUpdate);
				lastUpdate = curTime;

				x = event.values[SensorManager.DATA_X];
				y = event.values[SensorManager.DATA_Y];
				z = event.values[SensorManager.DATA_Z];

				float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

				// Log.d("Notifry", "diff: " + diffTime + " - speed: " + speed);
				if( speed > this.shakeThreshold )
				{
					// Log.d("Notifry", "Shake detected with speed: " + speed);

					// Tell this service to stop.
					Intent intentData = new Intent(getBaseContext(), SpeakService.class);
					intentData.putExtra("stopNow", true);
					startService(intentData);

					// Stop detected shake.
					sensorOffHandler.sendMessageDelayed(Message.obtain(), 0);
				}
				last_x = x;
				last_y = y;
				last_z = z;
			}
		}
	}

	// Based on
	// http://www.androidsoftwaredeveloper.com/2009/04/20/how-to-detect-call-state/
	private PhoneStateListener mPhoneListener = new PhoneStateListener()
	{
		public void onCallStateChanged( int state, String incomingNumber )
		{
			switch( state )
			{
				case TelephonyManager.CALL_STATE_RINGING:
				case TelephonyManager.CALL_STATE_OFFHOOK:
					// Stop any speaking now!
					tts.stop();
					// And temporarily disable.
					temporaryDisable = true;
					// Log.d("Notifry", "DISABLING - In call state.");
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					// Re-enable the service.
					// Log.d("Notifry", "Enabling temp disable again...");
					temporaryDisable = false;
					break;
				default:
					break;
			}
		}
	};
}
