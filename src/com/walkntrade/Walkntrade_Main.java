package com.walkntrade;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.walkntrade.io.DataParser;

public class Walkntrade_Main extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splash);
		
		checkForConnection();

        Button retry = (Button)findViewById(R.id.retryButton);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkForConnection();
            }
        });

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.feedback, menu);
		return true;
	}

    public void checkForConnection(){
        if(DataParser.isNetworkAvailable(this)) { //Checks if device has internet or mobile connection
            if(DataParser.getSchoolLongPref(this) != null) //There is a school preference
                startActivity(new Intent(this, SchoolPage.class)); //Starts SchoolPage Activity
            else
                startActivity(new Intent(this, Selector.class)); //Starts Selector (Select/Change School) activity

            finish(); //Closes this activity
        }
    }

}

//Some images were auto-generated and resized with Android Asset Studio
