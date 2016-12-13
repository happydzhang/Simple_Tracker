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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends AppCompatActivity implements View.OnClickListener {

    private Button returnButton, deleteButton, tripButton;
    private LinearLayout linearLayout;
    private TextView[] tv;
    private String[] files, filename;
    private String Filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        returnButton = (Button) findViewById(R.id.return_button);
        tripButton = (Button) findViewById(R.id.trip_button);
        deleteButton = (Button) findViewById(R.id.delete_button);

        linearLayout = (LinearLayout) findViewById(R.id.history_linear);

        // check for previous trips to display
        files = fileList();

        int size = files.length;
        tv = new TextView[size];
        TextView temp;
        String textview = "Existing Trips:";
        temp = new TextView(this);
        temp.setText(textview);
        int i = 0;
        for (String file : files) {
            filename = file.split("/");
            if (filename[filename.length - 1].matches("\\S*Trip_\\d*")) {
                temp = new TextView(this);
                textview = "\n- " + filename[filename.length - 1];
                temp.setText(textview);
                linearLayout.addView(temp);

                tv[i] = temp;
                i++;
            }
        }

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
        finish();
    }

    private void showTripAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip do you want to view? (Please enter the full trip name)")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.text_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.text);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            showTripAlert();
                            Toast.makeText(HistoryActivity.this, "Please enter a filename", Toast.LENGTH_LONG).show();
                        }else {
                            Filename = input;
                            boolean Launched = false;
                            for (String file : files) {
                                filename = file.split("/");
                                if (filename[filename.length - 1].matches(Filename)) {
                                    Launched = true;
                                    Intent mapsIntent = new Intent(HistoryActivity.this, MapsActivity.class);
                                    mapsIntent.putExtra("filename", Filename);
                                    startActivity(mapsIntent);
                                }
                            }
                            if (!Launched) {
                                Toast.makeText(HistoryActivity.this, "Please enter a valid filename!", Toast.LENGTH_SHORT).show();
                            }
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

        alertDialog.setMessage("What trip do you want to delete? (Please enter a full filename)")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.text_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.text);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(HistoryActivity.this, "Please enter a filename!", Toast.LENGTH_LONG).show();
                        }else {
                            Filename = input;
                            boolean Launched = false;
                            for (String file : files) {
                                filename = file.split("/");
                                if (filename[filename.length - 1].matches(Filename)) {
                                    Launched = true;
                                    showConfirmDeleteAlert();
                                }
                            }
                            if (!Launched) {
                                Toast.makeText(HistoryActivity.this, "Please enter a valid filename!", Toast.LENGTH_SHORT).show();
                            }
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
                        deleteFile(Filename);
                        for (TextView textView : tv){
                            linearLayout.removeView(textView);
                        }
                        // check for previous trips to display
                        files = fileList();

                        int size = files.length;
                        tv = new TextView[size];
                        TextView temp;
                        String textview = "Existing Trips:";
                        temp = new TextView(HistoryActivity.this);
                        temp.setText(textview);
                        int j = 0;
                        for (String file : files) {
                            filename = file.split("/");
                            if (filename[filename.length - 1].matches("\\S*Trip_\\d*")) {
                                temp = new TextView(HistoryActivity.this);
                                textview = "\n- " + filename[filename.length - 1];
                                temp.setText(textview);
                                linearLayout.addView(temp);

                                tv[j] = temp;
                                j++;
                            }
                        }
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
