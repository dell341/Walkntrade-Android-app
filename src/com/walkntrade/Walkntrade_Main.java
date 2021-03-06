package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.gcm.GcmRegistration;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.StatusCodeParser;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class Walkntrade_Main extends Activity {

    private static final String TAG = "Walkntrade_Main";
    private static final int RESOLUTION_REQUEST = 9000;

    private Context context;
    private boolean updatedCategories = false;
    private boolean hasPlayServicesError = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

        context = getApplicationContext();
        Button retry = (Button)findViewById(R.id.retryButton);
        GoogleAnalytics.getInstance(context).setAppOptOut(BuildConfig.DEBUG);

        //Check & update categories from the server
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                int serverResponse = StatusCodeParser.CONNECT_FAILED;
                DataParser database = new DataParser(context);

                try {
                    serverResponse = database.getCategories();
                } catch (IOException e) {
                    Log.e(TAG, "Getting Categories", e);
                }

                return serverResponse;
            }

            @Override
            protected void onPostExecute(Integer serverResponse) {
                if(serverResponse == StatusCodeParser.STATUS_OK)
                    updatedCategories = true;
            }
        }.execute();

        //Checks if device has the Google Play Services APK
        if (checkPlayServices()) {
            GcmRegistration gcmReg = new GcmRegistration(context);
            String regId = gcmReg.getRegistrationId();

            if(regId.isEmpty()) {
                Log.i(TAG, "Registration id is empty, Creating one now");
                gcmReg.registerForId();
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
                }.execute(regId);
            }
        }

        continueToApplication();

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if (hasConnection() && updatedCategories && checkPlayServices()) {
                if(hasConnection()) {
                    if(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG) != null) //There is a school preference
                        startActivity(new Intent(context, SchoolPage.class)); //Starts SchoolPage Activity
                    else
                        startActivity(new Intent(context, Selector.class)); //Starts Selector (Select/Change School) activity

                    finish(); //Closes this activity
                }
            }
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feedback, menu);
		return true;
	}

    public boolean hasConnection(){
        return DataParser.isNetworkAvailable(this); //Checks if device has internet or mobile connection
    }

    //Check if Google Play services is available. Required for push notifications
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            if(GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Log.v(TAG, "GooglePlayServices error: "+resultCode);
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, RESOLUTION_REQUEST).show();
            }
            else
                Log.i(TAG, "Play Services not available on device");

            hasPlayServicesError = true;
            return false;
        }

        hasPlayServicesError = false;
        return true;
    }

    //Attempt to connect to the rest of the app, if the conditions below have been met
    private void continueToApplication() {
        if (hasConnection() && !hasPlayServicesError) {
            if(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG) != null) //There is a school preference
                startActivity(new Intent(context, SchoolPage.class)); //Starts SchoolPage Activity
            else
                startActivity(new Intent(context, Selector.class)); //Starts Selector (Select/Change School) activity

            finish(); //Closes this activity
        }
    }
}

/* Proverbs 3:5-6
Trust in the Lord with all you heart
and lean not on your own understanding;
in all your ways submit to him
and he will make your paths straight.
 */
