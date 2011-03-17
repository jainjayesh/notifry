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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;

public class Settings extends PreferenceActivity
{
	private EditTextPreference delayReading;
	private EditTextPreference shakeThreshold;
	private EditTextPreference shakeWaitTime;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		Preference stopReadingNow = findPreference(getString(R.string.stopReadingNow));
		stopReadingNow.setOnPreferenceClickListener(stopSpeakingNowHandler);
		
		Preference testAccount = findPreference(getString(R.string.testAccount));
		testAccount.setOnPreferenceClickListener(testAccountHandler);

		Preference previewSpeech = findPreference(getString(R.string.previewSpeech));
		previewSpeech.setOnPreferenceClickListener(previewSpeechHandler);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		delayReading = (EditTextPreference) findPreference(getString(R.string.delayReadingTime));
		delayReading.setOnPreferenceChangeListener(delayReadingHandler);
		updateDelaySummary(settings.getString(getString(R.string.delayReadingTime), "0"));
		
		shakeThreshold = (EditTextPreference) findPreference(getString(R.string.shakeThreshold));
		shakeThreshold.setOnPreferenceChangeListener(shakeThresholdHandler);
		updateThresholdSummary(settings.getString(getString(R.string.shakeThreshold), "1500"));
		
		shakeWaitTime = (EditTextPreference) findPreference(getString(R.string.shakeWaitTime));
		shakeWaitTime.setOnPreferenceChangeListener(shakeWaitTimeHandler);
		updateShakeWaitTimeSummary(settings.getString(getString(R.string.shakeWaitTime), "60"));		
	}
	
	// On click handler for stopping the text in motion.
	OnPreferenceClickListener stopSpeakingNowHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			Intent intentData = new Intent(getBaseContext(), SpeakService.class);
			intentData.putExtra("stopNow", true);
			startService(intentData);
			return true;
		}
	};
	
	// On click handler for firing off the test account handler.
	OnPreferenceClickListener testAccountHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			Intent intentData = new Intent(getBaseContext(), ChooseAccount.class);
			startActivity(intentData);
			return true;
		}
	};	

	// On click handler for previewing speech.
	OnPreferenceClickListener previewSpeechHandler = new OnPreferenceClickListener()
	{
		public boolean onPreferenceClick(Preference preference)
		{
			Intent intentData = new Intent(getBaseContext(), SpeakService.class);
			intentData.putExtra("text", getString(R.string.preview_speak));
			startService(intentData);
			return true;
		}
	};

	// On Preference change listener to update the delay summary.
	OnPreferenceChangeListener delayReadingHandler = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			updateDelaySummary((String) newValue);
			return true;
		}
	};

	// On Preference change listener to update the delay summary.
	OnPreferenceChangeListener shakeThresholdHandler = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			updateThresholdSummary((String) newValue);
			return true;
		}
	};	

	// On Preference change listener to update the delay summary.
	OnPreferenceChangeListener shakeWaitTimeHandler = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			updateShakeWaitTimeSummary((String) newValue);
			return true;
		}
	};

	// Helper function to update the delay summary.
	private void updateDelaySummary(String value)
	{
		String template = getString(R.string.delay_readout_summary);

		try
		{
			Integer intValue = Integer.parseInt(value);
			String plural = "s";

			if (intValue == 1)
			{
				plural = "";
			}
			String result = String.format(template, intValue, plural);
			delayReading.setSummary(result);
		}
		catch( NumberFormatException ex )
		{
			// Not a valid number... ignore.
		}
	}
	
	// Helper function to update the threshold summary.
	private void updateThresholdSummary(String value)
	{
		String template = getString(R.string.shakethreshhold_summary);

		try
		{
			Integer intValue = Integer.parseInt(value);
			String result = String.format(template, intValue);
			shakeThreshold.setSummary(result);
		}
		catch( NumberFormatException ex )
		{
			// Not a valid number... ignore.
		}
	}
	
	// Helper function to update the wait time summary.
	private void updateShakeWaitTimeSummary(String value)
	{
		String template = getString(R.string.shakewaittime_summary);

		try
		{
			Integer intValue = Integer.parseInt(value);
			String result = String.format(template, intValue);
			shakeWaitTime.setSummary(result);
		}
		catch( NumberFormatException ex )
		{
			// Not a valid number... ignore.
		}
	}
}
