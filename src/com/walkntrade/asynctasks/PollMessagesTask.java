package com.walkntrade.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.walkntrade.io.DataParser;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class PollMessagesTask extends AsyncTask<Void, Void, String> {

    private static final String TAG = "ASYNCTASK:PollMessages";

    private Context context;

    public PollMessagesTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(Void... params) {
        DataParser database = new DataParser(context);
        String serverResponse = null;

        try {
            serverResponse = database.simpleGetIntent(DataParser.INTENT_GET_NEWMESSAGE);
        } catch (IOException e) {
            Log.e(TAG, "Polling messages", e);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Polling messages", e);
        }

        return serverResponse;
    }
}
