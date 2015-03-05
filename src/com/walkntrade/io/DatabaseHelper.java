package com.walkntrade.io;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Walkntrade.db";
    private static final int DATABASE_VERSION = 1;

    private static final String COMMA = " , ";
    private static final String TINY_TEXT_TYPE = "TINYTEXT";
    private static final String TEXT_TYPE = "TEXT";
    private static final String TINY_INT_TYPE = "TINYINT";
    private static final String INT_TYPE = "INT";
    private static final String NOTNULL = "NOT NULL";
    private static final String PRIMARYKEY = "PRIMARY KEY";
    private static final String FOREIGNKEY = "FOREIGN KEY";

    public static abstract class ThreadsEntry implements BaseColumns {
        public static final String TABLE_NAME = "threads";
        public static final String COLUMN_THREAD_ID = "threadId";
        public static final String COLUMN_POST_ID = "postId";
        public static final String COLUMN_POST_TITLE = "postTitle";
        public static final String COLUMN_LAST_MESSAGE = "lastMessage";
        public static final String COLUMN_LAST_USER_ID = "lastUserId";
        public static final String COLUMN_LAST_USER_NAME = "lastUserName";
        public static final String COLUMN_DATETIME = "datetime";
        public static final String COLUMN_RECIPIENT_ID = "recipientId";
        public static final String COLUMN_RECIPIENT_NAME = "recipientName";
        public static final String COLUMN_RECIPIENT_IMAGE = "recipientImage";
        public static final String COLUMN_NEW_MESSAGES = "newMessages";

        private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "+ ThreadsEntry.TABLE_NAME+
                "("+COLUMN_THREAD_ID+" "+ TEXT_TYPE +" "+PRIMARYKEY+COMMA+
                COLUMN_POST_ID+" "+ TEXT_TYPE+" "+NOTNULL+COMMA+
                COLUMN_POST_TITLE+" "+TINY_TEXT_TYPE+COMMA+
                COLUMN_LAST_MESSAGE+" "+TEXT_TYPE+COMMA+
                COLUMN_LAST_USER_ID+" "+INT_TYPE+COMMA+
                COLUMN_LAST_USER_NAME+" "+TEXT_TYPE+COMMA+
                COLUMN_DATETIME+" "+TEXT_TYPE+COMMA+
                COLUMN_RECIPIENT_ID+" "+INT_TYPE+COMMA+
                COLUMN_RECIPIENT_NAME+" "+TINY_TEXT_TYPE+COMMA+
                COLUMN_RECIPIENT_IMAGE+" "+TEXT_TYPE+COMMA+
                COLUMN_NEW_MESSAGES+" "+INT_TYPE+")";

        private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXIST "+TABLE_NAME;
    }

    public static abstract class ConversationEntry implements BaseColumns {
        public static final String TABLE_NAME = "conversation";
        public static final String COLUMN_THREAD_ID = "threadId";
        public static final String COLUMN_SENT_FROM_ME = "sentFromMe";
        public static final String COLUMN_CONTENTS = "contents";
        public static final String COLUMN_DATETIME = "datetime";
        public static final String COLUMN_SENDER_NAME = "senderName";
        public static final String COLUMN_SENDER_IMAGE = "senderImage";
        public static final String INDEX_NAME = "ConversationIndex";

        private static final String SQL_CREATE_ENTRIES = "CREATE TABLE "+ ConversationEntry.TABLE_NAME+
                "("+COLUMN_THREAD_ID+" "+TEXT_TYPE+" "+PRIMARYKEY+COMMA+
                COLUMN_SENT_FROM_ME+" "+TINY_INT_TYPE+" "+NOTNULL+COMMA+
                COLUMN_CONTENTS+" "+TEXT_TYPE+COMMA+
                COLUMN_DATETIME+" "+TEXT_TYPE+COMMA+
                COLUMN_SENDER_NAME+" "+TEXT_TYPE+COMMA+
                COLUMN_SENDER_IMAGE+" "+TEXT_TYPE+COMMA;

        private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXIST "+TABLE_NAME;

        private static final String SQL_INDEX_ENTRIES = "CREATE INDEX "+INDEX_NAME+" ON "+TABLE_NAME+" ("+COLUMN_THREAD_ID+")";

    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ThreadsEntry.SQL_CREATE_ENTRIES);
        db.execSQL(ConversationEntry.SQL_CREATE_ENTRIES);
        db.execSQL(ConversationEntry.SQL_INDEX_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(ThreadsEntry.SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
