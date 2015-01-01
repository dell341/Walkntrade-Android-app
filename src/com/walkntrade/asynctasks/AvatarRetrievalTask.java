package com.walkntrade.asynctasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.adapters.DrawerAdapter;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Asynchronous Task, received user avatar image
public class AvatarRetrievalTask extends AsyncTask<Void, Void, Bitmap> {

    private final String TAG = "ASYNCTASK:AvatarRetrieval";
    private Context context;
    private ListView drawerList;

    //Retrieves the user's avatar for the NavigationDrawer
    public AvatarRetrievalTask(Context _context, ListView _drawerList) {
        context = _context;
        drawerList = _drawerList;
    }

    @Override
    protected Bitmap doInBackground(Void... arg0) {
        DataParser database = new DataParser(context);
        Bitmap bm = null;
        DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
        try {
            String avatarURL;

            DataParser.StringResult result = database.getAvatarUrl();
            avatarURL = result.getValue();

            if (avatarURL == null)
                return null;

            String splitURL[] = avatarURL.split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

            if(bm == null) //If it doesn't exists, retrieve image from network
                bm = DataParser.loadBitmap(avatarURL);

            imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
        }
        catch(IOException e) {
            Log.e(TAG, "Retrieving user avatar", e);
        }
        catch(ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Image does not exist", e);
            //If user has not uploaded an image, leave Bitmap as null
        }
        finally {
            imageCache.close();
        }
        return bm;
    }

    @Override
    protected void onPostExecute(Bitmap avatar) {
        DrawerAdapter adapter = (DrawerAdapter) drawerList.getAdapter();
        DrawerItem item = adapter.getItem(0); //Get user header item

        if(avatar != null)
            item.setAvatar(avatar);

        adapter.notifyDataSetChanged();
    }
}
