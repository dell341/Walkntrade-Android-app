package com.walkntrade.objects;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class MiscPost extends Post {
	
	public MiscPost(String obsId, String identifier, String title, String details, String user, String imgURL, String date, String price, String views) {
		super(obsId, identifier, title, details, user, imgURL,date, price, views);
	}
	
	public String getCategory() {
		return Post.CATEGORY_MISC;
	}
	
	/*Parcelable implementation*/
	
	private MiscPost(Parcel in) {
		super(in);
	}
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public MiscPost createFromParcel(Parcel in) {
			return new MiscPost(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};
	
}