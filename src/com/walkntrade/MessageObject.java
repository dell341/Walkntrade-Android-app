package com.walkntrade;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class MessageObject implements Parcelable{

    private String id, user, subject, contents, date, read;

    public MessageObject(String id, String user, String subject, String contents, String date, String read) {
        this.id = id;
        this.user = user;
        this.subject = subject;
        this.contents = contents;
        this.date = date;
        this.read = read;

        Log.v("MessageObject", this.id);
        //TODO: Separate date from time
    }

    //Constructor when object is created from a parcel
    public MessageObject(Parcel in){
        id = in.readString();
        user = in.readString();
        subject = in.readString();
        contents = in.readString();
        date = in.readString();
        read = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        //Must be written out in the same order it is read in
        out.writeString(id);
        out.writeString(user);
        out.writeString(subject);
        out.writeString(contents);
        out.writeString(date);
        out.writeString(read);
    }

    public String getId(){
        Log.v("MessageObject", "ID: "+id);
        return id;
    }

    public String getUser(){
        return user;
    }

    public String getSubject() {
        return subject;
    }

    public String getContents() {
        return contents;
    }

    public String getDate() {
        return date;
    }

    public boolean isUnRead() {
        return read.equals("0"); //If this value is equal to 1, message has not been read
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<MessageObject> CREATOR = new Creator<MessageObject>() {
        @Override
        public MessageObject createFromParcel(Parcel source) {
            return new MessageObject(source);
        }

        @Override
        public MessageObject[] newArray(int size) {
            return new MessageObject[size];
        }
    };
}
