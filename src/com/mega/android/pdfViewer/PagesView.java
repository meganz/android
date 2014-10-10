package com.mega.android.pdfViewer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.mega.android.Util;
import com.mega.android.pdfViewer.BookmarkEntry;
import com.mega.android.pdfViewer.Actions;
import com.mega.android.pdfViewer.AndroidReflections;
import com.mega.android.pdfViewer.Bookmark;
import com.mega.android.pdfViewer.OpenPDFActivity;
import com.mega.android.pdfViewer.Options;

/**
 * View that simplifies displaying of paged documents.
 * TODO: redesign zooms, pages, margins, layout
 * TODO: use more floats for better align, or use more ints for performance ;) (that is, really analyse what should be used when)
 */
public class PagesView extends View implements 
		View.OnTouchListener, OnImageRenderedListener, View.OnKeyListener {
	/**
	 * Logging tag.
	 */
	private static final String TAG = "com.mega.android.pdfViewer.PagesView";
	
	/* Experiments show that larger tiles are faster, but the gains do drop off,
	 * and must be balanced against the size of memory chunks being requested.
	 */
	private static final int MIN_TILE_WIDTH = 256;
	private static final int MAX_TILE_WIDTH = 640;
	private static final int MIN_TILE_HEIGHT = 128;
	private static final int MAX_TILE_PIXELS = 640*360;
	
//	private final static int MAX_ZOOM = 4000;
//	private final static int MIN_ZOOM = 100;
	
	/**
	 * Space between screen edge and page and between pages.
	 */
	private int marginX = 0;
	private int marginY = 10;
	
	/* multitouch zoom */
	private boolean mtZoomActive = false;
	private float mtZoomValue;
	private float mtLastDistance;
	private float mtDistanceStart;
	private long mtDebounce;
	
	/* zoom steps */
	float step = 1.414f;
	
	/* volume keys page */
	boolean pageWithVolume = true;
	
	private Activity activity = null;
	
	/**
	 * Source of page bitmaps.
	 */
	private PagesProvider pagesProvider = null;
	
	
	@SuppressWarnings("unused")
	private long lastControlsUseMillis = 0;
	
	private int colorMode;
	
	private float maxRealPageSize[] = {0f, 0f};
	private float realDocumentSize[] = {0f, 0f};
	
	/**
	 * Current width of this view.
	 */
	private int width = 0;
	
	/**
	 * Current height of this view.
	 */
	private int height = 0;

	/**
	 * Position over book, not counting drag.
	 * This is position of viewports center, not top-left corner. 
	 */
	private int left = 0;
	
	/**
	 * Position over book, not counting drag.
	 * This is position of viewports center, not top-left corner.
	 */
	private int top = 0;
	
	/**
	 * Current zoom level.
	 * 1000 is 100%.
	 */
	private int zoomLevel = 1000;
	
	/**
	 * Current rotation of pages.
	 */
	private int rotation = 0;
	
	/**
	 * Base scaling factor - how much shrink (or grow) page to fit it nicely to screen at zoomLevel = 1000.
	 * For example, if we determine that 200x400 image fits screen best, but PDF's pages are 400x800, then
	 * base scaling would be 0.5, since at base scaling, without any zoom, page should fit into screen nicely.
	 */
	private float scaling0 = 0f;
	
	/**
	 * Page sized obtained from pages provider.
	 * These do not change.
	 */
	private int pageSizes[][];
	
	/**
	 * Find mode.
	 */
	private boolean findMode = false;

	/**
	 * Paint used to draw find results.
	 */
	private Paint findResultsPaint1 = null;
	
	/**
	 * Paint used to draw find results.
	 */
	private Paint findResultsPaint2 = null;
	
	/**
	 * Currently displayed find results.
	 */
	private List<FindResult> findResults = null;

	/**
	 * hold the currently displayed page 
	 */
	private int currentPage = 0;

	/**
	 * Bookmarked page to go to.
	 */
	private BookmarkEntry bookmarkToRestore = null;
	
	/**
	 * Construct this view.
	 * @param activity parent activity
	 */
	
	private boolean eink = false;
	private boolean showZoomOnScroll = false;
	private boolean volumeUpIsDown = false;
	private boolean volumeDownIsDown = false;	
	private GestureDetector gestureDetector = null;
	private Scroller scroller = null;
	
	private boolean verticalScrollLock = true;
	private boolean lockedVertically = false;
	private float downX = 0;
	private float downY = 0;
	private float lastX = 0;
	private float lastY = 0;
	private float maxExcursionY = 0;
	private int doubleTapAction = Options.DOUBLE_TAP_ZOOM_IN_OUT;
	private int zoomToRestore = 0;
	private int leftToRestore;
	private Actions actions = null;
	private boolean nook2 = false;
	private LinearLayout zoomLayout = null;


	public PagesView(Activity activity) {
		super(activity);
		this.activity = activity;
		this.actions = null;
		this.lastControlsUseMillis = System.currentTimeMillis();
		this.findResultsPaint1 = new Paint();
		this.findResultsPaint1.setARGB(0x80, 0x80, 0x80, 0x80);
		this.findResultsPaint1.setStyle(Paint.Style.STROKE);
		this.findResultsPaint1.setAntiAlias(true);
		this.findResultsPaint1.setStrokeWidth(3);
		this.findResultsPaint2 = new Paint();
		this.findResultsPaint2.setARGB(0xd0, 0xc5, 0, 0);
		this.findResultsPaint2.setStyle(Paint.Style.STROKE);
		this.findResultsPaint2.setAntiAlias(true);
		this.findResultsPaint2.setStrokeWidth(3);
		this.setOnTouchListener(this);
		this.setOnKeyListener(this);
		activity.setDefaultKeyMode(Activity.DEFAULT_KEYS_SEARCH_LOCAL);
		
		this.mtZoomActive = false;
		this.mtDebounce = 0;

		this.scroller = null; // new Scroller(activity);

		this.gestureDetector = new GestureDetector(activity,
				new GestureDetector.OnGestureListener() {
					public boolean onDown(MotionEvent e) {
						return false;
					}

					public boolean onFling(MotionEvent e1, MotionEvent e2,
							float velocityX, float velocityY) {

						if (lockedVertically)
							velocityX = 0;

						doFling(velocityX, velocityY);
						return true;
					}

					public void onLongPress(MotionEvent e) {
					}

					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						return false;
					}

					public void onShowPress(MotionEvent e) {
					}

					public boolean onSingleTapUp(MotionEvent e) {
						return false;
					}
		});
		
		final OpenPDFActivity openFileActivity = (OpenPDFActivity)activity;
		final PagesView pagesView = this;
		
		gestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {
			public boolean onDoubleTap(MotionEvent e) {
				switch(doubleTapAction) {
				case Options.DOUBLE_TAP_ZOOM_IN_OUT:
					if (zoomToRestore != 0) {
						left = leftToRestore;
						top = top * zoomToRestore / zoomLevel;
						zoomLevel = zoomToRestore;
						invalidate();
						zoomToRestore = 0;
					}
					else {
						int oldLeft = left;
						int oldZoom = zoomLevel;
						left += e.getX() - width/2;
						top += e.getY() - height/2;
						zoom(2f);
						zoomToRestore = oldZoom;
						leftToRestore = oldLeft;
					}
					return true;
				case Options.DOUBLE_TAP_ZOOM:
					left += e.getX() - width/2;
					top += e.getY() - height/2;
					zoom(2f);
					return true;
				default:
					return false;
				}
			}

			public boolean onDoubleTapEvent(MotionEvent e) {
				return false;
			}

			public boolean onSingleTapConfirmed(MotionEvent e) {
    			if (mtDebounce + 600 > SystemClock.uptimeMillis()) 
    				return false;

				
				
				if (!showZoomOnScroll) {
					openFileActivity.showZoom();
					pagesView.invalidate();
				}
				
				if (zoomLayout != null) {
					Rect r = new Rect();
					
					zoomLayout.getDrawingRect(r);
					
					r.set(r.left - 5, r.top - 5, r.right + 5, r.bottom + 5);
					
					if (r.contains((int)e.getX(), (int)e.getY()))
						return false;
				}
				
				return doAction(actions.getAction(e.getY() < height / 2 ? Actions.TOP_TAP : Actions.BOTTOM_TAP));
			}
		});
	}
	
	public void setStartBookmark(Bookmark b, String bookmarkName) {
		if (b != null) {
			this.bookmarkToRestore = b.getLast(bookmarkName);
			
			if (this.bookmarkToRestore == null)
				return;
						
			if (this.bookmarkToRestore.numberOfPages != this.pageSizes.length) {
				this.bookmarkToRestore = null;
				return;
			}

			if (0<this.bookmarkToRestore.page) {
				this.currentPage = this.bookmarkToRestore.page;
			}
		}
	}
		
	/**
	 * Handle size change event.
	 * Update base scaling, move zoom controls to correct place etc.
	 * @param w new width
	 * @param h new height
	 * @param oldw old width
	 * @param oldh old height
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		this.width = w;
		this.height = h;
		if (this.scaling0 == 0f) {
			this.scaling0 = Math.min(
					((float)this.height - 2*marginY) / (float)this.pageSizes[0][1],
					((float)this.width - 2*marginX) / (float)this.pageSizes[0][0]);
		}
		if (oldw == 0 && oldh == 0) {
			goToBookmark();
		}
	}
	
	public void goToBookmark() {

		if (this.bookmarkToRestore == null || this.bookmarkToRestore.absoluteZoomLevel == 0
				|| this.bookmarkToRestore.page < 0 
				|| this.bookmarkToRestore.page >= this.pageSizes.length ) {
			this.top  = this.height / 2;
			this.left = this.width / 2;
		}
		else {
			this.zoomLevel = (int)(this.bookmarkToRestore.absoluteZoomLevel / this.scaling0);
			this.rotation = this.bookmarkToRestore.rotation;
			Point pos = getPagePositionInDocumentWithZoom(this.bookmarkToRestore.page);
			this.currentPage = this.bookmarkToRestore.page;
			this.top = pos.y + this.height / 2;
			this.left = this.getCurrentPageWidth(this.currentPage)/2 + marginX + this.bookmarkToRestore.offsetX;
			this.bookmarkToRestore = null;
		}
	}
	
	public void setPagesProvider(PagesProvider pagesProvider) {
		this.pagesProvider = pagesProvider;
		if (this.pagesProvider != null) {
			this.pageSizes = this.pagesProvider.getPageSizes();
			
			maxRealPageSize[0] = 0f;
			maxRealPageSize[1] = 0f;
			realDocumentSize[0] = 0f;
			realDocumentSize[1] = 0f;
			
			for (int i = 0; i < this.pageSizes.length; i++) 
				for (int j = 0; j<2; j++) {
					if (pageSizes[i][j] > maxRealPageSize[j])
						maxRealPageSize[j] = pageSizes[i][j];
					realDocumentSize[j] += pageSizes[i][j]; 
				}
			
			if (this.width > 0 && this.height > 0) {
				this.scaling0 = Math.min(
						((float)this.height - 2*marginY) / (float)this.pageSizes[0][1],
						((float)this.width - 2*marginX) / (float)this.pageSizes[0][0]);
				this.left = this.width / 2;
				this.top = this.height / 2;
			}
		} else {
			this.pageSizes = null;
		}
		this.pagesProvider.setOnImageRenderedListener(this);
	}
	
	/**
	 * Draw view.
	 * @param canvas what to draw on
	 */
	
	int prevTop = -1;
	int prevLeft = -1;

	public void onDraw(Canvas canvas) {
		if (this.nook2) {
			N2EpdController.setGL16Mode();
		}
		this.drawPages(canvas);
		if (this.findMode) this.drawFindResults(canvas);
	}
	
	/**
	 * Get current maximum page width by page number taking into account zoom and rotation
	 */
	private int getCurrentMaxPageWidth() {
		float realpagewidth = this.maxRealPageSize[this.rotation % 2 == 0 ? 0 : 1];
		return (int)scale(realpagewidth);
	}
	
	/**
	 * Get current maximum page height by page number taking into account zoom and rotation
	 */
