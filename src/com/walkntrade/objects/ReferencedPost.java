package com.walkntrade.objects;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Does not hold all post information, so is not a complete Post object.
//Used in the ViewPost activity
public class ReferencedPost {
    private String school, schoolAbbv, obsId, category, title, date, views;
    private int expire;
    private boolean expired;

    public ReferencedPost(String school, String schoolAbbv, String obsId, String category, String title, String date, String views, int expire, boolean expired) {
        this.school = school;
        this.schoolAbbv = schoolAbbv;
        this.obsId = obsId;
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

    public String getSchoolAbbv() {
        return schoolAbbv;
    }

    public String getObsId(){
        return obsId;
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
