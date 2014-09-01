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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
    private String user, subject, message;
    private EditText messageContents;
    private Button button;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_user, container, false);

        context = getActivity().getApplicationContext();
        user = getArguments().getString(USER);
        subject = getArguments().getString(TITLE);

        TextView contactUser = (TextView) rootView.findViewById(R.id.contactUser);
        messageContents = (EditText) rootView.findViewById(R.id.message_contents);
        button = (Button) rootView.findViewById(R.id.button);

        //If user has no phone number on their account. Include a message without the phone number
        //TODO: Add checkbox to add or remove phone number
        if(DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE) == null || DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE).equals("0"))
            message = getString(R.string.post_message_content_no_phone);
        else
            message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.USER_PHONE));

        contactUser.setText(getString(R.string.contact)+" : "+user);
        messageContents.setText(message);

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
            Toast.makeText(context, response, Toast.LENGTH_SHORT).show();
            button.setEnabled(true);
        }
    }


}
