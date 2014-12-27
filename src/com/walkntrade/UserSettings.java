package com.walkntrade;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.walkntrade.io.DataParser;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class UserSettings extends Activity implements AdapterView.OnItemClickListener{

    private static final String TAG = "UserSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        ListView settingsList = (ListView) findViewById(R.id.settings_list);
        String[] userOptions = getResources().getStringArray(R.array.user_account);

        settingsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, userOptions));
        settingsList.setOnItemClickListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        if(!DataParser.isUserLoggedIn(this))
            finish(); //If user not logged in, close this activity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, Privacy_Feedback.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Intent settingsIntent;

        switch(position){
            case 0: //View Posts
                settingsIntent = new Intent(this, ViewPosts.class);
                startActivity(settingsIntent); break;
            case 1: //Change Avatar
                settingsIntent = new Intent(this, UserAvatar.class);
                startActivity(settingsIntent); break;
            case 2: //Account Settings
                settingsIntent = new Intent(this, AccountSettings.class);
                startActivity(settingsIntent); break;
            case 3: //Contact Preferences
                settingsIntent = new Intent(this, ContactPref.class);
                startActivity(settingsIntent); break;
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!DataParser.isUserLoggedIn(this))
            finish(); //If user not logged in, close this activity
    }
}
