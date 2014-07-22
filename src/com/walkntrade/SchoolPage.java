package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.walkntrade.adapters.DrawerAdapter;
import com.walkntrade.adapters.TabsPagerAdapter;
import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.asynctasks.AvatarRetrievalTask;
import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.asynctasks.UserNameTask;
import com.walkntrade.io.DataParser;

import java.util.ArrayList;

public class SchoolPage extends Activity {

    private final String TAG = "SchoolPage";
	public static final String SELECTED_POST = "Selected_Post";
	
	private String[] drawerOptions;
	private DrawerLayout mDrawerLayout;
	private ListView navigationDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	
	private ActionBar actionBar;
    private Context context;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_school_page);

        if(DataParser.isUserLoggedIn(this))
            new PollMessagesTask(this).execute();

		actionBar = getActionBar();
        context = getApplicationContext();
		drawerOptions = getResources().getStringArray(R.array.user_options);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		navigationDrawerList = (ListView) findViewById(R.id.navigation_drawer_list);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerTabStrip pagerTab = (PagerTabStrip) findViewById(R.id.pager_tab);
        TabsPagerAdapter tabsAdapter = new TabsPagerAdapter(getFragmentManager(), this);

        updateDrawer();

        actionBar.setDisplayHomeAsUpEnabled(true);

        //Set Title in Action Bar to the name of the school
        actionBar.setTitle(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG));

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close){
			
			public void onDrawerOpened(View view) {//Navigation Drawer is completely open
				actionBar.setTitle(getString(R.string.app_name));

                if(DataParser.isUserLoggedIn(context)){
                    //TODO: Revise this later. Make it more efficient

                    DrawerAdapter adapter = (DrawerAdapter)navigationDrawerList.getAdapter();
                    DrawerItem inboxItem = adapter.getItem(7);
                    inboxItem.setCounter(DataParser.getMessagesAmount(context));

                    adapter.notifyDataSetChanged();
                }

				invalidateOptionsMenu();
				super.onDrawerOpened(view);
			}
			
			public void onDrawerClosed(View view) { //Navigation Drawer is completely closed
                //Set Title in Action Bar to the name of the school
                actionBar.setTitle(DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG));
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

        if(DataParser.isUserLoggedIn(context))
            new PollMessagesTask(this).execute(); //Check for new messages

        invalidateOptionsMenu(); //Refreshes the ActionBar menu when activity is resumed
        updateDrawer();
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

        if(DataParser.isUserLoggedIn(context)) {
            //Disable log-in icon
            loginItem.setVisible(false);
            //User logged in, enable sign out option
            signOutItem.setVisible(true);
            //Add inbox item
            inboxItem.setEnabled(true);
            inboxItem.setVisible(true);

            if(DataParser.getMessagesAmount(context) > 0)
                inboxItem.setIcon(R.drawable.ic_action_unread);
            else
                inboxItem.setIcon(R.drawable.ic_action_email);
        }
        else if(!DataParser.isUserLoggedIn(context)) {
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

        switch(item.getItemId()) {
            case R.id.action_login:
                if(!DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context))
                    startActivity(new Intent(this, LoginActivity.class));
                return true;
            case R.id.action_inbox:
                if(DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context)) {
                    Intent getMessageIntent = new Intent(this, Messages.class);
                    getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
                    startActivity(getMessageIntent);
                }
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    //Update contents in Navigation Drawer. User logged in/ User not logged in
    private void updateDrawer() {
        if(DataParser.isNetworkAvailable(this) && DataParser.isUserLoggedIn(context)) {
            new AvatarRetrievalTask(this, navigationDrawerList).execute();
            new UserNameTask(this, navigationDrawerList).execute();
        }

        navigationDrawerList.setAdapter(null); //Clears out current information
        //Create titles and options for the NavigationDrawer
        ArrayList<DrawerItem> items = new ArrayList<DrawerItem>();

        if(DataParser.isUserLoggedIn(context)){
            //User is signed in
            items.add(new DrawerItem(R.drawable.avatar, DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_NAME), true)); //User Item
            items.add(new DrawerItem(drawerOptions[0])); //Post [SECTION]
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[1])); //Books
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[2])); //Tech
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[3])); //Services
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[4])); //Misc.
            items.add(new DrawerItem(drawerOptions[5])); //Messages [SECTION]
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[6], DataParser.getMessagesAmount(context))); //Inbox
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[7])); //Sent
            items.add(new DrawerItem(drawerOptions[8])); //Settings [SECTION]
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[9])); //Account
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[10])); //Select School
        }
        else {
            //User is signed out
            items.add(new DrawerItem(R.drawable.avatar, getString(R.string.user_name_no_login), true));
            items.add(new DrawerItem(drawerOptions[8])); //Settings [SECTION]
            items.add(new DrawerItem(R.drawable.ic_action_mic, drawerOptions[10])); //Select School
        }

        navigationDrawerList.setAdapter(new DrawerAdapter(this, items));
    }

    private void selectItem(int position, long id) {

        //TODO: Special case scenario. Revise later: Assign ids to items on drawer
        if(id == 1994) {
            startActivity(new Intent(SchoolPage.this, Selector.class));//Change School
            finish();
            return;
        }

		//Perform action based on selected item
		switch(position) {
            case 2:
            case 3:
            case 4:
            case 5:
                Intent addPostIntent = new Intent(this, AddPost.class);
                addPostIntent.putExtra(AddPost.CATEGORY_POSITION, position);
                startActivity(addPostIntent);  break;
            case 7:
                Intent getMessageIntent = new Intent(this, Messages.class);
                getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
                startActivity(getMessageIntent); break;
            case 8:
                getMessageIntent = new Intent(this, Messages.class);
                getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.SENT_MESSAGES);
                startActivity(getMessageIntent); break;
            case 10:
                 startActivity(new Intent(this, UserSettings.class)); break; //Account
            case 11: startActivity(new Intent(SchoolPage.this, Selector.class));//Change School
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
