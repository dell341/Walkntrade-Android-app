package com.walkntrade.objects;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import java.util.ArrayList;

public class UserProfileObject {

    private String userName, userImageUrl;
    private ArrayList<ReferencedPost> userPosts;

    public UserProfileObject(String userName, String userImageUrl, ArrayList<ReferencedPost> userPosts) {
        this.userName = userName;
        this.userImageUrl = userImageUrl;
        this.userPosts = userPosts;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public ArrayList<ReferencedPost> getUserPosts() {
        return userPosts;
    }

    public int getAmountOfPosts() {
        return userPosts.size();
    }

}
