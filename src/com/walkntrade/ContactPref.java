package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ContactPref extends Activity implements CompoundButton.OnCheckedChangeListener{

    private static final String TAG = "ContactPref";
    private static final String PROPERTY_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "last_recorded_app_version";
    private static final String SENDER_ID = "857653417054"; //Unique project number from Google API Developer Console
    private static final int RESOLUTION_REQUEST = 9000;

    private Context context;
    private GoogleCloudMessaging gcm;
    private AtomicInteger msgId = new AtomicInteger();
    private ProgressBar progressBar;
    private Switch switchEmail, switchNofication;

    private String regId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_pref);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        switchEmail = (Switch) findViewById(R.id.switch_email);
        switchNofication = (Switch) findViewById(R.id.switch_notifications);
        TextView notificationSettings = (TextView) findViewById(R.id.notification_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if(DataParser.isNetworkAvailable(this))
            new GetContactTask().execute(); //Get contact preference from server

        //Checks if device has the Google Play Services APK
        if(!checkPlayServices())
            switchNofication.setEnabled(false);
        else {
            switchNofication.setEnabled(true);
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId(context);
            Log.i(TAG, regId);

            if(regId.isEmpty())
                registerForId();
        }

        switchEmail.setChecked(DataParser.getSharedBooleanPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_EMAIL));
        switchNofication.setChecked(DataParser.getSharedBooleanPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_USER));

        switchEmail.setOnCheckedChangeListener(this);
        switchNofication.setOnCheckedChangeListener(this);

        notificationSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ContactPref.this, NotificationSettings.class));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Checks if device has the Google Play Services APK
        if(!checkPlayServices())
            switchNofication.setEnabled(false);
        else
            switchNofication.setEnabled(true);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();

        switch (id){
            case R.id.switch_email: DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_EMAIL, isChecked);
                if(isChecked)
                    new ChangeContactTask().execute("1");
                else
                    new ChangeContactTask().execute("2"); break;
            case R.id.switch_notifications: DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_USER, isChecked); break;
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

    //Registers the app with Google Cloud Messagin servers asynchronously
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

                        //TODO: Send registration id to Walkntrade server

                        storeRegistrationId(regId);
                    } catch(IOException e) {
                        msg = "Error: "+e.getMessage();
                    }
                    return msg;
                }

                @Override
                protected void onPostExecute(String msg) {
                    Log.i(TAG, msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
        }.execute();
    }

    //Store registration id and app version code
    private void storeRegistrationId(String regId){
        final SharedPreferences prefs = getSharedPreferences(ContactPref.class.getSimpleName(), Context.MODE_PRIVATE);
        int appVersion = getAppVersion();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    private class GetContactTask extends AsyncTask<Void, Void, String>{

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try{
                serverResponse = database.simpleGetIntent(DataParser.INTENT_GET_EMAILPREF);
            } catch (IOException e){
                Log.e(TAG, "Get User Contact", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            progressBar.setVisibility(View.INVISIBLE);

            if(s.equals("1")) //User wants receive emails
                switchEmail.setChecked(true);
        }
    }

    private class ChangeContactTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... pref) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try {
                serverResponse = database.setEmailPreference(Integer.parseInt(pref[0]));
            }catch (IOException e){
                Log.e(TAG, "Setting user contact", e);
            }

            return serverResponse;
        }
    }
}
