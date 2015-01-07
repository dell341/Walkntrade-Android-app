package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.adapters.MessageConversationAdapter;
import com.walkntrade.adapters.item.ConversationItem;
import com.walkntrade.gcm.GcmIntentService;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.ChatObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MessageConversation extends Activity {

    private static final String TAG = "MessageConversation";
    public static final String THREAD_ID = "id_of_current_message_thread";
    public static final String POST_TITLE = "title_of_current_post";

    private Context context;
    private ProgressBar progressBar;
    private String threadId;
    private ListView chatList;
    private EditText newMessage;
    private ImageView send;
    boolean canSendMessage = false;

    private MessageConversationAdapter conversationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_conversation);

        threadId = getIntent().getStringExtra(THREAD_ID);
        getActionBar().setTitle(getIntent().getStringExtra(POST_TITLE));

        context = getApplicationContext();
        GcmIntentService.resetNotfCounter(context); //Clears out all message notifications in Status Bar

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatList = (ListView) findViewById(R.id.chat_list);
        newMessage = (EditText) findViewById(R.id.edit_text);
        send = (ImageView) findViewById(R.id.send_message);

        new GetChatThreadTask().execute(threadId);

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

        newMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEND:
                        new AppendMessageTask(newMessage.getText().toString()).execute();
                        textView.setText("");
                }

                return false;
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Hide keyboard if send button was pressed
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(newMessage.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                new AppendMessageTask(newMessage.getText().toString()).execute();
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, Privacy_Feedback.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GetChatThreadTask extends AsyncTask<String, Void, Integer> {

        ArrayList<ChatObject> chatObjects;

        public GetChatThreadTask() {
            super();
            chatObjects = new ArrayList<ChatObject>();
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... strings) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                DataParser.ObjectResult<ArrayList<ChatObject>> result = database.retrieveThread(strings[0]);
                serverResponse = result.getStatus();
                chatObjects = result.getObject();

            } catch (IOException e) {
                Log.e(TAG, "Getting Chat Thread", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressBar.setVisibility(View.GONE);

            if (serverResponse == StatusCodeParser.STATUS_OK) {
                ArrayList<ConversationItem> conversationItems = new ArrayList<ConversationItem>();

                for(ChatObject c : chatObjects) {
                    ConversationItem item = new ConversationItem(c.getSenderName(), c.getContents(), c.getDateTime(), c.getDateTime(), c.isSentFromMe(), false);
                    conversationItems.add(item);

                    new UserAvatarRetrievalTask(item).execute(c.getUserImageUrl());
                }

                conversationAdapter = new MessageConversationAdapter(context, conversationItems);
                chatList.setAdapter(conversationAdapter);
                chatList.setSelection(conversationAdapter.getCount() - 1);
            }
        }
    }

    private class AppendMessageTask extends AsyncTask<Void, Void, Integer> {

        private ConversationItem conversationItem;
        private String message;

        public AppendMessageTask(String m) {
            super();
            this.message = m;
            conversationItem = new ConversationItem(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), m, "[Current date]", "[Current Time]", true, true);
        }

        @Override
        protected void onPreExecute() {
            send.setVisibility(View.GONE);
            conversationAdapter.addItem(conversationItem);
            new UserAvatarRetrievalTask(conversationItem).execute(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL));
            chatList.setSelection(conversationAdapter.getCount() - 1);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;
            try {
                serverResponse = database.appendMessage(threadId, message);
            } catch (IOException e) {
                Log.e(TAG, "Appending message thread", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            send.setVisibility(View.VISIBLE);
            newMessage.setText("");
            conversationItem.messageDelivered();
            conversationAdapter.notifyDataSetChanged();
        }
    }

    private class UserAvatarRetrievalTask extends AsyncTask<String, Void, Bitmap> {

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
        protected Bitmap doInBackground(String... avatarURL) {
            Bitmap bm = null;
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            try {
                String[] splitURL = avatarURL[0].split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

                if (bm == null)//If it doesn't exists, retrieve image from network
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
                conversationItem.setAvatar(bitmap);
                conversationAdapter.notifyDataSetChanged();
            }

        }
    }
}
