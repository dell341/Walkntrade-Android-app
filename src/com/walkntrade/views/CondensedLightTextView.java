package com.walkntrade.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class CondensedLightTextView extends LightTextView {
    public CondensedLightTextView(Context context) {
        super(context);
        setLightFont();
    }

    public CondensedLightTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLightFont();
    }

    public CondensedLightTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLightFont();
    }

    public void setLightFont(){
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "RobotoCondensed-Light.ttf");
        setTypeface(font);
    }
}
