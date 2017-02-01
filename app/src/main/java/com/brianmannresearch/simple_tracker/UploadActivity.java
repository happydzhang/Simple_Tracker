package com.brianmannresearch.simple_tracker;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class UploadActivity extends AppCompatActivity implements View.OnClickListener {

    private Button returnButton;
    private LinearLayout linearLayout;
    private TextView[] tv;
    private String[] files;
    private String Filename, upLoadServerUrl;
    private ProgressDialog dialog = null;
    private int serverResponseCode = 0, currentid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        returnButton = (Button) findViewById(R.id.return_button);

        upLoadServerUrl = "http://ndssl.000webhostapp.com/tracker/upload.php";

        linearLayout = (LinearLayout) findViewById(R.id.upload_linear);
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
            if (file.matches("\\S*Trip_\\d*")) {
                temp = new TextView(this);
                temp.setId(i);
                textview = file;
                temp.setText(textview);
                temp.setTextColor(Color.BLUE);
                temp.setClickable(true);
                temp.setOnClickListener(this);
                linearLayout.addView(temp);
                tv[i] = temp;
                i++;
            }
        }

        returnButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.return_button:
                finish();
                break;
            default:
                dialog = ProgressDialog.show(UploadActivity.this, "", "Uploading...", true);
                TextView textView = (TextView) findViewById(view.getId());
                currentid = view.getId();
                Filename = textView.getText().toString();
                showUploadAlert();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void showUploadAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Upload Photo");
        alertDialog.setMessage("Confirm that you want to upload " + Filename)
                .setCancelable(false)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener(){
                    public void onClick(final DialogInterface dialog, final int id) {
                        TextView textView = (TextView) findViewById(currentid);
                        textView.setTextColor(Color.RED);
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                uploadPhoto(Filename);
                            }
                        });
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id){
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private int uploadPhoto(String sourceFileUri) {
        HttpURLConnection conn;
        DataOutputStream dos;
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        File sourceFile = getBaseContext().getFileStreamPath(sourceFileUri);

        if (!sourceFile.isFile()) {
            dialog.dismiss();

            Log.e("uploadFile", "Source File does not exist: " + sourceFileUri);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UploadActivity.this, Filename + " does not exist", Toast.LENGTH_LONG).show();
                }
            });
            return 0;
        } else {
            try {
                // open a URL connection to the server
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUrl);

                // open a HTTP connection to the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // allow inputs
                conn.setDoOutput(true); // allow outputs
                conn.setUseCaches(false); // prevent a cached copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=filename;filename=" + sourceFileUri + "\r\n");
                dos.writeBytes("\r\n");

                dos.writeBytes(sourceFileUri);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;sourceFile=" + sourceFileUri + "\r\n");
                dos.writeBytes("\r\n");

                // create a buffer of max size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");

                // responses from the server
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is: " + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UploadActivity.this, "Upload complete...", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(UploadActivity.this, "Upload failed...", Toast.LENGTH_LONG).show();
                        }
                    });
                }

                // close the streams
                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(UploadActivity.this, "Malformed URL", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(UploadActivity.this, "Exception detected", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file exception", "Exception: " + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;
        }
    }
}
