package com.walkntrade.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.ImageDialog;
import com.walkntrade.LoginActivity;
import com.walkntrade.R;
import com.walkntrade.SchoolPage;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.posts.Post;

import java.io.IOException;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class Fragment_Post extends Fragment {

    public static final String TAG = "FRAGMENT:Post";
    public static String IMGSRC = "Link_For_Images";
    public static String IDENTIFIER = "Unique_Post_Id";
    public static String INDEX = "Image_Index";
    public static final String TWO_PANE = "layout_of_show_page";

    private static final String SAVED_POST = "saved_instance_post";
    private static final String SAVED_IMAGE_1 = "saved_instance_image_1";
    private static final String SAVED_IMAGE_2 = "saved_instance_image_2";
    private static final String SAVED_IMAGE_3 = "saved_instance_image_3";
    private static final String SAVED_IMAGE_4 = "saved_instance_image_4";

    private ContactUserListener contactListener;
    private Context context;
    private String identifier;
    private Post thisPost;
    private ProgressBar progressImage;
    private TextView title, details, user, date, price;
    private ImageView image, image2, image3, image4;
    private Button contact;
    private String[] imgURLs;

    public int imageCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);

        thisPost = getArguments().getParcelable(SchoolPage.SELECTED_POST);
        boolean twoPane = getArguments().getBoolean(TWO_PANE);

        context = getActivity().getApplicationContext();
        progressImage = (ProgressBar) rootView.findViewById(R.id.progressBar);
        title = (TextView) rootView.findViewById(R.id.postTitle);
        details = (TextView) rootView.findViewById(R.id.postDescr);
        user = (TextView) rootView.findViewById(R.id.userName);
        date = (TextView) rootView.findViewById(R.id.postDate);
        price = (TextView) rootView.findViewById(R.id.postPrice);
        contact = (Button) rootView.findViewById(R.id.postContact);

        image = (ImageView) rootView.findViewById(R.id.postImage1);
        image2 = (ImageView) rootView.findViewById(R.id.postImage2);
        image3 = (ImageView) rootView.findViewById(R.id.postImage3);
        image4 = (ImageView) rootView.findViewById(R.id.postImage4);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if(!twoPane) {
            image.getLayoutParams().width = (int) (displayMetrics.widthPixels* .98);
            image2.getLayoutParams().width = (int) (displayMetrics.widthPixels* .98);
            image3.getLayoutParams().width = (int) (displayMetrics.widthPixels* .98);
            image4.getLayoutParams().width = (int) (displayMetrics.widthPixels* .98);

            contact.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(DataParser.isUserLoggedIn(context)){
                        contactListener.contactUser(thisPost.getUser(), thisPost.getTitle());
                    }
                    else
                        startActivity(new Intent(context, LoginActivity.class));
                }
            });
        } else {
            image.getLayoutParams().width = (int) (displayMetrics.widthPixels * (.6666667 * .99));
            image2.getLayoutParams().width = (int) (displayMetrics.widthPixels * (.6666667 * .99));
            image3.getLayoutParams().width = (int) (displayMetrics.widthPixels * (.6666667 * .99));
            image4.getLayoutParams().width = (int) (displayMetrics.widthPixels * (.6666667 * .99));
        }


        identifier = thisPost.getIdentifier();
        title.setText(thisPost.getTitle());
        details.setText(thisPost.getDetails());
        user.setText(thisPost.getUser());
        date.setText(thisPost.getDate());
        if(!thisPost.getPrice().equals(""))
            price.setText(thisPost.getPrice());
        else
            price.setVisibility(View.GONE);
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

        //Set OnClick Listeners for each image
        image.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processClick(0);
            }
        });

        image2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processClick(1);
            }
        });

        image3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processClick(2);
            }
        });

        image4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                processClick(3);
            }
        });

        if(DataParser.isUserLoggedIn(context)) {
            new PollMessagesTask(context).execute();
            contact.setText(getString(R.string.contact));
        }
        else
            contact.setText(getString(R.string.contact_login));

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try { //Make sure that activity has implemented the listener
            contactListener = (ContactUserListener) activity;
        } catch (ClassCastException e){
            throw new  ClassCastException(activity.toString() + " must implement ContactUserListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(DataParser.isUserLoggedIn(context)) {
            new PollMessagesTask(context).execute();
            contact.setText(getString(R.string.contact));
        }
        else
            contact.setText(getString(R.string.contact_login));
    }

    private void processClick(int index){
        imgURLs = new String[imageCount];
        for(int i=0; i < imageCount; i++)
            generateValidImgURL(i);
        if(imgURLs.length > 0) {
            Intent imgDialog = new Intent(context, ImageDialog.class);
            imgDialog.putExtra(IMGSRC, imgURLs);
            imgDialog.putExtra(INDEX, index);
            imgDialog.putExtra(IDENTIFIER, identifier);
            startActivity(imgDialog);
        }
    }

    private String generateImgURL(int index){
        String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
        String imgUrl = "post_images/"+schoolID+"/";
        imgUrl = imgUrl+identifier+"-"+index+".jpeg";

        return imgUrl;
    }

    private void generateValidImgURL(int index){
        imgURLs[index] = generateImgURL(index);
    }

    //TODO: In the future receive amount of images belonging to current post. Then use that to predict amount of image urls to generate. Then delete this class
    public class SpecialImageRetrievalTask extends AsyncTask<String, Void, Bitmap>{
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
            if(index == 0) //Show progress only on the first big image
                progressImage.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            Bitmap bm = null;
            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
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
            else if(index == 0) { //If no images exist. Put the default image for the first image.
                imgView.setImageDrawable(getResources().getDrawable(R.drawable.post_image));
            }

            progressImage.setVisibility(View.INVISIBLE);
        }
    }

    public interface ContactUserListener {
        public void contactUser(String user, String title);
    }
}
