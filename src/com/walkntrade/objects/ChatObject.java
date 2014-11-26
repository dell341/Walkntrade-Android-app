package com.walkntrade.objects;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.graphics.Bitmap;

public class ChatObject {

    private static final String TAG = "ChatObject";
    private String postIdentifier, userImageUrl, user, contents, dateTime;
    private int messageId;
    private boolean read;

    private Bitmap userImage;
    private boolean hasImage = false;

    public ChatObject(String postIdentifier, String userImageUrl, int messageId, String user, String contents, String dateTime, boolean read) {
        this.postIdentifier = postIdentifier;
        this.userImageUrl = userImageUrl;
        this.messageId = messageId;
        this.user = user;
        this.contents = contents;
        this.dateTime = dateTime;
        this.read = read;
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

    public String getPostIdentifier() {
        return postIdentifier;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public String getUser() {
        return user;
    }

    public String getContents() {
        return contents;
    }

    public String getDateTime() {
        return dateTime;
    }

    public int getMessageId() {
        return messageId;
    }

    public boolean isRead() {
        return read;
    }
}
