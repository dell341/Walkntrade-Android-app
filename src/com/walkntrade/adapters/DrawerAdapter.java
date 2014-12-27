package com.walkntrade.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.walkntrade.LoginActivity;
import com.walkntrade.R;
import com.walkntrade.adapters.item.DrawerItem;
import com.walkntrade.io.DataParser;

import java.util.HashMap;
import java.util.List;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class DrawerAdapter extends BaseExpandableListAdapter {

    private static final String TAG = "DrawerAdapter";

    private Context context;
    private List<DrawerItem> drawerItemParents;
    private HashMap<DrawerItem, List<DrawerItem>> drawerItemChildren;

	public DrawerAdapter(Context context, List<DrawerItem> drawerItemParents, HashMap<DrawerItem, List<DrawerItem>> drawerItemChildren) {
        this.context = context;
        this.drawerItemParents = drawerItemParents;
        this.drawerItemChildren = drawerItemChildren;
	}

    @Override //Amount of headers
    public int getGroupCount() {
        return drawerItemParents.size();
    }

    @Override //Amount of items for a specific header
    public int getChildrenCount(int groupPosition) {
        try {
            return drawerItemChildren.get(drawerItemParents.get(groupPosition)).size();
        }
        catch (NullPointerException e) {
            return 0;
        }
    }

    @Override //Return specific group
    public Object getGroup(int groupPosition) {
        return drawerItemParents.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return drawerItemChildren.get(drawerItemParents.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition*100;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getGroupId(groupPosition)+(childPosition*10)+10;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View drawerItemView;

        DrawerItem item = (DrawerItem) getGroup(groupPosition);
        LayoutInflater inflater = LayoutInflater.from(context);

        //If user item is being created, call the appropriate layout inflater
        if (item.isUser()) {
            drawerItemView = inflater.inflate(R.layout.item_drawer_user, parent, false);
            drawerItemView.setFocusable(false);

            ImageView icon = (ImageView) drawerItemView.findViewById(R.id.drawer_user);
            TextView content = (TextView) drawerItemView.findViewById(R.id.drawer_user_name);
            Button login = (Button) drawerItemView.findViewById(R.id.drawer_login);

            if(!DataParser.isUserLoggedIn(context)){
                content.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);

                login.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(!DataParser.isUserLoggedIn(context) && DataParser.isNetworkAvailable(context))
                            ((Activity)context).startActivityForResult(new Intent(context, LoginActivity.class), LoginActivity.REQUEST_LOGIN);
                    }
                });
            }
            else {
                content.setVisibility(View.VISIBLE);
                login.setVisibility(View.GONE);
            }


            if(item.isDefaultAvatar()) //If the default avatar is being upload use resource
                icon.setImageResource(item.getIconResource());
            else //Else if user icon is being uploaded, use bitmap
                icon.setImageBitmap(item.getAvatar());

            content.setText(item.getTitle());
        }
        //If drawer title is being created, call appropriate layout inflater
        else if(item.isHeader()) {
            drawerItemView = inflater.inflate(R.layout.item_drawer_header, parent, false);

            TextView header = (TextView) drawerItemView.findViewById(R.id.content_title);
            ImageView expander = (ImageView) drawerItemView.findViewById(R.id.drawer_expand);

            drawerItemView.setBackgroundResource(R.drawable.list_selector_0);
            expander.setImageResource(item.getExpandResource());
            header.setText(item.getTitle());

            //If children count is not empty, show the indicator
            if(getChildrenCount(groupPosition) > 0)
                expander.setVisibility(View.VISIBLE);

        }
        //Else create a regular menu option
        else {
            drawerItemView = inflater.inflate(R.layout.item_drawer_content, parent, false);

            ImageView icon = (ImageView) drawerItemView.findViewById(R.id.drawer_icon);
            TextView content = (TextView) drawerItemView.findViewById(R.id.drawer_content);
            TextView counter = (TextView) drawerItemView.findViewById(R.id.counter);

            if(item.hasCounter()) {
                int amount = item.getCount();

                if(amount > 0) {
                    counter.setVisibility(View.VISIBLE);
                    if(amount > 99)
                        counter.setText("99+");
                    else
                        counter.setText(Integer.toString(amount));
                }
                else
                    counter.setVisibility(View.GONE);
            }

            drawerItemView.setBackgroundResource(R.drawable.list_selector_0);
            icon.setImageResource(item.getIconResource());
            content.setText(item.getTitle());
        }

        return drawerItemView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);

        View drawerItemView = inflater.inflate(R.layout.item_drawer_content, parent, false);
        DrawerItem item = (DrawerItem) getChild(groupPosition, childPosition);
        ImageView icon = (ImageView) drawerItemView.findViewById(R.id.drawer_icon);
        TextView content = (TextView) drawerItemView.findViewById(R.id.drawer_content);

        drawerItemView.setBackgroundResource(R.drawable.drawer_child_selector);
        icon.setImageResource(item.getIconResource());
        content.setText(item.getTitle());

        return drawerItemView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    public void clearContents() {
        if(!drawerItemParents.isEmpty() || !drawerItemChildren.isEmpty()) {
            drawerItemParents.clear();
            drawerItemChildren.clear();
            notifyDataSetChanged();
        }
    }
}
