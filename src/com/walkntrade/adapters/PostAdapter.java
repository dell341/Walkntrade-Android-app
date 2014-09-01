package com.walkntrade.adapters;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.posts.Post;

import java.util.List;

//Holds all post information for each specific grid
public class PostAdapter extends BaseAdapter {

    private static final String TAG = "ADAPTER:Post";
    private Context context;
    private List<Post> items;
    private int currentPostCount = 12; //Number of posts to load and display initially

    public PostAdapter(Context _context, List<Post> _items) {
        context = _context;
        items = _items;

        if(items.size() < currentPostCount) //If there are less than 12 available posts, set as the post count
            currentPostCount = items.size();
    }

    //Increase count by at most 12, if new data is being added
    public void incrementCount(List<Post> newData){
        if(newData.size() >= 12)
            currentPostCount += 12;
        else
            currentPostCount += newData.size();
    }

    public void clearContents(){
        currentPostCount = 0;
        items.clear();
    }

    @Override //The amount of views to display
    public int getCount() {
        return currentPostCount;
    }

    @Override
    public Post getItem(int i) throws IndexOutOfBoundsException{
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
		View postGridView = convertView;
        ViewHolder holder;

        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            postGridView = inflater.inflate(R.layout.item_on_grid, parent, false);

            holder = new ViewHolder();
            //Get the text views and image views and assign the proper one
            holder.image = (ImageView) postGridView.findViewById(R.id.post_image);
            holder.title = (TextView) postGridView.findViewById(R.id.post_title);
            holder.details = (TextView) postGridView.findViewById(R.id.post_details);
            holder.author = (TextView) postGridView.findViewById(R.id.post_user);
            holder.price = (TextView) postGridView.findViewById(R.id.post_price);

            postGridView.setTag(holder);
        }
        else
            holder = (ViewHolder)postGridView.getTag();

        try {
            //Gets the current item which will be a Post object
            Post post = getItem(position);
            //Get the values from the Post object
            try {
                holder.image.setImageBitmap(post.getBitmapImage());
            }
            catch(NullPointerException e){
                Log.e(TAG, "Getting post image", e);
            }
            holder.title.setText(post.getTitle());
		    holder.details.setText(post.getDetails());
            holder.author.setText(post.getUser());
		    holder.price.setText(post.getPrice());
        } catch(IndexOutOfBoundsException e){
            Log.e(TAG, "Getting post object", e);
        }

		return postGridView;
	}

    //Has remaining posts that are not yet displayed by Adapter
    public boolean hasMorePosts(){
        return (items.size() - currentPostCount) > 0;
    }


    public void loadMore() { //Add more displayable posts
        int remainingPosts = items.size() - currentPostCount;

        if(remainingPosts >= 12) //If there are more than 12 posts not yet loaded
            currentPostCount += 12; //Increases amount by 12
        else
            currentPostCount += remainingPosts; //Else add remaining amount of posts.;
    }

    private static class ViewHolder{ //Increase efficiency by decreasing the amount of calls to findViewById
        public ImageView image;
        public TextView title;
        public TextView details;
        public TextView author;
        public TextView price;
    }
}
