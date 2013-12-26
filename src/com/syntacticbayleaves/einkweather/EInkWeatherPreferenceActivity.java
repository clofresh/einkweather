package com.syntacticbayleaves.einkweather;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class EInkWeatherPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {


    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        Log.i("prefs", "Changed preference: " + key);
        if ("location".equals(key)) {
            String apiKey = prefs.getString("apiKey", null);
            String location = prefs.getString("location", null);
            new WeatherApiQuery(this).execute("LOCATION", apiKey, location, "false");
        }
    }
}


