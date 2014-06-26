package com.walkntrade.io;

//Copyright (c), All Rights Reserved, http://walkntrade.com

//Various sources and code implemented from Android Studio project, under the following license:
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//Caches all images to reduce loading time
public class DiskLruImageCache {
	
	private static final String TAG = "DiskLruImageCache";
	public static final String IMAGE_DIRECTORY = "_image_posts";
	public static final String USER_IMAGE= "userimage";
	private static final int APP_VERSION = 1;
	private static final int VALUE_COUNT = 1;
	private static final long MAX_LIMIT = 10485760L; //10MB, max space cache will occupy
	private final Object mDiskCacheLock = new Object();
	private final int DISK_CACHE_INDEX = 0;
	
	private DiskLruCache diskCache;
	private File cachingDir;
	
	private int compressQuality = 70; //PNG files will ignore the quality setting
	private boolean mDiskCacheStarting = true;

	
	public DiskLruImageCache(Context _context, String _uniqueDirectory){
		cachingDir = getCacheDir(_context, _uniqueDirectory);
		initDiskCache();
	}
	
	//Creates a sub-directory in the app's cache directory. First attempt is to use SD card
	// if none is available then internal cache is used
	public static File getCacheDir(Context _context, String _uniqueDirectory) {
		//If SD card is mounted, use this as the cache directory
		//Else use the internal cache directory
		final String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ? _context.getExternalCacheDir().getPath() 
				:_context.getCacheDir().getPath(); //For compatibility with API level 8, application does not check for built in external storage
				
		return new File(cachePath + File.separator + _uniqueDirectory);
	}
	
	//Initialize the DiskLruCache object
	public void initDiskCache(){
		synchronized(mDiskCacheLock) {
			if(diskCache == null || diskCache.isClosed()) {
				try {
				diskCache = DiskLruCache.open(cachingDir, APP_VERSION, VALUE_COUNT, MAX_LIMIT);
				} catch (IOException e) {
					Log.e(TAG, "initDiskCache", e);
                    clearCache(); //Just clear the cache if there apparent errors
				}
				mDiskCacheStarting = false; //Finish initializing
				mDiskCacheLock.notifyAll(); //Wake any waiting threads
			}
		}
	}
	
	public void addBitmapToCache(String key, Bitmap bm) {
		if(key == null || bm == null)
			return;
		
		synchronized(mDiskCacheLock) {
			if(diskCache != null) {
				OutputStream out = null;
				try {
					DiskLruCache.Snapshot snapshot = diskCache.get(key);
					if(snapshot == null) {
						final DiskLruCache.Editor editor = diskCache.edit(key);
						if(editor != null) {
							out = editor.newOutputStream(DISK_CACHE_INDEX);
							bm.compress(Bitmap.CompressFormat.JPEG, compressQuality, out);
							editor.commit();
							out.close();
						}
					} else {
						snapshot.getInputStream(DISK_CACHE_INDEX).close();
					}
				} catch (IOException e) {
					Log.e(TAG, "AddBitmapToCache", e);
                    clearCache(); //Just clear the cache if there apparent errors
				} catch(Exception e) {
					Log.e(TAG, "AddBitmapToCache", e);
                    clearCache(); //Just clear the cache if there apparent errors
				} finally {
					try {
						if(out != null)
							out.close();
					}
					catch(IOException e){
                        Log.e(TAG, "AddBitmapToCache", e);
                    }
				}
			}
		}
	}
	
	public Bitmap getBitmapFromDiskCache(String key) {
		Bitmap bitmap = null;
		
		synchronized (mDiskCacheLock) {
			while (mDiskCacheStarting) {
				try {
					mDiskCacheLock.wait();
				} catch(InterruptedException e) {
                    Log.e(TAG, "GetBitmapFromCache", e);
                }
			}
			
			if (diskCache != null) {
				InputStream inputStream = null;
				try {
					final DiskLruCache.Snapshot snapshot = diskCache.get(key);
					if(snapshot != null) {
						inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
						if(inputStream != null) {
							final BufferedInputStream buffIn = new BufferedInputStream(inputStream, 8*1024);
							bitmap = BitmapFactory.decodeStream(buffIn);
						}
					}
				} catch (IOException e) {
					Log.e(TAG, "GetBitmapFromCache", e);
				} finally {
					try {
						if (inputStream != null)
							inputStream.close();
					} catch (IOException e) {
                        Log.e(TAG, "GetBitmapFromCache", e);
                    }
				}
			}
		}
		
		return bitmap;
	}
	
	//Closes the current opened disk cache
	public void close() {
		synchronized(mDiskCacheLock) {
			if (diskCache != null) {
				try {
					if(!diskCache.isClosed()) {
						diskCache.close();
						diskCache = null;
					}
				} catch (IOException e) {
					Log.e(TAG, "Closing Disk Cache", e);
				}
			}
		}
	}
	
	public void clearCache(){
		synchronized(mDiskCacheLock) {
			mDiskCacheStarting = true;
			if (diskCache != null && !diskCache.isClosed()) {
				try {
					diskCache.delete();
				} catch (IOException e) {
					Log.e(TAG, "Clearing Cache", e);
				}
			}
			
			diskCache = null;
			initDiskCache();
		}
	}
	

}
