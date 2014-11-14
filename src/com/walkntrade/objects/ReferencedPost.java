package com.walkntrade.objects;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Does not hold all post information, so is not a complete Post object.
//Used in the ViewPost activity
public class ReferencedPost {
    private String school, link, category, title, date, views;
    private int expire;
    private boolean expired;

    public ReferencedPost(String school, String link, String category, String title, String date, String views, int expire, boolean expired) {
        this.school = school;
        this.link = link;
        this.category = category;
        this.title = title;
        this.date = date;
        this.views = views;
        this.expire = expire;
        this.expired = expired;
    }

    public String getSchool() {
        return school;
    }

    public String getLink(){
        return link;
    }

    public String getCategory(){
        return category;
    }

    public String getTitle(){
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getViews() {
        return views;
    }

    public int getExpire() {
        return expire;
    }

    public boolean isExpired() {
        return expired;
    }
}
