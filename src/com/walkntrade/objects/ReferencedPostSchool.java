package com.walkntrade.objects;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import java.util.ArrayList;

//Holds the number of referenced posts that belong to a school
//Helps arranging user's posts a lot easier
public class ReferencedPostSchool {

    private static final String TAG = "ReferencedPostSchool";

    private String schoolShortName, schoolLongName;
    private ArrayList<ReferencedPost> referencedPosts;

    public ReferencedPostSchool(String schoolShortName, String schoolLongName) {
        this.schoolShortName = schoolShortName;
        this.schoolLongName = schoolLongName;
        referencedPosts = new ArrayList<ReferencedPost>();
    }

    public String getSchoolShortName() {
        return schoolShortName;
    }

    public String getSchoolLongName() {
        return schoolLongName;
    }

    public int getAmountOfPosts() {
        return referencedPosts.size();
    }

    public boolean addReferencedPost(ReferencedPost post) {
        return referencedPosts.add(post);
    }

    public ReferencedPost getReferencedPost(int index) {
        return referencedPosts.get(index);
    }

    public boolean removeReferencedPost(ReferencedPost post) {
        return referencedPosts.remove(post);
    }
}
