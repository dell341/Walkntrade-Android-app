package com.walkntrade;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.walkntrade.io.DataParser;

import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class AccountSettings extends ActionBarActivity {

    private static final String TAG = "AccountSettings";
    private static final int REQUEST_ACCOUNT_CHANGE = 9000;
    public static final int RESULT_FINISH_ACTIVITY = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_list_view);

        ListView listView = (ListView) findViewById(R.id.list_view);

        String email = DataParser.getSharedStringPreference(this, DataParser.PREFS_USER, DataParser.KEY_USER_EMAIL);
        String phone = DataParser.getSharedStringPreference(this, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE);
        String password = getString(R.string.change_password);

        if(phone == null || phone.equals("0") || phone.isEmpty())
            phone = getString(R.string.add_phone);

        ArrayList<String> settings = new ArrayList<String>(3);
        settings.add(email);
        settings.add(phone);
        settings.add(password);

        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, settings));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(AccountSettings.this, AccountSettingsChange.class);
                switch(position){
                    case 0:
                        intent.putExtra(AccountSettingsChange.CHANGE_SETTING, AccountSettingsChange.SETTING_EMAIL);
                        startActivityForResult(intent, REQUEST_ACCOUNT_CHANGE);
                        break;
                    case 1:
                        intent.putExtra(AccountSettingsChange.CHANGE_SETTING, AccountSettingsChange.SETTING_PHONE);
                        startActivityForResult(intent, REQUEST_ACCOUNT_CHANGE);break;
                    case 2:
                        intent.putExtra(AccountSettingsChange.CHANGE_SETTING, AccountSettingsChange.SETTING_PASSWORD);
                        startActivityForResult(intent, REQUEST_ACCOUNT_CHANGE);break;
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_CHANGE :
                if(resultCode == RESULT_FINISH_ACTIVITY) {
                    setResult(UserSettings.RESULT_FINISH_ACTIVITY);
                    finish();
                }
        }
    }
}
