package com.walkntrade.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.walkntrade.EditPost;
import com.walkntrade.ImageDialog;
import com.walkntrade.LoginActivity;
import com.walkntrade.R;
import com.walkntrade.SchoolPage;
import com.walkntrade.adapters.item.ViewPostItem;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.FormatDateTime;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.Post;
import com.walkntrade.objects.ReferencedPost;
import com.walkntrade.objects.UserProfileObject;
import com.walkntrade.views.SnappingHorizontalScrollView;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class PostFragment extends Fragment {

    public static final String TAG = "PostFragment";
    public static String IMGSRC = "Link_For_Images";
    public static String IDENTIFIER = "Unique_Post_Id";
    public static String INDEX = "Image_Index";

    //    private static final String SAVED_IMAGE_1 = "saved_instance_image_1";
//    private static final String SAVED_IMAGE_2 = "saved_instance_image_2";
//    private static final String SAVED_IMAGE_3 = "saved_instance_image_3";
//    private static final String SAVED_IMAGE_4 = "saved_instance_image_4";
    private static final String SAVED_POST_ITEMS = "saved_instance_post_items";
    private static final String SAVED_USER_IMAGE = "saved_instance_user_image";

    private ContactUserListener contactListener;
    private Context context;
    private String identifier;
    private Post thisPost;
    private ProgressBar progressImage, progressUserImage, progressProfile;
    private LinearLayout linearLayout;
    private TextView title, details, user, date, price, profileUserName;
    private ImageView image, image2, image3, image4, userImage;
    private Button contact;
    private ArrayList<ViewPostItem> profilePostItems;
    private LinearLayout profilePosts;
    private String[] imgURLs;
    private String avatarUrl;

    private AsyncTask asyncTask1, asyncTask2, asyncTask3, asyncTask4;
    public int imageCount = 0;
    private boolean currentUserPost = false;
    private boolean imageOne, imageTwo, imageThree, imageFour; //Looks to see whether these images were already retrieved from the server (whether they exist or not)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_post, container, false);

        thisPost = getArguments().getParcelable(SchoolPage.SELECTED_POST);
        context = getActivity().getApplicationContext();

        final ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.scroll_view);
        final View postLayout = rootView.findViewById(R.id.postLayout);
        final RelativeLayout userProfile = (RelativeLayout) rootView.findViewById(R.id.user_profile);
        RelativeLayout userLayout = (RelativeLayout) rootView.findViewById(R.id.user_layout);
        SnappingHorizontalScrollView horizontalScrollView = (SnappingHorizontalScrollView) rootView.findViewById(R.id.horizontalView);
        linearLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout);
        profilePosts = (LinearLayout) rootView.findViewById(R.id.profile_posts);
        progressImage = (ProgressBar) rootView.findViewById(R.id.progressBar);
        title = (TextView) rootView.findViewById(R.id.postTitle);
        details = (TextView) rootView.findViewById(R.id.postDescr);
        user = (TextView) rootView.findViewById(R.id.userName);
        date = (TextView) rootView.findViewById(R.id.postDate);
        price = (TextView) rootView.findViewById(R.id.postPrice);
        contact = (Button) rootView.findViewById(R.id.post_contact);
        profileUserName = (TextView) rootView.findViewById(R.id.user_name);
        progressUserImage = (ProgressBar) rootView.findViewById(R.id.progressBar2);
        progressProfile = (ProgressBar) rootView.findViewById(R.id.profile_progress_bar);
        userImage = (ImageView) rootView.findViewById(R.id.user_image);
        image = (ImageView) rootView.findViewById(R.id.postImage1);
        image2 = (ImageView) rootView.findViewById(R.id.postImage2);
        image3 = (ImageView) rootView.findViewById(R.id.postImage3);
        image4 = (ImageView) rootView.findViewById(R.id.postImage4);

        if (savedInstanceState != null) {
            profilePostItems = savedInstanceState.getParcelableArrayList(SAVED_POST_ITEMS);
            String avatarUrl = savedInstanceState.getString(SAVED_USER_IMAGE);

            if (profilePostItems == null || avatarUrl == null || avatarUrl.isEmpty())
                new UserProfileRetrievalTask().execute(thisPost.getUser());
            else {
                new UserAvatarRetrievalTask().execute(avatarUrl);


                for (ViewPostItem item : profilePostItems) {
                    if (item.isHeader()) {
                        View school = LayoutInflater.from(context).inflate(R.layout.item_post_school, profilePosts, false);
                        ((TextView) school.findViewById(R.id.content_title)).setText(item.getContents());
                        profilePosts.addView(school);
                    } else {
                        View post = LayoutInflater.from(context).inflate(R.layout.item_profile_post, profilePosts, false);
                        ((TextView) post.findViewById(R.id.content_date)).setText(item.getDate());
                        ((TextView) post.findViewById(R.id.content_title)).setText(item.getContents());
                        profilePosts.addView(post);
                    }
                }
            }
        } else
            new UserProfileRetrievalTask().execute(thisPost.getUser());

        ArrayList<View> images = new ArrayList<View>(4);
        images.add(image);
        images.add(image2);
        images.add(image3);
        images.add(image4);
        horizontalScrollView.addItems(images); //Add image views to view, to allow and keep track of fling gesture

        identifier = thisPost.getIdentifier();
        title.setText(thisPost.getTitle());
        details.setText(thisPost.getDetails());
        user.setText(thisPost.getUser());
        date.setText(FormatDateTime.formatDate(thisPost.getDate()));
        if (!thisPost.getPrice().equals(""))
            price.setText(thisPost.getPrice());
        else
            price.setVisibility(View.GONE);

        profileUserName.setText(thisPost.getUser() + "'s posts");

        //Calls images to be displayed on show page
        imageOne = false;
        imageTwo = false;
        imageThree = false;
        imageFour = false;

        //First Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //Only perform an image retrieval if the image is no longer there, a search hasn't been performed yet, and a search isn't currently running
                if (image.getDrawable() == null && (asyncTask1 == null || asyncTask1.getStatus() != AsyncTask.Status.RUNNING) && !imageOne) {

                    //Adjust the first image (just in case there are no other images added)
                    image.getLayoutParams().width = (int) (postLayout.getMeasuredWidth() * .98);

                    String imgUrl = generateImgURL(0);
                    asyncTask1 = new SpecialImageRetrievalTask(image, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);
                }
            }
        });

        //Second Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (image2.getDrawable() == null && (asyncTask2 == null || asyncTask2.getStatus() != AsyncTask.Status.RUNNING) && !imageTwo) {
                    String imgUrl = generateImgURL(1);
                    asyncTask2 = new SpecialImageRetrievalTask(image2, 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);
                }
            }
        });

        //Third Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (image3.getDrawable() == null && (asyncTask3 == null || asyncTask3.getStatus() != AsyncTask.Status.RUNNING) && !imageThree) {
                    String imgUrl = generateImgURL(2);
                    asyncTask3 = new SpecialImageRetrievalTask(image3, 2).execute(imgUrl);
                }
            }
        });

        //Fourth Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (image4.getDrawable() == null && (asyncTask4 == null || asyncTask4.getStatus() != AsyncTask.Status.RUNNING) && !imageFour) {
                    String imgUrl = generateImgURL(3);
                    asyncTask4 = new SpecialImageRetrievalTask(image4, 3).execute(imgUrl);
                }
            }
        });

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

        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (DataParser.isUserLoggedIn(context)) {
                    if (currentUserPost) { //Edit Post
                        String schoolId = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                        String obsId = schoolId + ":" + thisPost.getIdentifier();

                        Intent editPost = new Intent(context, EditPost.class);

                        editPost.putExtra(EditPost.POST_OBJECT, thisPost);
                        editPost.putExtra(EditPost.POST_ID, obsId);
                        editPost.putExtra(EditPost.POST_IDENTIFIER, identifier);
                        startActivity(editPost);
                    } else
                        contactListener.contactUser(thisPost.getUser(), thisPost.getTitle());
                } else
                    startActivity(new Intent(context, LoginActivity.class));
            }
        });

        if (DataParser.isUserLoggedIn(context)) { //If user is logged in
            new PollMessagesTask(context).execute();
            if (thisPost.getUser().equals(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME))) { //If this is the current user's post
                currentUserPost = true;
                contact.setText(getString(R.string.edit_post));
            } else {
                currentUserPost = false;
                contact.setText(getString(R.string.contact));
            }
        } else
            contact.setText(getString(R.string.contact_login));

        userLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) userProfile.getLayoutParams();
                int topMargin = layout.topMargin;

                scrollView.smoothScrollTo(0, (int) userProfile.getY() - topMargin); //Scroll to user profile when user name is clicked
            }
        });

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_POST_ITEMS, profilePostItems);
        outState.putString(SAVED_USER_IMAGE, avatarUrl);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try { //Make sure that activity has implemented the listener
            contactListener = (ContactUserListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ContactUserListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (DataParser.isUserLoggedIn(context)) {
            new PollMessagesTask(context).execute();
            if (thisPost.getUser().equals(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_NAME))) {
                currentUserPost = true;
                contact.setText(getString(R.string.edit_post));
            } else {
                currentUserPost = false;
                contact.setText(getString(R.string.contact));
            }
        } else
            contact.setText(getString(R.string.contact_login));
    }

    private void processClick(int index) {
        imgURLs = new String[imageCount];
        for (int i = 0; i < imageCount; i++)
            generateValidImgURL(i);
        if (imgURLs.length > 0) {
            Intent imgDialog = new Intent(context, ImageDialog.class);
            imgDialog.putExtra(IMGSRC, imgURLs);
            imgDialog.putExtra(INDEX, index);
            imgDialog.putExtra(IDENTIFIER, identifier);
            startActivity(imgDialog);
        }
    }

    private String generateImgURL(int index) {
        String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
        String imgUrl = "post_images/" + schoolID + "/";
        imgUrl = imgUrl + identifier + "-" + index + ".jpeg";

        return imgUrl;
    }

    private void generateValidImgURL(int index) {
        imgURLs[index] = generateImgURL(index);
    }

    public interface ContactUserListener {
        public void contactUser(String user, String title);
    }

    //TODO: In the future receive amount of images belonging to current post. Then use that to predict amount of image urls to generate. Then delete this class
    private class SpecialImageRetrievalTask extends AsyncTask<String, Void, Bitmap> {
        private ImageView imgView;
        private int index;
        private DiskLruImageCache imageCache;

        public SpecialImageRetrievalTask(ImageView _imgView, int _index) {
            imgView = _imgView;
            index = _index;
        }

        @Override
        protected void onPreExecute() {
            if (index == 0) //Show progress only on the first big image
                progressImage.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            Bitmap bm = null;

            if (isCancelled())
                return null;

            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                String key = identifier + "_" + index;

                imageCache = new DiskLruImageCache(context, schoolID + DiskLruImageCache.DIRECTORY_POST_IMAGES);
                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

                if (bm == null) { //If it doesn't exists, retrieve image from network
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
                return null;
            } finally {
                imageCache.close();
            }

            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            switch (index) { //Do not search for these images again
                case 0:
                    imageOne = true; break;
                case 1:
                    imageTwo = true; break;
                case 2:
                    imageThree = true; break;
                case 3:
                    imageFour = true; break;
            }

            if (bitmap != null) {
                imgView.setVisibility(View.VISIBLE);
                imgView.setImageBitmap(bitmap);
                imageCount++;

                if (imageCount > 1) { //If there are more than 1 images, Adjust the image widths
                    FrameLayout.LayoutParams linearLayoutParams = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();
                    linearLayoutParams.gravity = Gravity.NO_GRAVITY;
                    linearLayout.setLayoutParams(linearLayoutParams);

                    if (index == 0)
                        imgView.getLayoutParams().width = (int) getResources().getDimension(R.dimen.post_image_width);
                }
            } else {
                if (index == 0)  //If no images exist. Set the first image as default post image.
                    imgView.setImageDrawable(getResources().getDrawable(R.drawable.post_image));
            }

            progressImage.setVisibility(View.INVISIBLE);
        }
    }

    private class UserProfileRetrievalTask extends AsyncTask<String, Void, Integer> {
        private UserProfileObject userProfile;

        @Override
        protected void onPreExecute() {
            progressProfile.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... strings) {
            int serverResponse = StatusCodeParser.CONNECT_FAILED;
            DataParser database = new DataParser(context);

            try {
                DataParser.ObjectResult<UserProfileObject> result = database.getUserProfile(strings[0], null);
                serverResponse = result.getStatus();
                userProfile = result.getObject();

                Log.i(TAG, "UserProfile : "+serverResponse);
            } catch (Exception e) {
                Log.e(TAG, "Downloading User Profile ", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {
            progressProfile.setVisibility(View.INVISIBLE);
            profilePosts.removeAllViews();

            if (serverResponse == StatusCodeParser.STATUS_OK) {
                avatarUrl = userProfile.getUserImageUrl();
                new UserAvatarRetrievalTask().execute(avatarUrl);
                profileUserName.setText(userProfile.getUserName() + "'s posts");

                ArrayList<ReferencedPost> userPosts = userProfile.getUserPosts();
                profilePostItems = new ArrayList<ViewPostItem>();
                String currentSchool = "";

                for (ReferencedPost p : userPosts) {
                    if (!p.getSchool().equalsIgnoreCase(currentSchool)) { //If this post is a new school, create a new header
                        currentSchool = p.getSchool();
                        profilePostItems.add(new ViewPostItem(p.getSchool(), p.getSchoolAbbv()));
                        View school = LayoutInflater.from(context).inflate(R.layout.item_post_school, profilePosts, false);
                        ((TextView) school.findViewById(R.id.content_title)).setText(p.getSchool());
                        profilePosts.addView(school);
                    }
                    profilePostItems.add(new ViewPostItem(p)); //Then continue adding posts
                    View post = LayoutInflater.from(context).inflate(R.layout.item_profile_post, profilePosts, false);
                    ((TextView) post.findViewById(R.id.content_date)).setText(p.getDate());
                    ((TextView) post.findViewById(R.id.content_title)).setText(p.getTitle());
                    profilePosts.addView(post);
                }
            }
        }
    }

    private class UserAvatarRetrievalTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            progressUserImage.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            Bitmap bm = null;
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);

            try {
                String avatarURL = url[0];

                String[] splitURL = avatarURL.split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache

                if (bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(avatarURL);

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Retrieving user avatar", e);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e(TAG, "Image does not exist", e);
                //If user has not uploaded an image, leave Bitmap as null
            } finally {
                imageCache.close();
            }
            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            progressUserImage.setVisibility(View.INVISIBLE);

            if (bitmap != null)
                userImage.setImageBitmap(bitmap);

        }
    }

}
