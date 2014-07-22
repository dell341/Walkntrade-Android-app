package com.walkntrade.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.walkntrade.Messages;
import com.walkntrade.R;
import com.walkntrade.ShowMessage;
import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class GcmIntentService extends IntentService {

    private static final String TAG = "GcmIntentService";
    public static final int NOTIFICATION_ID = 1;
    private static int numMessages = 0;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

        String messageType = gcm.getMessageType(intent);

        if(!DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_USER)) //If user does not want to receive notifications
            return;

        if(!extras.isEmpty()) {
            String id = extras.getString("id");
            String user = extras.getString("user");
            String message = extras.getString("message");
            String image = extras.getString("userImageURL");

            if(id != null) //As long as id is not null, send the notification
            sendNotification(id, user, message, image);
        }
        else
            Log.i(TAG, "Empty message received");
    }

    //Put the received message into a notification
    private void sendNotification(String id, String user, String message, String image) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        AsyncTask<String, Void, Bitmap> task = new GetImage();
        task.execute(image);

        Intent showMessage = new Intent(this, ShowMessage.class);

        //Allows parent navigation after clicking opening ShowMessage activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ShowMessage.class);
        stackBuilder.addNextIntent(showMessage); //Adds intent to the top of the stack

        showMessage.putExtra(ShowMessage.MESSAGE_ID, id);
        showMessage.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        try {
            builder.setSmallIcon(R.drawable.ic_launcher)
                    .setLargeIcon(task.get())
                    .setContentTitle("Message from: " + user)
                    .setContentText(message)
                    //.setContentInfo(++numMessages+"") TODO: Find a way to increment notifications properly
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        boolean hasSound = false;
        boolean vibrate = DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_VIBRATE);
        boolean showLight = DataParser.getSharedBooleanPreference(this, DataParser.PREFS_NOTIFICATIONS, DataParser.NOTIFY_LIGHT);

        String sound = DataParser.getSoundPref(this);
        if(sound != null) {
            hasSound = true;
            builder.setSound(Uri.parse(DataParser.getSoundPref(this)));
        }

        if(!hasSound) {
            if(vibrate && showLight) {
                builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
                builder.setLights(0xffffff, 500, 500);
            }
            else if (vibrate)
                builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
            else if (showLight) {
                builder.setDefaults(Notification.DEFAULT_SOUND);
                builder.setLights(0xff00ff, 500, 500);
            }
        }
        else if(vibrate && showLight) {
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
            builder.setLights(0xffffff, 500, 500);
        }
        else if (vibrate)
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        else if (showLight)
            builder.setLights(0xff00ff, 500, 500);

        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private class GetImage extends AsyncTask<String, Void, Bitmap>{
        @Override
        protected Bitmap doInBackground(String... image) {
            Bitmap largeIcon = null;
            try {
                largeIcon =  DataParser.loadBitmap(image[0]);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return largeIcon;
        }
    }

}
