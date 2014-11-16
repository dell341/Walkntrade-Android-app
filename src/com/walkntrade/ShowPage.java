package com.walkntrade;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
    private static final String TWOPANEBOOL = "saved_instance_two_pane";
    private static final String ONEPANEBOOL = "saved_instance_one_pane";

    private Context context;
    private boolean twoPaneUsedFirst = false;
    private boolean onePaneCreated = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_show_page);

        context = getApplicationContext();
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Post thisPost = getIntent().getParcelableExtra(SchoolPage.SELECTED_POST);
        Bundle args = new Bundle();
        args.putParcelable(SchoolPage.SELECTED_POST, thisPost);
        args.putBoolean(PostFragment.TWO_PANE, false);

        if(savedInstanceState != null){
            twoPaneUsedFirst = savedInstanceState.getBoolean(TWOPANEBOOL);
            onePaneCreated = savedInstanceState.getBoolean(ONEPANEBOOL);
        }

        if(findViewById(R.id.frame_layout) != null) {
         //If FrameLayout is not null, one-pane layout is being used
            if(savedInstanceState != null) { //Prevents activity from adding infinite amount of fragments on top of one another

                if(twoPaneUsedFirst && !onePaneCreated) { //If layout started in two-pane, one-pane doesn't yet exist. Create it.
                    onePaneCreated = true;
                    PostFragment postFragment = new PostFragment();
                    postFragment.setArguments(args);

                    getFragmentManager().beginTransaction().add(R.id.frame_layout, postFragment).commit();
                }

                return;
            }

            PostFragment postFragment = new PostFragment();
            postFragment.setArguments(args);

            getFragmentManager().beginTransaction().add(R.id.frame_layout, postFragment).commit();
        }
        else { //Two-pane layout is being used

            if(!twoPaneUsedFirst && !onePaneCreated)
                twoPaneUsedFirst = true;

            args.putString(ContactUserFragment.TITLE, thisPost.getTitle());
            args.putString(ContactUserFragment.USER, thisPost.getUser());
            args.putBoolean(PostFragment.TWO_PANE, true);

            PostFragment postFragment = new PostFragment();
            ContactUserFragment contactFragment = new ContactUserFragment();

            postFragment.setArguments(args);
            contactFragment.setArguments(args);

            getFragmentManager().beginTransaction().replace(R.id.postFragment, postFragment).commit();
            getFragmentManager().beginTransaction().replace(R.id.contactFragment, contactFragment).commit();
        }

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
            case android.R.id.home: //If the up button was selected, close this activity. Parent could be either search or school page
                finish();
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
                startActivity(new Intent(this, Privacy_Feedback.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        invalidateOptionsMenu(); //Refreshes the ActionBar menu when activity is resumed
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(TWOPANEBOOL, twoPaneUsedFirst);
        outState.putBoolean(ONEPANEBOOL, onePaneCreated);
    }

    @Override
    public void contactUser(String user, String title) {
        ContactUserFragment fragment = new ContactUserFragment();

        Bundle args = new Bundle();
        args.putString(ContactUserFragment.USER, user);
        args.putString(ContactUserFragment.TITLE, title);
        fragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.fragment_slide_in_left, R.animator.fragment_slide_off_left, R.animator.fragment_slide_in_right, R.animator.fragment_slide_off_right);
        transaction.replace(R.id.frame_layout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void signOut(){
        if(DataParser.isNetworkAvailable(this))
            new LogoutTask(this).execute(); //Starts asynchronous sign out

        invalidateOptionsMenu();
        //contact.setText(getString(R.string.contact_login));
    }
}
