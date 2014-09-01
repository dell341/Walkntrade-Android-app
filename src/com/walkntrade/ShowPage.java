package com.walkntrade;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.walkntrade.asynctasks.LogoutTask;
import com.walkntrade.fragments.Fragment_ContactUser;
import com.walkntrade.fragments.Fragment_Post;
import com.walkntrade.io.DataParser;
import com.walkntrade.posts.Post;

public class ShowPage extends Activity implements Fragment_Post.ContactUserListener {

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
        args.putBoolean(Fragment_Post.TWO_PANE, false);

        if(savedInstanceState != null){
            twoPaneUsedFirst = savedInstanceState.getBoolean(TWOPANEBOOL);
            onePaneCreated = savedInstanceState.getBoolean(ONEPANEBOOL);
        }

        if(findViewById(R.id.frame_layout) != null) {
         //If FrameLayout is not null, one-pane layout is being used
            if(savedInstanceState != null) { //Prevents activity from adding infinite amount of fragments on top of one another

                if(twoPaneUsedFirst && !onePaneCreated) { //If layout started in two-pane, one-pane doesn't yet exist. Create it.
                    onePaneCreated = true;
                    Fragment_Post postFragment = new Fragment_Post();
                    postFragment.setArguments(args);

                    getFragmentManager().beginTransaction().add(R.id.frame_layout, postFragment).commit();
                }

                return;
            }

            Fragment_Post postFragment = new Fragment_Post();
            postFragment.setArguments(args);

            getFragmentManager().beginTransaction().add(R.id.frame_layout, postFragment).commit();
        }
        else { //Two-pane layout is being used

            if(!twoPaneUsedFirst && !onePaneCreated)
                twoPaneUsedFirst = true;

            args.putString(Fragment_ContactUser.TITLE, thisPost.getTitle());
            args.putString(Fragment_ContactUser.USER, thisPost.getUser());
            args.putBoolean(Fragment_Post.TWO_PANE, true);

            Fragment_Post postFragment = new Fragment_Post();
            Fragment_ContactUser contactFragment = new Fragment_ContactUser();

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
            case R.id.action_privacy_feedback:
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
        Fragment_ContactUser fragment = new Fragment_ContactUser();

        Bundle args = new Bundle();
        args.putString(Fragment_ContactUser.USER, user);
        args.putString(Fragment_ContactUser.TITLE, title);
        fragment.setArguments(args);

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
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
