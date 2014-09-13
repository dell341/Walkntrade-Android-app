package com.walkntrade.io;

//Copyright (c), All Rights Reserved, http://walkntrade.com

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

import com.walkntrade.AccountSettingsChange;
import com.walkntrade.MessageObject;
import com.walkntrade.Messages;
import com.walkntrade.R;
import com.walkntrade.posts.Post;
import com.walkntrade.posts.PostReference;
import com.walkntrade.posts.Post_Book;
import com.walkntrade.posts.Post_Misc;
import com.walkntrade.posts.Post_Service;
import com.walkntrade.posts.Post_Tech;

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
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

//Handles almost all necessary network communications
public class DataParser {
    private static final String url = "http://api.walkntrade.com";
    private static final String TAG = "DATAPARSER";
    public static final String LOGIN_SUCCESS = "success";

    //SharedPreferences Strings
    public static final String PREFS_COOKIES = "CookiesPreferences";
    public static final String PREFS_USER = "UserPreferences";
    public static final String PREFS_SCHOOL = "SchoolPreferences";
    public static final String PREFS_NOTIFICATIONS = "NotificationPreferences";
    public static final String PREFS_AUTHORIZATION = "AuthorizationPreferences";
    public static final String USER_LOGIN = "user_login"; //Cookie title
    public static final String SESS_SEED = "sessionSeed"; //Cookie title
    public static final String SESS_UID = "sessionUid"; //Cookie title
    public static final String S_PREF = "sPref"; //Cookie title
    public static final String USER_NAME = "user_name"; //User-Pref title
    public static final String USER_PHONE = "phone_number"; //User-Pref title
    public static final String USER_EMAIL = "user_email"; //User-Pref title
    public static final String USER_MESSAGES = "user_messages"; //User-Pref title
    public static final String CURRENTLY_LOGGED_IN = "userLoggedIn"; //User-Pref title
    public static final String S_PREF_SHORT = "sPrefShort"; //School Preference title
    public static final String S_PREF_LONG = "sPrefLong"; //School Preference title
    public static final String NOTIFY_EMAIL = "notification_email"; //Notification preference
    public static final String NOTIFY_USER = "notification_status"; //Notification preference title (boolean)
    public static final String NOTIFY_VIBRATE = "notification_vibrate"; //Notification preference title (boolean)
    public static final String NOTIFY_SOUND = "notification_sound"; //Notification preference title
    public static final String NOTIFY_LIGHT = "notification_light"; //Notification preference title (boolean)
    public static final String AUTHORIZED = "user_authorization"; //Authorization-Pref title (boolean) User password changed or session id expired.
    public static final String BLANK = " ";

    //Non-unique intent declarations here
    public static final String INTENT_GET_USERNAME = "getUserName";
    public static final String INTENT_GET_AVATAR = "getAvatar";
    public static final String INTENT_GET_EMAILPREF = "getEmailPref";
    public static final String INTENT_GET_PHONENUM = "getPhoneNum";
    public static final String INTENT_GET_NEWMESSAGE = "pollNewWebmail";

    private AndroidHttpClient httpClient; //Android Client, Uses User-Agent, and executes request
    private HttpContext httpContext; //Contains CookieStore that is sent along with request
    private CookieStore cookieStore; //Holds cookies from server
    private HttpPost httpPost; //Contains message to be sent to client
    private final String USER_AGENT = System.getProperty("http.agent"); //Unique User-Agent of current device

