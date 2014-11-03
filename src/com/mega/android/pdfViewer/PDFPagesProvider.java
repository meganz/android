package com.mega.android.pdfViewer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Provide rendered bitmaps of pages.
 */
public class PDFPagesProvider extends PagesProvider {

	/**
	 * Const used by logging.
	 */
	private final static String TAG = "com.mega.android.pdfViewer.PDFPagesProvider";

	/* render a little more than twice the screen height, so the next page will be ready */
	private float renderAhead = 2.1f;
	private boolean doRenderAhead = true;
	private int extraCache = 0;
	private boolean omitImages;
	Activity activity = null;
	private static final int MB = 1024*1024;
	
	public void setExtraCache(int extraCache) {
		this.extraCache = extraCache; 
		
		setMaxCacheSize();
	}
	
	/* also calculates renderAhead */
	private void setMaxCacheSize() {
		long availLong = (long)(Runtime.getRuntime().maxMemory() - 4 * MB);
		
		int avail;
		if (availLong > 256*MB)
			avail = 256*MB;
		else
			avail = (int)availLong;
		
		int maxMax = 7*MB + this.extraCache; /* at most allocate this much unless absolutely necessary */
		if (maxMax < avail)
			maxMax = avail;		
		int minMax = 4*MB; /* at least allocate this much */
		if (maxMax < minMax)
			maxMax = minMax;

		int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
		int screenWidth = activity.getWindowManager().getDefaultDisplay().getWidth(); 
		int displaySize = screenWidth * screenHeight;
		
		if (displaySize <= 320*240)
			displaySize = 320*240;
		
		int m = (int)(displaySize * 1.25f * 1.0001f);
		
		if (doRenderAhead) {
			if ((int)(m * 2.1f) <= maxMax) {
				renderAhead = 2.1f;
				m = (int)(m * renderAhead);
			}
			else {
				renderAhead = 1.0001f;
			}
		}
		else {
			/* The extra little bit is to compensate for round-off */
			renderAhead = 1.0001f;
		}
		
		if (m < minMax)
			m = minMax;

		if (m + 20*MB <= maxMax)
			m = maxMax - 20 * MB;
		
		if (m < maxMax) {
			m += this.extraCache;
			if (maxMax < m)
				m = maxMax;
		}
		
		Log.v(TAG, "Setting cache size="+m+ " renderAhead="+renderAhead+" for "+screenWidth+"x"+screenHeight+" (avail="+avail+")");
		
		this.bitmapCache.setMaxCacheSizeBytes((int)m);
	}
	

	public void setOmitImages(boolean skipImages) {
		if (this.omitImages == skipImages)
			return;
		this.omitImages = skipImages;
		
		if (this.bitmapCache != null) {
			this.bitmapCache.clearCache();
		}
	}
	

	/**
	 * Smart page-bitmap cache.
	 * Stores up to approx maxCacheSizeBytes of images.
	 * Dynamically drops oldest unused bitmaps.
	 * TODO: Return high resolution bitmaps if no exact res is available.
	 * Bitmap images are tiled - tile size is specified in PagesView.TILE_SIZE.
	 */
	
	private static class BitmapCache {
		/**
		 * Stores cached bitmaps.
		 */
		private Map<Tile, BitmapCacheValue> bitmaps;
		
		private int maxCacheSizeBytes = 4*1024*1024; 
		
		/**
		 * Stats logging - number of cache hits.
		 */
		private long hits;
				
		
		/**
		 * Stats logging - number of misses.
		 */
		private long misses;
		
		BitmapCache() {
			this.bitmaps = new HashMap<Tile, BitmapCacheValue>();
			this.hits = 0;
			this.misses = 0;
		}
		
		public void setMaxCacheSizeBytes(int maxCacheSizeBytes) {
			this.maxCacheSizeBytes = maxCacheSizeBytes;
		}
		
