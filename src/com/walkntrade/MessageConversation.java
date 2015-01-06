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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private String threadId, postTitle;
    private ListView chatList;
    private ChatThreadAdapter chatAdapter;
    private EditText newMessage;
    private ImageView send;
    boolean canSendMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_conversation);

        threadId = getIntent().getStringExtra(THREAD_ID);
        postTitle = getIntent().getStringExtra(POST_TITLE);

        context = getApplicationContext();
        GcmIntentService.resetNotfCounter(context); //Clears out all message notifications in Status Bar

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatList = (ListView) findViewById(R.id.chat_list);
        newMessage = (EditText) findViewById(R.id.edit_text);
        send = (ImageView) findViewById(R.id.send_message);

        getActionBar().setTitle(getIntent().getStringExtra(POST_TITLE));

        chatAdapter = new ChatThreadAdapter(context, R.layout.item_message_thread, new ArrayList<ChatObject>());
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
                        new AppendMessageTask().execute(newMessage.getText().toString());
                        textView.setText("");
                }

                return false;
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AppendMessageTask().execute(newMessage.getText().toString());
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

    private class ChatThreadAdapter extends ArrayAdapter<ChatObject> {

        public ChatThreadAdapter(Context context, int resource, List<ChatObject> chatObjects) {
            super(context, resource, chatObjects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View messageView;
            ChatObject item = getItem(position);

            boolean myMessage = false;

            if (item.getSenderName().equalsIgnoreCase(DataParser.getSharedStringPreference(getContext(), DataParser.PREFS_USER, DataParser.KEY_USER_NAME)))
                myMessage = true;

            if (myMessage)
                messageView = inflater.inflate(R.layout.item_message_user_me, parent, false);
            else
                messageView = inflater.inflate(R.layout.item_message_user_other, parent, false);

            ImageView userImage = (ImageView) messageView.findViewById(R.id.user_image);
            TextView contents = (TextView) messageView.findViewById(R.id.message_contents);
            TextView user = (TextView) messageView.findViewById(R.id.user);
            TextView date = (TextView) messageView.findViewById(R.id.date);

            if (item.hasImage())
                userImage.setImageBitmap(item.getCurrentUserImage());
            contents.setText(item.getContents());
            user.setText(item.getSenderName());
            date.setText(item.getDateTime());

            return messageView;
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
            super.onPreExecute();
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

            if (serverResponse == StatusCodeParser.STATUS_OK) {
                chatAdapter.addAll(chatObjects);
                chatList.setAdapter(chatAdapter);
                chatList.setSelection(chatAdapter.getCount() - 1);

                new UserAvatarRetrievalTask(chatObjects, false).execute(chatObjects.get(0).getUserImageUrl()); //Retrieve image for other user's avatar
                new UserAvatarRetrievalTask(chatObjects, true).execute(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL)); //Retrieve image for current user's avatar
            }
        }
    }

    private class AppendMessageTask extends AsyncTask<String, Void, Integer> {

        private String message;

        @Override
        protected void onPreExecute() {
            send.setVisibility(View.GONE);
        }

        @Override
        protected Integer doInBackground(String... strings) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            message = strings[0];
            try {
                serverResponse = database.appendMessage(threadId, strings[0]);
            } catch (IOException e) {
                Log.e(TAG, "Appending message thread", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);

            send.setVisibility(View.VISIBLE);
            ChatObject chatObject = new ChatObject(true,DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), message, "[current time]", true, DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL));
            chatAdapter.add(chatObject);
            chatAdapter.notifyDataSetChanged();
            chatList.setSelection(chatAdapter.getCount() - 1);
            newMessage.setText("");
            ArrayList<ChatObject> c = new ArrayList<ChatObject>();
            c.add(chatObject);
            new UserAvatarRetrievalTask(c, true).execute(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL));

            Toast.makeText(context, integer.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private class UserAvatarRetrievalTask extends AsyncTask<String, Void, Bitmap> {

        private ArrayList<ChatObject> chatObjects;
        private boolean myImage;

        public UserAvatarRetrievalTask(ArrayList<ChatObject> chatObjects, boolean myImage) {
            super();
            this.chatObjects = chatObjects;
            this.myImage = myImage;
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

                bm = imageCache.getBitmapFromDiskCache(key.substring(0, 1)); //Try to retrieve image from cache

                if (bm == null) { //If it doesn't exists, retrieve image from network

                    //Get width and height of image view, so it returns a more-optimized image. Save memory and fits better
                    ImageView userImageView = (ImageView) LayoutInflater.from(context).inflate(R.layout.item_message_user_me, chatList).findViewById(R.id.user_image);
                    int width = userImageView.getWidth();
                    int height = userImageView.getHeight();

                    Log.v(TAG, "User Image View: width - " + width + " height - " + height);
                    bm = DataParser.loadOptBitmap(avatarURL[0], width, height);
                }

                imageCache.addBitmapToCache(key.substring(0, 1), bm); //Finally cache bitmap. Will override cache if already exists or write new cache
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

                if (myImage) {
                    for (ChatObject c : chatObjects)
                        if (c.getSenderName().equalsIgnoreCase(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME)))
                            c.setCurrentUserImage(bitmap);
                } else {
                    for (ChatObject c : chatObjects)
                        if (!c.getSenderName().equalsIgnoreCase(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME)))
                            c.setCurrentUserImage(bitmap);
                }

                chatAdapter.notifyDataSetChanged();
            }
        }
    }
}
