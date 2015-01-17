package com.walkntrade.asynctasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.SchoolPage;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class PollMessagesTask extends AsyncTask<Void, Void, Integer> {

    private static final String TAG = "PollMessages";
    private Context context;
    private int lastMessageValue;

    public PollMessagesTask(Context context) {
        this.context = context;
        lastMessageValue = DataParser.getSharedIntPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_MESSAGES);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        DataParser database = new DataParser(context);
        int serverResponse = StatusCodeParser.CONNECT_FAILED;

        try {
            ObjectResult<Integer> result = database.getNewMessages();
            serverResponse = result.getStatus();
        } catch (IOException e) {
            Log.e(TAG, "Polling messages", e);
        }
        return serverResponse;
    }

    @Override
    protected void onPostExecute(Integer serverResponse) {
        if(serverResponse == StatusCodeParser.STATUS_OK) {
            //Only update visible unread message amounts if it has changed
            if(lastMessageValue != DataParser.getSharedIntPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_MESSAGES))
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SchoolPage.ACTION_UPDATE_DRAWER));
        }
    }
}
