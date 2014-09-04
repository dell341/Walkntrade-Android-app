package com.walkntrade;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class AnimatedScrollLayout extends ScrollView {

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

    //Slide left into screen
    public void setXTranslate(float xTranslate){
        setX(layoutWidth - xTranslate * layoutWidth);
    }

    //Slide left off screen
    public void setAntiXTranslate(float antiXTranslate){
        setX(layoutWidth - (layoutWidth + antiXTranslate* layoutWidth));
    }
}