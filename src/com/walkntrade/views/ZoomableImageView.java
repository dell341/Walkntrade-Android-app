package com.walkntrade.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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
    private static final int MINIMUM_X_REQUIRED_VELOCITY = 500;
    private static final int MINIMUM_Y_REQUIRED_VELOCITY = 100;

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        Drawable drawable = getDrawable();
        if (drawable == null)
            return;

        final int imageWidth = drawable.getIntrinsicWidth();
        final int imageHeight = drawable.getIntrinsicHeight();

        final int viewWidth = getMeasuredWidth();
        final int viewHeight = getMeasuredHeight();

        RectF source = new RectF(0, 0, imageWidth, imageHeight);
        RectF view = new RectF(0, 0, viewWidth, viewHeight);
        identityMatrix.setRectToRect(source, view, Matrix.ScaleToFit.CENTER);
        setImageMatrix(identityMatrix);
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

            minScaleEnabled = true;
        }

        super.setImageMatrix(matrix);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            scaleGestureDetector.onTouchEvent(event); //Allow scaling concurrently with panning
            getParent().requestDisallowInterceptTouchEvent(true);
            return true;
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return scaleGestureDetector.onTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        Log.v(TAG, "hasFocus: " + gainFocus + ". Focus went to : " + direction);
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    }

    private void addTouchListeners() {
        scaleGestureDetector = new ScaleGestureDetector(context, new CustomScaleGestureListener());
        gestureDetector = new GestureDetector(context, new CustomGestureListener());
    }

    @Override
    public OnFocusChangeListener getOnFocusChangeListener() {
        return super.getOnFocusChangeListener();
    }

    private void scaleImage(float scaleFactor, float xFocal, float yFocal) {
        identityMatrix.postScale(scaleFactor, scaleFactor, xFocal, yFocal);
        setImageMatrix(identityMatrix);

        if (zoomLevel == ZOOM_MAX_SCALE && !maxScaleEnabled) {
            float[] m = new float[9];
            identityMatrix.getValues(m);
            maxXScale = m[Matrix.MSCALE_X];
            maxYScale = m[Matrix.MSCALE_Y];

            maxScaleEnabled = true;
        }

    }

    private void translateImage(float xDistance, float yDistance) {
        identityMatrix.postTranslate(xDistance, yDistance);
        setImageMatrix(identityMatrix);

        //float[] m = new float[9];
        //identityMatrix.getValues(m);

       //Log.v(TAG, "transX: "+m[Matrix.MTRANS_X]+" transY: "+m[Matrix.MTRANS_Y]);
    }

    public boolean imageAtLeftEdge() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];

        return transX == 0;
    }

    public boolean imageAtRightEdge() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float scaledWidth = getDrawable().getIntrinsicWidth() * m[Matrix.MSCALE_X];

        return transX == -(scaledWidth - getMeasuredWidth());
    }

    public boolean imageLargerThanView() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float scaledHeight = getDrawable().getIntrinsicHeight() * m[Matrix.MSCALE_Y];

        return scaledHeight > getMeasuredHeight();
    }

    public boolean imageZoomedIn() {
        return zoomLevel > ZOOM_MIN_SCALE;
    }

    private class CustomScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            int centerX = getMeasuredWidth() / 2;
            int centerY = getMeasuredHeight() / 2;

            float[] m = new float[9];
            identityMatrix.getValues(m);
            float xScale = m[Matrix.MSCALE_X];
            float yScale = m[Matrix.MSCALE_Y];

            if (scaleFactor >= 1f) { //Zooming In (Scaling Up)
                if (maxScaleEnabled) {
                    m[Matrix.MSCALE_X] = (xScale < maxXScale ? xScale : maxXScale);
                    m[Matrix.MSCALE_Y] = (yScale < maxYScale ? yScale : maxYScale);

                    if (m[Matrix.MSCALE_X] == maxXScale || m[Matrix.MTRANS_Y] == maxYScale) {
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
                if (minScaleEnabled) {
                    m[Matrix.MSCALE_X] = (xScale > minXScale ? xScale : minXScale);
                    m[Matrix.MSCALE_Y] = (yScale > minYScale ? yScale : minYScale);
                    m[Matrix.MTRANS_X] = xTrans;
                    m[Matrix.MTRANS_Y] = yTrans;

                    if (m[Matrix.MSCALE_X] == minXScale || m[Matrix.MTRANS_Y] == minYScale) {
                        identityMatrix.setValues(m);
                        setImageMatrix(identityMatrix);
                        return true;
                    }
                }
                if (zoomLevel > ZOOM_MIN_SCALE) {
                    zoomLevel *= scaleFactor;
                    zoomLevel = Math.max(zoomLevel, ZOOM_MIN_SCALE);
                }
            }

            scaleImage(scaleFactor, centerX, centerY);
            return true;
        }
    }

    private class CustomGestureListener extends GestureDetector.SimpleOnGestureListener {

//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//            if(Math.abs(velocityX) < MINIMUM_X_REQUIRED_VELOCITY && Math.abs(velocityY) < MINIMUM_Y_REQUIRED_VELOCITY)
//                return false;
//
//            float distanceX = e1.getX() - e2.getX();
//            float distanceY = e2.getY() - e2.getY();
//            Log.d(TAG, "DistanceX : "+distanceX);
//            Log.d(TAG, "DistanceY : "+distanceY);
//            float[] m = new float[9];
//            identityMatrix.getValues(m);
//            float transX = m[Matrix.MTRANS_X];
//            float transY = m[Matrix.MTRANS_Y];
//
//            Drawable drawable = getDrawable();
//            int viewWidth = getMeasuredWidth();
//            int viewHeight = getMeasuredHeight();
//            float scaledWidth = drawable.getIntrinsicWidth() * m[Matrix.MSCALE_X];
//            float scaledHeight = drawable.getIntrinsicHeight() * m[Matrix.MSCALE_Y];
//
//            if (distanceX < 0) //Panning image to the right (scrolling left to right)
//                distanceX = ((transX - distanceX) > 0 ? transX : distanceX);
//            else  //Panning image to the left (scrolling right to left)
//                distanceX = ((scaledWidth + transX - distanceX) < viewWidth ? -(viewWidth - scaledWidth - transX) : distanceX);
//
//            if (imageLargerThanView()) {
//                if (distanceY < 0)  //Panning down (scrolling top to bottom)
//                    distanceY = ((transY - distanceY) > 0 ? transY : distanceY);
//                else  //Panning up (scrolling bottom to top)
//                    distanceY = ((scaledHeight + transY - distanceY) <= viewHeight ? -(viewHeight - scaledHeight - transY) : distanceY);
//            } else
//                distanceY = 0;
//
//            translateImage(-distanceX, -distanceY); //Translate the inverse of the distance direction to follow the user's finger
//            return true;
//        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            boolean zoom = imageZoomedIn();

            /*Condition to prevent panning*/
            // 1. Image not zoomed in
            // 2. Attempting to pan right when image is already at the left edge
            // 3. Attempting to pan left when image is already at the right edge
            if (!zoom || (distanceX < 0 && imageAtLeftEdge()) || (distanceX > 0 && imageAtRightEdge()))
                return false;

            float[] m = new float[9];
            identityMatrix.getValues(m);
            float transX = m[Matrix.MTRANS_X];
            float transY = m[Matrix.MTRANS_Y];

            Drawable drawable = getDrawable();
            int viewWidth = getMeasuredWidth();
            int viewHeight = getMeasuredHeight();
            float scaledWidth = drawable.getIntrinsicWidth() * m[Matrix.MSCALE_X];
            float scaledHeight = drawable.getIntrinsicHeight() * m[Matrix.MSCALE_Y];

            if (distanceX < 0) //Panning image to the right (scrolling left to right)
                distanceX = ((transX - distanceX) > 0 ? transX : distanceX);
            else  //Panning image to the left (scrolling right to left)
                distanceX = ((scaledWidth + transX - distanceX) < viewWidth ? -(viewWidth - scaledWidth - transX) : distanceX);

            if (imageLargerThanView()) {
                if (distanceY < 0)  //Panning down (scrolling top to bottom)
                    distanceY = ((transY - distanceY) > 0 ? transY : distanceY);
                else  //Panning up (scrolling bottom to top)
                    distanceY = ((scaledHeight + transY - distanceY) <= viewHeight ? -(viewHeight - scaledHeight - transY) : distanceY);
            } else
                distanceY = 0;

            translateImage(-distanceX, -distanceY); //Translate the inverse of the distance direction to follow the user's finger
            return true;
        }
    }
}
