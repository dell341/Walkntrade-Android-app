package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;

import java.io.IOException;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class AccountSettingsChange extends Activity {

    private static final String TAG = "AccountSettingsChange";
    public static final String CHANGE_SETTING = "settings_to_change";
    public static final int SETTING_EMAIL = 0;
    public static final int SETTING_PHONE = 1;
    public static final int SETTING_PASSWORD = 2;

    private ProgressBar progressBar;
    private TextView errorMessage;
    private EditText newSetting, newSettingConfirm;
    private int setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_change);

        setting = getIntent().getIntExtra(CHANGE_SETTING, 0);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorMessage = (TextView) findViewById(R.id.errorMessage);
        newSetting = (EditText) findViewById(R.id.newSetting);
        newSettingConfirm = (EditText) findViewById(R.id.newSettingConfirm);
        Button button = (Button) findViewById(R.id.button);

        switch(setting){
            case SETTING_EMAIL:
                newSetting.setHint(R.string.new_email);
                newSettingConfirm.setHint(R.string.new_email_confirm);

                newSetting.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                newSettingConfirm.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case SETTING_PHONE:
                newSetting.setHint(R.string.new_phone);
                newSettingConfirm.setHint(R.string.new_phone_confirm);

                newSetting.setInputType(InputType.TYPE_CLASS_PHONE);
                newSettingConfirm.setInputType(InputType.TYPE_CLASS_PHONE);
                break;
            case SETTING_PASSWORD:
                newSetting.setHint(R.string.new_password);
                newSettingConfirm.setHint(R.string.new_password_confirm);

                newSetting.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
                newSettingConfirm.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errorMessage.setVisibility(View.GONE);

                if(canContinue() && DataParser.isNetworkAvailable(AccountSettingsChange.this)) {
                    View layout = getLayoutInflater().inflate(R.layout.activity_veryify_key, null);

                    final EditText editText = (EditText) layout.findViewById(R.id.verify_key);
                    Button button = (Button) layout.findViewById(R.id.verify_submit);
                    editText.setHint(getString(R.string.old_password));
                    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    button.setVisibility(View.GONE);

                    AlertDialog.Builder builder = new AlertDialog.Builder(AccountSettingsChange.this);
                    builder.setTitle(getString(R.string.confirm_changes))
                            .setView(layout)
                            .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    if(TextUtils.isEmpty(editText.getText().toString())) {
                                        errorMessage.setText(getString(R.string.error_empty_password));
                                        errorMessage.setVisibility(View.VISIBLE);
                                    }
                                    else
                                        new ChangeSettingTask().execute(editText.getText().toString());
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).create().show();
                }
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
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

    public boolean canContinue() {
        boolean canContinue = true;
        String t1 = newSetting.getText().toString();
        String t2 = newSettingConfirm.getText().toString();

        if(!t1.equals(t2)){
            canContinue = false;
            newSettingConfirm.setError(getString(R.string.error_fields_mismatch));
        }

        switch (setting) {
            case SETTING_EMAIL:
                if(TextUtils.isEmpty(t1) || !t1.contains("@")) {
                    newSetting.setError(getString(R.string.invalidEmailAddress));
                    canContinue = false;
                }
                break;
            case SETTING_PHONE:
                if(t1.length() != 10 && t1.length() != 0) {
                    newSetting.setError(getString(R.string.error_phoneNum));
                    canContinue = false;
                }
                break;
            case SETTING_PASSWORD:
                if(t1.length() < 8) {
                    newSetting.setError(getString(R.string.error_password_short));
                    canContinue = false;
                }
                break;
            default: canContinue = false;
        }

        if(!canContinue) {
            errorMessage.setText(getString(R.string.save_change_failed));
            errorMessage.setVisibility(View.VISIBLE);
        }
        return canContinue;
    }

    private class ChangeSettingTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... oldPassword) {
            DataParser database = new DataParser(AccountSettingsChange.this);
            String serverResponse = null;

            try {
                serverResponse = database.changeSetting(oldPassword[0], newSetting.getText().toString(), setting);
            } catch (IOException e) {
                Log.e(TAG, "Changing setting", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(String serverResponse) {
            progressBar.setVisibility(View.INVISIBLE);

            Toast toast = Toast.makeText(AccountSettingsChange.this, serverResponse, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
            toast.show();

            finish(); //Close this activity
        }
    }
}
