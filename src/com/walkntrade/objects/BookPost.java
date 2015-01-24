package com.walkntrade.objects;

import android.os.Parcel;
import android.os.Parcelable;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

public class BookPost extends Post {

    private String author, isbn;
	
	public BookPost(String obsId, String schoolId, String identifier, String title, String author, String details, String isbn, String user, String imgURL, String date, String price, String views, String tags) {
		super(obsId, schoolId, identifier, title, details, user, imgURL,date, price, views, tags);
        this.author = author;
        this.isbn = isbn;
	}
	
	public String getCategory() {
		return Post.CATEGORY_BOOK;
	}

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

	/*Parcelable implementation*/
	
	private BookPost(Parcel in) {
		super(in);
        author = in.readString();
        isbn = in.readString();
	}

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(author);
        out.writeString(isbn);
    }
	
	public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
		public BookPost createFromParcel(Parcel in) {
			return new BookPost(in);
		}
		
		public Post[] newArray(int size) {
			return new Post[size];
		}
	};

}