/*	private int getCurrentMaxPageHeight() {
		float realpageheight = this.maxRealPageSize[this.rotation % 2 == 0 ? 1 : 0];
		return (int)(realpageheight * scaling0 * (this.zoomLevel*0.001f));
	} */
	
	/**
	 * Get current maximum page width by page number taking into account zoom and rotation
	 */
	private int getCurrentDocumentHeight() {
		float realheight = this.realDocumentSize[this.rotation % 2 == 0 ? 1 : 0];
		/* we add pageSizes.length to account for round-off issues */
		return (int)(scale(realheight) +  
			(pageSizes.length - 1) * this.getCurrentMarginY());
	}
	
	/**
	 * Get current page width by page number taking into account zoom and rotation
	 * @param pageno 0-based page number
	 */
	private int getCurrentPageWidth(int pageno) {
		float realpagewidth = (float)this.pageSizes[pageno][this.rotation % 2 == 0 ? 0 : 1];
		float currentpagewidth = scale(realpagewidth);
		return (int)currentpagewidth;
	}
	
	private float scale(float unscaled) {
		return unscaled * scaling0 * this.zoomLevel * 0.001f;
	}
	
	/**
	 * Get current page height by page number taking into account zoom and rotation.
	 * @param pageno 0-based page number
	 */
	private float getCurrentPageHeight(int pageno) {
		float realpageheight = (float)this.pageSizes[pageno][this.rotation % 2 == 0 ? 1 : 0];
		float currentpageheight = scale(realpageheight);
		return currentpageheight;
	}
	
	private float getCurrentMarginX() {
		return scale((float)marginX);
	}
	
	private float getCurrentMarginY() {
		return scale((float)marginY);
	}
	
	/**
	 * This takes into account zoom level.
	 */
	private Point getPagePositionInDocumentWithZoom(int page) {
		float marginX = this.getCurrentMarginX();
		float marginY = this.getCurrentMarginY();
		float left = marginX;
		float top = 0;
		for(int i = 0; i < page; ++i) {
			top += this.getCurrentPageHeight(i);
		}
		top += (page+1) * marginY;
		
		return new Point((int)left, (int)top);
	}
	
	/**
	 * Calculate screens (viewports) top-left corner position over document.
	 */
	private Point getScreenPositionOverDocument() {
		float top = this.top - this.height / 2;
		float left = this.left - this.width / 2;
		return new Point((int)left, (int)top);
	}
	
	/**
	 * Calculate current page position on screen in pixels.
	 * @param page base-0 page number
	 */
	private Point getPagePositionOnScreen(int page) {
		if (page < 0) throw new IllegalArgumentException("page must be >= 0: " + page);
		if (page >= this.pageSizes.length) throw new IllegalArgumentException("page number too big: " + page);
		
		Point pagePositionInDocument = this.getPagePositionInDocumentWithZoom(page);
		Point screenPositionInDocument = this.getScreenPositionOverDocument();
		
		return new Point(
					pagePositionInDocument.x - screenPositionInDocument.x,
					pagePositionInDocument.y - screenPositionInDocument.y
				);
	}
	
	@Override
	public void computeScroll() {
		if (this.scroller == null) 
			return;
		
		if (this.scroller.computeScrollOffset()) {
			left = this.scroller.getCurrX();
			top = this.scroller.getCurrY();
			((OpenPDFActivity)activity).showPageNumber(false);
			postInvalidate();
		}
	}
	
	/**
	 * Draw pages.
	 * Also collect info what's visible and push this info to page renderer.
	 */
	private void drawPages(Canvas canvas) {
		if (this.eink) {
			canvas.drawColor(Color.WHITE);
		}

		Rect src = new Rect(); /* TODO: move out of drawPages */
		Rect dst = new Rect(); /* TODO: move out of drawPages */
		int pageWidth = 0;
		int pageHeight = 0;
		float pagex0, pagey0, pagex1, pagey1; // in doc, counts zoom
		int x, y; // on screen
		int viewx0, viewy0; // view over doc
		LinkedList<Tile> visibleTiles = new LinkedList<Tile>();
		float currentMarginX = this.getCurrentMarginX();
		float currentMarginY = this.getCurrentMarginY();
		
		if (this.pagesProvider != null) {
            if (this.zoomLevel < 5)
                this.zoomLevel = 5;

			int pageCount = this.pageSizes.length;

			viewx0 = this.left - this.width/2;
			viewy0 = this.top - this.height/2;

			int adjScreenLeft;
			int adjScreenTop;
			int adjScreenWidth;
			int adjScreenHeight;
			float renderAhead;
			
			if (mtZoomActive) {
				adjScreenWidth = (int)(this.width / mtZoomValue);
				adjScreenLeft = this.width/2 - adjScreenWidth/2;
				adjScreenHeight = (int)(this.height / mtZoomValue);
				adjScreenTop = this.height/2 - adjScreenHeight/2;
				renderAhead = 1f;
				Log.v(TAG, "adj:"+ adjScreenLeft+" "+adjScreenTop+" "+adjScreenWidth+" "+adjScreenHeight);
			}
			else {
				/* We now adjust the position to make sure we don't scroll too
				 * far away from the document text.
				 */
				int oldviewx0 = viewx0;
				int oldviewy0 = viewy0;
	
				viewx0 = adjustPosition(viewx0, width, (int)currentMarginX,
						getCurrentMaxPageWidth());
				viewy0 = adjustPosition(viewy0, height, (int)currentMarginY,
						(int)getCurrentDocumentHeight());
				
				this.left += viewx0 - oldviewx0;
				this.top += viewy0 - oldviewy0;

				adjScreenWidth = this.width;
				adjScreenLeft = 0;
				adjScreenHeight = this.height;
				adjScreenTop = 0;
				
				renderAhead = this.pagesProvider.getRenderAhead();
			}

			float currpageoff = currentMarginY;

			this.currentPage = -1;
			
			pagey0 = 0;
			int[] tileSizes = new int[2];
			
			for(int i = 0; i < pageCount; ++i) {
				// is page i visible?

				pageWidth = this.getCurrentPageWidth(i);
				pageHeight = (int) this.getCurrentPageHeight(i);

				pagex0 = currentMarginX;
				pagex1 = (int)(currentMarginX + pageWidth);
				pagey0 = currpageoff;
				pagey1 = (int)(currpageoff + pageHeight);
				
				if (rectsintersect(
							(int)pagex0, (int)pagey0, (int)pagex1, (int)pagey1, // page rect in doc
							viewx0 + adjScreenLeft, 
							viewy0 + adjScreenTop, 
							viewx0 + adjScreenLeft + adjScreenWidth, 
							viewy0 + adjScreenTop + (int)(renderAhead*adjScreenHeight) // viewport rect in doc, or close enough to it
						))
				{
					if (this.currentPage == -1)  {
						// remember the currently displayed page
						this.currentPage = i;
					}
									
					x = (int)pagex0 - viewx0 - adjScreenLeft;
					y = (int)pagey0 - viewy0 - adjScreenTop;
					
					getGoodTileSizes(tileSizes, pageWidth, pageHeight);
					
					for(int tileix = 0; tileix < (pageWidth + tileSizes[0]-1) / tileSizes[0]; ++tileix)
						for(int tileiy = 0; tileiy < (pageHeight + tileSizes[1]-1) / tileSizes[1]; ++tileiy) {
							
							dst.left = (int)(x + tileix*tileSizes[0]);
							dst.top = (int)(y + tileiy*tileSizes[1]);
							dst.right = dst.left + tileSizes[0];
							dst.bottom = dst.top + tileSizes[1];	
						
							if (dst.intersects(0, 0, adjScreenWidth, 
									(int)(renderAhead*adjScreenHeight))) {

								Tile tile = new Tile(i, (int)(this.zoomLevel * scaling0), 
										tileix*tileSizes[0], tileiy*tileSizes[1], this.rotation,
										tileSizes[0], tileSizes[1]);
								
								if (dst.intersects(0, 0, 
										adjScreenWidth,
										adjScreenHeight)) {
									Bitmap b = this.pagesProvider.getPageBitmap(tile);
									if (b != null) {
										//Log.d(TAG, "  have bitmap: " + b + ", size: " + b.getWidth() + " x " + b.getHeight());
										src.left = 0;
										src.top = 0;
										src.right = b.getWidth();
										src.bottom = b.getHeight();
										
										if (dst.right > x + pageWidth) {
											src.right = (int)(b.getWidth() * (float)((x+pageWidth)-dst.left) / (float)(dst.right - dst.left));
											dst.right = (int)(x + pageWidth);
										}
										
										if (dst.bottom > y + pageHeight) {
											src.bottom = (int)(b.getHeight() * (float)((y+pageHeight)-dst.top) / (float)(dst.bottom - dst.top));
											dst.bottom = (int)(y + pageHeight);
										}
										
										if (mtZoomActive) {
											dst.left = (int) ((dst.left-adjScreenWidth/2) * mtZoomValue + this.width/2); 
											dst.right = (int) ((dst.right-adjScreenWidth/2) * mtZoomValue + this.width/2); 
											dst.top = (int) ((dst.top-adjScreenHeight/2) * mtZoomValue + this.height/2); 
											dst.bottom = (int) ((dst.bottom-adjScreenHeight/2) * mtZoomValue + this.height/2); 
										}
										
										drawBitmap(canvas, b, src, dst);
									}
								}
								if (!mtZoomActive)
									visibleTiles.add(tile);
							}
						}
				}
				
				
				/* move to next page */
				currpageoff += currentMarginY + this.getCurrentPageHeight(i);
			}
			this.pagesProvider.setVisibleTiles(visibleTiles);
		}
	}
	
	/**
	 * Draw bitmap on canvas using color mode.
	 */
	private void drawBitmap(Canvas canvas, Bitmap b, Rect src, Rect dst) {
		if (colorMode != Options.COLOR_MODE_NORMAL) {
			Paint paint = new Paint();
			paint.setColorFilter(new 
					ColorMatrixColorFilter(new ColorMatrix(
							Options.getColorModeMatrix(this.colorMode))));
			canvas.drawBitmap(b, src, dst, paint);
		}
		else {
			canvas.drawBitmap(b, src, dst, null);
		}
	}

	/**
	 * Draw find results.
	 * TODO prettier icons
	 * TODO message if nothing was found
	 * @param canvas drawing target
	 */
	private void drawFindResults(Canvas canvas) {
		if (!this.findMode) throw new RuntimeException("drawFindResults but not in find results mode");
		if (this.findResults == null || this.findResults.isEmpty()) {
			Log.w(TAG, "nothing found");
			return;
		}
		for(FindResult findResult: this.findResults) {
			if (findResult.markers == null || findResult.markers.isEmpty())
				throw new RuntimeException("illegal FindResult: find result must have at least one marker");
			Iterator<Rect> i = findResult.markers.iterator();
			Rect r = null;
			Point pagePosition = this.getPagePositionOnScreen(findResult.page);
			float pagex = pagePosition.x;
			float pagey = pagePosition.y;
			float z = (this.scaling0 * (float)this.zoomLevel * 0.001f);
			while(i.hasNext()) {
				int marg = 5;
				int offs = 2;
				r = i.next();
				canvas.drawRect(
						r.left * z + pagex - marg + offs, r.top * z + pagey - marg + offs,
						r.right * z + pagex + marg + offs, r.bottom * z + pagey + marg + offs,
						this.findResultsPaint1);
				canvas.drawRect(
						r.left * z + pagex - marg, r.top * z + pagey - marg,
						r.right * z + pagex + marg, r.bottom * z + pagey + marg,
						this.findResultsPaint2);
//				canvas.drawLine(
//						r.left * z + pagex, r.top * z + pagey,
//						r.left * z + pagex, r.bottom * z + pagey,
//						this.findResultsPaint);
//				canvas.drawLine(
//						r.left * z + pagex, r.bottom * z + pagey,
//						r.right * z + pagex, r.bottom * z + pagey,
//						this.findResultsPaint);
//				canvas.drawLine(
//						r.right * z + pagex, r.bottom * z + pagey,
//						r.right * z + pagex, r.top * z + pagey,
//						this.findResultsPaint);
//			canvas.drawRect(
//					r.left * z + pagex,
//					r.top * z + pagey,
//					r.right * z + pagex,
//					r.bottom * z + pagey,
//					this.findResultsPaint);
//			Log.d(TAG, "marker lands on: " +
//					(r.left * z + pagex) + ", " +
//					(r.top * z + pagey) + ", " + 
//					(r.right * z + pagex) + ", " +
//					(r.bottom * z + pagey) + ", ");
			}
		}
	}

	private boolean unlocksVerticalLock(MotionEvent e) {
		float dx;
		float dy;
		
		dx = Math.abs(e.getX()-downX);
		dy = Math.abs(e.getY()-downY);
		
		if (dy > 0.25 * dx || maxExcursionY > 0.8 * dx)
			return false;
		
		return dx > width/5 || dx > height/5;
	}
	

	/**
	 * 
	 * @param event
	 * @return distance in multitouch event
	 */
	private float distance(MotionEvent event) {
//		double dx = event.getX(0)-event.getX(1);
//		double dy = event.getY(0)-event.getY(1);
		float dx = AndroidReflections.getMotionEventX(event, 0) - AndroidReflections.getMotionEventX(event, 1);
		float dy = AndroidReflections.getMotionEventY(event, 0) - AndroidReflections.getMotionEventY(event, 1);
		return (float)Math.sqrt(dx*dx+dy*dy);
	}



	/**
	 * Handle touch event coming from Android system.
	 */
	public boolean onTouch(View v, MotionEvent event) {
		this.lastControlsUseMillis = System.currentTimeMillis();
		if (!gestureDetector.onTouchEvent(event)) {
			// Log.v(TAG, ""+event.getAction());
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				downX = event.getX();
				downY = event.getY();
				lastX = downX;
				lastY = downY;
				lockedVertically = verticalScrollLock;
				maxExcursionY = 0;
				scroller = null;
			}
	        else if (event.getAction() == MotionEvent.ACTION_POINTER_2_DOWN
	        		&& AndroidReflections.getMotionEventPointerCount(event) >= 2) {
	        	float d = distance(event);
	        	if (d > 20f) {
		        	this.mtZoomActive = true;
		        	this.mtZoomValue = 1f;
		        	this.mtDistanceStart = distance(event);
		        	this.mtLastDistance = this.mtDistanceStart;
	        	}
	        }
			else if (event.getAction() == MotionEvent.ACTION_MOVE){
				if (this.mtZoomActive && AndroidReflections.getMotionEventPointerCount(event) >= 2) {
					float d = distance(event);
					if (d > 20f) {
						d = .6f * this.mtLastDistance + .4f * d;
						this.mtLastDistance = d;
						this.mtZoomValue = d / this.mtDistanceStart;
						if (this.mtZoomValue < 0.1f)
							this.mtZoomValue = 0.1f;
						else if (mtZoomValue > 10f)
							this.mtZoomValue = 10f;
						invalidate();
					}
				}
				else {
					if (lockedVertically && unlocksVerticalLock(event)) 
						lockedVertically = false;
					
					float dx = event.getX() - lastX;
					float dy = event.getY() - lastY;
					
					float excursionY = Math.abs(event.getY() - downY);
	
					if (excursionY > maxExcursionY)
						maxExcursionY = excursionY;
					
					if (lockedVertically)
						dx = 0;
					
					doScroll((int)-dx, (int)-dy);
					
					lastX = event.getX();
					lastY = event.getY();
				}
			}
			else if (event.getAction() == MotionEvent.ACTION_UP ||
					event.getAction() == MotionEvent.ACTION_POINTER_2_UP) {
				if (this.mtZoomActive) {
					this.mtDebounce = SystemClock.uptimeMillis();
					this.mtZoomActive = false;
					zoom(this.mtZoomValue);
				}
			}						
		}
		return true;
	}
	
	/**
	 * Handle keyboard events
	 */
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (this.pageWithVolume && event.getAction() == KeyEvent.ACTION_UP) {
			/* repeat is a little too fast sometimes, so trap these on up */
			switch(keyCode) {
				case KeyEvent.KEYCODE_VOLUME_UP:
					volumeUpIsDown = false;
					return true;
				case KeyEvent.KEYCODE_VOLUME_DOWN:
					volumeDownIsDown = false;
					return true;
			}
		}
		
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			int action = actions.getAction(keyCode);
			
			switch (keyCode) {
			case KeyEvent.KEYCODE_SEARCH:
				((OpenPDFActivity)activity).showFindDialog();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == Actions.ACTION_NONE)
					return false;
				if (!volumeDownIsDown) {
					/* Disable key repeat as on some devices the keys are a little too
					 * sticky for key repeat to work well.  TODO: Maybe key repeat disabling
					 * should be an option?  
					 */
					doAction(action);
				}
				volumeDownIsDown = true;
				return true;
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (action == Actions.ACTION_NONE)
					return false;
				if (!this.pageWithVolume)
					return false;
				if (!volumeUpIsDown) {
					doAction(action);
				}
				volumeUpIsDown = true;
				return true;
			case KeyEvent.KEYCODE_DPAD_UP:
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case 92:
			case 93:
			case 94:
			case 95:
				doAction(action);
				return true;
				
			case KeyEvent.KEYCODE_DEL:
			case KeyEvent.KEYCODE_K:
				doAction(Actions.ACTION_SCREEN_UP);
				return true;
			case KeyEvent.KEYCODE_SPACE:
			case KeyEvent.KEYCODE_J:
				doAction(Actions.ACTION_SCREEN_DOWN);
				return true;
			case KeyEvent.KEYCODE_H:
				this.left -= this.getWidth() / 4;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_L:
				this.left += this.getWidth() / 4;
				this.invalidate();
				return true;
			case KeyEvent.KEYCODE_O:
				zoom(1f/1.1f);
				return true;
			case KeyEvent.KEYCODE_P:
				zoom(1.1f);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Test if specified rectangles intersect with each other.
	 * Uses Androids standard Rect class.
	 */
	private boolean rectsintersect(
			int r1x0, int r1y0, int r1x1, int r1y1,
			int r2x0, int r2y0, int r2x1, int r2y1) {
		// r1.set(r1x0, r1y0, r1x1, r1y1);
		// return r1.intersects(r2x0, r2y0, r2x1, r2y1);
		// temporary "asserts"
//		if (r1x0 > r1x1) throw new RuntimeException("invalid rect");
//		if (r2x0 > r2x1) throw new RuntimeException("invalid rect");
//		if (r1y0 > r1y1) throw new RuntimeException("invalid rect");
//		if (r2y0 > r2y1) throw new RuntimeException("invalid rect");
		return !(
					r1x1 < r2x0 ||		// r1 left of r2
					r1x0 > r2x1 ||		// r1 right of r2
					r1y1 < r2y0 ||		// r1 above r2
					r1y0 > r2y1			// r1 below r2
				);
	}
	
	/**
	 * Used as a callback from pdf rendering code.
	 * TODO: only invalidate what needs to be painted, not the whole view
	 */
	public void onImagesRendered(Map<Tile,Bitmap> renderedTiles) {
		Rect rect = new Rect(); /* TODO: move out of onImagesRendered */

		int viewx0 = left - width/2;
		int viewy0 = top - height/2;
		
		int pageCount = this.pageSizes.length;
		float currentMarginX = this.getCurrentMarginX();
		float currentMarginY = this.getCurrentMarginY();
		
		viewx0 = adjustPosition(viewx0, width, (int)currentMarginX, 
				getCurrentMaxPageWidth());
		viewy0 = adjustPosition(viewy0, height, (int)currentMarginY,
				(int)getCurrentDocumentHeight());
		
		float currpageoff = currentMarginY;
		float renderAhead = this.pagesProvider.getRenderAhead();

		float pagex0;
		float pagex1;
		float pagey0 = 0;
		float pagey1;
		float x;
		float y;
		int pageWidth;
		int pageHeight;
		
		for(int i = 0; i < pageCount; ++i) {
			// is page i visible?

			pageWidth = this.getCurrentPageWidth(i);
			pageHeight = (int) this.getCurrentPageHeight(i);
			
			pagex0 = currentMarginX;
			pagex1 = (int)(currentMarginX + pageWidth);
			pagey0 = currpageoff;
			pagey1 = (int)(currpageoff + pageHeight);
			
			if (rectsintersect(
						(int)pagex0, (int)pagey0, (int)pagex1, (int)pagey1, // page rect in doc
						viewx0, viewy0, viewx0 + this.width, 
						viewy0 + this.height  
					))
			{
				x = pagex0 - viewx0;
				y = pagey0 - viewy0;
				
				for (Tile tile: renderedTiles.keySet()) {
					if (tile.getPage() == i) {
						Bitmap b = renderedTiles.get(tile); 
						
						rect.left = (int)(x + tile.getX());
						rect.top = (int)(y + tile.getY());
						rect.right = rect.left + b.getWidth();
						rect.bottom = rect.top + b.getHeight();	
					
						if (rect.intersects(0, 0, this.width, (int)(renderAhead*this.height))) {
							Log.v(TAG, "New bitmap forces redraw");
							postInvalidate();
							return;
						}
					}
				}
				
			}
			currpageoff += currentMarginY + this.getCurrentPageHeight(i);
		}
		Log.v(TAG, "New bitmap does not require redraw");
	}
	
	/**
	 * Handle rendering exception.
	 * Show error message and then quit parent activity.
	 * TODO: find a proper way to finish an activity when something bad happens in view.
	 */
	public void onRenderingException(RenderingException reason) {
		final Activity activity = this.activity;
		final String message = reason.getMessage();
		this.post(new Runnable() {
			public void run() {
    			AlertDialog errorMessageDialog = new AlertDialog.Builder(activity)
				.setTitle("Error")
				.setMessage(message)
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						activity.finish();
					}
				})
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						activity.finish();
					}
				})
				.create();
    			errorMessageDialog.show();
			}
		});
	}
	
	synchronized public void scrollToPage(int page) {
		scrollToPage(page, true);
	}
	
	public float pagePosition(int page) {
		float top = 0;
		
		for(int i = 0; i < page; ++i) {
			top += this.getCurrentPageHeight(i);
		}
		
		if (page > 0)
			top += scale((float)marginY) * (float)(page);
		
		return top;		
	}

	/**
	 * Move current viewport over n-th page.
	 * Page is 0-based.
	 * @param page 0-based page number
	 */
	synchronized public void scrollToPage(int page, boolean positionAtTop) {
		float top;
		
		if (page < 0) page = 0;
		else if (page >= this.getPageCount()) page = this.getPageCount() - 1;
		
		if (positionAtTop) {
			top = this.height/2 + pagePosition(page);
		}
		else {
			top = this.top - pagePosition(currentPage) + pagePosition(page);
		}

		this.top = (int)top;
		this.currentPage = page;
		this.invalidate();
	}
	
