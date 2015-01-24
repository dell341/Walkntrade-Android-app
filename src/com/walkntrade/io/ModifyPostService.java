package com.walkntrade.io;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.walkntrade.R;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

/**
 * Adds or edits a post here. Offloading most of the work from the activity
 */
public class ModifyPostService extends IntentService {
    public static final String ACTION_ADD_POST = "com.walkntrade.io.action.ADD";
    public static final String ACTION_EDIT_POST = "com.walkntrade.io.action.EDIT";
    public static final String ACTION_ADD_IMAGES = "com.walkntrade.io.action.ADD_IMAGES";
    public static final String ACTION_EDIT_IMAGES = "com.walkntrade.io.action.EDIT_IMAGES";

    public static final String EXTRA_SERVER_RESPONSE = "com.walkntrade.io.extra.RESPONSE";
    public static final String EXTRA_RECEIVED_IDENTIFIER = "com.walkntrade.io.extra.RECEIVED_IDENTIFIER";

    public static final String EXTRA_TITLE = "com.walkntrade.io.extra.TITLE";
    public static final String EXTRA_AUTHOR = "com.walkntrade.io.extra.AUTHOR";
    public static final String EXTRA_DESCRIPTION = "com.walkntrade.io.extra.DESCRIPTION";
    public static final String EXTRA_ISBN = "com.walkntrade.io.extra.ISBN";
    public static final String EXTRA_PRICE = "com.walkntrade.io.extra.PRICE";
    public static final String EXTRA_TAGS = "com.walkntrade.io.extra.TAGS";
    public static final String EXTRA_CATEGORY = "com.walkntrade.io.extra.CATEGORY";

    public static final String EXTRA_SCHOOL_ID = "com.walkntrade.io.extra.SCHOOL_ID";
    public static final String EXTRA_IDENTIFIER = "com.walkntrade.io.extra.IDENTIFIER";

    public static final String EXTRA_PHOTO_PATHS = "com.walkntrade.io.extra.PHOTO_PATHS";
    public static final String EXTRA_URI_STREAMS = "com.walkntrade.io.extra.URI_STREAMS";
    public static final String EXTRA_ORG_IMAGE_COUNT = "com.walkntrade.io.extra.IMAGE_COUNT";

    private static final String TAG = "ModifyPostService";

    public ModifyPostService() {
        super("ModifyPostService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            Log.i(TAG, "onHandle Adding/Editing/Imaging post");
            final String action = intent.getAction();
            if (ACTION_ADD_POST.equals(action)) {
                final String category = intent.getStringExtra(EXTRA_CATEGORY);
                final String title = intent.getStringExtra(EXTRA_TITLE);
                final String author = intent.getStringExtra(EXTRA_AUTHOR);
                final String description = intent.getStringExtra(EXTRA_DESCRIPTION);
                final String isbn = intent.getStringExtra(EXTRA_ISBN);
                final String price = intent.getStringExtra(EXTRA_PRICE);
                final String tags = intent.getStringExtra(EXTRA_TAGS);

                addPost(category, title, author, description, isbn, price, tags);
            } else if (ACTION_EDIT_POST.equals(action)) {
                final String schoolId = intent.getStringExtra(EXTRA_SCHOOL_ID);
                final String identifier = intent.getStringExtra(EXTRA_IDENTIFIER);
                final String title = intent.getStringExtra(EXTRA_TITLE);
                final String description = intent.getStringExtra(EXTRA_DESCRIPTION);
                final String price = intent.getStringExtra(EXTRA_PRICE);
                final String tags = intent.getStringExtra(EXTRA_TAGS);

                editPost(schoolId, identifier, title, description, price, tags);
            } else if (ACTION_ADD_IMAGES.equals(action)) {
                final String identifier = intent.getStringExtra(EXTRA_IDENTIFIER);
                final String[] photoPaths = intent.getStringArrayExtra(EXTRA_PHOTO_PATHS);
                final Parcelable[] parcelables = intent.getParcelableArrayExtra(EXTRA_URI_STREAMS);

                Uri[] uriStreams = new Uri[4];
                for (int i = 0; i < parcelables.length; i++) { //Each item has to be cast individually. It cannot be guaranteed that every Parcelable item is also a Uri item. [Prevents ClassCastException]
                    if (parcelables[i] != null)
                        uriStreams[i] = (Uri) parcelables[i];
                }

                addImages(identifier, photoPaths, uriStreams);
            } else if (ACTION_EDIT_IMAGES.equals(action)) {
                final String identifier = intent.getStringExtra(EXTRA_IDENTIFIER);
                final int orgImageCount = intent.getIntExtra(EXTRA_ORG_IMAGE_COUNT, 0);
                final String[] photoPaths = intent.getStringArrayExtra(EXTRA_PHOTO_PATHS);
                final Parcelable[] parcelables = intent.getParcelableArrayExtra(EXTRA_URI_STREAMS);

                Uri[] uriStreams = new Uri[4];
                for (int i = 0; i < parcelables.length; i++) { //Each item has to be cast individually. It cannot be guaranteed that every Parcelable item is also a Uri item. [Prevents ClassCastException]
                    if (parcelables[i] != null)
                        uriStreams[i] = (Uri) parcelables[i];
                }

                editImages(identifier, orgImageCount, photoPaths, uriStreams);
            }
        }
    }

