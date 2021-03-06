package com.walkntrade.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.FormatDateTime;
import com.walkntrade.io.ModifyPostService;
import com.walkntrade.io.ObjectResult;
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

    private static final String SAVED_POST_ITEMS = "saved_instance_post_items";
    private static final String SAVED_USER_IMAGE = "saved_instance_user_image";
    private static final int REQUEST_EDIT_POST = 100;

    private ContactUserListener contactListener;
    private Context context;
    private String identifier;
    private Post thisPost;
    private ProgressBar progressImage, progressUserImage, progressProfile;
    private View postLayout;
    private LinearLayout linearLayout;
    private TextView title, description, user, date, price, profileUserName;
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

        ActionBar actionBar = ((ActionBarActivity)getActivity()).getSupportActionBar();

        final ScrollView scrollView = (ScrollView) rootView.findViewById(R.id.scroll_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            scrollView.setNestedScrollingEnabled(true);

        final RelativeLayout userProfile = (RelativeLayout) rootView.findViewById(R.id.user_profile);
        RelativeLayout userLayout = (RelativeLayout) rootView.findViewById(R.id.user_layout);
        SnappingHorizontalScrollView horizontalScrollView = (SnappingHorizontalScrollView) rootView.findViewById(R.id.horizontalView);
        postLayout = rootView.findViewById(R.id.postLayout);
        linearLayout = (LinearLayout) rootView.findViewById(R.id.linear_layout);
        profilePosts = (LinearLayout) rootView.findViewById(R.id.profile_posts);
        progressImage = (ProgressBar) rootView.findViewById(R.id.progressBar);
        title = (TextView) rootView.findViewById(R.id.postTitle);
        description = (TextView) rootView.findViewById(R.id.postDescr);
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
            avatarUrl = savedInstanceState.getString(SAVED_USER_IMAGE);

            getCachedUserImage();
            if (profilePostItems == null)
                new UserProfileRetrievalTask().execute();
            else {
                for (ViewPostItem item : profilePostItems) {
                    if (item.isHeader()) {
                        View school = LayoutInflater.from(context).inflate(R.layout.item_profile_school, profilePosts, false);
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
        } else {
            new UserProfileRetrievalTask().execute();
            getCachedUserImage();
        }

        ArrayList<View> images = new ArrayList<View>(4);
        images.add(image);
        images.add(image2);
        images.add(image3);
        images.add(image4);
        horizontalScrollView.addItems(images); //Add image views to view, to allow and keep track of fling gesture

        identifier = thisPost.getIdentifier();
        title.setText(thisPost.getTitle());
        description.setText(thisPost.getDetails());
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

        //Uses onGlobalLayoutListener to get the size of the image view after it's placed
        //First Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //Only perform an image retrieval if the image is no longer there, a search hasn't been performed yet, and a search isn't currently running
                if (image.getDrawable() == null && (asyncTask1 == null || asyncTask1.getStatus() != AsyncTask.Status.RUNNING) && !imageOne) {
                    String imgUrl = generateImgURL(0);
                    getCachedPostImage(0, imgUrl, image);
                }
            }
        });

        //Second Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (image2.getDrawable() == null && (asyncTask2 == null || asyncTask2.getStatus() != AsyncTask.Status.RUNNING) && !imageTwo) {
                    String imgUrl = generateImgURL(1);
                    getCachedPostImage(1, imgUrl, image2);
                }
            }
        });

        //Third Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (image3.getDrawable() == null && (asyncTask3 == null || asyncTask3.getStatus() != AsyncTask.Status.RUNNING) && !imageThree) {
                    String imgUrl = generateImgURL(2);
                    getCachedPostImage(2, imgUrl, image3);
                }
            }
        });

        //Fourth Image
        image.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (image4.getDrawable() == null && (asyncTask4 == null || asyncTask4.getStatus() != AsyncTask.Status.RUNNING) && !imageFour) {
                    String imgUrl = generateImgURL(3);
                    getCachedPostImage(3, imgUrl, image4);
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
                        startActivityForResult(editPost, REQUEST_EDIT_POST);
                    } else
                        contactListener.contactUser(thisPost.getUser(), thisPost.getTitle());
                } else
                    startActivity(new Intent(context, LoginActivity.class));
            }
        });

        if (DataParser.isUserLoggedIn(context)) { //If user is logged in
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_EDIT_POST:
                if (resultCode == Activity.RESULT_OK) {
                    title.setText(data.getStringExtra(ModifyPostService.EXTRA_TITLE));
                    description.setText(data.getStringExtra(ModifyPostService.EXTRA_DESCRIPTION));
                    price.setText(data.getStringExtra(ModifyPostService.EXTRA_PRICE));

                    String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                    DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID + DiskLruImageCache.DIRECTORY_POST_IMAGES);

                    //Removes all possible images from disk cache
                    imageCache.removeFromCache(identifier + "_thumb");
                    for (int i = 0; i < 4; i++) {
                        String key = identifier + "_" + i;
                        imageCache.removeFromCache(key);
                    }

                    String imgUrl = generateImgURL(0);
                    asyncTask1 = new SpecialImageRetrievalTask(image, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

                    imgUrl = generateImgURL(1);
                    asyncTask2 = new SpecialImageRetrievalTask(image2, 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

                    imgUrl = generateImgURL(2);
                    asyncTask3 = new SpecialImageRetrievalTask(image3, 2).execute(imgUrl);

                    imgUrl = generateImgURL(3);
                    asyncTask4 = new SpecialImageRetrievalTask(image4, 3).execute(imgUrl);

                }
                break;
        }
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

    private void getCachedPostImage(int index, String imageUrl, ImageView imageView) {
        String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
        String key = identifier + "_" + index;

        DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID + DiskLruImageCache.DIRECTORY_POST_IMAGES);
        Bitmap bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

        if (bm == null)
            switch (index) {
                case 0:
                    asyncTask1 = new SpecialImageRetrievalTask(imageView, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
                    break;
                case 1:
                    asyncTask2 = new SpecialImageRetrievalTask(imageView, 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imageUrl);
                    break;
                case 2:
                    asyncTask3 = new SpecialImageRetrievalTask(imageView, 2).execute(imageUrl);
                    break;
                case 3:
                    asyncTask4 = new SpecialImageRetrievalTask(imageView, 3).execute(imageUrl);
                    break;
            }
        else {
            imageView.setImageBitmap(bm);
            imageView.setVisibility(View.VISIBLE);
            imageCount++;

            if (imageCount > 1) { //If there are more than 1 images, Adjust the gravity
                FrameLayout.LayoutParams linearLayoutParams = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();
                linearLayoutParams.gravity = Gravity.NO_GRAVITY;
                linearLayout.setLayoutParams(linearLayoutParams);
            }
        }

    }

    private void getCachedUserImage() {
        try {
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);

            String avatarURL = "/user_images/uid_" + thisPost.getUserId() + ".jpg";

            String splitURL[] = avatarURL.split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            Bitmap bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from cache
            imageCache.close();

            if (bm == null) {
                if (DataParser.isNetworkAvailable(context))
                    new UserAvatarRetrievalTask().execute();
            } else {
                userImage.setImageBitmap(bm);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Image does not exist", e);
            //If user has not uploaded an image, leave Bitmap as null
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

            String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
            DiskLruImageCache imageCache = new DiskLruImageCache(context, schoolID + DiskLruImageCache.DIRECTORY_POST_IMAGES);
            try {
                String key = identifier + "_" + index;

                int width = image.getWidth();
                int height = image.getHeight();

                bm = DataParser.loadOptBitmap(imgURL[0], width, height);
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
                    imageOne = true;
                    break;
                case 1:
                    imageTwo = true;
                    break;
                case 2:
                    imageThree = true;
                    break;
                case 3:
                    imageFour = true;
                    break;
            }

            if (bitmap != null) {
                imgView.setVisibility(View.VISIBLE);
                imgView.setImageBitmap(bitmap);
                imageCount++;

                if (imageCount > 1) { //If there are more than 1 images, Adjust the image widths
                    FrameLayout.LayoutParams linearLayoutParams = (FrameLayout.LayoutParams) linearLayout.getLayoutParams();
                    linearLayoutParams.gravity = Gravity.NO_GRAVITY;
                    linearLayout.setLayoutParams(linearLayoutParams);
                }
            } else {
                if (index == 0)  //If no images exist. Set the first image as default post image.
                    imgView.setImageDrawable(getResources().getDrawable(R.drawable.post_image));
            }

            progressImage.setVisibility(View.INVISIBLE);
        }
    }

    private class UserProfileRetrievalTask extends AsyncTask<Void, Void, Integer> {
        private UserProfileObject userProfile;

        @Override
        protected void onPreExecute() {
            progressProfile.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            int serverResponse = StatusCodeParser.CONNECT_FAILED;
            DataParser database = new DataParser(context);

            try {
                ObjectResult<UserProfileObject> result = database.getUserProfile(thisPost.getUser(), null);
                serverResponse = result.getStatus();
                userProfile = result.getObject();

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
                profileUserName.setText(userProfile.getUserName() + "'s posts");

                ArrayList<ReferencedPost> userPosts = userProfile.getUserPosts();
                profilePostItems = new ArrayList<>();
                String currentSchool = "";

                for (ReferencedPost p : userPosts) {
                    if (!p.getSchool().equalsIgnoreCase(currentSchool)) { //If this post is a new school, create a new header
                        currentSchool = p.getSchool();
                        profilePostItems.add(new ViewPostItem(p.getSchool(), p.getSchoolAbbv()));
                        View school = LayoutInflater.from(context).inflate(R.layout.item_profile_school, profilePosts, false);
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

    private class UserAvatarRetrievalTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected void onPreExecute() {
            progressUserImage.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bm = null;
            DataParser database = new DataParser(context);
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);

            try {
                if (avatarUrl == null || avatarUrl.isEmpty()) {
                    ObjectResult<String> result = database.getAvatarUrl(thisPost.getUser(), false);
                    if (result.getStatus() != StatusCodeParser.STATUS_OK)
                        return null;

                    avatarUrl = result.getObject();
                }

                String[] splitURL = avatarUrl.split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

                //Make dimensions of images similar to that of notification images. May be changed later.
                int largeIconWidth = Resources.getSystem().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
                int largeIconHeight = Resources.getSystem().getDimensionPixelSize(android.R.dimen.notification_large_icon_height);

                bm = DataParser.loadOptBitmap(avatarUrl, largeIconWidth, largeIconHeight);

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
