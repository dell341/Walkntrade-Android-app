package com.walkntrade;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class GcmIntentService extends IntentService {

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

        if(!extras.isEmpty()) {
            String id = extras.getString("id");
            String user = extras.getString("user");
            String message = extras.getString("message");

            sendNotification(id, user, message);
        }
    }

    //Put the received message into a notification
    private void sendNotification(String id, String user, String message) {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        Intent showMessage = new Intent(this, ShowMessage.class);

        //Allows parent navigation after clicking opening ShowMessage activity
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(ShowMessage.class);
        stackBuilder.addNextIntent(showMessage); //Adds intent to the top of the stack

        showMessage.putExtra(ShowMessage.MESSAGE_ID, id);
        showMessage.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
        PendingIntent contentIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("You received a message from: "+user)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_SOUND);

        builder.setContentText(message).setNumber(5);

        builder.setContentIntent(contentIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}
