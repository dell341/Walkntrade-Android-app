package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
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

import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;

import java.io.IOException;

//Swipes through images of post
public class ImageDialog extends Activity {

    private static final String TAG = "ImageDialog";
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
		imgURLs = getIntent().getStringArrayExtra(ShowPage.IMGSRC);
		identifier = getIntent().getStringExtra(ShowPage.IDENTIFIER);
        int index = getIntent().getIntExtra(ShowPage.INDEX, 0);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        ImageFragmentAdapter adapter = new ImageFragmentAdapter(getFragmentManager());
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

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_image, container, false);

            ImageView imageView = (ImageView) rootView.findViewById(R.id.full_post_image);
            ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
            Bundle args = getArguments();
            int index = args.getInt(IMAGE_INDEX);
            String url = args.getString(IMAGE_URL);

            if(DataParser.isNetworkAvailable(context)) {
                //Calls single image to be displayed in pop-up
                ImageRetrievalTask task = new ImageRetrievalTask(index, imageView, progressBar);
                task.execute(url);
            }

            return rootView;
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
            String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
            DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.IMAGE_DIRECTORY);

            Bitmap bm = null;
            try {

                String key = identifier+"_"+index;

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
            progressBar.setVisibility(View.GONE);
            if(bitmap != null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

}
