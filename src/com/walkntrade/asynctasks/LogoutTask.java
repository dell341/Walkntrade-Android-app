package com.walkntrade.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.SchoolPage;
import com.walkntrade.io.DataParser;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Asynchronous Task that signs user out
public class LogoutTask extends AsyncTask<Void, Void, Void> {

    private final String TAG = "ASYNCTASK:SchoolPosts";
    public Context context;

    public LogoutTask(Context _context) {
        context = _context;
    }

    @Override
    protected void onPreExecute() {
        DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_USER, DataParser.KEY_CURRENTLY_LOGGED_IN, false);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        DataParser database = new DataParser(context);
        try {
            database.logout();
        } catch(IOException e) {
            Log.e(TAG, "Logging out", e);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SchoolPage.ACTION_UPDATE_DRAWER)); //Update navigation drawer after logging out
    }
}
