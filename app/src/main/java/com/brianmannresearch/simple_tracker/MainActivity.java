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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.icu.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {

    private static final int LOCATION_REQUEST = 1, STORAGE_REQUEST = 2;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final String usernames = "saved_users.txt";

    private String[] files, filename;
    private int tripnumber = 1;
    private String Filename, username;
    private FileOutputStream fos, nfos, afos;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private List<String> users = new ArrayList<>();

    private Button startButton, endButton, exitButton, historyButton, uploadButton;
    private TextView LatitudeTextView, LongitudeTextView, TimeTextView;
    private Boolean mStoringLocationUpdates;
    private String mLastUpdateTime;

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

        // try to open file containing existing usernames
        try{
            FileInputStream fis = openFileInput(usernames);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            users.add("");
            while ((line = bufferedReader.readLine()) != null){
                users.add(line);
            }
            fis.close();
            afos = openFileOutput(usernames, Context.MODE_APPEND);
            // otherwise create the file
        }catch (Exception e){
            try {
                nfos = openFileOutput(usernames, Context.MODE_PRIVATE);
            }catch (Exception er){
                er.printStackTrace();
            }
            e.printStackTrace();
        }

        // get the username
        showUsernameAlert();

        startButton = (Button) findViewById(R.id.start_button);
        endButton = (Button) findViewById(R.id.end_button);
        historyButton = (Button) findViewById(R.id.history_button);
        exitButton = (Button) findViewById(R.id.exit_button);
        uploadButton = (Button) findViewById(R.id.upload_button);
        LatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        LongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        TimeTextView = (TextView) findViewById(R.id.time_text);

        startButton.setOnClickListener(this);
        endButton.setOnClickListener(this);
        historyButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);

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
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        // desired interval for updates
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
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

    private void showUsernameAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        View myView = inflater.inflate(R.layout.text_dialog, null);

        Spinner spinner = (Spinner) myView.findViewById(R.id.usernamespinner);
        final EditText editText = (EditText) myView.findViewById(R.id.text);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, users);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                editText.setText(adapterView.getItemAtPosition(pos).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        alertDialog.setMessage("Please enter a username:")
                .setCancelable(false)
                .setView(myView)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.text);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            showUsernameAlert();
                            Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_LONG).show();
                        }else {
                            username = input;
                            // check for previous trips in order to know which trip number should be instantiated
                            files = fileList();
                            for (String file : files) {
                                filename = file.split("/");
                                if (filename[filename.length - 1].matches(username + "\\S*Trip_\\d*")) {
                                    tripnumber++;
                                }
                            }
                            // check if this is first time the application has been run and write to the empty username file
                            if (nfos != null){
                                try {
                                    nfos.write((username+"\n").getBytes());
                                    nfos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // otherwise, append to the existing file
                            }else if (afos != null){
                                try {
                                    Boolean match = false;
                                    for (String user : users){
                                        if (username.matches(user)){
                                            match = true;
                                            break;
                                        }
                                    }
                                    // if it is a new username, add it to the list
                                    // otherwise, do not
                                    if (!match) {
                                        afos.write((username+"\n").getBytes());
                                        afos.close();
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                Toast.makeText(MainActivity.this, "Username not stored", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
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
                if (!mStoringLocationUpdates) {
                    mStoringLocationUpdates = true;
                    setButtonsEnabledState();
                    // create filename based off of current trip number
                    Filename = username + "_Trip_" + String.valueOf(tripnumber);
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
            case R.id.upload_button:
                Intent uploadIntent = new Intent(MainActivity.this, UploadActivity.class);
                startActivity(uploadIntent);
                break;
            case R.id.exit_button:
                showFinishAlert();
                break;
        }
    }
    private void startLocationUpdates(){
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
                    fos.write((mLastUpdateTime + "::" + String.valueOf(mCurrentLocation.getLatitude()) + "::" + String.valueOf(mCurrentLocation.getLongitude()) + "\n").getBytes());
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
                fos.write((mLastUpdateTime + "::" + String.valueOf(mCurrentLocation.getLatitude()) + "::" + String.valueOf(mCurrentLocation.getLongitude()) + "\n").getBytes());
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
