package com.walkntrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.walkntrade.gcm.GcmIntentService;
import com.walkntrade.io.DataParser;

import java.io.IOException;

public class ShowMessage extends Activity {

    private static final String TAG = "ShowMessage";
    public static final String MESSAGE_OBJECT = "message_object";

    private Context context;
    private int messageType;
    private TextView subject, user;
    private Button button;
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_message);

        context = getApplicationContext();
        subject = (TextView) findViewById(R.id.message_subject);
        TextView contents = (TextView) findViewById(R.id.message_contents);
        user = (TextView) findViewById(R.id.message_user);
        TextView date = (TextView) findViewById(R.id.message_date);
        button = (Button) findViewById(R.id.button);

        Intent previousIntent = getIntent();
        Bundle bundledExtras = previousIntent.getExtras();

        if(bundledExtras != null) {
            Log.v(TAG, "Bundle: "+bundledExtras.toString());

            MessageObject message = getIntent().getParcelableExtra(MESSAGE_OBJECT);
            messageType = getIntent().getIntExtra(Messages.MESSAGE_TYPE, -1);

            GcmIntentService.resetNotfCounter(context); //Clears out all message notifications in Status Bar

            if (messageType == Messages.RECEIVED_MESSAGES)
                getActionBar().setTitle(getString(R.string.received_message));
            else {
                getActionBar().setTitle(getString(R.string.sent_message));
                button.setVisibility(View.GONE);
            }

            new GetMessageTask().execute(message.getId());

            subject.setText(message.getSubject());
            contents.setText(message.getContents());
            user.setText(message.getUser());
            date.setText(message.getDate());

            createMessageDialog();

            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        else
            Log.v(TAG, "Bundle is null");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.privacy_feedback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Used to support Android 15 and below
            case android.R.id.home: //If the up button was selected, go back to parent activity
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_privacy_feedback:
                startActivity(new Intent(this, Privacy_Feedback.class));
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

        String title = getString(R.string.contact) + ": " + user.getText().toString();

        //Creates dialog popup to contact user
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setView(messageView)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String message = editText.getText().toString();

                        if (!message.isEmpty()) {
                            new SendMessageTask().execute(message);
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

    //Used just to show message as read: Server counts message as read, only when this intent is called
    private class GetMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... id) {
            DataParser database = new DataParser(context);

            try {
                database.getMessage(id[0]);
            } catch (Exception e) {
                Log.e(TAG, "Getting Single Message", e);
            }

            return null;
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
                response = database.messageUser(user.getText().toString(), "RE:" + subject.getText().toString(), message[0]);
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