		/**
		 * Get cached bitmap. Updates last access timestamp.
		 * @param k cache key
		 * @return bitmap found in cache or null if there's no matching bitmap
		 */
		Bitmap get(Tile k) {
			BitmapCacheValue v = this.bitmaps.get(k);
			Bitmap b = null;
			if (v != null) {
				// yeah
				b = v.bitmap;
				assert b != null;
				v.millisAccessed = System.currentTimeMillis();
				this.hits += 1;
			} else {
				// le fu
				this.misses += 1;
			}
			if ((this.hits + this.misses) % 100 == 0 && (this.hits > 0 || this.misses > 0)) {
				Log.d("com.mega.android.pdfViewer.pagecache", "hits: " + hits + ", misses: " + misses + ", hit ratio: " + (float)(hits) / (float)(hits+misses) +
						", size: " + this.bitmaps.size());
			}
			return b;
		}
		
		/**
		 * Put rendered tile in cache.
		 * @param tile tile definition (page, position etc), cache key
		 * @param bitmap rendered tile contents, cache value
		 */
		synchronized void put(Tile tile, Bitmap bitmap) {
			while (this.willExceedCacheSize(bitmap) && !this.bitmaps.isEmpty()) {
				Log.v(TAG, "Removing oldest");
				this.removeOldest();
			}
			this.bitmaps.put(tile, new BitmapCacheValue(bitmap, System.currentTimeMillis(), 0));
		}
		
		/**
		 * Check if cache contains specified bitmap tile. Doesn't update last-used timestamp.
		 * @return true if cache contains specified bitmap tile
		 */
		synchronized boolean contains(Tile tile) {
			return this.bitmaps.containsKey(tile);
		}
		
		/**
		 * Estimate bitmap memory size.
		 * This is just a guess.
		 */
		private static int getBitmapSizeInCache(Bitmap bitmap) {
			int numPixels = bitmap.getWidth() * bitmap.getHeight(); 
			if (bitmap.getConfig() == Bitmap.Config.RGB_565) {
				return numPixels * 2;
			}
			else if (bitmap.getConfig() == Bitmap.Config.ALPHA_8)
				return numPixels;
			else
				return numPixels * 4;
		}
		
		/**
		 * Get estimated sum of byte sizes of bitmaps stored in cache currently.
		 */
		private synchronized int getCurrentCacheSize() {
			int size = 0;
			Iterator<BitmapCacheValue> it = this.bitmaps.values().iterator();
			while(it.hasNext()) {
				BitmapCacheValue bcv = it.next();
				Bitmap bitmap = bcv.bitmap;
				size += getBitmapSizeInCache(bitmap);
			}
			Log.v(TAG, "Cache size: "+size);
			return size;
		}
		
		/**
		 * Determine if adding this bitmap would grow cache size beyond max size.
		 */
		private synchronized boolean willExceedCacheSize(Bitmap bitmap) {
			return (this.getCurrentCacheSize() + 
					    BitmapCache.getBitmapSizeInCache(bitmap) > maxCacheSizeBytes);
		}
		
		/**
		 * Remove oldest bitmap cache value.
		 */
		private void removeOldest() {
			Iterator<Tile> i = this.bitmaps.keySet().iterator();
			long minmillis = 0;
			Tile oldest = null;
			while(i.hasNext()) {
				Tile k = i.next();
				BitmapCacheValue v = this.bitmaps.get(k);
				if (oldest == null) {
					oldest = k;
					minmillis = v.millisAccessed;
				} else {
					if (minmillis > v.millisAccessed) {
						minmillis = v.millisAccessed;
						oldest = k;
					}
				}
			}
			if (oldest == null) throw new RuntimeException("couldnt find oldest");
			BitmapCacheValue v = this.bitmaps.get(oldest);
			v.bitmap.recycle();
			this.bitmaps.remove(oldest);
		}
		
		synchronized public void clearCache() {
			Iterator<Tile> i = this.bitmaps.keySet().iterator();

			while(i.hasNext()) {
				Tile k = i.next();
				Log.v("Deleting", k.toString());
				this.bitmaps.get(k).bitmap.recycle();
				i.remove();
			}			
		}
	}
	
	private static class RendererWorker implements Runnable {
		/**
		 * Worker stops rendering if error was encountered.
		 */
		private boolean isFailed = false;
		private PDFPagesProvider pdfPagesProvider;
		private BitmapCache bitmapCache;
		private Collection<Tile> tiles;
		
