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
import android.support.v4.app.NavUtils;
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
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.io.DataParser;
import com.walkntrade.io.ImageTool;
import com.walkntrade.views.SnappingHorizontalScrollView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
    public static final String CATEGORY_POSITION = "category_position";

    private static final int CAPTURE_IMAGE_ONE = 100;
    private static final int CAPTURE_IMAGE_TWO = 200;
    private static final int CAPTURE_IMAGE_THREE = 300;
    private static final int CAPTURE_IMAGE_FOUR = 400;
    private static final int GALLERY_IMAGE_ONE = 500;
    private static final int GALLERY_IMAGE_TWO = 600;
    private static final int GALLERY_IMAGE_THREE = 700;
    private static final int GALLERY_IMAGE_FOUR = 800;

    private ScrollView scrollView;
    private ProgressBar progressBar;
    private TextView postError;
    private EditText title, author, description, price, isbn, tags;
    private ImageView image1, image2, image3, image4;
    private String _title, _author, _description, _isbn, _price, _tags;
    private Context context;
    private Button submit;

    private String selectedCategory;
    private int currentPhotoIndex = 0;
    private String currentPhotoPath;
    private Uri[] uriStreams = new Uri[4];
    private String[] photoPaths = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int position = getIntent().getIntExtra(AddPost.CATEGORY_POSITION, 2);
        context = getApplicationContext();
        setContentView(R.layout.activity_add_post);

        switch (position) {
            case 0:
                selectedCategory = getString(R.string.server_category_book);
                getActionBar().setTitle(getString(R.string.add_book));
                break;
            case 1:
                selectedCategory = getString(R.string.server_category_tech);
                getActionBar().setTitle(getString(R.string.add_tech));
                break;
            case 2:
                selectedCategory = getString(R.string.server_category_service);
                getActionBar().setTitle(getString(R.string.add_service));
                break;
            case 3:
                selectedCategory = getString(R.string.server_category_misc);
                getActionBar().setTitle(getString(R.string.add_misc));
                break;
            default:
                selectedCategory = getString(R.string.server_category_book);
        }

        if (!DataParser.isUserLoggedIn(context))
            startLoginActivity();

        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        postError = (TextView) findViewById(R.id.post_error);
        title = (EditText) findViewById(R.id.content_title);
        description = (EditText) findViewById(R.id.post_description);
        price = (EditText) findViewById(R.id.post_price);
        tags = (EditText) findViewById(R.id.post_tags);
        if (selectedCategory.equals(getString(R.string.server_category_book))) {
            author = (EditText) findViewById(R.id.post_author);
            isbn = (EditText) findViewById(R.id.post_isbn);
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

        if(savedInstanceState != null) {
            int width = (int) context.getResources().getDimension(R.dimen.photo_width);
            int height = (int) context.getResources().getDimension(R.dimen.photo_width);

            currentPhotoPath = savedInstanceState.getString(SAVED_CURRENT_PATH);
            photoPaths = savedInstanceState.getStringArray(SAVED_IMAGE_PATHS);

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
                if (!DataParser.isUserLoggedIn(context)) {
                    Toast toast = Toast.makeText(context, getString(R.string.no_login), Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                    toast.show();
                    return;
                }
                postError.setVisibility(View.GONE);
                _title = title.getText().toString();
                _description = description.getText().toString();
                _tags = tags.getText().toString();
                _price = price.getText().toString();

                //Submitting a post for a book
                if (selectedCategory.equals(getString(R.string.server_category_book))) {
                    _author = author.getText().toString();
                    _isbn = isbn.getText().toString();

                    if (canPostBook() && DataParser.isNetworkAvailable(context))
                        new AddPostTask(context, selectedCategory, _title, _author, _description, _isbn, _price, _tags).execute();
                }

                //Submitting a post for other categories
                else if (canPostOther() && DataParser.isNetworkAvailable(context)) {
                    AddPostTask asyncTask = new AddPostTask(context, selectedCategory, _title, _author, _description, _isbn, _price, _tags);
                    asyncTask.execute();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(SAVED_CURRENT_PATH, currentPhotoPath);
        outState.putStringArray(SAVED_IMAGE_PATHS, photoPaths);
        outState.putParcelableArray(SAVED_IMAGE_URIS, uriStreams);
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

        if (_title.length() < 2) {
            title.setError(getString(R.string.error_short_title));
            canPost = false;
        }
        if (_title.length() > 150) {
            title.setError(getString(R.string.error_long_title));
            canPost = false;
        }
        if (_description.length() < 5) {
            description.setError(getString(R.string.error_short_description));
            canPost = false;
        }
        if (_description.length() > 3000) {
            description.setError(getString(R.string.error_long_description));
            canPost = false;
        }
        if (_tags.length() < 5) {
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

        if (_author.length() < 2) {
            author.setError(getString(R.string.error_short_author));
            canPost = false;
        }
        if (_author.length() > 50) {
            author.setError(getString(R.string.error_long_author));
            canPost = false;
        }
        if (_isbn.length() != 10 && _isbn.length() != 13 && !TextUtils.isEmpty(_isbn)) {
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

    //Asynchronous Task; Add post, receives category returns response
    public class AddPostTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private String selectedCategory;
        private String title, author, description, isbn, price, tags;
        private DataParser database;

        public AddPostTask(Context _context, String _selectedCategory, String _title, String _author, String _description, String _isbn, String _price, String _tags) {
            context = _context;
            selectedCategory = _selectedCategory;
            title = _title;
            author = _author;
            description = _description;
            isbn = _isbn;
            price = _price;
            tags = _tags;
        }

        @Override
        protected void onPreExecute() {
            submit.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... voids) {
            database = new DataParser(context);
            float priceValue;
            try {
                priceValue = Float.parseFloat(price);
            } catch (NumberFormatException e) {
                priceValue = 0; //If price field is empty or invalid, set to zero
            }

            String identifier = "";
            String schoolCode = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);

            try {
                //Calls different method if book was selected
                if (selectedCategory.equals(context.getString(R.string.server_category_book))) {
                    int isbnValue;
                    try {
                        isbnValue = Integer.parseInt(isbn);
                    } catch (NumberFormatException e) {
                        isbnValue = 0; //If isbn field is empty or invalid, set to zero
                    }
                    identifier = database.addPostBook(selectedCategory, schoolCode, title, author, description, priceValue, tags, isbnValue);
                } else
                    identifier = database.addPostOther(selectedCategory, schoolCode, title, description, priceValue, tags);
            } catch (IOException e) {
                Log.e(TAG, "Adding post", e);
            }

            return identifier;
        }

        @Override
        protected void onPostExecute(String identifier) {

            if(identifier == null || identifier.isEmpty()) {
                Toast.makeText(context, "Could not submit post", Toast.LENGTH_SHORT).show();
                scrollView.fullScroll(View.FOCUS_UP);
            }
            else
                new AddImagesTask().execute(identifier);
        }
    }

    //Asynchronous Task: Sends images after successfully adding a post
    public class AddImagesTask extends AsyncTask<String, String, String[]>{
        private DataParser database;

        @Override
        protected String[] doInBackground(String... identifier) {
            database = new DataParser(context);
            String[] responses = new String[4];

            try {
                //Add any images with photo paths. Pictures taken with the device's camera
                for (String photoPath : photoPaths)
                    if (photoPath != null && !photoPath.isEmpty()) {//Photopath is not null and it is not empty
                        responses[currentPhotoIndex] = database.uploadPostImage(identifier[0], photoPath, currentPhotoIndex++);
                    }

                    //Add any images from input streams. Existing images from device
                    for (Uri uriStream : uriStreams)
                        if (uriStream != null) {
                            try {
                                InputStream photoStream = context.getContentResolver().openInputStream(uriStream);
                                responses[currentPhotoIndex] = database.uploadPostImage(identifier[0], photoStream, currentPhotoIndex++);
                            } catch (FileNotFoundException e) {
                                Log.e(TAG, "File Not Found", e);
                            }
                        }

            } catch (IOException e){
                Log.e(TAG, "Uploading images", e);
            }
            return responses;
        }

        @Override
        protected void onPostExecute(String[] responses) {
            for(String response : responses)
                if(response != null)
                    Log.d(TAG, response);
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "Post Added", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}