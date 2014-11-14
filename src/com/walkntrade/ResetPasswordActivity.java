package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.walkntrade.io.DataParser;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class ResetPasswordActivity extends Activity {

    private static final String TAG = "ResetPasswordActivity";
    public static final String EMAIL = "user_email_to_be_reset";

    private Context context;
    private Button submit;
    private TextView errorMessage;
    private EditText enteredEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        context = getApplicationContext();
        submit = (Button) findViewById(R.id.submit);
        errorMessage = (TextView) findViewById(R.id.error_message);
        enteredEmail = (EditText) findViewById(R.id.email_address);

        String currentEmail = getIntent().getStringExtra(EMAIL);
        if(currentEmail != null)
            enteredEmail.setText(currentEmail);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorMessage.setVisibility(View.GONE);
                new ResetPasswordTask().execute(enteredEmail.getText().toString());
            }
        });
    }

    private class ResetPasswordTask extends AsyncTask<String, Void, Integer>{
        @Override
        protected void onPreExecute() {
            submit.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(String... email) {
            DataParser database = new DataParser(context);
            int serverResponse = -100;

            try {
                serverResponse = database.resetPassword(email[0]);
            } catch (IOException e) {
                Log.e(TAG, "Resetting password", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            submit.setEnabled(true);

            switch (serverResponse) {
                case 200:  setResult(Activity.RESULT_OK);
                    finish(); break;
                case 404: errorMessage.setText(getString(R.string.user_not_found));
                    errorMessage.setVisibility(View.VISIBLE); break;
                default: errorMessage.setText(getString(R.string.error_occured));
                errorMessage.setVisibility(View.VISIBLE);
            }
        }
    }

}
