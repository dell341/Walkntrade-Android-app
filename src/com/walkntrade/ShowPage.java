package com.walkntrade;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.fragments.ContactUserFragment;
import com.walkntrade.fragments.PostFragment;
import com.walkntrade.io.DataParser;
import com.walkntrade.objects.Post;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class ShowPage extends Activity implements PostFragment.ContactUserListener {

    private String TAG = "ShowPage";
    private Context context;
    private Post thisPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_page);

        context = getApplicationContext();
        getActionBar().setDisplayHomeAsUpEnabled(true);

        thisPost = getIntent().getParcelableExtra(SchoolPage.SELECTED_POST);
        Bundle args = new Bundle();
        args.putParcelable(SchoolPage.SELECTED_POST, thisPost);

        if (savedInstanceState != null) { //Prevents activity from adding infinite amount of fragments on top of one another
            return;
        }

        PostFragment postFragment = new PostFragment();
        postFragment.setArguments(args);

        getFragmentManager().beginTransaction().add(R.id.frame_layout, postFragment).commit();

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

        if (DataParser.isUserLoggedIn(context)) {
            //Disable log-in icon
            loginItem.setVisible(false);
            //User logged in, enable sign out option
            signOutItem.setVisible(true);
            //Add inbox item
            inboxItem.setEnabled(true);
            inboxItem.setVisible(true);

//            if(DataParser.getMessagesAmount(context) > 0)
//                inboxItem.setIcon(R.drawable.ic_action_unread);
//            else
                inboxItem.setIcon(R.drawable.ic_chat_white);
        }
        else {
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
        switch (item.getItemId()) {
            case android.R.id.home: //If the up button was selected, close this activity. Parent could be either search or school page
                finish();
                return true;
            case R.id.action_login:
                if (!DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context))
                    startActivity(new Intent(this, LoginActivity.class));
                return true;
            case R.id.action_inbox:
                if (DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context)) {
                    Intent getMessageIntent = new Intent(this, Messages.class);
                    startActivity(getMessageIntent);
                }
                return true;
            case R.id.action_sign_out:
                signOut();
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, Privacy_Feedback.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        invalidateOptionsMenu(); //Refreshes the ActionBar menu when activity is resumed
        super.onResume();
    }

    @Override
    public void contactUser(String user, String title) {
        ContactUserFragment fragment = new ContactUserFragment();

        Bundle args = new Bundle();
        args.putParcelable(SchoolPage.SELECTED_POST, thisPost);
        fragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fragment_slide_in_left, R.animator.fragment_slide_off_left, R.animator.fragment_slide_in_right, R.animator.fragment_slide_off_right);
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void signOut() {
        if (DataParser.isNetworkAvailable(this))
            new LogoutTask(this).execute(); //Starts asynchronous sign out

        invalidateOptionsMenu();
    }
}
