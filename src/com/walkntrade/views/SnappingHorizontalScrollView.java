package com.walkntrade.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import java.util.List;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class SnappingHorizontalScrollView extends HorizontalScrollView implements View.OnTouchListener {

    private static final String TAG = "SnappingHorizontalScrollView";
    private static final int MINIMUM_SWIPE_DISTANCE = 5;
    private static final int MINIMUM_REQUIRED_VELOCITY = 300;

    private Context context;
    private GestureDetector gestureDetector;
    private List<View> items;
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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void addTouchListeners() {
        gestureDetector = new GestureDetector(context, new CustomGestureListener());
        setOnTouchListener(this);
    }

    //Required to allow fling. Keeps track of current item
    public void addItems(List<View> items) {
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

            ViewGroup childView = (ViewGroup) getChildAt(0); //Use this view, it has the actual width of the scrolling content
            View viewContent = childView.getChildAt(0); //Gets the first view (should be ImageView) in this ScrollView. And uses that width to determine scrolling.
            /*This will only work properly if all 'view contents' are the same size*/
            int scrollX = getScrollX(); //New scrolled position of this View
            int width = viewContent.getMeasuredWidth(); //Width of selected view (In this example, they are all the same size)

            index = (scrollX + (width / 2)) / width; //Uses int rounding to find out if new scroll position is more than half of current view.
            int scrollTo = (index * width)+getPaddingLeft(); //Position of specified index

            smoothScrollTo(scrollTo, 0);
            return true;
        }
        return false;
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {

                ViewGroup childView = (ViewGroup) getChildAt(0); //Use this view, it has the actual width of the scrolling content
                View viewContent = childView.getChildAt(0);
                int width = viewContent.getMeasuredWidth(); //Width of selected view (In this example, they are all the same size)

                if (e1.getX() - e2.getX() > MINIMUM_SWIPE_DISTANCE && Math.abs(velocityX) > MINIMUM_REQUIRED_VELOCITY) { //Swipe from right to left
                    index = (index <= items.size() ? ++index : items.size() - 1); //onFling, increment index if current index is not the last item. Else set index to last item
                    int scrollTo = (index * width)+getPaddingLeft();
                    smoothScrollTo(scrollTo, 0);
                    return true;
                } else if (e2.getX() - e1.getX() > MINIMUM_SWIPE_DISTANCE && Math.abs(velocityX) > MINIMUM_REQUIRED_VELOCITY) { //Swipe from left to right
                    index = (index > 0 ? --index : 0); //onFling, decrement index if current index is not the first item. Else set index to first item
                    int scrollTo = (index * width)+getPaddingLeft();
                    smoothScrollTo(scrollTo, 0);
                    return true;
                }

            } catch (NullPointerException e) {
                Log.e(TAG, "Must call addItems(List<View>)", e);
            } catch (Exception e) {
                Log.e(TAG, "Flinging View", e);
            }
            return false;
        }
    }
}
