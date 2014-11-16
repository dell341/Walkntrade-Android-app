package com.walkntrade.adapters.item;

import com.walkntrade.objects.ReferencedPost;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Represents items in the listview from the view posts option in user settings
public class ViewPostItem {

    private String title, obsId, schoolAbbv;
    private int expire;
    private boolean expired;
    private boolean isHeader;

    //Header item (School)
    public ViewPostItem(String title) {
        this.title = title;
        isHeader = true;
    }

    //Post Item
    public ViewPostItem(ReferencedPost post){
        title = post.getTitle();
        obsId = post.getLink();
        schoolAbbv = post.getSchoolAbbv();
        expire = post.getExpire();
        expired = post.isExpired();
        isHeader = false;
    }

    public String getContents() {
        return title;
    }

    public String getObsId(){
        return obsId;
    }

    public String getSchoolAbbv() {
        return schoolAbbv;
    }

    public int getExpire() {
        return expire;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isHeader(){
        return isHeader;
    }
}
