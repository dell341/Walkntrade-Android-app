package com.walkntrade.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.adapters.item.ViewPostItem;

import java.util.List;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class ViewPostAdapter extends ArrayAdapter<ViewPostItem> {

    public ViewPostAdapter(Context _context, List<ViewPostItem> _items) {
        super(_context, R.layout.item_post_content, _items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View postItemView;

        ViewPostItem item = getItem(position);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //If item is header, use header layout
        if(item.isHeader()) {
            postItemView = inflater.inflate(R.layout.item_post_school, parent, false);

            TextView header = (TextView) postItemView.findViewById(R.id.drawer_header);
            header.setText(item.getContents());
        }
        else { //Item is a post, so use view post item layout
            postItemView = inflater.inflate(R.layout.item_post_content, parent, false);

            TextView postTitle = (TextView) postItemView.findViewById(R.id.view_post_title);
            postTitle.setText(item.getContents());
        }

        return postItemView;
    }

    @Override //Returns true if item is not a separator (non-selectable, non-clickable)
    //Prevents School names from being selected
    public boolean isEnabled(int position) {
        ViewPostItem item = getItem(position);

        //If item is not a header, it is selectable
        return !item.isHeader();
    }
}
