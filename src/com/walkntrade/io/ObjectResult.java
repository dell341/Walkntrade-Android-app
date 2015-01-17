package com.walkntrade.io;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

// Wraps server response codes and resulted data together, makes for easier deconstructing of data and errors (potential errors)
public class ObjectResult<T> {
    private int status = 0;
    private T object;

    public ObjectResult(int status, T object) {
        this.status = status;
        this.object = object;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setObject(T object) {
        this.object = object;
    }

    public int getStatus() {
        return status;
    }

    public T getObject() {
        if (object == null)
            throw new NullPointerException("Object is null");
        return object;
    }
}
