package com.walkntrade.io;

/*
 * Copyright (c) 2015. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class FormatDateTime {
    public static long ONE_MINUTE = 60000;
    public static long ONE_HOUR = 3600000;
    public static long ONE_DAY = 86400000;
    public static long SIX_DAYS = 518400000;
    public static long ONE_YEAR = (long) 3.15569e10;

    public static String FORMAT_DATE_SAME_YEAR = "MMM d";
    public static String FORMAT_NORMAL_DATE = "MMM d, yyyy";

    public static String FORMAT_DATETIME_SAME_DAY = "h:mm a";
    public static String FORMAT_DATETIME_SAME_WEEK = "E h:mm a";
    public static String FORMAT_DATETIME_SAME_YEAR = "MMM d h:mm a";
    public static String FORMAT_NORMAL_DATETIME = "MMM d, yyyy h:mm a";

    private static String TAG = "FormatDateTime";
    private static String SERVER_FORMAT_DATETIME = "yyyy-MM-dd h:m:s";
    private static String SERVER_FORMAT_DATE = "yyyy-MM-dd";

    public static String formatDate(String serverDate) {
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(SERVER_FORMAT_DATE);

        try {
            Date otherDate = dateFormat.parse(serverDate);

            Calendar currentCalendar = Calendar.getInstance(Locale.US);
            Calendar otherCalendar = Calendar.getInstance(Locale.US);
            currentCalendar.setTimeInMillis(currentTime);
            otherCalendar.setTime(otherDate);

            String format;
            if(isSameDay(currentCalendar, otherCalendar))
                return "Today";
            else
                format = FORMAT_NORMAL_DATE;

            dateFormat = new SimpleDateFormat(format, Locale.US);
            return dateFormat.format(otherDate);
        } catch (ParseException e) {
            Log.e(TAG, "Parsing Date", e);
            return serverDate;
        }
    }

    public static String formatDateTime(String serverDateTime) {
        long currentTime = System.currentTimeMillis();
        SimpleDateFormat dateFormat = new SimpleDateFormat(SERVER_FORMAT_DATETIME);

        try {
            Date otherDate = dateFormat.parse(serverDateTime);

            Calendar currentCalendar = Calendar.getInstance(Locale.US);
            Calendar otherCalendar = Calendar.getInstance(Locale.US);
            currentCalendar.setTimeInMillis(currentTime);
            otherCalendar.setTime(otherDate);

            long timeSince = currentTime - otherDate.getTime();

            String format;
            if (timeSince < ONE_MINUTE)
                return "just now";
            else if (timeSince < ONE_HOUR)
                return "less than an hour ago";
            else if (isSameDay(currentCalendar, otherCalendar))
                format = FORMAT_DATETIME_SAME_DAY;
            else if (isSameWeek(currentCalendar, otherCalendar))
                format = FORMAT_DATETIME_SAME_WEEK;
            else if (isSameYear(currentCalendar, otherCalendar))
                format = FORMAT_DATETIME_SAME_YEAR;
            else
                format = FORMAT_NORMAL_DATETIME;

            dateFormat = new SimpleDateFormat(format, Locale.US);
            return dateFormat.format(otherDate);
        } catch (ParseException e) {
            Log.e(TAG, "Parsing DateTime", e);
            return serverDateTime;
        }
    }

    private static boolean isSameDay(Calendar calendarOne, Calendar calendarTwo) {
        return (calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR)) && (calendarOne.get(Calendar.DAY_OF_YEAR) == calendarTwo.get(Calendar.DAY_OF_YEAR));
    }

    private static boolean isSameWeek(Calendar calendarOne, Calendar calendarTwo) {
        return (calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR)) && (calendarOne.get(Calendar.WEEK_OF_YEAR) == calendarTwo.get(Calendar.WEEK_OF_YEAR));
    }

    private static boolean isSameYear(Calendar calendarOne, Calendar calendarTwo) {
        return (calendarOne.get(Calendar.YEAR) == calendarTwo.get(Calendar.YEAR));
    }
}
