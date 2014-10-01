package com.walkntrade.posts;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

//Object representation for Walkntrade posts. Holds all information about the post
public abstract class Post implements Parcelable{ //Implements Parcelable for ability to send through intents and different activities
	
	//public static final String CATEGORY_ALL = "All";
	public static final String CATEGORY_BOOK = "Books";
	public static final String CATEGORY_TECH = "Tech";
	public static final String CATEGORY_SERVICE = "Services";
	public static final String CATEGORY_MISC = "Miscellaneous";
	
	private String identifier, title, details, user, imgURL, date, views;
	private String price = "";
	private Bitmap defaultImage = null;
	
	public Post(String _identifier, String _title, String _details, String _user, String _imgURL, String _date, String _price, String _views) {
        identifier = _identifier;
		title = _title;
		details = _details;
		user = _user;
		imgURL = _imgURL;
		date = _date;
		price = _price;
		views = _views;
	}

    public String getIdentifier(){
        return identifier;
    }
	
	public String getTitle() {
		return title;
	}
	
	public String getDetails() {
		return details;
	}
	
	public String getUser() {
		return user;
	}
	
	public String getImgUrl() {
		return imgURL;
	}
	
	public String getDate() {
		return date;
	}
	
	public String getPrice() {
		return price;
	}
	
	public String getViews() {
		return views;
	}
	
	public void setBitmapImage(Bitmap bm) {
		defaultImage = bm;
	}
	
	public Bitmap getBitmapImage() {
		return defaultImage;
	}
	
	//Each subclass will return a different result based on their category
	public abstract String getCategory();
	
	/*Parcelable implementation*/
	private int mData;
	
	protected Post(Parcel in) { //Called only by sub-classes
		mData = in.readInt();
        identifier = in.readString();
		title = in.readString();
		details = in.readString();
		user = in.readString();
		imgURL = in.readString();
		date = in.readString();
		price = in.readString();
		views = in.readString();
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		//Must be written out in the same order it is read in
		out.writeInt(mData);
        out.writeString(identifier);
		out.writeString(title);
		out.writeString(details);
		out.writeString(user);
		out.writeString(imgURL);
		out.writeString(date);
		out.writeString(price);
		out.writeString(views);
	}
	

}
