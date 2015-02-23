package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.fragments.TaskFragment;
import com.walkntrade.gcm.GcmRegistration;
import com.walkntrade.io.DataParser;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class LoginActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener, TaskFragment.TaskCallbacks {

    private static final String TAG = "LoginActivity";
    private static final String TAG_TASK_FRAGMENT = "Task_Fragment";
    private static final String SAVED_INSTANCE_PROGRESS_STATE = "saved_instance_progress_state";
    private static final int REQUEST_RESOLUTION = 9000;
    private static final int REQUEST_VERIFY = 100;
    private static final int REQUEST_RESET = 101;

    private SwipeRefreshLayout refreshLayout;
    private TextView loginError, resetPassword;
    private EditText emailAddress, password;
    private String _emailAddress, _password;
    private Context context;

    private TaskFragment taskFragment;
    private boolean isProgressShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        loginError = (TextView) findViewById(R.id.loginErrorMessage);
        resetPassword = (TextView) findViewById(R.id.forgot_password);
        emailAddress = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        Button submitButton = (Button) findViewById(R.id.submit);
        Button registerButton = (Button) findViewById(R.id.register);
        context = getApplicationContext();

        taskFragment = (TaskFragment) getFragmentManager().findFragmentByTag(TAG_TASK_FRAGMENT);
        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setEnabled(false);

        if(savedInstanceState != null) {
            isProgressShowing = savedInstanceState.getBoolean(SAVED_INSTANCE_PROGRESS_STATE, true);
            refreshLayout.setRefreshing(isProgressShowing);
        }

        resetPassword.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resetPasswordActivity = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                startActivityForResult(resetPasswordActivity, REQUEST_RESET);
            }
        });

        password.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) //If user presses 'Done', continue to log-in, but return false so the keyboard goes away
                    login();

                return false;
            }
        });

        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //If the refresh layout is refreshing, this means there is a current process running. Don't do anything else
                if(refreshLayout.isRefreshing())
                    return;
                //Hide keyboard if submit button was pressed
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(password.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                login();
            }
        });

        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //if(refreshLayout.isRefreshing())
                  //  return;
                Intent registerActivity = new Intent(context, RegistrationActivity.class);
                startActivity(registerActivity);
            }
        });
    }

    private void login() {
        loginError.setVisibility(View.GONE);
        _emailAddress = emailAddress.getText().toString();
        _password = password.getText().toString();

        if (canLogin() && DataParser.isNetworkAvailable(context)) {
            Log.d(TAG, "Performing login task");
            Bundle args = new Bundle();
            args.putInt(TaskFragment.ARG_TASK_ID, TaskFragment.TASK_LOGIN);
            args.putString(TaskFragment.ARG_LOGIN_USER, _emailAddress);
            args.putString(TaskFragment.ARG_LOGIN_PASSWORD, _password);

            taskFragment = new TaskFragment();
            taskFragment.setArguments(args);

            getFragmentManager().beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_VERIFY:
                if (resultCode == RESULT_OK) //If user has successfully verified their account, just log them in
                    login();
                break;
            case REQUEST_RESET:
                if (resultCode == RESULT_OK) {
                    loginError.setTextColor(getResources().getColor(R.color.green_dark));
                    loginError.setText(getString(R.string.reset_password_success));
                    loginError.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_INSTANCE_PROGRESS_STATE, isProgressShowing);
    }

    @Override
    public void onRefresh() {
    }

    //Verifies that login credentials are valid
    private boolean canLogin() {
        if (TextUtils.isEmpty(_emailAddress) || !_emailAddress.contains("@")) {
            emailAddress.setError(getString(R.string.invalid_email_address));
            loginError.setText(getString(R.string.login_error));
            loginError.setVisibility(View.VISIBLE);
            return false;
        }
        if (TextUtils.isEmpty(_password)) {
            password.setError(getString(R.string.invalid_password));
            loginError.setText(getString(R.string.login_error));
            loginError.setVisibility(View.VISIBLE);
            return false;
        }

        return true;
    }

    //Check if Google Play services is available. Required for push notifications
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, REQUEST_RESOLUTION).show();
            else
                Log.i(TAG, "Play Services not available on device");

            return false;
        }
        return true;
    }

    @Override
    public void onPreExecute(int taskId) {
        refreshLayout.setRefreshing(true);
        isProgressShowing = true;
    }

    @Override
    public void onProgressUpdate(int percent) {

    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onPostExecute(int taskId, Object result) {
        refreshLayout.setRefreshing(false);
        isProgressShowing = false;
        loginError.setVisibility(View.GONE);

        String response = result.toString();

        if (response.equals(DataParser.LOGIN_SUCCESS)) {
            new PollMessagesTask(context).execute(); //Get any of the user's unread messages on log in
            Log.i(TAG, "Login - setting logged in to true");
            DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_USER, DataParser.KEY_CURRENTLY_LOGGED_IN, true);

            //Checks if device has the Google Play Services APK
            if (checkPlayServices()) {
                GcmRegistration gcmReg = new GcmRegistration(context);
                String regId = gcmReg.getRegistrationId();
                Log.i(TAG, regId);

                if (regId.isEmpty()) {
                    Log.i(TAG, "Registration id is empty, Creating one now");
                    gcmReg.registerForId();
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SchoolPage.ACTION_UPDATE_DRAWER));
                    finish();
                } else { //If registration id is already found. Send it to the server to make sure it's still updated.
                    Log.i(TAG, "Registration id is found, sending to server");
                    new AsyncTask<String, Void, String>() {
                        @Override
                        protected String doInBackground(String... regId) {
                            String serverResponse = null;
                            DataParser database = new DataParser(context);

                            try {
                                serverResponse = database.setRegistrationId(regId[0]);
                            } catch (IOException e) {
                                Log.e(TAG, "Sending id to server", e);
                            }

                            return serverResponse;
                        }

                        @Override
                        protected void onPostExecute(String s) {
                            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SchoolPage.ACTION_UPDATE_DRAWER));
                            finish();
                        }
                    }.execute(regId);
                }
            } else {
                Log.e(TAG, "Google Services not available");
                Toast.makeText(context, "Google Services not available", Toast.LENGTH_SHORT).show();
                finish(); //Closes this activity if Google Play Services not available
            }
        } else if (response.equals("verify")) {
            Intent verifyIntent = new Intent(LoginActivity.this, VerifyKeyActivity.class);
            startActivityForResult(verifyIntent, REQUEST_VERIFY); //Starts Verify activity
        } else if (response.equals("reset")) {

            //Asks user if they want to reset their password
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle(getString(R.string.reset_password))
                    .setMessage(R.string.reset_password_ques)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent resetPasswordIntent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                            resetPasswordIntent.putExtra(ResetPasswordActivity.EMAIL, _emailAddress);
                            startActivityForResult(resetPasswordIntent, REQUEST_RESET);
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).create().show();

        } else {
            loginError.setText(response);
            loginError.setVisibility(View.VISIBLE);
        }
    }
}
