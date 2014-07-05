package com.walkntrade;

import android.graphics.Bitmap;

//Represents the actual menu option in the Navigation Drawer
public class DrawerItem {
	private int iconResource;
	private String title;
    private int counter;
	private Bitmap avatar;
	private boolean isHeader = false;
	private boolean isUserItem = false;
	private boolean isDefaultAvatar = true;
    private boolean hasCounter = false;
	
	//Constructor for user item
	public DrawerItem(int _iconResource, String _title, boolean isUser) {
		iconResource = _iconResource;
		title = _title;
		isUserItem = true;
	}
	
	//Header item
	public DrawerItem(String _title) {
		title = _title;
		isHeader = true;
	}
	
	//Regular menu item
	public DrawerItem(int _iconResource, String _title) {
		iconResource = _iconResource;
		title = _title;
	}

    //Regular menu item with a counter
    public DrawerItem(int _iconResource, String _title, int _counter) {
        iconResource = _iconResource;
        title = _title;
        counter = _counter;
        hasCounter = true;
    }
	
	public String getTitle() {
		return title;
	}

    public void setTitle(String t){
        title = t;
    }
	
	public int getIconResource() {
		return iconResource;
	}

    public int getCount(){return counter;}
	
	public Bitmap getAvatar() {
		return avatar;
	}

    public void setAvatar(Bitmap a){
        avatar = a;
        isDefaultAvatar = false;
    }
	
	public boolean isHeader(){
		return isHeader;
	}
	
	public boolean isUser() {
		return isUserItem;
	}
	
	public boolean isDefaultAvatar(){
		return isDefaultAvatar;
	}

    public boolean hasCounter() {
        return hasCounter;
    }
	

}
