package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.walkntrade.gcm.Analytics;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.SchoolObject;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class Selector extends Activity implements OnItemClickListener {

    private String TAG = "Selector"; //Used for Log messages

    private TextView noResults;
    private ListView schoolList;
    private EditText editText;
    private ProgressBar progressBar;
    private Context context;
    private SchoolNameTask asyncTask;

    private ArrayList<SchoolObject> schoolObjects;
    private ArrayAdapter<SchoolObject> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);
        context = getApplicationContext();

        noResults = (TextView) findViewById(R.id.noResults);
        schoolList = (ListView) findViewById(R.id.schoolList);
        editText = (EditText) findViewById(R.id.schoolSearch);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        schoolObjects = new ArrayList<>();
        asyncTask = new SchoolNameTask();
        schoolList.setOnItemClickListener(this);
        mAdapter = new ArrayAdapter<SchoolObject>(context, R.layout.list_item) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView;

                if (convertView == null)
                    view = getLayoutInflater().inflate(R.layout.list_item, parent, false);

                TextView schoolName = (TextView) view.findViewById(R.id.text_view);
                schoolName.setText(getItem(position).getFullName());

                return view;
            }
        };

        //Search with a click
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mAdapter.clear();
                    schoolList.setAdapter(mAdapter);
                    String query = editText.getText().toString();
                    search(query);
                }
                return false;
            }
        });

        //Search on text change
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString();
                if (!query.isEmpty() && query.length() > 1) { //Do not perform a blank or one letter search on text change
                    search(query);
                }
            }
        });
    }

    //Gets the item selected from the ListView
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SchoolObject school = (SchoolObject) parent.getItemAtPosition(position);

        String schoolName = school.getFullName();
        String schoolId = school.getShortName();
        String schoolIdReadable = schoolId.replaceAll("[0-9]","").replace('-','\0');
        Log.i(TAG, "Before : "+schoolId+" After : "+schoolIdReadable);

        DataParser.setSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG, schoolName);
        DataParser.setSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT, "sPref="+schoolId);
        DataParser.setSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT_READABLE, schoolIdReadable);


        Intent schoolPage = new Intent(context, SchoolPage.class);
        startActivity(schoolPage);
        finish(); //Close this activity. App will now start-up from preferred school
    }

    private void search(String query) {
        noResults.setVisibility(View.GONE);
        if (asyncTask.cancel(true) || asyncTask.getStatus() == AsyncTask.Status.FINISHED) { //Attempts run new search by cancelling a running task or if the previous has finished
            asyncTask = new SchoolNameTask();
            asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
        }
    }

    //Asynchronous Task, looks for names of schools based on query
    public class SchoolNameTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... schoolName) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;
            String schoolQuery = schoolName[0];

            Tracker t = ((Analytics)getApplication()).getTracker(Analytics.TrackerName.APP_TRACKER);
            t.send(new HitBuilders.EventBuilder("User Input", "School Search").setLabel(schoolQuery).build());
            try {
                schoolObjects = new ArrayList<>();
                ObjectResult<ArrayList<SchoolObject>> result = database.getSchools(schoolQuery);
                serverResponse = result.getStatus();
                schoolObjects = result.getObject();
            } catch (IOException e) {
                Log.e(TAG, "Retrieving school name", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressBar.setVisibility(View.GONE);
            mAdapter.clear();

            GoogleAnalytics.getInstance(getBaseContext()).dispatchLocalHits();

            if(serverResponse == StatusCodeParser.STATUS_OK) {
                if (schoolObjects.size() <= 0) {
                    noResults.setText(context.getString(R.string.school_not_found));
                    noResults.setVisibility(View.VISIBLE);
                }
                else {
                    noResults.setVisibility(View.GONE);
                    mAdapter.addAll(schoolObjects);
                }
            }
            else if(serverResponse == StatusCodeParser.STATUS_NOT_FOUND) {
                noResults.setText(context.getString(R.string.school_not_found));
                noResults.setVisibility(View.VISIBLE);
            }
            else {
                noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
                noResults.setVisibility(View.VISIBLE);
            }

            schoolList.setAdapter(mAdapter);
        }
    }

}
