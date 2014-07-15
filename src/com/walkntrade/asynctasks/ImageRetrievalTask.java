package com.walkntrade.asynctasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;

import java.io.IOException;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Asynchronous Task that retrieves the post image in the background.
public class ImageRetrievalTask extends AsyncTask<String, Void, Bitmap> {

    private final String TAG = "ASYNCTASK:ImageRetrieval";
    private Context context;
    private ImageView imgView;
    private String identifier;
    private int index;
    private DiskLruImageCache imageCache;

    public ImageRetrievalTask(Context _context, ImageView _imgView, String _identifier, int _index){
        context = _context;
        imgView = _imgView;
        identifier = _identifier;
        index = _index;
    }

    @Override
    protected Bitmap doInBackground(String... imgURL) {
        Bitmap bm = null;
        try {
            String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
            String key = identifier+"_"+index;

            imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.IMAGE_DIRECTORY);
            bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

            if(bm == null) //If it doesn't exists, retrieve image from network
                bm = DataParser.loadBitmap(imgURL[0]);

            imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
        } catch (IOException e) {
            Log.e(TAG, "Retrieving image", e);
        }
        finally{
            imageCache.close();
        }

        return bm;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if(bitmap != null) {
            imgView.setVisibility(View.VISIBLE);
            imgView.setImageBitmap(bitmap);
        }
    }
}
