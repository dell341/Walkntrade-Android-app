package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.asynctasks.PollMessagesTask;
import com.walkntrade.io.DataParser;

import java.io.IOException;
import java.util.ArrayList;

public class ShowMessage extends Activity {

    private static final String TAG = "ShowMessage";
    public static final String MESSAGE_ID = "id_of_message";

    private Context context;
    private ProgressBar progressBar;
    private TextView subject, contents, user, date;
    private Button button;
    private AlertDialog dialog;
    private int messageType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_message);

        context = getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        subject = (TextView) findViewById(R.id.message_subject);
        contents = (TextView) findViewById(R.id.message_contents);
        user = (TextView) findViewById(R.id.message_user);
        date = (TextView) findViewById(R.id.message_date);
        button = (Button) findViewById(R.id.button);

        String id = getIntent().getStringExtra(MESSAGE_ID);
        messageType = getIntent().getIntExtra(Messages.MESSAGE_TYPE, -1);

        new PollMessagesTask(this).execute();
        new GetMessageTask().execute(id);

        if(messageType == Messages.RECEIVED_MESSAGES)
            getActionBar().setTitle(getString(R.string.received_message));
        else {
            getActionBar().setTitle(getString(R.string.sent_message));
            button.setVisibility(View.GONE);
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                upIntent.putExtra(Messages.MESSAGE_TYPE, messageType);
                NavUtils.navigateUpTo(this, upIntent);
                return true;
            case R.id.action_feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createMessageDialog() {
        //Custom Message View
        LayoutInflater inflater = this.getLayoutInflater();
        View messageView = inflater.inflate(R.layout.activity_message_dialog, null);
        final EditText editText = (EditText) messageView.findViewById(R.id.post_message);
        editText.setText(null);

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
                        String message = editText.getText().toString();

                        if (!message.isEmpty()) {
                            new SendMessageTask().execute();
                            dialogInterface.dismiss();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        dialog = builder.create();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });
    }

    private class GetMessageTask extends AsyncTask<String, Void, ArrayList<MessageObject>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected ArrayList<MessageObject> doInBackground(String... id) {
            DataParser database = new DataParser(context);
            ArrayList<MessageObject> message = new ArrayList<MessageObject>();

            try {
                message = database.getMessages(messageType, Integer.parseInt(id[0]));
            }catch (Exception e){
               Log.e(TAG, "Getting Single Message", e);
            }

            return message;
        }

        @Override
        protected void onPostExecute(ArrayList<MessageObject> messageObject) {
            progressBar.setVisibility(View.INVISIBLE);

            MessageObject message = messageObject.get(0);

            subject.setText(message.getSubject());
            contents.setText(message.getContents());
            user.setText(message.getUser());
            date.setText(message.getDate());

            createMessageDialog();
        }
    }

    //Sends message to user
    private class SendMessageTask extends AsyncTask<String, Void, String> {
        private DataParser database;

        @Override
        protected String doInBackground(String... message) {
            database = new DataParser(context);
            String response = context.getString(R.string.message_failed);

            try {
                response = database.messageUser(user.getText().toString(), "RE:"+subject.getText().toString(), message[0]);
            } catch (IOException e) {
                Log.e(TAG, "Messaging user", e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
        }
    }
}
