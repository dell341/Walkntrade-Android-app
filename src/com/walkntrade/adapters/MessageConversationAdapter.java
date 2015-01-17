package com.walkntrade.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.adapters.item.ConversationItem;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MessageConversationAdapter extends BaseAdapter {

    private static final String TAG = "MessageConversationAdapter";
    private static final String CHAT_ME = "chat_me_item";
    private static final String CHAT_OTHER = "chat_other_item";

    private Context context;
    private ArrayList<ConversationItem> items;

    public MessageConversationAdapter(Context context, ArrayList<ConversationItem> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    public boolean hasItems() {
        return items != null && items.size() >0;
    }

    public boolean addItem(ConversationItem item) {
        return items.add(item);
    }

    public boolean addItems(List<ConversationItem> i) {
        return items.addAll(i);
    }

    @Override
    public ConversationItem getItem(int i) {
        return items.get(i);
    }

    public int getIndexOfItem(ConversationItem item) {
        return items.indexOf(item);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public ArrayList<ConversationItem> getItems() {
        return items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView;
        ViewHolder holder;
        ConversationItem currentItem = getItem(position);

        String tag = (currentItem.isSentFromMe() ? CHAT_ME : CHAT_OTHER);

        //Use the recycled view (convert view) if it is not null, and is compatible with the new view
        if (convertView != null && tag.equals(convertView.getTag())) {
            messageView = convertView;
            holder = (ViewHolder) convertView.getTag(R.id.holder);
        } else {
            if (currentItem.isSentFromMe())
                messageView = inflater.inflate(R.layout.item_message_user_me, parent, false);
            else
                messageView = inflater.inflate(R.layout.item_message_user_other, parent, false);

            holder = new ViewHolder();
            holder.userImage = (ImageView) messageView.findViewById(R.id.user_image);
            holder.progressBar = (ProgressBar) messageView.findViewById(R.id.progressBar);
            holder.contents = (TextView) messageView.findViewById(R.id.message_contents);
            holder.user = (TextView) messageView.findViewById(R.id.user);
            holder.date = (TextView) messageView.findViewById(R.id.date);

            messageView.setTag(R.id.holder, holder);
        }

        messageView.setTag(tag);

        holder.contents.setText(currentItem.getContents());
        holder.user.setText(currentItem.getSenderName());
        holder.date.setText(currentItem.getDisplayableDateTime());

        if (currentItem.hasAvatar())
            holder.userImage.setImageBitmap(currentItem.getAvatar());
        if (currentItem.isSentFromThisDevice()) {//If this message was sent from this device. Show a progress bar until it is delivered.
            holder.progressBar.setVisibility((currentItem.isDelivered() ? View.GONE : View.VISIBLE));
            if (currentItem.hasMessageFailed()) {
                holder.date.setText(currentItem.getErrorMessage());
                holder.date.setTextColor(context.getResources().getColor(R.color.red));
                holder.progressBar.setVisibility(View.GONE);
            }
        }

        return messageView;
    }

    private static class ViewHolder { //Increase efficiency by decreasing the amount of calls to findViewById
        public ImageView userImage;
        public ProgressBar progressBar;
        TextView contents;
        TextView user;
        TextView date;
    }
}
