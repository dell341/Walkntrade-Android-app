package com.walkntrade.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.MessageConversation;
import com.walkntrade.Messages;
import com.walkntrade.R;
import com.walkntrade.SchoolPage;
import com.walkntrade.adapters.item.ConversationItem;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.objects.ChatObject;
import com.walkntrade.objects.MessageThread;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Handles notification events received from broadcast receiver
public class GcmIntentService extends IntentService {

    private static final String TAG = "GcmIntentService";
    public static final String NOTIFICATION_BLOCKED = "com.walkntrade.gcm.gcmintentservice.block";
    public static final String NOTIFICATION_NEW = "com.walkntrade.gcm.gcmintentservice.new";
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

        //If user does want to receive notifications and is not logged, do not continue.
        if (!DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_USER) && !DataParser.getSharedBooleanPreference(this, DataParser.PREFS_USER, DataParser.KEY_CURRENTLY_LOGGED_IN))
            return;

        if (!extras.isEmpty()) {
            new PollMessagesTask(getApplicationContext()).execute(); //Poll new message, when this message arrived.
            //Log.d(TAG, "GCM-Push: "+extras.toString());

            String threadId = extras.getString("id");
            String user = extras.getString("user");
            String subject = extras.getString("subject");
            String contents = extras.getString("message");
            String date = extras.getString("date");
            String imageUrl = extras.getString("userImageURL");

            if (threadId != null && user != null) //As long as id and user fields are not null, send the notification
                sendNotification(threadId, user, subject, contents, date, imageUrl);
        } else
            Log.i(TAG, "Incomplete/Empty message received");
    }

    //Put the received message into a notification
    private void sendNotification(String threadId, String user, String subject, String contents, String date, String imageUrl) {
        //Update all messages list
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(NOTIFICATION_NEW));

        //Do not create notification if user is viewing the conversation
        if(threadId.equals(DataParser.getSharedStringPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_ACTIVE_THREAD))) {

            Intent test = new Intent(NOTIFICATION_BLOCKED);
            test.putExtra(MessageConversation.LIST_CONVERSATION, new ConversationItem(user, contents, date, imageUrl, false, false));
            LocalBroadcastManager.getInstance(this).sendBroadcast(test);
            return;
        }

        messageObjects.add(new ChatObject(false, user, contents, date, false, imageUrl));
        numOfMessages++;

        if(!threadIds.contains(threadId))
            threadIds.add(threadId);

        Intent showMessage;
        Intent notfBroadcast = new Intent(this, NotificationBroadcastReceiver.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this); //Allows parent navigation after opening the app from the notification

        if(threadIds.size() > 1) {//If there are more than one conversations in the notification. Go to messages list, not individual message
            showMessage = new Intent(this, Messages.class);
            stackBuilder.addParentStack(Messages.class);
        } else { //Else go straight to the individual message
            showMessage = new Intent(this, MessageConversation.class);
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
        try {
            builder.setSmallIcon(R.drawable.walkntrade_icon)
                    .setLargeIcon(DataParser.loadOptBitmap(imageUrl, largeIconWidth, largeIconHeight))
                    .setContentTitle(getApplicationContext().getString(R.string.notification_from) + " " + user)
                    .setContentText(contents)
                    .setContentInfo(numOfMessages + "")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
        } catch (IOException e) {
            e.printStackTrace();
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
        boolean vibrate = DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_VIBRATE);
        boolean showLight = DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.KEY_NOTIFY_LIGHT);

        String sound = DataParser.getSoundPref(this);
        if (sound != null) {
            hasSound = true;
            builder.setSound(Uri.parse(DataParser.getSoundPref(this)));
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
