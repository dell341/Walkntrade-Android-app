package com.walkntrade.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.walkntrade.MessageObject;
import com.walkntrade.R;

import java.util.List;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class MessageAdapter extends ArrayAdapter<MessageObject>{

    public MessageAdapter(Context context, List<MessageObject> messages){
        super(context, R.layout.item_message, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView;

        if(convertView == null)
            messageView = inflater.inflate(R.layout.item_message, parent, false);
        else
            messageView = convertView;

        MessageObject message = getItem(position);
        TextView subject = (TextView) messageView.findViewById(R.id.message_subject);
        TextView user = (TextView) messageView.findViewById(R.id.message_user);
        TextView contents = (TextView) messageView.findViewById(R.id.message_contents);

        subject.setText(message.getSubject());
        user.setText(message.getUser());
        contents.setText(message.getContents());

        if(message.isUnRead()) //If message is unread, highlight it
            messageView.setBackgroundResource(R.drawable.unread_message_selector);

        return messageView;
    }
}
