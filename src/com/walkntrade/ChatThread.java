package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

public class ChatThread extends Activity {

    private static final String TAG = "ChatThread";

    private Context context;
    private ProgressBar progressBar;
    private ListView chatList;
    private ChatThreadAdapter chatAdapter;
    private EditText newMessage;
    private ImageView send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_conversation);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        chatList = (ListView) findViewById(R.id.chat_list);
        newMessage = (EditText) findViewById(R.id.edit_text);
        send = (ImageView) findViewById(R.id.send_message);

        chatAdapter = new ChatThreadAdapter(context, R.layout.item_message_thread, new ArrayList<ChatObject>());
        new GetChatThreadTask().execute("k3lrlk23nd2");

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = newMessage.getText().toString();

                ChatObject chatObject = new ChatObject("jksndkadan", "ihakhskjahsk",99, DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), text, "[current time]", false);
                chatAdapter.add(chatObject);
                chatAdapter.notifyDataSetChanged();
                chatList.setSelection(chatAdapter.getCount()-1);
                newMessage.setText("");
                ArrayList<ChatObject> c = new ArrayList<ChatObject>();
                c.add(chatObject);
                new UserAvatarRetrievalTask(c, true).execute(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL));
            }
        });
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

            if (item.getUser().equalsIgnoreCase(DataParser.getSharedStringPreference(getContext(), DataParser.PREFS_USER, DataParser.KEY_USER_NAME)))
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
            user.setText(item.getUser());
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
                DataParser.ObjectResult<ArrayList<ChatObject>> result = database.getMessageThread(strings[0], -1);
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
                chatList.setSelection(chatAdapter.getCount()-1);

                new UserAvatarRetrievalTask(chatObjects, false).execute(chatObjects.get(0).getUserImageUrl()); //Retrieve image for other user's avatar
                new UserAvatarRetrievalTask(chatObjects, true).execute(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL)); //Retrieve image for current user's avatar
            }
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
                String splitURL[] = avatarURL[0].split("_");
                String key = splitURL[2]; //The URL will also be used as the key to cache their avatar image

                bm = imageCache.getBitmapFromDiskCache(key.substring(0, 1)); //Try to retrieve image from cache

                if (bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(avatarURL[0]);

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
                        if (c.getUser().equalsIgnoreCase(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME)))
                            c.setCurrentUserImage(bitmap);
                } else {
                    for (ChatObject c : chatObjects)
                        if (!c.getUser().equalsIgnoreCase(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME)))
                            c.setCurrentUserImage(bitmap);
                }

                chatAdapter.notifyDataSetChanged();
            }
        }
    }
}
