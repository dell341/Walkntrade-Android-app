package com.walkntrade.posts;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Does not hold all post information, so is not a complete Post object.
//Used in the ViewPost Fragment
public class PostReference {
    private String school, link, category, title, date, views;

    public PostReference(String _school, String _link, String _category, String _title, String _date, String _views) {
        school = _school;
        link = _link;
        category = _category;
        title = _title;
        date = _date;
        views = _views;
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
}
