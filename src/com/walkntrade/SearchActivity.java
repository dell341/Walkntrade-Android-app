package com.walkntrade;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.walkntrade.adapters.PostAdapter;
import com.walkntrade.fragments.TaskFragment;
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

public class SearchActivity extends ActionBarActivity implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener, TaskFragment.TaskCallbacks {

    private static final String TAG = "SearchActivity";
    private static final String TAG_TASK_FRAGMENT = "com.walkntrade.SearchActivity.Task_Fragment";
    private static final String SAVED_ARRAYLIST = "saved_instance_arraylist";
    private static final String SAVED_CATEGORY = "saved_instance_category";
    private static final String SAVED_QUERY = "saved_instance_query";
    private static final String SAVED_INDEX = "saved_instance_index";
    public static final String EXTRA_CATEGORY = "extra_current_category";
    public static final String EXTRA_QUERY = "extra_search_query";
    public static final String EXTRA_INDEX = "extra_index";

    private ProgressBar progressBar;
    private Context context;
    private ArrayList<Post> schoolPosts;
    private PostAdapter postsAdapter;
    private TextView noResults;
    private GridView gridView;
    private EditText editText;
    private String searchQuery, category;
    private int offset, index;
    private boolean downloadMore = true;

    private TaskFragment taskFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        context = getApplicationContext();
        searchQuery = getIntent().getStringExtra(EXTRA_QUERY);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        index = getIntent().getIntExtra(EXTRA_INDEX, 0);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        noResults = (TextView) findViewById(R.id.noResults);
        gridView = (GridView) findViewById(R.id.gridView);
        final Spinner spinner = (Spinner) findViewById(R.id.spinner);
        editText = (EditText) findViewById(R.id.edit_text);

        taskFragment = (TaskFragment) getFragmentManager().findFragmentByTag(TAG_TASK_FRAGMENT);

        if(savedInstanceState != null) {
            schoolPosts = savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST);
            category = savedInstanceState.getString(SAVED_CATEGORY);
            searchQuery = savedInstanceState.getString(SAVED_QUERY);
            index = savedInstanceState.getInt(SAVED_INDEX);

            offset = 0;
            postsAdapter = new PostAdapter(context, schoolPosts);
            gridView.setAdapter(postsAdapter);
            editText.setText(searchQuery);
            getSupportActionBar().setTitle(searchQuery);
        }
        else
            init();

        String[] categoryTitles = new String[DataParser.getSharedIntPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_AMOUNT)];

        for (int i = 0; i < categoryTitles.length; i++)
            categoryTitles[i] = DataParser.getSharedStringPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_NAME + i);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getActionBar().getThemedContext(), android.R.layout.simple_spinner_item, categoryTitles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setSelection(index);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                category = DataParser.getSharedStringPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_ID + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        //Search with a click
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchQuery = editText.getText().toString();
                    init();
                }
                return false;
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void init() {
        getActionBar().setTitle(searchQuery);

        gridView.setAdapter(null);
        offset = 0;
        schoolPosts = new ArrayList<>();
        postsAdapter = new PostAdapter(context, schoolPosts);
        gridView.setAdapter(postsAdapter);
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(this);
        editText.setText(searchQuery);
        downloadMorePosts();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home: //If the up button was selected, close this activity
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                downloadMorePosts();

            postsAdapter.loadMore();
            postsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Post selectedPost = (Post) parent.getItemAtPosition(position);

        Intent showPage = new Intent(this, ShowPage.class);
        showPage.putExtra(SchoolPage.SELECTED_POST, selectedPost);
        startActivity(showPage);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_ARRAYLIST, schoolPosts);
        outState.putString(SAVED_CATEGORY, category);
        outState.putString(SAVED_QUERY, editText.getText().toString());
        outState.putInt(SAVED_INDEX, index);
    }

    //Download more posts from the server
    private void downloadMorePosts() {
        noResults.setVisibility(View.GONE);
        Bundle args = new Bundle();
        args.putInt(TaskFragment.ARG_TASK_ID, TaskFragment.TASK_POST_SEARCH);
        args.putString(TaskFragment.ARG_SEARCH_QUERY, searchQuery);
        args.putString(TaskFragment.ARG_CATEGORY, category);
        args.putInt(TaskFragment.ARG_OFFSET, offset);

        taskFragment = new TaskFragment();
        taskFragment.setArguments(args);

        getFragmentManager().beginTransaction().add(taskFragment, TAG_TASK_FRAGMENT).commit();
    }

    @Override
    public void onPreExecute(int taskId) {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onProgressUpdate(int percent) {

    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onPostExecute(int taskId, Object result) {
        progressBar.setVisibility(View.GONE);

        offset += 15;
        ObjectResult<ArrayList<Post>> objectResult = (ObjectResult<ArrayList<Post>>) result;

        int serverResponse = objectResult.getStatus();

        if(serverResponse == StatusCodeParser.STATUS_OK) {
            ArrayList<Post> posts = ((ObjectResult<ArrayList<Post>>) result).getObject();

            if (posts.isEmpty()) { //If server returned empty list, don't try to download anymore
                downloadMore = false;
                noResults.setText(context.getString(R.string.no_results));
                noResults.setVisibility(View.VISIBLE);

            } else {
                noResults.setVisibility(View.GONE);
                postsAdapter.incrementCount(posts);
                for (Post i : posts)
                    schoolPosts.add(i);

                postsAdapter.notifyDataSetChanged();

                for (Post post : posts)
                    new ThumbnailTask(post).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, post.getImgUrl());
            }
        }
        else {
            postsAdapter.clearContents();
            postsAdapter.notifyDataSetChanged();
            noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
            noResults.setVisibility(View.VISIBLE);
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
