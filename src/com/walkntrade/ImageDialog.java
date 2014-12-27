package com.walkntrade;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.walkntrade.fragments.PostFragment;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;

import java.io.IOException;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Swipes through images of post
public class ImageDialog extends Activity {

    private static final String TAG = "ImageDialog";
    private static final String SAVED_IMAGE = "saved_instance_image";
    private static final String IMAGE_INDEX = "Index_of_image";
    private static final String IMAGE_URL = "Url_of_image";

    private static Context context;
    private static String identifier;
    private String[] imgURLs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_image_dialog);

        context = getApplicationContext();
		imgURLs = getIntent().getStringArrayExtra(PostFragment.IMGSRC);
		identifier = getIntent().getStringExtra(PostFragment.IDENTIFIER);
        int index = getIntent().getIntExtra(PostFragment.INDEX, 0);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        ImageFragmentAdapter adapter = new ImageFragmentAdapter(getFragmentManager());
        viewPager.setPageMargin(10);
        viewPager.setAdapter(adapter);

        viewPager.setCurrentItem(index);
	}

    private class ImageFragmentAdapter extends FragmentPagerAdapter{

        public ImageFragmentAdapter(FragmentManager fm){
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = new ImageFragment();
            Bundle args = new Bundle();

            if(imgURLs[position] == null) //If there is no image at that location. Return null.
                return null;

            args.putInt(IMAGE_INDEX, position);
            args.putString(IMAGE_URL, imgURLs[position]);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public int getCount() {
            return imgURLs.length;
        }
    }

    public static class ImageFragment extends Fragment{
        private ImageView imageView;
        private ImageRetrievalTask imageRetrievalTask;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_image, container, false);

            imageView = (ImageView) rootView.findViewById(R.id.full_post_image);
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
            Bundle args = getArguments();
            int index = args.getInt(IMAGE_INDEX);
            String url = args.getString(IMAGE_URL);

            if(savedInstanceState != null) {
                Bitmap bitmap = savedInstanceState.getParcelable(SAVED_IMAGE);
                imageView.setImageBitmap(bitmap);

                if(bitmap == null) {
                    imageRetrievalTask = new ImageRetrievalTask(index, imageView, progressBar);
                    imageRetrievalTask.execute(url);
                }

            }
            else if(DataParser.isNetworkAvailable(context)) {
                //Calls single image to be displayed in pop-up
                imageRetrievalTask = new ImageRetrievalTask(index, imageView, progressBar);
                imageRetrievalTask.execute(url);
            }

            return rootView;
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            try {
                outState.putParcelable(SAVED_IMAGE, ((BitmapDrawable) imageView.getDrawable()).getBitmap());
            } catch (NullPointerException e){
                Log.e(TAG, "Orientation Change before image downloaded");
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            imageRetrievalTask.cancel(true);
        }
    }

    private static class ImageRetrievalTask extends AsyncTask<String, Void, Bitmap>{

        private int index;
        private ImageView imageView;
        private ProgressBar progressBar;

        public ImageRetrievalTask(int index, ImageView imageView, ProgressBar progressBar){
            this.index = index;
            this.imageView = imageView;
            this.progressBar = progressBar;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
            DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.DIRECTORY_POST_IMAGES);

            Bitmap bm = null;

            if(isCancelled())
                return null;

            try {

                String key = identifier+"_"+index;

                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

                if(bm == null)  //If it doesn't exists, retrieve image from network
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
            progressBar.setVisibility(View.GONE);
            if(bitmap != null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

}
