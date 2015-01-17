package com.walkntrade.io;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.fragments.ContactUserFragment;

import java.io.IOException;


/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

/**
 * Handles asynchronous messaging, so that a message is sent sequentially
 * and regardless of whether or not the activity or application is still running
 */
public class SendMessageService extends IntentService {
    public static final String ACTION_CREATE_MESSAGE_THREAD = "com.walkntrade.io.create_message";
    public static final String ACTION_APPEND_MESSAGE_THREAD = "com.walkntrade.io.append_message";

    public static final String EXTRA_POST_OBSID = "com.walkntrade.io.post_obsid";
    public static final String EXTRA_THREAD_ID = "com.walkntrade.io.thread_id";
    public static final String EXTRA_MESSAGE_CONTENTS = "com.walkntrade.io.message_contents";

    private static final String TAG = "SendMessageService";

    public SendMessageService() {
        super("SendMessageService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CREATE_MESSAGE_THREAD.equals(action)) {
                final String obsId = intent.getStringExtra(EXTRA_POST_OBSID);
                final String messageContents = intent.getStringExtra(EXTRA_MESSAGE_CONTENTS);
                createMessageThread(obsId, messageContents);
            } else if (ACTION_APPEND_MESSAGE_THREAD.equals(action)) {
                final String threadId = intent.getStringExtra(EXTRA_THREAD_ID);
                final String messageContents = intent.getStringExtra(EXTRA_MESSAGE_CONTENTS);
            }
        }
    }

    private void createMessageThread(String obsId, String messageContents) {
        DataParser database = new DataParser(getApplicationContext());

        Integer serverResponse = StatusCodeParser.CONNECT_FAILED;

        try {
            serverResponse = database.createMessageThread(obsId, messageContents);
        } catch (IOException e) {
            Log.e(TAG, "Messaging user", e);
        } finally {
            Intent intent = new Intent(ACTION_CREATE_MESSAGE_THREAD);
            intent.putExtra(ContactUserFragment.EXTRA_SERVER_RESPONSE, serverResponse);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    private void appendMessageThread(String threadId, String messageContents) {

    }
}
