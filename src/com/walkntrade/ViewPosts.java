package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.adapters.ViewPostAdapter;
import com.walkntrade.asynctasks.UserPostsTask;
import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class ViewPosts extends Activity {

    private static final String TAG = "ViewPost";

    private Context context;
    private ListView listOfPosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_posts);

        context = getApplicationContext();
        listOfPosts = (ListView) findViewById(R.id.postsList);
        TextView noResults = (TextView) findViewById(R.id.noPosts);
        ProgressBar pBar = (ProgressBar) findViewById(R.id.progressBarViewPosts);

        new UserPostsTask(this, pBar, noResults, listOfPosts).execute();

        listOfPosts.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listOfPosts.setMultiChoiceModeListener(new MultiChoiceListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_posts, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu_post, menu);
    }

    private class MultiChoiceListener implements AbsListView.MultiChoiceModeListener{
        ArrayList<String> listOfPostId = new ArrayList<String>();
        private int count = 0;

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean selected) {

            ViewPostAdapter adapter = (ViewPostAdapter) listOfPosts.getAdapter();
            ViewPostItem item = adapter.getItem(position);

            if(selected) {
                listOfPostId.add(item.getObsId());
                count++;
            }
            else {
                listOfPostId.remove(item.getObsId());
                count--;
            }

            actionMode.setTitle(count+" post(s) selected");
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.context_menu_post, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch(menuItem.getItemId()){
                case R.id.action_delete_post:
                    new RemovePostTask().execute(listOfPostId);
                    actionMode.finish(); //Close the Contextual Action Bar
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            count = 0;
        }
    }

    private class RemovePostTask extends AsyncTask<ArrayList<String>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<String>... postToDelete) {
            DataParser database = new DataParser(context);

            try {
                for(String s : postToDelete[0])
                    Log.v(TAG, "Removing " + s + ":" + database.removePost(s));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

    }
}
