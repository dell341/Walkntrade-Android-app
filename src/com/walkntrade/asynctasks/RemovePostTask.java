package com.walkntrade.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class RemovePostTask extends AsyncTask<ArrayList<String>, Void, Void> {

    private static String TAG = "ASYNCTASK:RemovePost";

    private Context context;

    public RemovePostTask(Context _context){
        context = _context;
    }

    @Override
    protected Void doInBackground(ArrayList<String>... postToDelete) {
        DataParser database = new DataParser(context);

        try {
              for(String s : postToDelete[0])
                Log.v(TAG, "Removing "+s+":"+database.removePost(s));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
