package com.walkntrade.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.adapters.item.ConversationItem;

import java.util.List;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MessageConversationAdapter extends BaseAdapter {

    private static final String TAG = "MessageConversationAdapter";

    private Context context;
    private List<ConversationItem> items;

    public MessageConversationAdapter(Context context, List<ConversationItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    public boolean addItem(ConversationItem item) {
        return items.add(item);
    }

    @Override
    public ConversationItem getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView;
        ConversationItem currentItem = getItem(position);

        if (currentItem.isSentFromMe())
            messageView = inflater.inflate(R.layout.item_message_user_me, parent, false);
        else
            messageView = inflater.inflate(R.layout.item_message_user_other, parent, false);

        ImageView userImage = (ImageView) messageView.findViewById(R.id.user_image);
        ProgressBar progressBar = (ProgressBar) messageView.findViewById(R.id.progressBar);
        TextView contents = (TextView) messageView.findViewById(R.id.message_contents);
        TextView user = (TextView) messageView.findViewById(R.id.user);
        TextView date = (TextView) messageView.findViewById(R.id.date);

        if (currentItem.hasAvatar())
            userImage.setImageBitmap(currentItem.getAvatar());
        if(currentItem.isSentFromThisDevice()) //If this message was sent from this device. Show a progress bar until it is delivered.
            progressBar.setVisibility( (currentItem.isDelivered() ? View.GONE : View.VISIBLE));

        contents.setText(currentItem.getContents());
        user.setText(currentItem.getSenderName());
        date.setText(currentItem.getDate());

        return messageView;
    }
}
