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
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
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
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.Post;

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

public class EditPost extends Activity implements View.OnClickListener {

    private static final String TAG = "EditPost";
    private static final String SAVED_CURRENT_PATH = "saved_instance_current_path";
    private static final String SAVED_CURRENT_SCHOOLID = "saved_instance_current_school";
    private static final String SAVED_CURRENT_INDEX = "saved_instance_current_index";
    private static final String SAVED_IMAGE_PATHS = "saved_instance_image_paths";
    private static final String SAVED_IMAGE_URIS = "saved_instance_image_uri";
    public static final String POST_OBJECT = "post_object";
    public static final String POST_ID = "post_obs_id";
    public static final String POST_IDENTIFIER = "post_identifier";
    public static final String POST_SCHOOL = "post_school";

    private static final int CAPTURE_IMAGE_ONE = 100;
    private static final int CAPTURE_IMAGE_TWO = 200;
    private static final int CAPTURE_IMAGE_THREE = 300;
    private static final int CAPTURE_IMAGE_FOUR = 400;
    private static final int GALLERY_IMAGE_ONE = 500;
    private static final int GALLERY_IMAGE_TWO = 600;
    private static final int GALLERY_IMAGE_THREE = 700;
    private static final int GALLERY_IMAGE_FOUR = 800;

    private Context context;
    private String obsId, identifier;
    private ProgressBar progress1, progress2, progress3, progress4, saveProgressBar;
    private TextView title, details, price, errorMessage;
    private ImageView image1, image2, image3, image4;
    private Button submit;

    private String schoolId; //School id might change for each post being edited
    public int originalImageCount = 0;
    private int imageIndex = -1; //Index of current image that is being edited
    private String selectedCategory;
    private String currentPhotoPath;
    private Uri[] uriStreams = new Uri[4]; //Uri (addresses) for images located on device or cloud storage
    private String[] photoPaths = new String[4]; //File paths for pictures taken with camera, or located on device

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        Post thisPost = getIntent().getParcelableExtra(POST_OBJECT);
        obsId = getIntent().getStringExtra(POST_ID);
        identifier = getIntent().getStringExtra(POST_IDENTIFIER);
        schoolId = getIntent().getStringExtra(POST_SCHOOL);

        context = getApplicationContext();
        progress1 = (ProgressBar) findViewById(R.id.progressBar1);
        progress2 = (ProgressBar) findViewById(R.id.progressBar2);
        progress3 = (ProgressBar) findViewById(R.id.progressBar3);
        progress4 = (ProgressBar) findViewById(R.id.progressBar4);
        saveProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        errorMessage = (TextView) findViewById(R.id.error_message);
        title = (TextView) findViewById(R.id.postTitle);
        details = (TextView) findViewById(R.id.postDescr);
        price = (TextView) findViewById(R.id.postPrice);
        submit = (Button) findViewById(R.id.button);

        image1 = (ImageView) findViewById(R.id.postImage1);
        image2 = (ImageView) findViewById(R.id.postImage2);
        image3 = (ImageView) findViewById(R.id.postImage3);
        image4 = (ImageView) findViewById(R.id.postImage4);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        image1.getLayoutParams().width = (int) (displayMetrics.widthPixels * .50);
        image2.getLayoutParams().width = (int) (displayMetrics.widthPixels * .50);
        image3.getLayoutParams().width = (int) (displayMetrics.widthPixels * .50);
        image4.getLayoutParams().width = (int) (displayMetrics.widthPixels * .50);

