package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.io.DataParser;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class ContactPref extends Activity implements CompoundButton.OnCheckedChangeListener, SwipeRefreshLayout.OnRefreshListener{

    private static final String TAG = "ContactPref";
    private static final String PROPERTY_REG_ID = "gcm_registration_id";
    private static final String PROPERTY_APP_VERSION = "last_recorded_app_version";
    private static final String SENDER_ID = "857653417054"; //Unique project number from Google API Developer Console
    private static final int RESOLUTION_REQUEST = 9000;

    private Context context;
    private SwipeRefreshLayout refreshLayout;
    private Switch switchEmail, switchNofication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_pref);

        context = getApplicationContext();
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        switchEmail = (Switch) findViewById(R.id.switch_email);
        switchNofication = (Switch) findViewById(R.id.switch_notifications);
        TextView notificationSettings = (TextView) findViewById(R.id.notification_settings);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setEnabled(false);

        if(DataParser.isNetworkAvailable(this))
            new GetContactTask().execute(); //Get contact preference from server

        //Checks if device has the Google Play Services APK
        if(!checkPlayServices())
            switchNofication.setEnabled(false);
        else
            switchNofication.setEnabled(true);

        String emailPreference = DataParser.getSharedStringPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_EMAIL);
        if(emailPreference != null){
            boolean value = emailPreference.equals("1");
            switchEmail.setChecked(value);
        }

        switchNofication.setChecked(DataParser.getSharedBooleanPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_USER));
        switchNofication.setOnCheckedChangeListener(this);

        //Change notification sound, vibrate, light
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
            case R.id.switch_email:
                if(isChecked)
                    new ChangeContactTask().execute("1");
                else
                    new ChangeContactTask().execute("0");
                break;
            case R.id.switch_notifications: DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_USER, isChecked); break;
        }
    }

    @Override
    public void onRefresh() {
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

    //Gets the user contact preference
    private class GetContactTask extends AsyncTask<Void, Void, String>{

        @Override
        protected void onPreExecute() {
            refreshLayout.setRefreshing(true);
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
            refreshLayout.setRefreshing(false);
            if(s.equals("1")) //User wants receive emails
                switchEmail.setChecked(true);
            else
                switchEmail.setChecked(false);

            switchEmail.setOnCheckedChangeListener(ContactPref.this); //Prevents contact pref from being undone
        }
    }

    private class ChangeContactTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... pref) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try {
                serverResponse = database.setEmailPreference(pref[0]);
            }catch (IOException e){
                Log.e(TAG, "Setting user contact", e);
            }

            return serverResponse;
        }
    }
}
