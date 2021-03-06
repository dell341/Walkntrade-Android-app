package com.walkntrade;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.fragments.TaskFragment;
import com.walkntrade.gcm.GcmIntentService;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DatabaseHelper;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.FormatDateTime;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.MessageThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class Messages extends ActionBarActivity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, TaskFragment.TaskCallbacks {

    private static final String TAG = "Messages";
    private static final String TAG_TASK_FRAGMENT = "com.walkntrade.Messages.Task_Fragment";
    private static final String SAVED_INSTANCE_MESSAGES = "saved_instance_messages";
    private static final String SAVED_INSTANCE_PROGRESS_STATE = "saved_instance_progress_state";
    private static final String SAVED_PROGRESS_MESSAGE = "saved_instance_progress_message";
    private static final String SAVED_INSTANCE_ERROR_MESSAGE_STATE = "saved_instance_error_state";
    private static final String SAVED_PROGRESS_DIALOG_STATE = "saved_instance_progress_dialog_state";
    private static final String SAVED_EXISTING_DATABASE = "saved_instance_database_state";

    private Context context;
    private ProgressBar progressBar;
    private SwipeRefreshLayout refreshLayout;
    private TextView noResults;
    private ListView messageList;
    private MessageThreadAdapter threadAdapter;

    private ProgressDialog progressDialog;
    private String progressMessage = "";
    private boolean isDialogShowing = false;
    private boolean isProgressShowing = false;
    private boolean existingDatabase = false;
    private TaskFragment taskFragment;
    private static int amountToDelete = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        context = getApplicationContext();
        GcmIntentService.resetNotfCounter(context); //Clears out all message notifications in Status Bar

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        noResults = (TextView) findViewById(R.id.noResults);
        messageList = (ListView) findViewById(R.id.messageList);
        messageList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        messageList.setMultiChoiceModeListener(new MultiChoiceListener());
        messageList.setOnItemClickListener(this);

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(null);
        progressDialog.setCanceledOnTouchOutside(false);

        /*Fragment implementation is used for getting thread, to allow continuous download during configuration changes
        * i.e. device rotation
        */
        taskFragment = (TaskFragment) getFragmentManager().findFragmentByTag(TAG_TASK_FRAGMENT);
        if (taskFragment == null) //If this fragment already exists during onCreate, do not download message threads
            downloadMessageThreads();

        if (savedInstanceState != null) {
            ArrayList<MessageThread> messageThreads = savedInstanceState.getParcelableArrayList(SAVED_INSTANCE_MESSAGES);
            isDialogShowing = savedInstanceState.getBoolean(SAVED_PROGRESS_DIALOG_STATE);
            isProgressShowing = savedInstanceState.getBoolean(SAVED_INSTANCE_PROGRESS_STATE, true);
            progressBar.setVisibility(isProgressShowing ? View.VISIBLE : View.INVISIBLE);
            progressDialog.setMessage(savedInstanceState.getString(SAVED_PROGRESS_MESSAGE));
            progressMessage = savedInstanceState.getString(SAVED_PROGRESS_MESSAGE);
            existingDatabase = savedInstanceState.getBoolean(SAVED_EXISTING_DATABASE);

            if (isDialogShowing)
                progressDialog.show();

            if (messageThreads != null) {
                threadAdapter = new MessageThreadAdapter(context, messageThreads);
                messageList.setAdapter(threadAdapter);
            }
        } else {
            new PollMessagesTask(context).execute();

            //Load any messages from the database before downloading from the server
            DatabaseHelper databaseHelper = new DatabaseHelper(context);
            SQLiteDatabase db = databaseHelper.getReadableDatabase();

            Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseHelper.ThreadsEntry.TABLE_NAME + " LIMIT 50", null);

            ArrayList<MessageThread> messageThreads = new ArrayList<>(cursor.getCount());
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                existingDatabase = true;
                MessageThread messageThread = toMessageThread(cursor);
                getCachedUserImage(messageThread, false);
                messageThreads.add(messageThread);
                cursor.moveToNext();
            }
            threadAdapter = new MessageThreadAdapter(context, messageThreads);
            messageList.setAdapter(threadAdapter);

            cursor.close();
            db.close();
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(newMessagesReceiver, new IntentFilter(GcmIntentService.ACTION_NOTIFICATION_NEW));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void downloadMessageThreads() {
        Bundle args = new Bundle();
        args.putInt(TaskFragment.ARG_TASK_ID, TaskFragment.TASK_GET_MESSAGE_THREADS);
        taskFragment = new TaskFragment();
        taskFragment.setArguments(args);

        getFragmentManager().beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
    }

    private MessageThread toMessageThread(Cursor cursor) {
        String threadId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_THREAD_ID));
        String postIdentifier = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_POST_ID));
        String postTitle = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_POST_TITLE));
        String lastMessage = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_LAST_MESSAGE));
        String lastUserName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_LAST_USER_NAME));
        int lastUserId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_LAST_USER_ID));
        String lastDateTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_DATETIME));
        int userId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_RECIPIENT_ID));
        String userName = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_RECIPIENT_NAME));
        String userImageUrl = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_RECIPIENT_IMAGE));
        int newMessages = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ThreadsEntry.COLUMN_NEW_MESSAGES));

        return new MessageThread(threadId, postIdentifier, postTitle, lastMessage, lastUserName, lastUserId, lastDateTime, userId, userName, userImageUrl, newMessages);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                //Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, Feedback.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (threadAdapter != null)
            outState.putParcelableArrayList(SAVED_INSTANCE_MESSAGES, threadAdapter.getItems());
        outState.putBoolean(SAVED_INSTANCE_PROGRESS_STATE, isProgressShowing);
        outState.putString(SAVED_PROGRESS_MESSAGE, progressMessage);
        outState.putBoolean(SAVED_PROGRESS_DIALOG_STATE, isDialogShowing);
        outState.putBoolean(SAVED_EXISTING_DATABASE, existingDatabase);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(newMessagesReceiver);
    }

    private BroadcastReceiver newMessagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            taskFragment.runTask(TaskFragment.TASK_GET_MESSAGE_THREADS);
        }
    };

    @Override
    public void onRefresh() {
        downloadMessageThreads();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MessageThread message = (MessageThread) parent.getItemAtPosition(position);

        Intent showConversationIntent = new Intent(this, MessageConversation.class);
        showConversationIntent.putExtra(MessageConversation.THREAD_ID, message.getThreadId());
        showConversationIntent.putExtra(MessageConversation.POST_TITLE, message.getPostTitle());
        startActivity(showConversationIntent);

        message.clearNewMessages();
        //Updates database for specified message thread
        DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ThreadsEntry.COLUMN_NEW_MESSAGES, 0);
        String[] whereArgs = {message.getThreadId()};
        db.update(DatabaseHelper.ThreadsEntry.TABLE_NAME, values, DatabaseHelper.ThreadsEntry.COLUMN_THREAD_ID+"=?",whereArgs);
        db.close();
        threadAdapter.notifyDataSetChanged();
    }

    private class MultiChoiceListener implements AbsListView.MultiChoiceModeListener {
        ArrayList<String> messageIds = new ArrayList<String>();
        private int count = 0;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_post, menu);
            refreshLayout.setEnabled(false);
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean selected) {
            MessageThreadAdapter adapter = (MessageThreadAdapter) messageList.getAdapter();
            MessageThread item = adapter.getItem(position);

            if (selected) {
                messageIds.add(item.getThreadId());
                count++;
            } else {
                messageIds.remove(item.getThreadId());
                count--;
            }

            mode.setTitle(count + " message(s) selected");
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteMessageThreads();
                    mode.finish(); //Close the Contextual Action Bar
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            count = 0;
            refreshLayout.setEnabled(true);
        }

        private void deleteMessageThreads() {
            Bundle args = new Bundle();
            args.putInt(TaskFragment.ARG_TASK_ID, TaskFragment.TASK_REMOVE_MESSAGE_THREADS);
            args.putStringArrayList(TaskFragment.ARG_MESSAGES_THREAD_IDS, messageIds);
            taskFragment = new TaskFragment();
            taskFragment.setArguments(args);

            amountToDelete = messageIds.size();
            getFragmentManager().beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
        }
    }

    private void getCachedUserImage(MessageThread m, boolean updateAdapter) {
        try {
            String userImageUrl = m.getUserImageUrl();
            String[] splitURL = userImageUrl.split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            Bitmap bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

            if (bm == null) {//If it doesn't exists, retrieve image from network
                new UserAvatarRetrievalTask(m).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getUserImageUrl());
            } else {
                m.setBitmap(bm);
                if (updateAdapter)
                    threadAdapter.notifyDataSetChanged();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Image does not exist");
            //If user has not uploaded an image, leave Bitmap as null
        }
    }

    private class MessageThreadAdapter extends BaseAdapter {

        private Context context;
        private ArrayList<MessageThread> items;

        public MessageThreadAdapter(Context context, ArrayList<MessageThread> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public MessageThread getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        public boolean addAll(ArrayList<MessageThread> items) {
            return this.items.addAll(items);
        }

        public ArrayList<MessageThread> getItems() {
            return items;
        }

        public void clearData() {
            items.clear();
        }

        public void removeItems(String[] threadIds) { //Remove items matching the specified Ids
            for (String id : threadIds)
                for (Iterator<MessageThread> iterator = items.iterator(); iterator.hasNext(); ) { //Uses iterator because, cannot modify and iterate over ArrayList concurrently
                    MessageThread m = iterator.next();
                    if (m.getThreadId().equals(id))
                        iterator.remove();
                }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View messageView;

            if (convertView != null)
                messageView = convertView;
            else
                messageView = inflater.inflate(R.layout.item_message_thread, parent, false);

            MessageThread item = getItem(position);

            ImageView userImage = (ImageView) messageView.findViewById(R.id.user_image);
            TextView postTitle = (TextView) messageView.findViewById(R.id.message_title);
            TextView lastMessage = (TextView) messageView.findViewById(R.id.message_last);
            TextView lastMessageDate = (TextView) messageView.findViewById(R.id.message_last_date);

            postTitle.setText(item.getPostTitle());
            lastMessage.setText(item.getLastUserName() + " : " + item.getLastMessage());
            lastMessageDate.setText(FormatDateTime.formatDateTime(item.getLastDateTime()));
            if (item.hasImage()) {
                userImage.setImageBitmap(item.getUserImage());
            }

            if (item.getNewMessages() > 0) {
                postTitle.setTypeface(postTitle.getTypeface(), Typeface.BOLD);
                lastMessage.setTypeface(lastMessage.getTypeface(), Typeface.BOLD);
                lastMessage.setTextColor(getResources().getColor(R.color.black));
                lastMessageDate.setTypeface(lastMessageDate.getTypeface(), Typeface.BOLD);
                lastMessageDate.setTextColor(getResources().getColor(R.color.black));
            } else {
                postTitle.setTypeface(Typeface.DEFAULT);
                lastMessage.setTypeface(Typeface.DEFAULT);
                lastMessage.setTextColor(getResources().getColor(R.color.dark_gray_text));
                lastMessageDate.setTypeface(Typeface.DEFAULT);
                lastMessageDate.setTextColor(getResources().getColor(R.color.dark_gray_text));
            }

            return messageView;
        }
    }

    @Override
    public void onPreExecute(int taskId) {

        switch (taskId) {
            case TaskFragment.TASK_GET_MESSAGE_THREADS:
                if (!refreshLayout.isRefreshing() && !existingDatabase) {
                    progressBar.setVisibility(View.VISIBLE);
                    isProgressShowing = true;
                }
                break;
            case TaskFragment.TASK_REMOVE_MESSAGE_THREADS:
                isDialogShowing = true;
                if (amountToDelete > 1)
                    progressMessage = getString(R.string.removing_messages_multiple);
                else
                    progressMessage = getString(R.string.removing_messages);
                progressDialog.setMessage(progressMessage);
                progressDialog.show();
                break;
        }
        noResults.setVisibility(View.GONE);
        refreshLayout.setEnabled(false); //Do not allow drag to refresh, while performing a network call
    }

    @Override
    public void onProgressUpdate(int percent) {
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onPostExecute(int taskId, Object result) {
        progressBar.setVisibility(View.INVISIBLE);
        isProgressShowing = false;

        int requestStatus;

        switch (taskId) {
            case TaskFragment.TASK_GET_MESSAGE_THREADS:
                ObjectResult<ArrayList<MessageThread>> objectResult = (ObjectResult<ArrayList<MessageThread>>) result;
                requestStatus = objectResult.getStatus();

                if (requestStatus == StatusCodeParser.STATUS_OK) {
                    ArrayList<MessageThread> messageThreads = objectResult.getObject();

                    if (messageThreads.isEmpty()) {
                        noResults.setText(context.getString(R.string.no_messages));
                        noResults.setVisibility(View.VISIBLE);
                        messageList.setAdapter(null);
                    } else {
                        threadAdapter.clearData();
                        threadAdapter.addAll(messageThreads);
                        threadAdapter.notifyDataSetChanged();

                        DatabaseHelper databaseHelper = new DatabaseHelper(context);
                        SQLiteDatabase db = databaseHelper.getWritableDatabase();
                        db.delete(DatabaseHelper.ThreadsEntry.TABLE_NAME, null, null);

                        for (MessageThread m : messageThreads) {
                            getCachedUserImage(m, true);
                            ContentValues values = new ContentValues();
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_THREAD_ID, m.getThreadId());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_POST_ID, m.getPostIdentifier());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_POST_TITLE, m.getPostTitle());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_LAST_MESSAGE, m.getLastMessage());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_LAST_USER_ID, m.getLastUserId());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_LAST_USER_NAME, m.getLastUserName());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_DATETIME, m.getLastDateTime());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_RECIPIENT_ID, m.getUserId());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_RECIPIENT_NAME, m.getUserName());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_RECIPIENT_IMAGE, m.getUserImageUrl());
                            values.put(DatabaseHelper.ThreadsEntry.COLUMN_NEW_MESSAGES, m.getNewMessages());

                            long newRowId = db.insert(DatabaseHelper.ThreadsEntry.TABLE_NAME, null, values);
                            Log.d(TAG, "New table row : " + newRowId);
                        }

                        db.close();
                    }

                } else {
                    if (!existingDatabase) {
                        messageList.setAdapter(null);
                        noResults.setText(StatusCodeParser.getStatusString(context, requestStatus));
                        noResults.setVisibility(View.VISIBLE);
                    } else
                        Toast.makeText(context, StatusCodeParser.getStatusString(context, requestStatus), Toast.LENGTH_SHORT).show();
                }
                break;
            case TaskFragment.TASK_REMOVE_MESSAGE_THREADS:
                ObjectResult<String[]> objectResult1 = (ObjectResult<String[]>) result;
                requestStatus = objectResult1.getStatus();
                isDialogShowing = false;

                threadAdapter.removeItems(objectResult1.getObject());
                threadAdapter.notifyDataSetChanged();

                if (requestStatus == StatusCodeParser.STATUS_OK)
                    progressDialog.dismiss();
                else {
                    progressDialog.setMessage(StatusCodeParser.getStatusString(context, requestStatus));
                    progressDialog.setCancelable(true);
                    progressDialog.setCanceledOnTouchOutside(true);
                }

                if (threadAdapter.isEmpty()) {
                    noResults.setText(context.getString(R.string.no_messages));
                    noResults.setVisibility(View.VISIBLE);
                    messageList.setAdapter(null);
                }

                break;
        }

        refreshLayout.setRefreshing(false);
        refreshLayout.setEnabled(true);
    }

    private class UserAvatarRetrievalTask extends AsyncTask<String, Void, Bitmap> {

        private MessageThread messageThread;

        public UserAvatarRetrievalTask(MessageThread messageThread) {
            super();
            this.messageThread = messageThread;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... avatarURL) {
            Bitmap bm = null;
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            try {
                String[] splitURL = avatarURL[0].split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

                bm = DataParser.loadBitmap(avatarURL[0]);

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
                messageThread.setBitmap(bitmap);
                threadAdapter.notifyDataSetChanged();
            }
        }
    }
}
