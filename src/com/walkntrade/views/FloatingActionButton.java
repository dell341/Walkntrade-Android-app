package com.walkntrade.views;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.walkntrade.R;

public class FloatingActionButton extends View {

    private int buttonColor;
    private int iconColor;
    private boolean isMiniButton;
    private Drawable buttonIcon;

    private int defaultButtonColor = R.color.green_dark;
    private int defaultIconColor = R.color.white;

    private Paint buttonPaint;
    private float buttonDiameter;
    private float buttonRadius;
    private float buttonPadding;

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(attrs);
    }

    public FloatingActionButton(Context context) {
        super(context);

        init(null);
    }

    private void init(AttributeSet attrs) {
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.FloatingActionButton, 0, 0);

        try {
            buttonColor = typedArray.getColor(R.styleable.FloatingActionButton_buttonColor, defaultButtonColor);
            iconColor = typedArray.getColor(R.styleable.FloatingActionButton_iconColor, defaultIconColor);
            isMiniButton = typedArray.getBoolean(R.styleable.FloatingActionButton_miniButton, false);
            buttonIcon = typedArray.getDrawable(R.styleable.FloatingActionButton_buttonIcon);
        } finally {
            typedArray.recycle();
        }

        if(isMiniButton)
            setToMiniButton();
        else
            setToDefaultButton();

        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setStyle(Paint.Style.FILL);
        buttonPaint.setColor(buttonColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension( (int) buttonDiameter, (int) buttonDiameter);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int centerX = getWidth()/2;
        int centerY = getHeight()/2;


        canvas.drawCircle(centerX, centerY, buttonRadius, buttonPaint);

        int padding = (int) buttonPadding;

        int contentWidth = getWidth() - padding - padding;
        int contentHeight = getHeight() - padding - padding;

        buttonIcon.setBounds(padding, padding, padding + contentWidth, padding + contentHeight);
        buttonIcon.draw(canvas);
    }

    //Any time a change is made that may change how the view looks, call this method
    private void invalidateAndRequest() {
        invalidate();
        requestLayout();
    }

    private void setToMiniButton() {
        buttonDiameter = getResources().getDimension(R.dimen.mini_floating_action_button_diameter);
        buttonPadding = getResources().getDimension(R.dimen.mini_floating_action_button_padding);
        buttonRadius = buttonDiameter/2;
    }

    private void setToDefaultButton() {
        buttonDiameter = getResources().getDimension(R.dimen.floating_action_button_diameter);
        buttonPadding = getResources().getDimension(R.dimen.floating_action_button_padding);
        buttonRadius = buttonDiameter/2;
    }

    public int getButtonColor() {
        return buttonColor;
    }

    public void setButtonColor(int buttonColor) {
        this.buttonColor = buttonColor;
        invalidateAndRequest();
    }

    public int getIconColor() {
        return iconColor;
    }

    public void setIconColor(int iconColor) {
        this.iconColor = iconColor;
        invalidateAndRequest();
    }

    public boolean isMiniButton() {
        return isMiniButton;
    }

    public void setMiniButton(boolean isMini) {
        this.isMiniButton = isMini;

        if(isMini)
            setToMiniButton();
        else
            setToDefaultButton();

        invalidateAndRequest();
    }

    public Drawable getButtonIcon() {
        return buttonIcon;
    }

    public void setButtonIcon(Drawable icon) {
        this.buttonIcon = icon;
        invalidateAndRequest();
    }
}
