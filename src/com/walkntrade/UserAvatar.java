package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.ImageTool;
import com.walkntrade.io.ObjectResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class UserAvatar extends Activity implements View.OnClickListener {

    private static final String TAG = "UserAvatar";
    private static final String SAVED_IMAGE_PATH = "saved_image_path";
    private static final String SAVED_IMAGE_URI = "saved_image_uri";
    private static final String SAVED_STATE = "saved_image_awaiting_save";
    private static final int CAPTURE_IMAGE = 100;
    private static final int GALLERY_IMAGE = 200;

    private Context context;
    private ScrollView scrollView;
    private RelativeLayout imageContainerLayout;
    private ImageView imageContainer, avatar;
    private TextView error, noImage;
    private ProgressBar progress;
    private InputStream inputStream;
    private Uri uriStream;
    private String currentPhotoPath;
    private boolean imageAwaitingSave = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_avatar);

        context = getApplicationContext();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        imageContainerLayout = (RelativeLayout) findViewById(R.id.image_container_layout);
        imageContainer = (ImageView) findViewById(R.id.image_container_icon);
        error = (TextView) findViewById(R.id.error_message);
        noImage = (TextView) findViewById(R.id.no_image_uploaded);
        avatar = (ImageView) findViewById(R.id.avatar);
        Button button = (Button) findViewById(R.id.uploadButton);

        if (savedInstanceState != null) {
            int width = (int) getResources().getDimension(R.dimen.image_size_height);
            int height = (int) getResources().getDimension(R.dimen.image_size_height);

            currentPhotoPath = savedInstanceState.getString(SAVED_IMAGE_PATH);
            uriStream = savedInstanceState.getParcelable(SAVED_IMAGE_URI);
            imageAwaitingSave = savedInstanceState.getBoolean(SAVED_STATE);

            if (currentPhotoPath != null) //Image from camera capture
                avatar.setImageBitmap(ImageTool.getImageFromDevice(currentPhotoPath, width, height));
            else if (uriStream != null) { //Image from existing upload
                try {
                    avatar.setImageBitmap(ImageTool.getImageFromDevice(context, uriStream, width, height));
                    inputStream = getContentResolver().openInputStream(uriStream);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File Not Found", e);
                }
            } else { //Current avatar image
                getCachedImage();
            }
        } else
            getCachedImage();

        if (imageAwaitingSave)
            imageContainerLayout.setBackgroundColor(getResources().getColor(R.color.yellow));

        avatar.setOnClickListener(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);

                if (currentPhotoPath == null && inputStream == null) {
                    error.setText(getString(R.string.null_avatar_upload));
                    error.setVisibility(View.VISIBLE);
                } else
                    new UploadAvatarTask().execute();
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void getCachedImage() {
        DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);

        String avatarURL;
        avatarURL = DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_AVATAR_URL);

        if(avatarURL == null || avatarURL.isEmpty()) {
            if(DataParser.isNetworkAvailable(context))
                new GetAvatarTask().execute();
            return;
        }

        try {
            String splitURL[] = avatarURL.split("_");
            String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
            splitURL = key.split("\\.");
            key = splitURL[0];

            Bitmap bm = imageCache.getBitmapFromDiskCache(key);
            imageCache.close();

            if (bm == null) {
                if (DataParser.isNetworkAvailable(context))
                    new GetAvatarTask().execute();
            } else
                avatar.setImageBitmap(bm); //Try to retrieve image from cache
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "Image does not exist", e);
            //If user has not uploaded an image, leave Bitmap as null
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_IMAGE_PATH, currentPhotoPath);
        outState.putParcelable(SAVED_IMAGE_URI, uriStream);
        outState.putBoolean(SAVED_STATE, imageAwaitingSave);
    }

    @Override
    public void onClick(View view) {
        error.setVisibility(View.GONE);
        //Creates dialog popup to take a new picture or upload existing photo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_photo))
                .setItems(R.array.add_photo_options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        switch (index) {
                            case 0: //Use camera

                                //Create intent to take picture
                                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                File image = null;

                                try {
                                    image = createImageFile();
                                } catch (IOException e) {
                                    Log.e(TAG, "Creating image file", e);
                                }

                                if (image != null) {
                                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image)); //Set image file name and location
                                    startActivityForResult(cameraIntent, CAPTURE_IMAGE);
                                }
                                dialogInterface.dismiss();
                                break;
                            case 1: //Upload existing photo

                                Intent galleryPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                galleryPhotoIntent.addCategory(Intent.CATEGORY_OPENABLE);
                                galleryPhotoIntent.setType("image/jpeg");

                                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE);
                                dialogInterface.dismiss();
                                break;
                        }
                    }
                }).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        int width = (int) getResources().getDimension(R.dimen.image_size_height);
        int height = (int) getResources().getDimension(R.dimen.image_size_height);

        if (resultCode == RESULT_OK) {
            imageAwaitingSave = true;
            imageContainerLayout.setBackgroundColor(getResources().getColor(R.color.yellow));
            noImage.setVisibility(View.INVISIBLE);

            if (requestCode == GALLERY_IMAGE) {
                Uri returnUri = data.getData();

                if (returnUri == null)
                    return;

                try {
                    currentPhotoPath = null;
                    uriStream = returnUri;
                    inputStream = getContentResolver().openInputStream(returnUri);
                    avatar.setImageBitmap(ImageTool.getImageFromDevice(context, returnUri, width, height));
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found", e);
                }
            } else {
                addPicToGallery();
                inputStream = null;
                avatar.setImageBitmap(ImageTool.getImageFromDevice(currentPhotoPath, width, height));
            }
        }
    }

    //Creates image file to be saved
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Random generator = new Random();
        int num = generator.nextInt(89999) + 10000; //Generate a five-digit number
        String imageFileName = "wnt_" + timeStamp + num;

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), getString(R.string.app_name));

        //If the directory doesn't exist, create it
        if (!storageDir.exists())
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Could not create Media directory");
                return null;
            }

        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = imageFile.getAbsolutePath(); //Gets image path of created image
        return imageFile;
    }

    //Add file to device default gallery
    private void addPicToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    //Put avatar in ImageView, if available
    private class GetAvatarTask extends AsyncTask<Void, Void, Bitmap> {
        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            //Taken directly from AvatarRetrievalTask
            DataParser database = new DataParser(context);
            Bitmap bm = null;
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            try {
                String avatarURL;

                ObjectResult<String> result = database.getAvatarUrl(null, true);
                avatarURL = result.getObject();

                if (avatarURL == null)
                    return null;

                String splitURL[] = avatarURL.split("_");
                String key = splitURL[2]; //The user id will be used as the key to cache their avatar image
                splitURL = key.split("\\.");
                key = splitURL[0];

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
            progress.setVisibility(View.GONE);

            if (currentPhotoPath == null && uriStream == null) { //If device was rotated before new image could be picked, do nothing
                if (bitmap != null)
                    avatar.setImageBitmap(bitmap);
                else
                    noImage.setVisibility(View.VISIBLE);
            }
        }
    }

    private class UploadAvatarTask extends AsyncTask<Void, Void, String> {

        private DiskLruImageCache imageCache;

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            String response = null;

            try {

                if (currentPhotoPath != null)
                    response = database.uploadUserAvatar(currentPhotoPath);
                else
                    response = database.uploadUserAvatar(inputStream);

                imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            } catch (IOException e) {
                Log.e(TAG, "Uploading user avatar", e);
            } finally {
                imageCache.clearCache(); //Clears avatar user image from cache. So new one will be uploaded.
                imageCache.close();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            progress.setVisibility(View.INVISIBLE);

            if (!response.equals("0")) {
                if (response.substring(0, 44).equals("<br /><b>Warning</b>:  imagecreatefromjpeg()"))
                    error.setText("Only .jpg images are currently supported");
                else
                    error.setText(response);

                imageContainerLayout.setBackgroundColor(getResources().getColor(R.color.lighter_red));
                error.setVisibility(View.VISIBLE);
                scrollView.fullScroll(ScrollView.FOCUS_UP);
            } else {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SchoolPage.ACTION_UPDATE_DRAWER));
                imageContainerLayout.setBackgroundColor(getResources().getColor(R.color.green_secondary));
                imageContainer.setImageResource(R.drawable.ic_action_accept);
            }
        }
    }

}
