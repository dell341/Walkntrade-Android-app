package com.walkntrade.asynctasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.walkntrade.R;
import com.walkntrade.adapters.PostAdapter;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.objects.Post;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Retrieves the thumbnail images for posts
public class ThumbnailTask extends AsyncTask<String, Void, Bitmap> {

    private static final String TAG = "AsycnTask:Thumbnail";

    private Context context;
    private PostAdapter postAdapter;
    private Post post;

    public ThumbnailTask(Context _context, PostAdapter adapter, Post _post){
        context = _context;
        postAdapter = adapter;
        post = _post;
    }

    @Override
    protected Bitmap doInBackground(String... imgURL) {
        Bitmap bm = null;

        if(context == null) //App was closed before AsyncTask was completed
            return null;

        if(imgURL[0].equalsIgnoreCase(context.getString(R.string.default_image_url)))
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.post_image);

        String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
        String key = post.getIdentifier()+"_thumb";

        DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID + DiskLruImageCache.DIRECTORY_POST_IMAGES);

        try {
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
        if(bitmap != null)
            post.setBitmapImage(bitmap);

        postAdapter.notifyDataSetChanged();
    }
}
