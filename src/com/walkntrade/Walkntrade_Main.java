package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.walkntrade.io.DataParser;

import java.io.IOException;

public class Walkntrade_Main extends Activity {

    private static final String TAG = "Walkntrade_Main";
    private static final String PROPERTY_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "last_recorded_app_version";
    private static final String SENDER_ID = "857653417054"; //Unique project number from Google API Developer Console
    private static final int RESOLUTION_REQUEST = 9000;

    private Context context;
    private GoogleCloudMessaging gcm;
    private String regId;

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
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId(context);
            Log.i(TAG, regId);

            if(regId.isEmpty()) {
                registerForId();
                finish();
            }
            else
                finish();
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

    //Gets current registation id for GCM, if it exists
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getSharedPreferences(ContactPref.class.getSimpleName(), Context.MODE_PRIVATE);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if(registrationId.isEmpty()) {
            Log.i(TAG, "GCM registration not found");
            return "";
        }

        //If app was updated, get new registration ID. Current is not guaranteed to be compatible
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if(registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed");
            return "";
        }
        return registrationId;
    }

    private int getAppVersion(){
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Package name not found: "+e);
        }
    }

    //Registers the app with Google Cloud Messaging servers asynchronously
    private void registerForId(){
        new AsyncTask<Void, Void, String>(){
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";

                try {
                    if(gcm == null)
                        gcm = GoogleCloudMessaging.getInstance(context);
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID="+regId;

                    DataParser database = new DataParser(context);
                    Log.v(TAG, "Register Id: " + database.setRegistrationId(regId));

                    storeRegistrationId();
                } catch(IOException e) {
                    msg = "Error: "+e.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i(TAG, msg);
            }
        }.execute();
    }

    //Store registration id and app version code
    private void storeRegistrationId(){
        final SharedPreferences prefs = getSharedPreferences(Walkntrade_Main.class.getSimpleName(), Context.MODE_PRIVATE);
        int appVersion = getAppVersion();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }
}

//Some images were auto-generated and resized with Android Asset Studio
