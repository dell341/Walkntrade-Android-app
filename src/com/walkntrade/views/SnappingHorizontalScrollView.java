package com.walkntrade.views;

import android.content.Context;
import android.support.annotation.NonNull;
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

    private Context context;
    private GestureDetector gestureDetector;
    private ArrayList<View> items;
    private int index = 0;

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
        gestureDetector = new GestureDetector(context, new CustomGestureListener());
        setOnTouchListener(this);
    }

    //Required to allow fling. Keeps track of current item
    public void addItems(ArrayList<View> items) {
        this.items = items;
    }

    /* Order of touch events. Child events are called first, and then parent events. Last In First Out*/
    //dispatchTouchEvent( --> onInterceptTouchEvent)
    //onTouch
    //onTouchEvent

    @Override //Intercept touch events before it is given to child
    public boolean onInterceptTouchEvent(@NonNull MotionEvent motionEvent) {
        //If gesture was a swipe, don't send motion event to child view
        return gestureDetector.onTouchEvent(motionEvent) || super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        if (gestureDetector.onTouchEvent(motionEvent)) //If gesture was handled by class below. Just return. Else continue processing action.
            return true;
        else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            int scrollX = getScrollX(); //New scrolled position of this View
            int width = view.getMeasuredWidth(); //Width of selected view
            index = (scrollX + (width / 2)) / width; //Uses int rounding to find out if new scroll position is more than half of current view.
            int scrollTo = index * width; //Position of specified index

            smoothScrollTo(scrollTo, 0);
            return true;
        }
        return false;
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
