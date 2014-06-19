package com.walkntrade.io;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Used when getting existing image from device, or getting it's path
public class ImageTool {

    private static final String  TAG = "ImageTool";

    //Get existing photo path from the URI
    public static String getPath(Context context, Uri uri){
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if(cursor != null){
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            String filePath = cursor.getString(columnIndex);
            cursor.close();
            return filePath;
        }
        else
            return uri.getPath();
    }

    //Returns a scaled image for specified image view
    public static Bitmap getImageFromDevice(String path, ImageView imageView) {
        //Scale bitmap to ImageView, minimizing memory usage
        int targetWidth = imageView.getWidth();
        int targetHeight = imageView.getHeight();

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
        bmOptions.inPurgeable = true;

        return BitmapFactory.decodeFile(path, bmOptions);
    }
}
