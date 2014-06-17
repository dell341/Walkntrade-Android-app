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

import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;
import com.walkntrade.posts.Post;

import java.io.IOException;

public class ShowPage extends Activity {

    private String TAG = "ShowPage";
	public static String IMGSRC = "Link_For_Images";
	public static String IDENTIFIER = "Unique_Post_Id";
    public static String INDEX = "Image_Index";

    private Context context;
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
		String identifier = getIntent().getStringExtra(SchoolPage.SELECTED_POST);

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

        new FullPostTask().execute(identifier); //Retrieves full information for this post

        if(DataParser.getPhonePref(context) == null)
            message = getString(R.string.post_message_content_no_phone);
        else
            message = String.format(getString(R.string.post_message_content_phone), DataParser.getPhonePref(context));

        //Custom Message View
        LayoutInflater inflater = this.getLayoutInflater();
        View messageView = inflater.inflate(R.layout.activity_message_dialog, null);
        ((EditText)messageView.findViewById(R.id.post_message)).setText(message);

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
                        new SendMessageTask().execute();
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

        contact_nologin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ShowPage.this, LoginActivity.class));
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.show__page, menu);
		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: //If the up button was selected, go back to parent activity
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        if(DataParser.isUserLoggedIn(this) && DataParser.isNetworkAvailable(this)) { //if user is already logged in, allow user to contact
            contact.setVisibility(View.VISIBLE);
            contact_nologin.setVisibility(View.GONE);
        }
        super.onResume();
    }

    private void processClick(int index){
        imgURLs = new String[imageCount];
        for(int i=0; i < imageCount; i++)
            generateValidImgURL(i);
        if(imgURLs.length > 0) {
            Intent imgDialog = new Intent(ShowPage.this, ImageDialog.class);
            imgDialog.putExtra(IMGSRC, imgURLs);
            imgDialog.putExtra(INDEX, index);
            imgDialog.putExtra(IDENTIFIER, thisPost.getIdentifier());
            startActivity(imgDialog);
        }
    }

    private String generateImgURL(int index){
        String schoolID = DataParser.getSchoolPref(this);
        String imgUrl = "post_images/"+schoolID+"/";
        imgUrl = imgUrl+thisPost.getIdentifier()+"-"+index+".jpeg";

        return imgUrl;
    }

    private void generateValidImgURL(int index){
        imgURLs[index] = generateImgURL(index);
    }

    //Retrieves full post information
    private class FullPostTask extends AsyncTask<String, Void, Post> {
        @Override
        protected Post doInBackground(String... identifier) {
            DataParser database = new DataParser(context);
            Post post = null;

            try {
                String schoolID = DataParser.getSchoolPref(context);
                post = database.getFullPost(identifier[0], schoolID);
            } catch (Exception e){
                e.printStackTrace();
            }

            return post;
        }

        @Override
        protected void onPostExecute(Post post) {
            thisPost = post;
            progress.setVisibility(View.GONE);

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

                //Calls images to be displayed on show page

                //First Image
                String imgUrl = generateImgURL(0);
                Log.v(TAG, "IMAGE URL: " + imgUrl);
                new SpecialImageRetrievalTask(image, post.getIdentifier(), 0).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

                //Second Image
                imgUrl = generateImgURL(1);
                Log.v(TAG, "IMAGE URL: "+imgUrl);
                new SpecialImageRetrievalTask(image2, post.getIdentifier(), 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

                //Third Image
                imgUrl = generateImgURL(2);
                Log.v(TAG, "IMAGE URL: "+imgUrl);
                new SpecialImageRetrievalTask(image3, post.getIdentifier(), 2).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

                //Fourth Image
                imgUrl = generateImgURL(3);
                Log.v(TAG, "IMAGE URL: "+imgUrl);
                new SpecialImageRetrievalTask(image4, post.getIdentifier(), 3).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imgUrl);

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
        }
    }

    //Sends message to user
    private class SendMessageTask extends AsyncTask<Void, Void, String> {
        private DataParser database;

        @Override
        protected String doInBackground(Void... voids) {
            database = new DataParser(context);
            String response = context.getString(R.string.message_failed);

            try {
                response = database.messageUser(thisPost.getAuthor(), thisPost.getTitle(), message);
            } catch (IOException e) {
                e.printStackTrace();
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
                String schoolID = DataParser.getSchoolPref(context);
                String key = identifier+"_"+index;

                imageCache = new DiskLruImageCache(context, schoolID+DiskLruImageCache.IMAGE_DIRECTORY);
                bm = imageCache.getBitmapFromDiskCache(key); //Try to retrieve image from Cache

                if(bm == null) //If it doesn't exists, retrieve image from network
                    bm = DataParser.loadBitmap(imgURL[0]);

                imageCache.addBitmapToCache(key, bm); //Finally cache bitmap. Will override cache if already exists or write new cache
            } catch (IOException e) {
                e.printStackTrace();
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
