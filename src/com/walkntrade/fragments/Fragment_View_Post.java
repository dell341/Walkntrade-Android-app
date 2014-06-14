package com.walkntrade.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.ViewPostItem;
import com.walkntrade.adapters.ViewPostAdapter;
import com.walkntrade.asynctasks.RemovePostTask;
import com.walkntrade.asynctasks.UserPostsTask;

import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class Fragment_View_Post extends Fragment {

    private static final String TAG = "Fragment:ViewPost";
    public static final String POST_LINK = "Post_Obs_Id";

    private ActionMode actionMode;
    private ListView listOfPosts;
    private String selectedObsId;

    //TODO: Add checkboxes to posts
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
       View rootView = inflater.inflate(R.layout.fragment_view_posts, container, false);

        listOfPosts = (ListView) rootView.findViewById(R.id.postsList);
        TextView noResults = (TextView) rootView.findViewById(R.id.noPosts);
        ProgressBar pBar = (ProgressBar) rootView.findViewById(R.id.progressBarViewPosts);

        new UserPostsTask(getActivity(), pBar, noResults, listOfPosts).execute();

        listOfPosts.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listOfPosts.setMultiChoiceModeListener(new MultiChoiceListener());

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
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
                    new RemovePostTask(getActivity()).execute(listOfPostId);
                    actionMode.finish(); //Close the Contextual Action Bar

                    //Refresh current Fragment
                    FragmentManager manager = getActivity().getFragmentManager();
                    FragmentTransaction ft = manager.beginTransaction();
                    Fragment newFragment = Fragment_View_Post.this;
                    Fragment_View_Post.this.onDestroy();
                    ft.remove(Fragment_View_Post.this);
                    ft.replace(R.id.frame_layout, newFragment);
                    ft.addToBackStack(null);
                    ft.commit();

                    return true;
                default: return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            actionMode = null;
            count = 0;
        }
    }
}
