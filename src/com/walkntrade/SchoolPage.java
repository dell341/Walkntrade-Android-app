package com.walkntrade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.walkntrade.adapters.TabsPagerAdapter;
import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.fragments.SchoolPostsFragment;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.ObjectResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */


public class SchoolPage extends ActionBarActivity implements SchoolPostsFragment.ConnectionFailedListener {

    private final String TAG = "SchoolPage";
    private static final String SAVED_AVATAR_IMAGE = "saved_instance_avatar";
    public static final String SELECTED_POST = "Selected_Post";
    public static final String ACTION_UPDATE_DRAWER = "com.walkntrade.SchoolPage.update_drawer";

    private DrawerLayout mDrawerLayout;
    private ListView navigationDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private ActionBar actionBar;
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_page);


        context = getApplicationContext();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawerList = (ListView) findViewById(R.id.navigation_drawer_list);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerTabStrip pagerTab = (PagerTabStrip) findViewById(R.id.pager_tab);
        TabsPagerAdapter tabsAdapter = new TabsPagerAdapter(getFragmentManager(), this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        LocalBroadcastManager.getInstance(context).registerReceiver(schoolPageUpdateReceiver, new IntentFilter(ACTION_UPDATE_DRAWER));
        new PollMessagesTask(context).execute();
        updateDrawer();

        //Retrieve saved avatar image
        if (savedInstanceState != null && DataParser.isNetworkAvailable(context) && DataParser.isUserLoggedIn(context)) {
            Bitmap bm = savedInstanceState.getParcelable(SAVED_AVATAR_IMAGE);

            if (bm == null)
                new AvatarRetrievalTask(this, navigationDrawerList).execute();
            else {
                DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getAdapter();
                DrawerItem item = adapter.getItem(0); //Get user header item
                item.setAvatar(bm);
                adapter.notifyDataSetChanged();
            }
        }

        //Set Title in Action Bar to the name of the school
        actionBar.setTitle(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT_READABLE).toUpperCase());

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View view) {//Navigation Drawer is completely open
                invalidateOptionsMenu();
                super.onDrawerOpened(view);
            }

            public void onDrawerClosed(View view) { //Navigation Drawer is completely closed
                invalidateOptionsMenu(); //Forces action bar to refresh
                super.onDrawerClosed(view);
            }
        };

        viewPager.setAdapter(tabsAdapter);
        pagerTab.setTabIndicatorColor(getResources().getColor(R.color.green_dark));
        pagerTab.setTextColor(getResources().getColor(android.R.color.white));
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        navigationDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private class DrawerItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position, id);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu(); //Refreshes the ActionBar menu when activity is resumed
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getAdapter();
        DrawerItem item = adapter.getItem(0); //Get user header item

        try {
            outState.putParcelable(SAVED_AVATAR_IMAGE, item.getAvatar());
        } catch (NullPointerException e) {
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
        } else {
            //User logged out, disable sign out option
            signOutItem.setVisible(false);
            //Remove inbox item
            inboxItem.setVisible(false);

            loginItem.setIcon(R.drawable.ic_action_person);

            //If the navigation drawer is open, login is hidden
            boolean drawerOpen = (mDrawerLayout.isDrawerOpen(navigationDrawerList));
            loginItem.setVisible(!drawerOpen);
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
                    startActivity(new Intent(this, LoginActivity.class));
                return true;
            case R.id.action_inbox:
                if (DataParser.isUserLoggedIn(context)) {
                    Intent getMessageIntent = new Intent(this, Messages.class);
                    startActivity(getMessageIntent);
                }
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, Privacy_Feedback.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        //Pressing the back button will close the navigation drawer, if it is open
        if (mDrawerLayout.isDrawerOpen(navigationDrawerList)) {
            mDrawerLayout.closeDrawers();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(schoolPageUpdateReceiver);
    }

    @Override //Creates an animated TextView when there is no connection, or for any other error.
    public void hasConnection(boolean isConnected, String message) {

    }

    private BroadcastReceiver schoolPageUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_UPDATE_DRAWER))
                updateDrawer();
        }
    };

    //Update contents in Navigation Drawer. User logged in/ User not logged in
    private void updateDrawer() {
        Log.i(TAG, "Updating navigation drawer");

        //Create titles and options for the NavigationDrawer
        ArrayList<DrawerItem> items = new ArrayList<DrawerItem>();

        if (DataParser.isUserLoggedIn(context)) {
            //User is signed in
            items.add(new DrawerItem(0, R.drawable.circle, DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), true)); //User Item
            //Add all of the add post for the different categories
            for (int i = 0; i < DataParser.getSharedIntPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_AMOUNT); i++) {
                String categoryName = DataParser.getSharedStringPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_NAME + i);

                int iconResource;

                if (categoryName.equals(context.getString(R.string.category_name_all)))
                    continue;
                else if (categoryName.equals(context.getString(R.string.category_name_book)))
                    iconResource = R.drawable.ic_book;
                else if (categoryName.equals(context.getString(R.string.category_name_housing)))
                    iconResource = R.drawable.ic_service;
                else if (categoryName.equals(context.getString(R.string.category_name_tech)))
                    iconResource = R.drawable.ic_tech;
                else if (categoryName.equals(context.getString(R.string.category_name_misc)))
                    iconResource = R.drawable.ic_misc;
                else
                    iconResource = R.drawable.ic_action_remove;

                items.add(new DrawerItem(100 + i, iconResource, categoryName));
            }
            items.add(new DrawerItem(200, R.drawable.ic_message, getString(R.string.drawer_messages), DataParser.getSharedIntPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_MESSAGES))); //Messages
            items.add(new DrawerItem(300, R.drawable.ic_account, getString(R.string.drawer_account))); //Account
            items.add(new DrawerItem(400, R.drawable.ic_location, getString(R.string.drawer_change_school))); //Select School
        } else {
            //User is signed out
            items.add(new DrawerItem(0, R.drawable.circle, getString(R.string.user_name_no_login), true));
            items.add(new DrawerItem(400, R.drawable.ic_location, getString(R.string.drawer_change_school))); //Select School
        }

        navigationDrawerList.setAdapter(new DrawerAdapter(this, items));
        if (DataParser.isNetworkAvailable(this) && DataParser.isUserLoggedIn(context)) {
            getUserName();
            getCachedImage();
        }
    }

    private void getUserName() {
        String userName = DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME); //Gets username, which should already be on device

        DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getAdapter();
        DrawerItem item = adapter.getItem(0); //Get user header item
        item.setTitle(userName);
        adapter.notifyDataSetChanged();
    }

    private void getCachedImage() {
        String avatarURL = DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL);

        if(avatarURL == null) {
            if(DataParser.isNetworkAvailable(context))
                new AvatarRetrievalTask(context, navigationDrawerList).execute();
            return;
        }

        try {
            String splitURL[] = avatarURL.split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            Bitmap bm = imageCache.getBitmapFromDiskCache(key);
            imageCache.close();

            if (bm == null) {
                if (DataParser.isNetworkAvailable(context))
                    new AvatarRetrievalTask(context, navigationDrawerList).execute();
            } else {
                DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getAdapter();
                DrawerItem item = adapter.getItem(0); //Get user header item

                item.setAvatar(bm);
                adapter.notifyDataSetChanged();
            }
        }catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Image does not exist", e);
            //If user has not uploaded an image, leave Bitmap as null
        }
    }

    private void selectItem(int position, long id) {

        int castedId = (int) id;

        //Perform action based on selected item
        switch (castedId) {
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
                Intent addPostIntent = new Intent(this, AddPost.class);
                String category = DataParser.getSharedStringPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_ID + (castedId - 100));
                addPostIntent.putExtra(AddPost.CATEGORY_NAME, category);
                startActivityForResult(addPostIntent, AddPost.REQUEST_ADD_POST);
                break; //Add Post
            case 200:
                Intent getMessageIntent = new Intent(this, Messages.class);
                startActivity(getMessageIntent);
                break; //Messages
            case 300:
                startActivity(new Intent(this, UserSettings.class));
                break; //Account
            case 400:
                startActivity(new Intent(SchoolPage.this, Selector.class));//Change School
                finish();
                break;
            default:
                return;
        }

        //Highlight the selected item, update the title, close the drawer
        navigationDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(navigationDrawerList);
    }

    private void signOut() {
        if (DataParser.isNetworkAvailable(context)) {
            DataParser.setSharedBooleanPreferences(context, DataParser.PREFS_USER, DataParser.KEY_CURRENTLY_LOGGED_IN, false);

            new LogoutTask(this).execute(); //Starts asynchronous sign out
        }
        invalidateOptionsMenu();
    }

    private class DrawerAdapter extends ArrayAdapter<DrawerItem> {

        public DrawerAdapter(Context _context, List<DrawerItem> objects) {
            super(_context, R.layout.item_drawer_content, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View drawerItemView;

            DrawerItem item = getItem(position);
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            //If user item is being created, call the appropriate layout inflater
            if (item.isUser()) {
                drawerItemView = inflater.inflate(R.layout.item_drawer_user, parent, false);

                ImageView icon = (ImageView) drawerItemView.findViewById(R.id.drawer_user);
                TextView content = (TextView) drawerItemView.findViewById(R.id.drawer_user_name);
                TextView login = (TextView) drawerItemView.findViewById(R.id.drawer_login);

                if (!DataParser.isUserLoggedIn(context)) {
                    content.setVisibility(View.GONE);
                    login.setVisibility(View.VISIBLE);

                    login.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (!DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context))
                                startActivity(new Intent(context, LoginActivity.class));
                        }
                    });
                } else {
                    content.setVisibility(View.VISIBLE);
                    login.setVisibility(View.GONE);
                }

                if (!item.isDefaultAvatar())   //if user icon is being uploaded, use bitmap
                    icon.setImageBitmap(item.getAvatar());

                content.setText(item.getTitle());
            }
            //Else create a regular menu option
            else {
                drawerItemView = inflater.inflate(R.layout.item_drawer_content, parent, false);

                ImageView icon = (ImageView) drawerItemView.findViewById(R.id.drawer_icon);
                TextView content = (TextView) drawerItemView.findViewById(R.id.drawer_content);
                TextView counter = (TextView) drawerItemView.findViewById(R.id.counter);

                if (item.hasCounter()) {
                    int amount = item.getCount();

                    if (amount > 0) {
                        counter.setVisibility(View.VISIBLE);
                        if (amount > 99)
                            counter.setText("99+");
                        else
                            counter.setText(Integer.toString(amount));
                    } else
                        counter.setVisibility(View.GONE);
                }

                icon.setImageResource(item.getIconResource());
                content.setText(item.getTitle());
            }
            return drawerItemView;
        }

        //Disables click-ability of User drawer item and Header item
        @Override
        public boolean isEnabled(int position) {
            DrawerItem item = getItem(position);

            return !(item.isHeader() || item.isUser());
        }

        @Override
        public long getItemId(int position) {
            DrawerItem item = getItem(position);

            return item.getId();
        }
    }

    //Asynchronous Task, received user avatar image
    private class AvatarRetrievalTask extends AsyncTask<Void, Void, Bitmap> {
        private Context context;
        private ListView drawerList;

        //Retrieves the user's avatar for the NavigationDrawer
        public AvatarRetrievalTask(Context _context, ListView _drawerList) {
            context = _context;
            drawerList = _drawerList;
        }

        @Override
        protected Bitmap doInBackground(Void... arg0) {
            DataParser database = new DataParser(context);
            Bitmap bm = null;
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            try {
                String avatarURL;

                ObjectResult<String> result = database.getAvatarUrl(null, true);
                avatarURL = result.getObject();

                if (avatarURL == null)
                    return null;

                String splitURL[] = avatarURL.split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

                if (bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(avatarURL);

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Retrieving user avatar", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Image does not exist", e);
                //If user has not uploaded an image, leave Bitmap as null
            } finally {
                imageCache.close();
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap avatar) {
            DrawerAdapter adapter = (DrawerAdapter) drawerList.getAdapter();
            DrawerItem item = adapter.getItem(0); //Get user header item

            if (avatar != null)
                item.setAvatar(avatar);

            adapter.notifyDataSetChanged();
        }
    }

}
