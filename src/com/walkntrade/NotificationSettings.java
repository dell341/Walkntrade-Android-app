package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.walkntrade.io.DataParser;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class NotificationSettings extends Activity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener{

    private static final String TAG = "NotificationSettings";
    private static final int REQUEST_CODE = 100;

    private Context context;
    private TextView soundTitle;

    private int numMessages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        context = getApplicationContext();
        Switch vibrate = (Switch) findViewById(R.id.switch_vibrate);
        Switch light = (Switch) findViewById(R.id.switch_light);
        LinearLayout sound = (LinearLayout) findViewById(R.id.notification_sound);
        soundTitle = (TextView) findViewById(R.id.sound_title);

        vibrate.setChecked(DataParser.getSharedBooleanPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_VIBRATE));
        light.setChecked(DataParser.getSharedBooleanPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_LIGHT));

        if(DataParser.getSoundPref(context) == null) { //If no sound preference is found. Use current sound.
            Uri uri = Settings.System.DEFAULT_NOTIFICATION_URI;
            soundTitle.setText(RingtoneManager.getRingtone(context, uri).getTitle(context));
            DataParser.setSoundPref(context, uri);
        }
        else {
            Uri uri = Uri.parse(DataParser.getSoundPref(context));
            soundTitle.setText(RingtoneManager.getRingtone(context, uri).getTitle(context));
        }

        vibrate.setOnCheckedChangeListener(this);
        light.setOnCheckedChangeListener(this);
        sound.setOnClickListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int id = buttonView.getId();

        switch (id){
            case R.id.switch_vibrate:
                DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_VIBRATE, isChecked); break;
            case R.id.switch_light:
                DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_LIGHT, isChecked); break;
        }
    }

    @Override
    public void onClick(View v) {
        Intent soundIntent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        soundIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        startActivityForResult(soundIntent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if(resultCode == RESULT_OK) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                DataParser.setSoundPref(context, uri);
                soundTitle.setText(RingtoneManager.getRingtone(context, uri).getTitle(context));
            }
    }
}
