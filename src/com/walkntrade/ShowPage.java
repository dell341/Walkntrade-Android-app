package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.posts.Post;

import java.io.IOException;

public class ShowPage extends Activity {

    private String TAG = "ShowPage";
	public static String IMGSRC = "Link_For_Images";
	public static String IDENTIFIER = "Unique_Post_Id";
    public static String INDEX = "Image_Index";

    private static final String SAVED_POST = "saved_instance_post";
    private static final String SAVED_IMAGE_1 = "saved_instance_image_1";
    private static final String SAVED_IMAGE_2 = "saved_instance_image_2";
    private static final String SAVED_IMAGE_3 = "saved_instance_image_3";
    private static final String SAVED_IMAGE_4 = "saved_instance_image_4";

    private Context context;
    private String identifier;
    private Post thisPost;
    private ProgressBar progress;
    private TextView title, details, user, date, price;
    private ImageView image, image2, image3, image4;
    private AlertDialog dialog;
    private String message;
    private Button contact, contact_nologin;
    private String[] imgURLs;

    public int imageCount = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show__page);

        context = getApplicationContext();
		identifier = getIntent().getStringExtra(SchoolPage.SELECTED_POST);

        progress = (ProgressBar) findViewById(R.id.progressBar);
		title = (TextView) findViewById(R.id.show_post_title);
		details = (TextView) findViewById(R.id.show_post_details);
		user = (TextView) findViewById(R.id.show_post_user);
		date = (TextView) findViewById(R.id.show_post_date);
		price = (TextView) findViewById(R.id.show_post_price);
        contact = (Button) findViewById(R.id.show_post_contact);
        contact_nologin = (Button) findViewById(R.id.show_post_contact_login);

        image = (ImageView) findViewById(R.id.show_post_image);
        image2 = (ImageView) findViewById(R.id.show_post_image_2);
        image3 = (ImageView) findViewById(R.id.show_post_image_3);
        image4 = (ImageView) findViewById(R.id.show_post_image_4);

        //If orientation has been changed, retrieve previously saved data instead of another network connection.
        if(savedInstanceState != null){

            thisPost = savedInstanceState.getParcelable(SAVED_POST);
            title.setText(thisPost.getTitle());
            details.setText(thisPost.getDetails());
            user.setText(thisPost.getAuthor());
            date.setText(thisPost.getDate());
            price.setText("$"+thisPost.getPrice());

            title.setVisibility(View.VISIBLE);
            details.setVisibility(View.VISIBLE);
            user.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
            price.setVisibility(View.VISIBLE);

//            image.setImageBitmap((Bitmap)savedInstanceState.getParcelable(SAVED_IMAGE_1));
//            image2.setImageBitmap((Bitmap)savedInstanceState.getParcelable(SAVED_IMAGE_2));
//            image3.setImageBitmap((Bitmap)savedInstanceState.getParcelable(SAVED_IMAGE_3));
//            image4.setImageBitmap((Bitmap)savedInstanceState.getParcelable(SAVED_IMAGE_4));

            createMessageDialog();
        }
        else {
            new FullPostTask().execute(identifier); //Retrieves full information for this post
        }

        //Calls images to be displayed on show page

        //First Image
        String imgUrl = generateImgURL(0);
        new SpecialImageRetrievalTask(image, identifier, 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);
        //Second Image
        imgUrl = generateImgURL(1);
        new SpecialImageRetrievalTask(image2, identifier, 1).execute(imgUrl);
        //Third Image
        imgUrl = generateImgURL(2);
        new SpecialImageRetrievalTask(image3, identifier, 2).execute(imgUrl);
        //Fourth Image
        imgUrl = generateImgURL(3);
        new SpecialImageRetrievalTask(image4, identifier, 3).execute(imgUrl);

        //Set OnClick Listeners for each image
        image.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                processClick(0);
            }
        });

        image2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                processClick(1);
            }
        });

        image3.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                processClick(2);
            }
        });

        image4.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                processClick(3);
            }
        });

        if(DataParser.getSharedStringPreference(this, DataParser.PREFS_USER, DataParser.USER_PHONE) == null || DataParser.getSharedStringPreference(this, DataParser.PREFS_USER, DataParser.USER_PHONE).equals("0"))
            message = getString(R.string.post_message_content_no_phone);
        else
            message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(this, DataParser.PREFS_USER, DataParser.USER_PHONE));

        contact_nologin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ShowPage.this, LoginActivity.class));
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
        if(DataParser.isUserLoggedIn(this))
            new PollMessagesTask(this).execute();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem signOutItem = menu.findItem(R.id.action_sign_out);
        MenuItem loginItem = menu.findItem(R.id.action_login);
        MenuItem inboxItem = menu.findItem(R.id.action_inbox);

        if(DataParser.isUserLoggedIn(context)) {
            //Disable log-in icon
            loginItem.setVisible(false);
            //User logged in, enable sign out option
            signOutItem.setVisible(true);
            //Add inbox item
            inboxItem.setEnabled(true);
            inboxItem.setVisible(true);

            if(DataParser.getMessagesAmount(context) > 0)
                inboxItem.setIcon(R.drawable.ic_action_unread);
            else
                inboxItem.setIcon(R.drawable.ic_action_email);
        }
        else if(!DataParser.isUserLoggedIn(context)) {
            //User logged out, disable sign out option
            signOutItem.setVisible(false);
            //Remove inbox item
            inboxItem.setVisible(false);

            loginItem.setIcon(R.drawable.ic_action_person);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: //If the up button was selected, go back to parent activity
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_login:
                if(!DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context))
                    startActivity(new Intent(this, LoginActivity.class));
                return true;
            case R.id.action_inbox:
                if(DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context)) {
                    Intent getMessageIntent = new Intent(this, Messages.class);
                    getMessageIntent.putExtra(Messages.MESSAGE_TYPE, Messages.RECEIVED_MESSAGES);
                    startActivity(getMessageIntent);
                }
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override //Saves data to be used upon recreation of activity. Prevents additional network connections.
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SAVED_POST, thisPost);
//        outState.putParcelable(SAVED_IMAGE_1, ((BitmapDrawable)image.getDrawable()).getBitmap());
//        outState.putParcelable(SAVED_IMAGE_2, ((BitmapDrawable)image.getDrawable()).getBitmap());
//        outState.putParcelable(SAVED_IMAGE_3, ((BitmapDrawable)image.getDrawable()).getBitmap());
//        outState.putParcelable(SAVED_IMAGE_4, ((BitmapDrawable)image.getDrawable()).getBitmap());
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onResume() {
        invalidateOptionsMenu(); //Refreshes the ActionBar menu when activity is resumed

        if(DataParser.isUserLoggedIn(this) && DataParser.isNetworkAvailable(this)) { //if user is already logged in, allow user to contact
            new PollMessagesTask(this).execute();
            contact.setVisibility(View.VISIBLE);
            contact_nologin.setVisibility(View.GONE);
        }
        super.onResume();
    }

    private void signOut(){
        if(DataParser.isNetworkAvailable(this))
            new LogoutTask(this).execute(); //Starts asynchronous sign out
        invalidateOptionsMenu();
    }

    private void processClick(int index){
        imgURLs = new String[imageCount];
        for(int i=0; i < imageCount; i++)
            generateValidImgURL(i);
        if(imgURLs.length > 0) {
            Intent imgDialog = new Intent(ShowPage.this, ImageDialog.class);
            imgDialog.putExtra(IMGSRC, imgURLs);
            imgDialog.putExtra(INDEX, index);
            imgDialog.putExtra(IDENTIFIER, identifier);
            startActivity(imgDialog);
        }
    }

    private String generateImgURL(int index){
        String schoolID = DataParser.getSharedStringPreference(this, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
        String imgUrl = "post_images/"+schoolID+"/";
        imgUrl = imgUrl+identifier+"-"+index+".jpeg";

        return imgUrl;
    }

    private void generateValidImgURL(int index){
        imgURLs[index] = generateImgURL(index);
    }

    private void createMessageDialog() {
        //Custom Message View
        LayoutInflater inflater = this.getLayoutInflater();
        View messageView = inflater.inflate(R.layout.activity_message_dialog, null);
        final EditText editText = ((EditText)messageView.findViewById(R.id.post_message));
        editText.setText(message);

        //Adds color to Contact title
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder();
        String t = getString(R.string.contact)+": ";
        String u = user.getText().toString();

        SpannableString string = new SpannableString(t);
        stringBuilder.append(string);
        string = new SpannableString(u);
        string.setSpan(new ForegroundColorSpan(Color.BLACK), 0, u.length(), 0);
        stringBuilder.append(string);

        //Creates dialog popup to contact user
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(stringBuilder)
                .setView(messageView)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new SendMessageTask().execute(editText.getText().toString());
                        dialogInterface.dismiss();

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        dialog = builder.create();

        contact.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });
    }

    //Retrieves full post information
    private class FullPostTask extends AsyncTask<String, Void, Post> {

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected Post doInBackground(String... identifier) {
            DataParser database = new DataParser(context);
            Post post = null;

            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
                post = database.getFullPost(identifier[0], schoolID);
            } catch (Exception e){
                Log.e(TAG, "Retrieving full post", e);
            }

            return post;
        }

        @Override
        protected void onPostExecute(Post post) {
            thisPost = post;
            progress.setVisibility(View.INVISIBLE);

            title.setVisibility(View.VISIBLE);
            details.setVisibility(View.VISIBLE);
            user.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
            price.setVisibility(View.VISIBLE);

            if(DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context)) { //if user is already logged in, allow user to contact
                contact.setVisibility(View.VISIBLE);
                contact_nologin.setVisibility(View.GONE);
            }

            title.setText(post.getTitle());
            details.setText(post.getDetails());
            user.setText(post.getAuthor());
            date.setText(post.getDate());
            price.setText("$"+post.getPrice());

            createMessageDialog();
        }
    }

    //Sends message to user
    private class SendMessageTask extends AsyncTask<String, Void, String> {
        private DataParser database;

        @Override
        protected String doInBackground(String... message) {
            database = new DataParser(context);
            String response = context.getString(R.string.message_failed);

            try {
                response = database.messageUser(thisPost.getAuthor(), thisPost.getTitle(), message[0]);
            } catch (IOException e) {
                Log.e(TAG, "Messaging user", e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
        }
    }

    //TODO: In the future receive amount of images belonging to current post. Then use that to predict amount of image urls to generate. Then delete this class
    public class SpecialImageRetrievalTask extends AsyncTask<String, Void, Bitmap>{
        private final String TAG = "ASYNCTASK:SPECIALImageRetrieval";
        private ImageView imgView;
        private String identifier;
        private int index;
        private DiskLruImageCache imageCache;

        public SpecialImageRetrievalTask(ImageView _imgView, String _identifier, int _index){
            imgView = _imgView;
            identifier = _identifier;
            index = _index;
        }

        @Override
        protected Bitmap doInBackground(String... imgURL) {
            Bitmap bm = null;
            try {
                String schoolID = DataParser.getSharedStringPreference(context, DataParser.PREFS_SCHOOL, DataParser.S_PREF_SHORT);
                String key = identifier+"_"+index;

                imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.IMAGE_DIRECTORY);
                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

                if(bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(imgURL[0]);

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                Log.e(TAG, "Retrieving post image", e);
            }
            finally{
                imageCache.close();
            }

            return bm;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null) {
                imgView.setVisibility(View.VISIBLE);
                imgView.setImageBitmap(bitmap);
                imageCount++;
            }
        }
    }
}
