package com.walkntrade.gcm;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

//Initializes trackers used by Google Analytics
public class Analytics extends Application {

    private static final String PROPERTY_ID = "UA-57595854-1";
    private static int GENERAL_TRACKER = 0;

    HashMap<TrackerName, Tracker> trackers = new HashMap<TrackerName, Tracker>();

    public enum TrackerName {
        APP_TRACKER //Tracker used throughout application
    }

    //TrackerName parameter is irrelevant for now. But when more tracker names are added, it could be useful.
    public synchronized Tracker getTracker(TrackerName trackerId) {
        if(!trackers.containsKey(TrackerName.APP_TRACKER)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(PROPERTY_ID);
            t.enableExceptionReporting(true);
            t.enableAutoActivityTracking(true);
            trackers.put(TrackerName.APP_TRACKER, t);
        }
        return trackers.get(TrackerName.APP_TRACKER);
    }
}
