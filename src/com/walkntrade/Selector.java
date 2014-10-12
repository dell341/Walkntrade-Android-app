package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.asynctasks.SchoolNameTask;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DiskLruImageCache;

import java.io.IOException;

public class Selector extends Activity implements OnItemClickListener {

    private String TAG = "Selector"; //Used for Log messages
    private static final String SAVED_BACKGROUND = "background_image";

    private TextView noResults;
    private ListView schoolList;
    private ImageView imageView;
    private EditText editText;
    private ProgressBar progressBar;
	private Context context;
    private SchoolNameTask asyncTask;

    private Bitmap background;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selector);
        context = getApplicationContext();

        imageView = (ImageView) findViewById(R.id.background);
        noResults = (TextView) findViewById(R.id.noResults);
        schoolList = (ListView) findViewById(R.id.schoolList);
        editText = (EditText) findViewById(R.id.schoolSearch);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        asyncTask = new SchoolNameTask(context, progressBar, noResults, schoolList);

        schoolList.setOnItemClickListener(this);

        if(savedInstanceState != null) {
            Bitmap bm = savedInstanceState.getParcelable(SAVED_BACKGROUND);

            if(bm == null)
                new DownloadBackgroundTask().execute();
            else {
                background = bm;
                imageView.setImageBitmap(bm);
            }
        }
        else
           new DownloadBackgroundTask().execute();

        //Search with a click
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = editText.getText().toString();
                    search(query);
                    return true;
                }

                return false;
            }
        });

        //Search on text change
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString();
                if (!query.isEmpty() && query.length() > 1) { //Do not perform a blank or one letter search on text change
                    search(query);
                }
            }
        });
	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putParcelable(SAVED_BACKGROUND, background);
        } catch (NullPointerException e) {
            Log.e(TAG, "Orientation change before image downloaded");
        }
    }

    //Gets the item selected from the ListView
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String schoolName = ((TextView)view).getText().toString();

        DataParser database = new DataParser(context);
        database.setSharedStringPreference(DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_LONG, schoolName);

        Intent schoolPage = new Intent(context, SchoolPage.class);
        startActivity(schoolPage);
        finish(); //Close this activity. App will now start-up from preferred school
    }

    private void search(String query){
        if(asyncTask.cancel(true) || asyncTask.getStatus() == AsyncTask.Status.FINISHED) { //Attempts run new search by cancelling a running task or if the previous has finished
            asyncTask = new SchoolNameTask(context, progressBar, noResults, schoolList);
            asyncTask.execute(query);
        }
    }

    private class DownloadBackgroundTask extends AsyncTask<Void, Void, Bitmap>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            Bitmap bm;
            String key = "background_0";
            String url = context.getResources().getString(R.string.background_image_1);

            DiskLruImageCache imageCache = new DiskLruImageCache(context, DiskLruImageCache.DIRECTORY_OTHER_IMAGES);
            bm = imageCache.getBitmapFromDiskCache(key);

            try {
                if (bm == null)
                    bm = DataParser.loadBitmap(context.getResources().getString(R.string.images_directory) + url);

                imageCache.addBitmapToCache(key, bm);
            } catch(IOException e) {
                Log.e(TAG, "Retrieving image", e);
            }
            finally {
                imageCache.close();
            }

            return bm;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if(bitmap != null) {
                background = bitmap;
                imageView.setImageBitmap(bitmap);
            }

        }
    }

}
