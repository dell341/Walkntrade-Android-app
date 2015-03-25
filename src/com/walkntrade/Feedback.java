package com.walkntrade;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.StatusCodeParser;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class Feedback extends ActionBarActivity {

    private static final String TAG = "FeedbackActivity";

    private Context context;
    private TextView errorMessage;
    private EditText email, message;
    private String _email, _message;
    private ProgressBar progressBar;
    private Button submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorMessage = (TextView) findViewById(R.id.error_message);
        email = (EditText) findViewById(R.id.email);
        message = (EditText) findViewById(R.id.message);
        submit = (Button) findViewById(R.id.button);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errorMessage.setVisibility(View.GONE);
                _email = email.getText().toString();
                _message = message.getText().toString();

                if (canContinue() && DataParser.isNetworkAvailable(context))
                    new SendFeedbackTask().execute();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    private boolean canContinue() {
        boolean canContinue = true;

        if(!TextUtils.isEmpty(_email) && !_email.contains("@")) {
            canContinue = false;
            email.setError(getString(R.string.invalid_email_address));
        }

        if(TextUtils.isEmpty(_message)) {
            canContinue = false;
            message.setError(getString(R.string.invalid_message));
        }

        if(!canContinue) {
            errorMessage.setVisibility(View.VISIBLE);
            errorMessage.setText(getString(R.string.feedback_fail));
        }

        return canContinue;
    }

    private class SendFeedbackTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            submit.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                serverResponse = database.sendFeedback("ANDROID USER - "+_email, _message);
            } catch(IOException e){
                Log.e(TAG, "Sending Feedback", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressBar.setVisibility(View.GONE);
            submit.setEnabled(true);

            if(serverResponse == StatusCodeParser.STATUS_OK)
                Toast.makeText(context, getString(R.string.feedback_thanks), Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(context, getString(R.string.feedback_fail)+" - "+StatusCodeParser.getStatusString(context, serverResponse), Toast.LENGTH_SHORT).show();

            finish();
        }
    }

}
