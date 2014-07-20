package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.io.DataParser;

import java.io.IOException;

public class Walkntrade_Main extends Activity {

    private static final String TAG = "Walkntrade_Main";
    private static final int RESOLUTION_REQUEST = 9000;

    private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		checkForConnection();

        context = getApplicationContext();
        Button retry = (Button)findViewById(R.id.retryButton);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForConnection();
            }
        });

        //Checks if device has the Google Play Services APK
        if (checkPlayServices()) {
            GcmRegistration gcmReg = new GcmRegistration(context);
            String regId = gcmReg.getRegistrationId();
            Log.i(TAG, regId);

            if(regId.isEmpty()) {
                Log.i(TAG, "Registration id is empty, Creating one now");
                gcmReg.registerForId();
                finish();
            }
            else { //If registration id is already found. Send it to the server to make sure it's still updated.
                new AsyncTask<String, Void, String>(){
                    @Override
                    protected String doInBackground(String... regId) {
                        String serverResponse = null;

                        try {
                            DataParser database = new DataParser(context);
                            serverResponse = database.setRegistrationId(regId[0]);
                        } catch(IOException e) {
                            Log.e(TAG, "Sending id to server", e);
                        }

                        return serverResponse;
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        finish();
                    }
                }.execute(regId);
            }
        }
        else
            finish(); //Closes this activity

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feedback, menu);
		return true;
	}

    public void checkForConnection(){
        if(DataParser.isNetworkAvailable(this)) { //Checks if device has internet or mobile connection
            if(DataParser.getSharedStringPreference(this, DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG) != null) //There is a school preference
                startActivity(new Intent(this, SchoolPage.class)); //Starts SchoolPage Activity
            else
                startActivity(new Intent(this, Selector.class)); //Starts Selector (Select/Change School) activity
        }
    }

    //Check if Google Play services is available. Required for push notifications
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, RESOLUTION_REQUEST).show();
            else
                Log.i(TAG, "Play Services not available on device");

            return false;
        }
        return true;
    }
}

//Some images were auto-generated and resized with Android Asset Studio
