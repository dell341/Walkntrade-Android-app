package com.walkntrade.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.SchoolPage;
import com.walkntrade.SearchActivity;
import com.walkntrade.ShowPage;
import com.walkntrade.adapters.PostAdapter;
import com.walkntrade.asynctasks.ThumbnailTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.Post;

import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class SchoolPostsFragment extends Fragment implements OnItemClickListener, AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

    private String TAG = "FRAGMENT:School_Page";
    public static final String ARG_CATEGORY = "Fragment Category";
    public static final String INDEX = "index";
    private String category;

    private static final String SAVED_ARRAYLIST = "saved_instance_array_list";
    private static final String SAVED_CATEGORY = "saved_instance_category";
    private static final String SAVED_INDEX = "saved_instance_index";
    private static final int AMOUNT_OF_POSTS = 15; //Amount of posts to load

    private SwipeRefreshLayout refreshLayout;
    private PostAdapter postsAdapter;
    private ProgressBar bigProgressBar, progressBar;
    private SchoolPostsTask schoolPostsTask;
    private Context context;
    private TextView noResults;
    private String searchQuery = ""; //Search query stays the same throughout all fragments
    private ArrayList<Post> schoolPosts = new ArrayList<Post>();
    private int index;
    private int offset = 0;
    private boolean downloadMore = true;
    private boolean shouldClearContents = false; //Clear current list when manually refreshing

    @Override //This method may be called several times
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_school_page, container, false);
        setHasOptionsMenu(true); //Options in Actionbar (Search)
        setRetainInstance(true); //Prevents fragment from being destroyed during activity change. (Especially if AsyncTask is currently running)

        bigProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBarGrid);
        noResults = (TextView) rootView.findViewById(R.id.noResults);
        GridView gridView = (GridView) rootView.findViewById(R.id.gridView);
        Bundle args = getArguments();
        context = getActivity().getApplicationContext();

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);

        bigProgressBar.setVisibility(View.GONE);

        //Recalls data from onSaveState. Prevents network calls for a simple orientation change
        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
            if (savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST) != null) {
                schoolPosts = savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST);
                category = savedInstanceState.getString(SAVED_CATEGORY);
                index = savedInstanceState.getInt(SAVED_INDEX);
                offset += schoolPosts.size();
            }
        } else {
            category = args.getString(ARG_CATEGORY);
            index = args.getInt(INDEX);
        }

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_school_page, menu);

        final MenuItem menuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setQueryHint(getString(R.string.post_search));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent search = new Intent(SchoolPostsFragment.this.getActivity(), SearchActivity.class);
                search.putExtra(SearchActivity.EXTRA_CATEGORY, category);
                search.putExtra(SearchActivity.EXTRA_QUERY, query);
                search.putExtra(SearchActivity.EXTRA_INDEX, index);
                startActivity(search);

                menuItem.collapseActionView();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        //If bottom of GridView is visible and Adapter has remaining posts not yet visible
        if (firstVisibleItem + visibleItemCount >= totalItemCount && postsAdapter.hasMorePosts() && !refreshLayout.isRefreshing()) {
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
        showPage.putExtra(SchoolPage.SELECTED_POST, selectedPost);
        startActivity(showPage);
    }

    @Override
    public void onRefresh() {
        refreshLayout.setEnabled(false);
        offset = 0;

        shouldClearContents = true;

        bigProgressBar.setVisibility(View.VISIBLE);
        downloadMorePosts(bigProgressBar);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_ARRAYLIST, schoolPosts);
        outState.putString(SAVED_CATEGORY, category);
        outState.putInt(SAVED_INDEX, index);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    //Download more posts from the server
    private void downloadMorePosts(ProgressBar progress) {
        noResults.setVisibility(View.GONE);
        schoolPostsTask = new SchoolPostsTask(progress);
        schoolPostsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataParser.getSharedStringPreference(getActivity(), DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG));
    }

    //Set to false if the server returned an empty list
    public void shouldDownLoadMore(boolean downloadMore) {
        this.downloadMore = downloadMore;

        if (schoolPosts.isEmpty()) {
            noResults.setText(context.getString(R.string.no_results));
            noResults.setVisibility(View.VISIBLE);
        } else
            noResults.setVisibility(View.GONE);
    }

    //Asynchronous Task, looks for posts from the specified school
    private class SchoolPostsTask extends AsyncTask<String, Void, Integer> {

        private ProgressBar progress;
        private DataParser database;
        ArrayList<Post> newPosts;

        public SchoolPostsTask(ProgressBar progress) {
            super();
            this.progress = progress;
            newPosts = new ArrayList<Post>();
            database = new DataParser(context);
        }

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... schoolName) {
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            if (isCancelled())
                return null;

            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                DataParser.ObjectResult<ArrayList<Post>> result = database.getSchoolPosts(schoolID, searchQuery, category, offset, AMOUNT_OF_POSTS);
                serverResponse = result.getStatus();
                newPosts = result.getValue();
            } catch (Exception e) {
                Log.e(TAG, "Retrieving school post(s)", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            super.onPostExecute(serverResponse);

            offset += 15;
            progress.setVisibility(View.GONE);

            if (serverResponse == StatusCodeParser.STATUS_OK) {
                if (newPosts.isEmpty()) //If server returned empty list, don't try to download anymore from this category
                    shouldDownLoadMore(false);
                else { //Add new data from the serve to the ArrayList and update the adapter
                    if (shouldClearContents) { //When doing a manual refresh, contents are overwritten not appended
                        postsAdapter.clearContents();
                        postsAdapter.notifyDataSetChanged();
                        shouldClearContents = false;
                    }
                    postsAdapter.incrementCount(newPosts);
                    for (Post i : newPosts)
                        schoolPosts.add(i);

                    postsAdapter.notifyDataSetChanged();
                    for (Post post : newPosts)
                        new ThumbnailTask(getActivity(), postsAdapter, post).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, post.getImgUrl());
                }
            } else {
                postsAdapter.clearContents();
                postsAdapter.notifyDataSetChanged();
                noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
                noResults.setVisibility(View.VISIBLE);
            }

            refreshLayout.setEnabled(true);
            refreshLayout.setRefreshing(false);
        }
    }


}