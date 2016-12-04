package com.brianmannresearch.simple_tracker;

import android.Manifest;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

    private static final int LOCATION_REQUEST = 1, STORAGE_REQUEST = 2;
    public static final long UPDATE_INTERVERAL_IN_MILLISECONDS = 1000;

    String[] files, filename;
    int tripnumber = 1;
    String Filename;
    FileOutputStream fos;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    protected Button startButton, endButton, exitButton, historyButton;
    protected TextView LatitudeTextView, LongitudeTextView, TimeTextView;
    protected Boolean mStoringLocationUpdates;
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
        historyButton = (Button) findViewById(R.id.history_button);
        exitButton = (Button) findViewById(R.id.exit_button);
        LatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        LongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        TimeTextView = (TextView) findViewById(R.id.time_text);

        startButton.setOnClickListener(this);
        endButton.setOnClickListener(this);
        historyButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);

        mStoringLocationUpdates = false;
        mLastUpdateTime = "";

        setButtonsEnabledState();

        if (mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();
        showUpdatesAlert();
    }

    protected void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        // desired interval for updates
        mLocationRequest.setInterval(UPDATE_INTERVERAL_IN_MILLISECONDS);
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

    private void showUpdatesAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("How often do you want to receive location updates? (Default is 1 second)")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(MainActivity.this, "Interval set to default", Toast.LENGTH_LONG).show();
                            mLocationRequest.setInterval(UPDATE_INTERVERAL_IN_MILLISECONDS);
                        }else {
                            mLocationRequest.setInterval(Integer.parseInt(input) * 1000);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start_button:
                if (!mStoringLocationUpdates) {
                    mStoringLocationUpdates = true;
                    setButtonsEnabledState();
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
                if (mStoringLocationUpdates) {
                    mStoringLocationUpdates = false;
                    setButtonsEnabledState();
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
            case R.id.history_button:
                Intent historyIntent = new Intent(MainActivity.this, HistoryActivity.class);
                startActivity(historyIntent);
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
        if (mStoringLocationUpdates){
            startButton.setEnabled(false);
            historyButton.setEnabled(false);
            exitButton.setEnabled(false);
            endButton.setEnabled(true);
        }else{
            startButton.setEnabled(true);
            historyButton.setEnabled(true);
            exitButton.setEnabled(true);
            endButton.setEnabled(false);
        }
    }

    // refresh the display
    private void updateUI(){
        LatitudeTextView.setText(String.format(Locale.US, "%f", mCurrentLocation.getLatitude()));
        LongitudeTextView.setText(String.format(Locale.US, "%f", mCurrentLocation.getLongitude()));
        TimeTextView.setText(String.format(Locale.US, "%s", mLastUpdateTime));
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
        if (mGoogleApiClient.isConnected()){
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
            try {
                if (mStoringLocationUpdates) {
                    // write the new data to the file
                    fos.write((String.valueOf(TimeTextView.getText()) + "::" + LatitudeTextView.getText() + "::" + LongitudeTextView.getText() + "\n").getBytes());
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        startLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location){
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        try {
            if (mStoringLocationUpdates) {
                // write the new data to the file
                fos.write((String.valueOf(TimeTextView.getText()) + "::" + LatitudeTextView.getText() + "::" + LongitudeTextView.getText() + "\n").getBytes());
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
