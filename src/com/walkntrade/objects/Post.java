package com.walkntrade.objects;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Object representation for Walkntrade posts. Holds all information about the post
public abstract class Post implements Parcelable{ //Implements Parcelable for ability to send through intents and different activities
	
	//public static final String CATEGORY_ALL = "All";
	public static final String CATEGORY_BOOK = "book";
	public static final String CATEGORY_TECH = "tech";
	public static final String CATEGORY_HOUSING = "housing";
	public static final String CATEGORY_MISC = "misc";
    public static final String CATEGORY_UNKNOWN = "Unknown";
	
	private String obsId, schoolId, identifier, title, details, user, userId, imgURL, date, views, tags;
	private String price = "";
	private Bitmap defaultImage = null;
	
	public Post(String obsId, String schoolId, String identifier, String title, String details, String user, String userId, String imgURL, String date, String price, String views, String tags) {
        this.obsId = obsId;
        this.schoolId = schoolId;
        this.identifier = identifier;
		this.title = title;
		this.details = details;
		this.user = user;
        this.userId = userId;
		this.imgURL = imgURL;
		this.date = date;
		this.price = price;
		this.views = views;
        this.tags = tags;
	}

    public String getObsId() {
        return obsId;
    }

    public String getSchoolId() {
        return schoolId;
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

    public String getUserId() {
        return userId;
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

    public String getTags() {
        return tags;
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
	
	protected Post(Parcel in) { //Called only by sub-classes
        obsId = in.readString();
        schoolId = in.readString();
        identifier = in.readString();
		title = in.readString();
		details = in.readString();
		user = in.readString();
        userId = in.readString();
		imgURL = in.readString();
		date = in.readString();
		price = in.readString();
		views = in.readString();
        tags = in.readString();
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		//Must be written out in the same order it is read in
        out.writeString(obsId);
        out.writeString(schoolId);
        out.writeString(identifier);
		out.writeString(title);
		out.writeString(details);
		out.writeString(user);
        out.writeString(userId);
		out.writeString(imgURL);
		out.writeString(date);
		out.writeString(price);
		out.writeString(views);
        out.writeString(tags);
	}
	

}
