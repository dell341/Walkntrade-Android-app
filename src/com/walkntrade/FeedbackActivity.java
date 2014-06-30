package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;

import java.io.IOException;

public class FeedbackActivity extends Activity{

    private static final String TAG = "FeedbackActivity";

    private Context context;
    private ProgressBar progressBar;
    private TextView errorMessage;
    private EditText email, message;
    private String _email, _message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorMessage = (TextView) findViewById(R.id.error_message);
        email = (EditText) findViewById(R.id.email);
        message = (EditText) findViewById(R.id.message);
        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                errorMessage.setVisibility(View.GONE);
                _email = email.getText().toString();
                _message = message.getText().toString();

                if(canContinue() && DataParser.isNetworkAvailable(context))
                    new SendFeedbackTask().execute();
            }
        });
    }

    private boolean canContinue() {
        boolean canContinue = true;

        if(!TextUtils.isEmpty(_email) && !_email.contains("@")) {
            canContinue = false;
            email.setError(getString(R.string.invalidEmailAddress));
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

    private class SendFeedbackTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try {
                serverResponse = database.sendFeedback("ANDROID USER - "+_email, _message);
            } catch(IOException e){
                Log.e(TAG, "Sending Feedback", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(String response) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}
