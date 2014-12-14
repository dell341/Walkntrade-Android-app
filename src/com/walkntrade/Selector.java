package com.walkntrade;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.walkntrade.gcm.Analytics;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
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
    private static final String SAVED_BACKGROUND = "background_image";

    private TextView noResults;
    private ListView schoolList;
    private ImageView imageView;
    private EditText editText;
    private ProgressBar progressBar;
    private Context context;
    private SchoolNameTask asyncTask;

    private ArrayList<SchoolObject> schoolObjects;
    private ArrayAdapter<SchoolObject> mAdapter;
    private Bitmap background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector);
        context = getApplicationContext();

        imageView = (ImageView) findViewById(R.id.background);
        noResults = (TextView) findViewById(R.id.noResults);
        schoolList = (ListView) findViewById(R.id.schoolList);
        editText = (EditText) findViewById(R.id.schoolSearch);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (savedInstanceState != null) {
            Bitmap bm = savedInstanceState.getParcelable(SAVED_BACKGROUND);

            if (bm == null)
                new DownloadBackgroundTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else {
                background = bm;
                imageView.setImageBitmap(bm);
            }
        } else
            new DownloadBackgroundTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        schoolObjects = new ArrayList<SchoolObject>();
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putParcelable(SAVED_BACKGROUND, background);
        } catch (NullPointerException e) {
            Log.e(TAG, "Orientation change before image downloaded");
        }
    }

    //Gets the item selected from the ListView
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SchoolObject school = (SchoolObject) parent.getItemAtPosition(position);

        String schoolName = school.getFullName();
        String schoolId = school.getShortName();

        DataParser database = new DataParser(context);
        database.setSharedStringPreference(DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG, schoolName);
        database.setSharedStringPreference(DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT, "sPref="+schoolId);

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
//        new SchoolNameTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
    }

    private class DownloadBackgroundTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bm;
            String key = "background_0";
            String url = context.getResources().getString(R.string.background_image_1);
        //    Log.i(TAG, "Downloading background");

            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            bm = imageCache.getBitmapFromDiskCache(key);

            try {
                if (bm == null)
                    bm = DataParser.loadBitmap(context.getResources().getString(R.string.images_directory) + url);

                imageCache.addBitmapToCache(key, bm);
            } catch (IOException e) {
                Log.e(TAG, "Retrieving image", e);
            } finally {
                imageCache.close();
            }

            return bm;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

        //    Log.i(TAG, "Downloading background complete");
            if (bitmap != null) {
                background = bitmap;
                imageView.setImageBitmap(bitmap);
            }

        }
    }

    //Asynchronous Task, looks for names of schools based on query
    public class SchoolNameTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected void onPreExecute() {
        //    Log.d(TAG, "PRE-EXECUTE: Downloading school name: ");
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
                schoolObjects = new ArrayList<SchoolObject>();
                DataParser.ObjectResult<ArrayList<SchoolObject>> result = database.getSchools(schoolQuery);
                serverResponse = result.getStatus();
                schoolObjects = result.getValue();
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
                    noResults.setText(context.getString(R.string.no_results));
                    noResults.setVisibility(View.VISIBLE);
                }
                else {
                    noResults.setVisibility(View.GONE);
                    mAdapter.addAll(schoolObjects);
                }
            }
            else {
                noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
                noResults.setVisibility(View.VISIBLE);
            }

            schoolList.setAdapter(mAdapter);
        }
    }

}
