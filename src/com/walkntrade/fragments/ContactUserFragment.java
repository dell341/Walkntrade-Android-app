package com.walkntrade.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.walkntrade.SchoolPage;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.SendMessageService;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.Post;

import java.io.IOException;

/*
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

public class ContactUserFragment extends Fragment {

    private static final String TAG = "ContactUser";

    private Context context;
    private Post thisPost;
    private TextView messageFeedback;
    private String message;
    private EditText messageContents;
    private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        context = getActivity().getApplicationContext();
        LocalBroadcastManager.getInstance(context).registerReceiver(messageStatusReceiver, new IntentFilter(SendMessageService.ACTION_CREATE_MESSAGE_THREAD));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_user, container, false);
        thisPost = getArguments().getParcelable(SchoolPage.SELECTED_POST);

        messageFeedback = (TextView) rootView.findViewById(R.id.message_error);
        TextView contactUser = (TextView) rootView.findViewById(R.id.contactUser);
        messageContents = (EditText) rootView.findViewById(R.id.edit_message_contents);
        final CheckBox checkBox = (CheckBox) rootView.findViewById(R.id.checkBox);
        button = (Button) rootView.findViewById(R.id.button);

        String user = thisPost.getUser();

        //If user has no phone number on their account. Include a message without the phone number
        String phoneNumber = DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE);
        if(phoneNumber == null || phoneNumber.isEmpty() || phoneNumber.equals("0") )
            message = getString(R.string.post_message_content_no_phone);
        else {
            message = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE));
            checkBox.setEnabled(true);
            checkBox.setChecked(true);
        }

        contactUser.setText(getString(R.string.contacting_user)+" "+ user);
        messageContents.setText(message);
        messageContents.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkBox.setEnabled(false); //If the user edits the default message, disable the phone number checkbox option
            }
        });

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

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageFeedback.setVisibility(View.INVISIBLE);

                    if(!DataParser.isUserLoggedIn(context)) {
                        messageFeedback.setTextColor(getResources().getColor(R.color.red));
                        messageFeedback.setText(context.getString(R.string.no_login));
                        messageFeedback.setVisibility(View.VISIBLE);
                        return;
                    }

                    message = messageContents.getText().toString();

                    //Confirms if user wants to send a message
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.send_message))
                            .setMessage(R.string.send_message_quest)
                            .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    button.setEnabled(false);

                                    Intent createMessage = new Intent(getActivity(), SendMessageService.class);
                                    createMessage.setAction(SendMessageService.ACTION_CREATE_MESSAGE_THREAD);
                                    createMessage.putExtra(SendMessageService.EXTRA_POST_OBSID, thisPost.getObsId());
                                    createMessage.putExtra(SendMessageService.EXTRA_MESSAGE_CONTENTS, message);
                                    getActivity().startService(createMessage); //Starts IntentService to create a new message thread. Different from AsyncTask in some ways
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

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageStatusReceiver);
    }

    //Listens for result from SendMessageService, handles result here
    private BroadcastReceiver messageStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SendMessageService.ACTION_CREATE_MESSAGE_THREAD)) {
                int serverResponse = intent.getIntExtra(SendMessageService.EXTRA_SERVER_RESPONSE, StatusCodeParser.CONNECT_FAILED);

                if(serverResponse == StatusCodeParser.STATUS_OK){
                    messageFeedback.setTextColor(getResources().getColor(R.color.holo_blue));
                    messageFeedback.setText(context.getString(R.string.message_success));
                }
                else {
                    messageFeedback.setTextColor(getResources().getColor(R.color.red));
                    messageFeedback.setText(StatusCodeParser.getStatusString(context, serverResponse));
                }

                messageFeedback.setVisibility(View.VISIBLE);
                button.setEnabled(true);
            }
        }
    };
}
