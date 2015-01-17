package com.walkntrade.fragments;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;

import com.walkntrade.io.DataParser;
import com.walkntrade.objects.ChatObject;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Implementation thanks to Alex Lockwood, from StackOverflow & Android Design Patterns
 */
//Handles an AsyncTask, so that it is retained across configuration changes.
public class TaskFragment extends Fragment {

    private static final String TAG = "TaskFragment";
    public static final String ARG_THREAD_ID = "argument_thread_id";
    public static final String ARG_TASK_ID = "async_task_id";

    public static final int TASK_GET_CHAT_THREAD = 100;


    private TaskCallbacks callbacks;
    private int taskId;

    //Callback interface that allows the Activity to access AsyncTask updates
    public static interface TaskCallbacks {
        void onPreExecute();
        void onProgressUpdate(int percent);
        void onCancelled();
        void onPostExecute(int taskId, Object result);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //Retains this fragment, when configuration changes.

        taskId = getArguments().getInt(ARG_TASK_ID, 0);

        switch(taskId) {
            case TASK_GET_CHAT_THREAD:
                String threadId = getArguments().getString(ARG_THREAD_ID);
                ChatThreadTask asyncTask = new ChatThreadTask();
                asyncTask.execute(threadId); break;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            callbacks = (TaskCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()+" must implement TaskCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = null;
    }

    private class ChatThreadTask extends AsyncTask<String, Void, DataParser.ObjectResult<ArrayList<ChatObject>>> {

        public ChatThreadTask() {
            super();
            Log.i(TAG, "Starting ChatThreadTask from Fragment - " + getActivity());
        }

        @Override
        protected void onPreExecute() {
            if (callbacks != null)
                callbacks.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (callbacks != null)
                callbacks.onProgressUpdate(0);
        }

        @Override
        protected void onCancelled() {
            if (callbacks != null)
                callbacks.onCancelled();
        }

        @Override
        protected DataParser.ObjectResult<ArrayList<ChatObject>> doInBackground(String... threadId) {
            DataParser database = new DataParser(getActivity().getApplicationContext());

            try {
                return database.retrieveThread(threadId[0]); //Return object result
            } catch (IOException e) {
                Log.e(TAG, "Getting Chat Thread", e);
            }
            return null; //If an error occurred, return null
        }

        @Override
        protected void onPostExecute(DataParser.ObjectResult<ArrayList<ChatObject>> result) {
            Log.d(TAG, "Completing ChatThreadTask from Fragment - "+getActivity());
            if (callbacks != null)
                callbacks.onPostExecute(taskId, result);
        }
    }
}
