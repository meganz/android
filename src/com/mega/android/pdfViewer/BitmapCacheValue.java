package com.mega.android.pdfViewer;

import android.graphics.Bitmap;

public class BitmapCacheValue {
	public Bitmap bitmap;
	/* public long millisAdded; */
	public long millisAccessed;
	public long priority;
	
	public BitmapCacheValue(Bitmap bitmap, long millisAdded, long priority) {
		this.bitmap = bitmap;
		/* this.millisAdded = millisAdded; */
		this.millisAccessed = millisAdded;
		this.priority = priority;
	}
}
