package com.walkntrade.gcm;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.walkntrade.io.DataParser;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class GcmRegistration {
    private static final String TAG = "GCMRegistration";
    private static final String PROPERTY_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "last_recorded_app_version";
    private static final String SENDER_ID = "857653417054"; //Unique project number from Google API Developer Console

    private Context context;
    private GoogleCloudMessaging gcm;
    private String regId;

    public GcmRegistration(Context context) {
        this.context = context;
        gcm = GoogleCloudMessaging.getInstance(context);
    }

    //Gets current registration id for GCM, if it exists
    public String getRegistrationId() {
        final SharedPreferences prefs = context.getSharedPreferences(DataParser.PREFS_USER, Context.MODE_PRIVATE);
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
    public void registerForId(){
        new AsyncTask<Void, Void, String>(){
            @Override
            protected String doInBackground(Void... params) {
                String msg;

                try {
                    if(gcm == null)
                        gcm = GoogleCloudMessaging.getInstance(context);
                    regId = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID="+regId;

                    DataParser database = new DataParser(context);
                    Log.i(TAG, "Registered id to server?: " + database.setRegistrationId(regId));

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
        final SharedPreferences prefs = context.getSharedPreferences(DataParser.PREFS_USER, Context.MODE_PRIVATE);
        int appVersion = getAppVersion();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

}
