package com.walkntrade.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.walkntrade.R;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class Fragment_Contact_Prefs extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_prefs, container, false);

        //TODO: SharedPreferences settings for contact preferences
        return rootView;
    }
}
