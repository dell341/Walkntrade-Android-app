package com.walkntrade.fragments;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
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
    private static final String SAVED_INSTANCE_MESSAGE_DIALOG = "saved_instance_state_message_dialog";

    private Context context;
    private Post thisPost;
    private TextView messageFeedback;
    private String message;
    private EditText messageContents;
    private Button button;

    private ProgressDialog progressDialog;
    private boolean messageDialogShowing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        final String messageNoPhone = getString(R.string.post_message_content_no_phone);
        final String messageWPhone = String.format(getString(R.string.post_message_content_phone), DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE));

        //If user has no phone number on their account. Include a message without the phone number
        final String phoneNumber = DataParser.getSharedStringPreference(context, DataParser.PREFS_USER, DataParser.KEY_USER_PHONE);

        if(phoneNumber == null || phoneNumber.isEmpty() || phoneNumber.equals("0")) {
            message = messageNoPhone;
            checkBox.setEnabled(false);
        }
        else {
            message = messageWPhone;
            checkBox.setEnabled(true);
            checkBox.setChecked(true);
        }
        contactUser.setText(getString(R.string.contacting_user)+" "+ user);

        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.sending_message));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(null);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        if(savedInstanceState != null) {
            messageDialogShowing = savedInstanceState.getBoolean(SAVED_INSTANCE_MESSAGE_DIALOG);

            if(messageDialogShowing)
                progressDialog.show();
        }

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
                if(s.toString().equals(messageNoPhone) && phoneNumber != null && !phoneNumber.isEmpty() && !phoneNumber.equals("0")) {
                    checkBox.setEnabled(true);
                    checkBox.setChecked(false);
                }
                else if(s.toString().equals(messageWPhone)) {
                    checkBox.setEnabled(true);
                    checkBox.setChecked(true);
                }
                else
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
                    messageFeedback.setText("");

                    if(!DataParser.isUserLoggedIn(context)) {
                        messageFeedback.setTextColor(getResources().getColor(R.color.red));
                        messageFeedback.setText(context.getString(R.string.no_login));
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
                                    progressDialog.show();
                                    messageDialogShowing = true;

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_INSTANCE_MESSAGE_DIALOG, messageDialogShowing);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        progressDialog.dismiss();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(messageStatusReceiver);
    }

    //Listens for result from SendMessageService, handles result here
    private BroadcastReceiver messageStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressDialog.dismiss();
            messageDialogShowing = false;

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
