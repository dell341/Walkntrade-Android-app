package com.walkntrade.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class LightTextView extends TextView {

    public LightTextView(Context context) {
        super(context);
        setLightFont();
    }

    public LightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLightFont();
    }

    public LightTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLightFont();
    }

    public void setLightFont(){
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Light.ttf");
        setTypeface(font);
    }
}
