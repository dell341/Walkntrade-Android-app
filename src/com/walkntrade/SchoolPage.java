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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.walkntrade.adapters.DrawerAdapter;
import com.walkntrade.adapters.TabsPagerAdapter;
import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.asynctasks.AvatarRetrievalTask;
import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.asynctasks.UserNameTask;
import com.walkntrade.fragments.SchoolPostsFragment;
import com.walkntrade.io.DataParser;

import java.util.ArrayList;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */


public class SchoolPage extends Activity implements SchoolPostsFragment.ConnectionFailedListener{

    private final String TAG = "SchoolPage";
    private static final String SAVED_AVATAR_IMAGE = "saved_instance_avatar";
    public static final String SELECTED_POST = "Selected_Post";

    private DrawerLayout mDrawerLayout;
    private ListView navigationDrawerList;
    private TextView textView;
    private ActionBarDrawerToggle mDrawerToggle;

    private ActionBar actionBar;
    private Context context;

    private boolean hasAvatar;
    private boolean hasPausedActivity = false;
    private boolean isLoggedIn;
    private boolean lastConnectedValue = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_page);

        if (DataParser.isUserLoggedIn(this))
            new PollMessagesTask(this).execute();

        actionBar = getActionBar();
        context = getApplicationContext();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationDrawerList = (ListView) findViewById(R.id.navigation_drawer_list);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerTabStrip pagerTab = (PagerTabStrip) findViewById(R.id.pager_tab);
        textView = (TextView) findViewById(R.id.text_view);
        TabsPagerAdapter tabsAdapter = new TabsPagerAdapter(getFragmentManager(), this);
        hasAvatar = false;

        actionBar.setDisplayHomeAsUpEnabled(true);

        if(savedInstanceState != null)
            hasAvatar = true;

        updateDrawer();

        //Retrieve saved avatar image
        if(savedInstanceState != null &&  DataParser.isNetworkAvailable(this) && DataParser.isUserLoggedIn(context)) {
            Bitmap bm = savedInstanceState.getParcelable(SAVED_AVATAR_IMAGE);

            if(bm == null)
                new AvatarRetrievalTask(this, navigationDrawerList).execute();
            else {
                DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getAdapter();
                DrawerItem item = adapter.getItem(0); //Get user header item
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
                    DrawerAdapter adapter = (DrawerAdapter)navigationDrawerList.getAdapter();
                    DrawerItem inboxItem = adapter.getItem(5);
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
        navigationDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() { //Set position of text view whenever layout is updated. (Only really need to run one time)
                if(lastConnectedValue)
                    textView.setY(0-textView.getHeight());
                else
                    textView.setY(0);
            }
        });
	}

	private class DrawerItemClickListener implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectItem(position, id);
		}
		
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

        DrawerAdapter adapter = (DrawerAdapter) navigationDrawerList.getAdapter();
        DrawerItem item = adapter.getItem(0); //Get user header item

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

//            if (DataParser.getMessagesAmount(context) > 0)
//                inboxItem.setIcon(R.drawable.ic_action_unread);
//            else
                inboxItem.setIcon(R.drawable.ic_chat_white);
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

    @Override //Creates an animated TextView when there is no connection, or for any other error.
    public void hasConnection(boolean isConnected, String message) {
        textView.setText(message);

        if(isConnected == lastConnectedValue) //If connection status has not changed, do not perform another animation
            return;

        if(!isConnected)
            textView.animate().setDuration(500).translationY(0);
        else
            textView.setY(0);

        lastConnectedValue = isConnected;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //If user logs in, update the navigation drawer
        if(requestCode == LoginActivity.REQUEST_LOGIN)
            if(resultCode == Activity.RESULT_OK)
                updateDrawer();
    }


    //Update contents in Navigation Drawer. User logged in/ User not logged in
    private void updateDrawer() {
        if (DataParser.isNetworkAvailable(this) && DataParser.isUserLoggedIn(context)) {
            new UserNameTask(this, navigationDrawerList).execute();
            if(!hasAvatar)
                new AvatarRetrievalTask(this, navigationDrawerList).execute();
        }

        //Create titles and options for the NavigationDrawer
        ArrayList<DrawerItem> items = new ArrayList<DrawerItem>();

        if(DataParser.isUserLoggedIn(context)){
            //User is signed in
            items.add(new DrawerItem(0, R.drawable.ic_action_person, DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME), true)); //User Item
            items.add(new DrawerItem(100, R.drawable.ic_book, getString(R.string.drawer_book))); //Books
            items.add(new DrawerItem(101, R.drawable.ic_tech, getString(R.string.drawer_tech))); //Tech
            items.add(new DrawerItem(102, R.drawable.ic_service, getString(R.string.drawer_service))); //Services
            items.add(new DrawerItem(103, R.drawable.ic_misc, getString(R.string.drawer_misc))); //Misc.
            items.add(new DrawerItem(200, R.drawable.ic_message, getString(R.string.drawer_messages), DataParser.getMessagesAmount(context))); //Messages
            items.add(new DrawerItem(300, R.drawable.ic_account, getString(R.string.drawer_account))); //Account
            items.add(new DrawerItem(400, R.drawable.ic_location, getString(R.string.drawer_change_school))); //Select School
        }
        else if(!DataParser.isUserLoggedIn(context)){
            //User is signed out
            items.add(new DrawerItem(0, R.drawable.ic_action_person, getString(R.string.user_name_no_login), true));
            items.add(new DrawerItem(400, R.drawable.ic_location, getString(R.string.drawer_change_school))); //Select School
        }

        navigationDrawerList.setAdapter(new DrawerAdapter(this, items));
    }

    private void selectItem(int position, long id) {

        int castedId = (int)id;

        //Perform action based on selected item
        switch (castedId) {
            case 100:
            case 101:
            case 102:
            case 103:
                Intent addPostIntent = new Intent(this, AddPost.class);
                addPostIntent.putExtra(AddPost.CATEGORY_ID, castedId);
                startActivity(addPostIntent);  break; //Add Post
            case 200:
                Intent getMessageIntent = new Intent(this, Messages.class);
                startActivity(getMessageIntent); break; //Messages
            case 300:
                startActivity(new Intent(this, UserSettings.class)); break; //Account
            case 400:
                startActivity(new Intent(SchoolPage.this, Selector.class));//Change School
                finish(); break;
            default: return;
        }

		//Highlight the selected item, update the title, close the drawer
		navigationDrawerList.setItemChecked(position, true);
		mDrawerLayout.closeDrawer(navigationDrawerList);
	}

    private void signOut(){
        if(DataParser.isNetworkAvailable(this))
            new LogoutTask(this).execute(); //Starts asynchronous sign out

        invalidateOptionsMenu();
        updateDrawer(); //Update navigation drawer after logging out
    }

}
