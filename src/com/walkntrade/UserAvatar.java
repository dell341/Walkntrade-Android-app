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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.io.ImageTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class UserAvatar extends Activity implements View.OnClickListener {

    private static final String TAG = "UserAvatar";
    private static final String SAVED_IMAGE_PATH = "saved_image_path";
    private static final String SAVED_IMAGE_URI = "saved_image_uri";
    private static final String SAVED_IMAGE = "saved_image_instance";
    private static final int CAPTURE_IMAGE = 100;
    private static final int GALLERY_IMAGE = 200;

    private Context context;
    private ImageView avatar;
    private TextView error;
    private ProgressBar progress;
    private InputStream inputStream;
    private Uri uriStream;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_avatar);

        context = getApplicationContext();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        error = (TextView) findViewById(R.id.error_message);
        avatar = (ImageView) findViewById(R.id.avatar);
        Button button = (Button) findViewById(R.id.uploadButton);

        if(savedInstanceState != null) {
            int width = (int) getResources().getDimension(R.dimen.image_size_height);
            int height = (int) getResources().getDimension(R.dimen.image_size_height);

            currentPhotoPath = savedInstanceState.getString(SAVED_IMAGE_PATH);
            uriStream = savedInstanceState.getParcelable(SAVED_IMAGE_URI);

            if(currentPhotoPath != null) //Image from camera capture
                avatar.setImageBitmap(ImageTool.getImageFromDevice(currentPhotoPath, width, height));
            else if (uriStream != null) { //Image from existing upload
                try {
                    avatar.setImageBitmap(ImageTool.getImageFromDevice(context, uriStream, width, height));
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File Not Found", e);
                }
            }
            else { //Current avatar image
               Bitmap bitmap = savedInstanceState.getParcelable(SAVED_IMAGE);
               avatar.setImageBitmap(bitmap);

                if(bitmap == null && DataParser.isNetworkAvailable(this))
                    new GetAvatarTask().execute();
            }

        } else {
            if(DataParser.isNetworkAvailable(this))
                new GetAvatarTask().execute();
        }

        avatar.setOnClickListener(this);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);

                if(currentPhotoPath == null && inputStream == null) {
                    error.setText(getString(R.string.null_avatar_upload));
                    error.setVisibility(View.VISIBLE);
                }
                else
                    new UploadAvatarTask().execute();
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
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

        try {
            outState.putParcelable(SAVED_IMAGE, ((BitmapDrawable) avatar.getDrawable()).getBitmap());
        } catch (NullPointerException e){
            Log.e(TAG, "Orientation Change before image downloaded");
        }
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
        int width = (int) getResources().getDimension(R.dimen.image_size_height);
        int height = (int) getResources().getDimension(R.dimen.image_size_height);

        if (resultCode == RESULT_OK) {

            if (requestCode == GALLERY_IMAGE) {
                Uri returnUri = data.getData();

                if(returnUri == null)
                    return;

                try {
                    currentPhotoPath = null;
                    uriStream = returnUri;
                    inputStream = getContentResolver().openInputStream(returnUri);
                    avatar.setImageBitmap(ImageTool.getImageFromDevice(context, returnUri, width, height));
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found", e);
                }

            }
            else {
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
            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.USER_IMAGE);
            try {
                String avatarURL = database.simpleGetIntent(DataParser.INTENT_GET_AVATAR);

                if (avatarURL == null)
                    return null;

                String splitURL[] = avatarURL.split("_");
                String key = splitURL[2]; //The URL will also be used as the key to cache their avatar image

                bm = imageCache.getBitmapFromDiskCache(key.substring(0, 1)); //Try to retrieve image from cache

                if (bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(avatarURL);

                imageCache.addBitmapToCache(key.substring(0, 1), bm); //Finally cache bitmap. Will override cache if already exists or write new cache
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

            if (bitmap != null)
                avatar.setImageBitmap(bitmap);
            else
                avatar.setImageDrawable(getResources().getDrawable(R.drawable.no_avatar));
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

                if(currentPhotoPath != null)
                    response = database.uploadUserAvatar(currentPhotoPath);
                else
                    response = database.uploadUserAvatar(inputStream);

                imageCache = new DiskLruImageCache(context, DiskLruImageCache.USER_IMAGE);
            } catch(IOException e){
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

            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
        }
    }

}
