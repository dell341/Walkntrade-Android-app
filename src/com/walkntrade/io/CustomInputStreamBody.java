package com.walkntrade.io;

import android.util.Log;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.InputStreamBody;

import java.io.IOException;
import java.io.InputStream;

/**
 * Copyright (c) 2014, All Rights Reserved
 * http://walkntrade.com
 */
public class CustomInputStreamBody extends InputStreamBody {
    private static final String TAG = "CustomInputStreamBody";
    private InputStream in;

    public CustomInputStreamBody(InputStream in, ContentType contentType, String filename) {
        super(in, contentType, filename);
        this.in = in;
    }

    @Override
    public long getContentLength() {
        try {
            return in.available();
        } catch (IOException e) {
            Log.e(TAG, "CustomInputStreamBody", e);
        }

        return 0;
    }
}
