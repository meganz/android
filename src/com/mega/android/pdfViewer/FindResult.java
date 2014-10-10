package com.mega.android.pdfViewer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.graphics.Rect;
import android.util.Log;


/**
 * Find result.
 */
public class FindResult {
	
	/**
	 * Logging tag.
	 */
	public static final String TAG = "com.mega.android.pdfViewer.FindResult";
	
	/**
	 * Page number.
	 */
	public int page;

	/**
	 * List of rects that mark find result occurences.
	 * In page dimensions (not scalled).
	 */
	public List<Rect> markers;
	
	/**
	 * Add marker.
	 */
	public void addMarker(int x0, int y0, int x1, int y1) {
		if (x0 >= x1) throw new IllegalArgumentException("x0 must be smaller than x1: " + x0 + ", " + x1);
		if (y0 >= y1) throw new IllegalArgumentException("y0 must be smaller than y1: " + y0 + ", " + y1);
		if (this.markers == null)
			this.markers = new ArrayList<Rect>();
		Rect nr = new Rect(x0, y0, x1, y1);
		if (this.markers.isEmpty()) {
			this.markers.add(nr);
		} else {
			this.markers.get(0).union(nr);
		}
	}
	
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("FindResult(");
		if (this.markers == null || this.markers.isEmpty()) {
			b.append("no markers");
		} else {
			Iterator<Rect> i = this.markers.iterator();
			Rect r = null;
			while(i.hasNext()) {
				r = i.next();
				b.append(r);
				if (i.hasNext()) b.append(", ");
			}
		}
		b.append(")");
		return b.toString();
	}
	
	public void finalize() {
		Log.i(TAG, this + ".finalize()");
	}
}
