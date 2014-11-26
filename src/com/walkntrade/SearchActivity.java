package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.walkntrade.asynctasks.ThumbnailTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.Post;

import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class SearchActivity extends Activity implements AdapterView.OnItemClickListener, AbsListView.OnScrollListener {

    private static final String TAG = "SearchActivity";
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

        if(savedInstanceState != null) {
            schoolPosts = savedInstanceState.getParcelableArrayList(SAVED_ARRAYLIST);
            category = savedInstanceState.getString(SAVED_CATEGORY);
            searchQuery = savedInstanceState.getString(SAVED_QUERY);
            index = savedInstanceState.getInt(SAVED_INDEX);

            offset = 0;
            postsAdapter = new PostAdapter(context, schoolPosts);
            gridView.setAdapter(postsAdapter);
            editText.setText(searchQuery);
            getActionBar().setTitle(searchQuery);
        }
        else
            init();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(adapter);
        spinner.setSelection(index);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long id) {
                switch (i) {
                    case 0: index = 0;
                        category = context.getString(R.string.server_category_all); break;
                    case 1: index = 1;
                        category = context.getString(R.string.server_category_book); break;
                    case 2: index = 2;
                        category = context.getString(R.string.server_category_tech); break;
                    case 3: index = 3;
                        category = context.getString(R.string.server_category_service); break;
                    case 4: index = 4;
                        category = context.getString(R.string.server_category_misc); break;
                }
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
        schoolPosts = new ArrayList<Post>();
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
        new SchoolPostsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG));
    }

    private class SchoolPostsTask extends AsyncTask<String, Void, Integer> {

        private ArrayList<Post> posts;

        public SchoolPostsTask() {
            posts = new ArrayList<Post>();
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... schoolName) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                DataParser.ObjectResult<ArrayList<Post>> result = database.getSchoolPosts(schoolID, searchQuery, category, offset, 15);
                serverResponse = result.getStatus();
                posts = result.getObject();
            } catch (Exception e) {
                Log.e(TAG, "Retrieving school post(s)", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressBar.setVisibility(View.GONE);

            offset += 15;
            if(serverResponse == StatusCodeParser.STATUS_OK) {
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
                        new ThumbnailTask(SearchActivity.this, postsAdapter, post).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, post.getImgUrl());
                }
            }
            else {
                postsAdapter.clearContents();
                postsAdapter.notifyDataSetChanged();
                noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
                noResults.setVisibility(View.VISIBLE);
            }
        }
    }
}
