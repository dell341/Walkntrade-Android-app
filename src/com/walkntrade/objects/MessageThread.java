package com.walkntrade.objects;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MessageThread implements Parcelable {

    private String threadId, postIdentifier, postTitle, lastMessage, lastUserName, lastDateTime, userName, userImageUrl;
    private int lastUserId, userId, newMessages;

    private boolean hasImage = false;
    private Bitmap userImage;

    public MessageThread(String threadId, String postIdentifier, String postTitle, String lastMessage, String lastUserName, int lastUserId, String lastDateTime, int userId, String userName, String userImageUrl, int newMessages) {
        this.threadId = threadId;
        this.postIdentifier = postIdentifier;
        this.postTitle = postTitle;
        this.lastMessage = lastMessage;
        this.lastUserName = lastUserName;
        this.lastUserId = lastUserId;
        this.lastDateTime = lastDateTime;
        this.userId = userId;
        this.userName = userName;
        this.userImageUrl = userImageUrl;
        this.newMessages = newMessages;
    }

    //Constructor when object is created from a parcel
    public MessageThread(Parcel in) {
        threadId = in.readString();
        postIdentifier = in.readString();
        postTitle = in.readString();
        lastMessage = in.readString();
        lastUserName = in.readString();
        lastUserId = in.readInt();
        lastDateTime = in.readString();
        userId = in.readInt();
        userName = in.readString();
        userImageUrl = in.readString();
        newMessages = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //Must be written out in the same order it is read in
        out.writeString(threadId);
        out.writeString(postIdentifier);
        out.writeString(postTitle);
        out.writeString(lastMessage);
        out.writeString(lastUserName);
        out.writeInt(lastUserId);
        out.writeString(lastDateTime);
        out.writeInt(userId);
        out.writeString(userName);
        out.writeString(userImageUrl);
        out.writeInt(newMessages);
    }

    public boolean hasImage() {
        return hasImage;
    }

    public void setBitmap(Bitmap bm) {
        userImage = bm;
        hasImage = true;
    }

    public Bitmap getUserImage() {
        return userImage;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getPostIdentifier() {
        return postIdentifier;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getLastUserName() {
        return lastUserName;
    }

    public String getUserName() {
        return userName;
    }

    public int getLastUserId() {
        return lastUserId;
    }

    public int getUserId() {
        return userId;
    }

    public int getNewMessages() {
        return newMessages;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public String getLastDateTime() {
        return lastDateTime;
    }

    public void clearNewMessages() {
        newMessages = 0;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MessageThread> CREATOR = new Creator<MessageThread>() {
        @Override
        public MessageThread createFromParcel(Parcel source) {
            return new MessageThread(source);
        }

        @Override
        public MessageThread[] newArray(int size) {
            return new MessageThread[size];
        }
    };
}
