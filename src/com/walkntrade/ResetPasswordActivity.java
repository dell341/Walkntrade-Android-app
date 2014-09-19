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

    private class ResetPasswordTask extends AsyncTask<String, Void, String>{
        @Override
        protected void onPreExecute() {
            submit.setEnabled(false);
        }

        @Override
        protected String doInBackground(String... email) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try {
                serverResponse = database.resetPassword(email[0]);
            } catch (IOException e) {
                Log.e(TAG, "Resetting password", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(String response) {
            submit.setEnabled(true);

            if(response.equals("Thanks! Check your inbox for the new password")) {
                setResult(Activity.RESULT_OK);
                finish();
            }
            else {
                errorMessage.setVisibility(View.VISIBLE);
                errorMessage.setText(response);
            }
        }
    }

}
