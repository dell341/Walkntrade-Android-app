package com.walkntrade.objects;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MessageThread implements Parcelable{

    private String uniqueThreadId, postIdentifier, postTitle, userImageUrl, lastUser, lastContent, lastDateTime;
    private int lastMessageId;
    private boolean lastMessageRead;

    private boolean hasImage = false;
    private Bitmap userImage;

    public MessageThread(String uniqueThreadId, String postIdentifier, String postTitle, String userImageUrl, int lastMessageId, String lastUser, String lastContent, String lastDateTime, boolean lastMessageRead) {
        this.uniqueThreadId = uniqueThreadId;
        this.postIdentifier = postIdentifier;
        this.postTitle = postTitle;
        this.userImageUrl = userImageUrl;
        this.lastMessageId = lastMessageId;
        this.lastUser = lastUser;
        this.lastContent = lastUser+" : "+lastContent;
        this.lastDateTime = lastDateTime;
        this.lastMessageRead = lastMessageRead;

        //TODO: Separate date from time
    }

    //Constructor when object is created from a parcel
    public MessageThread(Parcel in){
        uniqueThreadId = in.readString();
        postIdentifier = in.readString();
        postTitle = in.readString();
        userImageUrl = in.readString();
        lastMessageId = in.readInt();
        lastUser = in.readString();
        lastContent = in.readString();
        lastDateTime = in.readString();
        lastMessageRead = false; ////////////////////////////////TODO: Change to read from parcel. //////////////////////////////////////////////////////////////
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //Must be written out in the same order it is read in
        out.writeString(uniqueThreadId);
        out.writeString(postIdentifier);
        out.writeString(postTitle);
        out.writeString(userImageUrl);
        out.writeInt(lastMessageId);
        out.writeString(lastUser);
        out.writeString(lastContent);
        out.writeString(lastDateTime);
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

    public String getUniqueThreadId() {
        return uniqueThreadId;
    }

    public String getPostIdentifier() {
        return postIdentifier;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public String getLastUser() {
        return lastUser;
    }

    public String getLastContent() {
        return lastContent;
    }

    public String getLastDateTime() {
        return lastDateTime;
    }

    public int getLastMessageId() {
        return lastMessageId;
    }

    public boolean isLastMessageRead() {
        return lastMessageRead;
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
