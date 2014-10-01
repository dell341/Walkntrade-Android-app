package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.posts.Post;

import java.io.IOException;

public class EditPost extends Activity {

    private static final String TAG = "EditPost";
    public static final String POST_ID = "post_obs_id";

    private Context context;
    private String identifier;
    private ProgressBar progress1, progress2, progress3, progress4;
    private TextView title, details, price;
    private ImageView image, image2, image3, image4;
    private Button submit;

    public int imageCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_post);

        String obsId = getIntent().getStringExtra(POST_ID);

        //Get post from the id
        new LaunchPostTask().execute(obsId);

        context = getApplicationContext();
        progress1 = (ProgressBar) findViewById(R.id.progressBar1);
        progress2 = (ProgressBar) findViewById(R.id.progressBar2);
        progress3 = (ProgressBar) findViewById(R.id.progressBar3);
        progress4 = (ProgressBar) findViewById(R.id.progressBar4);
        title = (TextView) findViewById(R.id.postTitle);
        details = (TextView) findViewById(R.id.postDescr);
        price = (TextView) findViewById(R.id.postPrice);
        submit = (Button) findViewById(R.id.submit);

        image = (ImageView) findViewById(R.id.postImage1);
        image2 = (ImageView) findViewById(R.id.postImage2);
        image3 = (ImageView) findViewById(R.id.postImage3);
        image4 = (ImageView) findViewById(R.id.postImage4);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        image.getLayoutParams().width = (int) (displayMetrics.widthPixels* .50);
        image2.getLayoutParams().width = (int) (displayMetrics.widthPixels* .50);
        image3.getLayoutParams().width = (int) (displayMetrics.widthPixels* .50);
        image4.getLayoutParams().width = (int) (displayMetrics.widthPixels* .50);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_post_page, menu);
        return true;
    }

    private String generateImgURL(int index){
        String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
        String imgUrl = "post_images/"+schoolID+"/";
        imgUrl = imgUrl+identifier+"-"+index+".jpeg";

        return imgUrl;
    }

    private class LaunchPostTask extends AsyncTask<String, Void, Post> {

        @Override
        protected Post doInBackground(String... obsId) {
            DataParser database = new DataParser(context);

            Post post = null;
            try {
                post = database.getPostByIdentifier(obsId[0]);
            } catch (Exception e) {
                Log.e(TAG, "Retrieving post by identifier", e);
            }

            return post;
        }

        @Override
        protected void onPostExecute(Post thisPost) {

            getActionBar().setTitle(thisPost.getTitle());
            identifier = thisPost.getIdentifier();
            title.setText(thisPost.getTitle());
            details.setText(thisPost.getDetails());

            if(!thisPost.getPrice().equals("0"))
                price.setText(thisPost.getPrice());

            //Calls images to be displayed on show page

            //First Image
            String imgUrl = generateImgURL(0);
            new SpecialImageRetrievalTask(image, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);
            //Second Image
            imgUrl = generateImgURL(1);
            new SpecialImageRetrievalTask(image2, 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);
            //Third Image
            imgUrl = generateImgURL(2);
            new SpecialImageRetrievalTask(image3, 2).execute(imgUrl);
            //Fourth Image
            imgUrl = generateImgURL(3);
            new SpecialImageRetrievalTask(image4, 3).execute(imgUrl);
        }
    }

    //TODO: In the future receive amount of images belonging to current post. Then use that to predict amount of image urls to generate. Then delete this class
    public class SpecialImageRetrievalTask extends AsyncTask<String, Void, Bitmap> {
        private final String TAG = "ASYNCTASK:SPECIALImageRetrieval";
        private ImageView imgView;
        private int index;
        private DiskLruImageCache imageCache;

        public SpecialImageRetrievalTask(ImageView _imgView, int _index){
            imgView = _imgView;
            index = _index;
        }

        @Override
        protected void onPreExecute() {
            switch(index) {
                case 0: progress1.setVisibility(View.VISIBLE); break;
                case 1: progress2.setVisibility(View.VISIBLE); break;
                case 2: progress3.setVisibility(View.VISIBLE); break;
                case 3: progress4.setVisibility(View.VISIBLE); break;
            }
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            Bitmap bm = null;
            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                String key = identifier+"_"+index;

                imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.IMAGE_DIRECTORY);
                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

                if(bm == null) { //If it doesn't exists, retrieve image from network
                    int width;
                    int height;

                    do { //Keep measuring the width of the ImageView if it's zero
                        width = image.getWidth();
                        height = image.getHeight();
                    } while (width == 0 || height == 0);

                    bm = DataParser.loadOptBitmap(imgURL[0], width, height);
                }

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Image does not exist");
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
                imageCount++;
            }

            switch(index) {
                case 0: progress1.setVisibility(View.INVISIBLE); break;
                case 1: progress2.setVisibility(View.INVISIBLE); break;
                case 2: progress3.setVisibility(View.INVISIBLE); break;
                case 3: progress4.setVisibility(View.INVISIBLE); break;
            }
        }
    }
}