//	/**
//	 * Compute what's currently visible.
//	 * @return collection of tiles that define what's currently visible
//	 */
//	private Collection<Tile> computeVisibleTiles() {
//		LinkedList<Tile> tiles = new LinkedList<Tile>();
//		float viewx = this.left + (this.dragx1 - this.dragx);
//		float viewy = this.top + (this.dragy1 - this.dragy);
//		float pagex = MARGIN;
//		float pagey = MARGIN;
//		float pageWidth;
//		float pageHeight;
//		int tileix;
//		int tileiy;
//		int thisPageTileCountX;
//		int thisPageTileCountY;
//		float tilex;
//		float tiley;
//		for(int page = 0; page < this.pageSizes.length; ++page) {
//			
//			pageWidth = this.getCurrentPageWidth(page);
//			pageHeight = this.getCurrentPageHeight(page);
//			
//			thisPageTileCountX = (int)Math.ceil(pageWidth / TILE_SIZE);
//			thisPageTileCountY = (int)Math.ceil(pageHeight / TILE_SIZE);
//			
//			if (viewy + this.height < pagey) continue; /* before first visible page */
//			if (viewx > pagey + pageHeight) break; /* after last page */
//
//			for(tileix = 0; tileix < thisPageTileCountX; ++tileix) {
//				for(tileiy = 0; tileiy < thisPageTileCountY; ++tileiy) {
//					tilex = pagex + tileix * TILE_SIZE;
//					tiley = pagey + tileiy * TILE_SIZE;
//					if (rectsintersect(viewx, viewy, viewx+this.width, viewy+this.height,
//							tilex, tiley, tilex+TILE_SIZE, tiley+TILE_SIZE)) {
//						tiles.add(new Tile(page, this.zoomLevel, (int)tilex, (int)tiley, this.rotation));
//					}
//				}
//			}
//			
//			/* move to next page */
//			pagey += this.getCurrentPageHeight(page) + MARGIN;
//		}
//		return tiles;
//	}
//	synchronized Collection<Tile> getVisibleTiles() {
//		return this.visibleTiles;
//	}
	
	/**
	 * Rotate pages.
	 * Updates rotation variable, then invalidates view.
	 * @param rotation rotation
	 */
	synchronized public void rotate(int rotation) {
		this.rotation = (this.rotation + rotation) % 4;
		this.invalidate();
	}
	
	/**
	 * Set find mode.
	 * @param m true if pages view should display find results and find controls
	 */
	synchronized public void setFindMode(boolean m) {
		if (this.findMode != m) {
			this.findMode = m;
			if (!m) {
				this.findResults = null;
			}
		}
	}
	
	/**
	 * Return find mode.
	 * @return find mode - true if view is currently in find mode
	 */
	public boolean getFindMode() {
		return this.findMode;
	}

