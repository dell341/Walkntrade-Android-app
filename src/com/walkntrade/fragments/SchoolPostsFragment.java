package com.walkntrade.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
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

import com.walkntrade.AddPost;
import com.walkntrade.R;
import com.walkntrade.SchoolPage;
import com.walkntrade.SearchActivity;
import com.walkntrade.ShowPage;
import com.walkntrade.adapters.PostAdapter;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.Post;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class SchoolPostsFragment extends Fragment implements OnItemClickListener, AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

    private String TAG = "SchoolPostsFragment";
    public static final String ARG_CATEGORY = "Fragment Category";
    public static final String INDEX = "index";
    public static final String ACTION_UPDATE_POSTS = "com.walkntrade.fragments.SchoolPostsFragment.update_posts";
    private String category;

    private static final String SAVED_ARRAYLIST = "saved_instance_array_list";
    private static final String SAVED_CATEGORY = "saved_instance_category";
    private static final String SAVED_INDEX = "saved_instance_index";
    private static final int AMOUNT_OF_POSTS = 15; //Amount of posts to load

    private SwipeRefreshLayout refreshLayout;
    private ConnectionFailedListener connectionListener;
    private PostAdapter postsAdapter;
    private ProgressBar bigProgressBar, progressBar;
    private SchoolPostsTask schoolPostsTask;
    private Context context;
    private TextView noResults, swipeToRefresh, swipeToRefreshBottom;
    private String searchQuery = ""; //Search query stays the same throughout all fragments
    private ArrayList<Post> schoolPosts = new ArrayList<Post>();
    private int index;
    private int offset = 0;
    private boolean downloadMore = true;
    private boolean shouldClearContents = false; //Clear current list when manually refreshing

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); //Options in Actionbar (Search)
        setRetainInstance(true); //Prevents fragment from being destroyed during activity change. (Especially if AsyncTask is currently running)

        Log.d(TAG, "Fragment - onCreate");
        context = getActivity().getApplicationContext();
        postsAdapter = new PostAdapter(this.getActivity(), schoolPosts);
    }

    @Override //This method may be called several times
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "Fragment - onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_school_page, container, false);

        bigProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        refreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.refresh_layout);
        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBarGrid);
        noResults = (TextView) rootView.findViewById(R.id.noResults);
        swipeToRefresh = (TextView) rootView.findViewById(R.id.text_swipe_refresh);
        swipeToRefreshBottom = (TextView) rootView.findViewById(R.id.text_swipe_refresh_error);
        GridView gridView = (GridView) rootView.findViewById(R.id.gridView);

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);

        bigProgressBar.setVisibility(View.GONE);

        Bundle args = getArguments();
        //Recalls data from onSaveState. Prevents network calls for a simple orientation change
//        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
//            Log.d(TAG, "savedInstanceState not null");
//            if (savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST) != null) {
//                Log.d(TAG, "savedInstanceState has ArrayList data");
//                schoolPosts = savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST);
//                category = savedInstanceState.getString(SAVED_CATEGORY);
//                index = savedInstanceState.getInt(SAVED_INDEX);
//                offset += schoolPosts.size();
//
//                postsAdapter = new PostAdapter(this.getActivity(), schoolPosts);
////                for(Post p : schoolPosts)
////                    new ThumbnailTask(p).execute();
//            }
//        else {
            category = args.getString(ARG_CATEGORY);
            index = args.getInt(INDEX);
//        }

        /*On initial create, ArrayList will be empty. But onCreateView may be called several times
        Only call this method if the ArrayList is empty, which should only be during the initial creation*/
        if (schoolPosts.isEmpty()) {
            bigProgressBar.setVisibility(View.VISIBLE);
            downloadMorePosts(bigProgressBar);
        }

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

    /*Not needed now because Fragment uses setRetainInstance(true)*/
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putParcelableArrayList(SAVED_ARRAYLIST, schoolPosts);
//        outState.putString(SAVED_CATEGORY, category);
//        outState.putInt(SAVED_INDEX, index);
//    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            connectionListener = (ConnectionFailedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ConnectionFailedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Unregister when fragment is visible, because it only needs to be update it's not visible (Adding/Editing a post)
        LocalBroadcastManager.getInstance(context).unregisterReceiver(updatePostsReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(context).registerReceiver(updatePostsReceiver, new IntentFilter(ACTION_UPDATE_POSTS));
    }

    private BroadcastReceiver updatePostsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentCategoryName = intent.getStringExtra(AddPost.CATEGORY_NAME);
            if((intentCategoryName != null && category.equals(intentCategoryName)) || category.equals(context.getString(R.string.server_category_all))) {
                //Equivalent of onRefresh()
                refreshLayout.setEnabled(false);
                offset = 0;
                shouldClearContents = true;

                bigProgressBar.setVisibility(View.VISIBLE);
                downloadMorePosts(bigProgressBar);
            }
        }
    };

    //Download more posts from the server
    private void downloadMorePosts(ProgressBar progress) {
        noResults.setVisibility(View.INVISIBLE);
        swipeToRefresh.setVisibility(View.GONE);
        schoolPostsTask = new SchoolPostsTask(progress);
        schoolPostsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG));
    }

    //Set to false if the server returned an empty list
    public void shouldDownLoadMore(boolean downloadMore) {
        this.downloadMore = downloadMore;

        if (schoolPosts.isEmpty()) {
            noResults.setVisibility(View.VISIBLE);
            swipeToRefresh.setVisibility(View.VISIBLE);
        } else {
            noResults.setVisibility(View.INVISIBLE);
            swipeToRefresh.setVisibility(View.GONE);
        }
    }

    public interface ConnectionFailedListener {
        public void hasConnection(boolean isConnected, String message);
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
            swipeToRefreshBottom.setVisibility(View.INVISIBLE);
        }

        @Override
        protected Integer doInBackground(String... schoolName) {
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            if (isCancelled())
                return null;

            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                ObjectResult<ArrayList<Post>> result = database.getSchoolPosts(schoolID, searchQuery, category, offset, AMOUNT_OF_POSTS);
                serverResponse = result.getStatus();
                newPosts = result.getObject();
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
                connectionListener.hasConnection(true, StatusCodeParser.getStatusString(context, StatusCodeParser.STATUS_OK));
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
                        new ThumbnailTask(post).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, post.getImgUrl());
                }
            } else {
                connectionListener.hasConnection(false, StatusCodeParser.getStatusString(context, serverResponse));

                //Show different refresh options based on number of already-visible posts.
                if (postsAdapter.isEmpty()) {
                    noResults.setVisibility(View.VISIBLE);
                    swipeToRefresh.setVisibility(View.VISIBLE);
                } else
                    swipeToRefreshBottom.setVisibility(View.VISIBLE);
            }
            refreshLayout.setEnabled(true);
            refreshLayout.setRefreshing(false);
        }
    }

    //Retrieves the thumbnail images for posts
    private class ThumbnailTask extends AsyncTask<String, Void, Bitmap> {

        private Post post;

        public ThumbnailTask(Post post){
            this.post = post;
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            Bitmap bm = null;

            if(imgURL[0].equalsIgnoreCase(context.getString(R.string.default_image_url)))
                return BitmapFactory.decodeResource(context.getResources(), R.drawable.post_image);

            String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
            String key = post.getIdentifier()+"_thumb";

            DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID + DiskLruImageCache.DIRECTORY_POST_IMAGES);

            try {
                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

                if(bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(imgURL[0]);

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Retrieving image", e);
            }
            finally{
                imageCache.close();
            }

            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                post.setBitmapImage(bitmap);

            postsAdapter.notifyDataSetChanged();
        }
    }

}