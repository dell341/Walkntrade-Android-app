package com.walkntrade.io;

//Copyright (c), All Rights Reserved, http://walkntrade.com

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.AndroidHttpClient;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

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
    public static final String USER_LOGIN = "user_login"; //Cookie title
    public static final String SESS_SEED = "sessionSeed"; //Cookie title
    public static final String SESS_UID = "sessionUid"; //Cookie title
    public static final String S_PREF = "sPref"; //Cookie title
    public static final String USER_NAME = "user_name"; //User-Pref title
    public static final String PHONE_NUM = "phone_number"; //User-Pref title
    public static final String CURRENTLY_LOGGED_IN = "userLoggedIn"; //User-Pref title
    public static final String S_PREF_SHORT = "sPrefShort"; //School Preference title
    public static final String S_PREF_LONG = "sPrefLong"; //School Preference title
    public static final String BLANK = " ";

    private AndroidHttpClient httpClient; //Android Client, Uses User-Agent, and executes request
    private HttpContext httpContext; //Contains CookieStore that is sent along with request
    private CookieStore cookieStore; //Holds cookies from server
    private HttpPost httpPost; //Contains message to be sent to client
    private final String USER_AGENT = System.getProperty("http.agent"); //Unique User-Agent of current device

    //Initialized here to enable usage within Handler classes
    private ArrayList<String> schools;
    private ArrayList<Post> schoolPosts;
    private ArrayList<PostReference> userPosts;
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
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, 0);
        userLoginCookie = settings.getString(USER_LOGIN, BLANK);
        sessionSeedCookie = settings.getString(SESS_SEED, BLANK);
        sessionUidCookie = settings.getString(SESS_UID, BLANK);

        //Short name of school will be used in the cookies
        settings = context.getSharedPreferences(PREFS_SCHOOL, 0);
        sPrefCookie = settings.getString(S_PREF_SHORT, "sPref=" + BLANK);
    }

    //Update all cookies with new values from Cookie Store
    private void updateCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, 0);
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
        settings = context.getSharedPreferences(PREFS_SCHOOL, 0);
        sPrefCookie = settings.getString(S_PREF_SHORT, "sPref=" + BLANK);

        editor.commit(); //Save changes to the SharedPreferences
    }

    //Clear locally stored cookies
    private void clearCookies() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_COOKIES, 0);
        settings.edit().clear().commit();
    }

    //Clear locally stored user information
    private void clearUserInfo() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_USER, 0);
        settings.edit().clear().commit();
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

    //Sets the selected school preference. Used when adding post to server
    public void setSchoolPref(String school) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SCHOOL, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(S_PREF_SHORT, "sPref=" + school);

        editor.commit(); //Save changes to the SharedPreferences
    }

    //Returns the school preference. The most recently visited school page
    public static String getSchoolPref(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_SCHOOL, 0);
        return settings.getString(S_PREF_SHORT, null).split("=")[1]; //sPref=[school] is split and the second index [school] is returned
    }

    //Sets the long name for school preference. Used to start app on School Page Activity
    public void setSchoolLongPref(String school) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_SCHOOL, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(S_PREF_LONG, school);

        editor.commit();
    }

    public static String getSchoolLongPref(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_SCHOOL, 0);
        return settings.getString(S_PREF_LONG, null);
    }

    //Stores username in SharedPreferences to avoid network connection every time
    private void setNamePref(String userName) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_USER, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(USER_NAME, userName);

        editor.commit();
    }

    //Gets the stored username to avoid network connection every time
    public static String getNamePref(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_USER, 0);
        return settings.getString(USER_NAME, null);
    }

    //Stores phone number in SharedPreferences
    private void setPhonePref(String phoneNum) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_USER, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PHONE_NUM, phoneNum);

        editor.commit();
    }

    //Gets the stored phone number
    public static String getPhonePref(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_USER, 0);
        return settings.getString(PHONE_NUM, null);
    }

    //Returns user login status
    public static boolean isUserLoggedIn(Context _context) {
        SharedPreferences settings = _context.getSharedPreferences(PREFS_USER, 0);
        return settings.getBoolean(DataParser.CURRENTLY_LOGGED_IN, false);
    }

    //Requirement check for registration
    public boolean isUserNameFree(String username) throws IOException {
        establishConnection();

        String query = "intent=checkUsername&username=" + username;

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
        return serverResponse.equals("0");
    }

    //Attempts to register User account into server
    public String registerUser(String username, String email, String password, String phoneNum) throws IOException {
        establishConnection();

        String query = "intent=addUser&username=" + username + "&email=" + email + "&password=" + password + "&phone" + phoneNum;

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server);

        disconnectAll();
        return serverResponse;
    }

    //Verifies user based on key provided
    public String verifyUser(String key) throws IOException {
        establishConnection();

        String query = "intent=verifyKey&key=" + key;

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
        return serverResponse;
    }

    //Logins User into Walkntrade
    public String login(String email, String password) throws IOException {
        establishConnection(); //Instantiate all streams and opens the connection
        String query = "intent=login&password=" + password + "&email=" + email + "&rememberMe=true";

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
        return serverResponse;
    }

    //Logs user out of Walkntrade
    public void logout() throws IOException {
        establishConnection();

        String query = "intent=logout";

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        clearCookies(); //Clears locally stored cookies
        clearUserInfo(); //Clears locally stored user information

        disconnectAll();
    }

    public String getUserName() throws IOException {
        establishConnection();

        String query = "intent=getUserName";

        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        //Stores username locally to device
        setNamePref(serverResponse);

        disconnectAll();
        return serverResponse;
    }

    public String getUserAvatar() throws IOException {
        establishConnection();

        String query = "intent=getAvatar";
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
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

        HttpEntity entity = builder.build();
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream);

        disconnectAll();

        return serverResponse;
    }

    public String getEmailPref() throws IOException {
        establishConnection();

        String query = "intent=getEmailPref";
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream);

        disconnectAll();
        return serverResponse;
    }

    public String setEmailPref(int preference) throws IOException {
        establishConnection();

        String query = "intent=setEmailPref&pref="+preference;
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream);

        disconnectAll();
        return serverResponse;
    }

    public String getPhoneNumber() throws IOException {
        establishConnection();

        String query = "intent=getPhoneNum";
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream);

        //Stores phone number locally to device
        setPhonePref(serverResponse);

        disconnectAll();
        return serverResponse;
    }

    public String messageUser(String userName, String title, String message) throws IOException {
        establishConnection();

        String query = "intent=messageUser&uuid=&userName=" + userName + "&title=" + title + "&message=" + message;
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
        return serverResponse;
    }

    //Add Post to Walkntrade
    public String addPostBook(String category, String school, String title, String author, String description,
                              float price, String tags, int isbn) throws IOException {
        establishConnection();

        String query = "intent=addPost&cat=" + category + "&school=" + school + "&title=" + title +
                "&author=" + author + "&details=" + description + "&price=" + price + "&tags=" + tags + "&isbn=" + isbn;
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
        return serverResponse; //Returns post identifier or error message
    }

    public String addPostOther(String category, String school, String title, String description,
                               float price, String tags) throws IOException {
        establishConnection();
        String query = "intent=addPost&cat=" + category + "&school=" + school + "&title=" + title + "&details=" + description + "&price=" + price + "&tags=" + tags;
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
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

        HttpEntity entity = builder.build();
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream);

        disconnectAll();

        return serverResponse;
    }

    public String removePost(String obsId) throws IOException {
        establishConnection();

        String query = "intent=removePost&" + obsId + "=";
        HttpEntity entity = new StringEntity(query); //wraps the query into a String entity
        InputStream inputStream = processRequest(entity);
        String serverResponse = readInputAsString(inputStream); //Reads message response from server

        disconnectAll();
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

    public Post getFullPost(String id, String sId) throws Exception {
        establishConnection();
        identifier = id;
        schoolID = sId;

        try {
            String query = "intent=getPostByIdentifier&" + schoolID+":"+identifier + "==";
            HttpEntity entity = new StringEntity(query);
            InputStream inStream = processRequest(entity);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler xmlHandler = new DefaultHandler() {

                private String currentElement;
                private boolean parsingPost = false;

                String title = "DNE";
                String category = "DNE";
                String details = "DNE";
                String user = "DNE";
                String price = "DNE";
                String imgURL = "DNE";
                String date = "DNE";
                String views = "DNE";

                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    currentElement = qName;

                    if(qName.equalsIgnoreCase("POST"))
                        parsingPost = true;
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {

                    if (currentElement.equalsIgnoreCase("TITLE"))
                        title = new String(ch, start, length);
                    else if (currentElement.equalsIgnoreCase("CATEGORY"))
                        category = new String(ch, start, length);
                    else if (currentElement.equalsIgnoreCase("DETAILS"))
                        details = new String(ch, start, length);
                    else if (currentElement.equalsIgnoreCase("USERNAME"))
                        user = new String(ch, start, length);
                    else if (currentElement.equalsIgnoreCase("PRICE"))
                        price = new String(ch, start, length);
                    else if (currentElement.equalsIgnoreCase("DATE"))
                        date = new String(ch, start, length);
                    else if (currentElement.equalsIgnoreCase("VIEWS"))
                        views = new String(ch, start, length);
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {

                    if(qName.equalsIgnoreCase("POST"))
                        parsingPost = false;

                    if(!parsingPost) { //End of Post has been reached
                        if (category.equalsIgnoreCase(context.getString(R.string.server_category_book)))
                            post = new Post_Book(identifier, title, details, user, imgURL, date, price, views);
                        else if (category.equalsIgnoreCase(context.getString(R.string.server_category_tech)))
                            post = new Post_Tech(identifier, title, details, user, imgURL, date, price, views);
                        else if (category.equalsIgnoreCase(context.getString(R.string.server_category_service)))
                            post = new Post_Service(identifier, title, details, user, imgURL, date, price, views);
                        else if (category.equalsIgnoreCase(context.getString(R.string.server_category_misc)))
                            post = new Post_Misc(identifier, title, details, user, imgURL, date, price, views);
                    }
                    currentElement = "";
                }
            };
            saxParser.parse(inStream, xmlHandler);
        } finally { //If anything happens above, at least disconnect the HttpClient
            disconnectAll();
        }

        return post;
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

                            //The last attribute to be initialized, views, will mark end of first post
                            if (!views.equals("DNE"))
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

        InputStream in = new java.net.URL("http://walkntrade.com/" + _url).openStream();
        bitmap = BitmapFactory.decodeStream(in);

        in.close();

        return bitmap;
    }
}