		/**
		 * Internal worker number for debugging.
		 */
		private static int workerThreadId = 0;
		
		/**
		 * Used as a synchronized flag.
		 * If null, then there's no designated thread to render tiles.
		 * There might be other worker threads running, but those have finished
		 * their work and will finish really soon.
		 * Only this one should pick up new jobs.
		 */
		private Thread workerThread = null;
		
		/**
		 * Create renderer worker.
		 * @param pdfPagesProvider parent pages provider
		 */
		RendererWorker(PDFPagesProvider pdfPagesProvider) {
			this.tiles = null;
			this.pdfPagesProvider = pdfPagesProvider;
		}
		
		/**
		 * Called by outside world to provide more work for worker.
		 * This also starts rendering thread if one is needed.
		 * @param tiles a collection of tile objects that carry information about what should be rendered next
		 */
		synchronized void setTiles(Collection<Tile> tiles, BitmapCache bitmapCache) {
			this.tiles = tiles;
			this.bitmapCache = bitmapCache;
			
			if (this.workerThread == null) {
				Thread t = new Thread(this);
				t.setPriority(Thread.MIN_PRIORITY);
				t.setName("RendererWorkerThread#" + RendererWorker.workerThreadId++);
				this.workerThread = t;
				t.start();
				Log.d(TAG, "started new worker thread");
			} else {
				//Log.i(TAG, "setTiles notices tiles is not empty, that means RendererWorkerThread exists and there's no need to start new one");
			}
		}
		
		/**
		 * Get tiles that should be rendered next. May not block.
		 * Also sets this.workerThread to null if there's no tiles to be rendered currently,
		 * so that calling thread may finish.
		 * If there are more tiles to be rendered, then this.workerThread is not reset.
		 * @return some tiles
		 */
		synchronized Collection<Tile> popTiles() {
			if (this.tiles == null || this.tiles.isEmpty()) {
				this.workerThread = null; /* returning null, so calling thread will finish it's work */
				return null;
			}
			Tile tile = this.tiles.iterator().next();
			this.tiles.remove(tile);
			return Collections.singleton(tile);
		}
		
		/**
		 * Thread's main routine.
		 * There might be more than one running, but only one will get new tiles. Others
		 * will get null returned by this.popTiles and will finish their work.
		 */
		public void run() {
			while(true) {
				if (this.isFailed) {
					Log.i(TAG, "RendererWorker is failed, exiting");
					break;
				}
				Collection<Tile> tiles = this.popTiles(); /* this can't block */
				if (tiles == null || tiles.size() == 0) break;
				try {
					Map<Tile,Bitmap> renderedTiles = this.pdfPagesProvider.renderTiles(tiles, bitmapCache);
					if (renderedTiles.size() > 0)
						this.pdfPagesProvider.publishBitmaps(renderedTiles);
				} catch (RenderingException e) {
					this.isFailed = true;
					this.pdfPagesProvider.publishRenderingException(e);
				}
			}
		}
	}

	private PDF pdf = null;
	private BitmapCache bitmapCache = null;
	private RendererWorker rendererWorker = null;
	private OnImageRenderedListener onImageRendererListener = null;
	
	public float getRenderAhead() {
		return this.renderAhead;
	}
	
	public PDFPagesProvider(Activity activity, PDF pdf, boolean skipImages,
			boolean doRenderAhead) {
		this.pdf = pdf;
		this.omitImages = skipImages;
		this.bitmapCache = new BitmapCache();
		this.rendererWorker = new RendererWorker(this);
		this.activity = activity;
		this.doRenderAhead = doRenderAhead;
		setMaxCacheSize();
	}
	
	public void setRenderAhead(boolean doRenderAhead) {
		this.doRenderAhead = doRenderAhead;
		setMaxCacheSize();
	}
	
	/**
	 * Render tiles.
	 * Called by worker, calls PDF's methods that in turn call native code.
	 * @param tiles job description - what to render
	 * @return mapping of jobs and job results, with job results being Bitmap objects
	 */
	private Map<Tile,Bitmap> renderTiles(Collection<Tile> tiles, BitmapCache ignore) throws RenderingException {
		Map<Tile,Bitmap> renderedTiles = new HashMap<Tile,Bitmap>();
		Iterator<Tile> i = tiles.iterator();
		Tile tile = null;

		while(i.hasNext()) {
			tile = i.next();
			Bitmap bitmap = this.renderBitmap(tile);
			if (bitmap != null)
				renderedTiles.put(tile, bitmap);
		}
		
		return renderedTiles;
	}
	
