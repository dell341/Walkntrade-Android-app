package com.walkntrade.asynctasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

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
        SharedPreferences settings = context.getSharedPreferences(DataParser.PREFS_USER, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean(DataParser.KEY_CURRENTLY_LOGGED_IN, false);
        editor.apply();
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
}
