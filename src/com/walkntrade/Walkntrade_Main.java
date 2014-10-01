package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.gcm.GcmRegistration;
import com.walkntrade.io.DataParser;

import java.io.IOException;

public class Walkntrade_Main extends Activity {

    private static final String TAG = "Walkntrade_Main";
    private static final int RESOLUTION_REQUEST = 9000;

    private Context context;
    private boolean hasError = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);

        context = getApplicationContext();
        Button retry = (Button)findViewById(R.id.retryButton);

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

                            if(serverResponse.equals("Not authorized")) {
                                SharedPreferences settings = context.getSharedPreferences(DataParser.PREFS_AUTHORIZATION, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();

                                editor.putBoolean(DataParser.KEY_AUTHORIZED, false);
                                editor.apply();
                                new LogoutTask(context).execute();
                            }

                        } catch(IOException e) {
                            Log.e(TAG, "Sending id to server", e);
                        }
                        return serverResponse;
                    }

                }.execute(regId);
            }
        }
        if (hasConnection() && !hasError) {
            if(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG) != null) //There is a school preference
                startActivity(new Intent(context, SchoolPage.class)); //Starts SchoolPage Activity
            else
                startActivity(new Intent(context, Selector.class)); //Starts Selector (Select/Change School) activity

            finish(); //Closes this activity
        }

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasConnection() && checkPlayServices()) {
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
		getMenuInflater().inflate(R.menu.privacy_feedback, menu);
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

            hasError = true;
            return false;
        }

        hasError = false;
        return true;
    }
}

//Some images were auto-generated and resized with Android Asset Studio
