package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.walkntrade.io.DataParser;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";
    private static final int RESOLUTION_REQUEST = 9000;
    private static final int VERIFY_REQUEST = 100;

    private LinearLayout loginHeader;
	private TextView loginError;
    private ProgressBar progressBar;
    private EditText emailAddress, password;
	private String _emailAddress, _password;
    private Context context;
	private SharedPreferences settings;

    //TODO: Handle password or email changes with auto-login
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		settings = getSharedPreferences(DataParser.PREFS_USER, 0);
        loginHeader = (LinearLayout) findViewById(R.id.login_header);
		loginError = (TextView) findViewById(R.id.loginErrorMessage);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        TextView skipLogin = (TextView) findViewById(R.id.skipLogin);
		emailAddress = (EditText) findViewById(R.id.email);
		password = (EditText) findViewById(R.id.password);
        Button submitButton = (Button) findViewById(R.id.submit);
        Button registerButton = (Button) findViewById(R.id.register);
		context = getApplicationContext();
		
		submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loginError.setVisibility(View.GONE);
                _emailAddress = emailAddress.getText().toString();
                _password = password.getText().toString();
                String[] userCredentials = {_emailAddress, _password};

                if (canLogin() && DataParser.isNetworkAvailable(context))
                    new LoginTask().execute(userCredentials);
            }
        });

        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent registerActivity = new Intent(context, RegistrationActivity.class);
                startActivity(registerActivity);
            }
        });
		
		skipLogin.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(DataParser.CURRENTLY_LOGGED_IN, false);
                editor.apply();

                finish();
            }
        });
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feedback, menu);
		return true;
	}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == VERIFY_REQUEST){
            if(resultCode == RESULT_OK) //If user has successfully verified their account, just log them in
                if (canLogin() && DataParser.isNetworkAvailable(context)) {
                    loginError.setVisibility(View.GONE);
                    _emailAddress = emailAddress.getText().toString();
                    _password = password.getText().toString();
                    String[] userCredentials = {_emailAddress, _password};

                    new LoginTask().execute(userCredentials);
                }

        }
    }

    //Verifies that login credentials are valid
	private boolean canLogin() {
		if(TextUtils.isEmpty(_emailAddress) || !_emailAddress.contains("@")) {
			emailAddress.setError(getString(R.string.invalidEmailAddress));
			loginError.setText(getString(R.string.loginError));
			loginError.setVisibility(View.VISIBLE);
			return false;
		}
		if(TextUtils.isEmpty(_password)) {
			password.setError(getString(R.string.invalidPassword));
			loginError.setText(getString(R.string.loginError));
			loginError.setVisibility(View.VISIBLE);
			return false;
		}
			
		return true;
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
	
	//Asynchronous Task logs in user & retrieves username and password
	private class LoginTask extends AsyncTask<String, Void, String> {
		private DataParser database = new DataParser(context);

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
		protected String doInBackground(String... userCredentials) {
			String _emailAddress = userCredentials[0];
			String _password = userCredentials[1];
			
			String response = context.getString(R.string.login_failed);
			try {
				response = database.login(_emailAddress, _password);
                database.simpleGetIntent(DataParser.INTENT_GET_USERNAME);
                database.setSharedStringPreference(DataParser.PREFS_USER, DataParser.USER_EMAIL, _emailAddress);
                database.simpleGetIntent(DataParser.INTENT_GET_PHONENUM);
			}
			catch(Exception e) {
				Log.e(TAG, "Logging in", e);
			}
			
			return response;
		}

		@Override
		protected void onPostExecute(String response) {
            progressBar.setVisibility(View.INVISIBLE);

			if(response.equals(DataParser.LOGIN_SUCCESS)) {
				loginError.setVisibility(View.GONE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putBoolean(DataParser.CURRENTLY_LOGGED_IN, true);
				editor.apply();

                //Checks if device has the Google Play Services APK
                if (checkPlayServices()) {
                    GcmRegistration gcmReg = new GcmRegistration(context);
                    String regId = gcmReg.getRegistrationId();
                    Log.i(TAG, regId);

                    if(regId.isEmpty()) {
                        gcmReg.registerForId();
                        finish();
                    }
                    else
                        finish();
                }
                else
                    finish(); //Closes this activity
			}
            else if(response.equals(getString(R.string.error_need_verification))) {
                Intent verifyIntent = new Intent(LoginActivity.this, VerifyKeyActivity.class);
                startActivityForResult(verifyIntent, VERIFY_REQUEST); //Starts Verify activity
            }
			else {
				loginError.setText(response);
				loginError.setVisibility(View.VISIBLE);
			}
				
		}
		
	}

}
