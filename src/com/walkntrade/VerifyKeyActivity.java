package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.walkntrade.io.DataParser;

import java.io.IOException;


public class VerifyKeyActivity extends Activity {

    private static final String TAG = "VerifyKey";

    private TextView verifyError;
    private EditText verifyKey;
    private String _verifyKey;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veryify_key);

        context = getApplicationContext();
        verifyError = (TextView) findViewById(R.id.error_message);
        verifyKey = (EditText) findViewById(R.id.verify_key);
        Button submitButton = (Button) findViewById(R.id.submit);

        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                verifyError.setVisibility(View.GONE);
                _verifyKey = verifyKey.getText().toString();
                new VerifyKeyTask().execute();
            }
        });
    }

    private class VerifyKeyTask extends AsyncTask<Void, Void, String> {
        private DataParser database;

        @Override
        protected String doInBackground(Void... voids) {
            database = new DataParser(context);

            String response = null;
            try {
                response = database.verifyUser(_verifyKey);
            }
            catch (IOException e) {
                Log.e(TAG, "Verifying registration key", e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            if(response.equals(getString(R.string.verify_success))) {
                setResult(Activity.RESULT_OK); //Successfully registered
                finish();
            }

            verifyError.setVisibility(View.VISIBLE);
            verifyError.setText(getString(R.string.verify_failed));

        }
    }
}
