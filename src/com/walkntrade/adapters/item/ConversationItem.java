package com.walkntrade.adapters.item;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.graphics.Bitmap;

public class ConversationItem {
    private String senderName, contents, date, time;
    private boolean sentFromMe, sentFromThisDevice, isDelivered, hasAvatar;
    private Bitmap avatar;

    public ConversationItem(String senderName, String contents, String date, String time, boolean sentFromMe, boolean sentFromThisDevice) {
        this.senderName = senderName;
        this.contents = contents;
        this.date = date;
        this.time = time;
        this.sentFromMe = sentFromMe;
        this.sentFromThisDevice = sentFromThisDevice;
        isDelivered = false;
        hasAvatar = false;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContents() {
        return contents;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public boolean isSentFromMe() {
        return sentFromMe;
    }

    public boolean isSentFromThisDevice() {
        return sentFromThisDevice;
    }

    public void messageDelivered() {
        isDelivered = true;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
        this.hasAvatar = true;
    }

    public boolean hasAvatar() {
        return hasAvatar;
    }

    public Bitmap getAvatar() {
        return avatar;
    }
}
