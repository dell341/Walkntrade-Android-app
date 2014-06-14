package com.walkntrade.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.walkntrade.fragments.Fragment_SchoolPage;
import com.walkntrade.io.DataParser;
import com.walkntrade.posts.Post;

import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Asynchronous Task, looks for posts from the specified school
public class SchoolPostsTask extends AsyncTask<String, Void, ArrayList<Post>> {

    private final String TAG = "ASYNCTASK:SchoolPosts";

    private final Context context;
    private Fragment_SchoolPage fragment;
    private ProgressBar progressBar;
    private String query, category;
    private int offset, amount;

//    private DiskLruImageCache imageCache;

    public SchoolPostsTask(Context _context, Fragment_SchoolPage _fragment, ProgressBar _progressBar, String _query, String _category, int _offset, int _amount) {
        context = _context;
        fragment = _fragment;
        progressBar = _progressBar;
        query = _query;
        category = _category;
        offset = _offset;
        amount = _amount;
    }

    @Override
    protected void onPreExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected ArrayList<Post> doInBackground(String... schoolName) {
        String schoolID;
        ArrayList<Post> schoolPosts = new ArrayList<Post>();
        DataParser database = new DataParser(context);

        try {
            schoolID = database.getSchoolId(schoolName[0]);

            //Set School Preference
            database.setSchoolPref(schoolID);

            Log.v(TAG, "Searching for Category: "+category);
            schoolPosts = database.getSchoolPosts(schoolID, query, category, offset, amount);

//            imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.IMAGE_DIRECTORY);

//            for(Post i: schoolPosts) {
//                String key = i.getIdentifier()+"_0";//Caches "_0" as the thumbnail image
//
//                Bitmap bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache
//
//                if(bm == null) //If it doesn't exists, retrieve image from network
//                    bm = DataParser.loadBitmap(i.getImgUrl());
//
//                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
//                i.setBitmapImage(bm);
//            }

        }catch(Exception e) {
            e.printStackTrace();
        }
//        finally {
//            imageCache.close();
//        }

        return schoolPosts;
    }

    @Override
    protected void onPostExecute(ArrayList<Post> posts) {
        super.onPostExecute(posts);
        progressBar.setVisibility(View.GONE);

        if(posts.isEmpty()) //If server returned empty list, don't try to download anymore from this category
            fragment.shouldDownLoadMore(false);
        else
            fragment.updateData(posts);

        Log.v(TAG, category+" posts updated");
    }
}
