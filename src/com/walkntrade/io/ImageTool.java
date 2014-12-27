package com.walkntrade.io;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileNotFoundException;
import java.io.InputStream;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Used when getting existing image from device, sd card, or Document Provider
public class ImageTool {

    private static final String TAG = "ImageTool";

    //Returns a scaled image for specified image view
    public static Bitmap getImageFromDevice(String path, int targetWidth, int targetHeight) {
        //Scale bitmap to ImageView, minimizing memory usage

        //Get dimensions of actual image
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoWidth = bmOptions.outWidth;
        int photoHeight = bmOptions.outHeight;

        //Scale image
        int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeFile(path, bmOptions);
    }

    public static Bitmap getImageFromDevice(Context context, Uri returnedUri, int targetWidth, int targetHeight) throws FileNotFoundException {
        InputStream inStream = context.getContentResolver().openInputStream(returnedUri);

        //Get dimensions of actual image
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inStream, null, bmOptions);
        int photoWidth = bmOptions.outWidth;
        int photoHeight = bmOptions.outHeight;

        //Scale image
        int scaleFactor = Math.min(photoWidth / targetWidth, photoHeight / targetHeight);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        inStream = context.getContentResolver().openInputStream(returnedUri);

        return BitmapFactory.decodeStream(inStream, null, bmOptions);
    }
}