    private void addPost(String category, String title, String author, String description, String isbn, String price, String tags) {
        DataParser database = new DataParser(getApplicationContext());
        float priceValue;
        try {
            priceValue = Float.parseFloat(price);
        } catch (NumberFormatException e) {
            priceValue = 0; //If price field is empty or invalid, set to zero
        }

        String identifier = "";
        String schoolCode = DataParser.getSharedStringPreference(getApplicationContext(), DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);

        try {
            //Calls different method if book was selected
            if (category.equals(getApplicationContext().getString(R.string.server_category_book))) {
                int isbnValue;
                try {
                    isbnValue = Integer.parseInt(isbn);
                } catch (NumberFormatException e) {
                    isbnValue = 0; //If isbn field is empty or invalid, set to zero
                }
                identifier = database.addPostBook(category, schoolCode, title, author, description, priceValue, tags, isbnValue);
            } else
                identifier = database.addPostOther(category, schoolCode, title, description, priceValue, tags);
        } catch (IOException e) {
            Log.e(TAG, "Adding post", e);
        }

        Intent intent = new Intent(ACTION_ADD_POST);
        intent.putExtra(EXTRA_RECEIVED_IDENTIFIER, identifier);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void editPost(String schoolId, String identifier, String title, String description, String price, String tags) {
        DataParser database = new DataParser(getApplicationContext());
        int requestStatus = StatusCodeParser.CONNECT_FAILED;

        try {
            requestStatus = database.editPost(schoolId, identifier, title, description, price, tags);
        } catch (IOException e) {
            Log.e(TAG, "Editing Post", e);
        }

        Intent intent = new Intent(ACTION_EDIT_POST);
        intent.putExtra(EXTRA_SERVER_RESPONSE, requestStatus);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void addImages(String identifier, String[] photoPaths, Uri[] uriStreams) {
        int currentPhotoIndex = 0;
        DataParser database = new DataParser(getApplicationContext());
        String[] responses = new String[4];

        try {
            //Add any images with photo paths. Pictures taken with the device's camera
            for (String photoPath : photoPaths)
                if (photoPath != null && !photoPath.isEmpty()) //Photopath is not null and it is not empty
                    responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoPath, currentPhotoIndex++);

            //Add any images from input streams. Existing images from device
            for (Uri uriStream : uriStreams)
                if (uriStream != null) {
                    try {
                        InputStream photoStream = getApplicationContext().getContentResolver().openInputStream(uriStream);
                        responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoStream, currentPhotoIndex++);
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File Not Found", e);
                    }
                }
        } catch (IOException e) {
            Log.e(TAG, "Uploading images", e);
        }

        Intent intent = new Intent(ACTION_ADD_IMAGES);
        intent.putExtra(EXTRA_SERVER_RESPONSE, responses);
        intent.putExtra(EXTRA_IDENTIFIER, identifier);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void editImages(String identifier, int originalImageCount, String[] photoPaths, Uri[] uriStreams) {
        int currentPhotoIndex = 0;
        DataParser database = new DataParser(getApplicationContext());
        String[] responses = new String[4];

        try {
            //Add any images with photo paths. Pictures taken with the device's camera
            for (int i = 0; i < photoPaths.length; i++) {
                if (photoPaths[i] != null && !photoPaths[i].isEmpty()) {
                    switch (i) {
                        case 0:
                            responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoPaths[i], currentPhotoIndex++);
                            break;
                        case 1:
                            if (originalImageCount > 0) {
                                responses[1] = database.uploadPostImage(identifier, photoPaths[i], 1);
                                currentPhotoIndex = 2;
                            } else
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoPaths[i], currentPhotoIndex++);
                            break;
                        case 2:
                            if (originalImageCount > 1) {
                                responses[2] = database.uploadPostImage(identifier, photoPaths[i], 2);
                                currentPhotoIndex = 3;
                            } else
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoPaths[i], currentPhotoIndex++);
                            break;
                        case 3:
                            if (originalImageCount > 2)
                                responses[3] = database.uploadPostImage(identifier, photoPaths[i], 3);
                            else
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoPaths[i], currentPhotoIndex++);
                            break;
                    }
                }
            }
            //Add any images from input streams. Existing images from device
            for (int i = 0; i < uriStreams.length; i++) {
                if (uriStreams[i] != null) {
                    InputStream photoStream = getApplicationContext().getContentResolver().openInputStream(uriStreams[i]);
                    switch (i) {
                        case 0:
                            responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoStream, currentPhotoIndex++);
                            break;
                        case 1:
                            if (originalImageCount > 0) {
                                responses[1] = database.uploadPostImage(identifier, photoStream, 1);
                                currentPhotoIndex = 2;
                            } else
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoStream, currentPhotoIndex++);
                            break;
                        case 2:
                            if (originalImageCount > 1) {
                                responses[2] = database.uploadPostImage(identifier, photoStream, 2);
                                currentPhotoIndex = 3;
                            } else
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoStream, currentPhotoIndex++);
                            break;
                        case 3:
                            if (originalImageCount > 2) {
                                responses[3] = database.uploadPostImage(identifier, photoStream, 3);
                            } else
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier, photoStream, currentPhotoIndex++);
                            break;
                    }
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Uploading images", e);
        }

        Intent intent = new Intent(ACTION_ADD_IMAGES);
        intent.putExtra(EXTRA_SERVER_RESPONSE, responses);
        intent.putExtra(EXTRA_IDENTIFIER, identifier);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
