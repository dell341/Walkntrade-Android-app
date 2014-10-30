package com.walkntrade.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import java.util.ArrayList;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class SnappingHorizontalScrollView extends HorizontalScrollView implements View.OnTouchListener {

    private static final String TAG = "SnappingHorizontalScrollView";
    private static final int MINIMUM_SWIPE_DISTANCE = 5;
    private static final int MINIMUM_REQUIRED_VELOCITY = 300;
    private static final int SCROLL_THRESHOLD = 10; //Pixel distance defined as a move

    private Context context;
    private GestureDetector gestureDetector;
    private ArrayList<View> items;
    private int index = 0;
    private boolean isOnClick;
    private float downX, downY;

    public SnappingHorizontalScrollView(Context context) {
        super(context);
        this.context = context;
        addTouchListeners();
    }

    public SnappingHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        addTouchListeners();
    }

    public SnappingHorizontalScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        addTouchListeners();
    }

    private void addTouchListeners() {
        gestureDetector = new GestureDetector(context, new CustomGestureDetector());
        setOnTouchListener(this);
    }

    //Required to allow fling. Keeps track of current item
    public void addItems(ArrayList<View> items) {
        this.items = items;
    }

    @Override //Intercept touch events before it is given to child
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        boolean returning = this.onTouchEvent(motionEvent);
        Log.v(TAG, "onIntercept: "+returning);
        return returning;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        switch(motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_UP:
                Log.i(TAG, "onTouchEvent - Action Up. isOnClick: " + isOnClick);
                if (isOnClick)
                    return false; //Return false is click is detected, so child view can handle the event. Else continue
                break;
            case MotionEvent.ACTION_DOWN:
                isOnClick = true; //Begin click
                downX = motionEvent.getX();
                downY = motionEvent.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                //If user is on down click, but has moved more than 10 pixels. Click is invalidated
                if(isOnClick && (Math.abs(downX - motionEvent.getX()) > SCROLL_THRESHOLD || Math.abs(downY - motionEvent.getY()) > SCROLL_THRESHOLD))
                    isOnClick = false;
                break;
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
            Log.i(TAG, "onTouch - Action Up. isOnClick: " + isOnClick);
            if (isOnClick)
                return false; //Return false is click is detected, so child view can handle the event. Else continue
        }
        if (gestureDetector.onTouchEvent(motionEvent)) //If gesture was handled by class below. Just return. Else continue processing action.
            return true;
        else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            Log.d(TAG, "On Touch: "+(motionEvent.getAction() == MotionEvent.ACTION_UP ? "Action Up" : "Action Cancel"));
            int scrollX = getScrollX(); //New scrolled position of this View
            int width = view.getMeasuredWidth(); //Width of selected view
            index = (scrollX + (width / 2)) / width; //Uses int rounding to find out if new scroll position is more than half of current view.
            int scrollTo = index * width; //Position of specified index

            Log.d(TAG, "ScrollX: "+scrollX+" | Width: "+width+" | index: "+index+" | scrollTo: "+scrollTo);
            smoothScrollTo(scrollTo, 0);
            return true;
        }
        return false;
    }

    private class CustomGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "Flinging");
            try {
                int width = getMeasuredWidth();

                if (e1.getX() - e2.getX() > MINIMUM_SWIPE_DISTANCE && Math.abs(velocityX) > MINIMUM_REQUIRED_VELOCITY) { //Swipe from right to left
                    index = (index <= items.size() ? ++index : items.size() - 1); //onFling, increment index if current index is not the last item. Else set index to last item
                    int scrollTo = index * width;
                    smoothScrollTo(scrollTo, 0);

                    return true;
                } else if (e2.getX() - e1.getX() > MINIMUM_SWIPE_DISTANCE && Math.abs(velocityX) > MINIMUM_REQUIRED_VELOCITY) { //Swipe from left to right
                    index = (index > 0 ? --index : 0); //onFling, decrement index if current index is not the first item. Else set index to first item
                    int scrollTo = index * width;
                    smoothScrollTo(scrollTo, 0);

                    return true;
                }

            } catch (NullPointerException e) {
                Log.e(TAG, "Need to add items with addItems(ArrayList<View>) or some other null pointer exception", e);
            } catch (Exception e) {
                Log.e(TAG, "Flinging View", e);
            }
            return false;
        }
    }
}
