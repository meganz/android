package mega.privacy.android.app;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import androidx.collection.LruCache;

/*
 * LRU thumbnails cache to display in list
 */
public class PreviewCache {
	
	List<Long>nulls;
	LruPreviewCache cache;
	
	public PreviewCache() {
		cache = new LruPreviewCache(5);
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
	
	private static class LruPreviewCache extends LruCache<Long, Bitmap> {

		public LruPreviewCache(int maxSize) {
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
