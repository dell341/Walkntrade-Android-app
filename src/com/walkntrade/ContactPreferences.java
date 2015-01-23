package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
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

public class ContactPreferences extends Activity implements CompoundButton.OnCheckedChangeListener{

    private static final String TAG = "ContactPreferences";
    private static final int RESOLUTION_REQUEST = 9000;
    private static final int REQUEST_SOUND_PICKER = 100;

    private Context context;
    private CheckBox checkEmail, checkVibrate, checkLight;
    private Switch switchNofication;
    private LinearLayout sound;
    private TextView soundTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_preferences);

        context = getApplicationContext();
        checkEmail = (CheckBox) findViewById(R.id.check_email);
        switchNofication = (Switch) findViewById(R.id.switch_notifications);
        checkVibrate = (CheckBox) findViewById(R.id.check_vibrate);
        checkLight = (CheckBox) findViewById(R.id.check_light);
        sound = (LinearLayout) findViewById(R.id.notification_sound);
        soundTitle = (TextView) findViewById(R.id.sound_title);

        switchNofication.setChecked(DataParser.getSharedBooleanPreferenceTrueByDefault(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_USER));
        checkVibrate.setChecked(DataParser.getSharedBooleanPreferenceTrueByDefault(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_VIBRATE));
        checkLight.setChecked(DataParser.getSharedBooleanPreferenceTrueByDefault(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_LIGHT));
        String emailPreference = DataParser.getSharedStringPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_EMAIL);

        //Checks if device has the Google Play Services APK
        if(!checkPlayServices())
            disableNotifications();
        else
            enableNotifications();

        if(DataParser.getSoundPref(context) == null) { //If no sound preference is found. Use default sound.
            Uri uri = Settings.System.DEFAULT_NOTIFICATION_URI;
            soundTitle.setText(RingtoneManager.getRingtone(context, uri).getTitle(context));
            DataParser.setSoundPref(context, uri);
        }
        else {
            Uri uri = Uri.parse(DataParser.getSoundPref(context));
            soundTitle.setText(RingtoneManager.getRingtone(context, uri).getTitle(context));
        }

        if(emailPreference != null)
            checkEmail.setChecked(emailPreference.equals("1"));

        checkVibrate.setOnCheckedChangeListener(this);
        checkLight.setOnCheckedChangeListener(this);
        switchNofication.setOnCheckedChangeListener(this);
        sound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent soundIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                soundIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                startActivityForResult(soundIntent, REQUEST_SOUND_PICKER);
            }
        });

        if(DataParser.isNetworkAvailable(this))
            new GetContactTask().execute(); //Get contact preference from server

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void disableNotifications() {
        checkVibrate.setEnabled(false);
        checkLight.setEnabled(false);
        sound.setEnabled(false);
    }

    private void enableNotifications() {
        checkVibrate.setEnabled(true);
        checkLight.setEnabled(true);
        sound.setEnabled(true);
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
            disableNotifications();
        else
            enableNotifications();
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();
        switch (id){
            case R.id.check_email:
                if(isChecked)
                    new ChangeContactTask().execute("1");
                else
                    new ChangeContactTask().execute("0"); break;
            case R.id.switch_notifications: DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_USER, isChecked);
                if(!isChecked)
                    disableNotifications();
                else
                    enableNotifications(); break;
            case R.id.check_vibrate: DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_VIBRATE, isChecked); break;
            case R.id.check_light: DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_LIGHT, isChecked); break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            DataParser.setSoundPref(context, uri);
            soundTitle.setText(RingtoneManager.getRingtone(context, uri).getTitle(context));
        }
    }

    //Gets the user contact preference
    private class GetContactTask extends AsyncTask<Void, Void, String>{

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
            if(s.equals("1")) //User wants receive emails
                checkEmail.setChecked(true);
            else
                checkEmail.setChecked(false);

            checkEmail.setOnCheckedChangeListener(ContactPreferences.this); //Prevents contact pref from being undone
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
