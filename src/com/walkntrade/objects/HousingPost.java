package com.walkntrade.objects;


import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class HousingPost extends Post {
	
	public HousingPost(String obsId, String schoolId, String identifier, String title, String details, String user, String userId, String imgURL, String date, String price, String views, String tags) {
		super(obsId, schoolId, identifier, title, details, user, userId, imgURL,date, price, views, tags);
	}
	
	public String getCategory() {
		return Post.CATEGORY_HOUSING;
	}
	
	/*Parcelable implementation*/
	
	private HousingPost(Parcel in) {
		super(in);
	}
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public HousingPost createFromParcel(Parcel in) {
			return new HousingPost(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};
}
