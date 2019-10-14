package mega.privacy.android.app;

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
	LruThumbnailCachePath cachePath;
	
	public ThumbnailCache() {
		cache = new LruThumbnailCache(70);
		cachePath = new LruThumbnailCachePath(70);
		nulls = new ArrayList<Long>();
	}
	
	public ThumbnailCache(int value){
		if (value == 0){
			cache = new LruThumbnailCache(70);
			nulls = new ArrayList<Long>();
		}
		else{
			cachePath = new LruThumbnailCachePath(70);
		}
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
	
	public void put(String key, Bitmap value){
		cachePath.put(key, value);
	}
	
	/*
	 * Remove item from the cache
	 */
	public void remove(Long key) {
		nulls.remove(key);
		cache.remove(key);
	}
	
	public void remove(String key){
		cachePath.remove(key);
	}
	
	/*
	 * Get item from the cache
	 */
	public Bitmap get(Long key) {
		return cache.get(key);
	}
	
	public Bitmap get(String key){
		return cachePath.get(key);
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
	
	public boolean containsKey(String key){
		if (cachePath.get(key) != null){
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
	
	private static class LruThumbnailCachePath extends LruCache<String, Bitmap> {

		public LruThumbnailCachePath(int maxSize) {
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
