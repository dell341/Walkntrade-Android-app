package com.walkntrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class Privacy_Feedback extends ActionBarActivity implements AdapterView.OnItemClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy__feedback);

        ListView listView = (ListView) findViewById(R.id.listView);

        String[] items = {getString(R.string.send_feedback), getString(R.string.privacy_policy)};
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
        listView.setOnItemClickListener(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
       switch (item.getItemId()){
           case android.R.id.home: finish(); return true;
           default: return super.onOptionsItemSelected(item);
       }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch(position){
            case 0: startActivity(new Intent(this, Feedback.class)); break;
            case 1: startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.url_privacy)))); break;
        }
    }
}
