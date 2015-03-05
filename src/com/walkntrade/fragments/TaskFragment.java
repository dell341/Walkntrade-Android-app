package com.walkntrade.fragments;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.app.Activity;
import android.app.Fragment;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.walkntrade.R;
import com.walkntrade.io.DataParser;
import com.walkntrade.io.DatabaseHelper;
import com.walkntrade.io.ObjectResult;
import com.walkntrade.io.StatusCodeParser;
import com.walkntrade.objects.ChatObject;
import com.walkntrade.objects.MessageThread;
import com.walkntrade.objects.Post;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Implementation thanks to Alex Lockwood, from StackOverflow & Android Design Patterns
 */

//Handles an AsyncTask, so that it is retained across configuration changes. This way
// the resulting data can be handled properly without losing connection with the displayed activity.
public class TaskFragment extends Fragment {

    private static final String TAG = "TaskFragment";

    public static final String ARG_TASK_ID = "com.walkntrade.fragments.arg.ASYNC_TASK_ID";

    public static final int TASK_LOGIN = 100;
    public static final String ARG_LOGIN_USER = "com.walkntrade.fragments.arg.LOGIN_USER";
    public static final String ARG_LOGIN_PASSWORD = "com.walkntrade.fragments.arg.LOGIN_PASSWORD";

    public static final int TASK_GET_MESSAGE_THREADS = 200;

    public static final int TASK_GET_CHAT_THREAD = 300;
    public static final String ARG_THREAD_ID = "com.walkntrade.fragments.arg.THREAD_ID";

    public static final int TASK_REMOVE_MESSAGE_THREADS = 400;
    public static final String ARG_MESSAGES_THREAD_IDS = "com.walkntrade.fragments.arg.MESSAGES_THREAD_IDS";

    public static final int TASK_POST_SEARCH = 500;
    public static final String ARG_SEARCH_QUERY = "com.walkntrade.fragments.args.SEARCH_QUERY";
    public static final String ARG_CATEGORY = "com.walkntrade.fragments.args.CATEGORY";
    public static final String ARG_OFFSET = "com.walkntade.fragments.args.OFFSET";

    private TaskCallbacks callbacks;
    private AsyncTask asyncTask;
    private int taskId;

    //Callback interface that allows the Activity to access AsyncTask updates
    public static interface TaskCallbacks {
        void onPreExecute(int taskId);
        void onProgressUpdate(int percent);
        void onCancelled();
        void onPostExecute(int taskId, Object result);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true); //Retains this fragment, when configuration changes.

