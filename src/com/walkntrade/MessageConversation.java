package com.walkntrade;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.adapters.MessageConversationAdapter;
import com.walkntrade.adapters.item.ConversationItem;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.fragments.TaskFragment;
import com.walkntrade.gcm.GcmIntentService;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DatabaseHelper;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.SendMessageService;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.ChatObject;

import java.io.IOException;
import java.util.ArrayList;


/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MessageConversation extends ActionBarActivity implements TaskFragment.TaskCallbacks{

    private static final String TAG = "MessageConversation";
    private static final String TAG_TASK_FRAGMENT = "Task_Fragment";
    private static final String SAVED_INSTANCE_CONVERSATION = "saved_instance_state_conversation";
    private static final String SAVED_INSTANCE_PROGRESS_STATE = "saved_instance_progress_state";
    private static final String SAVED_INSTANCE_ERROR_MESSAGE_STATE = "saved_instance_error_state";
    private static final String SAVED_EXISTING_DATABASE = "saved_instance_database_state";
    public static final String FROM_CONTACT_FRAGMENT = "extra_incoming_from_contact_user";
    public static final String LIST_CONVERSATION = "extra_arraylist_conversation";
    public static final String THREAD_ID = "id_of_current_message_thread";
    public static final String POST_TITLE = "title_of_current_post";


    private Context context;
    private ProgressBar progressBar;
    private String threadId;
    private ListView chatList;
    private TextView errorMessage;
    private EditText newMessage;
    private ImageView send;
    private boolean canSendMessage = false;
    private boolean existingDatabase = false;

    private MessageConversationAdapter conversationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_conversation);

        threadId = getIntent().getStringExtra(THREAD_ID);
        getSupportActionBar().setTitle(getIntent().getStringExtra(POST_TITLE));

        if(!getIntent().getBooleanExtra(FROM_CONTACT_FRAGMENT, false))
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        context = getApplicationContext();
        GcmIntentService.resetNotfCounter(context); //Clears out all message notifications in Status Bar

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatList = (ListView) findViewById(R.id.chat_list);
        errorMessage = (TextView) findViewById(R.id.error_message);
        newMessage = (EditText) findViewById(R.id.edit_text);
        send = (ImageView) findViewById(R.id.send_message);

       /*Fragment implementation is used for getting thread, to allow continuous download during configuration changes
        * i.e. device rotation
        */
        FragmentManager fm = getFragmentManager();
        TaskFragment taskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
        if(taskFragment == null) {
            taskFragment = new TaskFragment();
            Bundle args = new Bundle();
            args.putString(TaskFragment.ARG_THREAD_ID, threadId);
            args.putInt(TaskFragment.ARG_TASK_ID, TaskFragment.TASK_GET_CHAT_THREAD);
            taskFragment.setArguments(args);

            fm.beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
        }

        if(savedInstanceState != null) {
            ArrayList<ConversationItem> items = savedInstanceState.getParcelableArrayList(SAVED_INSTANCE_CONVERSATION);
            progressBar.setVisibility(savedInstanceState.getInt(SAVED_INSTANCE_PROGRESS_STATE) == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
            errorMessage.setVisibility(savedInstanceState.getInt(SAVED_INSTANCE_ERROR_MESSAGE_STATE) == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
            existingDatabase = savedInstanceState.getBoolean(SAVED_EXISTING_DATABASE);

            if (items!= null) {
                conversationAdapter = new MessageConversationAdapter(context, items);
                chatList.setAdapter(conversationAdapter);
                chatList.setSelection(conversationAdapter.getCount() - 1);
            }
        } else {
            new PollMessagesTask(context).execute();

            //Loads any chat bubbles from the database before downloading from the server
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            String[] whereArgs = {threadId};
            Cursor cursor = db.rawQuery("SELECT * FROM "+ DatabaseHelper.ConversationEntry.TABLE_NAME+" WHERE "+DatabaseHelper.ConversationEntry.COLUMN_THREAD_ID+"=?", whereArgs);

            ArrayList<ConversationItem> items = new ArrayList<>();
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                existingDatabase = true;
                ConversationItem item = toConversationItem(cursor);
                getCachedUserImage(item, false);
                items.add(item);
                cursor.moveToNext();
            }
            conversationAdapter = new MessageConversationAdapter(context, items);
            chatList.setAdapter(conversationAdapter);
            chatList.setSelection(conversationAdapter.getCount() - 1);
            send.setEnabled(true);

            cursor.close();
            db.close();
        }

        newMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean hasText = editable.length() > 0;
                canSendMessage = hasText;
                send.setVisibility(hasText ? View.VISIBLE : View.INVISIBLE);
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Hide keyboard if send button was pressed
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(newMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                send.setVisibility(View.GONE);
                String messageContents = newMessage.getText().toString();
                ConversationItem newConversationItem = new ConversationItem(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), messageContents, "just now", DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL),true, true);
                conversationAdapter.addItem(newConversationItem);
                conversationAdapter.notifyDataSetChanged();
                new UserAvatarRetrievalTask(newConversationItem).execute();

                newMessage.setText("");
                chatList.smoothScrollToPosition(conversationAdapter.getCount() - 1);
                Intent appendMessage = new Intent(MessageConversation.this, SendMessageService.class);
                appendMessage.setAction(SendMessageService.ACTION_APPEND_MESSAGE_THREAD);
                appendMessage.putExtra(SendMessageService.EXTRA_THREAD_ID, threadId);
                appendMessage.putExtra(SendMessageService.EXTRA_MESSAGE_CONTENTS, messageContents);
                appendMessage.putExtra(SendMessageService.EXTRA_CONVERSATION_ITEM_INDEX, conversationAdapter.getIndexOfItem(newConversationItem));
                startService(appendMessage);

                send.setVisibility(View.VISIBLE);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(GcmIntentService.ACTION_NOTIFICATION_BLOCKED);
        filter.addAction(SendMessageService.ACTION_APPEND_MESSAGE_THREAD);
        LocalBroadcastManager.getInstance(context).registerReceiver(messageConversationReceiver, filter);
        DataParser.setSharedStringPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_ACTIVE_THREAD, threadId); //Set this conversation as active, to disable notifications
    }

    private ConversationItem toConversationItem(Cursor cursor) {
        String senderName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ConversationEntry.COLUMN_SENDER_NAME));
        String contents = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ConversationEntry.COLUMN_CONTENTS));
        String dateTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ConversationEntry.COLUMN_DATETIME));
        String imageUrl = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ConversationEntry.COLUMN_SENDER_IMAGE));
        boolean sentFromMe = (cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ConversationEntry.COLUMN_SENT_FROM_ME)) == 1);
        return new ConversationItem(senderName, contents, dateTime, imageUrl, sentFromMe, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new MarkThreadAsRead().execute();
        GcmIntentService.resetNotfCounter(context); //Clears out all message notifications in Status Bar
        DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_DISPLAY_ON, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_DISPLAY_ON, false);
    }

    @Override
    protected void onDestroy() {
        DataParser.setSharedStringPreference(context, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_ACTIVE_THREAD, null);
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageConversationReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver messageConversationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(GcmIntentService.ACTION_NOTIFICATION_BLOCKED)) { //Intercepting messages on an active chat thread
                ConversationItem item = intent.getParcelableExtra(LIST_CONVERSATION);

                if (conversationAdapter == null)
                    Log.w(TAG, "Conversation Adapter is null");
                else {
                    new UserAvatarRetrievalTask(item).execute();
                    conversationAdapter.addItem(item);
                    conversationAdapter.notifyDataSetChanged();
                    chatList.smoothScrollToPosition(conversationAdapter.getCount() - 1);

                    if (DataParser.getSharedBooleanPreference(getApplicationContext(), DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_DISPLAY_ON))
                        new MarkThreadAsRead().execute(); //Mark this thread as read, if this conversation is actively being viewed. But only if the screen currently displaying the app
                }
            }
           else if(intent.getAction().equals(SendMessageService.ACTION_APPEND_MESSAGE_THREAD)) {
                int serverResponse = intent.getIntExtra(SendMessageService.EXTRA_SERVER_RESPONSE, StatusCodeParser.CONNECT_FAILED);
                int itemIndex = intent.getIntExtra(SendMessageService.EXTRA_CONVERSATION_ITEM_INDEX, -1);

                if(itemIndex == -1) {
                    Toast.makeText(context, context.getString(R.string.error_email_edu), Toast.LENGTH_SHORT).show();
                    return;
                }

                ConversationItem conversationItem = conversationAdapter.getItem(itemIndex);

                if(serverResponse == StatusCodeParser.STATUS_OK) {
                    conversationItem.messageDelivered();
                    DatabaseHelper databaseHelper = new DatabaseHelper(context);
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();

                    ContentValues values = new ContentValues();
                    values.put(DatabaseHelper.ConversationEntry.COLUMN_THREAD_ID, threadId);
                    values.put(DatabaseHelper.ConversationEntry.COLUMN_SENT_FROM_ME, (conversationItem.isSentFromMe()? 1 : 0));
                    values.put(DatabaseHelper.ConversationEntry.COLUMN_CONTENTS, conversationItem.getContents());
                    values.put(DatabaseHelper.ConversationEntry.COLUMN_DATETIME, conversationItem.getDateTime());
                    values.put(DatabaseHelper.ConversationEntry.COLUMN_SENDER_NAME, conversationItem.getSenderName());
                    values.put(DatabaseHelper.ConversationEntry.COLUMN_SENDER_IMAGE, conversationItem.getImageUrl());
                    db.insert(DatabaseHelper.ConversationEntry.TABLE_NAME, null, values);
                    db.close();
                }
                else
                    conversationItem.messageFailedToDeliver(StatusCodeParser.getStatusString(context, serverResponse));

                conversationAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                NavUtils.navigateUpFromSameTask(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(conversationAdapter != null)
            outState.putParcelableArrayList(SAVED_INSTANCE_CONVERSATION, conversationAdapter.getItems());
        outState.putInt(SAVED_INSTANCE_PROGRESS_STATE, progressBar.getVisibility());
        outState.putInt(SAVED_INSTANCE_ERROR_MESSAGE_STATE, errorMessage.getVisibility());
        outState.putBoolean(SAVED_EXISTING_DATABASE, existingDatabase);
    }

    private void getCachedUserImage(ConversationItem item, boolean updateAdapter) {
        try {
            String[] splitURL = item.getImageUrl().split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            Bitmap bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

            if (bm == null) {//If it doesn't exists, retrieve image from network
                new UserAvatarRetrievalTask(item).execute();
            } else {
                item.setAvatar(bm);
                if(updateAdapter)
                conversationAdapter.notifyDataSetChanged();
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Image does not exist", e);
            //If user has not uploaded an image, leave Bitmap as null
        }
    }

    @Override
    public void onPreExecute(int taskId) {

        switch (taskId) {
            case TaskFragment.TASK_GET_CHAT_THREAD:
                if(!existingDatabase)
                progressBar.setVisibility(View.VISIBLE);
                send.setEnabled(false); break;
        }

    }

    @Override
    public void onProgressUpdate(int percent) {
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onPostExecute(int taskId, Object result) {

        switch (taskId) {
            case TaskFragment.TASK_GET_CHAT_THREAD:
                progressBar.setVisibility(View.GONE);
                ObjectResult<ArrayList<ChatObject>> objectResult = (ObjectResult<ArrayList<ChatObject>>)result;
                int serverResponse = objectResult.getStatus();

                if (serverResponse == StatusCodeParser.STATUS_OK) {
                    ArrayList<ChatObject> chatObjects = objectResult.getObject();
                    ArrayList<ConversationItem> conversationItems = new ArrayList<ConversationItem>();

                    DatabaseHelper databaseHelper = new DatabaseHelper(context);
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    String[] whereArgs = {threadId};
                    db.delete(DatabaseHelper.ConversationEntry.TABLE_NAME, DatabaseHelper.ConversationEntry.COLUMN_THREAD_ID+"=?", whereArgs);

                    for (ChatObject c : chatObjects) {
                        ConversationItem item = new ConversationItem(c.getSenderName(), c.getContents(), c.getDateTime(), c.getUserImageUrl(), c.isSentFromMe(), false);
                        conversationItems.add(item);

                        getCachedUserImage(item, true);

                        //Save chat objects into database for later use
                        ContentValues values = new ContentValues();
                        values.put(DatabaseHelper.ConversationEntry.COLUMN_THREAD_ID, threadId);
                        values.put(DatabaseHelper.ConversationEntry.COLUMN_SENT_FROM_ME, (c.isSentFromMe()? 1 : 0));
                        values.put(DatabaseHelper.ConversationEntry.COLUMN_CONTENTS, c.getContents());
                        values.put(DatabaseHelper.ConversationEntry.COLUMN_DATETIME, c.getDateTime());
                        values.put(DatabaseHelper.ConversationEntry.COLUMN_SENDER_NAME, c.getSenderName());
                        values.put(DatabaseHelper.ConversationEntry.COLUMN_SENDER_IMAGE, c.getUserImageUrl());
                        db.insert(DatabaseHelper.ConversationEntry.TABLE_NAME, null, values);
                    }

                    db.close();
                    int previousSize = conversationAdapter.getCount();
                    conversationAdapter.clearData();
                    conversationAdapter.addAll(conversationItems);
                    conversationAdapter.notifyDataSetChanged();
                    if(previousSize != conversationAdapter.getCount())
                        chatList.smoothScrollToPosition(conversationAdapter.getCount() - 1);
                    send.setEnabled(true);
                } else {
                    errorMessage.setText(StatusCodeParser.getStatusString(context, serverResponse));
                    errorMessage.setVisibility(View.VISIBLE);
                } break;
        }
    }

    private class MarkThreadAsRead extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                serverResponse = database.markThreadAsRead(threadId);
            } catch (IOException e) {
                Log.e(TAG, "Marking thread as read", e);
            }

            return serverResponse;
        }
    }

    private class UserAvatarRetrievalTask extends AsyncTask<Void, Void, Bitmap> {

        private ConversationItem conversationItem;

        public UserAvatarRetrievalTask(ConversationItem conversationItem) {
            super();
            this.conversationItem = conversationItem;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bm = null;
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            try {
                String[] splitURL = conversationItem.getImageUrl().split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

                bm = DataParser.loadBitmap(conversationItem.getImageUrl());
                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Retrieving user avatar", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Image does not exist", e);
                //If user has not uploaded an image, leave Bitmap as null
            } finally {
                imageCache.close();
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (bitmap != null) {
                conversationItem.setAvatar(bitmap);
                conversationAdapter.notifyDataSetChanged();
            }

        }
    }
}
