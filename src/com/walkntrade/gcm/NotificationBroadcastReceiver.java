package com.walkntrade.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Called only to reset the counter when a Notification is dismissed by the user.
//Could be a more efficient way to do this, but it works.
public class NotificationBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "NotfBroadcastReceiver";

    public NotificationBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "NotificationBroadcast onReceive");
        GcmIntentService.resetNotfCounter(context);
    }
}
