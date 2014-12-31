package com.walkntrade.adapters;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;

import com.walkntrade.R;
import com.walkntrade.fragments.SchoolPostsFragment;
import com.walkntrade.io.DataParser;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Creates category-specific fragments
public class TabsPagerAdapter extends FragmentPagerAdapter {
    private Context context;
    private String[] tabTitles;

    public TabsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        //String titles pulled from resources to allow easy language translation
        tabTitles = new String[DataParser.getSharedIntPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_AMOUNT)];

        for (int i = 0; i < tabTitles.length; i++)
            tabTitles[i] = DataParser.getSharedStringPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_NAME + i);
    }

    @Override //Initially creates the fragments here. Only called once for each fragment
    public Fragment getItem(int position) {
        Fragment fragment = new SchoolPostsFragment();
        Bundle args = new Bundle();

        args.putString(SchoolPostsFragment.ARG_CATEGORY, DataParser.getSharedStringPreference(context, DataParser.PREFS_CATEGORIES, DataParser.KEY_CATEGORY_ID + position));
        args.putInt(SchoolPostsFragment.INDEX, position);
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public int getCount() {
        //Returns the number of tabs
        return tabTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //Returns name of tabs
        return tabTitles[position];
    }

}
