package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class UserAvatar extends Activity implements View.OnClickListener {

    private static final String TAG = "UserAvatar";
    private static final int CAPTURE_IMAGE = 100;
    private static final int GALLERY_IMAGE = 200;

    private Context context;
    private ImageView avatar;
    private TextView error;
    private ProgressBar progress;
    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_avatar);

        context = getApplicationContext();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        error = (TextView) findViewById(R.id.error_message);
        avatar = (ImageView) findViewById(R.id.avatar);
        Button button = (Button) findViewById(R.id.uploadButton);

        if(DataParser.isNetworkAvailable(this))
         new GetAvatarTask().execute();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                error.setVisibility(View.GONE);

                if(mCurrentPhotoPath == null) {
                    error.setText(getString(R.string.null_avatar_upload));
                    error.setVisibility(View.VISIBLE);
                }
                else
                    new UploadAvatarTask().execute(mCurrentPhotoPath);
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
    public void onClick(View view) {
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
                                    if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(image)); //Set image file name and location
                                        startActivityForResult(cameraIntent, CAPTURE_IMAGE);
                                    }
                                }
                                dialogInterface.dismiss();
                                break;
                            case 1: //Upload existing photo

                                Intent galleryPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                galleryPhotoIntent.setType("image/jpeg");

                                if (galleryPhotoIntent.resolveActivity(getPackageManager()) != null)
                                    startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE);
                                dialogInterface.dismiss();
                                break;
                        }
                    }
                }).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {

            if (requestCode == GALLERY_IMAGE) {
                Uri returnUri = data.getData();
                mCurrentPhotoPath = ImageTool.getPath(context, returnUri); //Gets image path of Gallery image
            }
            else {
                Log.v(TAG, "PATH: "+mCurrentPhotoPath);
                addPicToGallery();
            }

            avatar.setImageBitmap(ImageTool.getImageFromDevice(mCurrentPhotoPath, avatar));
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
        mCurrentPhotoPath = imageFile.getAbsolutePath(); //Gets image path of created image
        return imageFile;
    }

    //Add file to device default gallery
    private void addPicToGallery() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
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
                String avatarURL = database.getUserAvatar();

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

            avatar.setOnClickListener(UserAvatar.this);
        }
    }

    private class UploadAvatarTask extends AsyncTask<String, Void, String> {

        private DiskLruImageCache imageCache;

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
        }

        @Override
        protected String doInBackground(String... imagePath) {
            DataParser database = new DataParser(context);
            String response = null;

            try {
                response = database.uploadUserAvatar(imagePath[0]);
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
