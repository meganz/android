package com.shockwave.pdfium;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.ParcelFileDescriptor;

import com.shockwave.pdfium.util.Size;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class PdfiumCore {
    private static final String TAG = PdfiumCore.class.getName();

    static {
        try {
            System.loadLibrary("modpng");
            System.loadLibrary("modft2");
            System.loadLibrary("modpdfium");
            System.loadLibrary("jniPdfium");
        } catch (UnsatisfiedLinkError e) {
            Timber.e(e, TAG, "Native libraries failed to load - %s");
        }
    }

    private native long nativeOpenDocument(int fd, String password);

    private native long nativeOpenMemDocument(byte[] data, String password);

    private native void nativeCloseDocument(long docPtr);

    private native int nativeGetPageCount(long docPtr);

    private native long nativeLoadPage(long docPtr, int pageIndex);

    private native void nativeClosePage(long pagePtr);

    private native int nativeGetPageWidthPoint(long pagePtr);

    private native int nativeGetPageHeightPoint(long pagePtr);

    private native void nativeRenderPageBitmap(long pagePtr, Bitmap bitmap, int dpi,
                                               int startX, int startY,
                                               int drawSizeHor, int drawSizeVer,
                                               boolean renderAnnot);

    private native String nativeGetDocumentMetaText(long docPtr, String tag);

    private native Long nativeGetFirstChildBookmark(long docPtr, Long bookmarkPtr);

    private native Long nativeGetSiblingBookmark(long docPtr, long bookmarkPtr);

    private native String nativeGetBookmarkTitle(long bookmarkPtr);

    private native long nativeGetBookmarkDestIndex(long docPtr, long bookmarkPtr);

    private native Size nativeGetPageSizeByIndex(long docPtr, int pageIndex, int dpi);

    private native long[] nativeGetPageLinks(long pagePtr);

    private native Integer nativeGetDestPageIndex(long docPtr, long linkPtr);

    private native String nativeGetLinkURI(long docPtr, long linkPtr);

    private native RectF nativeGetLinkRect(long linkPtr);

    private native Point nativePageCoordsToDevice(long pagePtr, int startX, int startY, int sizeX,
                                                  int sizeY, int rotate, double pageX, double pageY);

    /* synchronize native methods */
    private static final Object lock = new Object();
    private static Field mFdField = null;
    private final int mCurrentDpi;

    @SuppressLint("DiscouragedPrivateApi")
    public static int getNumFd(ParcelFileDescriptor fdObj){
        try {
            if (mFdField == null) {
                // Update implementation, since this causes a "Reflective access to descriptor" Warning
                String descriptorField = "descriptor";
                mFdField = FileDescriptor.class.getDeclaredField(descriptorField);
                mFdField.setAccessible(true);
            }
            return mFdField.getInt(fdObj.getFileDescriptor());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Timber.e(e);
            return -1;
        }
    }


    /**
     * Context needed to get screen density
     */
    public PdfiumCore(Context ctx) {
        mCurrentDpi = ctx.getResources().getDisplayMetrics().densityDpi;
    }

    /**
     * Create new document from file
     */
    public PdfDocument newDocument(ParcelFileDescriptor fd) {
        return newDocument(fd, null);
    }

    /**
     * Create new document from file with password
     */
    public PdfDocument newDocument(ParcelFileDescriptor fd, String password) {
        PdfDocument document = new PdfDocument();
        document.parcelFileDescriptor = fd;
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenDocument(getNumFd(fd), password);
        }

        return document;
    }

    /**
     * Create new document from bytearray with password
     */
    public PdfDocument newDocument(byte[] data, String password) {
        PdfDocument document = new PdfDocument();
        synchronized (lock) {
            document.mNativeDocPtr = nativeOpenMemDocument(data, password);
        }
        return document;
    }

    /**
     * Get total number of pages in document
     */
    public int getPageCount(PdfDocument doc) {
        synchronized (lock) {
            return nativeGetPageCount(doc.mNativeDocPtr);
        }
    }

    /**
     * Open page and store native pointer in {@link PdfDocument}
     */
    public long openPage(PdfDocument doc, int pageIndex) {
        long pagePtr;
        synchronized (lock) {
            pagePtr = nativeLoadPage(doc.mNativeDocPtr, pageIndex);
            doc.mNativePagesPtr.put(pageIndex, pagePtr);
            return pagePtr;
        }

    }

    /**
     * Get page width in PostScript points (1/72th of an inch).<br>
     * This method requires page to be opened.
     */
    public int getPageWidthPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageWidthPoint(pagePtr);
            }
            return 0;
        }
    }

    /**
     * Get page height in PostScript points (1/72th of an inch).<br>
     * This method requires page to be opened.
     */
    public int getPageHeightPoint(PdfDocument doc, int index) {
        synchronized (lock) {
            Long pagePtr;
            if ((pagePtr = doc.mNativePagesPtr.get(index)) != null) {
                return nativeGetPageHeightPoint(pagePtr);
            }
            return 0;
        }
    }

    /**
     * Get size of page in pixels.<br>
     * This method does not require given page to be opened.
     */
    public Size getPageSize(PdfDocument doc, int index) {
        synchronized (lock) {
            return nativeGetPageSizeByIndex(doc.mNativeDocPtr, index, mCurrentDpi);
        }
    }

    /**
     * Render page fragment on {@link Bitmap}.<br>
     * Page must be opened before rendering.
     * <p>
     * Supported bitmap configurations:
     * <ul>
     * <li>ARGB_8888 - best quality, high memory usage, higher possibility of OutOfMemoryError
     * <li>RGB_565 - little worse quality, twice less memory usage
     * </ul>
     */
    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex,
                                 int startX, int startY, int drawSizeX, int drawSizeY) {
        renderPageBitmap(doc, bitmap, pageIndex, startX, startY, drawSizeX, drawSizeY, false);
    }

    /**
     * Render page fragment on {@link Bitmap}. This method allows to render annotations.<br>
     * Page must be opened before rendering.
     * <p>
     * For more info see {@link PdfiumCore#renderPageBitmap(PdfDocument, Bitmap, int, int, int, int, int)}
     */
    public void renderPageBitmap(PdfDocument doc, Bitmap bitmap, int pageIndex,
                                 int startX, int startY, int drawSizeX, int drawSizeY,
                                 boolean renderAnnot) {
        synchronized (lock) {
            try {
                Long pagePtr = doc.mNativePagesPtr.get(pageIndex);
                if (pagePtr != null) {
                    nativeRenderPageBitmap(pagePtr, bitmap, mCurrentDpi, startX, startY, drawSizeX, drawSizeY, renderAnnot);
                }
            } catch (NullPointerException e) {
                Timber.e(e, TAG, "mContext may be null");
            } catch (Exception e) {
                Timber.e(e, TAG, "Exception thrown from native");
            }
        }
    }

    /**
     * Release native resources and opened file
     */
    public void closeDocument(PdfDocument doc) {
        synchronized (lock) {
            for (Integer index : doc.mNativePagesPtr.keySet()) {
                try {
                    Long pagePtr = doc.mNativePagesPtr.get(index);
                    if (pagePtr != null) {
                        nativeClosePage(pagePtr);
                    }
                } catch (NullPointerException e) {
                    Timber.e(e, TAG, "NullPointerException occurred in closeDocument()");
                }
            }
            doc.mNativePagesPtr.clear();

            nativeCloseDocument(doc.mNativeDocPtr);

            if (doc.parcelFileDescriptor != null) { //if document was loaded from file
                try {
                    doc.parcelFileDescriptor.close();
                } catch (IOException e) {
                    /* ignore */
                }
                doc.parcelFileDescriptor = null;
            }
        }
    }

    /**
     * Get metadata for given document
     */
    public PdfDocument.Meta getDocumentMeta(PdfDocument doc) {
        synchronized (lock) {
            PdfDocument.Meta meta = new PdfDocument.Meta();
            meta.title = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Title");
            meta.author = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Author");
            meta.subject = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Subject");
            meta.keywords = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Keywords");
            meta.creator = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Creator");
            meta.producer = nativeGetDocumentMetaText(doc.mNativeDocPtr, "Producer");
            meta.creationDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "CreationDate");
            meta.modDate = nativeGetDocumentMetaText(doc.mNativeDocPtr, "ModDate");

            return meta;
        }
    }

    /**
     * Get table of contents (bookmarks) for given document
     */
    public List<PdfDocument.Bookmark> getTableOfContents(PdfDocument doc) {
        synchronized (lock) {
            List<PdfDocument.Bookmark> topLevel = new ArrayList<>();
            Long first = nativeGetFirstChildBookmark(doc.mNativeDocPtr, null);
            if (first != null) {
                recursiveGetBookmark(topLevel, doc, first);
            }
            return topLevel;
        }
    }

    private void recursiveGetBookmark(List<PdfDocument.Bookmark> tree, PdfDocument doc, long bookmarkPtr) {
        PdfDocument.Bookmark bookmark = new PdfDocument.Bookmark();
        bookmark.mNativePtr = bookmarkPtr;
        bookmark.title = nativeGetBookmarkTitle(bookmarkPtr);
        bookmark.pageIdx = nativeGetBookmarkDestIndex(doc.mNativeDocPtr, bookmarkPtr);
        tree.add(bookmark);

        Long child = nativeGetFirstChildBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (child != null) {
            recursiveGetBookmark(bookmark.getChildren(), doc, child);
        }

        Long sibling = nativeGetSiblingBookmark(doc.mNativeDocPtr, bookmarkPtr);
        if (sibling != null) {
            recursiveGetBookmark(tree, doc, sibling);
        }
    }

    /**
     * Get all links from given page
     */
    public List<PdfDocument.Link> getPageLinks(PdfDocument doc, int pageIndex) {
        synchronized (lock) {
            List<PdfDocument.Link> links = new ArrayList<>();
            Long pagePtr = doc.mNativePagesPtr.get(pageIndex);
            if (pagePtr == null) return links;
            long[] linkPtrs = nativeGetPageLinks(pagePtr);
            for (long linkPtr : linkPtrs) {
                Integer index = nativeGetDestPageIndex(doc.mNativeDocPtr, linkPtr);
                String uri = nativeGetLinkURI(doc.mNativeDocPtr, linkPtr);

                RectF rect = nativeGetLinkRect(linkPtr);
                if (rect != null && (index != null || uri != null)) {
                    links.add(new PdfDocument.Link(rect, index, uri));
                }

            }
            return links;
        }
    }

    /**
     * Map page coordinates to device screen coordinates
     *
     * @param doc       pdf document
     * @param pageIndex index of page
     * @param startX    left pixel position of the display area in device coordinates
     * @param startY    top pixel position of the display area in device coordinates
     * @param sizeX     horizontal size (in pixels) for displaying the page
     * @param sizeY     vertical size (in pixels) for displaying the page
     * @param rotate    page orientation: 0 (normal), 1 (rotated 90 degrees clockwise),
     *                  2 (rotated 180 degrees), 3 (rotated 90 degrees counter-clockwise)
     * @param pageX     X value in page coordinates
     * @param pageY     Y value in page coordinate
     * @return mapped coordinates
     */
    public Point mapPageCoordsToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX,
                                       int sizeY, int rotate, double pageX, double pageY) {
        long pagePtr = 0L;
        try {
            Long nativePagePtr = doc.mNativePagesPtr.get(pageIndex);
            if (nativePagePtr != null) {
                pagePtr = nativePagePtr;
            }
        } catch (NullPointerException e) {
            Timber.e(e, TAG, "NullPointerException occurred in mapPageCoordsToDevice()");
        }
        return nativePageCoordsToDevice(pagePtr, startX, startY, sizeX, sizeY, rotate, pageX, pageY);
    }

    /**
     * @return mapped coordinates
     * @see PdfiumCore#mapPageCoordsToDevice(PdfDocument, int, int, int, int, int, int, double, double)
     */
    public RectF mapRectToDevice(PdfDocument doc, int pageIndex, int startX, int startY, int sizeX,
                                 int sizeY, int rotate, RectF coords) {

        Point leftTop = mapPageCoordsToDevice(doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
                coords.left, coords.top);
        Point rightBottom = mapPageCoordsToDevice(doc, pageIndex, startX, startY, sizeX, sizeY, rotate,
                coords.right, coords.bottom);
        return new RectF(leftTop.x, leftTop.y, rightBottom.x, rightBottom.y);
    }
}
