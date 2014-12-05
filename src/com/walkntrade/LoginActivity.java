package com.walkntrade;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.gcm.GcmRegistration;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class LoginActivity extends Activity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "LoginActivity";
    private static final String SAVED_BACKGROUND = "background_image";
    private static final int REQUEST_RESOLUTION = 9000;
    private static final int REQUEST_VERIFY = 100;
    public static final int REQUEST_LOGIN = 200;
    private static final int REQUEST_RESET = 300;

    private SwipeRefreshLayout refreshLayout;
    private TextView loginError, resetPassword;
    private ImageView imageView;
    private EditText emailAddress, password;
    private String _emailAddress, _password;
    private Context context;
    private SharedPreferences settings;

    private Bitmap background;

    //TODO: Handle password or email changes with auto-login
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        settings = getSharedPreferences(DataParser.PREFS_USER, 0);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        imageView = (ImageView) findViewById(R.id.background);
        loginError = (TextView) findViewById(R.id.loginErrorMessage);
        resetPassword = (TextView) findViewById(R.id.forgot_password);
        emailAddress = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        Button submitButton = (Button) findViewById(R.id.submit);
        Button registerButton = (Button) findViewById(R.id.register);
        context = getApplicationContext();

        if(savedInstanceState != null) {
            Bitmap bm = savedInstanceState.getParcelable(SAVED_BACKGROUND);

            if(bm == null)
                new DownloadBackgroundTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else {
                background = bm;
                imageView.setImageBitmap(bm);
            }
        }
        else
            new DownloadBackgroundTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setEnabled(false);

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
                if(actionId == EditorInfo.IME_ACTION_DONE) //If user presses 'Done' login, but return false so the keyboard goes away
                    login();

                return false;
            }
        });

        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Hide keyboard if submit button was pressed
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(password.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                login();
            }
        });

        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerActivity = new Intent(context, RegistrationActivity.class);
                startActivity(registerActivity);
            }
        });
    }

    private void login() {
        loginError.setVisibility(View.GONE);
        _emailAddress = emailAddress.getText().toString();
        _password = password.getText().toString();
        String[] userCredentials = {_emailAddress, _password};

        if (canLogin() && DataParser.isNetworkAvailable(context))
            new LoginTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, userCredentials);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putParcelable(SAVED_BACKGROUND, background);
        } catch (NullPointerException e) {
            Log.e(TAG, "Orientation change before image downloaded");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_VERIFY:
                if (resultCode == RESULT_OK) //If user has successfully verified their account, just log them in
                    if (canLogin() && DataParser.isNetworkAvailable(context)) {
                        loginError.setVisibility(View.GONE);
                        _emailAddress = emailAddress.getText().toString();
                        _password = password.getText().toString();
                        String[] userCredentials = {_emailAddress, _password};

                        new LoginTask().execute(userCredentials);
                    }
                break;
            case REQUEST_RESET:
                    if(resultCode == RESULT_OK) {
                        loginError.setTextColor(getResources().getColor(R.color.green_dark));
                        loginError.setText(getString(R.string.reset_password_success));
                        loginError.setVisibility(View.VISIBLE);
                    }
                break;
            default:
                break;
        }

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

    //Asynchronous Task logs in user & retrieves username and password
    private class LoginTask extends AsyncTask<String, Void, String> {
        private DataParser database = new DataParser(context);

        @Override
        protected void onPreExecute() {
        //    Log.d(TAG, "PRE-EXECUTE - Login");
            refreshLayout.setRefreshing(true);
        }

        @Override
        protected String doInBackground(String... userCredentials) {
            String _emailAddress = userCredentials[0];
            String _password = userCredentials[1];
            String userName;

            String response = context.getString(R.string.login_failed);
            try {
                response = database.login(_emailAddress, _password);
                database.setSharedStringPreference(DataParser.PREFS_USER, DataParser.KEY_USER_EMAIL, _emailAddress);
                DataParser.StringResult result = database.getUserName();
                userName = result.getValue();
                database.simpleGetIntent(DataParser.INTENT_GET_PHONENUM);
            } catch (Exception e) {
                Log.e(TAG, "Logging in", e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            refreshLayout.setRefreshing(false);
        //    Log.d(TAG, "Login complete");

            if (response.equals(DataParser.LOGIN_SUCCESS)) {
                loginError.setVisibility(View.GONE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(DataParser.KEY_CURRENTLY_LOGGED_IN, true);
                editor.apply();

                //Checks if device has the Google Play Services APK
                if (checkPlayServices()) {
                    GcmRegistration gcmReg = new GcmRegistration(context);
                    String regId = gcmReg.getRegistrationId();
                    Log.i(TAG, regId);

                    if (regId.isEmpty()) {
                        Log.i(TAG, "Registration id is empty, Creating one now");
                        gcmReg.registerForId();
                        setResult(Activity.RESULT_OK);
                        finish();
                    } else { //If registration id is already found. Send it to the server to make sure it's still updated.
                        new AsyncTask<String, Void, String>() {
                            @Override
                            protected String doInBackground(String... regId) {
                                String serverResponse = null;

                                try {
                                    serverResponse = database.setRegistrationId(regId[0]);
                                } catch (IOException e) {
                                    Log.e(TAG, "Sending id to server", e);
                                }

                                return serverResponse;
                            }

                            @Override
                            protected void onPostExecute(String s) {
                                setResult(Activity.RESULT_OK);
                                finish();
                            }
                        }.execute(regId);
                    }
                } else {
                    Log.e(TAG, "Google Services not available");
                    setResult(Activity.RESULT_CANCELED);
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

            }
            else {
                loginError.setText(response);
                loginError.setVisibility(View.VISIBLE);
            }

        }

    }

    private class DownloadBackgroundTask extends AsyncTask<Void, Void, Bitmap>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bm;
            String key = "background_1";
            String url = context.getResources().getString(R.string.background_image_2);

        //    Log.i(TAG, "Downloading background image");
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            bm = imageCache.getBitmapFromDiskCache(key);

            try {
                if (bm == null)
                    bm = DataParser.loadBitmap(context.getResources().getString(R.string.images_directory) + url);

                imageCache.addBitmapToCache(key, bm);
            } catch(IOException e) {
                Log.e(TAG, "Retrieving image", e);
            }
            finally {
                imageCache.close();
            }

            return bm;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

        //    Log.i(TAG, "Background image complete");
            if(bitmap != null) {
                background = bitmap;
                imageView.setImageBitmap(bitmap);
            }

        }
    }

}
