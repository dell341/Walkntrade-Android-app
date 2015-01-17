package com.walkntrade.io;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.walkntrade.R;
import com.walkntrade.objects.BookPost;
import com.walkntrade.objects.ChatObject;
import com.walkntrade.objects.MessageThread;
import com.walkntrade.objects.MiscPost;
import com.walkntrade.objects.Post;
import com.walkntrade.objects.ReferencedPost;
import com.walkntrade.objects.SchoolObject;
import com.walkntrade.objects.HousingPost;
import com.walkntrade.objects.TechPost;
import com.walkntrade.objects.UserProfileObject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/*
 * Copyright (c) 2014. All Rights Reserved. Walkntrade
 * https://walkntrade.com
 */

//Handles almost all necessary network communications
public class DataParser {
    private static final String url = "https://walkntrade.com/";
    private static final String apiUrl = "https://walkntrade.com/api2/";
    private static final String TAG = "DataParser";
    private static final String STATUS = "status"; //name:"value" pair for JSON request status
    private static final String MESSAGE = "message"; //name:"value" pair for JSON returned message
    private static final String PAYLOAD = "payload"; //name:"value" pair for JSON payload (actual data)
    public static final String LOGIN_SUCCESS = "success";

    //Cookies
    public static final String COOKIES_USER_LOGIN = "user_login"; //Cookie title
    public static final String COOKIES_SESS_SEED = "sessionSeed"; //Cookie title
    public static final String COOKIES_SESS_UID = "sessionUid"; //Cookie title
    public static final String COOKIES_SCHOOL_PREF = "sPref"; //Cookie title

    //SharedPreferences preference names
    public static final String PREFS_COOKIES = "CookiesPreferences";
    public static final String PREFS_CATEGORIES = "CategoriesPreferences";
    public static final String PREFS_USER = "UserPreferences";
    public static final String PREFS_SCHOOL = "SchoolPreferences";
    public static final String PREFS_NOTIFICATIONS = "NotificationPreferences";
    public static final String PREFS_AUTHORIZATION = "AuthorizationPreferences";

    //SharedPreferences key names
    public static final String KEY_USER_NAME = "user_name"; //User-Pref title
    public static final String KEY_USER_PHONE = "phone_number"; //User-Pref title
    public static final String KEY_USER_EMAIL = "user_email"; //User-Pref title
    public static final String KEY_USER_AVATAR_URL = "user_avatar_url"; //User-pref title
    public static final String KEY_USER_MESSAGES = "user_messages"; //User-Pref title
    public static final String KEY_CURRENTLY_LOGGED_IN = "userLoggedIn"; //User-Pref title
    public static final String KEY_SCHOOL_SHORT = "sPrefShort"; //School Preference title
    public static final String KEY_SCHOOL_LONG = "sPrefLong"; //School Preference title
    public static final String KEY_NOTIFY_EMAIL = "notification_email"; //Notification preference
    public static final String KEY_NOTIFY_USER = "notification_status"; //Notification preference title (boolean)
    public static final String KEY_NOTIFY_VIBRATE = "notification_vibrate"; //Notification preference title (boolean)
    public static final String KEY_NOTIFY_SOUND = "notification_sound"; //Notification preference title
    public static final String KEY_NOTIFY_LIGHT = "notification_light"; //Notification preference title (boolean)
    public static final String KEY_NOTIFY_ACTIVE_THREAD = "notification_active_thread"; //Notification preference title. Disable notifications for threads matching this id
    public static final String KEY_NOTIFY_DISPLAY_ON = "notification_display_on"; //Notification preference title (boolean)
    public static final String KEY_AUTHORIZED = "user_authorization"; //Authorization-Pref title (boolean) User password changed or session id expired.
    public static final String KEY_CATEGORY_AMOUNT = "category_amount"; //Category preference
    public static final String KEY_CATEGORY_ID = "category_"; //Category preference. Number must be added at the end. 'category_#'
    public static final String KEY_CATEGORY_NAME = "category_name_"; //Category preference. Number must be added at the end. 'category_#'

    public static final String BLANK = " ";

    //Server commands & intent names
    public static final String INTENT_GET_EMAILPREF = "getEmailPref";
    public static final String INTENT_GET_PHONENUM = "getPhoneNum";

    private AndroidHttpClient httpClient; //Android Client, Uses User-Agent, and executes request
    private HttpContext httpContext; //Contains CookieStore that is sent along with request
    private CookieStore cookieStore; //Holds cookies from server
    private HttpPost httpPost; //Contains message to be sent to client
    private final String USER_AGENT = System.getProperty("http.agent"); //Unique User-Agent of current device

