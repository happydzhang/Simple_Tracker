package com.brianmannresearch.simple_tracker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import static android.icu.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {


    // Play around with update interval - add ability for user to modify update interval
    // implement map feature (i.e. place points on google map)
    // begin work on server feature

    private static final int LOCATION_REQUEST = 1, STORAGE_REQUEST = 2;
    public static final long UPDATE_INTERVERAL_IN_MILLISECONDS = 1500;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    String[] files, filename;
    int tripnumber = 1;
    String Filename;
    FileOutputStream fos;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    protected Button startButton, endButton, exitButton;
    protected TextView LatitudeTextView, LongitudeTextView, TimeTextView;
    protected Boolean mRequestingLocationUpdates;
    protected String mLastUpdateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check if location services are enabled on the device
        if (!isLocationEnabled(this)) {
            showSettingsAlert();
        }

        // check if application has permission to use location services
        // if not, request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
        }
        // check if application has permission to access public storage
        // if not, request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_REQUEST);
            }
        }

        // check for previous trips in order to know which trip number should be instantiated
        files = fileList();
        for (String file : files) {
            filename = file.split("/");
            if (filename[filename.length - 1].matches("Trip_\\d*")) {
                tripnumber++;
            }
        }

        startButton = (Button) findViewById(R.id.start_button);
        endButton = (Button) findViewById(R.id.end_button);
        exitButton = (Button) findViewById(R.id.exit_button);
        LatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        LongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        TimeTextView = (TextView) findViewById(R.id.time_text);

        startButton.setOnClickListener(this);
        endButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        updateValuesFromBundle(savedInstanceState);

        if (mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        createLocationRequest();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState){
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    protected void createLocationRequest(){
        mLocationRequest = new LocationRequest();

        // desired interval for updates
        mLocationRequest.setInterval(UPDATE_INTERVERAL_IN_MILLISECONDS);
        // fastest that the app can receive updates
        mLocationRequest.setFastestInterval(UPDATE_INTERVERAL_IN_MILLISECONDS/2);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    // check if location is enabled
    private static boolean isLocationEnabled(Context context) {
        int locationMode;
        String locationProviders;

        // check what version of android is being run on the device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try{
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            }catch (Settings.SettingNotFoundException e){
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    // inform user that location services must be enabled
    private void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Location Services");
        alertDialog.setMessage("Your GPS seems to be disabled. This application requires GPS to be turned on. Do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id){
                        dialog.cancel();
                        finish();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                if (!mRequestingLocationUpdates) {
                    mRequestingLocationUpdates = true;
                    // begin getting location updates
                    startLocationUpdates();
                    // create filename based off of current trip number
                    Filename = "Trip_" + String.valueOf(tripnumber);
                    // increment trip number value for future trips that might be run in this session
                    tripnumber++;
                    try {
                        // open file
                        fos = openFileOutput(Filename, Context.MODE_PRIVATE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.end_button:
                if (mRequestingLocationUpdates) {
                    mRequestingLocationUpdates = false;
                    stopLocationUpdates();
                    try {
                        // close file
                        fos.close();
                        // Launch Map Activity
                        Intent mapsIntent = new Intent(MainActivity.this, MapsActivity.class);
                        mapsIntent.putExtra("filename", Filename);
                        startActivity(mapsIntent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.exit_button:
                showFinishAlert();
                break;
        }
    }
    protected void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    // modify what buttons can be pressed
    private void setButtonsEnabledState(){
        if (mRequestingLocationUpdates){
            startButton.setEnabled(false);
            endButton.setEnabled(true);
        }else{
            startButton.setEnabled(true);
            endButton.setEnabled(false);
        }
    }

    // refresh the display
    private void updateUI(){
        try {
            LatitudeTextView.setText(String.format(Locale.US, "%f", mCurrentLocation.getLatitude()));
            LongitudeTextView.setText(String.format(Locale.US, "%f", mCurrentLocation.getLongitude()));
            TimeTextView.setText(String.format(Locale.US, "%s", mLastUpdateTime));
            // write the new data to the file
            fos.write((String.valueOf(TimeTextView.getText()) + "::" + LatitudeTextView.getText() + "::" + LongitudeTextView.getText() + "\n").getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    protected void stopLocationUpdates(){
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void showFinishAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates){
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (mGoogleApiClient.isConnected()){
            stopLocationUpdates();
        }
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onBackPressed(){

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null){
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mLastUpdateTime = getTimeInstance().format(new Date());
            }
            updateUI();
        }

        if (mRequestingLocationUpdates){
            startLocationUpdates();
        }
    }

    @Override
    public void onLocationChanged(Location location){
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public void onSaveInstanceState(Bundle savedInstanceState){
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }
}
