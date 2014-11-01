package com.walkntrade.views;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class ZoomableImageView extends ImageView {
    private static final String TAG = "ZoomableImageView";

    private Context context;
    private ScaleGestureDetector gestureDetector;
    private Matrix identityMatrix = new Matrix();

    private float zoomLevel = 1f;

    public ZoomableImageView(Context context) {
        super(context);
        this.context = context;
        addTouchListener();
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        addTouchListener();
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        addTouchListener();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        identityMatrix.set(matrix);
        super.setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void addTouchListener() {
        gestureDetector = new ScaleGestureDetector(context, new CustomGestureListener());
    }

    private void scaleImage(float scaleFactor, float xFocal, float yFocal) {
        identityMatrix.postScale(scaleFactor, scaleFactor, xFocal, yFocal);
        setImageMatrix(identityMatrix);
    }

    private class CustomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            //setScaleType(ScaleType.MATRIX);
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float xFocal = detector.getFocusX();
            float yFocal = detector.getFocusY();

            if (scaleFactor >= 1f && zoomLevel < 3f) { //Zooming in
                zoomLevel *= scaleFactor;
                zoomLevel = Math.min(zoomLevel, 3f);
            } else if (scaleFactor <1f && zoomLevel > 1f) { //Zooming out
                zoomLevel *= scaleFactor;
                zoomLevel = Math.max(zoomLevel, 1f);
            }

            //Log.d(TAG, "Focal: ("+xFocal+","+yFocal+")");

            scaleFactor = (zoomLevel != 1f && zoomLevel != 3f ? scaleFactor : 1.0f);

            scaleImage(scaleFactor, xFocal, yFocal);
            return true;
        }
    }
}
