package com.mega.android;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

/*
 * LRU thumbnails cache to display in list
 */
public class ThumbnailCache {
	
	List<Long>nulls;
	LruThumbnailCache cache;
	
	public ThumbnailCache() {
		cache = new LruThumbnailCache(70);
		nulls = new ArrayList<Long>();
	}
	
	/*
	 * Put new item into cache
	 */
	public void put(Long key, Bitmap value) {
		if (value == null) {
			nulls.add(key);
		} else {
			cache.put(key, value);
		}
	}
	
	/*
	 * Remove item from the cache
	 */
	public void remove(Long key) {
		nulls.remove(key);
		cache.remove(key);
	}
	
	/*
	 * Get item from the cache
	 */
	public Bitmap get(Long key) {
		return cache.get(key);
	}
	
	/*
	 * Check is cache contains key
	 */
	public boolean containsKey(Long key) {
		if (cache.get(key) != null) {
			return true;
		}
		if (nulls.contains(key)) {
			return true;
		}
		return false;
	}
	
	private static class LruThumbnailCache extends LruCache<Long, Bitmap> {

		public LruThumbnailCache(int maxSize) {
			super(maxSize);
		}
	
		protected void entryRemoved (boolean evicted, String key, Bitmap oldValue, Bitmap newValue)
		{
			try {
				oldValue.recycle();
			}catch(Exception e) {}
		}
	}
}
