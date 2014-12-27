package com.walkntrade.adapters.item;

import android.graphics.Bitmap;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Represents the actual menu option in the Navigation Drawer
public class DrawerItem {
	private int iconResource, expandResource;
	private String title;
    private int counter;
	private Bitmap avatar;
	private boolean isHeader = false;
	private boolean isUserItem = false;
	private boolean isDefaultAvatar = true;
    private boolean hasCounter = false;
	
	//Constructor for user item
	public DrawerItem(int iconResource, String title, boolean isUser) {
		this.iconResource = iconResource;
		this.title = title;
		isUserItem = true;
	}
	
	//Header item
	public DrawerItem(String title, int expandResource) {
		this.title = title;
		isHeader = true;
        this.expandResource = expandResource;
	}
	
	//Regular menu item
	public DrawerItem(int iconResource, String title) {
		this.iconResource = iconResource;
		this.title = title;
	}

    //Regular menu item with a counter
    public DrawerItem(int iconResource, String title, int counter) {
        this.iconResource = iconResource;
        this.title = title;
        this.counter = counter;
        hasCounter = true;
    }
	
	public String getTitle() {
		return title;
	}

    public void setTitle(String t){
        title = t;
    }

    public void setExpandResource(int expandResource){
        this.expandResource = expandResource;
    }

    public int getExpandResource(){
        return expandResource;
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