        taskId = getArguments().getInt(ARG_TASK_ID, 0);
        runTask(taskId);
    }

    public void runTask(int taskId) {
        switch(taskId) {
            case TASK_LOGIN:
                asyncTask = new LoginTask();
                final String loginUser = getArguments().getString(ARG_LOGIN_USER);
                final String loginPassword = getArguments().getString(ARG_LOGIN_PASSWORD);
                ((LoginTask)asyncTask).execute(loginUser, loginPassword);
                break;
            case TASK_GET_MESSAGE_THREADS:
                asyncTask = new GetMessagesTask();
                ((GetMessagesTask)asyncTask).execute(); break;
            case TASK_GET_CHAT_THREAD:
                final String threadId = getArguments().getString(ARG_THREAD_ID);
                asyncTask = new ChatThreadTask();
                ((ChatThreadTask)asyncTask).execute(threadId); break;
            case TASK_REMOVE_MESSAGE_THREADS:
                final ArrayList<String> messagesToDelete = getArguments().getStringArrayList(ARG_MESSAGES_THREAD_IDS);
                asyncTask = new DeleteThreadTask(messagesToDelete);
                ((DeleteThreadTask)asyncTask).execute(); break;
            case TASK_POST_SEARCH:
                final String searchQuery = getArguments().getString(ARG_SEARCH_QUERY);
                final String category = getArguments().getString(ARG_CATEGORY);
                final int offset = getArguments().getInt(ARG_OFFSET);
                asyncTask = new PostSearchTask(searchQuery, category, offset);

                ((PostSearchTask)asyncTask).execute();
                break;
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

    @Override
    public void onDestroy() {
        asyncTask.cancel(true);
        super.onDestroy();
    }

    //Asynchronous Task logs in user & retrieves username and password
    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            if (callbacks != null)
                callbacks.onPreExecute(taskId);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (callbacks != null)
                callbacks.onProgressUpdate(0);
        }

        @Override
        protected void onCancelled(String s) {
            if (callbacks != null)
                callbacks.onCancelled();
        }

        @Override
        protected String doInBackground(String... userCredentials) {
            if(isCancelled())
                return null;

            DataParser database = new DataParser(getActivity().getApplicationContext());
            String _emailAddress = userCredentials[0];
            String _password = userCredentials[1];

            String response = getActivity().getApplicationContext().getString(R.string.login_failed);
            try {
                response = database.login(_emailAddress, _password);
                DataParser.setSharedStringPreference(getActivity().getApplicationContext(), DataParser.PREFS_USER, DataParser.KEY_USER_EMAIL, _emailAddress);
                database.getUserName();
                database.getAvatarUrl(null, true);
                database.simpleGetIntent(DataParser.INTENT_GET_PHONENUM); //Get user's phone number when logging in
                database.simpleGetIntent(DataParser.INTENT_GET_EMAILPREF); //Get user's contact preference when logging in
            } catch (Exception e) {
                Log.e(TAG, "Logging in", e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if (callbacks != null)
                callbacks.onPostExecute(taskId, result);
        }
    }

    private class GetMessagesTask extends AsyncTask<Void, Void, ObjectResult<ArrayList<MessageThread>>> {
        @Override
        protected void onPreExecute() {
            if (callbacks != null)
                callbacks.onPreExecute(taskId);
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
        protected ObjectResult<ArrayList<MessageThread>> doInBackground(Void... voids) {
            if(isCancelled())
                return null;

            DataParser database = new DataParser(getActivity().getApplicationContext());

            try {
                return database.getMessageThreads(0, -1);
            } catch (IOException e) {
                Log.e(TAG, "Get MessageThreads", e);
            }

            return new ObjectResult<>(StatusCodeParser.CONNECT_FAILED, null);
        }

        @Override
        protected void onPostExecute(ObjectResult<ArrayList<MessageThread>> result) {
            if (callbacks != null)
                callbacks.onPostExecute(taskId, result);
        }
    }

    private class ChatThreadTask extends AsyncTask<String, Void, ObjectResult<ArrayList<ChatObject>>> {
        @Override
        protected void onPreExecute() {
            if (callbacks != null)
                callbacks.onPreExecute(taskId);
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
        protected ObjectResult<ArrayList<ChatObject>> doInBackground(String... threadId) {
            if(isCancelled())
                return null;

            DataParser database = new DataParser(getActivity().getApplicationContext());

            try {
                return database.retrieveThread(threadId[0]); //Return object result
            } catch (IOException e) {
                Log.e(TAG, "Getting Chat Thread", e);
            }
            return new ObjectResult<>(StatusCodeParser.CONNECT_FAILED, null); //If an error occurred, return a connection failed object
        }

        @Override
        protected void onPostExecute(ObjectResult<ArrayList<ChatObject>> result) {
            if (callbacks != null)
                callbacks.onPostExecute(taskId, result);
        }
    }

    private class DeleteThreadTask extends AsyncTask<Void, Void, ObjectResult<String[]>> {

        private ArrayList<String> messageToDelete;

        public DeleteThreadTask(ArrayList<String> messagesToDelete) {
            this.messageToDelete = messagesToDelete;
        }

        @Override
        protected void onPreExecute() {
            if (callbacks != null)
            callbacks.onPreExecute(taskId);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (callbacks != null)
                callbacks.onProgressUpdate(0);
        }

        @Override
        protected ObjectResult<String[]> doInBackground(Void... voids) {
            if(isCancelled())
                return null;

            int numDeleted = 0;
            String[] threadIds = new String[messageToDelete.size()];
            DataParser database = new DataParser(getActivity().getApplicationContext());
            ObjectResult<String[]> result = new ObjectResult<>(StatusCodeParser.CONNECT_FAILED, threadIds);
            DatabaseHelper databaseHelper = new DatabaseHelper(getActivity().getApplicationContext());
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            try {
                for(String threadId : messageToDelete) {
                    int serverResponse = database.deleteThread(threadId);

                    if(serverResponse != StatusCodeParser.STATUS_OK) { //If an error occurred, stop and return code
                        result.setStatus(serverResponse);
                        return result;
                    }
                    else {
                        threadIds[numDeleted] = threadId;
                        numDeleted++;
                        String[] whereArgs = {threadId};
                        db.delete(DatabaseHelper.ThreadsEntry.TABLE_NAME, DatabaseHelper.ThreadsEntry.COLUMN_THREAD_ID + "=?", whereArgs);
                        db.delete(DatabaseHelper.ConversationEntry.TABLE_NAME, DatabaseHelper.ConversationEntry.COLUMN_THREAD_ID + "=?", whereArgs);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Deleting message(s)", e);
            }

            result.setStatus(StatusCodeParser.STATUS_OK);
            return result;
        }

        @Override
        protected void onPostExecute(ObjectResult<String[]> result) {
            messageToDelete.clear();
            if (callbacks != null)
                callbacks.onPostExecute(taskId, result);
        }
    }

    private class PostSearchTask extends AsyncTask<Void, Void, ObjectResult<ArrayList<Post>>> {
        private String searchQuery, category;
        private int offset;

        public PostSearchTask(String searchQuery, String category, int offset) {
            super();
            this.searchQuery = searchQuery;
            this.category = category;
            this.offset = offset;
        }

        @Override
        protected void onPreExecute() {
            if (callbacks != null)
                callbacks.onPreExecute(taskId);
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
        protected ObjectResult<ArrayList<Post>> doInBackground(Void... voids) {
            if(isCancelled())
                return null;

            DataParser database = new DataParser(getActivity().getApplicationContext());
            ObjectResult<ArrayList<Post>> result = new ObjectResult<>(StatusCodeParser.CONNECT_FAILED, null);

            try {
                String schoolID = DataParser.getSharedStringPreference(getActivity().getApplicationContext(), DataParser.PREFS_SCHOOL, DataParser.KEY_SCHOOL_SHORT);
                result = database.getSchoolPosts(schoolID, searchQuery, category, offset, 15);
            } catch (Exception e) {
                Log.e(TAG, "Retrieving school post(s)", e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(ObjectResult<ArrayList<Post>> result) {
            super.onPostExecute(result);
            if (callbacks != null)
                callbacks.onPostExecute(taskId, result);
        }
    }
}
