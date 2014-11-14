package com.walkntrade;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import com.walkntrade.adapters.DrawerAdapter;
import com.walkntrade.adapters.TabsPagerAdapter;
import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.asynctasks.AvatarRetrievalTask;
import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.asynctasks.UserNameTask;
import com.walkntrade.io.DataParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class SchoolPage extends Activity implements ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener {

    private final String TAG = "SchoolPage";
    private static final String SAVED_AVATAR_IMAGE = "saved_instance_avatar";
    public static final String SELECTED_POST = "Selected_Post";

    private String[] drawerOptions;
    private DrawerLayout mDrawerLayout;
    private ExpandableListView navigationDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private ActionBar actionBar;
    private Context context;

    private boolean hasAvatar;
    private boolean hasPausedActivity = false;
    private boolean isLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_page);

        if (DataParser.isUserLoggedIn(this))
            new PollMessagesTask(this).execute();

        actionBar = getActionBar();
        context = getApplicationContext();
        drawerOptions = getResources().getStringArray(R.array.user_options);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawerList = (ExpandableListView) findViewById(R.id.navigation_drawer_list);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerTabStrip pagerTab = (PagerTabStrip) findViewById(R.id.pager_tab);
        TabsPagerAdapter tabsAdapter = new TabsPagerAdapter(getFragmentManager(), this);
        hasAvatar = false;

        actionBar.setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null)
            hasAvatar = true;

        updateDrawer();
        navigationDrawerList.expandGroup(1); //Expand add post group

        //Retrieve saved avatar image
        if(savedInstanceState != null &&  DataParser.isNetworkAvailable(this) && DataParser.isUserLoggedIn(context)) {
            Bitmap bm = savedInstanceState.getParcelable(SAVED_AVATAR_IMAGE);

            if(bm == null)
                new AvatarRetrievalTask(this, navigationDrawerList).execute();
            else {
                DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getExpandableListAdapter();
                DrawerItem item = (DrawerItem)adapter.getGroup(0); //Get user header item
                item.setAvatar(bm);
                adapter.notifyDataSetChanged();
            }
        }

        //Set Title in Action Bar to the name of the school
        actionBar.setTitle(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG));

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View view) {//Navigation Drawer is completely open
                actionBar.setTitle(getString(R.string.app_name));

                if (DataParser.isUserLoggedIn(context)) {
                    //TODO: Find a better way to update the inbox amount

                    DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getExpandableListAdapter();
                    DrawerItem inboxItem = (DrawerItem) adapter.getGroup(3);
                    inboxItem.setCounter(DataParser.getMessagesAmount(context));

                    adapter.notifyDataSetChanged();
                }

                invalidateOptionsMenu();
                super.onDrawerOpened(view);
            }

            public void onDrawerClosed(View view) { //Navigation Drawer is completely closed
                //Set Title in Action Bar to the name of the school
                actionBar.setTitle(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG));
                invalidateOptionsMenu(); //Forces action bar to refresh
                super.onDrawerClosed(view);
            }
        };

        viewPager.setAdapter(tabsAdapter);
        pagerTab.setTabIndicatorColor(getResources().getColor(R.color.green_dark));
        pagerTab.setTextColor(getResources().getColor(android.R.color.white));
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        navigationDrawerList.setOnGroupClickListener(this);
        navigationDrawerList.setOnChildClickListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        hasPausedActivity = true;
        isLoggedIn = DataParser.isUserLoggedIn(context);
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean currentlyLoggedIn = DataParser.isUserLoggedIn(context);

        if(hasPausedActivity && isLoggedIn != currentlyLoggedIn) //If user changed login status, from the last time page was visited. Update the drawer.
            updateDrawer();

        if (currentlyLoggedIn)
            new PollMessagesTask(this).execute(); //Check for new messages

        invalidateOptionsMenu(); //Refreshes the ActionBar menu when activity is resumed
        hasPausedActivity = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getExpandableListAdapter();
        DrawerItem item = (DrawerItem)adapter.getGroup(0); //Get user header item

        try {
            outState.putParcelable(SAVED_AVATAR_IMAGE, item.getAvatar());
        } catch (NullPointerException e){
            Log.e(TAG, "Orientation Change before image downloaded");
        }
    }

    //Must be called at this state for the ActionBarDrawerToggle
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    //Must be called at this state for the ActionBarDrawerToggle
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem signOutItem = menu.findItem(R.id.action_sign_out);
        MenuItem loginItem = menu.findItem(R.id.action_login);
        MenuItem inboxItem = menu.findItem(R.id.action_inbox);

        if (DataParser.isUserLoggedIn(context)) {
            //Disable log-in icon
            loginItem.setVisible(false);
            //User logged in, enable sign out option
            signOutItem.setVisible(true);
            //Add inbox item
            inboxItem.setEnabled(true);
            inboxItem.setVisible(true);

            if (DataParser.getMessagesAmount(context) > 0)
                inboxItem.setIcon(R.drawable.ic_action_unread);
            else
                inboxItem.setIcon(R.drawable.ic_action_email);
        } else if (!DataParser.isUserLoggedIn(context)) {
            //User logged out, disable sign out option
            signOutItem.setVisible(false);
            //Remove inbox item
            inboxItem.setVisible(false);

            loginItem.setIcon(R.drawable.ic_action_person);

            //If the navigation drawer is open, login is hidden
            boolean drawerOpen = (mDrawerLayout.isDrawerOpen(navigationDrawerList));
            loginItem.setVisible(!drawerOpen);
        }

        SharedPreferences preference = this.getSharedPreferences(DataParser.PREFS_AUTHORIZATION, Context.MODE_PRIVATE);
        boolean isAuthorized = preference.getBoolean(DataParser.KEY_AUTHORIZED, true);

        if (!isAuthorized) { //If user is not authorized clear all info and sign out
            SharedPreferences.Editor editor = preference.edit();
            editor.putBoolean(DataParser.KEY_AUTHORIZED, true);
            editor.apply();
            signOut();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //ActionBarToggle handles navigation drawer events
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.action_login:
                if (!DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context))
                    startActivityForResult(new Intent(this, LoginActivity.class), LoginActivity.REQUEST_LOGIN);
                return true;
            case R.id.action_inbox:
                if (DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context)) {
                    Intent getMessageIntent = new Intent(this, Messages.class);
                    getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
                    startActivity(getMessageIntent);
                }
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            case R.id.action_privacy_feedback:
                startActivity(new Intent(this, Privacy_Feedback.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If user logs in, update the navigation drawer
        if(requestCode == LoginActivity.REQUEST_LOGIN)
            if(resultCode == Activity.RESULT_OK) {
                updateDrawer();
                navigationDrawerList.expandGroup(1); //Expand add post group
            }
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        DrawerItem item = (DrawerItem) parent.getExpandableListAdapter().getGroup(groupPosition);

        //For some reason, isGroupExpanded gives the opposite of the expected values
        if (!parent.isGroupExpanded(groupPosition)) //If group is expanded
            item.setExpandResource(R.drawable.expander_close_holo_light);
        else //If group is collapsed
            item.setExpandResource(R.drawable.expander_open_holo_light);

        if(DataParser.isUserLoggedIn(context))
            selectParentLogin(groupPosition);
        else
            selectParentNoLogin(groupPosition);
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        selectChild(childPosition); //Only 1 set of children exists. So currently, this is acceptable.
        return true;
    }

    //Update contents in Navigation Drawer. User logged in/ User not logged in
    private void updateDrawer() {
        if (DataParser.isNetworkAvailable(this) && DataParser.isUserLoggedIn(context)) {
            new UserNameTask(this, navigationDrawerList).execute();
            if(!hasAvatar)
                new AvatarRetrievalTask(this, navigationDrawerList).execute();
        }

        //Create titles and options for the NavigationDrawer
        ArrayList<DrawerItem> drawerItemParents = new ArrayList<DrawerItem>();
        ArrayList<DrawerItem> drawerItemChildList = new ArrayList<DrawerItem>();
        HashMap<DrawerItem, List<DrawerItem>> drawerItemChildren = new HashMap<DrawerItem, List<DrawerItem>>();

        if (DataParser.isUserLoggedIn(context)) {
            //User is signed in
            drawerItemParents.add(new DrawerItem(R.drawable.ic_action_person, DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), true)); //User Item id:000

            //Expandable section
            DrawerItem postSection = new DrawerItem(drawerOptions[0], R.drawable.expander_open_holo_light);
            drawerItemParents.add(postSection); //Post [SECTION] id:100
            drawerItemChildList.add(new DrawerItem(R.drawable.ic_book, drawerOptions[1])); //Books id:110
            drawerItemChildList.add(new DrawerItem(R.drawable.ic_tech, drawerOptions[2])); //Tech id:120
            drawerItemChildList.add(new DrawerItem(R.drawable.ic_service, drawerOptions[3])); //Services id:130
            drawerItemChildList.add(new DrawerItem(R.drawable.ic_misc, drawerOptions[4])); //Misc. id:140

            drawerItemChildren.put(postSection, drawerItemChildList);

            drawerItemParents.add(new DrawerItem(drawerOptions[5], R.drawable.expander_open_holo_light)); //Messages [SECTION] id:200
            drawerItemParents.add(new DrawerItem(R.drawable.ic_message, drawerOptions[6], DataParser.getMessagesAmount(context))); //Inbox id:300
            drawerItemParents.add(new DrawerItem(R.drawable.ic_message, drawerOptions[7])); //Sent id:400
            drawerItemParents.add(new DrawerItem(drawerOptions[8], R.drawable.expander_open_holo_light)); //Settings [SECTION] id:500
            drawerItemParents.add(new DrawerItem(R.drawable.ic_account, drawerOptions[9])); //Account id:600
            drawerItemParents.add(new DrawerItem(R.drawable.ic_location, drawerOptions[10])); //Select School id:700
        } else if (!DataParser.isUserLoggedIn(context)) {
            //User is signed out
            drawerItemParents.add(new DrawerItem(R.drawable.ic_action_person, getString(R.string.user_name_no_login), true)); //id:000
            drawerItemParents.add(new DrawerItem(drawerOptions[8], R.drawable.expander_open_holo_light)); //Settings [SECTION] id:100
            drawerItemParents.add(new DrawerItem(R.drawable.ic_location, drawerOptions[10])); //Select School id:200
        }

        navigationDrawerList.setAdapter(new DrawerAdapter(this, drawerItemParents, drawerItemChildren));
    }

    private void selectParentLogin(int position) {
        switch(position){
            case 0: break;
            case 1: break;
            case 2: break;
            case 3:
                Intent getMessageIntent = new Intent(this, Messages.class);
                getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
                startActivity(getMessageIntent);
                mDrawerLayout.closeDrawer(navigationDrawerList); break;
            case 4:
                getMessageIntent = new Intent(this, Messages.class);
                getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.SENT_MESSAGES);
                startActivity(getMessageIntent);
                mDrawerLayout.closeDrawer(navigationDrawerList); break;
            case 5: break;
            case 6:
                startActivity(new Intent(this, UserSettings.class));
                mDrawerLayout.closeDrawer(navigationDrawerList);break;
            case 7:
                startActivity(new Intent(SchoolPage.this, Selector.class));//Change School
                finish();
                mDrawerLayout.closeDrawer(navigationDrawerList); break;
        }
    }

    private void selectParentNoLogin(int position){
        switch(position){
            case 0: break;
            case 1: break;
            case 2:
                startActivity(new Intent(SchoolPage.this, Selector.class));//Change School
                finish();
                break;
        }

        mDrawerLayout.closeDrawer(navigationDrawerList);
    }

    private void selectChild(int position){
        switch(position){
            case 0:
            case 1:
            case 2:
            case 3:
                Intent addPostIntent = new Intent(this, AddPost.class);
                addPostIntent.putExtra(AddPost.CATEGORY_POSITION, position);
                startActivity(addPostIntent);
                break;
        }

        mDrawerLayout.closeDrawer(navigationDrawerList);
    }

    private void signOut() {
        if (DataParser.isNetworkAvailable(this))
            new LogoutTask(this).execute(); //Starts asynchronous sign out

        invalidateOptionsMenu();
        updateDrawer(); //Update navigation drawer after logging out
    }

}
