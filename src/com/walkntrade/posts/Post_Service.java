package com.walkntrade.posts;


import android.os.Parcel;
import android.os.Parcelable;

//Copyright (c), All Rights Reserved, http://walkntrade.com

public class Post_Service extends Post {
	
	public Post_Service(String _identifier, String _title, String _details, String _author, String _imgURL, String _date, String _price, String _views) {
		super(_identifier, _title, _details, _author, _imgURL,_date, _price,_views);
	}
	
	public String getCategory() {
		return Post.CATEGORY_SERVICE;
	}
	
	/*Parcelable implementation*/
	
	private Post_Service(Parcel in) {
		super(in);
	}
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public Post_Service createFromParcel(Parcel in) {
			return new Post_Service(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};
}
