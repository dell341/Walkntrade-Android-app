package com.walkntrade.adapters.item;

import android.view.View;

import com.walkntrade.objects.ReferencedPost;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Represents items in the listview from the view posts option in user settings
public class ViewPostItem {

    private String contents, obsId, schoolAbbv;
    private int expire;
    private boolean expired;
    private boolean isHeader;
    private View itemView;

    //Header item (School)
    public ViewPostItem(String school, String schoolAbbv) {
        contents = school;
        this.schoolAbbv = schoolAbbv;
        isHeader = true;
    }

    //Post Item
    public ViewPostItem(ReferencedPost post){
        contents = post.getTitle();
        obsId = post.getLink();
        schoolAbbv = post.getSchoolAbbv();
        expire = post.getExpire();
        expired = post.isExpired();
        isHeader = false;
    }

    //View in adapter that holds this object's infomation
    public void setItemView(View itemView) {
        this.itemView = itemView;
    }

    public View getItemView() {
        return itemView;
    }

    public String getContents() {
        return contents;
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
