package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;

import java.io.IOException;


public class RegistrationActivity extends Activity {

    private String TAG = "Registration";
    private ScrollView scrollView;
    private ProgressBar progressBar;
    private TextView error;
    private EditText userName, email, phoneNumber, password, passwordVerf;
    private String _userName, _email, _phoneNumber, _password, _passwordVerf;
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
        Button submitButton = (Button) findViewById(R.id.submit);

        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);

                //Collect all string values from input fields
                _userName = userName.getText().toString();
                _email = email.getText().toString();
                _phoneNumber = phoneNumber.getText().toString();
                _password = password.getText().toString();
                _passwordVerf = passwordVerf.getText().toString();

                if (canRegister() && DataParser.isNetworkAvailable(context)) {
                    new RegistrationTask().execute();
                }
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
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
            error.setVisibility(View.VISIBLE);
            scrollView.fullScroll(View.FOCUS_UP);
        }

        return canRegister;
    }

    private class RegistrationTask extends AsyncTask<Void, Void, String> {

        private boolean userNameTaken;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            userNameTaken = false;
            DataParser database = new DataParser(context);

            String response = null;
            try {
                if(!database.isUserNameFree(_userName)) {
                    userNameTaken = true;
                    return null;
                }
                response = database.registerUser(_userName, _email, _password, _phoneNumber); //Sends request to register user
            } catch (IOException e) {
                Log.e(TAG, "Registering user", e);
            }
            return response;
        }

        @Override
            protected void onPostExecute(String response) {
            progressBar.setVisibility(View.INVISIBLE);

            if(userNameTaken) { //If username is taken set error as so in the RegistrationActivity
                error.setText(context.getString(R.string.error_username_taken));
                error.setVisibility(View.VISIBLE);
                scrollView.fullScroll(View.FOCUS_UP);
            }
            else {
                if(response.equals("3")) { //If email is already taken set error as so
                    error.setText(context.getString(R.string.error_email_taken));
                    error.setVisibility(View.VISIBLE);
                    scrollView.fullScroll(View.FOCUS_UP);
                }
                else { //Close Registration Activity
                    Toast.makeText(context, "Successfully Registered", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

}
