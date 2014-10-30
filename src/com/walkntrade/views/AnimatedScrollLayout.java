package com.walkntrade.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//ScrollView that built for transition animations
public class AnimatedScrollLayout extends ScrollView {

    private static final String TAG = "AnimatedScrollLayout";
    private int layoutWidth;

    public AnimatedScrollLayout(Context context) {
        super(context);
    }

    public AnimatedScrollLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedScrollLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutWidth = w;
    }

    //Slide to the left 'onto' the screen or slide to the right 'off' the screen
    public void setXTranslate(float xTranslate){
        setX(layoutWidth - xTranslate * layoutWidth);
    }

    //Slide to the right 'onto' the screen or slide to the left 'off' the screen
    public void setAntiXTranslate(float antiXTranslate){
        if(layoutWidth != 0)  //This will prevent the layout from being set at the default position, when the layout width hasn't yet been determined
            setX((layoutWidth - (layoutWidth + antiXTranslate * layoutWidth)));
        else
            setX(-600);
    }
}