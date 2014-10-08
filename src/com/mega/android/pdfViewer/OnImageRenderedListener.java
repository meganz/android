package com.mega.android.pdfViewer;

import java.util.Map;

import android.graphics.Bitmap;

/**
 * Allow renderer to notify view that new bitmaps are ready.
 * Implemented by PagesView.
 */
public interface OnImageRenderedListener {
	void onImagesRendered(Map<Tile,Bitmap> renderedImages);
	void onRenderingException(RenderingException reason);
}
