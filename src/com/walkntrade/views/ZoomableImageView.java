package com.walkntrade.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
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
    private static final long FLING_ANIMATION_DURATION = 400L; //Duration of fling animation
    private static final float FLING_DISTANCE_FACTOR = 0.2f; //Reduces distance travelled from fling's velocity

    private Context context;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private Matrix identityMatrix = new Matrix();
    private DecelerateInterpolator interpolator;

    private float zoomLevel = 1f;
    private float maxXScale, maxYScale, minXScale, minYScale; //Zoom limits
    private float xTrans, yTrans; //Original translation positions
    private long startTime, endTime; //References for fling animation
    float totalAnimDx, totalAnimDy, lastAnimDx, lastAnimDy;
    private boolean matrixCentered = false;
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

        if (!matrixCentered) {
            final int imageWidth = drawable.getIntrinsicWidth();
            final int imageHeight = drawable.getIntrinsicHeight();

            final int viewWidth = getMeasuredWidth();
            final int viewHeight = getMeasuredHeight();

            centerImageMatrix(imageWidth, imageHeight, viewWidth, viewHeight);
        }
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

    //Scale image to fit exactly at the center of the screen
    private void centerImageMatrix(int imageWidth, int imageHeight, int viewWidth, int viewHeight) {
        RectF source = new RectF(0, 0, imageWidth, imageHeight);
        RectF view = new RectF(0, 0, viewWidth, viewHeight);
        identityMatrix.setRectToRect(source, view, Matrix.ScaleToFit.CENTER);
        setImageMatrix(identityMatrix);

        matrixCentered = true;
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
    }

    //Return true if image at left edge of screen
    public boolean imageAtLeftEdge() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];

        return transX == 0;
    }

    //Return true if image at right edge of screen
    public boolean imageAtRightEdge() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float scaledWidth = getDrawable().getIntrinsicWidth() * m[Matrix.MSCALE_X];

        return transX == -(scaledWidth - getMeasuredWidth());
    }

    //Return true if image left or right bounds expand past the bounds of the screen
    public boolean imageHorizontalLargerThanView() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float scaledWidth = getDrawable().getIntrinsicWidth() * m[Matrix.MSCALE_X];

        return scaledWidth > getMeasuredWidth();
    }

    //Return true if image top or bottom bounds expand past the bounds of the screen
    public boolean imageVerticalLargerThanView() {
        float[] m = new float[9];
        identityMatrix.getValues(m);
        float scaledHeight = getDrawable().getIntrinsicHeight() * m[Matrix.MSCALE_Y];

        return scaledHeight > getMeasuredHeight();
    }

    //Return true if image is scaled up
    public boolean imageZoomedIn() {
        return zoomLevel > ZOOM_MIN_SCALE;
    }

    /*Animation Code implemented from question on StackOverflow. Very Helpful, exactly what was on my mind.*/
    /*I'm so happy I'm giving credit to user: Hank.*/

    //Initialize animation used for fling
    public void initAnimation(float dx, float dy, long duration) {
        interpolator = new DecelerateInterpolator();
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;
        totalAnimDx = dx;
        totalAnimDy = dy;
        lastAnimDx = 0;
        lastAnimDy = 0;

        post(new Runnable() {
            @Override
            public void run() {
                onAnimateStep();
            }
        });

    }

    private void onAnimateStep() {
        long currentTime = System.currentTimeMillis();
        float percentTime = (float) (currentTime - startTime) / (float) (endTime - startTime); //Elapsed time as percentage of total time. (elapsed time)/(duration
        float percentDistance = interpolator.getInterpolation(percentTime); //Interpolation returns percentage of distance traveled in relation to amount of time passed.
        float curDx = percentDistance * totalAnimDx;
        float curDy = percentDistance * totalAnimDy;

        float diffCurDx = curDx - lastAnimDx; //Current x distance to move
        float diffCurDy = curDy - lastAnimDy; //Current y distance to move
        lastAnimDx = curDx;
        lastAnimDy = curDy;

        translateImage(diffCurDx, diffCurDy);

        if (percentTime < 1.0f) //As long as duration has not been reached, continue animating translation
            post(new Runnable() {
                @Override
                public void run() {
                    onAnimateStep();
                }
            });
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

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            /*Conditions to prevent panning by flinging*/
            // 1. X or Y velocity is too small
            // 2. Image is normally scaled (nothing to pan, full image is displayed)
            // 3a. Flinging from left to right, when image is already at the left edge
            // 3b. Flinging from right to left, when image is already at the right edge
            if (Math.abs(velocityX) < MINIMUM_X_REQUIRED_VELOCITY && Math.abs(velocityY) < MINIMUM_Y_REQUIRED_VELOCITY || !imageZoomedIn() || ( (velocityX < 0 && imageAtLeftEdge()) || (velocityX > 0 && imageAtRightEdge())))
                return false;

            float distanceX = FLING_DISTANCE_FACTOR * velocityX;
            float distanceY = FLING_DISTANCE_FACTOR * velocityY;

            float[] m = new float[9];
            identityMatrix.getValues(m);
            float transX = m[Matrix.MTRANS_X];
            float transY = m[Matrix.MTRANS_Y];

            Drawable drawable = getDrawable();
            int viewWidth = getMeasuredWidth();
            int viewHeight = getMeasuredHeight();
            float scaledWidth = drawable.getIntrinsicWidth() * m[Matrix.MSCALE_X];
            float scaledHeight = drawable.getIntrinsicHeight() * m[Matrix.MSCALE_Y];

            if(imageHorizontalLargerThanView()) {
                if (distanceX > 0) //Panning image to the right (scrolling left to right)
                    distanceX = ((transX + distanceX) > 0 ? 0 - transX : distanceX);
                else  //Panning image to the left (scrolling right to left)
                    distanceX = ((scaledWidth + transX + distanceX) < viewWidth ? viewWidth - scaledWidth - transX : distanceX);
            } else
                distanceX = 0;

            if (imageVerticalLargerThanView()) {
                if (distanceY > 0)  //Panning down (scrolling top to bottom)
                    distanceY = ((transY + distanceY) > 0 ? 0-transY : distanceY);
                else  //Panning up (scrolling bottom to top)
                    distanceY = ((scaledHeight + transY + distanceY) <= viewHeight ? viewHeight - scaledHeight - transY : distanceY);
            } else
                distanceY = 0;

            initAnimation(distanceX, distanceY, FLING_ANIMATION_DURATION);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            /*Condition to prevent panning by scrolling*/
            // 1. Image not zoomed in
            // 2. Attempting to pan right when image is already at the left edge
            // 3. Attempting to pan left when image is already at the right edge
            if (!imageZoomedIn() || (distanceX < 0 && imageAtLeftEdge()) || (distanceX > 0 && imageAtRightEdge()))
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

            if(imageHorizontalLargerThanView()) {
                if (distanceX < 0) //Panning image to the right (scrolling left to right)
                    distanceX = ((transX - distanceX) > 0 ? transX : distanceX);
                else  //Panning image to the left (scrolling right to left)
                    distanceX = ((scaledWidth + transX - distanceX) < viewWidth ? -(viewWidth - scaledWidth - transX) : distanceX);
            } else
                distanceX = 0;
            if (imageVerticalLargerThanView()) {
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
