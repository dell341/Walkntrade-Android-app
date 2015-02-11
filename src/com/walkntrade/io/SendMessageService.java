package com.walkntrade.io;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.MessageConversation;
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

    public static final String EXTRA_SERVER_RESPONSE = "com.walkntrade.io.server_response";
    public static final String EXTRA_RETURNED_DATA = "com.walkntrade.io.returned_data";
    public static final String EXTRA_POST_OBSID = "com.walkntrade.io.post_obsid";
    public static final String EXTRA_THREAD_ID = "com.walkntrade.io.thread_id";
    public static final String EXTRA_CONVERSATION_ITEM_INDEX = "com.walkntrade.io.conversation_index";
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
                final int itemIndex = intent.getIntExtra(EXTRA_CONVERSATION_ITEM_INDEX, -1);
                appendMessageThread(threadId, messageContents, itemIndex);
            }
        }
    }

    private void createMessageThread(String obsId, String messageContents) {
        DataParser database = new DataParser(getApplicationContext());
        ObjectResult<String []> result = new ObjectResult<>(StatusCodeParser.CONNECT_FAILED, null);

        try {
            result = database.createMessageThread(obsId, messageContents);
        } catch (IOException e) {
            Log.e(TAG, "Messaging user", e);
        } finally {
            Intent intent = new Intent(ACTION_CREATE_MESSAGE_THREAD);
            intent.putExtra(EXTRA_SERVER_RESPONSE, result.getStatus());
            if(result.getStatus() == StatusCodeParser.STATUS_OK)
                intent.putExtra(EXTRA_RETURNED_DATA, result.getObject());
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    private void appendMessageThread(String threadId, String messageContents, int itemIndex) {
        DataParser database = new DataParser(getApplicationContext());
        Integer serverResponse = StatusCodeParser.CONNECT_FAILED;

        try {
            serverResponse = database.appendMessage(threadId, messageContents);
        } catch (IOException e) {
            Log.e(TAG, "Messaging user", e);
        } finally {
            Intent intent = new Intent(ACTION_APPEND_MESSAGE_THREAD);
            intent.putExtra(EXTRA_SERVER_RESPONSE, serverResponse);
            intent.putExtra(EXTRA_CONVERSATION_ITEM_INDEX, itemIndex);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}