//	/**
//	 * Ask pages provider to focus on next find result.
//	 * @param forward direction of search - true for forward, false for backward
//	 */
//	public void findNext(boolean forward) {
//		this.pagesProvider.findNext(forward);
//		this.scrollToFindResult();
//		this.invalidate();
//	}
	
	/**
	 * Move viewport position to find result (if any).
	 * Does not call invalidate().
	 */
	public void scrollToFindResult(int n) {
		if (this.findResults == null || this.findResults.isEmpty()) return;
		Rect center = new Rect();
		FindResult findResult = this.findResults.get(n);

		for(Rect marker: findResult.markers) {
			center.union(marker);
		}

		float x = scale((center.left + center.right) / 2);
		float y = pagePosition(findResult.page) + scale((center.top + center.bottom) / 2);
		
		this.left = (int)(x + this.getCurrentMarginX());
		this.top = (int)(y);
	}
	
	/**
	 * Get the current page number
	 * 
	 * @return the current page. 0-based
	 */
	public int getCurrentPage() {
		return currentPage;
	}
	
	/**
	 * Get the current zoom level
	 * 
	 * @return the current zoom level
	 */
	public int getCurrentAbsoluteZoom() {
		return zoomLevel;
	}
	
	/**
	 * Get the current rotation
	 * 
	 * @return the current rotation
	 */
	public int getPageRotation() {
		return rotation;
	}
	
	/**
	 * Get page count.
	 */
	public int getPageCount() {
		return this.pageSizes.length;
	}
	
	/**
	 * Set find results.
	 */
	public void setFindResults(List<FindResult> results) {
		this.findResults = results;
	}
	
	/**
	 * Get current find results.
	 */
	public List<FindResult> getFindResults() {
		return this.findResults;
	}
	
	private void doFling(float vx, float vy) {
		float avx = vx > 0 ? vx : -vx;
		float avy = vy > 0 ? vy : -vy;
		
		if (avx < .25 * avy) {
			vx = 0;
		}
		else if (avy < .25 * avx) {
			vy = 0;
		}
		
		int marginX = (int)getCurrentMarginX();
		int marginY = (int)getCurrentMarginY();
		int minx = this.width/2 + getLowerBound(this.width, marginX, 
				getCurrentMaxPageWidth());
		int maxx = this.width/2 + getUpperBound(this.width, marginX, 
				getCurrentMaxPageWidth());
		int miny = this.height/2 + getLowerBound(this.height, marginY,
				  getCurrentDocumentHeight());
		int maxy = this.height/2 + getUpperBound(this.height, marginY,
				  getCurrentDocumentHeight());

		this.scroller = new Scroller(activity);
		this.scroller.fling(this.left, this.top, 
				(int)-vx, (int)-vy,
				minx, maxx,
				miny, maxy);
		invalidate();
	}
	
	private void doScroll(int dx, int dy) {
		this.left += dx;
		this.top += dy;
		invalidate();
	}
	
	/**
	 * Zoom down one level
	 */
	public void zoom(float value) {
		this.zoomLevel *= value;
		this.left *= value;
		this.top *= value;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		zoomToRestore = 0;
		this.invalidate();		
	}

	/* zoom to width */
	public void zoomWidth() {
		int page = currentPage < 0 ? 0 : currentPage;
		int pageWidth = getCurrentPageWidth(page);
		if (pageWidth <= 0) {
			throw new RuntimeException("invalid page " + page + " with: " + pageWidth); 
		}
		this.top = (this.top - this.height / 2) * this.width / pageWidth + this.height / 2;
		this.zoomLevel = this.zoomLevel * (this.width - 2*marginX) / pageWidth;
		this.left = (int) (this.width/2);
		zoomToRestore = 0;
		this.invalidate();		
	}

	/* zoom to fit */
	public void zoomFit() {
		int page = currentPage < 0 ? 0 : currentPage;
		int z1 = this.zoomLevel * this.width / getCurrentPageWidth(page);
		int z2 = (int)(this.zoomLevel * this.height / getCurrentPageHeight(page));
		this.zoomLevel = z2 < z1 ? z2 : z1;
		Point pos = getPagePositionInDocumentWithZoom(page);
		this.left = this.width/2 + pos.x;
		this.top = this.height/2 + pos.y;
		zoomToRestore = 0;
		this.invalidate();		
	}

	/**
	 * Set zoom
	 */
	public void setZoomLevel(int zoomLevel) {
		if (this.zoomLevel == zoomLevel)
			return;
		this.zoomLevel = zoomLevel;
		Log.d(TAG, "zoom level changed to " + this.zoomLevel);
		zoomToRestore = 0;
		this.invalidate();
	}
	
	
	/**
	 * Set rotation
	 */
	public void setRotation(int rotation) {
		if (this.rotation == rotation)
			return;
		this.rotation = rotation;
		Log.d(TAG, "rotation changed to " + this.rotation);
		this.invalidate();
	}
	
	
	public void setVerticalScrollLock(boolean verticalScrollLock) {
		this.verticalScrollLock = verticalScrollLock;
	}
	
	
	public void setColorMode(int colorMode) {
		this.colorMode = colorMode;
		this.invalidate();
	}


	public void setZoomIncrement(float step) {
		this.step = step;
	}
	
	public void setPageWithVolume(boolean pageWithVolume) {
		this.pageWithVolume = pageWithVolume;
	}
	

	private void getGoodTileSizes(int[] sizes, int pageWidth, int pageHeight) {
		sizes[0] = getGoodTileSize(pageWidth, MIN_TILE_WIDTH, MAX_TILE_WIDTH);		
		sizes[1] = getGoodTileSize(pageHeight, MIN_TILE_HEIGHT, MAX_TILE_PIXELS / sizes[0]); 
	}
	
	private int getGoodTileSize(int pageSize, int minSize, int maxSize) {
		if (pageSize <= 2)
			return 2;
		if (pageSize <= maxSize)
			return pageSize;
		int numInPageSize = (pageSize + maxSize - 1) / maxSize;
		int proposedSize = (pageSize + numInPageSize - 1) / numInPageSize;
		if (proposedSize < minSize)
			return minSize;
		else
			return proposedSize;
	}
	
	/* Get the upper and lower bounds for the viewpoint.  The document itself is
	 * drawn from margin to margin+docDim.   
	 */
	private int getLowerBound(int screenDim, int margin, int docDim) {
		if (docDim <= screenDim) {
			/* all pages can and do fit */
			return margin + docDim - screenDim;
		}
		else {
			/* document is too wide/tall to fit */
			return 0; 
		}
	}
	
	private int getUpperBound(int screenDim, int margin, int docDim) {
		if (docDim <= screenDim) {
			/* all pages can and do fit */
			return margin;
		}
		else {
			/* document is too wide/tall to fit */
			return 2 * margin + docDim - screenDim;
		}
	}
	
	private int adjustPosition(int pos, int screenDim, int margin, int docDim) {
		int min = getLowerBound(screenDim, margin, docDim);
		int max = getUpperBound(screenDim, margin, docDim);
		
		if (pos < min)
			return min;
		else if (max < pos)
			return max;
		else
			return pos;
	}
	
	public BookmarkEntry toBookmarkEntry() {
		return new BookmarkEntry(this.pageSizes.length, 
				this.currentPage, scaling0*zoomLevel, rotation, 
				this.left - this.getCurrentPageWidth(this.currentPage)/2 - marginX);
	}
	
	public void setSideMargins(int margin) {
		this.marginX = margin;
	}
	
	public void setTopMargin(int margin) {
		int delta = margin - this.marginY;
		top += this.currentPage * delta; 
		this.marginY = margin;
	}
	
	public void setDoubleTap(int doubleTapAction) {
		this.doubleTapAction = doubleTapAction;
	}
	
	public boolean doAction(int action) {
		float zoomValue = Actions.getZoomValue(action);
		if (0f < zoomValue) {
			zoom(zoomValue);
			return true;
		}
		switch(action) {
		case Actions.ACTION_FULL_PAGE_DOWN:
			scrollToPage(currentPage + 1, false);
			return true;
		case Actions.ACTION_FULL_PAGE_UP:
			scrollToPage(currentPage - 1, false);
			return true;
		case Actions.ACTION_PREV_PAGE:
			scrollToPage(currentPage - 1);
			return true;
		case Actions.ACTION_NEXT_PAGE:
			scrollToPage(currentPage + 1);
			return true;
		case Actions.ACTION_SCREEN_DOWN:
			log("Screen Down");
			this.top += this.getHeight() - 16;
			this.invalidate();
			return true;
		case Actions.ACTION_SCREEN_UP:
			this.top -= this.getHeight() - 16;
			this.invalidate();
			return true;
		default:
			return false;
		}
	}
	
	public void setActions(Actions actions) {
		this.actions = actions;
	}
	
	public void setEink(boolean eink) {
		this.eink = eink;
	}

	public void setNook2(boolean nook2) {
		this.nook2 = nook2;
	}
	
	public void setShowZoomOnScroll(boolean showZoomOnScroll) {
		this.showZoomOnScroll = showZoomOnScroll;
	}

	public void setZoomLayout(LinearLayout zoomLayout) {
		this.zoomLayout = zoomLayout;
	}
	
	public static void log(String log) {
		Util.log("PagesView", log);
	}
}
