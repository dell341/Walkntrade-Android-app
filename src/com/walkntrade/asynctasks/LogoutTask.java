package com.walkntrade.asynctasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.walkntrade.io.DataParser;

import java.io.IOException;

/**
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
    protected Void doInBackground(Void... voids) {
        DataParser database = new DataParser(context);
        try {
            SharedPreferences settings = context.getSharedPreferences(DataParser.PREFS_USER, 0);
            SharedPreferences.Editor editor = settings.edit();

            editor.putBoolean(DataParser.CURRENTLY_LOGGED_IN, false);
            editor.commit();

            database.logout();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
