package com.walkntrade.adapters.item;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class ConversationItem implements Parcelable{
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

    public ConversationItem(Parcel in) {
        senderName = in.readString();
        contents = in.readString();
        date = in.readString();
        time = in.readString();

        boolean[] values = new boolean[4];
        in.readBooleanArray(values);
        sentFromMe = values[0];
        sentFromThisDevice = values[1];
        isDelivered = values[2];
        hasAvatar = values[3];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(senderName);
        parcel.writeString(contents);
        parcel.writeString(date);
        parcel.writeString(time);
        boolean[] values = {sentFromMe, sentFromThisDevice, isDelivered, hasAvatar};
        parcel.writeBooleanArray(values);
    }

    public static final Parcelable.Creator<ConversationItem> CREATOR = new Parcelable.Creator<ConversationItem>() {
        @Override
        public ConversationItem createFromParcel(Parcel parcel) {
            return new ConversationItem(parcel);
        }

        @Override
        public ConversationItem[] newArray(int size) {
            return new ConversationItem[size];
        }
    };

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
