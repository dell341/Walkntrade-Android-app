package com.walkntrade;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
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

import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.fragments.TaskFragment;
import com.walkntrade.gcm.GcmIntentService;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.FormatDateTime;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.MessageThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class Messages extends Activity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, TaskFragment.TaskCallbacks {

    private static final String TAG = "Messages";
    private static final String TAG_TASK_FRAGMENT = "Task_Fragment";
    private static final String SAVED_INSTANCE_MESSAGES = "saved_instance_messages";
    private static final String SAVED_INSTANCE_PROGRESS_STATE = "saved_instance_progress_state";
    private static final String SAVED_INSTANCE_ERROR_MESSAGE_STATE = "saved_instance_error_state";

    private Context context;
    private ProgressBar progressBar;
    private SwipeRefreshLayout refreshLayout;
    private TextView noResults;
    private ListView messageList;
    private MessageThreadAdapter threadAdapter;

    private Fragment taskFragment;

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

        /*Fragment implementation is used for getting thread, to allow continuous download during configuration
        * i.e. device rotation
        */
        taskFragment = getFragmentManager().findFragmentByTag(TAG_TASK_FRAGMENT);
        if(taskFragment == null) //If this Fragment already exists during onCreate, do not download message threads
            downloadMessageThreads();

        if(savedInstanceState != null) {
            ArrayList<MessageThread> messages = savedInstanceState.getParcelableArrayList(SAVED_INSTANCE_MESSAGES);
            progressBar.setVisibility(savedInstanceState.getInt(SAVED_INSTANCE_PROGRESS_STATE) == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);

            if(messages != null) {
                threadAdapter = new MessageThreadAdapter(context, messages);
                messageList.setAdapter(threadAdapter);
            }
        } else {
            new PollMessagesTask(context).execute();
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(newMessagesReceiver, new IntentFilter(GcmIntentService.ACTION_NOTIFICATION_NEW));
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void downloadMessageThreads() {
        taskFragment = new TaskFragment();
        Bundle args = new Bundle();
        args.putInt(TaskFragment.ARG_TASK_ID, TaskFragment.TASK_GET_MESSAGE_THREADS);
        taskFragment.setArguments(args);

        getFragmentManager().beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
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
        if(threadAdapter != null)
            outState.putParcelableArrayList(SAVED_INSTANCE_MESSAGES, threadAdapter.getItems());
        outState.putInt(SAVED_INSTANCE_PROGRESS_STATE, progressBar.getVisibility());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(newMessagesReceiver);
    }

    private BroadcastReceiver newMessagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            downloadMessageThreads();
        }
    };

    @Override
    public void onRefresh() {
        refreshLayout.setRefreshing(true);
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
        threadAdapter.notifyDataSetChanged();
        messageList.setAdapter(threadAdapter);
    }

    private class MultiChoiceListener implements AbsListView.MultiChoiceModeListener {
        ArrayList<String> messageIds = new ArrayList<String>();
        private int count = 0;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_post, menu);
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
                    new DeleteThreadTask().execute(messageIds);
                    mode.finish(); //Close the Contextual Action Bar
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            count = 0;
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

        public ArrayList<MessageThread> getItems() {
            return items;
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
            if (item.hasImage())
                userImage.setImageBitmap(item.getUserImage());
            if (item.getNewMessages() > 0) {
                postTitle.setTypeface(postTitle.getTypeface(), Typeface.BOLD);
                lastMessage.setTypeface(lastMessage.getTypeface(), Typeface.BOLD);
                lastMessage.setTextColor(getResources().getColor(R.color.black));
                lastMessageDate.setTypeface(lastMessageDate.getTypeface(), Typeface.BOLD);
                lastMessageDate.setTextColor(getResources().getColor(R.color.black));
            }

            return messageView;
        }
    }

    @Override
    public void onPreExecute(int taskId) {
        if(!refreshLayout.isRefreshing())
            progressBar.setVisibility(View.VISIBLE);

        noResults.setVisibility(View.GONE);
    }

    @Override
    public void onProgressUpdate(int percent) {
    }

    @Override
    public void onCancelled() {
    }

    @Override
    public void onPostExecute(int taskId, Object result) {
        progressBar.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);

        ObjectResult<ArrayList<MessageThread>> objectResult = (ObjectResult<ArrayList<MessageThread>>)result;
        int serverResponse = objectResult.getStatus();

        if(serverResponse == StatusCodeParser.STATUS_OK) {
            ArrayList<MessageThread> messageThreads = objectResult.getObject();

            if(messageThreads.isEmpty()) {
                noResults.setText(context.getString(R.string.no_messages));
                noResults.setVisibility(View.VISIBLE);
            } else {
                threadAdapter = new MessageThreadAdapter(context, messageThreads);
                messageList.setAdapter(threadAdapter);

                for (MessageThread m : messageThreads)
                    new UserAvatarRetrievalTask(m).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, m.getUserImageUrl());
            }

        } else {
            messageList.setAdapter(null);
            noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
            noResults.setVisibility(View.VISIBLE);
        }
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

                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

                if (bm == null) //If it doesn't exists, retrieve image from network
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

    private class DeleteThreadTask extends AsyncTask<ArrayList<String>, Void, Integer> {
        @Override
        protected void onPreExecute() {
            refreshLayout.setRefreshing(true);
        }

        @Override
        protected Integer doInBackground(ArrayList<String>... messagesToDelete) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                for (String s : messagesToDelete[0])
                    serverResponse = database.deleteThread(s);
            } catch (IOException e) {
                Log.e(TAG, "Deleting message(s)", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            downloadMessageThreads();
        }
    }
}
