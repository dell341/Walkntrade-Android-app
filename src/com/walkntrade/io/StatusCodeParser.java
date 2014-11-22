package com.walkntrade.io;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.content.Context;

import com.walkntrade.R;

//Parses all status codes returned from server through JSON objects
public class StatusCodeParser {

    public static final int CONNECT_FAILED = -100; //Used only locally to verify success of network request. Never returned from server.
    public static final int STATUS_OK = 200;
    public static final int STATUS_NOT_AUTH = 401;
    public static final int STATUS_NOT_FOUND = 404;
    public static final int STATUS_REQUEST_INVALID = 406;
    public static final int STATUS_INTERNAL_SERVER_ERR = 500;
    public static final int STATUS_PROFANITY = 530;

    //Returns string representation of status code
    public static String getStatusString(Context context, int status) {
        switch (status){
            case CONNECT_FAILED: return context.getResources().getString(R.string.status_connection_failed);
            case STATUS_OK: return context.getResources().getString(R.string.status_ok);
            case STATUS_NOT_AUTH: return context.getResources().getString(R.string.status_not_authorized);
            case STATUS_NOT_FOUND: return context.getResources().getString(R.string.status_not_found);
            case STATUS_REQUEST_INVALID: return context.getResources().getString(R.string.status_request_invalid);
            case STATUS_INTERNAL_SERVER_ERR: return context.getResources().getString(R.string.status_internal_server_error);
            case STATUS_PROFANITY: return context.getResources().getString(R.string.status_profanity);
            default: return "null";
        }
    }
}
