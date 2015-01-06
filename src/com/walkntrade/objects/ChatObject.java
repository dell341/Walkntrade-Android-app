package com.walkntrade.objects;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.graphics.Bitmap;

public class ChatObject {

    private static final String TAG = "ChatObject";
    private String senderName, contents, dateTime, userImageUrl;
    private int senderId;
    private boolean sentFromMe, messageSeen;

    private Bitmap userImage;
    private boolean hasImage = false;

    public ChatObject(boolean sentFromMe, String senderName, String contents, String dateTime, boolean messageSeen, String userImageUrl) {
        this.sentFromMe = sentFromMe;
        this.senderName = senderName;
        this.contents = contents;
        this.dateTime = dateTime;
        this.messageSeen = messageSeen;
        this.userImageUrl = userImageUrl;
    }

    public void setCurrentUserImage(Bitmap image) {
        userImage = image;
        hasImage = true;
    }

    public Bitmap getCurrentUserImage() {
        return userImage;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContents() {
        return contents;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public boolean isSentFromMe() {
        return sentFromMe;
    }

    public boolean isMessageSeen() {
        return messageSeen;
    }

    public Bitmap getUserImage() {
        return userImage;
    }

    public boolean isHasImage() {
        return hasImage;
    }
}
