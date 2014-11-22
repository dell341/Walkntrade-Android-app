package com.walkntrade.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.StatusCodeParser;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class ContactUserFragment extends Fragment {

    private static final String TAG = "FRAGMENT:ContactUser";
    public static final String USER = "user_to_message";
    public static final String TITLE = "title_of_message";

    private Context context;
    private TextView messageFeedback;
    private String user, subject, message;
    private EditText messageContents;
    private Button button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_user, container, false);

        context = getActivity().getApplicationContext();
        user = getArguments().getString(USER);
        subject = getArguments().getString(TITLE);

        messageFeedback = (TextView) rootView.findViewById(R.id.message_error);
        TextView contactUser = (TextView) rootView.findViewById(R.id.contactUser);
        messageContents = (EditText) rootView.findViewById(R.id.edit_message_contents);
        CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.checkBoz);
        button = (Button) rootView.findViewById(R.id.button);

        //If user has no phone number on their account. Include a message without the phone number
        if(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE) == null || DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE).equals("0"))
            message = getString(R.string.post_message_content_no_phone);
        else {
            message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE));
            checkBox.setEnabled(true);
            checkBox.setChecked(true);
        }

        contactUser.setText(getString(R.string.contacting_user)+" "+user);
        messageContents.setText(message);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE));
                    messageContents.setText(message);
                }
                else {
                    message = getString(R.string.post_message_content_no_phone);
                    messageContents.setText(message);
                }
            }
        });

        if(DataParser.isUserLoggedIn(context)) { //If user is logged in

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageFeedback.setVisibility(View.INVISIBLE);
                    message = messageContents.getText().toString();

                    //Confirms if user wants to send a message
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.send_message))
                            .setMessage(R.string.send_message_quest)
                            .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    new SendMessageTask().execute();
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create().show();
                }
            });
        }
        else
            button.setEnabled(false);

        return rootView;
    }

    //Sends message to user
    private class SendMessageTask extends AsyncTask<Void, Void, Integer> {
        private DataParser database;

        @Override
        protected void onPreExecute() {
            button.setEnabled(false);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            database = new DataParser(context);
            Integer serverResponse = StatusCodeParser.CONNECT_FAILED;

            try {
                serverResponse = database.messageUser(user, subject, message);
            } catch (IOException e) {
                Log.e(TAG, "Messaging user", e);
            }
            return serverResponse;
        }

        @Override
        protected void onPostExecute(Integer response) {

            if(response == StatusCodeParser.STATUS_OK){
                messageFeedback.setTextColor(getResources().getColor(R.color.holo_blue));
                messageFeedback.setText(context.getString(R.string.message_success));
            }
            else {
                messageFeedback.setTextColor(getResources().getColor(R.color.red));
                messageFeedback.setText(StatusCodeParser.getStatusString(context, response));
            }

            messageFeedback.setVisibility(View.VISIBLE);
            button.setEnabled(true);
        }
    }


}
