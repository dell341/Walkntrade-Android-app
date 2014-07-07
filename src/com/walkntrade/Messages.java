package com.walkntrade;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.adapters.MessageAdapter;
import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.ArrayList;

public class Messages extends Activity implements AdapterView.OnItemClickListener{

    private static final String TAG = "Messages";
    public static final String MESSAGE_TYPE = "reading_inbox_or_sent";
    public static final int RECEIVED_MESSAGES = 0;
    public static final int SENT_MESSAGES = 1;

    private Context context;
    private ProgressBar progressBar;
    private TextView noResults;
    private ListView messageList;
    private int messageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        noResults = (TextView) findViewById(R.id.noResults);
        messageList = (ListView) findViewById(R.id.messageList);
        messageList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        messageList.setMultiChoiceModeListener(new MultiChoiceListener());

        messageType = getIntent().getIntExtra(MESSAGE_TYPE, 0);

        if(messageType == RECEIVED_MESSAGES)
            getActionBar().setTitle(getString(R.string.received_messages));
        else
            getActionBar().setTitle(getString(R.string.sent_messages));

        new PollMessagesTask(this).execute();
        new GetMessagesTask().execute();

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    protected void onResume() {
        if(messageList.getAdapter() != null)
            new GetMessagesTask().execute();

        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MessageObject message = (MessageObject)parent.getItemAtPosition(position);

        Intent showMessageIntent = new Intent(this, ShowMessage.class);
        showMessageIntent.putExtra(ShowMessage.MESSAGE_ID, message.getId());
        showMessageIntent.putExtra(MESSAGE_TYPE, messageType);
        startActivity(showMessageIntent);
    }

    private class MultiChoiceListener implements AbsListView.MultiChoiceModeListener{
        ArrayList<String> messageIds = new ArrayList<String>();
        private int count = 0;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_post, menu);
            return true;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean selected) {
            MessageAdapter adapter = (MessageAdapter) messageList.getAdapter();
            MessageObject item = adapter.getItem(position);

            if(selected) {
                messageIds.add(item.getId());
                count++;
            }
            else {
                messageIds.remove(item.getId());
                count--;
            }

            mode.setTitle(count+" message(s) selected");
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    new RemoveMessageTask().execute(messageIds);
                    mode.finish(); //Close the Contextual Action Bar
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            count = 0;
        }
    }

    private class GetMessagesTask extends AsyncTask<Void, Void, ArrayList<MessageObject>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<MessageObject> doInBackground(Void... params) {
            DataParser database = new DataParser(context);
            ArrayList<MessageObject> messages = new ArrayList<MessageObject>();

            try {
                messages = database.getMessages(messageType, -1);
            } catch (Exception e) {
                Log.e(TAG, "Get Messages", e);
            }

            return messages;
        }

        @Override
        protected void onPostExecute(ArrayList<MessageObject> messageObjects) {
            progressBar.setVisibility(View.GONE);
            messageList.setAdapter(null);

            if(messageObjects.isEmpty())
                noResults.setVisibility(View.VISIBLE);
            else {
                noResults.setVisibility(View.GONE);
                messageList.setAdapter(new MessageAdapter(context, messageObjects));
                messageList.setOnItemClickListener(Messages.this);
            }
        }
    }

    private class RemoveMessageTask extends AsyncTask<ArrayList<String>, Void, Void> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(ArrayList<String>... messagesToDelete) {
            DataParser database = new DataParser(context);

            try {
                for(String s : messagesToDelete[0])
                    database.removeMessage(s);
            } catch (IOException e) {
                Log.e(TAG, "Deleting message(s)", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            new GetMessagesTask().execute();
        }
    }
}
