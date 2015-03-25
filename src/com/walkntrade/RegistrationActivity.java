package com.walkntrade;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class RegistrationActivity extends ActionBarActivity {

    private String TAG = "Registration";
    private ScrollView scrollView;
    private ProgressBar progressBar;
    private TextView error;
    private EditText userName, email, phoneNumber, password, passwordVerf;
    private String _userName, _email, _phoneNumber, _password, _passwordVerf;
    private Button submit;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        context = getApplicationContext();
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        error = (TextView) findViewById(R.id.register_error);
        userName = (EditText) findViewById(R.id.register_username);
        email = (EditText) findViewById(R.id.register_email);
        phoneNumber = (EditText) findViewById(R.id.register_phoneNum);
        password = (EditText) findViewById(R.id.register_password);
        passwordVerf = (EditText) findViewById(R.id.register_password_verf);
        submit = (Button) findViewById(R.id.submit);

        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);

                //Collect all string values from input fields
                _userName = userName.getText().toString().trim();
                _email = email.getText().toString();
                _phoneNumber = phoneNumber.getText().toString();
                _password = password.getText().toString();
                _passwordVerf = passwordVerf.getText().toString();

                if (canRegister() && DataParser.isNetworkAvailable(context)) {
                    //Hide keyboard if submit button was pressed
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(password.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                    new RegistrationTask().execute();
                }
            }
        });

        passwordVerf.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                //If user presses 'Done' register the account, but return false so the keyboard goes away
                if(actionId == EditorInfo.IME_ACTION_DONE && canRegister() && DataParser.isNetworkAvailable(context))
                    new RegistrationTask().execute();

                return false;
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    //Verifies that all registration requirements are met
    private boolean canRegister() {
        boolean canRegister = true;
        boolean invalidEmail = false;

        if(_userName.length() < 5) {
            userName.setError(getString(R.string.error_username_short));
            canRegister = false;
        }

        if(_userName.length() > 20) {
            userName.setError(getString(R.string.error_username_long));
            canRegister = false;
        }

        if(_userName.contains(" ")) {
            userName.setError(getString(R.string.error_username_spaces));
            canRegister = false;
        }

        if(TextUtils.isEmpty(_email) || !_email.contains("@")) {
            email.setError(getString(R.string.invalid_email_address));
            canRegister = false;
        }

        if(!_email.contains(".edu")) {
            email.setError(getString(R.string.error_email_edu));
            canRegister = false;
            invalidEmail = true;
        }

        if(_phoneNumber.length() != 10 && _phoneNumber.length() != 0) {
            phoneNumber.setError(getString(R.string.error_phoneNum));
            canRegister = false;
        }

        if(_password.length() < 8) {
            password.setError(getString(R.string.error_password_short));
            canRegister = false;
        }

        if(!_passwordVerf.equals(_password)) {
            passwordVerf.setError(getString(R.string.error_password_no_match));
            canRegister = false;
        }

        if(!canRegister){
            error.setText(getString(R.string.error_registration));
            if(invalidEmail)
                error.setText(getString(R.string.error_email_edu_long));
            error.setVisibility(View.VISIBLE);
            scrollView.fullScroll(View.FOCUS_UP);
        }

        return canRegister;
    }

    private class RegistrationTask extends AsyncTask<Void, Void, ObjectResult<String>> {

        private boolean userNameTaken;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            submit.setEnabled(false);
        }

        @Override
        protected ObjectResult<String> doInBackground(Void... voids) {
            userNameTaken = false;
            DataParser database = new DataParser(context);

            ObjectResult<String> serverResult = null;
            try {
                if(database.isUserNameFree(_userName) != StatusCodeParser.STATUS_OK) {
                    userNameTaken = true;
                    return null;
                }
                serverResult = database.registerUser(_userName, _email, _password, _phoneNumber); //Sends request to register user
            } catch (IOException e) {
                Log.e(TAG, "Registering user", e);
            }
            return serverResult;
        }

        @Override
            protected void onPostExecute(ObjectResult<String> serverResult) {
            progressBar.setVisibility(View.INVISIBLE);
            submit.setEnabled(true);

            if(userNameTaken) { //If username is taken set error as so in the RegistrationActivity
                error.setText(context.getString(R.string.error_username_taken));
                error.setVisibility(View.VISIBLE);
                scrollView.fullScroll(View.FOCUS_UP);
            }
            else if (serverResult != null) {
                if(serverResult.getStatus() != StatusCodeParser.STATUS_OK) { //If email is already taken set error as so
                    error.setText(serverResult.getObject());
                    error.setVisibility(View.VISIBLE);
                    scrollView.fullScroll(View.FOCUS_UP);
                }
                else { //Close Registration Activity
                    //Confirms if user wants to send a message
                    AlertDialog.Builder builder = new AlertDialog.Builder(RegistrationActivity.this);
                    builder.setTitle(getString(R.string.email_verification))
                            .setMessage(R.string.register_email_verification_message)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                               }
                    }).create().show();
                }
            }
        }
    }

}
