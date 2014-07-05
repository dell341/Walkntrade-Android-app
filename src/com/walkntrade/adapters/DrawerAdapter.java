package com.walkntrade.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.walkntrade.DrawerItem;
import com.walkntrade.LoginActivity;
import com.walkntrade.R;
import com.walkntrade.io.DataParser;

import java.util.List;

public class DrawerAdapter extends ArrayAdapter<DrawerItem>{

    private Context context;

	public DrawerAdapter(Context _context, List<DrawerItem> objects) {
		super(_context, R.layout.item_drawer_content, objects);
        context = _context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View drawerItemView;
		
		DrawerItem item = getItem(position);
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		//If user item is being created, call the appropriate layout inflater
		if (item.isUser()) {
			drawerItemView = inflater.inflate(R.layout.item_drawer_user, parent, false);
			
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
                            context.startActivity(new Intent(context, LoginActivity.class));
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
		//If drawer header is being created, call appropriate layout inflater
		else if(item.isHeader()) { 
			drawerItemView = inflater.inflate(R.layout.item_drawer_header, parent, false);
			
			TextView header = (TextView) drawerItemView.findViewById(R.id.drawer_header);
			header.setText(item.getTitle());
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
			
			icon.setImageResource(item.getIconResource());
			content.setText(item.getTitle());
		}
		return drawerItemView;
	}

	//Disables click-ability of User drawer item and Header item
	@Override
	public boolean isEnabled(int position) {
		DrawerItem item = getItem(position);
		
		return !(item.isHeader() || item.isUser());
	}

    @Override
    public long getItemId(int position) {
        DrawerItem item = getItem(position);

        /*REVISE LATER*/
        //Special case, returns id of 1994 when 'change school' selected from School Page. Because position changes when user signed out
        if(item.getTitle() != null && item.getTitle().equalsIgnoreCase("Change School"))
                return 1994;
        else
            return super.getItemId(position);
    }
}
