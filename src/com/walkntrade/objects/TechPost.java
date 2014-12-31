package com.walkntrade.objects;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class TechPost extends Post {
	

	public TechPost(String obsId, String schoolId, String identifier, String title, String details, String user, String imgURL, String date, String price, String views) {
		super(obsId, schoolId, identifier, title, details, user, imgURL,date, price, views);
	}

	@Override
	public String getCategory() {
		return Post.CATEGORY_TECH;
	}
	
	/*Parcelable implementation*/
	
	private TechPost(Parcel in) {
		super(in);
	}
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public TechPost createFromParcel(Parcel in) {
			return new TechPost(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};

}
