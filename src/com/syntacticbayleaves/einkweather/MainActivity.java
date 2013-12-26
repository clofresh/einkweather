package com.syntacticbayleaves.einkweather;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
    public static final String DEG_C = "\u2103";
    public static final String DEG_F = "\u2109";
    public static final String timeZone = "America/New_York";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        refreshData(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshData(true);
    }

    public void refreshData(boolean useCached) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String apiKey = sharedPrefs.getString("apiKey", "");
        String location = sharedPrefs.getString("location", "");

        // AsyncTask to query the weather api and pass the result to renderWeather()
        new WeatherApiQuery(this).execute("WEATHER", apiKey, location, useCached ? "true" : "false");
    }

    public void renderWeather(String tempF, String weatherCode, String weatherDesc, Date observationTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        TextView locationView = (TextView) findViewById(R.id.location);
        locationView.setText(prefs.getString("locationName", ""));

        ImageView image = (ImageView) findViewById(R.id.weatherIcon);
        image.setImageResource(getWeatherImageId(weatherCode, observationTime));

        TextView temperature = (TextView) findViewById(R.id.temperature);
        temperature.setText(tempF + MainActivity.DEG_F);

        TextView desc = (TextView) findViewById(R.id.weatherDesc);
        desc.setText(weatherDesc);

        TextView datetime = (TextView) findViewById(R.id.datetime);
        SimpleDateFormat df = new SimpleDateFormat("'As of' hh:mm a 'on' MMM d", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone(prefs.getString("timeZone", "GMT")));
        datetime.setText(df.format(observationTime));
    }

    private int getWeatherImageId(String weatherCode, Date observationTime) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // Determine if it's day or night
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone(prefs.getString("timeZone", "GMT")));
        cal.setTime(observationTime);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String timeOfDay;
        if (hour >= 6 && hour < 18) {
            timeOfDay = "d";
        } else {
            timeOfDay = "n";
        }
        String imageName = "icon_" + weatherCode + "_" + timeOfDay;
        Log.i("render", "Rendering " + imageName);
        int imageId = getResources().getIdentifier(imageName, "drawable", "com.syntacticbayleaves.einkweather");
        return imageId;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.refresh:
                refreshData(false);
                return true;
            case R.id.preferences:
                Intent intent = new Intent(this, EInkWeatherPreferenceActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
