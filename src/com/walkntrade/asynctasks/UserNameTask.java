package com.walkntrade.asynctasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ExpandableListView;

import com.walkntrade.adapters.DrawerAdapter;
import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.io.DataParser;

import java.io.IOException;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class UserNameTask extends AsyncTask<Void, Void, String> {

    private final String TAG = "ASYNCTASK:UserNameTask";
    private Context context;
    private ExpandableListView drawerList;

    public UserNameTask(Context _context, ExpandableListView _drawerList){
        context = _context;
        drawerList = _drawerList;
    }
    @Override
    protected String doInBackground(Void... voids) {
        String userName = DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_NAME); //Gets username if already stored on device

        if (userName == null) //If username is not already stored locally
        {
            DataParser database = new DataParser(context);
            try {
                userName = database.simpleGetIntent(DataParser.INTENT_GET_USERNAME);
            } catch (IOException e) {
                Log.e(TAG, "Get username", e);
            }
        }

        return userName;
    }

    @Override
    protected void onPostExecute(String userName) {
        DrawerAdapter adapter = (DrawerAdapter) drawerList.getExpandableListAdapter();
        DrawerItem item = (DrawerItem) adapter.getGroup(0); //Get user header item
        item.setTitle(userName);
        adapter.notifyDataSetChanged();
    }
}
