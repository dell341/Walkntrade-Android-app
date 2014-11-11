package com.walkntrade.posts;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Posts with an unknown or special category
public class Post_Wildcard extends Post {

    public Post_Wildcard(String obsId, String identifier, String title, String details, String user, String imgURL, String date, String price, String views) {
        super(obsId, identifier, title, details, user, imgURL,date, price, views);
    }

    @Override
    public String getCategory() {
        return Post.CATEGORY_UNKNOWN;
    }

    /*Parcelable implementation*/

    private Post_Wildcard(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        public Post_Wildcard createFromParcel(Parcel in) {
            return new Post_Wildcard(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };
}