    //Initialized here to enable usage within Handler classes
    private ArrayList<String> schools;
    private ArrayList<Post> schoolPosts;
    private ArrayList<PostReference> userPosts;
    private ArrayList<MessageObject> messages;
    private Post post;
    private String identifier;
    private String schoolID;
    private String schoolName;

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
        httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setHeader("Cookie", sessionSeedCookie + ";" + sessionUidCookie + ";" + userLoginCookie + ";" + sPrefCookie);
    }

    //Called after communication is complete
    private void disconnectAll() {
        httpClient.close();
    }

    //Get Cookies already stored on device
    private void getCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, Context.MODE_PRIVATE);
        userLoginCookie = settings.getString(USER_LOGIN, BLANK);
        sessionSeedCookie = settings.getString(SESS_SEED, BLANK);
        sessionUidCookie = settings.getString(SESS_UID, BLANK);

        //Short name of school will be used in the cookies
        settings = context.getSharedPreferences(PREFS_SCHOOL, Context.MODE_PRIVATE);
        sPrefCookie = settings.getString(S_PREF_SHORT, "sPref=" + BLANK);
    }

    //Update all cookies with new values from Cookie Store
    private void updateCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        List<Cookie> cookieList = cookieStore.getCookies();
        for (Cookie cookie : cookieList) {
            if (cookie.getName().equals(USER_LOGIN))
                editor.putString(USER_LOGIN, cookie.getName() + "=" + cookie.getValue());
            else if (cookie.getName().equals(SESS_SEED))
                editor.putString(SESS_SEED, cookie.getName() + "=" + cookie.getValue());
            else if (cookie.getName().equals(SESS_UID))
                editor.putString(SESS_UID, cookie.getName() + "=" + cookie.getValue());
            else
                Log.e(TAG, "Found Extra Cookie: " + cookie.getName());
        }
        settings = context.getSharedPreferences(PREFS_SCHOOL, Context.MODE_PRIVATE);
        sPrefCookie = settings.getString(S_PREF_SHORT, "sPref=" + BLANK);

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

    private String readInputAsString(InputStream inStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder builder = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null)
            builder.append(line);

        inStream.close();

        return builder.toString();
    }

    //Sends out POST request and returns an InputStream
    private InputStream processRequest(HttpEntity entity) throws IOException {
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.execute(httpPost, httpContext); //Executes the request along with the cookie store

        updateCookies();
        return response.getEntity().getContent();
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
    public void setSharedStringPreference(String preferenceName, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);

        editor.apply(); //Save changes to the SharedPreferences
    }

    public void setSharedIntPreferences(String preferenceName, String key, int value) {
        SharedPreferences settings = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
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

        if(key.equals(S_PREF_SHORT))
            return settings.getString(S_PREF_SHORT, null).split("=")[1]; //sPref=[school] is split and the second index [school] is returned

        return settings.getString(key, null);
    }

    //Since the default value is true, only use this with switches that should be true
    public static boolean getSharedBooleanPreference(Context _context, String preferenceName, String key) {
        SharedPreferences settings = _context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        return settings.getBoolean(key, true);
    }

    public static void setSoundPref(Context _context, Uri uri) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(NOTIFY_SOUND, uri.toString());

        editor.apply();
    }

    public static String getSoundPref(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE);
        return settings.getString(NOTIFY_SOUND, null);
    }

    public static int getMessagesAmount(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
        return settings.getInt(USER_MESSAGES, 0);
    }

    //Returns user login status
    public static boolean isUserLoggedIn(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
        return settings.getBoolean(DataParser.CURRENTLY_LOGGED_IN, false);
    }

    //Requirement check for registration
    public boolean isUserNameFree(String username) throws IOException {
        establishConnection();

        String query = "intent=checkUsername&username=" + username;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }

        return serverResponse.equals("0");
    }

    //Attempts to register User account into server
    public String registerUser(String username, String email, String password, String phoneNum) throws IOException {
        establishConnection();

        String query = "intent=addUser&username=" + username + "&email=" + email + "&password=" + password + "&phone" + phoneNum;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
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
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Login User into Walkntrade
    public String login(String email, String password) throws IOException {
        establishConnection(); //Instantiate all streams and opens the connection
        String query = "intent=login&password=" + password + "&email=" + email + "&rememberMe=true";
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
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

    //Used for repetitious intents like get username, phone number, email preference etc.
    public String simpleGetIntent(String intentValue) throws IOException {
        establishConnection();

        String query = "intent="+intentValue;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }

        if(intentValue.equals(INTENT_GET_USERNAME))
            setSharedStringPreference(PREFS_USER, USER_NAME, serverResponse); //Stores username locally to device
        else if(intentValue.equals(INTENT_GET_PHONENUM))
            setSharedStringPreference(PREFS_USER, USER_PHONE, serverResponse); //Stores phone number locally to device
        else if(intentValue.equals(INTENT_GET_NEWMESSAGE))
            setSharedIntPreferences(PREFS_USER, USER_MESSAGES, Integer.parseInt(serverResponse)); //Stores amount of unread messages here
        else if(intentValue.equals(INTENT_GET_EMAILPREF))
            setSharedStringPreference(PREFS_NOTIFICATIONS, NOTIFY_EMAIL, serverResponse); //Stores email contact preference
        else
            Log.e(TAG, "Intent unread: "+intentValue);

        return serverResponse;
    }

    public String uploadUserAvatar(String imagePath) throws IOException{
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

    public String setEmailPreference(String preference) throws IOException {
        establishConnection();

        String query = "intent=setEmailPref&pref="+preference;
        setSharedStringPreference(PREFS_NOTIFICATIONS, NOTIFY_EMAIL, preference); //Stores email contact preference
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Sends the registration id, which will be used to receive push notifications, to the server
    public String setRegistrationId(String deviceToken) throws IOException {
        establishConnection();

        String query = "intent=addAndroidDeviceId&deviceId="+deviceToken;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }

        return serverResponse;
    }


    public String changeSetting(String password, String settingValue, int setting) throws IOException{
        establishConnection();

        String query;

        switch (setting){
            case AccountSettingsChange.SETTING_EMAIL:
                query = "intent=controlPanel&oldPw="+password+"&email="+settingValue;
                setSharedStringPreference(PREFS_USER, USER_EMAIL, settingValue);
                break;
            case AccountSettingsChange.SETTING_PHONE:
                query = "intent=controlPanel&oldPw="+password+"&phone="+settingValue;
                setSharedStringPreference(PREFS_USER, USER_PHONE, settingValue);
                break;
            case AccountSettingsChange.SETTING_PASSWORD:
                query = "intent=controlPanel&oldPw="+password+"&newPw="+settingValue;
                clearUserInfo();
                clearCookies();
                break;
            default:
                disconnectAll();
                return "Setting not selected";
        }

        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
    }

    public String messageUser(String userName, String title, String message) throws IOException {
        establishConnection();

        String query = "intent=messageUser&uuid=&userName=" + userName + "&title=" + title + "&message=" + message;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
    }

    public ArrayList<MessageObject> getMessages(int _messageType) throws Exception{
        establishConnection();

        try {
            messages = new ArrayList<MessageObject>();
            final int messageType = _messageType;
            String query;

            if(messageType == Messages.RECEIVED_MESSAGES)
                query = "intent=getWebmail";
            else
                query = "intent=getSentWebmail";

            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inStream = processRequest(entity);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler xmlHandler = new DefaultHandler(){
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    String id = "DNE";
                    String user = "DNE";
                    String subject = "DNE";
                    String contents = "DNE";
                    String date = "DNE";
                    String read = "DNE";

                    for(int i=0; i<attributes.getLength(); i++){
                        if (attributes.getLocalName(i).equalsIgnoreCase("id"))
                            id = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("to") && messageType == Messages.SENT_MESSAGES)
                            user = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("from") && messageType == Messages.RECEIVED_MESSAGES)
                            user = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("subject"))
                            subject = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("message"))
                            contents = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("datetime"))
                            date = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("read"))
                            read = attributes.getValue(i);
                    }

                    //The last attribute to be initialized is the date, end of message
                    if(messageType == Messages.RECEIVED_MESSAGES) { //Received messages need the read attribute
                        if (!read.equals("DNE"))
                            messages.add(new MessageObject(id, user, subject, contents, date, read));
                    }
                    else if(!date.equals("DNE")) //Sent messages do not have the read attribute
                        messages.add(new MessageObject(id, user, subject, contents, date, read));
                }
            };
            saxParser.parse(inStream, xmlHandler);
        } finally {
            disconnectAll();
        }

        return messages;
    }

    //Currently not used to retrieve message, but just mark it as read. Server marks a message as read, when this intent is called.
    public void getMessage(String id) throws IOException{
        establishConnection();

        String query = "intent=getMessage&message_id="+id;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            processRequest(entity);
        }
        finally {
            disconnectAll();
        }
    }

    public String removeMessage(String id) throws IOException{
        establishConnection();

        String query = "intent=removeMessage&message_id="+id;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
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
        }
        finally {
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
        }
        finally {
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
        }
        finally {
        disconnectAll(); }

        return serverResponse;
    }
    public String removePost(String obsId) throws IOException {
        establishConnection();

        String query = "intent=removePost&" + obsId + "=";
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
    }

    //Send feedback to feedback@walkntrade.com
    public String sendFeedback(String email, String message) throws IOException{
        establishConnection();

        String query = "intent=sendFeedback&email="+email+"&message="+message;
        String serverResponse = null;

        try {
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inputStream = processRequest(entity);
            serverResponse = readInputAsString(inputStream); //Reads message response from server
        }
        finally {
            disconnectAll();
        }
        return serverResponse;
    }

    // Gets keyword from user and returns an ArrayList of school names
    public ArrayList<String> getSchools(String userInput) throws Exception {
        establishConnection(); // Instantiate all streams and opens the
        // connection

        schools = new ArrayList<String>();
        try {
            String query = "intent=getSchools&query=" + userInput;
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inStream = processRequest(entity);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            // Handler that parses through the XML file and retrieves a specific
            // attribute name
            DefaultHandler xmlHandler = new DefaultHandler() {
                public void startElement(String uri, String localName,
                                         String qName, Attributes attributes) throws SAXException {
                    for (int i = 0; i < attributes.getLength(); i++)
                        if (attributes.getLocalName(i).equalsIgnoreCase("NAME"))
                            schools.add(attributes.getValue(i));
                }
            };
            saxParser.parse(inStream, xmlHandler);
        } finally { //If anything happens above, at least disconnect the HttpClient
            disconnectAll();
        }

        return schools;
    }

    public String getSchoolId(String _schoolName) throws Exception {
        establishConnection();
        schoolID = "School Not Found";//Will be returned if the school is not found
        schoolName = _schoolName;

        try {
            String query = "intent=getSchools&query=" + _schoolName;
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inStream = processRequest(entity);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler xmlHandler = new DefaultHandler() {
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    for (int i = 0; i < attributes.getLength(); i++) {
                        if (attributes.getValue(i).equalsIgnoreCase(schoolName))
                            schoolID = attributes.getValue(i + 1);
                    }
                }
            };
            saxParser.parse(inStream, xmlHandler);
        } finally { //If anything happens above, at least disconnect the HttpClient
            disconnectAll();
        }
        return schoolID;
    }

    //TODO: Get isbn and author for books
    //Returns all the posts from the specified school and category
    public ArrayList<Post> getSchoolPosts(String schoolID, String searchQuery, String category, int offset, int amount) throws Exception {

        establishConnection();
        try {
            schoolPosts = new ArrayList<Post>();

            String query = "intent=getPosts&query=" + searchQuery + "&school=" + schoolID + "&cat=" + category + "&offset=" + offset + "&sort=0" + "&amount=" + amount;
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inStream = processRequest(entity);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler xmlHandler = new DefaultHandler() {
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    String identifier = "DNE";
                    String title = "DNE";
                    String category = "DNE";
                    String details = "DNE";
                    String author = "DNE";
                    String price = "DNE";
                    String imgURL = "DNE";
                    String date = "DNE";
                    String views = "DNE";

                    for (int i = 0; i < attributes.getLength(); i++) {

                        //Android does not support Java 7, so switch-case on Strings cannot be done
                        if (attributes.getLocalName(i).equalsIgnoreCase("obsId")) {
                            String obsID = attributes.getValue(i); //OBSID also includes school id
                            String splitID[] = obsID.split(":");
                            identifier = splitID[1]; //Identifier only holds the unique generated number for the post. Used in image url
                            identifier = identifier.toLowerCase(Locale.US);//Some IDs contain capital letters, doesn't meet regex requirement of DiskLruCache
                        } else if (attributes.getLocalName(i).equalsIgnoreCase("title"))
                            title = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("category"))
                            category = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("details"))
                            details = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("username"))
                            author = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("price"))
                            price = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("image"))
                            imgURL = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("date"))
                            date = attributes.getValue(i);
                        else if (attributes.getLocalName(i).equalsIgnoreCase("views"))
                            views = attributes.getValue(i);

                        //The last attribute to be initialized, views, will mark end of first post
                        if (!views.equals("DNE")) {
                            if (category.equalsIgnoreCase(context.getString(R.string.server_category_book)))
                                schoolPosts.add(new Post_Book(identifier, title, details, author, imgURL, date, price, views));
                            else if (category.equalsIgnoreCase(context.getString(R.string.server_category_tech)))
                                schoolPosts.add(new Post_Tech(identifier, title, details, author, imgURL, date, price, views));
                            else if (category.equalsIgnoreCase(context.getString(R.string.server_category_service)))
                                schoolPosts.add(new Post_Service(identifier, title, details, author, imgURL, date, price, views));
                            else if (category.equalsIgnoreCase(context.getString(R.string.server_category_misc)))
                                schoolPosts.add(new Post_Misc(identifier, title, details, author, imgURL, date, price, views));
                        }
                    }
                }
            };
            saxParser.parse(inStream, xmlHandler);
        } finally { //If anything happens above, at least disconnect the HttpClient
            disconnectAll();
        }
        return schoolPosts;
    }

    public ArrayList<PostReference> getUserPosts() throws Exception {
        establishConnection();

        userPosts = new ArrayList<PostReference>();

        try {
            String query = "intent=getPostsCurrentUser";
            HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
            InputStream inStream = processRequest(entity);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler xmlHandler = new DefaultHandler() {
                private boolean parsingSchool = false;
                private String currentSchool = "";

                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    String link = "DNE";
                    String category = "DNE";
                    String title = "DNE";
                    String date = "DNE";
                    String views = "DNE";
                    String expire = "DNE";
                    String expired = "DNE";

                    if (qName.equalsIgnoreCase("SCHOOL"))//At the start of this element are we parsing a new school
                        parsingSchool = true;

                    for (int i = 0; i < attributes.getLength(); i++) {
                        if (attributes.getLocalName(i).equalsIgnoreCase("LONGNAME")) //Gets current school name
                            currentSchool = attributes.getValue(i);

                        if (parsingSchool) {
                            if (attributes.getLocalName(i).equalsIgnoreCase("link"))
                                link = attributes.getValue(i);
                            else if (attributes.getLocalName(i).equalsIgnoreCase("category"))
                                category = attributes.getValue(i);
                            else if (attributes.getLocalName(i).equalsIgnoreCase("title"))
                                title = attributes.getValue(i);
                            else if (attributes.getLocalName(i).equalsIgnoreCase("date"))
                                date = attributes.getValue(i);
                            else if (attributes.getLocalName(i).equalsIgnoreCase("views"))
                                views = attributes.getValue(i);
                            else if (attributes.getLocalName(i).equalsIgnoreCase("expire"))
                                expire = attributes.getValue(i);
                            else if (attributes.getLocalName(i).equalsIgnoreCase("expired"))
                                expired = attributes.getValue(i);

                            //The last attribute to be initialized, views, will mark end of first post
                            if (!expired.equals("DNE"))
                                userPosts.add(new PostReference(currentSchool, link, category, title, date, views));
                        }
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("SCHOOL")) { //Reached the end of current school
                        parsingSchool = false;
                    }
                }
            };
            saxParser.parse(inStream, xmlHandler);
        } finally {
            disconnectAll();
        }

        return userPosts;
    }

    //Can be called from anywhere without creating a DataParser object
    public static Bitmap loadBitmap(String _url) throws IOException {
        Bitmap bitmap;
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };

        try {
            in = new java.net.URL("http://walkntrade.com/" + _url).openStream();
            bitmap = BitmapFactory.decodeStream(in);
        }
        finally {
            in.close();
        }

        return bitmap;
    }

    //Gets a sample sized bitmap, so the device uses less memory to store it
    public static Bitmap loadOptBitmap(String _url, int width, int height) throws IOException {
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

            in = new java.net.URL("http://walkntrade.com/" + _url).openStream();
            BitmapFactory.decodeStream(in, null, options);

            options.inSampleSize = getSampleSize(options, width, height);
            Log.v(TAG, "Sample size: "+options.inSampleSize);

            options.inJustDecodeBounds = false;
            in = new java.net.URL("http://walkntrade.com/" + _url).openStream();
            bitmap = BitmapFactory.decodeStream(in, null, options);
        }
        finally {
            in.close();
        }

        return bitmap;
    }

    public static int getSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth){
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while((halfHeight / inSampleSize) > reqHeight && (halfWidth/inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
   }
}
