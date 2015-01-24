package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.fragments.SchoolPostsFragment;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.ImageTool;
import com.walkntrade.io.ModifyPostService;
import com.walkntrade.views.SnappingHorizontalScrollView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class AddPost extends Activity implements OnClickListener {

    private static final String TAG = "AddPost";
    private static final String SAVED_CURRENT_PATH = "saved_instance_current_path";
    private static final String SAVED_IMAGE_PATHS = "saved_instance_image_paths";
    private static final String SAVED_IMAGE_URIS = "saved_instance_image_uri";
    private static final String SAVED_UPLOADING_STATE = "saved_instance_uploading_state";
    private static final String SAVED_PROGRESS_MESSAGE = "saved_instance_progress_message";
    public static final String CATEGORY_NAME = "category_name";
    public static final int REQUEST_ADD_POST = 200;
    //public static final String CATEGORY_POSITION = "category_position";

    private static final int CAPTURE_IMAGE_ONE = 100;
    private static final int CAPTURE_IMAGE_TWO = 200;
    private static final int CAPTURE_IMAGE_THREE = 300;
    private static final int CAPTURE_IMAGE_FOUR = 400;
    private static final int GALLERY_IMAGE_ONE = 500;
    private static final int GALLERY_IMAGE_TWO = 600;
    private static final int GALLERY_IMAGE_THREE = 700;
    private static final int GALLERY_IMAGE_FOUR = 800;

    private ProgressDialog progressDialog;
    private ScrollView scrollView;
    private TextView postError;
    private EditText title, author, description, price, isbn, tags;
    private ImageView image1, image2, image3, image4;
    private String sTitle, sAuthor, sDescription, sIsbn, sPrice, sTags;
    private Context context;
    private Button submit;

    private boolean isUploading = false;
    private String progressMessage = "";
    private String selectedCategory;
    private String currentPhotoPath;
    private Uri[] uriStreams = new Uri[4];
    private String[] photoPaths = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectedCategory = getIntent().getStringExtra(CATEGORY_NAME);
        context = getApplicationContext();
        setContentView(R.layout.activity_add_post);

        getActionBar().setTitle("Adding "+selectedCategory+" post");

        if (!DataParser.isUserLoggedIn(context))
            startLoginActivity();

        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        postError = (TextView) findViewById(R.id.post_error);
        title = (EditText) findViewById(R.id.content_title);
        description = (EditText) findViewById(R.id.post_description);
        author = (EditText) findViewById(R.id.post_author);
        isbn = (EditText) findViewById(R.id.post_isbn);
        price = (EditText) findViewById(R.id.post_price);
        tags = (EditText) findViewById(R.id.post_tags);
        if (selectedCategory.equals(getString(R.string.server_category_book))) {
            author.setVisibility(View.VISIBLE);
            isbn.setVisibility(View.VISIBLE);
        }
        SnappingHorizontalScrollView horizontalScrollView = (SnappingHorizontalScrollView) findViewById(R.id.horizontalView);
        image1 = (ImageView) findViewById(R.id.add_image_1);
        image2 = (ImageView) findViewById(R.id.add_image_2);
        image3 = (ImageView) findViewById(R.id.add_image_3);
        image4 = (ImageView) findViewById(R.id.add_image_4);
        submit = (Button) findViewById(R.id.post_submit);

        ArrayList<View> views = new ArrayList<View>();
        views.add(image1); views.add(image2); views.add(image3); views.add(image4);
        horizontalScrollView.addItems(views);

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(null);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        if(savedInstanceState != null) {
            int width = (int) context.getResources().getDimension(R.dimen.photo_width);
            int height = (int) context.getResources().getDimension(R.dimen.photo_width);

            currentPhotoPath = savedInstanceState.getString(SAVED_CURRENT_PATH);
            photoPaths = savedInstanceState.getStringArray(SAVED_IMAGE_PATHS);
            isUploading = savedInstanceState.getBoolean(SAVED_UPLOADING_STATE);
            progressMessage = savedInstanceState.getString(SAVED_PROGRESS_MESSAGE);

            if(isUploading) {
                progressDialog.setMessage(progressMessage);
                progressDialog.show();
            }

            //Each item has to be cast individually. It cannot be guaranteed that every Parcelable item is also a Uri item. [Prevents ClassCastException]
            Parcelable[] parcelables = savedInstanceState.getParcelableArray(SAVED_IMAGE_URIS);
            for(int i=0; i<parcelables.length; i++) {
                if(parcelables[i] != null)
                    uriStreams[i] = (Uri) parcelables[i];
            }

            int index = 0;

            //Get camera captured image on orientation change
            for(String photoPath : photoPaths){
                if(photoPath != null) {
                    Bitmap bm = ImageTool.getImageFromDevice(photoPaths[index], width, height);
                    switch(index) {
                        case 0:
                            image1.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image1.setImageBitmap(bm); break;
                        case 1:
                            image2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image2.setImageBitmap(bm); break;
                        case 2:
                            image3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image3.setImageBitmap(bm); break;
                        case 3:
                            image4.setScaleType(ImageView.ScaleType.FIT_CENTER);
                            image4.setImageBitmap(bm); break;
                        default: break;
                    }
                }
                index++;
            }

            index = 0;
            //Get uploaded image on orientation change
            for(Uri uri : uriStreams) {
                if(uri != null) {
                    try {
                        Bitmap bm = ImageTool.getImageFromDevice(context, uri, width, height);

                        switch (index) {
                            case 0:
                                image1.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                image1.setImageBitmap(bm); break;
                            case 1:
                                image2.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                image2.setImageBitmap(bm); break;
                            case 2:
                                image3.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                image3.setImageBitmap(bm); break;
                            case 3:
                                image4.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                image4.setImageBitmap(bm); break;
                            default: break;
                        }
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "File not found", e);
                    }
                }
                index++;
            }

        }

        submit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isUploading) //If a post is already being uploaded, do nothing.
                    return;

                if (!DataParser.isUserLoggedIn(context)) {
                    Toast toast = Toast.makeText(context, getString(R.string.no_login), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    return;
                }

                Intent addPost = new Intent(AddPost.this, ModifyPostService.class);
                postError.setVisibility(View.GONE);
                sTitle = title.getText().toString();
                sAuthor = author.getText().toString();
                sDescription = description.getText().toString();
                sIsbn = isbn.getText().toString();
                sPrice = price.getText().toString();
                sTags = tags.getText().toString();

                //Submitting a post for a book
                if (selectedCategory.equals(getString(R.string.server_category_book))) {
                    if (canPostBook() && DataParser.isNetworkAvailable(context)) {
                        addPost.setAction(ModifyPostService.ACTION_ADD_POST);
                        addPost.putExtra(ModifyPostService.EXTRA_CATEGORY, selectedCategory);
                        addPost.putExtra(ModifyPostService.EXTRA_TITLE, sTitle);
                        addPost.putExtra(ModifyPostService.EXTRA_AUTHOR, sAuthor);
                        addPost.putExtra(ModifyPostService.EXTRA_DESCRIPTION, sDescription);
                        addPost.putExtra(ModifyPostService.EXTRA_ISBN, sIsbn);
                        addPost.putExtra(ModifyPostService.EXTRA_PRICE, sPrice);
                        addPost.putExtra(ModifyPostService.EXTRA_TAGS, sTags);

                        getApplicationContext().startService(addPost);
                        progressDialog.setMessage(context.getString(R.string.adding_post_changes));
                        progressMessage = context.getString(R.string.adding_post_changes);
                        progressDialog.show();
                        isUploading = true;
                    }
                }
                //Submitting a post for other categories
                else if (canPostOther() && DataParser.isNetworkAvailable(context)) {
                    addPost.setAction(ModifyPostService.ACTION_ADD_POST);
                    addPost.putExtra(ModifyPostService.EXTRA_CATEGORY, selectedCategory);
                    addPost.putExtra(ModifyPostService.EXTRA_TITLE, sTitle);
                    addPost.putExtra(ModifyPostService.EXTRA_DESCRIPTION, sDescription);
                    addPost.putExtra(ModifyPostService.EXTRA_PRICE, sPrice);
                    addPost.putExtra(ModifyPostService.EXTRA_TAGS, sTags);

                    getApplicationContext().startService(addPost);
                    progressDialog.setMessage(context.getString(R.string.adding_post_changes));
                    progressMessage = context.getString(R.string.adding_post_changes);
                    progressDialog.show();
                    isUploading = true;
                }
            }

        });

        image1.setOnClickListener(this);
        image2.setOnClickListener(this);
        image3.setOnClickListener(this);
        image4.setOnClickListener(this);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ModifyPostService.ACTION_ADD_POST);
        intentFilter.addAction(ModifyPostService.ACTION_ADD_IMAGES);
        LocalBroadcastManager.getInstance(context).registerReceiver(addPostReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(addPostReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_CURRENT_PATH, currentPhotoPath);
        outState.putStringArray(SAVED_IMAGE_PATHS, photoPaths);
        outState.putParcelableArray(SAVED_IMAGE_URIS, uriStreams);
        outState.putBoolean(SAVED_UPLOADING_STATE, isUploading);
        outState.putString(SAVED_PROGRESS_MESSAGE, progressMessage);
    }

    @Override
    protected void onDestroy() {
        progressDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        final View currentView = view;
        boolean viewHasImage = false;
        int index = 0;

        switch(currentView.getId()) {
            case R.id.add_image_1:
                index = 0; break;
            case R.id.add_image_2:
                index = 1; break;
            case R.id.add_image_3:
                index = 2; break;
            case R.id.add_image_4:
                index = 3; break;
        }

        if(uriStreams[index] != null || photoPaths[index] != null)
            viewHasImage = true;

        //Creates dialog popup to take a new picture or upload existing photo
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_photo));

        if(viewHasImage) //Add option to remove image if there is one at this location
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
                            dialogInterface.dismiss();
                            removeImage(currentView);
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
                case R.id.add_image_1:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_ONE);
                    break;
                case R.id.add_image_2:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_TWO);
                    break;
                case R.id.add_image_3:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_THREE);
                    break;
                case R.id.add_image_4:
                    startActivityForResult(cameraIntent, CAPTURE_IMAGE_FOUR);
                    break;
            }
        }
    }

    private void useExistingImage(View currentView) {
        Intent galleryPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryPhotoIntent.addCategory(Intent.CATEGORY_OPENABLE);
        galleryPhotoIntent.setType("image/jpeg");

        switch (currentView.getId()) {
            case R.id.add_image_1:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_ONE);
                break;
            case R.id.add_image_2:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_TWO);
                break;
            case R.id.add_image_3:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_THREE);
                break;
            case R.id.add_image_4:
                startActivityForResult(galleryPhotoIntent, GALLERY_IMAGE_FOUR);
                break;
        }
    }

    private void removeImage(View currentView) {
        switch (currentView.getId()) {
            case R.id.add_image_1:
                uriStreams[0] = null;
                photoPaths[0] = null;
                image1.setImageResource(R.drawable.ic_action_new_picture);
                image1.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.add_image_2:
                uriStreams[1] = null;
                photoPaths[1] = null;
                image2.setImageResource(R.drawable.ic_action_new_picture);
                image2.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.add_image_3:
                uriStreams[3] = null;
                photoPaths[3] = null;
                image3.setImageResource(R.drawable.ic_action_new_picture);
                image3.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.add_image_4:
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
                Log.i(TAG, "OnActivityResult: "+returnUri.getScheme()+"."+returnUri.getPath());

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
                        default: return;
                    }

                    if(returnUri == null)
                        return;

                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    imageView.setImageBitmap(ImageTool.getImageFromDevice(context, returnUri, width, height));
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found", e);
                }

            }
            else {

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

    //Verifies that post credentials are valid
    private boolean canPostOther() {
        boolean canPost = true;

        if (sTitle.length() < 2) {
            title.setError(getString(R.string.error_short_title));
            canPost = false;
        }
        if (sTitle.length() > 150) {
            title.setError(getString(R.string.error_long_title));
            canPost = false;
        }
        if (sDescription.length() < 5) {
            description.setError(getString(R.string.error_short_description));
            canPost = false;
        }
        if (sDescription.length() > 3000) {
            description.setError(getString(R.string.error_long_description));
            canPost = false;
        }
        if (sTags.length() < 5) {
            tags.setError(getString(R.string.error_short_tags));
            canPost = false;
        }

        if (!canPost) {
            postError.setText(getString(R.string.post_error));
            postError.setVisibility(View.VISIBLE);
            scrollView.scrollTo(0,0);
        }

        return canPost;
    }

    private boolean canPostBook() {
        boolean canPost = true;
        if (!canPostOther()) //Checks if all other fields are valid
            canPost = false;

        if (sAuthor.length() < 2) {
            author.setError(getString(R.string.error_short_author));
            canPost = false;
        }
        if (sAuthor.length() > 50) {
            author.setError(getString(R.string.error_long_author));
            canPost = false;
        }
        if (sIsbn.length() != 10 && sIsbn.length() != 13 && !TextUtils.isEmpty(sIsbn)) {
            isbn.setError(getString(R.string.error_isbn));
            canPost = false;
        }

        if (!canPost) {
            postError.setText(getString(R.string.post_error));
            postError.setVisibility(View.VISIBLE);
            scrollView.scrollTo(0,0);
        }

        return canPost;
    }

    private void startLoginActivity() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        startActivity(loginIntent); //Starts Login  activity
    }

    private BroadcastReceiver addPostReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(ModifyPostService.ACTION_ADD_POST)) { //Get result from adding a post
                String identifier = intent.getStringExtra(ModifyPostService.EXTRA_RECEIVED_IDENTIFIER);

                if(identifier == null || identifier.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.add_post_failed), Toast.LENGTH_SHORT).show();
                    scrollView.fullScroll(View.FOCUS_UP);
                    isUploading = false;
                }
                else {
                    //Add images
                    Intent addImages = new Intent(AddPost.this, ModifyPostService.class);
                    addImages.setAction(ModifyPostService.ACTION_ADD_IMAGES);
                    addImages.putExtra(ModifyPostService.EXTRA_IDENTIFIER, identifier);
                    addImages.putExtra(ModifyPostService.EXTRA_PHOTO_PATHS, photoPaths);
                    addImages.putExtra(ModifyPostService.EXTRA_URI_STREAMS, uriStreams);
                    getApplicationContext().startService(addImages);

                    progressMessage = context.getString(R.string.adding_post_image_changes);
                    progressDialog.setMessage(context.getString(R.string.adding_post_image_changes));
                    progressDialog.setCancelable(true);
                    progressDialog.setCanceledOnTouchOutside(true);
                }
            } else if(intent.getAction().equals(ModifyPostService.ACTION_ADD_IMAGES)) { //Get result from adding images
                isUploading = false;
                progressMessage = context.getString(R.string.done);

                Intent i = new Intent(SchoolPostsFragment.ACTION_UPDATE_POSTS);
                i.putExtra(CATEGORY_NAME, selectedCategory);
                LocalBroadcastManager.getInstance(context).sendBroadcast(i); //Update post list if this successfully post was added

                progressDialog.setMessage(context.getString(R.string.done));
                progressDialog.cancel();
                finish();
            }

        }
    };

}