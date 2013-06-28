package com.mutanti.vatman;

import java.util.List;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

public class VatmanPreferences extends PreferenceActivity {

	public static final String KEY_MY_PREFERENCE = "com.mutanti.vatman.preferences";
	public static final String KEY_PREF_GPS = "gps";
	public static final String KEY_PREF_ORIENTATION = "orientation";
	public static final String KEY_PREF_USERNAME = "username";
	public static final String KEY_PREF_PASSWORD = "password";
	public static final String KEY_PREF_STOP_VERSION = "stopVersion";
	public static final String KEY_PREF_VIEW_MODE = "view_mode";
	public static final String KEY_PREF_GLOBAL_VIEW_MODE = "global_view_mode";
	public static final String KEY_PREF_SAVE_ZOOM_LEVEL = "save_zoom_level";
	public static final String KEY_PREF_LAST_ZOOM_LEVEL = "last_zoom_level";
	public static final String KEY_PREF_STOPS_TABLE_VERSION = "stops_table_version";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		setResult(RESULT_OK);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return true;
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.preference_headers, target);
	}

	public static class PrefsFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// setResult(RESULT_OK);

			addPreferencesFromResource(R.xml.preferences);
			Preference stopVersion = findPreference(KEY_PREF_STOP_VERSION
					+ "_display");
			stopVersion.setSummary(""
					+ getPreferenceScreen().getSharedPreferences().getLong(
							KEY_PREF_STOP_VERSION, 0));

		}

		@Override
		public void onStart() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onStop() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);

		}

		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (key.equals(KEY_PREF_STOP_VERSION)) {
				Preference stopVersion = findPreference(KEY_PREF_STOP_VERSION
						+ "_display");
				stopVersion.setSummary(""
						+ sharedPreferences.getLong(KEY_PREF_STOP_VERSION, 0));
			}
		}
	}

}
