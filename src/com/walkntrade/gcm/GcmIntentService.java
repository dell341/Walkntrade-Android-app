package com.walkntrade.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.walkntrade.MessageObject;
import com.walkntrade.Messages;
import com.walkntrade.R;
import com.walkntrade.ShowMessage;
import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.ArrayList;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class GcmIntentService extends IntentService {

    private static final String TAG = "GcmIntentService";
    public static final int NOTIFICATION_ID = 1;

    private static ArrayList<MessageObject> messageObjects = new ArrayList<MessageObject>();

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Message Received");
        Bundle extras = intent.getExtras();

        //GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        //String messageType = gcm.getMessageType(intent);

        //If user does want to receive notifications and is not logged, do not continue.
        if (!DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_USER) && !DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.CURRENTLY_LOGGED_IN))
            return;

        if (!extras.isEmpty()) {
            String id = extras.getString("id");
            String user = extras.getString("user");
            String subject = extras.getString("subject");
            String message = extras.getString("message");
            String date = extras.getString("date");
            String image = extras.getString("userImageURL");

            if (id != null && user != null) //As long as id and user fields are not null, send the notification
                sendNotification(id, user, subject, message, date, image);
        } else
            Log.i(TAG, "Incomplete/Empty message received");
    }

    //Put the received message into a notification
    private void sendNotification(String id, String user, String subject, String message, String date, String image) {
        MessageObject newMessage = new MessageObject(id, user, subject, message, date, "0"); //Create message object from the parameters
        messageObjects.add(newMessage);

        Intent showMessage = new Intent(this, ShowMessage.class);
        Intent notfBroadcast = new Intent(this, NotificationBroadcastReceiver.class);

        showMessage.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        showMessage.putExtra(ShowMessage.MESSAGE_OBJECT, newMessage);
        showMessage.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
        showMessage.setAction("ACTION_" + System.currentTimeMillis()); //Makes intents unique, so Android does not reuse invalid intents with null extras

        //Allows parent navigation after opening ShowMessage activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ShowMessage.class);
        stackBuilder.addNextIntent(showMessage); //Adds intent to the top of the stack

        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, notfBroadcast, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        try {
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(DataParser.loadBitmap(image))
                    .setContentTitle(getApplicationContext().getString(R.string.notification_from) + " " + user)
                    .setContentText(message)
                    .setContentInfo(messageObjects.size() + "")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set big view if more than one notification has been received
        if (messageObjects.size() > 1) {
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setSummaryText(DataParser.getSharedStringPreference(getApplicationContext(), DataParser.PREFS_USER, DataParser.USER_NAME));

            for (MessageObject messageObject : messageObjects) {
                String userText = messageObject.getUser();
                String contentText = messageObject.getContents();

                if (messageObjects.indexOf(messageObject) > 4) { //Inbox style only holds up to 5 lines of text
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
        boolean vibrate = DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_VIBRATE);
        boolean showLight = DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_LIGHT);

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

        builder.setContentIntent(contentIntent); //Fired when the notification is clicked
        builder.setDeleteIntent(deleteIntent); //Fired when notification is dismissed

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

//    private class GetImage extends AsyncTask<String, Void, Bitmap>{
//        @Override
//        protected Bitmap doInBackground(String... image) {
//            Bitmap largeIcon = null;
//            try {
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            return largeIcon;
//        }
//    }

    public static void resetNotfCounter(Context context) {
        NotificationManager notfManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notfManager.cancel(NOTIFICATION_ID);
        messageObjects.clear();
    }

}