        if (savedInstanceState != null) {
            image1.setImageResource(R.drawable.ic_action_new_picture);
            image2.setImageResource(R.drawable.ic_action_new_picture);
            image3.setImageResource(R.drawable.ic_action_new_picture);
            image4.setImageResource(R.drawable.ic_action_new_picture);

            schoolId = savedInstanceState.getString(SAVED_CURRENT_SCHOOLID);
            imageIndex = savedInstanceState.getInt(SAVED_CURRENT_INDEX);
            currentPhotoPath = savedInstanceState.getString(SAVED_CURRENT_PATH);
            photoPaths = savedInstanceState.getStringArray(SAVED_IMAGE_PATHS);
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(SAVED_IMAGE_URIS);
            for (int i = 0; i < parcelables.length; i++) { //Each item has to be cast individually. It cannot be guaranteed that every Parcelable item is also a Uri item. [Prevents ClassCastException]
                if (parcelables[i] != null)
                    uriStreams[i] = (Uri) parcelables[i];
            }

            int width = image1.getLayoutParams().width;
            int height = image1.getLayoutParams().height;
            int index = 0;

            //Get camera captured image on orientation change
            for (String photoPath : photoPaths) {
                if (photoPath != null) {
                    Bitmap bm = ImageTool.getImageFromDevice(photoPaths[index], width, height);
                    switch (index) {
                        case 0: image1.setImageBitmap(bm);
                            image1.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                        case 1: image2.setImageBitmap(bm);
                            image2.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                        case 2: image3.setImageBitmap(bm);
                            image3.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                        case 3: image4.setImageBitmap(bm);
                            image4.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                        default: break;
                    }
                }
                index++;
            }

            index = 0;
            //Get uploaded image on orientation change
            for (Uri uri : uriStreams) {
                if (uri != null) {
                    try {
                        Bitmap bm = ImageTool.getImageFromDevice(context, uri, width, height);
                        switch (index) {
                            case 0: image1.setImageBitmap(bm);
                                image1.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                            case 1: image2.setImageBitmap(bm);
                                image2.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                            case 2: image3.setImageBitmap(bm);
                                image3.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                            case 3: image4.setImageBitmap(bm);
                                image4.setScaleType(ImageView.ScaleType.FIT_CENTER); break;
                            default: break;
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File not found", e);
                    }
                }
                index++;
            }

            //Get original images if their position was not replaced
            if (photoPaths[0] == null && uriStreams[0] == null && imageIndex != 0)
                new SpecialImageRetrievalTask(image1, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, generateImgURL(0));
            if (photoPaths[1] == null && uriStreams[1] == null && imageIndex != 1)
                new SpecialImageRetrievalTask(image2, 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, generateImgURL(1));
            if (photoPaths[2] == null && uriStreams[2] == null && imageIndex != 2)
                new SpecialImageRetrievalTask(image3, 2).execute(generateImgURL(2));
            if (photoPaths[3] == null && uriStreams[3] == null && imageIndex != 3)
                new SpecialImageRetrievalTask(image4, 3).execute(generateImgURL(3));

        } else {
            if (thisPost != null) { //If post object was sent from the ShowPage
                schoolId = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                identifier = thisPost.getIdentifier();
                title.setText(thisPost.getTitle());
                details.setText(thisPost.getDetails());

                if (!thisPost.getPrice().equals("0"))
                    price.setText(thisPost.getPrice());

            } else //Get post from the id
                new LaunchPostTask().execute(obsId);

            //Calls images to be displayed on show page
            //First Image
            String imgUrl = generateImgURL(0);
            new SpecialImageRetrievalTask(image1, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

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

        image1.setOnClickListener(this);
        image2.setOnClickListener(this);
        image3.setOnClickListener(this);
        image4.setOnClickListener(this);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddImagesTask().execute();
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_post_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: //If the up button was selected, close this activity
                finish();
                return true;
            case R.id.action_delete:
                //Confirms if user wants to delete this current post
                AlertDialog.Builder builder = new AlertDialog.Builder(EditPost.this);
                builder.setTitle(getString(R.string.delete_post))
                        .setMessage(R.string.delete_post_quest)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new RemovePostTask().execute();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_CURRENT_SCHOOLID, schoolId);
        outState.putString(SAVED_CURRENT_PATH, currentPhotoPath);
        outState.putInt(SAVED_CURRENT_INDEX, imageIndex);
        outState.putStringArray(SAVED_IMAGE_PATHS, photoPaths);
        outState.putParcelableArray(SAVED_IMAGE_URIS, uriStreams);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uriStreams = null;
        photoPaths = null;
    }

    @Override
    public void onClick(View view) {
        final View currentView = view;
        boolean viewHasImage = false;
        int index;

        switch (currentView.getId()) {
            case R.id.postImage1:
                index = 0;
                if (uriStreams[index] != null || photoPaths[index] != null)
                    viewHasImage = true;
                break;
            case R.id.postImage2:
                index = 1;
                if (uriStreams[index] != null || photoPaths[index] != null)
                    viewHasImage = true;
                break;
            case R.id.postImage3:
                index = 2;
                if (uriStreams[index] != null || photoPaths[index] != null)
                    viewHasImage = true;
                break;
            case R.id.postImage4:
                index = 3;
                if (uriStreams[index] != null || photoPaths[index] != null)
                    viewHasImage = true;
                break;
        }

        //Creates dialog popup to take a new picture or upload existing photo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_photo));

        if (viewHasImage) //Add option to remove image if there is one at this location
            builder.setItems(R.array.add_remove_photo_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    switch (index) {
                        case 0: //Use camera
                            useCamera(currentView);
                            dialogInterface.dismiss();
                            break;
                        case 1: //Upload existing photo
                            useExistingImage(currentView);
                            dialogInterface.dismiss();
                            break;
                        case 2: //Remove photo
                            removeImage(currentView);
                            dialogInterface.dismiss();
                            break;
                    }
                }
            }).create().show();
        else
            builder.setItems(R.array.add_photo_options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    switch (index) {
                        case 0: //Use camera
                            useCamera(currentView);
                            dialogInterface.dismiss();
                            break;
                        case 1: //Upload existing photo
                            useExistingImage(currentView);
                            dialogInterface.dismiss();
                            break;
                    }
                }
            }).create().show();
    }

    private void useCamera(View currentView) {
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

            switch (currentView.getId()) {
                case R.id.postImage1:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_ONE);
                    imageIndex = 0;
                    break;
                case R.id.postImage2:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_TWO);
                    imageIndex = 1;
                    break;
                case R.id.postImage3:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_THREE);
                    imageIndex = 2;
                    break;
                case R.id.postImage4:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_FOUR);
                    imageIndex = 3;
                    break;
            }
        }
    }

    private void useExistingImage(View currentView) {
        Intent galleryPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryPhotoIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryPhotoIntent.setType("image/jpeg");

        switch (currentView.getId()) {
            case R.id.postImage1:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_ONE);
                imageIndex = 0;
                break;
            case R.id.postImage2:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_TWO);
                imageIndex = 1;
                break;
            case R.id.postImage3:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_THREE);
                imageIndex = 2;
                break;
            case R.id.postImage4:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_FOUR);
                imageIndex = 3;
                break;
        }
    }

    private void removeImage(View currentView) {
        switch (currentView.getId()) {
            case R.id.postImage1:
                uriStreams[0] = null;
                photoPaths[0] = null;
                image1.setImageResource(R.drawable.ic_action_new_picture);
                image1.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.postImage2:
                uriStreams[1] = null;
                photoPaths[1] = null;
                image2.setImageResource(R.drawable.ic_action_new_picture);
                image2.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.postImage3:
                uriStreams[2] = null;
                photoPaths[2] = null;
                image3.setImageResource(R.drawable.ic_action_new_picture);
                image3.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.postImage4:
                uriStreams[3] = null;
                photoPaths[3] = null;
                image4.setImageResource(R.drawable.ic_action_new_picture);
                image4.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ImageView imageView;
            int width = (int) context.getResources().getDimension(R.dimen.photo_width);
            int height = (int) context.getResources().getDimension(R.dimen.photo_width);

            if (requestCode > CAPTURE_IMAGE_FOUR) {
                Uri returnUri = data.getData();

                try {
                    switch (requestCode) {
                        case GALLERY_IMAGE_ONE:
                            photoPaths[0] = null;
                            uriStreams[0] = returnUri;
                            imageView = image1;
                            break;
                        case GALLERY_IMAGE_TWO:
                            photoPaths[1] = null;
                            uriStreams[1] = returnUri;
                            imageView = image2;
                            break;
                        case GALLERY_IMAGE_THREE:
                            photoPaths[2] = null;
                            uriStreams[2] = returnUri;
                            imageView = image3;
                            break;
                        case GALLERY_IMAGE_FOUR:
                            photoPaths[3] = null;
                            uriStreams[3] = returnUri;
                            imageView = image4;
                            break;
                        default:
                            return;
                    }

                    if (returnUri == null)
                        return;

                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.setImageBitmap(ImageTool.getImageFromDevice(context, returnUri, width, height));
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found", e);
                }

            } else {

                switch (requestCode) {
                    case CAPTURE_IMAGE_ONE:
                        uriStreams[0] = null;
                        photoPaths[0] = currentPhotoPath;
                        imageView = image1;
                        break;
                    case CAPTURE_IMAGE_TWO:
                        uriStreams[1] = null;
                        photoPaths[1] = currentPhotoPath;
                        imageView = image2;
                        break;
                    case CAPTURE_IMAGE_THREE:
                        uriStreams[2] = null;
                        photoPaths[2] = currentPhotoPath;
                        imageView = image3;
                        break;
                    case CAPTURE_IMAGE_FOUR:
                        uriStreams[3] = null;
                        photoPaths[3] = currentPhotoPath;
                        imageView = image4;
                        break;
                    default:
                        return;
                }
                addPicToGallery();
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageView.setImageBitmap(ImageTool.getImageFromDevice(currentPhotoPath, width, height));
            }
        }

        imageIndex = -1;
    }

    private String generateImgURL(int index) {
        String imgUrl = "post_images/" + schoolId + "/";
        imgUrl = imgUrl + identifier + "-" + index + ".jpeg";

        return imgUrl;
    }

    //Creates image file to be saved
    public File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Random generator = new Random();
        int num = generator.nextInt(89999) + 10000; //Generate a five-digit number
        String imageFileName = "wnt_" + timeStamp + num;

        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getString(R.string.app_name));

        //If the directory doesn't exist, create it
        if (!storageDir.exists())
            if (!storageDir.mkdirs()) {
                Log.e(TAG, "Could not create Media directory");
                return null;
            }

        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = imageFile.getAbsolutePath();
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

    private class LaunchPostTask extends AsyncTask<String, Void, Integer> {

        private Post post;

        @Override
        protected Integer doInBackground(String... obsId) {
            DataParser database = new DataParser(context);
            int serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                DataParser.ObjectResult<Post> result = database.getPostByIdentifier(obsId[0]);
                serverResponse = result.getStatus();
                post = result.getValue();
            } catch (NullPointerException e) {
                Log.e(TAG, "Post does not exist");
                return StatusCodeParser.STATUS_NOT_FOUND;
            }
            catch (IOException e) {
                Log.e(TAG, "Retrieving post by identifier", e);
            }

            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer serverResponse) {

            if(serverResponse == StatusCodeParser.STATUS_OK) {
                getActionBar().setTitle(post.getTitle());
                title.setText(post.getTitle());
                details.setText(post.getDetails());

                if (!post.getPrice().equals("0"))
                    price.setText(post.getPrice());
            }
            else { //If connection failed or post does not exist, just exit edit post activity.
                Toast.makeText(context, StatusCodeParser.getStatusString(context, serverResponse), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
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
            switch (index) {
                case 0:
                    progress1.setVisibility(View.VISIBLE);
                    break;
                case 1:
                    progress2.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    progress3.setVisibility(View.VISIBLE);
                    break;
                case 3:
                    progress4.setVisibility(View.VISIBLE);
                    break;
            }
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            Bitmap bm = null;
            try {
                String key = identifier + "_" + index;

                imageCache = new DiskLruImageCache(context, schoolId + DiskLruImageCache.DIRECTORY_POST_IMAGES);

                int width;
                int height;

                do { //Keep measuring the width of the ImageView if it's zero
                    width = image1.getWidth();
                    height = image1.getHeight();
                } while (width == 0 || height == 0);

                //Always retrieve bitmap image, not cached image
                bm = DataParser.loadOptBitmap(imgURL[0], width, height);

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Image does not exist");
            } finally {
                imageCache.close();
            }

            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imgView.setImageBitmap(bitmap);
                originalImageCount++;
            } else {
                imgView.setImageResource(R.drawable.ic_action_new_picture);
                imgView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            }

            switch (index) {
                case 0:
                    progress1.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    progress2.setVisibility(View.INVISIBLE);
                    break;
                case 2:
                    progress3.setVisibility(View.INVISIBLE);
                    break;
                case 3:
                    progress4.setVisibility(View.INVISIBLE);
                    break;
            }
        }
    }

    private class EditPostTask extends AsyncTask<Void, Void, Integer> {

        @Override
        protected void onPreExecute() {
            saveProgressBar.setVisibility(View.VISIBLE);
            submit.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
            int requestStatus = StatusCodeParser.CONNECT_FAILED;

            try {
                requestStatus = database.editPost(identifier, title.getText().toString(), "", price.getText().toString(), details.getText().toString(), "", "");
            } catch (IOException e) {
                Log.e(TAG, "Editing Post", e);
            }

            return requestStatus;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            new AddImagesTask().execute();
        }
    }

    //Asynchronous Task: Sends images after successfully adding a post
    private class AddImagesTask extends AsyncTask<Void, String, String[]> {

        private int currentPhotoIndex = 0;

        @Override
        protected void onPreExecute() {
            saveProgressBar.setVisibility(View.VISIBLE);
            submit.setEnabled(false);
        }

        @Override
        protected String[] doInBackground(Void... voids) {
            DataParser database = new DataParser(context);
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
                        InputStream photoStream = context.getContentResolver().openInputStream(uriStreams[i]);
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
            return responses;
        }

        @Override
        protected void onPostExecute(String[] responses) {
            for (String response : responses)
                if (response != null)
                    Log.d(TAG, response);

            submit.setEnabled(true);
            saveProgressBar.setVisibility(View.GONE);
            Toast.makeText(context, "Images edited", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private class RemovePostTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            DataParser database = new DataParser(context);

            try {
                database.removePost(obsId);
            } catch (IOException e) {
                Log.e(TAG, "Deleting post", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            setResult(ViewPosts.RESULT_REPOPULATE);
            finish();
        }
    }
}
