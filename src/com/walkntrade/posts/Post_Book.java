package com.walkntrade.posts;

//Copyright (c), All Rights Reserved, http://walkntrade.com
import android.os.Parcel;
import android.os.Parcelable;

public class Post_Book extends Post {
	
	public Post_Book(String _identifier, String _title, String _details, String _user, String _imgURL, String _date, String _price, String _views) {
		super(_identifier, _title, _details, _user, _imgURL,_date, _price, _views);
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
