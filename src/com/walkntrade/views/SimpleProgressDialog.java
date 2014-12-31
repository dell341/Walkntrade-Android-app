package com.walkntrade.views;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class SimpleProgressDialog extends ProgressDialog {

    private static final String SAVED_INSTANCE_SHOWING = "saved_instance_showing";
    private static final String SAVED_INSTANCE_PROGRESS = "saved_instance_current_progress";
    private static final String SAVED_INSTANCE_MESSAGE = "saved_instance_message";

    private String message;

    public SimpleProgressDialog(Context context) {
        super(context);
    }

    public SimpleProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    @Override
    public void setMessage(CharSequence message) {
        super.setMessage(message);
        this.message = message.toString();
    }

    @Override //Save progress dialog values, in case of events like orientation change
    public Bundle onSaveInstanceState() {
        Bundle bundle = super.onSaveInstanceState();

        bundle.putBoolean(SAVED_INSTANCE_SHOWING, isShowing());
        bundle.putInt(SAVED_INSTANCE_PROGRESS, getProgress());
        bundle.putString(SAVED_INSTANCE_MESSAGE, message);

        return bundle;
    }

    @Override //Restore previous values
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        boolean wasShowing = savedInstanceState.getBoolean(SAVED_INSTANCE_SHOWING);
        int progress = savedInstanceState.getInt(SAVED_INSTANCE_PROGRESS);
        String m = savedInstanceState.getString(SAVED_INSTANCE_MESSAGE);

        setProgress(progress);
        setMessage(m);
        if(wasShowing)
            show();
    }
}
