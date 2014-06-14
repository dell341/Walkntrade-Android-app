package com.walkntrade;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.walkntrade.adapters.DrawerAdapter;
import com.walkntrade.asynctasks.AvatarRetrievalTask;
import com.walkntrade.asynctasks.UserNameTask;
import com.walkntrade.fragments.Fragment_Contact_Prefs;
import com.walkntrade.fragments.Fragment_Settings;
import com.walkntrade.fragments.Fragment_View_Post;
import com.walkntrade.io.DataParser;

import java.util.ArrayList;

//Copyright (c), All Rights Reserved, http://walkntrade.com

public class UserSettings extends Activity {

    private static final String TAG = "UserSettings";

    private Context context;
    private String[] drawerOptions;
    private DrawerLayout drawerLayout;
    private ListView navigationDrawerList;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_drawer_skin);

        context = getApplicationContext();
        drawerOptions= getResources().getStringArray(R.array.user_account);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawerList = (ListView) findViewById(R.id.navigation_drawer_list);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {//Navigation Drawer is completely open
                getActionBar().setTitle(R.string.title_activity_user_settings);
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {//Navigation Drawer is completely closed
                super.onDrawerClosed(drawerView);
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
        navigationDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        getActionBar().setDisplayHomeAsUpEnabled(true);
        updateDrawer();
        selectItem(2); //View Posts by default
    }

    private class DrawerItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            selectItem(position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    //TODO: Consider removing user options from NavigationDrawer or adjusting the NavigationDrawer
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
            items.add(new DrawerItem(R.drawable.avatar, DataParser.getNamePref(context), true)); //User Item
            items.add(new DrawerItem(drawerOptions[0])); //Account Options [SECTION]
            items.add(new DrawerItem(android.R.drawable.ic_btn_speak_now, drawerOptions[1])); //View Posts
            items.add(new DrawerItem(android.R.drawable.ic_btn_speak_now, drawerOptions[2])); //Change Avatar
            items.add(new DrawerItem(android.R.drawable.ic_btn_speak_now, drawerOptions[3])); //Account settings
            items.add(new DrawerItem(android.R.drawable.ic_btn_speak_now, drawerOptions[4])); //Contact Preferences
        }
        else {
            //User is signed out
            items.add(new DrawerItem(R.drawable.avatar, getString(R.string.user_name_no_login), true));
            items.add(new DrawerItem(drawerOptions[0])); //Account Options [SECTION]
        }

        navigationDrawerList.setAdapter(new DrawerAdapter(this, items));
    }

    private void selectItem(int position) {
        Fragment fragment;

        switch (position) {
            case 2: //View Posts
                getActionBar().setTitle(drawerOptions[1]);
                fragment = new Fragment_View_Post();
                break;
            case 3: //Change Avatar
                getActionBar().setTitle("Not yet implemented");
                fragment = new Fragment_Settings();
                break;
            case 4: //Account Settings
                getActionBar().setTitle("Not yet implemented");
                fragment = new Fragment_Settings();
                break;
            case 5: //Contact Preferences
                getActionBar().setTitle(drawerOptions[4]);
                fragment = new Fragment_Contact_Prefs();
                break;
            default:

                drawerLayout.closeDrawer(navigationDrawerList);
                return;
        }

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.frame_layout, fragment).commit();

        //Highlight the selected item, update the title, close the drawer
        navigationDrawerList.setItemChecked(position, true);
        drawerLayout.closeDrawer(navigationDrawerList);
    }
}
