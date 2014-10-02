package com.walkntrade.posts;

//Copyright (c), All Rights Reserved, http://walkntrade.com
import android.os.Parcel;
import android.os.Parcelable;

public class Post_Book extends Post {
	
	public Post_Book(String obsId, String identifier, String title, String details, String user, String imgURL, String date, String price, String views) {
		super(obsId, identifier, title, details, user, imgURL,date, price, views);
	}
	
	public String getCategory() {
		return Post.CATEGORY_BOOK;
	}
	
	/*Parcelable implementation*/
	
	private Post_Book(Parcel in) {
		super(in);
	}
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public Post_Book createFromParcel(Parcel in) {
			return new Post_Book(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};

}
