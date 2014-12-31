package com.walkntrade.objects;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Posts with an unknown or special category
public class WildcardPost extends Post {

    public WildcardPost(String obsId, String schoolId, String identifier, String title, String details, String user, String imgURL, String date, String price, String views) {
        super(obsId, schoolId, identifier, title, details, user, imgURL,date, price, views);
    }

    @Override
    public String getCategory() {
        return Post.CATEGORY_UNKNOWN;
    }

    /*Parcelable implementation*/

    private WildcardPost(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        public WildcardPost createFromParcel(Parcel in) {
            return new WildcardPost(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };
}
