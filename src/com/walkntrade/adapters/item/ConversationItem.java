package com.walkntrade.adapters.item;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

import com.walkntrade.io.FormatDateTime;

public class ConversationItem implements Parcelable{
    private String senderName, contents, dateTime, imageUrl, errorMessage;
    private boolean sentFromMe, sentFromThisDevice, isDelivered, hasAvatar, messageFailed;
    private static Bitmap myAvatar, otherUserAvatar; //Hold a single reference to each Bitmap. The images will be the same, no need for different objects.

    public ConversationItem(String senderName, String contents, String dateTime, String imageUrl, boolean sentFromMe, boolean sentFromThisDevice) {
        this.senderName = senderName;
        this.contents = contents;
        this.dateTime = dateTime;
        this.sentFromMe = sentFromMe;
        this.sentFromThisDevice = sentFromThisDevice;
        this.imageUrl = imageUrl;
        isDelivered = false;
        hasAvatar = false;
        messageFailed = false;
        errorMessage = "";
    }

    public ConversationItem(Parcel in) {
        senderName = in.readString();
        contents = in.readString();
        dateTime = in.readString();
        errorMessage = in.readString();
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
        parcel.writeString(dateTime);
        parcel.writeString(errorMessage);
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

    public String getDateTime() {
        return dateTime;
    }

    public String getDisplayableDateTime() {
        return FormatDateTime.formatDateTime(dateTime);
    }

    public String getImageUrl() {
        return imageUrl;
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

    public void messageFailedToDeliver(String errorMessage) {
        messageFailed = true;
        this.errorMessage = errorMessage;
    }

    public boolean hasMessageFailed() {
        return messageFailed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setAvatar(Bitmap avatar) {

        if(sentFromMe)
            myAvatar = avatar;
        else
            otherUserAvatar = avatar;

        hasAvatar = true;
    }

    public boolean hasAvatar() {
        return hasAvatar;
    }

    public Bitmap getAvatar() {
        return (sentFromMe ? myAvatar : otherUserAvatar);
    }
}
