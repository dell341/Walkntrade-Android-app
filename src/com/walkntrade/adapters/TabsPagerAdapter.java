package com.walkntrade.adapters;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;

import com.walkntrade.R;
import com.walkntrade.fragments.Fragment_SchoolPage;

//Creates category-specific fragments
public class TabsPagerAdapter extends FragmentPagerAdapter {
	private Context context;

	private String[] tabTitles;

	public TabsPagerAdapter(FragmentManager fm, Context _context) {
		super(fm);
		context = _context;
		//String titles pulled from resources to allow easy language translation
		tabTitles = new String[] {context.getString(R.string.category_all) , context.getString(R.string.category_book),
                context.getString(R.string.category_tech), context.getString(R.string.category_service), context.getString(R.string.category_misc)};
	}

    @Override //Initially creates the fragments here. Only called once for each fragment
    public Fragment getItem(int position) {
        Fragment fragment = new Fragment_SchoolPage();
        Bundle args = new Bundle();

        switch(position){
            case 0:
                args.putString(Fragment_SchoolPage.ARG_CATEGORY, context.getString(R.string.server_category_all));
                args.putInt(Fragment_SchoolPage.INDEX, 0);
                fragment.setArguments(args);
                return fragment;
            case 1:
                args.putString(Fragment_SchoolPage.ARG_CATEGORY, context.getString(R.string.server_category_book));
                args.putInt(Fragment_SchoolPage.INDEX, 1);
                fragment.setArguments(args);
                return fragment;
            case 2:
                args.putString(Fragment_SchoolPage.ARG_CATEGORY, context.getString(R.string.server_category_tech));
                args.putInt(Fragment_SchoolPage.INDEX, 2);
                fragment.setArguments(args);
                return fragment;
            case 3:
                args.putString(Fragment_SchoolPage.ARG_CATEGORY, context.getString(R.string.server_category_service));
                args.putInt(Fragment_SchoolPage.INDEX, 3);
                fragment.setArguments(args);
                return fragment;
            default:
                args.putString(Fragment_SchoolPage.ARG_CATEGORY, context.getString(R.string.server_category_misc));
                args.putInt(Fragment_SchoolPage.INDEX, 4);
                fragment.setArguments(args);
                return fragment;
        }
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
