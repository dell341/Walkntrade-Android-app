package com.walkntrade;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Switch;
import android.widget.TextView;

public class NotificationSettings extends Activity {

    private static final String TAG = "NotificationSettings";

    private TextView sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Switch vibrate = (Switch) findViewById(R.id.switch_vibrate);
        Switch light = (Switch) findViewById(R.id.switch_light);
        sound = (TextView) findViewById(R.id.notification_sound);



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
}
