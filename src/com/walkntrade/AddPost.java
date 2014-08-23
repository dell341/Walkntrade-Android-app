package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class AddPost extends Activity implements OnClickListener {

    private static final String TAG = "AddPost";
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
    private String mCurrentPhotoPath;
    private String[] photoPaths = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int position = getIntent().getIntExtra(AddPost.CATEGORY_POSITION, 2);
        context = getApplicationContext();
        setContentView(R.layout.add_post);

        switch (position) {
            case 0:
                selectedCategory = getString(R.string.server_category_book);
                getActionBar().setTitle("Adding " + getString(R.string.server_category_book));
                break;
            case 1:
                selectedCategory = getString(R.string.server_category_tech);
                getActionBar().setTitle("Adding " + getString(R.string.server_category_tech));
                break;
            case 2:
                selectedCategory = getString(R.string.server_category_service);
                getActionBar().setTitle("Adding " + getString(R.string.server_category_service));
                break;
            case 3:
                selectedCategory = getString(R.string.server_category_misc);
                getActionBar().setTitle("Adding " + getString(R.string.server_category_misc));
                break;
            default:
                selectedCategory = getString(R.string.server_category_book);
        }

        if (!DataParser.isUserLoggedIn(context))
            startLoginActivity();

        scrollView = (ScrollView) findViewById(R.id.scrollView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        postError = (TextView) findViewById(R.id.post_error);
        title = (EditText) findViewById(R.id.post_title);
        description = (EditText) findViewById(R.id.post_description);
        price = (EditText) findViewById(R.id.post_price);
        tags = (EditText) findViewById(R.id.post_tags);
        if (selectedCategory.equals(getString(R.string.server_category_book))) {
            author = (EditText) findViewById(R.id.post_user);
            isbn = (EditText) findViewById(R.id.post_isbn);
            author.setVisibility(View.VISIBLE);
            isbn.setVisibility(View.VISIBLE);
        }
        image1 = (ImageView) findViewById(R.id.add_image_1);
        image2 = (ImageView) findViewById(R.id.add_image_2);
        image3 = (ImageView) findViewById(R.id.add_image_3);
        image4 = (ImageView) findViewById(R.id.add_image_4);
        submit = (Button) findViewById(R.id.post_submit);

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
        getMenuInflater().inflate(R.menu.privacy_feedback, menu);
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

    @Override //Captures click event for the image views
    public void onClick(View view) {
        final View currentView = view;
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
                                dialogInterface.dismiss();
                                break;
                            case 1: //Upload existing photo

                                Intent galleryPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                galleryPhotoIntent.setType("image/jpg");

                                if (galleryPhotoIntent.resolveActivity(getPackageManager()) != null)
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

                                dialogInterface.dismiss();
                                break;
                        }
                    }
                }).create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ImageView imageView;

            if (requestCode > 400) {
                    Uri returnUri = data.getData();
                    mCurrentPhotoPath = ImageTool.getPath(context, returnUri);
                    switch (requestCode) {
                        case GALLERY_IMAGE_ONE:
                            photoPaths[0] = mCurrentPhotoPath;
                            imageView = image1;
                            break;
                        case GALLERY_IMAGE_TWO:
                            photoPaths[1] = mCurrentPhotoPath;
                            imageView = image2;
                            break;
                        case GALLERY_IMAGE_THREE:
                            photoPaths[2] = mCurrentPhotoPath;
                            imageView = image3;
                            break;
                        case GALLERY_IMAGE_FOUR:
                            photoPaths[3] = mCurrentPhotoPath;
                            imageView = image4;
                            break;
                        default:
                            return;
                    }
            }
            else {
                switch (requestCode) {
                    case CAPTURE_IMAGE_ONE:
                        photoPaths[0] = mCurrentPhotoPath;
                        imageView = image1;
                        break;
                    case CAPTURE_IMAGE_TWO:
                        photoPaths[1] = mCurrentPhotoPath;
                        imageView = image2;
                        break;
                    case CAPTURE_IMAGE_THREE:
                        photoPaths[2] = mCurrentPhotoPath;
                        imageView = image3;
                        break;
                    case CAPTURE_IMAGE_FOUR:
                        photoPaths[3] = mCurrentPhotoPath;
                        imageView = image4;
                        break;
                    default:
                        return;
                }
                addPicToGallery();
            }
            imageView.setImageBitmap(ImageTool.getImageFromDevice(mCurrentPhotoPath, imageView));
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
        mCurrentPhotoPath = imageFile.getAbsolutePath();
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
            scrollView.fullScroll(View.FOCUS_UP);
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
            scrollView.fullScroll(View.FOCUS_UP);
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
            String schoolCode = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);

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
    public class AddImagesTask extends AsyncTask<String, String, String>{
        private DataParser database;

        @Override
        protected String doInBackground(String... identifier) {
            database = new DataParser(context);
            String response = null;

            try {
                for(int i=0; i<photoPaths.length; i++)
                    if(photoPaths[i] != null && !photoPaths[i].isEmpty()) //Photopath is not null and it is not empty
                    response = database.uploadPostImage(identifier[0], photoPaths[i], i);
            } catch (IOException e){
                Log.e(TAG, "Uploading images", e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String s) {
            progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(context, "Post Added", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}