package com.walkntrade.views;

import android.content.Context;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class ZoomableImageView extends ImageView {
    private static final String TAG = "ZoomableImageView";
    private static final float ZOOM_MAX_SCALE = 3f; //3x times the size of the image
    private static final float ZOOM_MIN_SCALE = 1f; //Full image size

    private Context context;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Matrix identityMatrix = new Matrix();

    private float zoomLevel = 1f;
    private float maxXScale, maxYScale, minXScale, minYScale; //Zoom limits
    private float xTrans, yTrans; //Original translation positions
    private boolean minScaleEnabled = false;
    private boolean maxScaleEnabled = false;

    public ZoomableImageView(Context context) {
        super(context);
        this.context = context;
        addTouchListeners();
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        addTouchListeners();
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        addTouchListeners();
    }

    @Override
    public void setImageMatrix(Matrix matrix) {
        identityMatrix.set(matrix);

        if (!minScaleEnabled) {
            float[] m = new float[9];
            identityMatrix.getValues(m);
            minXScale = m[Matrix.MSCALE_X];
            minYScale = m[Matrix.MSCALE_Y];
            xTrans = m[Matrix.MTRANS_X];
            yTrans = m[Matrix.MTRANS_Y];

            Log.w(TAG, "MIN: xScale: " + minXScale + " | yScale: " + minYScale+" | xTrans: "+xTrans+" | xTransY: "+yTrans);
            minScaleEnabled = true;
        }

        super.setImageMatrix(matrix);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        //gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return scaleGestureDetector.onTouchEvent(event);
    }

    private void addTouchListeners() {
        scaleGestureDetector = new ScaleGestureDetector(context, new CustomScaleGestureListener());
        gestureDetector = new GestureDetector(context, new CustomGestureListener());
    }

    private void scaleImage(float scaleFactor, float xFocal, float yFocal) {
        identityMatrix.postScale(scaleFactor, scaleFactor, xFocal, yFocal);
        setImageMatrix(identityMatrix);

        if (zoomLevel == ZOOM_MAX_SCALE && !maxScaleEnabled) {
            float[] m = new float[9];
            identityMatrix.getValues(m);
            maxXScale = m[Matrix.MSCALE_X];
            maxYScale = m[Matrix.MSCALE_Y];
            float transX = m[Matrix.MTRANS_X];
            float transY = m[Matrix.MTRANS_Y];

            Log.w(TAG, "MAX: xScale: " + maxXScale + " | yScale: " + maxYScale+" | xTrans: "+transX+" | xTransY: "+transY);

            maxScaleEnabled = true;
        }
    }

    private void translateImage(float xDistance, float yDistance) {
        identityMatrix.postTranslate(xDistance, yDistance);
        setImageMatrix(identityMatrix);
    }

    public boolean imageZoomedIn() {
        return zoomLevel > ZOOM_MIN_SCALE;
    }

    private class CustomScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float xFocal = detector.getFocusX();
            float yFocal = detector.getFocusY();
            int centerX = getMeasuredWidth() / 2;
            int centerY = getMeasuredHeight() / 2;

            float[] m = new float[9];
            identityMatrix.getValues(m);
            float xScale = m[Matrix.MSCALE_X];
            float yScale = m[Matrix.MSCALE_Y];

            if (scaleFactor >= 1f) { //Zooming In (Scaling Up)
                if(maxScaleEnabled) {
                    m[Matrix.MSCALE_X] = (xScale < maxXScale ? xScale : maxXScale);
                    m[Matrix.MSCALE_Y] = (yScale < maxYScale ? yScale : maxYScale);

                    if(m[Matrix.MSCALE_X] == maxXScale || m[Matrix.MTRANS_Y] == maxYScale) {
                        Log.e(TAG, "Zoom In Max Reached: x = "+ m[Matrix.MSCALE_X]+" | y = "+m[Matrix.MSCALE_Y]+" | xTrans: "+m[Matrix.MTRANS_X]+" | xTransY: "+m[Matrix.MTRANS_Y]);
                        identityMatrix.setValues(m);
                        setImageMatrix(identityMatrix);
                        return true;
                    }
                }

                if (zoomLevel < ZOOM_MAX_SCALE) {
                    zoomLevel *= scaleFactor;
                    zoomLevel = Math.min(zoomLevel, ZOOM_MAX_SCALE);
                }
            } else if (scaleFactor < 1f) { //Zooming Out (Scaling down)
                if(minScaleEnabled) {
                    m[Matrix.MSCALE_X] = (xScale > minXScale ? xScale : minXScale);
                    m[Matrix.MSCALE_Y] = (yScale > minYScale ? yScale : minYScale);
                    m[Matrix.MTRANS_X] = xTrans;
                    m[Matrix.MTRANS_Y] = yTrans;

                    if(m[Matrix.MSCALE_X] == minXScale || m[Matrix.MTRANS_Y] == minYScale) {
                        Log.e(TAG, "Zoom Out Max Reached: x = "+ m[Matrix.MSCALE_X]+" | y = "+m[Matrix.MSCALE_Y]+" | xTrans: "+m[Matrix.MTRANS_X]+" | xTransY: "+m[Matrix.MTRANS_Y]);
                        identityMatrix.setValues(m);
                        setImageMatrix(identityMatrix);
                        return true;
                    }
                }
                if(zoomLevel > ZOOM_MIN_SCALE) {
                    zoomLevel *= scaleFactor;
                    zoomLevel = Math.max(zoomLevel, ZOOM_MIN_SCALE);
                }
            }

            //Log.d(TAG, "Center: (" + centerX + "," + centerY + ")");
            Log.d(TAG, "Zoom Level: " + zoomLevel);
            Log.i(TAG, "xScale: " + xScale + " | yScale: " + yScale);

            //scaleFactor = (zoomLevel != ZOOM_MIN_SCALE && zoomLevel != ZOOM_MAX_SCALE ? scaleFactor : 1.0f); //If the zoom limits have been reached, keep scale factor as 1. (unchanged)
            scaleImage(scaleFactor, centerX, centerY);

            return true;
        }
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean zoom = imageZoomedIn();
            Log.v(TAG, "Zoom ? " + zoom);

            if (!zoom)
                return false;

            float translateX = (e2.getX() - e1.getX()) * .10f;
            float translateY = (e2.getY() - e1.getY()) * .10f;

            Log.i(TAG, "TranslateX: " + translateX + ". TranslateY: " + translateY);
            Log.v(TAG, "DistanceX: " + distanceX + ". DistanceY: " + distanceY);
            translateImage(translateX, translateY);
            return true;
        }
    }
}