	/**
	 * Really render bitmap. Takes time, should be done in background thread.
	 * Calls native code (through PDF object).
	 */
	private Bitmap renderBitmap(Tile tile) throws RenderingException {
		synchronized(tile) {
			/* last minute check to make sure some other thread hasn't rendered this tile */
			if (this.bitmapCache.contains(tile))
				return null;
			
			PDF.Size size = new PDF.Size(tile.getPrefXSize(), tile.getPrefYSize());
			int[] pagebytes = null;

			pagebytes = pdf.renderPage(tile.getPage(), tile.getZoom(), tile.getX(), tile.getY(), 
					tile.getRotation(), omitImages, size); /* native */

			if (pagebytes == null) throw new RenderingException("Couldn't render page " + tile.getPage());
			
			/* create a bitmap from the 32-bit color array */			
			Bitmap b = Bitmap.createBitmap(pagebytes, size.width, size.height, 
					Bitmap.Config.RGB_565);
			this.bitmapCache.put(tile, b);
			return b;
		}
	}
	
	/**
	 * Called by worker.
	 */
	private void publishBitmaps(Map<Tile,Bitmap> renderedTiles) {
		if (this.onImageRendererListener != null) {
			this.onImageRendererListener.onImagesRendered(renderedTiles);
		} else {
			Log.w(TAG, "we've got new bitmaps, but there's no one to notify about it!");
		}
	}
	
	/**
	 * Called by worker.
	 */
	private void publishRenderingException(RenderingException e) {
		if (this.onImageRendererListener != null) {
			this.onImageRendererListener.onRenderingException(e);
		}
	}
	
	@Override
	public void setOnImageRenderedListener(OnImageRenderedListener l) {
		this.onImageRendererListener = l;
	}
	
	/**
	 * Get tile bitmap if it's already rendered.
	 * @param tile which bitmap
	 * @return rendered tile; tile represents rect of TILE_SIZE x TILE_SIZE pixels,
	 * but might be of different size (should be scaled when painting) 
	 */
	@Override
	public Bitmap getPageBitmap(Tile tile) {
		Bitmap b = null;
		b = this.bitmapCache.get(tile);
		if (b != null) return b;
		return null;
	}

	/**
	 * Get page count.
	 * @return number of pages
	 */
	@Override
	public int getPageCount() {
		int c = this.pdf.getPageCount();
		if (c <= 0) throw new RuntimeException("failed to load pdf file: getPageCount returned " + c);
		return c;
	}
	
	/**
	 * Get page sizes from pdf file.
	 * @return array of page sizes
	 */
	@Override
	public int[][] getPageSizes() {
		int cnt = this.getPageCount();
		int[][] sizes = new int[cnt][];
		PDF.Size size = new PDF.Size();
		int err;
		for(int i = 0; i < cnt; ++i) {
			err = this.pdf.getPageSize(i, size);
			if (err != 0) {
				throw new RuntimeException("failed to getPageSize(" + i + ",...), error: " + err);
			}
			sizes[i] = new int[2];
			sizes[i][0] = size.width;
			sizes[i][1] = size.height;
		}
		return sizes;
	}
	
	/**
	 * View informs provider what's currently visible.
	 * Compute what should be rendered and pass that info to renderer worker thread, possibly waking up worker.
	 * @param tiles specs of whats currently visible
	 */
	synchronized public void setVisibleTiles(Collection<Tile> tiles) {
		List<Tile> newtiles = null;
		for(Tile tile: tiles) {
			if (!this.bitmapCache.contains(tile)) {
				if (newtiles == null) newtiles = new LinkedList<Tile>();
				newtiles.add(tile);
			}
		}
		if (newtiles != null) {
			this.rendererWorker.setTiles(newtiles, this.bitmapCache);
		}
	}	
}
