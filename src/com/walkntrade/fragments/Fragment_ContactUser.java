package com.walkntrade.fragments;

import android.app.Fragment;
import android.content.Context;
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

import java.io.IOException;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class Fragment_ContactUser extends Fragment {

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
        messageContents = (EditText) rootView.findViewById(R.id.message_contents);
        CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.checkBoz);
        button = (Button) rootView.findViewById(R.id.button);

        //If user has no phone number on their account. Include a message without the phone number
        if(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE) == null || DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE).equals("0"))
            message = getString(R.string.post_message_content_no_phone);
        else {
            message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE));
            checkBox.setEnabled(true);
            checkBox.setChecked(true);
        }

        contactUser.setText(getString(R.string.contacting_user)+" "+user);
        messageContents.setText(message);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE));
                    messageContents.setText(message);
                }
                else {
                    message = getString(R.string.post_message_content_no_phone);
                    messageContents.setText(message);
                }
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = messageContents.getText().toString();
                new SendMessageTask().execute();
            }
        });

        return rootView;
    }

    //Sends message to user
    private class SendMessageTask extends AsyncTask<Void, Void, String> {
        private DataParser database;

        @Override
        protected void onPreExecute() {
            button.setEnabled(false);
        }

        @Override
        protected String doInBackground(Void... voids) {
            database = new DataParser(context);
            String response = context.getString(R.string.message_failed);

            try {
                response = database.messageUser(user, subject, message);
            } catch (IOException e) {
                Log.e(TAG, "Messaging user", e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {

            if(response.equals("success")){
                messageFeedback.setTextColor(getResources().getColor(R.color.holo_blue));
                messageFeedback.setText(response);
            }
            else {
                messageFeedback.setText(response);
            }

            button.setEnabled(true);
        }
    }


}
