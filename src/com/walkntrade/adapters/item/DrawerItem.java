package com.walkntrade.adapters.item;

import android.graphics.Bitmap;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Represents the actual menu option in the Navigation Drawer
public class DrawerItem {

    private long id; //Id used to identify this drawer item
	private int iconResource; //Drawable resource used for drawer
	private String title; //Content in item
    private int counter; //Number used for unread messages
	private Bitmap avatar;
	private boolean isHeader = false;
	private boolean isUserItem = false;
	private boolean isDefaultAvatar = true;
    private boolean hasCounter = false;
	
	//Constructor for user item
	public DrawerItem(long id, int iconResource, String title, boolean isUser) {
        this.id = id;
		this.iconResource = iconResource;
		this.title = title;
		isUserItem = true;
	}

	//Regular menu item
	public DrawerItem(long id, int iconResource, String title) {
        this.id = id;
		this.iconResource = iconResource;
		this.title = title;
	}

    //Regular menu item with a counter
    public DrawerItem(long id, int iconResource, String title, int counter) {
        this.id = id;
        this.iconResource = iconResource;
        this.title = title;
        this.counter = counter;
        hasCounter = true;
    }

    public long getId(){
        return id;
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

    public void setCounter(int num) {
        counter = num;
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
