package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;

import com.walkntrade.io.DataParser;

import java.io.IOException;

public class ContactPref extends Activity {

    private static final String TAG = "ContactPref";

    private Context context;
    private ProgressBar progressBar;
    private CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_pref);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        if(DataParser.isNetworkAvailable(this))
            new GetContactTask().execute(); //Get contact preference from server

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked)
                    new ChangeContactTask().execute("1");
                else
                    new ChangeContactTask().execute("0");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GetContactTask extends AsyncTask<Void, Void, String>{

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try{
                serverResponse = database.getEmailPref();
            } catch (IOException e){
                Log.e(TAG, "Get User Contact", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(String s) {
            progressBar.setVisibility(View.INVISIBLE);

            if(s.equals("1")) //User wants receive emails
                checkBox.setChecked(true);
        }
    }

    private class ChangeContactTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... pref) {
            DataParser database = new DataParser(context);
            String serverResponse = null;

            try {
                serverResponse = database.setEmailPref(Integer.parseInt(pref[0]));
            }catch (IOException e){
                Log.e(TAG, "Setting user contact", e);
            }

            return serverResponse;
        }
    }
}
