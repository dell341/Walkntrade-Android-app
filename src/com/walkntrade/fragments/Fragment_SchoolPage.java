package com.walkntrade.fragments;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.SchoolPage;
import com.walkntrade.ShowPage;
import com.walkntrade.adapters.PostAdapter;
import com.walkntrade.asynctasks.SchoolPostsTask;
import com.walkntrade.asynctasks.ThumbnailTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.posts.Post;

import java.util.ArrayList;

public class Fragment_SchoolPage extends Fragment implements OnItemClickListener, AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

    private String TAG = "FRAGMENT:School_Page";
    public static final String ARG_CATEGORY = "Fragment Category";
    private String category;

    private SwipeRefreshLayout refreshLayout;
    private GridView gridView;
    private PostAdapter postsAdapter;
    private ProgressBar bigProgressBar, progressBar;
    private TextView noResults;
    private ArrayList<Post> schoolPosts = new ArrayList<Post>();
    private int offset = 0;
    private boolean downloadMore = true;

    @Override //This method may be called several times
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_school_page, container, false);

        bigProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshLayout);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBarGrid);
        noResults = (TextView) rootView.findViewById(R.id.noResults);
        gridView = (GridView) rootView.findViewById(R.id.gridView);
        Bundle args = getArguments();

        category = args.getString(ARG_CATEGORY);

        refreshLayout.setColorScheme(R.color.walkntrade_green, R.color.blue, R.color.walkntrade_green, R.color.blue);
        refreshLayout.setOnRefreshListener(this);

        bigProgressBar.setVisibility(View.GONE);
        /*On initial create, ArrayList will be empty. But onCreateView may be called several times
        Only call this method if the ArrayList is empty, which should only be during the initial creation*/
        if (schoolPosts.isEmpty()) {
            bigProgressBar.setVisibility(View.VISIBLE);
            downloadMorePosts(bigProgressBar);
        }

        postsAdapter = new PostAdapter(this.getActivity(), schoolPosts);
        gridView.setAdapter(postsAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(this);
        gridView.setVisibility(View.VISIBLE);

        return rootView;
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //If bottom of GridView is visible and Adapter has remaining posts not yet visible
        if (firstVisibleItem + visibleItemCount >= totalItemCount && postsAdapter.hasMorePosts()) {
            //If there more posts not yet downloaded and the GridView is not empty (to ensure it doesn't try to download several times)
            if (downloadMore && visibleItemCount != 0)
                downloadMorePosts(progressBar);

            postsAdapter.loadMore();
            postsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Post selectedPost = (Post) parent.getItemAtPosition(position);

        Intent showPage = new Intent(getActivity(), ShowPage.class);
        showPage.putExtra(SchoolPage.SELECTED_POST, selectedPost.getIdentifier());
        startActivity(showPage);
    }

    @Override
    public void onRefresh() {
        offset = 0;
        schoolPosts.clear();

        bigProgressBar.setVisibility(View.VISIBLE);
        downloadMorePosts(bigProgressBar);
        postsAdapter = new PostAdapter(this.getActivity(), schoolPosts);

        refreshLayout.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(false);
                gridView.setAdapter(postsAdapter);
            }
        }, 3000);

    }

    //Download more posts from the server
    private void downloadMorePosts(ProgressBar progressBar) {
        new SchoolPostsTask(getActivity(), this, progressBar, "", category, offset, 15).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataParser.getSharedStringPreference(getActivity(), DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG));
        offset += 15;
    }

    //Set to false if the server returned an empty list
    public void shouldDownLoadMore(boolean _downloadMore) {
        downloadMore = _downloadMore;

        if (schoolPosts.isEmpty())
            noResults.setVisibility(View.VISIBLE);
        else
            noResults.setVisibility(View.GONE);
    }

    //Add new data from the serve to the ArrayList and update the adapter
    public void updateData(ArrayList<Post> newData) {
        postsAdapter.incrementCount(newData);
        for (Post i : newData)
            schoolPosts.add(i);

        postsAdapter.notifyDataSetChanged();

        for (Post post : newData)
            new ThumbnailTask(getActivity(), postsAdapter, post).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, post.getImgUrl());
    }
}