    //Initialized here to enable usage within Handler classes
    private ArrayList<MessageThread> messages;

    private String userLoginCookie, sessionSeedCookie, sessionUidCookie, sPrefCookie;
    private Context context;

    public DataParser(Context _context) {
        context = _context;
    }

    //First call whenever connecting across the user's network
    private void establishConnection() {
        cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore); //Attach CookieStore to the HttpContext

        getCookies(); //Retrieve currently stored cookies

        httpClient = AndroidHttpClient.newInstance(USER_AGENT);
        httpPost = new HttpPost(apiUrl);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Cookie", sessionSeedCookie + ";" + sessionUidCookie + ";" + userLoginCookie + ";" + sPrefCookie);

        //HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), 10000);
        //HttpConnectionParams.setSoTimeout(httpClient.getParams(), 10000);
    }

    //Called after communication is complete
    private void disconnectAll() {
        httpClient.close();
    }

    //Abort current POST operation
    public synchronized void abortOperation() {
        Log.i(TAG, "Aborting POST operation");
        httpPost.abort();
    }

    //Get Cookies already stored on device
    private void getCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, Context.MODE_PRIVATE);
        userLoginCookie = settings.getString(COOKIES_USER_LOGIN, BLANK);
        sessionSeedCookie = settings.getString(COOKIES_SESS_SEED, BLANK);
        sessionUidCookie = settings.getString(COOKIES_SESS_UID, BLANK);

        //Short name of school will be used in the cookies
        settings = context.getSharedPreferences(PREFS_SCHOOL, Context.MODE_PRIVATE);
        sPrefCookie = settings.getString(KEY_SCHOOL_SHORT, "sPref=" + BLANK); //Set sPref cookie in 'sPref=[value]' form or leave value as blank
    }

    //Update all cookies with new values from Cookie Store
    private void updateCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        List<Cookie> cookieList = cookieStore.getCookies();
        for (Cookie cookie : cookieList) {
            if (cookie.getName().equals(COOKIES_USER_LOGIN))
                editor.putString(COOKIES_USER_LOGIN, cookie.getName() + "=" + cookie.getValue());
            else if (cookie.getName().equals(COOKIES_SESS_SEED))
                editor.putString(COOKIES_SESS_SEED, cookie.getName() + "=" + cookie.getValue());
            else if (cookie.getName().equals(COOKIES_SESS_UID))
                editor.putString(COOKIES_SESS_UID, cookie.getName() + "=" + cookie.getValue());
            else
                Log.e(TAG, "Found Extra Cookie: " + cookie.getName());
        }
        settings = context.getSharedPreferences(PREFS_SCHOOL, Context.MODE_PRIVATE);
        sPrefCookie = settings.getString(KEY_SCHOOL_SHORT, "sPref=" + BLANK); //Set sPref cookie in 'sPref=[value]' form or leave value as blank

        editor.apply(); //Save changes to the SharedPreferences
    }

    //Clear locally stored cookies
    private void clearCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, Context.MODE_PRIVATE);
        settings.edit().clear().apply();
    }

    //Clear locally stored user information
    private void clearUserInfo() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
        settings.edit().clear().apply();
    }

    private void clearNotificationInfo() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        settings.edit().clear().apply();
    }

    //Sends out POST_OBJECT request and returns an InputStream
    private InputStream processRequest(HttpEntity entity) throws IOException {
        if (httpPost.isAborted())
            return null;

        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost, httpContext); //Executes the request along with the cookie store

        updateCookies();
        return response.getEntity().getContent();
    }

    //Read String from InputStream and return the result
    private String readInputAsString(InputStream inStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        inStream.close();

        return builder.toString();
    }

    //Used for JSONObjects that just return a String
    private ObjectResult<String> getIntentResult(String query) throws IOException {

        ObjectResult<String> result = new ObjectResult<String>(StatusCodeParser.CONNECT_FAILED, null);
        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            result.setStatus(jsonObject.getInt(STATUS));
            result.setObject(jsonObject.getString(MESSAGE));
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }
        return result;
    }

    //Checks network availability before performing actions
    public static boolean isNetworkAvailable(Context _context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting())
            return true;

        Toast toast = Toast.makeText(_context, _context.getString(R.string.no_connection), Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();
        return false;
    }

    //Stores values for later use. preferred school, username, phone number, email, etc.
    public static void setSharedStringPreference(Context _context, String preferenceName, String key, String value) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);

        editor.apply(); //Save changes to the SharedPreferences
    }

    public static void setSharedIntPreferences(Context _context, String preferenceName, String key, int value) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);

        editor.apply();
    }

    public static void setSharedBooleanPreferences(Context _context, String preferenceName, String key, boolean value) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);

        editor.apply();
    }

    //Gets values stored on device
    public static String getSharedStringPreference(Context _context, String preferenceName, String key) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);

        if (key.equals(KEY_SCHOOL_SHORT))
            return settings.getString(KEY_SCHOOL_SHORT, null).split("=")[1]; //sPref=[school] is split and the second index [school] is returned

        return settings.getString(key, null);
    }

    public static int getSharedIntPreference(Context _context, String preferenceName, String key) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        return settings.getInt(key, 0);
    }

    //Default boolean value is true
    public static boolean getSharedBooleanPreferenceTrueByDefault(Context _context, String preferenceName, String key) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        return settings.getBoolean(key, true);
    }

    public static boolean getSharedBooleanPreference(Context _context, String preferenceName,String key) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        return settings.getBoolean(key, false);
    }

    public static void setSoundPref(Context _context, Uri uri) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(KEY_NOTIFY_SOUND, uri.toString());

        editor.apply();
    }

    public static String getSoundPref(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        return settings.getString(KEY_NOTIFY_SOUND, null);
    }

    //Returns user login status
    public static boolean isUserLoggedIn(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
        return settings.getBoolean(DataParser.KEY_CURRENTLY_LOGGED_IN, false);
    }

    //All category references exist on the server, so they can be updated and changed dynamically
    public int getCategories() throws IOException {
        establishConnection();

        String query = "intent=getCategories";
        int requestStatus = StatusCodeParser.CONNECT_FAILED;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            requestStatus = jsonObject.getInt(STATUS);

            JSONObject payload = jsonObject.getJSONObject(PAYLOAD);
            JSONArray categories = payload.getJSONArray("categories");
            int numOfCategories = categories.length();

            setSharedIntPreferences(context, PREFS_CATEGORIES, KEY_CATEGORY_AMOUNT, numOfCategories);

            for(int i=0; i<numOfCategories; i++) {
                JSONArray currentCategory = categories.getJSONArray(i);

                String categoryId = currentCategory.getString(0);
                String categoryName = currentCategory.getString(1);
                String categoryDesc = currentCategory.getString(2);

                setSharedStringPreference(context, PREFS_CATEGORIES, KEY_CATEGORY_ID + i, categoryId);
                setSharedStringPreference(context, PREFS_CATEGORIES, KEY_CATEGORY_NAME+i, categoryName);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }
        return requestStatus;
    }

    //Requirement check for registration
    public int isUserNameFree(String username) throws IOException {
        establishConnection();

        String query = "intent=checkUsername&username=" + username;
        int requestStatus = StatusCodeParser.CONNECT_FAILED;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            requestStatus = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return requestStatus;
    }

    //Attempts to register User account into server
    public ObjectResult<String> registerUser(String username, String email, String password, String phoneNum) throws IOException {
        establishConnection();

        String query = "intent=addUser&username=" + username + "&email=" + email + "&password=" + password + "&phone" + phoneNum;
        ObjectResult<String> result = new ObjectResult<String>(StatusCodeParser.CONNECT_FAILED, null);

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            int status = jsonObject.getInt(STATUS);
            String message = jsonObject.getString(MESSAGE);

            result = new ObjectResult<String>(status, message);

        } catch (JSONException e) {
          Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return result;
    }

    //Resets user's password
    public int resetPassword(String email) throws IOException {
        establishConnection();
        int requestStatus = StatusCodeParser.CONNECT_FAILED;

        String query = "intent=resetPassword&email=" + email;
        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            requestStatus = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return requestStatus;
    }

    //Verifies user based on key provided
    public String verifyUser(String key) throws IOException {
        establishConnection();

        String query = "intent=verifyKey&key=" + key;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Login User into Walkntrade
    public String login(String email, String password) throws IOException {
        establishConnection(); //Instantiate all streams and opens the connection
        String query = "intent=login&password=" + password + "&email=" + email + "&rememberMe=true";
        String serverResponse = null;

        //    Log.d(TAG, "Logging in");
        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Logs user out of Walkntrade
    public void logout() throws IOException {
        establishConnection();

        String query = "intent=logout&GCMClear=true";

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        processRequest(entity);
        clearCookies(); //Clears locally stored cookies
        clearUserInfo(); //Clears locally stored user information
        clearNotificationInfo();

        disconnectAll();
    }

    public ObjectResult<String> getUserName() throws IOException {
        establishConnection();

        String query = "intent=getUserName";
        ObjectResult<String> result = getIntentResult(query);

        //Stores username locally to device
        setSharedStringPreference(context, PREFS_USER, KEY_USER_NAME, result.getObject()); //Stores username locally to device
        return result;
    }

    public ObjectResult<String> getAvatarUrl() throws IOException {
        establishConnection();

        String query = "intent=getAvatar";
        ObjectResult<String> result = getIntentResult(query);

        //Stores user's avatar url
        setSharedStringPreference(context, PREFS_USER, KEY_USER_AVATAR_URL, result.getObject());
        return result;
    }

    public ObjectResult<String> getUserPhoneNumber() throws IOException {
        establishConnection();

        String query = "getPhoneNum";
        ObjectResult<String> result = getIntentResult(query);

        //Stores phone number locally to device
        setSharedStringPreference(context, PREFS_USER, KEY_USER_PHONE, result.getObject());
        return result;
    }

    public ObjectResult<String> getEmailPreference() throws IOException {
        establishConnection();

        String query = "getEmailPref";
        ObjectResult<String> result = getIntentResult(query);

        //Stores email contact preference locally
        setSharedStringPreference(context, PREFS_NOTIFICATIONS, KEY_NOTIFY_EMAIL, result.getObject());
        return result;
    }

    public ObjectResult<Integer> getNewMessages() throws IOException {
        ObjectResult<Integer> result = new ObjectResult<Integer>(StatusCodeParser.CONNECT_FAILED, null);

        if(!isUserLoggedIn(context)) //If user is not logged in, do attempt to poll new messages.
            return result;

        establishConnection();
        String query = "intent=hasNewMessages";

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            int requestStatus = jsonObject.getInt(STATUS);

            int messages = jsonObject.getInt(MESSAGE);
            result = new ObjectResult<Integer>(requestStatus, messages);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        //Stores amount of unread messages here
        setSharedIntPreferences(context, PREFS_USER, KEY_USER_MESSAGES, result.getObject());
        return result;
    }

    public String setEmailPreference(String preference) throws IOException {
        establishConnection();

        String query = "intent=setEmailPref&pref=" + preference;
        setSharedStringPreference(context, PREFS_NOTIFICATIONS, KEY_NOTIFY_EMAIL, preference); //Stores email contact preference
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Sends the registration id, which will be used to receive push notifications, to the server
    public String setRegistrationId(String deviceToken) throws IOException {
        establishConnection();

        String query = "intent=addAndroidDeviceId&deviceId=" + deviceToken;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    public String changeEmail(String password, String email) throws IOException {
        establishConnection();

        String query = "intent=controlPanel&oldPw=" + password + "&email=" + email;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse;
    }

    public String changePhoneNumber(String password, String phoneNumber) throws IOException {
        establishConnection();

        String query = "intent=controlPanel&oldPw=" + password + "&phone=" + phoneNumber;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse;
    }

    public String changePassword(String password, String newPassword) throws IOException {
        establishConnection();

        String query = "intent=controlPanel&oldPw=" + password + "&newPw=" + newPassword;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            clearUserInfo();
            clearCookies();
            disconnectAll();
        }

        return serverResponse;
    }

    //Used for repetitious intents like get username, phone number, email preference etc.
    public String simpleGetIntent(String intentValue) throws IOException {
        establishConnection();

        String query = "intent=" + intentValue;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }

        if (intentValue.equals(INTENT_GET_PHONENUM))
            setSharedStringPreference(context, PREFS_USER, KEY_USER_PHONE, serverResponse); //Stores phone number locally to device
        else if (intentValue.equals(INTENT_GET_EMAILPREF))
            setSharedStringPreference(context, PREFS_NOTIFICATIONS, KEY_NOTIFY_EMAIL, serverResponse); //Stores email contact preference
        else
            Log.e(TAG, "Intent unread: " + intentValue);

        return serverResponse;
    }

    public String uploadUserAvatar(String imagePath) throws IOException {
        establishConnection();

        httpPost.removeHeaders("Content-Type"); //Handled by MultipartEntityBuilder, cause conflictions

        File file = new File(imagePath);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("intent", new StringBody("uploadAvatar", ContentType.TEXT_PLAIN));
        builder.addPart("avatar", new FileBody(file, ContentType.create("image/jpeg"), "avatar_image"));

        String serverResponse = null;

        try {
            HttpEntity entity = builder.build();
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    public String uploadUserAvatar(InputStream inStream) throws IOException {
        establishConnection();

        httpPost.removeHeaders("Content-Type"); //Handled by MultipartEntityBuilder, cause conflictions

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("intent", new StringBody("uploadAvatar", ContentType.TEXT_PLAIN));
        builder.addPart("avatar", new CustomInputStreamBody(inStream, ContentType.create("image/jpeg"), "avatar.jpg"));

        String serverResponse = null;

        try {
            HttpEntity entity = builder.build();
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    //Create a new message conversation with a user. Regarding a specific post
    public int createMessageThread(String postIdentifier, String message) throws IOException{
        establishConnection();

        int serverResponse = StatusCodeParser.CONNECT_FAILED;
        String query = "intent=createMessageThread&post_id="+postIdentifier+"&message="+message;

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            serverResponse = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    //Mark thread as read
    public int markThreadAsRead(String threadId) throws IOException {
        establishConnection();

        int serverResponse = StatusCodeParser.CONNECT_FAILED;
        String query = "intent=markThreadAsRead&thread_id="+threadId;

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            serverResponse = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    //Reply to add-on to an existing conversation
    public int appendMessage(String threadId, String message) throws IOException {
        establishConnection();

        int serverResponse = StatusCodeParser.CONNECT_FAILED;
        String query = "intent=appendMessage&thread_id="+threadId+"&message="+message;

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            serverResponse = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    //Delete thread
    public int deleteThread(String threadId) throws IOException {
        establishConnection();

        int serverResponse = StatusCodeParser.CONNECT_FAILED;
        String query = "intent=deleteThread&thread_id="+threadId;

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            serverResponse = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    public ObjectResult<ArrayList<MessageThread>> getMessageThreads(int offset, int amount) throws IOException {
        establishConnection();
        ObjectResult<ArrayList<MessageThread>> result = new ObjectResult<ArrayList<MessageThread>>(StatusCodeParser.CONNECT_FAILED, null);

        String query = "intent=getMessageThreadsCurrentUser&offset="+offset+"&amount="+amount;

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            int requestStatus = jsonObject.getInt(STATUS);
            JSONArray payload = jsonObject.getJSONArray(PAYLOAD);

            ArrayList<MessageThread> messageThreads = new ArrayList<MessageThread>();
            for(int i=0; i<payload.length(); i++) {
                JSONObject messageThread = payload.getJSONObject(i);

                String threadId = messageThread.getString("thread_id");
                String postIdentifier = messageThread.getString("post_id");
                String postTitle = StringEscapeUtils.unescapeHtml4(messageThread.getString("post_title"));
                String lastMessage = StringEscapeUtils.unescapeHtml4(messageThread.getString("last_message"));
                int lastUserId = messageThread.getInt("last_user_id");
                String lastUserName = messageThread.getString("last_user_name");
                String lastDateTime = messageThread.getString("datetime");
                int userId = messageThread.getInt("associated_with");
                String userName = messageThread.getString("associated_with_name");
                String userImageUrl = messageThread.getString("associated_with_image");
                int newMessages = messageThread.getInt("new_messages");

                messageThreads.add(new MessageThread(threadId, postIdentifier, postTitle, lastMessage, lastUserName, lastUserId, lastDateTime, userId, userName, userImageUrl, newMessages));
            }

            result = new ObjectResult<ArrayList<MessageThread>>(requestStatus, messageThreads);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return result;
    }

    public ObjectResult<ArrayList<ChatObject>> retrieveThread(String threadId) throws IOException{
        establishConnection();
        ObjectResult<ArrayList<ChatObject>> result = new ObjectResult<ArrayList<ChatObject>>(StatusCodeParser.CONNECT_FAILED, null);

        String query = "intent=retrieveThread&thread_id="+threadId;

        try {
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);
            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            int requestStatus = jsonObject.getInt(STATUS);
            JSONArray payload = jsonObject.getJSONArray(PAYLOAD);

            ArrayList<ChatObject> messages = new ArrayList<ChatObject>();

            for(int i=0; i<payload.length(); i++) {
                JSONObject message = payload.getJSONObject(i);

                int messageId = message.getInt("message_id");
                int value = message.getInt("sentFromMe");
                boolean sentFromMe = (value == 1);
                int senderId = message.getInt("sender_id");
                String senderName = message.getString("sender_name");
                String messageContent = StringEscapeUtils.unescapeHtml4(message.getString("message_content"));
                String dateTime = message.getString("datetime");
                value = message.getInt("message_seen");
                boolean messageSeen = (value == 1);
                String userImageUrl = message.getString("avatar");

                messages.add(new ChatObject(sentFromMe, senderName, messageContent, dateTime, messageSeen, userImageUrl));
            }

            result = new ObjectResult<ArrayList<ChatObject>>(requestStatus, messages);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return result;
    }

    //Get user profile. Search using either username or user id
    public ObjectResult<UserProfileObject> getUserProfile(String name, String uid) throws Exception{
        establishConnection();
        ObjectResult<UserProfileObject> result = new ObjectResult<UserProfileObject>(StatusCodeParser.CONNECT_FAILED, null);

        try {
            String query;

            if(name != null)
                query = "intent=getUserProfile&userName="+name;
            else
                query = "intent=getUserProfile&uid="+uid;

            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));

            JSONObject payload = jsonObject.getJSONObject(PAYLOAD);
            int serverResponse = jsonObject.getInt(STATUS);

            ArrayList<ReferencedPost> postList = new ArrayList<ReferencedPost>();
            String userName = payload.getString("username");
            String userImgUrl = payload.getString("avatarUrl");
            JSONArray userPosts = payload.getJSONArray("posts");

            for(int i=0; i<userPosts.length(); i++) {
                JSONObject post = userPosts.getJSONObject(i);

                String schoolName = post.getString("schoolLongName");
                String schoolAbbv = post.getString("schoolShortName");
                String link = post.getString("post_identifier");
                String title = StringEscapeUtils.unescapeHtml4(post.getString("title"));
                String date = post.getString("date");
                String category = post.getString("category");

                postList.add(new ReferencedPost(schoolName, schoolAbbv, link, category, title, date, "0", 0, false));
            }

            result = new ObjectResult<UserProfileObject>(serverResponse, new UserProfileObject(userName, userImgUrl, postList));

        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        }finally {
            disconnectAll();
        }

        return result;
    }

    public ObjectResult<Post> getPostByIdentifier(String id) throws IOException {
        establishConnection();
        ObjectResult<Post> result = new ObjectResult<Post>(StatusCodeParser.CONNECT_FAILED, null);

        try {
            String query = "intent=getPostByIdentifier&" + id + "==";
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            JSONObject payload = jsonObject.getJSONObject(PAYLOAD);
            int requestStatus = jsonObject.getInt(STATUS);

            String category = payload.getString("category");
            String schoolId = id.split(":")[0];
            String identifier = id.split(":")[1].toLowerCase(Locale.US); //Identifier only holds the unique generated number for the post. Used in image url
            String title = StringEscapeUtils.unescapeHtml4(payload.getString("title"));
            String author = payload.getString("author");
            String isbn = payload.getString("isbn");
            String details = StringEscapeUtils.unescapeHtml4(payload.getString("details"));
            String user = payload.getString("username");
            String date = payload.getString("date");
            String price = payload.getString("price");
            String views = payload.getString("views");

            Post post;

            if (category.equalsIgnoreCase(context.getString(R.string.server_category_book)))
                post = new BookPost(id, schoolId, identifier, title, author, details, isbn, user, null, date, price, views);
            else if (category.equalsIgnoreCase(context.getString(R.string.server_category_tech)))
                post = new TechPost(id, schoolId, identifier, title, details, user, null, date, price, views);
            else if (category.equalsIgnoreCase(context.getString(R.string.server_category_housing)))
                post = new HousingPost(id, schoolId, identifier, title, details, user, null, date, price, views);
            else
                post = new MiscPost(id, schoolId, identifier, title, details, user, null, date, price, views);

            result = new ObjectResult<Post>(requestStatus, post);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally { //If anything happens above, at least disconnect the HttpClient
            disconnectAll();
        }

        return result;
    }

    public ObjectResult<ArrayList<ReferencedPost>> getUserPosts() throws IOException {
        establishConnection();
        ObjectResult<ArrayList<ReferencedPost>> result = new ObjectResult<ArrayList<ReferencedPost>>(StatusCodeParser.CONNECT_FAILED, null);

        try {
            String query = "intent=getPostsCurrentUser";
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            JSONArray payload = jsonObject.getJSONArray(PAYLOAD);
            int requestStatus = jsonObject.getInt(STATUS);
            ArrayList<ReferencedPost> referencedPosts = new ArrayList<ReferencedPost>();

            for (int i = 0; i < payload.length(); i++) {
                JSONObject jsonSchool = payload.getJSONObject(i);
                JSONArray jsonPosts = jsonSchool.getJSONArray("post");

                String shortName = jsonSchool.getString("shortName");
                String longName = jsonSchool.getString("longName");

                for (int j = 0; j < jsonPosts.length(); j++) {
                    JSONObject jsonPost = jsonPosts.getJSONObject(j);

                    String link = jsonPost.getString("link");
                    String category = jsonPost.getString("category");
                    String title = StringEscapeUtils.unescapeHtml4(jsonPost.getString("title"));
                    String date = jsonPost.getString("date");
                    String views = jsonPost.getString("views");
                    int expire = jsonPost.getInt("expire");
                    boolean expired = jsonPost.getBoolean("expired");

                    referencedPosts.add(new ReferencedPost(longName, shortName, link, category, title, date, views, expire, expired));
                }
            }

            result = new ObjectResult<ArrayList<ReferencedPost>>(requestStatus, referencedPosts);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return result;
    }

    public int editPost(String schoolId, String identifier, String title, String description, String price, String tags) throws IOException{
        establishConnection();
        int requestStatus = StatusCodeParser.CONNECT_FAILED;

        try {
            String query = "intent=editPost&school="+schoolId+"&identifier="+identifier+"&title="+title+"&details="+description+"&price="+price+"&tags="+tags;
            HttpEntity entity = new StringEntity(query);
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            requestStatus = jsonObject.getInt(STATUS);

        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return requestStatus;
    }

    public String renewPost(String obsId) throws IOException {
        establishConnection();

        String query = "intent=renewPost&" + obsId + "==";
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    public String removePost(String obsId) throws IOException {
        establishConnection();
        String serverResponse = null;

        try {
            String query = "intent=removePost&" + obsId + "=";
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Send feedback to feedback@walkntrade.com
    public int sendFeedback(String email, String message) throws IOException {
        establishConnection();
        int requestStatus = StatusCodeParser.STATUS_OK;

        try {
            String query = "intent=sendFeedback&email=" + email + "&message=" + message;
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            requestStatus = jsonObject.getInt(STATUS);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return requestStatus;
    }

    // Searches for school, and places schools into given ArrayList. Returns request status code
    public ObjectResult<ArrayList<SchoolObject>> getSchools(String search) throws IOException {
        establishConnection();
        ObjectResult<ArrayList<SchoolObject>> result = new ObjectResult<ArrayList<SchoolObject>>(StatusCodeParser.CONNECT_FAILED, null);

        //    Log.d(TAG, "Downloading school name: "+search);
        try {
            String query = "intent=getSchools&query=" + search;
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            JSONArray payload = jsonObject.getJSONArray(PAYLOAD);

            ArrayList<SchoolObject> schoolObjects = new ArrayList<SchoolObject>();
            int requestStatus = jsonObject.getInt(STATUS);

            for (int i = 0; i < payload.length(); i++)
                schoolObjects.add((new SchoolObject(payload.getJSONObject(i).getString("name"), payload.getJSONObject(i).getString("textId"))));

            result = new ObjectResult<ArrayList<SchoolObject>>(requestStatus, schoolObjects);

        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);
        } finally {
            disconnectAll();
        }

        return result;
    }

    //TODO: Get isbn and author for books
    // Searches for posts, and places posts into given ArrayList. Returns request status code
    public ObjectResult<ArrayList<Post>> getSchoolPosts(String schoolID, String searchQuery, String cat, int offset, int amount) throws IOException {
        establishConnection();
        ObjectResult<ArrayList<Post>> result = new ObjectResult<ArrayList<Post>>(StatusCodeParser.CONNECT_FAILED, null);
        try {
            String query = "intent=getPosts&query=" + searchQuery + "&school=" + schoolID + "&cat=" + cat + "&offset=" + offset + "&sort=0" + "&amount=" + amount;
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);

            JSONObject jsonObject = new JSONObject(readInputAsString(inputStream));
            JSONArray payload = jsonObject.getJSONArray(PAYLOAD);

            ArrayList<Post> posts = new ArrayList<Post>();
            int requestStatus = jsonObject.getInt(STATUS);

            //Retrieve all post attributes from JSONObject
            for (int i = 0; i < payload.length(); i++) {

                JSONObject jsonPost = payload.getJSONObject(i);

                String category = jsonPost.getString("category");
                String obsId = jsonPost.getString("obsId");
                String schoolId = obsId.split(":")[0];
                String identifier = obsId.split(":")[1].toLowerCase(Locale.US); //Identifier only holds the unique generated number for the post. Used in image url
                String title = jsonPost.getString("title");
                String author = "";
                String details = StringEscapeUtils.unescapeHtml4(jsonPost.getString("details"));
                String isbn = "";
                String user = jsonPost.getString("username");
                String imgURL = jsonPost.getString("image");
                String date = jsonPost.getString("date");
                String price = jsonPost.getString("price");
                String views = jsonPost.getString("views");

                if (category.equalsIgnoreCase(context.getString(R.string.server_category_book)))
                    posts.add(new BookPost(obsId, schoolId, identifier, title, author, details, isbn, user, imgURL, date, price, views));
                else if (category.equalsIgnoreCase(context.getString(R.string.server_category_tech)))
                    posts.add(new TechPost(obsId, schoolId, identifier, title, details, user, imgURL, date, price, views));
                else if (category.equalsIgnoreCase(context.getString(R.string.server_category_housing)))
                    posts.add(new HousingPost(obsId, schoolId, identifier, title, details, user, imgURL, date, price, views));
                else
                    posts.add(new MiscPost(obsId, schoolId, identifier, title, details, user, imgURL, date, price, views));
            }

            result = new ObjectResult<ArrayList<Post>>(requestStatus, posts);
        } catch (JSONException e) {
            Log.e(TAG, "Parsing JSON", e);

        } finally { //If anything happens above, at least disconnect the HttpClient
            disconnectAll();
        }
        return result;
    }

    //Add Post to Walkntrade
    public String addPostBook(String category, String school, String title, String author, String description,
                              float price, String tags, int isbn) throws IOException {
        establishConnection();

        String query = "intent=addPost&cat=" + category + "&school=" + school + "&title=" + title +
                "&author=" + author + "&details=" + description + "&price=" + price + "&tags=" + tags + "&isbn=" + isbn;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse; //Returns post identifier or error message
    }

    public String addPostOther(String category, String school, String title, String description,
                               float price, String tags) throws IOException {
        establishConnection();
        String query = "intent=addPost&cat=" + category + "&school=" + school + "&title=" + title + "&details=" + description + "&price=" + price + "&tags=" + tags;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        } finally {
            disconnectAll();
        }
        return serverResponse; //Returns post identifier or error message
    }

    //Adds image to post corresponding to the appropriate identifier
    public String uploadPostImage(String identifier, String imagePath, int index) throws IOException {
        establishConnection();

        httpPost.removeHeaders("Content-Type"); //Handled by MultipartEntityBuilder, cause conflictions

        File file = new File(imagePath);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("intent", new StringBody("uploadPostImages", ContentType.TEXT_PLAIN));
        builder.addPart("iteration", new StringBody(index + "", ContentType.TEXT_PLAIN));
        builder.addPart("identifier", new StringBody(identifier, ContentType.TEXT_PLAIN));
        builder.addPart("image", new FileBody(file));

        String serverResponse = null;

        try {
            HttpEntity entity = builder.build();
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    //Adds image to post corresponding to the appropriate identifier
    public String uploadPostImage(String identifier, InputStream inStream, int index) throws IOException {
        establishConnection();

        httpPost.removeHeaders("Content-Type"); //Handled by MultipartEntityBuilder, cause conflictions

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();

        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("intent", new StringBody("uploadPostImages", ContentType.TEXT_PLAIN));
        builder.addPart("iteration", new StringBody(index + "", ContentType.TEXT_PLAIN));
        builder.addPart("identifier", new StringBody(identifier, ContentType.TEXT_PLAIN));
        builder.addPart("image", new CustomInputStreamBody(inStream, ContentType.create("image/jpeg"), "post_image_" + index + ".jpg"));

        String serverResponse = null;

        try {
            HttpEntity entity = builder.build();
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream);
        } finally {
            disconnectAll();
        }

        return serverResponse;
    }

    //Can be called from anywhere without creating a DataParser object
    public static Bitmap loadBitmap(String link) throws IOException {
        Bitmap bitmap;
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };

        try {
            in = new java.net.URL(url + link).openStream();
            bitmap = BitmapFactory.decodeStream(in);
        } finally {
            in.close();
        }

        return bitmap;
    }

    //Gets a sample sized bitmap, so the device uses less memory to store it
    public static Bitmap loadOptBitmap(String link, int width, int height) throws IOException {
        Bitmap bitmap;
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            in = new java.net.URL(url + link).openStream();
            BitmapFactory.decodeStream(in, null, options);

            options.inSampleSize = getSampleSize(options, width, height);

            options.inJustDecodeBounds = false;
            in = new java.net.URL(url + link).openStream();
            bitmap = BitmapFactory.decodeStream(in, null, options);
        } finally {
            in.close();
        }

        return bitmap;
    }

    public static int getSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
