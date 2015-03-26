package com.walkntrade.adapters;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.walkntrade.R;
import com.walkntrade.objects.Post;

import java.util.List;

public class PostRecycleAdapter extends RecyclerView.Adapter<PostRecycleAdapter.PostViewHolder>{
    private static final String TAG = "PostRecycleAdapter";

    private List<Post> items;
    private PostClickListener postClickListener;

    public PostRecycleAdapter(List<Post> items) {
        this.items = items;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView image;
        public TextView title;
        public TextView details;
        public TextView author;
        public TextView price;

        public PostClickListener listener;

        public PostViewHolder(View v, PostClickListener listener) {
            super(v);

            image = (ImageView) v.findViewById(R.id.post_image);
            title = (TextView) v.findViewById(R.id.content_title);
            details = (TextView) v.findViewById(R.id.post_details);
            author = (TextView) v.findViewById(R.id.post_author);
            price = (TextView) v.findViewById(R.id.post_price);

            this.listener = listener;
            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(listener != null)
                listener.onItemClicked(getLayoutPosition());
        }
    }

    public static interface PostClickListener {
        public void onItemClicked(int position);
    }

    public void setPostClickListener(PostClickListener postClickListener) {
        this.postClickListener = postClickListener;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_on_grid, viewGroup, false);

        return new PostViewHolder(v, postClickListener);
    }

    @Override
    public void onBindViewHolder(PostViewHolder viewHolder, int position) {
        //Gets the current item which will be a Post object
        Post post = items.get(position);
        //Get the values from the Post object
        try {
            viewHolder.image.setImageBitmap(post.getBitmapImage());
        }
        catch(NullPointerException e){
            Log.e(TAG, "Getting post image", e);
        }
        viewHolder.title.setText(post.getTitle());
        viewHolder.details.setText(post.getDetails());
        viewHolder.author.setText(post.getUser());
        viewHolder.price.setText(post.getPrice());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty() {
        return items == null || items.size() == 0;
    }
}
