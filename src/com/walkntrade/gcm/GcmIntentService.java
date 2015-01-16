package com.walkntrade.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.MessageConversation;
import com.walkntrade.Messages;
import com.walkntrade.R;
import com.walkntrade.adapters.item.ConversationItem;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.objects.ChatObject;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Handles notification events received from broadcast receiver
public class GcmIntentService extends IntentService {

    private static final String TAG = "GcmIntentService";
    public static final String ACTION_NOTIFICATION_BLOCKED = "com.walkntrade.gcm.gcmintentservice.block";
    public static final String ACTION_NOTIFICATION_NEW = "com.walkntrade.gcm.gcmintentservice.new";
    public static final int NOTIFICATION_ID = 1;

    private static ArrayList<String> threadIds = new ArrayList<String>();
    private static ArrayList<ChatObject> messageObjects = new ArrayList<ChatObject>();
    private static int numOfMessages = 0;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        //GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        //String messageType = gcm.getMessageType(intent);

        //If user is not logged, do not continue.
        if (!DataParser.isUserLoggedIn(getApplicationContext()))
            return;

        if (!extras.isEmpty()) {
            new PollMessagesTask(getApplicationContext()).execute(); //Look for new messages, when this message arrives.
            Log.v(TAG, "GCM-Push: "+extras.toString());

            String threadId = extras.getString("id");
            String user = extras.getString("user");
            String subject = StringEscapeUtils.unescapeHtml4(extras.getString("subject"));
            String contents = StringEscapeUtils.unescapeHtml4(extras.getString("message"));
            String dateTime = extras.getString("date");
            String imageUrl = extras.getString("userImageURL");

            if (threadId != null && user != null) //As long as id and user fields are not null, send the notification
                sendNotification(threadId, user, subject, contents, dateTime, imageUrl);
        } else
            Log.i(TAG, "Incomplete/Empty message received");
    }

    //Put the received message into a notification
    private void sendNotification(String threadId, String user, String subject, String contents, String dateTime, String imageUrl) {
        //Update all messages list
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ACTION_NOTIFICATION_NEW));

        //Send the message to the active conversation the user viewing
        if(threadId.equals(DataParser.getSharedStringPreference(getApplicationContext(), DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_ACTIVE_THREAD))) {
            Intent test = new Intent(ACTION_NOTIFICATION_BLOCKED);
            test.putExtra(MessageConversation.LIST_CONVERSATION, new ConversationItem(user, contents, dateTime.replace('/','-'), imageUrl, false, false));
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(test);

            //Do not create a notification if the activity is currently being viewed
            if(DataParser.getSharedBooleanPreference(getApplicationContext(), DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_DISPLAY_ON))
                return;
        }
        if (!DataParser.getSharedBooleanPreferenceTrueByDefault(this, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_USER)) //If user does want to receive notifications, do not create a notification
            return;

        messageObjects.add(new ChatObject(false, user, contents, dateTime, false, imageUrl));
        if(!threadIds.contains(threadId))
            threadIds.add(threadId);

        Intent showMessage;
        Intent notfBroadcast = new Intent(getApplicationContext(), NotificationBroadcastReceiver.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this); //Allows parent navigation after opening the app from the notification

        if(threadIds.size() > 1) {//If there are more than one conversations in the notification. Go to messages list, not individual message
            showMessage = new Intent(getApplicationContext(), Messages.class);
            stackBuilder.addParentStack(Messages.class);
        } else { //Else go straight to the individual message
            showMessage = new Intent(getApplicationContext(), MessageConversation.class);
            stackBuilder.addParentStack(MessageConversation.class);
        }

        stackBuilder.addNextIntent(showMessage); //Adds intent to the top of the stack
        showMessage.putExtra(MessageConversation.THREAD_ID, threadId);
        showMessage.putExtra(MessageConversation.POST_TITLE, subject);
        showMessage.setAction("ACTION_" + System.currentTimeMillis()); //Makes intents unique, so Android does not reuse invalid intents with null extras

        //Get dimensions the notification will need to be
        int largeIconWidth = Resources.getSystem().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        int largeIconHeight = Resources.getSystem().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.walkntrade_icon)
                .setContentTitle(getApplicationContext().getString(R.string.notification_from) + " " + user)
                .setContentText(contents)
                .setContentInfo(++numOfMessages + "") //Increment then add new messages amount
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        try {
            DiskLruImageCache imageCache = new DiskLruImageCache(getApplicationContext(), DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            String[] splitURL = imageUrl.split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            Bitmap userBitmap = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

            if (userBitmap == null)//If it doesn't exists, retrieve image from network
                userBitmap = DataParser.loadOptBitmap(imageUrl, largeIconWidth, largeIconHeight);

            if(userBitmap != null)
                builder.setLargeIcon(userBitmap);
        } catch (IOException e) {
            Log.e(TAG, "Could not load notification icon", e);
        } catch (ArrayIndexOutOfBoundsException e1) {
            Log.e(TAG, "User url not standard", e1);
        }

        //Set big view if more than one notification has been received
        if (numOfMessages > 1) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setSummaryText(DataParser.getSharedStringPreference(getApplicationContext(), DataParser.PREFS_USER, DataParser.KEY_USER_NAME));

            for (ChatObject m : messageObjects) {
                String userText = m.getSenderName();
                String contentText = m.getContents();

                if (messageObjects.indexOf(m) > 4) { //Inbox style only holds up to 5 lines of text
                    inboxStyle.setBigContentTitle(getApplicationContext().getString(R.string.notification_overflow));
                    break;
                } else {
                    inboxStyle.setBigContentTitle(messageObjects.size() + " " + getApplicationContext().getString(R.string.notification_title));
                    inboxStyle.addLine(userText + " : " + contentText);
                }
            }

            builder.setStyle(inboxStyle);
        }

        boolean hasSound = false;
        boolean vibrate = DataParser.getSharedBooleanPreferenceTrueByDefault(getApplicationContext(), DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_VIBRATE);
        boolean showLight = DataParser.getSharedBooleanPreferenceTrueByDefault(getApplicationContext(), DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_LIGHT);

        String sound = DataParser.getSoundPref(getApplicationContext());
        if (sound != null) {
            hasSound = true;
            builder.setSound(Uri.parse(DataParser.getSoundPref(getApplicationContext())));
        }

        if (!hasSound) {
            if (vibrate && showLight) {
                builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                builder.setLights(0xffffff, 500, 500);
            } else if (vibrate)
                builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
            else if (showLight) {
                builder.setDefaults(Notification.DEFAULT_SOUND);
                builder.setLights(0xff00ff, 500, 500);
            }
        } else if (vibrate && showLight) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            builder.setLights(0xffffff, 500, 500);
        } else if (vibrate)
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        else if (showLight)
            builder.setLights(0xff00ff, 500, 500);

        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, notfBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(contentIntent); //Fired when the notification is clicked
        builder.setDeleteIntent(deleteIntent); //Fired when notification is dismissed

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    public static void resetNotfCounter(Context context) {
        NotificationManager notfManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notfManager.cancelAll();

        threadIds = new ArrayList<String>();
        messageObjects = new ArrayList<ChatObject>();
        numOfMessages = 0;
    }

}
