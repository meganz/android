package com.mega.android.pdfViewer;

import java.util.Collection;

import android.graphics.Bitmap;


/**
 * Provide content of pages rendered by PagesView.
 */
public abstract class PagesProvider {

	/**
	 * Get page image tile for drawing.
	 */
	public abstract Bitmap getPageBitmap(Tile tile);
	
	/**
	 * Get page count.
	 * This cannot change between executions - PagesView assumes (for now) that docuement doesn't change.
	 */
	public abstract int getPageCount();
	
	/**
	 * Get page sizes.
	 * This cannot change between executions - PagesView assumes (for now) that docuement doesn't change.
	 */
	public abstract int[][] getPageSizes();
	
	/**
	 * Set notify target.
	 * Usually page rendering takes some time. If image cannot be provided
	 * immediately, provider may return null.
	 * Then it's up to provider to notify view that requested image has arrived
	 * and is ready to be drawn.
	 * This function, if overridden, can be used to inform provider who
	 * should be notifed.
	 * Default implementation does nothing. 
	 */
	public void setOnImageRenderedListener(OnImageRenderedListener listener) {
		/* to be overridden when needed */
	}
	
	public abstract void setVisibleTiles(Collection<Tile> tiles);

	public abstract float getRenderAhead();
}
