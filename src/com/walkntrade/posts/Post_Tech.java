package com.walkntrade.posts;

//Copyright (c), All Rights Reserved, http://walkntrade.com
import android.os.Parcel;
import android.os.Parcelable;

public class Post_Tech extends Post {
	

	public Post_Tech(String _identifier, String _title, String _details, String _user, String _imgURL, String _date, String _price, String _views) {
		super(_identifier, _title, _details, _user, _imgURL,_date, _price, _views);
	}

	@Override
	public String getCategory() {
		return Post.CATEGORY_TECH;
	}
	
	/*Parcelable implementation*/
	
	private Post_Tech(Parcel in) {
		super(in);
	}
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public Post_Tech createFromParcel(Parcel in) {
			return new Post_Tech(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};

}
