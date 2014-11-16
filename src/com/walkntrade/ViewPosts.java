package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.adapters.item.ViewPostItem;
import com.walkntrade.io.DataParser;
import com.walkntrade.objects.ReferencedPost;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class ViewPosts extends Activity implements AdapterView.OnItemClickListener{

    private static final String TAG = "ViewPost";
    private static final int REQUEST_EDIT_POST = 100;
    public static final int RESULT_REPOPULATE = 200;

    private Context context;
    private ProgressBar progressBar;
    private TextView noResults;
    private ListView listOfPosts;
    private ViewPostAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_posts);

        context = getApplicationContext();
        listOfPosts = (ListView) findViewById(R.id.postsList);
        noResults = (TextView) findViewById(R.id.noPosts);
        progressBar = (ProgressBar) findViewById(R.id.progressBarViewPosts);

        new UserPostsTask().execute();

        listOfPosts.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listOfPosts.setMultiChoiceModeListener(new MultiChoiceListener());
        listOfPosts.setOnItemClickListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ViewPostItem item = (ViewPostItem) parent.getItemAtPosition(position);

        if(!item.isHeader()) {
            Intent editPost = new Intent(ViewPosts.this, EditPost.class);

            String obsId = item.getObsId();
            String splitID[] = obsId.split(":");
            String identifier = splitID[1];
            identifier = identifier.toLowerCase(Locale.US);

            editPost.putExtra(EditPost.POST_ID, obsId);
            editPost.putExtra(EditPost.POST_IDENTIFIER, identifier);
            editPost.putExtra(EditPost.POST_SCHOOL, item.getSchoolAbbv());
            startActivityForResult(editPost, REQUEST_EDIT_POST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_EDIT_POST:
                if(resultCode == RESULT_REPOPULATE)
                    //Repopulate list
                    new UserPostsTask().execute(); break;
        }
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
                case R.id.action_delete:
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

    private class ViewPostAdapter extends ArrayAdapter<ViewPostItem> {
        public ViewPostAdapter(Context _context, List<ViewPostItem> _items) {
            super(_context, R.layout.item_post_content, _items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View postItemView;

            final ViewPostItem item = getItem(position);
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            //If item is header, use header layout
            if(item.isHeader()) {
                postItemView = inflater.inflate(R.layout.item_post_school, parent, false);

                TextView header = (TextView) postItemView.findViewById(R.id.drawer_header);
                header.setText(item.getContents());
            }
            else { //Item is a post, so use view post item layout
                postItemView = inflater.inflate(R.layout.item_post_content, parent, false);

                TextView postTitle = (TextView) postItemView.findViewById(R.id.view_post_title);
                TextView renewPost = (TextView) postItemView.findViewById(R.id.renew_post);

                if(item.isExpired()) {
                    postItemView.setBackgroundColor(getContext().getResources().getColor(R.color.lighter_red));
                    renewPost.setVisibility(View.VISIBLE);
                }
                else if(item.getExpire() > -1) {
                    postItemView.setBackgroundColor(getContext().getResources().getColor(R.color.yellow));
                    renewPost.setVisibility(View.VISIBLE);
                }

                postTitle.setText(item.getContents());
                renewPost.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new RenewPostTask().execute(item.getObsId());
                    }
                });
            }

            return postItemView;
        }

        @Override //Returns true if item is not a separator (non-selectable, non-clickable)
        //Prevents School names from being selected
        public boolean isEnabled(int position) {
            ViewPostItem item = getItem(position);

            //If item is not a header, it is selectable
            return !item.isHeader();
        }
    }

    private class UserPostsTask extends AsyncTask<Void, Void, Integer> {

        private ArrayList<ReferencedPost> userPosts;

        public UserPostsTask() {
            userPosts = new ArrayList<ReferencedPost>();
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            int serverResponse = -100;

            try {
                serverResponse = database.getUserPosts(userPosts);
            }
            catch(Exception e) {
                Log.e(TAG, "Get user posts", e);
            }
            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressBar.setVisibility(View.GONE);
            listOfPosts.setAdapter(null); //Clears out any previous items

            if (userPosts.isEmpty())
                noResults.setVisibility(View.VISIBLE);
            else {
                noResults.setVisibility(View.GONE);
                ArrayList<ViewPostItem> items = new ArrayList<ViewPostItem>();

                String currentSchool = "";

                for (ReferencedPost p : userPosts) {
                    if(!p.getSchool().equalsIgnoreCase(currentSchool)) { //If this post is a new school, create a new header
                        currentSchool = p.getSchool();
                        items.add(new ViewPostItem(p.getSchool()));
                    }

                    items.add(new ViewPostItem(p)); //Then continue adding posts
                }

                adapter = new ViewPostAdapter(context, items);
                listOfPosts.setAdapter(adapter);
            }
        }
    }

    private class RenewPostTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... obsId) {
            DataParser database = new DataParser(context);

            try {
                database.renewPost(obsId[0]);
            } catch(IOException e) {
                Log.e(TAG, "Renewing post", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Repopulate with updated info
            new UserPostsTask().execute();
        }
    }

    private class RemovePostTask extends AsyncTask<ArrayList<String>, Void, Void> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(ArrayList<String>... postToDelete) {
            DataParser database = new DataParser(context);

            try {
                for(String s : postToDelete[0])
                    database.removePost(s);
            } catch (IOException e) {
                Log.e(TAG, "Deleting post(s)", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //Repopulate list
            new UserPostsTask().execute();
        }
    }
}
