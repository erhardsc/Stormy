package com.sumnererhard.stormy;

import android.Manifest.permission;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private CurrentWeather mCurrentWeather;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    protected GoogleApiClient mGoogleApiClient;
    private double mLatitude; // need to make a shared preference out of these values
    private double mLongitude;
    private String mTimezone;
    private SunriseSunsetCalculator mCalculator;

    /**
     * Represents a geographical location.
     */
    protected android.location.Location mLastLocation;

    // Binding views with the ButterKnife view library
    @BindView(R.id.locationLabel) TextView mLocationLabel;
    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;
    @BindView(R.id.constraintLayout) ConstraintLayout mConstraintLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Add all the views using ButterKnife
        ButterKnife.bind(this);
        // Initialize JodaTime
        JodaTimeAndroid.init(this);
        permissionCheck();
        buildGoogleApiClient();

        mProgressBar.setVisibility(View.INVISIBLE);

//        final double latitude = 37.422;
//        final double longitude = -122.084;

        mRefreshImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getBackground();
                getForecast();
            }
        });

//        getBackground(latitude, longitude);
//        getForecast(latitude, longitude);

        Log.d(TAG, "Main UI code is running! Method");
    }

    private String getCityName() {
        String cityName = "";
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(mLatitude, mLongitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (addresses.size() > 0)
        {
            Log.d("Loaction: ", addresses.get(0).getLocality());
            cityName = addresses.get(0).getLocality();
        }
        else
        {
            Log.d("ERROR: ", "Could not find City Name");
        }
        return cityName;
    }

    private void permissionCheck() {
        Log.d("permissionCheck", "permissionCheck Method");
        int locationPermissionCheck = ContextCompat.checkSelfPermission(this,
                permission.ACCESS_COARSE_LOCATION);

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission.ACCESS_COARSE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_COARSE_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    private void getForecast() {
        Log.d("getForecast", "getForecast Method");
        String apiKey = "a9b2d44bd8115dc2976c8136299ee4e4";

        String forecastURL = "https://api.darksky.net/forecast/" + apiKey +
                "/" + mLatitude + "," + mLongitude;

        if(isNetworkAvailable()) {

            toggleRefresh();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastURL)
                    .build();
            Call call = client.newCall(request);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            // Updating the display out of the back-ground thread
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });

                        } else {
                            alertUserAboutError(getResources().getString(R.string.error_title),
                                    getResources().getString(R.string.error_message));
                        }
                    }catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }

                }
            });
        }else{
            alertUserAboutError("Oh no!"
                    ,getResources().getString(R.string.network_unavailable_message));
        }
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }else{
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {

        mLocationLabel.setText(getCityName());
        mTemperatureLabel.setText(String.format("%s", (int) mCurrentWeather.getTemperature()));
        mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it is");
        mHumidityValue.setText(String.format("%s", mCurrentWeather.getHumidity()));
        mPrecipValue.setText(String.format("%s", mCurrentWeather.getPrecipChance()));
        mSummaryLabel.setText(mCurrentWeather.getSummary());

        Drawable drawable = ResourcesCompat.getDrawable(getResources(), mCurrentWeather.getIconID(), null);
        mIconImageView.setImageDrawable(drawable);

    }

    public void getBackground(){
        Log.d("getBackground", "getBackground Method");

        DateTime dateTime = new DateTime();

        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.pref_timezone_key), Context.MODE_PRIVATE);

        mTimezone = getResources().getString(R.string.pref_timezone);
        Location location = new Location(mLatitude + "", mLongitude + "");
        mCalculator = new SunriseSunsetCalculator(location, mTimezone); // Can be updated dynamically
        String officialSunrise = mCalculator.getOfficialSunriseForDate(Calendar.getInstance());
        String officialSunset = mCalculator.getOfficialSunsetForDate(Calendar.getInstance());

        Log.d("Official Sunrise",officialSunrise + "");
        Log.d("Official Sunset",officialSunset + "");

        String sunriseFirstTwoDigits = officialSunrise.substring(0,2);
        String sunsetFirstTwoDigits = officialSunset.substring(0,2);
        int sunset = Integer.parseInt(sunriseFirstTwoDigits); // For some reason the officialSunrise variable returns the official sunrise
        int sunrise = Integer.parseInt(sunsetFirstTwoDigits);
        int currentHourOfDay = dateTime.getHourOfDay();

        Log.d("Sunrise",sunriseFirstTwoDigits + "");
        Log.d("Sunset",sunsetFirstTwoDigits + "");
        Log.d("Current Time", currentHourOfDay + "");

        if (sunrise > currentHourOfDay && sunset > currentHourOfDay || sunrise < currentHourOfDay && sunset < currentHourOfDay){
                mConstraintLayout.setBackgroundColor(Color.parseColor("#637A91")); // Dark Theme
        }
        getForecast();
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException{
        Log.d("getCurrentDetails", "getCurrentDetails Method");

        JSONObject forecast = new JSONObject(jsonData);

        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.pref_timezone_key), Context.MODE_PRIVATE);
        String timezone = forecast.getString("timezone");

        if (!timezone.equals(mTimezone)){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.apply(); // Handles the save in the background
            editor.putString(getString(R.string.pref_timezone_key), timezone);
            mTimezone = timezone;
            Log.i(TAG, "YOOOOO: " + "using shared preference");
        }else{
            mTimezone = timezone;
        }

        Log.i(TAG, "From JSON: " + mTimezone);

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(mTimezone);

        Log.d(TAG, currentWeather.getFormattedTime());

        return currentWeather;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError(String errorTitle, String errorMessage) {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.setErrorTitle(errorTitle);
        dialog.setErrorText(errorMessage);
        dialog.show(getFragmentManager(), "error_dialog");
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Log.d("buildGoogleApiClient", "buildGoogleApiClient Method");
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        Log.d("onStart", "onStart Method");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d("onConnected", "onConnected Method");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            Log.d("Lat", mLastLocation.getLatitude() + "");
            Log.d("Lat", mLastLocation.getLongitude() + "");

            mLatitude = mLastLocation.getLatitude();
            mLongitude = mLastLocation.getLongitude();

            getBackground();
        } else {
            Toast.makeText(this, "no location detected", Toast.LENGTH_LONG).show();
            //startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mGoogleApiClient.connect();
    }
}
