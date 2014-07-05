package com.walkntrade;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class MessageObject {

    private String id, user, subject, contents, date, read;

    public MessageObject(String id, String user, String subject, String contents, String date, String read) {
        this.id = id;
        this.user = user;
        this.subject = subject;
        this.contents = contents;
        this.date = date;
        this.read = read;

        //TODO: Separate date from time
    }

    public String getId(){
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
}
