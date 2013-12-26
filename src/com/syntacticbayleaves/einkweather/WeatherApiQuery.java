package com.syntacticbayleaves.einkweather;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;

public class WeatherApiQuery extends AsyncTask<String, Integer, Pair<WeatherApiMethod, JSONObject>> {
    private static final long MAX_CACHE_AGE = 3600000;
    private Activity activity;
    private ProgressDialog progress;

    public WeatherApiQuery(Activity activity) {
        this.activity = activity;
        this.progress = new ProgressDialog(this.activity);
        this.progress.setMessage("Loading...");
        this.progress.setCancelable(true);
        this.progress.show();
    }

    @Override
    protected Pair<WeatherApiMethod, JSONObject> doInBackground(String... args) {
        try {
            WeatherApiMethod method = WeatherApiMethod.valueOf(args[0]);
            String apiKey = args[1];
            String location = args[2];
            boolean useCached = args[3] == "true";
            Log.i("api", String.format("Received WeatherApiQuery for %s", method));
            switch (method) {
                case WEATHER:
                    return Pair.create(method, queryWeather(apiKey, location, useCached, this.activity));
                case LOCATION:
                    return Pair.create(method, queryLocation(apiKey, location, useCached, this.activity));
                default:
                    Log.e("api", "Invalid method: " + method);
                    return null;
            }
        } catch (WeatherApiException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(Pair<WeatherApiMethod, JSONObject> asyncResponse) {
        try {
            if (asyncResponse != null) {
                JSONObject apiResponse = asyncResponse.second;
                switch(asyncResponse.first) {
                    case WEATHER:
                        populateWeather(apiResponse);
                        break;
                    case LOCATION:
                        populateLocation(apiResponse);
                        break;
                    default:
                        Log.e("api", "Invalid WeatherApiMethod: " + asyncResponse.first.toString());
                }
            }
        } finally {
            this.progress.dismiss();
        }
    }

    private void populateWeather(JSONObject apiResponse) {
        try {
            // Get the temperature and weather code
            JSONArray currentConditions = apiResponse.getJSONArray("current_condition");
            JSONObject currentCondition = currentConditions.getJSONObject(0);
            String tempF = currentCondition.getString("temp_F");
            String weatherCode = currentCondition.getString("weatherCode");
            JSONArray weatherDescs = currentCondition.getJSONArray("weatherDesc");
            String weatherDesc = weatherDescs.getJSONObject(0).getString("value");

            // Parse the date and time of the weather sample
            String observationTime = currentCondition.getString("observation_time");
            JSONArray weathers = apiResponse.getJSONArray("weather");
            JSONObject weather = weathers.getJSONObject(0);
            String observationDate = weather.getString("date");
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-ddhh:mm a", Locale.US);
            df.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date observationDateTime = df.parse(observationDate + observationTime);

            // Render the data to the UI
            ((MainActivity) activity).renderWeather(tempF, weatherCode, weatherDesc, observationDateTime);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void populateLocation(JSONObject apiResponse) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            JSONObject location = (JSONObject) apiResponse.getJSONArray("result").get(0);
            String areaName = ((JSONObject) location.getJSONArray("areaName").get(0)).getString("value");
            String region = ((JSONObject) location.getJSONArray("region").get(0)).getString("value");
            float tzOffset = Float.parseFloat(((JSONObject) location.getJSONArray("timezone").get(0)).getString("offset"));
            int tzOffsetInMinutes = (int) (tzOffset * 60.0);
            int tzOffsetHours = tzOffsetInMinutes / 60;
            int tzOffsetMinutes = tzOffsetInMinutes % 60;
            StringBuilder tzStr = new StringBuilder("GMT");
            if (tzOffsetInMinutes > 0) {
                tzStr.append("+");
            } else {
                tzStr.append("-");
            }
            tzStr.append(String.valueOf(Math.abs(tzOffsetHours)));
            tzStr.append(String.valueOf(String.format("%02d", tzOffsetMinutes)));
            String timeZone = tzStr.toString();
            String locationName = areaName + ", " + region;
            Log.i("prefs", String.format("Writing timeZone: %s, location: %s", timeZone, locationName));
            editor.putString("timeZone", timeZone);
            editor.putString("locationName", locationName);
            editor.commit();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static JSONObject queryWeather(String apiKey, String location, boolean useCached, Context context) throws WeatherApiException {
        // Build the url
        URL url;
        try {
            url = new URL("http://api.worldweatheronline.com/free/v1/weather.ashx?format=json&num_of_days=1&key=" + apiKey + "&q=" + URLEncoder.encode(location, "UTF-8"));
        } catch (MalformedURLException e) {
            throw new WeatherApiException(e);
        } catch (UnsupportedEncodingException e) {
            throw new WeatherApiException(e);
        }
        JSONObject response = makeHttpRequest(url, useCached ? getCacheDir(context) : null);
        try {
            return response.getJSONObject("data");
        } catch (JSONException e) {
            throw new WeatherApiException(e);
        }
    }

    public static JSONObject queryLocation(String apiKey, String location, boolean useCached, Context context) throws WeatherApiException {
        // Build the url
        URL url;
        try {
            url = new URL("http://api.worldweatheronline.com/free/v1/search.ashx?format=json&timezone=yes&key=" + apiKey + "&query=" + URLEncoder.encode(location, "UTF-8"));
        } catch (MalformedURLException e) {
            throw new WeatherApiException(e);
        } catch (UnsupportedEncodingException e) {
            throw new WeatherApiException(e);
        }
        JSONObject response = makeHttpRequest(url, useCached ? getCacheDir(context) : null);
        try {
            return response.getJSONObject("search_api");
        } catch (JSONException e) {
            throw new WeatherApiException(e);
        }
    }

    private static File getCacheDir(Context context) {
        return new File(context.getCacheDir(), "json");
    }

    private static JSONObject makeHttpRequest(URL url, File cacheDir) throws WeatherApiException {
        boolean noFile, useCached;
        File cachedJson;
        if (cacheDir == null) {
            noFile = true;
            useCached = false;
            cachedJson = null;
        } else {
            // Check if there is a cached value for this url
            String cacheFile = new String(Hex.encodeHex(DigestUtils.md5(url.toString().getBytes())));
            cacheDir.mkdirs();
            cachedJson = new File(cacheDir, cacheFile);
            noFile = !cachedJson.exists();
            useCached = true;
        }

        boolean tooOld = false;
        long cacheAge = -1;
        if (!noFile && cachedJson != null) {
            cacheAge = System.currentTimeMillis() - cachedJson.lastModified();
            tooOld = cacheAge > WeatherApiQuery.MAX_CACHE_AGE;
        }
        String response;
        if (noFile || tooOld || !useCached) {
            if (!useCached) {
                Log.i("cache", "Forced refresh");
            } else if (tooOld) {
                Log.i("cache", "Cached response exists but is too old");
            }
            try {
                // Make the http request and read the response
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    response = IOUtils.toString(in);
                } finally {
                    urlConnection.disconnect();
                }

                if (cachedJson != null) {
                    // Write fetched data to cache file
                    FileOutputStream outputStream = new FileOutputStream(cachedJson.toString());
                    try {
                      outputStream.write(response.getBytes());
                    } finally {
                      outputStream.close();
                    }
                }
            } catch (IOException e) {
                Log.e("api", "Could not connect to " + url.toString());
                throw new WeatherApiException(e);
            }
            Log.i("json", "Fetched from " + url.toString() + ":\n" + response);
        } else {
            try {
                response = FileUtils.readFileToString(cachedJson);
            } catch (IOException e) {
                Log.e("cache", "Could not read from cache file: " + cachedJson.toString());
                throw new WeatherApiException(e);
            }
            Log.i("json", "Using " + String.valueOf(cacheAge) + "ms cached response from " + cachedJson.toString() + ":\n" + response);
        }

        // Marshall the json string into an object and return
        JSONObject object;
        try {
            object = (JSONObject) new JSONTokener(response).nextValue();
        } catch (JSONException e) {
            throw new WeatherApiException(e);
        }
        return object;
    }

}
