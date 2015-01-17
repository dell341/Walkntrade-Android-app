package com.walkntrade;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.adapters.item.ViewPostItem;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.ReferencedPost;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class ViewPosts extends Activity implements AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ViewPost";
    private static final String SAVED_LIST = "saved_list_of_items";
    private static final int REQUEST_EDIT_POST = 100;
    public static final int RESULT_REPOPULATE = 200;

    private Context context;
    private ProgressBar progressBar;
    private TextView noResults;
    private SwipeRefreshLayout refreshLayout;
    private ListView listView;
    private ViewPostAdapter adapter;
    private MultiChoiceListener multiChoiceListener;
    private boolean actionModeActivated = false;
    private boolean useConvertedView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_posts);

        context = getApplicationContext();
        listView = (ListView) findViewById(R.id.postsList);
        noResults = (TextView) findViewById(R.id.noPosts);
        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        progressBar = (ProgressBar) findViewById(R.id.progressBarViewPosts);

        refreshLayout.setColorSchemeResources(R.color.green_progress_1, R.color.green_progress_2, R.color.green_progress_3, R.color.green_progress_1);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setEnabled(false);
        multiChoiceListener = new MultiChoiceListener();

        if(savedInstanceState != null) {
            ArrayList<ViewPostItem> items = savedInstanceState.getParcelableArrayList(SAVED_LIST);
            adapter = new ViewPostAdapter(context, items);
            listView.setAdapter(adapter);

            if(adapter.isEmpty())
                new UserPostsTask().execute();
        }
        else {
            adapter = new ViewPostAdapter(context, new ArrayList<ViewPostItem>());
            new UserPostsTask().execute();
        }

        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(multiChoiceListener);
        listView.setOnItemClickListener(this);

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

        if (!item.isHeader()) {
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
                if (resultCode == RESULT_REPOPULATE)
                    //Repopulate list
                    new UserPostsTask().execute();
                break;
        }
    }

    @Override
    public void onRefresh() {
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_LIST, adapter.getAllItems());
    }

    private void removeView(final View view, final ViewPostItem item) {
        ViewPropertyAnimator animator = view.animate();
        animator.setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                adapter.remove(item);
                adapter.notifyDataSetChanged();
                view.setAlpha(1);
                try {
                    ((CheckBox) view.findViewById(R.id.checkBox)).setChecked(false); //Set checked to false, so it's not initially checked when the view is reused
                } catch (NullPointerException e) {
                    Log.e(TAG, "CheckBox doesn't exist for this view");
                } finally {
                    if (adapter.getSize() <= 0) {
                        noResults.setText(context.getString(R.string.no_posts));
                        noResults.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        animator.setDuration(500).alpha(0);
    }

    private class MultiChoiceListener implements AbsListView.MultiChoiceModeListener, View.OnClickListener {
        ArrayList<ViewPostItem> itemsToDelete = new ArrayList<ViewPostItem>();
        ArrayList<View> viewsToAnimate = new ArrayList<View>();
        private int count = 0;
        private boolean checkBoxCounted = false; //Prevents count from being counted twice, when checkBox is checked
        private boolean checkBoxClicked = false;

        //Keeps track of when checkbox was clicked vs entire row clicked
        @Override
        public void onClick(View view) {
            checkBoxClicked = true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean isChecked) {
            ViewPostAdapter adapter = (ViewPostAdapter) listView.getAdapter();
            ViewPostItem selectedItem = adapter.getItem(position);
            View selectedView = selectedItem.getItemView();

            ((CheckBox) selectedView.findViewById(R.id.checkBox)).setChecked(isChecked);

            if (!checkBoxCounted || checkBoxClicked) {
                if (isChecked) {
                    itemsToDelete.add(selectedItem);
                    viewsToAnimate.add(selectedView);
                    count++;
                } else {
                    itemsToDelete.remove(selectedItem);
                    viewsToAnimate.remove(selectedView);
                    count--;
                }
            }

            actionMode.setTitle(count + " post(s) selected");
            if (!checkBoxClicked) //Do not change if checkbox was selected. Only if item was selected.
                checkBoxCounted = !checkBoxCounted;

            checkBoxClicked = false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            useConvertedView = true;
            actionModeActivated = true;
            actionMode.getMenuInflater().inflate(R.menu.context_menu_post, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    new RemovePostTask(itemsToDelete, viewsToAnimate).execute();
                    actionMode.finish(); //Close the Contextual Action Bar
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            useConvertedView = false;
            actionModeActivated = false;
            count = 0;
        }
    }

    private class ViewPostAdapter extends ArrayAdapter<ViewPostItem> {
        private static final String HEADER = "header_item";
        private static final String CONTENT = "content_item";
        private ArrayList<ViewPostItem> items;

        public ViewPostAdapter(Context _context, ArrayList<ViewPostItem> _items) {
            super(_context, R.layout.item_post_content, _items);
            items = _items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View postItemView;
            final ViewPostItem item = getItem(position);

            //If the recycled view (converted view) is not null. And the recycled view is compatible with the new view. Use the recycled view instead of creating a new one
            if (convertView != null  && (item.isContent() && ((String) convertView.getTag()).equalsIgnoreCase(CONTENT))) {
                postItemView = convertView;
            } else {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                if (item.isHeader()) {
                    postItemView = inflater.inflate(R.layout.item_post_school, parent, false);
                    postItemView.setTag(HEADER);
                }
                else {
                    postItemView = inflater.inflate(R.layout.item_post_content, parent, false);
                    postItemView.setTag(CONTENT);
                }
            }

            //If item is header, use header layout
            if (item.isHeader()) {
                //If there are no more posts belonging to a school. Remove this school header
                if (items.size() <= 1 || getItem(position + 1) == null || !item.getSchoolAbbv().equalsIgnoreCase(getItem(position + 1).getSchoolAbbv()))
                    removeView(postItemView, item);

                TextView header = (TextView) postItemView.findViewById(R.id.content_title);
                header.setText(item.getContents());
            } else { //Item is a post, so use view post item layout
                ImageView renewPost = (ImageView) postItemView.findViewById(R.id.renew_post);
                TextView postTitle = (TextView) postItemView.findViewById(R.id.content_title);
                CheckBox checkBox = (CheckBox) postItemView.findViewById(R.id.checkBox);

                if (item.isExpired() || item.getExpire() > -1) {
                    renewPost.setVisibility(View.VISIBLE);
                    postTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                }

                item.setItemView(postItemView);
                final int p = position;
                checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                        listView.setItemChecked(p, value);
                    }
                });
                checkBox.setOnClickListener(multiChoiceListener);
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

        @Override
        public ViewPostItem getItem(int position) {
            if (position >= items.size())
                return null;
            return super.getItem(position);
        }

        public int getSize() {
            return items.size();
        }

        public ArrayList<ViewPostItem> getAllItems() {
            return items;
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
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                ObjectResult<ArrayList<ReferencedPost>> result = database.getUserPosts();
                serverResponse = result.getStatus();
                userPosts = result.getObject();
            }
            catch(Exception e) {
                Log.e(TAG, "Get user posts", e);
            }
            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressBar.setVisibility(View.GONE);
            listView.setAdapter(null); //Clears out any previous items

            if (serverResponse == StatusCodeParser.STATUS_OK) {
                if (userPosts.isEmpty()) {
                    noResults.setText(context.getString(R.string.no_posts));
                    noResults.setVisibility(View.VISIBLE);
                } else {
                    noResults.setVisibility(View.GONE);
                    ArrayList<ViewPostItem> items = new ArrayList<ViewPostItem>();

                    String currentSchool = "";

                    for (ReferencedPost p : userPosts) {
                        if (!p.getSchool().equalsIgnoreCase(currentSchool)) { //If this post is a new school, create a new header
                            currentSchool = p.getSchool();
                            items.add(new ViewPostItem(p.getSchool(), p.getSchoolAbbv()));
                        }

                        items.add(new ViewPostItem(p)); //Then continue adding posts
                    }

                    adapter = new ViewPostAdapter(context, items);
                    listView.setAdapter(adapter);
                }
            } else {
                noResults.setText(StatusCodeParser.getStatusString(context, serverResponse));
                noResults.setVisibility(View.VISIBLE);
            }
        }
    }

    private class RenewPostTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... obsId) {
            DataParser database = new DataParser(context);
            String response = null;

            try {
                response = database.renewPost(obsId[0]);
            } catch (IOException e) {
                Log.e(TAG, "Renewing post", e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            //Repopulate with updated info
            new UserPostsTask().execute();
        }
    }

    private class RemovePostTask extends AsyncTask<Void, Void, Integer> {

        private ArrayList<ViewPostItem> itemsToDelete;
        private ArrayList<View> viewsToAnimate;

        public RemovePostTask(ArrayList<ViewPostItem> itemsToDelete, ArrayList<View> viewsToAnimate) {
            super();
            this.itemsToDelete = itemsToDelete;
            this.viewsToAnimate = viewsToAnimate;
        }

        @Override
        protected void onPreExecute() {
            refreshLayout.setRefreshing(true);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            int serverResponse = StatusCodeParser.CONNECT_FAILED;
            DataParser database = new DataParser(context);

            try {
            for (ViewPostItem p : itemsToDelete)
                    database.removePost(p.getObsId());
            } catch (IOException e) {
                Log.e(TAG, "Deleting post(s)", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            //Animate changes
            for (int i = 0; i < itemsToDelete.size(); i++) {
                removeView(viewsToAnimate.get(i), itemsToDelete.get(i));
            }
            refreshLayout.setRefreshing(false);
        }
    }
}
