package com.walkntrade.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.ViewPostItem;
import com.walkntrade.adapters.ViewPostAdapter;
import com.walkntrade.io.DataParser;
import com.walkntrade.posts.PostReference;

import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class UserPostsTask extends AsyncTask<Void, Void, ArrayList<PostReference>> {

    private static final String TAG = "AsyncTask:UserPosts";

    private Context context;
    private ProgressBar progressBar;
    private TextView noResults;
    private ListView listOfPosts;

    public UserPostsTask(Context _context, ProgressBar _pBar, TextView _noResults, ListView _listOfPosts) {
        context = _context;
        progressBar = _pBar;
        noResults = _noResults;
        listOfPosts = _listOfPosts;
    }

    @Override
    protected void onPreExecute() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    protected ArrayList<PostReference> doInBackground(Void... voids) {
        ArrayList<PostReference> userPosts = new ArrayList<PostReference>();
        DataParser database = new DataParser(context);

        try {
            userPosts = database.getUserPosts();
        }
        catch(Exception e) {
            Log.e(TAG, "Get user posts", e);
        }
        return userPosts;
    }

    @Override
    protected void onPostExecute(ArrayList<PostReference> falsePosts) {
        progressBar.setVisibility(View.GONE);
        listOfPosts.setAdapter(null); //Clears out any previous items

        if (falsePosts.isEmpty())
            noResults.setVisibility(View.VISIBLE);
        else {
            noResults.setVisibility(View.GONE);
            ArrayList<ViewPostItem> items = new ArrayList<ViewPostItem>();

            String currentSchool = "";

            for (PostReference p : falsePosts) {
                if(!p.getSchool().equalsIgnoreCase(currentSchool)) { //If this post is a new school, create a new header
                    currentSchool = p.getSchool();
                    items.add(new ViewPostItem(p.getSchool()));
                }

                items.add(new ViewPostItem(p)); //Then continue adding posts
            }

            ViewPostAdapter adapter = new ViewPostAdapter(context, items);
            listOfPosts.setAdapter(adapter);
        }
    }
}
