package com.walkntrade.adapters.item;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.walkntrade.objects.ReferencedPost;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Represents items in the listview from the view posts option in user settings
public class ViewPostItem implements Parcelable {

    private String contents, obsId, schoolAbbv, date;
    private int expire;
    private boolean expired;
    private boolean isHeader;
    private boolean isContent;
    private View itemView;

    //Header item (School)
    public ViewPostItem(String school, String schoolAbbv) {
        contents = school;
        this.schoolAbbv = schoolAbbv;
        isHeader = true;
        isContent = false;
    }

    //Post Item
    public ViewPostItem(ReferencedPost post) {
        contents = post.getTitle();
        obsId = post.getLink();
        schoolAbbv = post.getSchoolAbbv();
        date = post.getDate();
        expire = post.getExpire();
        expired = post.isExpired();
        isHeader = false;
        isContent = true;
    }

    protected ViewPostItem(Parcel in) {
        contents = in.readString();
        obsId = in.readString();
        schoolAbbv = in.readString();
        expire = in.readInt();
        date = in.readString();

        boolean array[] = new boolean[3];
        in.readBooleanArray(array);
        expired = array[0];
        isHeader = array[1];
        isContent = array[2];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(contents);
        parcel.writeString(obsId);
        parcel.writeString(schoolAbbv);
        parcel.writeInt(expire);
        parcel.writeString(date);
        boolean array[] = {expired, isHeader, isContent};
        parcel.writeBooleanArray(array);
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

    public String getObsId() {
        return obsId;
    }

    public String getSchoolAbbv() {
        return schoolAbbv;
    }

    public String getDate() {
        return date;
    }

    public int getExpire() {
        return expire;
    }

    public boolean isExpired() {
        return expired;
    }

    public boolean isHeader() {
        return isHeader;
    }

    public boolean isContent() {
        return isContent;
    }
}
