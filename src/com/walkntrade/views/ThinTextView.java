package com.walkntrade.views;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class ThinTextView extends TextView {

    public ThinTextView(Context context) {
        super(context);
        setLightFont();
    }

    public ThinTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLightFont();
    }

    public ThinTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLightFont();
    }

    public void setLightFont(){
        if(isInEditMode())
            return;
        Typeface font = Typeface.createFromAsset(getContext().getAssets(), "Roboto-Thin.ttf");
        setTypeface(font);
    }
}
