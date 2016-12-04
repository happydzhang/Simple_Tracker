package com.brianmannresearch.simple_tracker;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener {

    protected Button returnButton, deleteButton, tripButton;
    protected TextView tripText;

    protected int tripid;
    protected String[] files, filename;
    protected String Filename;
    protected StringBuilder trips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        tripText = (TextView) findViewById(R.id.tripText);
        returnButton = (Button) findViewById(R.id.return_button);
        tripButton = (Button) findViewById(R.id.trip_button);
        deleteButton = (Button) findViewById(R.id.delete_button);

        trips = new StringBuilder();
        trips.append("Existing Trips:");
        // check for previous trips to display
        files = fileList();
        for (String file : files) {
            filename = file.split("/");
            if (filename[filename.length - 1].matches("Trip_\\d*")) {
                trips.append("\n").append("- ").append(filename[filename.length - 1]);
            }
        }
        tripText.setText(trips);

        returnButton.setOnClickListener(this);
        tripButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.trip_button:
                showTripAlert();
                break;
            case R.id.delete_button:
                showDeleteAlert();
                break;
            case R.id.return_button:
                finish();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void showTripAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip number do you want to view?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(HistoryActivity.this, "Please enter a value", Toast.LENGTH_LONG).show();
                        }else {
                            tripid = Integer.parseInt(input);
                            Filename = "Trip_" + String.valueOf(tripid);
                            Intent mapsIntent = new Intent(HistoryActivity.this, MapsActivity.class);
                            mapsIntent.putExtra("filename", Filename);
                            startActivity(mapsIntent);
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

    private void showDeleteAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip number do you want to delete?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(HistoryActivity.this, "Please enter a value", Toast.LENGTH_LONG).show();
                        }else {
                            tripid = Integer.parseInt(input);
                            showConfirmDeleteAlert();
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

    private void showConfirmDeleteAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Are you sure you want to delete this trip?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Filename = "Trip_" + String.valueOf(tripid);
                        deleteFile(Filename);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }
}
