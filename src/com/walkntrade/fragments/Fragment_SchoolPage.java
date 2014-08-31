package com.walkntrade.fragments;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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

    private static final String SAVED_ARRAYLIST = "saved_instance_array_list";
    private static final String SAVED_CATEGORY = "saved_instance_category";

    private SwipeRefreshLayout refreshLayout;
    private PostAdapter postsAdapter;
    private ProgressBar bigProgressBar, progressBar;
    private GridView gridView;
    private TextView noResults;
    private String searchQuery = ""; //Search query stays the same throughout all fragments
    private ArrayList<Post> schoolPosts = new ArrayList<Post>();
    private int offset = 0;
    private boolean downloadMore = true;
    private boolean openHasFired = false;

    @Override //This method may be called several times
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_school_page, container, false);
        setHasOptionsMenu(true); //Options in Actionbar (Search)
        setRetainInstance(true); //Prevents fragment from being destroyed during activity change. (Especially if AsyncTask is currently running)

        bigProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refreshLayout);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBarGrid);
        noResults = (TextView) rootView.findViewById(R.id.noResults);
        gridView = (GridView) rootView.findViewById(R.id.gridView);
        Bundle args = getArguments();

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);

        bigProgressBar.setVisibility(View.GONE);

        //Recalls data from onSaveState. Prevents network calls for a simple orientation change
        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
            if(savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST) != null) {
                schoolPosts = savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST);
                category = savedInstanceState.getString(SAVED_CATEGORY);
                offset += schoolPosts.size();
            }
        }
        else
            category = args.getString(ARG_CATEGORY);

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

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHasFired = true;
                if(!searchQuery.isEmpty())
                    searchView.setQuery(searchQuery, false);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            int queryLength = 0;

            @Override
            public boolean onQueryTextSubmit(String query) {
                openHasFired = false;
                searchQuery = query;
                getActivity().getActionBar().setTitle(searchQuery);
                menuItem.collapseActionView();

                gridView.setAdapter(null);
                postsAdapter.clearContents();
                offset = 0;
                downloadMorePosts(bigProgressBar);

                gridView.setAdapter(postsAdapter);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if(queryLength > newText.length() && newText.length() == 0 && openHasFired) {
                    searchQuery = "";
                    getActivity().getActionBar().setTitle(DataParser.getSharedStringPreference(getActivity(), DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG));

                    gridView.setAdapter(null);
                    postsAdapter.clearContents();
                    offset = 0;
                    downloadMorePosts(bigProgressBar);

                    gridView.setAdapter(postsAdapter);
                }

                queryLength = newText.length();
                return true;
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

        postsAdapter.clearContents();

        bigProgressBar.setVisibility(View.VISIBLE);
        downloadMorePosts(bigProgressBar);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_ARRAYLIST, schoolPosts);
        outState.putString(SAVED_CATEGORY, category);
    }

    //Download more posts from the server
    private void downloadMorePosts(ProgressBar progressBar) {
        noResults.setVisibility(View.GONE);
        new SchoolPostsTask(getActivity(), this, progressBar, searchQuery, category, offset, 15).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataParser.getSharedStringPreference(getActivity(), DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG));
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
        refreshLayout.setEnabled(true);
        refreshLayout.setRefreshing(false);

        postsAdapter.incrementCount(newData);
        for (Post i : newData)
            schoolPosts.add(i);

        postsAdapter.notifyDataSetChanged();

        for (Post post : newData)
            new ThumbnailTask(getActivity(), postsAdapter, post).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, post.getImgUrl());
    }

}