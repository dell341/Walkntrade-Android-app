package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.asynctasks.SchoolNameTask;
import com.walkntrade.io.DataParser;

public class Selector extends Activity implements OnItemClickListener {

    private String TAG = "Selector"; //Used for Log messages

    private TextView noResults;
    private ListView schoolList;
    private EditText editText;
    private ProgressBar progressBar;
	private Context context;
    private SchoolNameTask asyncTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selector);
        context = getApplicationContext();

        noResults = (TextView) findViewById(R.id.noResults);
        schoolList = (ListView) findViewById(R.id.schoolList);
        editText = (EditText) findViewById(R.id.schoolSearch);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        asyncTask = new SchoolNameTask(context, progressBar, noResults, schoolList);

        schoolList.setOnItemClickListener(this);

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

}
