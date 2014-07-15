package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
    private ListView listOfSchools;
    private EditText schoolSearch;
    private ProgressBar pBar;
	private Context context;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selector);
        context = getApplicationContext();

        noResults = (TextView) findViewById(R.id.noResults);
        listOfSchools = (ListView) findViewById(R.id.schoolList);
        schoolSearch = (EditText) findViewById(R.id.schoolSearch);
        pBar = (ProgressBar) findViewById(R.id.progressBar);

        listOfSchools.setOnItemClickListener(this);

        //Search with a click
        schoolSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String query = schoolSearch.getText().toString();
                    new SchoolNameTask(context, pBar, noResults, listOfSchools).execute(query);
                    return true;
                }

                return false;
            }
        });

        //Search on text change
        schoolSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String query = editable.toString();
                if(!query.isEmpty() && query.length() > 1) //Do not perform a blank or one letter search on text change
                    new SchoolNameTask(context, pBar, noResults, listOfSchools).execute(query);
            }
        });
	}

    //Gets the item selected from the ListView
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String schoolName = ((TextView)view).getText().toString();

        DataParser database = new DataParser(context);
        database.setSharedStringPreference(DataParser.PREFS_SCHOOL, DataParser.S_PREF_LONG, schoolName);

        Intent schoolPage = new Intent(context, SchoolPage.class);
        startActivity(schoolPage);
        finish(); //Close this activity. App will now start-up from preferred school
    }

}
