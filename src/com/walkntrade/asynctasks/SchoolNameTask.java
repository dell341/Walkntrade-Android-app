package com.walkntrade.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.io.DataParser;

import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Asynchronous Task, looks for names of schools based on query
public class SchoolNameTask extends AsyncTask<String, Void, ArrayList<String>> {

    private final String TAG = "ASYNCTASK:SchoolName";

    private Context context;
    private ProgressBar progressBar;
    private TextView noResults;
    private ListView listOfSchools;
    private ArrayAdapter<String> mAdapter;

    public SchoolNameTask(Context _context, ProgressBar _pBar, TextView _noResults, ListView _listOfSchools) {
        context = _context;
        progressBar = _pBar;
        noResults = _noResults;
        listOfSchools = _listOfSchools;
        mAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,0);
    }
    @Override
    protected void onPreExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected ArrayList<String> doInBackground(String... schoolName) {
        ArrayList<String> schoolsList = new ArrayList<String>();
        DataParser database = new DataParser(context);

        try {
            String name = schoolName[0];
            schoolsList = database.getSchools(name);
        }
        catch(Exception e) {
            Log.e(TAG, "Retrieving school name", e);
        }
        return schoolsList;
    }

    @Override
    protected void onPostExecute(ArrayList<String> names) {
        progressBar.setVisibility(View.GONE);

        mAdapter.clear();
        if(names.size() <= 0)
            noResults.setVisibility(View.VISIBLE);
        else {
            noResults.setVisibility(View.GONE);
            mAdapter.addAll(names);
        }

        listOfSchools.setAdapter(mAdapter);
    }
}
