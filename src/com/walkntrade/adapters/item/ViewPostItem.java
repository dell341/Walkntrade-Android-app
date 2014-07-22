package com.walkntrade.adapters.item;

import com.walkntrade.posts.PostReference;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */

//Represents items in the listview from the view posts option in user settings
public class ViewPostItem {

    private String text;
    private String obsId;
    private boolean isHeader;

    //Header item (School)
    public ViewPostItem(String _text) {
        text = _text;
        isHeader = true;
    }

    //Post Item
    public ViewPostItem(PostReference post){
        text = post.getTitle();
        obsId = post.getLink();
        isHeader = false;
    }

    public String getContents() {
        return text;
    }

    public String getObsId(){
        return obsId;
    }

    public boolean isHeader(){
        return isHeader;
    }
}
