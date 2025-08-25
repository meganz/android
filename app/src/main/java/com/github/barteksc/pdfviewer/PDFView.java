/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer;

import static mega.privacy.android.app.utils.AlertDialogUtil.isAlertDialogShown;
import static mega.privacy.android.app.utils.Util.isOnline;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.github.barteksc.pdfviewer.exception.PageRenderingException;
import com.github.barteksc.pdfviewer.link.DefaultLinkHandler;
import com.github.barteksc.pdfviewer.link.LinkHandler;
import com.github.barteksc.pdfviewer.listener.Callbacks;
import com.github.barteksc.pdfviewer.listener.OnDrawListener;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.listener.OnPageErrorListener;
import com.github.barteksc.pdfviewer.listener.OnPageScrollListener;
import com.github.barteksc.pdfviewer.listener.OnRenderListener;
import com.github.barteksc.pdfviewer.listener.OnTapListener;
import com.github.barteksc.pdfviewer.model.PagePart;
import com.github.barteksc.pdfviewer.scroll.ScrollHandle;
import com.github.barteksc.pdfviewer.source.AssetSource;
import com.github.barteksc.pdfviewer.source.ByteArraySource;
import com.github.barteksc.pdfviewer.source.DocumentSource;
import com.github.barteksc.pdfviewer.source.FileSource;
import com.github.barteksc.pdfviewer.source.InputStreamSource;
import com.github.barteksc.pdfviewer.source.UriSource;
import com.github.barteksc.pdfviewer.util.Constants;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.github.barteksc.pdfviewer.util.MathUtils;
import com.github.barteksc.pdfviewer.util.Util;
import com.google.android.material.textfield.TextInputLayout;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;
import com.shockwave.pdfium.util.SizeF;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import mega.privacy.android.app.R;
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity;
import timber.log.Timber;

/**
 * It supports animations, zoom, cache, and swipe.
 * <p>
 * To fully understand this class you must know its principles :
 * - The PDF document is seen as if we always want to draw all the pages.
 * - The thing is that we only draw the visible parts.
 * - All parts are the same size, this is because we can't interrupt a native page rendering,
 * so we need these renderings to be as fast as possible, and be able to interrupt them
 * as soon as we can.
 * - The parts are loaded when the current offset or the current zoom level changes
 * <p>
 * Important :
 * - DocumentPage = A page of the PDF document.
 * - UserPage = A page as defined by the user.
 * By default, they're the same. But the user can change the pages order
 * using {@link #load(DocumentSource, String, int[])}. In this
 * particular case, a userPage of 5 can refer to a documentPage of 17.
 */
public class PDFView extends RelativeLayout {

    private static final String TAG = PDFView.class.getSimpleName();

    public static final float DEFAULT_MAX_SCALE = 3.0f;
    public static final float DEFAULT_MID_SCALE = 1.75f;
    public static final float DEFAULT_MIN_SCALE = 1.0f;

    private float minZoom = DEFAULT_MIN_SCALE;
    private float midZoom = DEFAULT_MID_SCALE;
    private float maxZoom = DEFAULT_MAX_SCALE;

    private PdfViewerActivity pdfViewer;

    /**
     * START - scrolling in first page direction
     * END - scrolling in last page direction
     * NONE - not scrolling
     */
    enum ScrollDir {
        NONE, START, END
    }

    private ScrollDir scrollDir = ScrollDir.NONE;

    /** Rendered parts go to the cache manager */
    CacheManager cacheManager;

    /** Animation manager manage all offset and zoom animation */
    private AnimationManager animationManager;

    /** Drag manager manage all touch events */
    private DragPinchManager dragPinchManager;

    PdfFile pdfFile;

    /** The index of the current sequence */
    private int currentPage;

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture
     */
    private float currentXOffset = 0;

    /**
     * If you picture all the pages side by side in their optimal width,
     * and taking into account the zoom level, the current offset is the
     * position of the left border of the screen in this big picture
     */
    private float currentYOffset = 0;

    /** The zoom level, always >= 1 */
    private float zoom = 1f;

    /** True if the PDFView has been recycled */
    private boolean recycled = true;

    /** Current state of the view */
    private State state = State.DEFAULT;

    /** ExecutorService used during the loading phase to decode a PDF document */
    private ExecutorService decodingExecutorService;

    /** The thread {@link #renderingHandler} will run on */
    private final HandlerThread renderingHandlerThread;
    /** Handler always waiting in the background and rendering tasks */
    RenderingHandler renderingHandler;

    private PagesLoader pagesLoader;

    Callbacks callbacks = new Callbacks();

    /** Paint object for drawing */
    private Paint paint;

    /** Paint object for drawing debug stuff */
    private Paint debugPaint;

    /** Policy for fitting pages to screen */
    private FitPolicy pageFitPolicy = FitPolicy.WIDTH;

    private int defaultPage = 0;

    /** True if should scroll through pages vertically instead of horizontally */
    private boolean swipeVertical = true;

    private boolean enableSwipe = true;

    private boolean doubletapEnabled = true;

    /** Pdfium core for loading and rendering PDFs */
    private PdfiumCore pdfiumCore;

    private ScrollHandle scrollHandle;

    private boolean isScrollHandleInit = false;

    ScrollHandle getScrollHandle() {
        return scrollHandle;
    }

    /**
     * True if bitmap should use ARGB_8888 format and take more memory
     * False if bitmap should be compressed by using RGB_565 format and take less memory
     */
    private boolean bestQuality = false;

    /**
     * True if annotations should be rendered
     * False otherwise
     */
    private boolean annotationRendering = false;

    /**
     * True if the view should render during scaling and False if otherwise
     */
    private boolean renderDuringScale = false;

    /** Antialiasing and bitmap filtering */
    private boolean enableAntialiasing = true;
    private PaintFlagsDrawFilter antialiasFilter =
            new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    /** Spacing between pages, in px */
    private int spacingPx = 0;

    /** pages numbers used when calling onDrawAllListener */
    private List<Integer> onDrawPagesNums = new ArrayList<>(10);
    private Handler handler = new Handler(Looper.getMainLooper());

    /** Construct the initial view */
    public PDFView(Context context, AttributeSet set) {
        super(context, set);

        pdfViewer = (PdfViewerActivity) getContext();

        renderingHandlerThread = new HandlerThread("PDF renderer");

        if (isInEditMode()) {
            return;
        }

        cacheManager = new CacheManager();
        animationManager = new AnimationManager(this);
        dragPinchManager = new DragPinchManager(this, animationManager);
        pagesLoader = new PagesLoader(this);

        paint = new Paint();
        debugPaint = new Paint();
        debugPaint.setStyle(Style.STROKE);

        pdfiumCore = new PdfiumCore(context);
        setWillNotDraw(false);
    }

    private void load(DocumentSource docSource, String password) {
        load(docSource, password, null);
    }

    private void load(DocumentSource docSource, String password, int[] userPages) {

        if (!recycled) {
            Timber.e("Don't call load on a PDF View without recycling it first.");
            return;
        }

        recycled = false;
        // Start decoding document
        decodeDocument(docSource, password, userPages, this, pdfiumCore);
    }

    private void decodeDocument(DocumentSource docSource, String password, int[] userPages, PDFView pdfView, PdfiumCore pdfiumCore) {
        decodingExecutorService = Executors.newSingleThreadExecutor();

        try {
            decodingExecutorService.execute(() -> {
                Throwable error = null;
                try {
                    PdfDocument pdfDocument = docSource.createDocument(pdfView.getContext(), pdfiumCore, password);
                    pdfFile = new PdfFile(pdfiumCore, pdfDocument, pdfView.getPageFitPolicy(), getViewSize(pdfView),
                            userPages, pdfView.isSwipeVertical(), pdfView.getSpacingPx());
                } catch (Throwable t) {
                    error = t;
                }

                Throwable finalError = error;
                handler.post(() -> {
                    if (recycled) return;
                    if (finalError != null) {
                        pdfView.loadError(finalError);
                    } else if (!decodingExecutorService.isShutdown()) {
                        pdfView.loadComplete(pdfFile);
                        decodingExecutorService.shutdownNow();
                    }
                });
            });
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private Size getViewSize(PDFView pdfView) {
        return new Size(pdfView.getWidth(), pdfView.getHeight());
    }

    /**
     * Go to the given page.
     *
     * @param page Page index.
     */
    public void jumpTo(int page, boolean withAnimation) {
        if (pdfFile == null) {
            return;
        }

        page = pdfFile.determineValidPageNumberFrom(page);
        float offset = -pdfFile.getPageOffset(page, zoom);
        if (swipeVertical) {
            if (withAnimation) {
                animationManager.startYAnimation(currentYOffset, offset);
            } else {
                moveTo(currentXOffset, offset);
            }
        } else {
            if (withAnimation) {
                animationManager.startXAnimation(currentXOffset, offset);
            } else {
                moveTo(offset, currentYOffset);
            }
        }
        showPage(page);
    }

    public void jumpTo(int page) {
        jumpTo(page, false);
    }

    void showPage(int pageNb) {
        if (recycled) {
            return;
        }

        // Check the page number and makes the
        // difference between UserPages and DocumentPages
        pageNb = pdfFile.determineValidPageNumberFrom(pageNb);
        currentPage = pageNb;

        loadPages();

        if (scrollHandle != null && !documentFitsView()) {
            scrollHandle.setPageNum(currentPage + 1);
        }

        callbacks.callOnPageChange(currentPage, pdfFile.getPagesCount());
    }

    /**
     * Get current position as ratio of document length to visible area.
     * 0 means that document start is visible, 1 that document end is visible
     *
     * @return offset between 0 and 1
     */
    public float getPositionOffset() {
        float offset;
        if (swipeVertical) {
            offset = -currentYOffset / (pdfFile.getDocLen(zoom) - getHeight());
        } else {
            offset = -currentXOffset / (pdfFile.getDocLen(zoom) - getWidth());
        }
        return MathUtils.limit(offset, 0, 1);
    }

    /**
     * @param progress   must be between 0 and 1
     * @param moveHandle whether to move scroll handle
     * @see PDFView#getPositionOffset()
     */
    public void setPositionOffset(float progress, boolean moveHandle) {
        if (swipeVertical) {
            moveTo(currentXOffset, (-pdfFile.getDocLen(zoom) + getHeight()) * progress, moveHandle);
        } else {
            moveTo((-pdfFile.getDocLen(zoom) + getWidth()) * progress, currentYOffset, moveHandle);
        }
        loadPageByOffset();
    }

    public void setPositionOffset(float progress) {
        setPositionOffset(progress, true);
    }

    public void stopFling() {
        animationManager.stopFling();
    }

    public int getPageCount() {
        if (pdfFile == null) {
            return 0;
        }
        return pdfFile.getPagesCount();
    }

    public void setSwipeEnabled(boolean enableSwipe) {
        this.enableSwipe = enableSwipe;
    }

    void enableDoubletap(boolean enableDoubletap) {
        this.doubletapEnabled = enableDoubletap;
    }

    boolean isDoubletapEnabled() {
        return doubletapEnabled;
    }

    void onPageError(PageRenderingException ex) {
        if (!callbacks.callOnPageError(ex.getPage(), ex.getCause())) {
            Log.e(TAG, "Cannot open page " + ex.getPage(), ex.getCause());
        }
    }

    public void recycle() {

        animationManager.stopAll();
        dragPinchManager.disable();
        handler.removeCallbacksAndMessages(null);

        // Stop tasks
        if (renderingHandler != null) {
            renderingHandler.stop();
            renderingHandler.removeMessages(RenderingHandler.MSG_RENDER_TASK);
        }
        if (decodingExecutorService != null) {
            decodingExecutorService.shutdownNow();
        }

        // Clear caches
        cacheManager.recycle();

        if (scrollHandle != null && isScrollHandleInit) {
            scrollHandle.destroyLayout();
        }

        if (pdfFile != null) {
            pdfFile.dispose();
            pdfFile = null;
        }

        renderingHandler = null;
        scrollHandle = null;
        isScrollHandleInit = false;
        currentXOffset = currentYOffset = 0;
        zoom = 1f;
        recycled = true;
        callbacks = new Callbacks();
        state = State.DEFAULT;
    }

    public boolean isRecycled() {
        return recycled;
    }

    /** Handle fling animation */
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (isInEditMode()) {
            return;
        }
        animationManager.computeFling();
    }

    @Override
    protected void onDetachedFromWindow() {
        recycle();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (isInEditMode() || state != State.SHOWN) {
            return;
        }
        animationManager.stopAll();
        pdfFile.recalculatePageSizes(new Size(w, h));
        if (swipeVertical) {
            moveTo(currentXOffset, -pdfFile.getPageOffset(currentPage, zoom));
        } else {
            moveTo(-pdfFile.getPageOffset(currentPage, zoom), currentYOffset);
        }
        loadPageByOffset();
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        if (swipeVertical) {
            if (direction < 0 && currentXOffset < 0) {
                return true;
            } else if (direction > 0 && currentXOffset + toCurrentScale(pdfFile.getMaxPageWidth()) > getWidth()) {
                return true;
            }
        } else {
            if (direction < 0 && currentXOffset < 0) {
                return true;
            } else if (direction > 0 && currentXOffset + pdfFile.getDocLen(zoom) > getWidth()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        if (swipeVertical) {
            if (direction < 0 && currentYOffset < 0) {
                return true;
            } else if (direction > 0 && currentYOffset + pdfFile.getDocLen(zoom) > getHeight()) {
                return true;
            }
        } else {
            if (direction < 0 && currentYOffset < 0) {
                return true;
            } else if (direction > 0 && currentYOffset + toCurrentScale(pdfFile.getMaxPageHeight()) > getHeight()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }
        // As I said in this class javadoc, we can think of this canvas as a huge
        // strip on which we draw all the images. We actually only draw the rendered
        // parts, of course, but we render them in the place they belong in this huge
        // strip.

        // That's where Canvas.translate(x, y) becomes very helpful.
        // This is the situation :
        //  _______________________________________________
        // |   			 |					 			   |
        // | the actual  |					The big strip  |
        // |	canvas	 | 								   |
        // |_____________|								   |
        // |_______________________________________________|
        //
        // If the rendered part is on the bottom right corner of the strip
        // we can draw it but we won't see it because the canvas is not big enough.

        // But if we call translate(-X, -Y) on the canvas just before drawing the object :
        //  _______________________________________________
        // |   			  					  _____________|
        // |   The big strip     			 |			   |
        // |		    					 |	the actual |
        // |								 |	canvas	   |
        // |_________________________________|_____________|
        //
        // The object will be on the canvas.
        // This technique is massively used in this method, and allows
        // abstraction of the screen position when rendering the parts.

        // Draws background

        if (enableAntialiasing) {
            canvas.setDrawFilter(antialiasFilter);
        }

        Drawable bg = getBackground();
        if (bg == null) {
            canvas.drawColor(Color.WHITE);
        } else {
            bg.draw(canvas);
        }

        if (recycled) {
            return;
        }

        if (state != State.SHOWN) {
            return;
        }

        // Moves the canvas before drawing any element
        float currentXOffset = this.currentXOffset;
        float currentYOffset = this.currentYOffset;
        canvas.translate(currentXOffset, currentYOffset);

        // Draws thumbnails
        for (PagePart part : cacheManager.getThumbnails()) {
            drawPart(canvas, part);

        }

        // Draws parts
        for (PagePart part : cacheManager.getPageParts()) {
            drawPart(canvas, part);
            if (callbacks.getOnDrawAll() != null
                    && !onDrawPagesNums.contains(part.getPage())) {
                onDrawPagesNums.add(part.getPage());
            }
        }

        for (Integer page : onDrawPagesNums) {
            drawWithListener(canvas, page, callbacks.getOnDrawAll());
        }
        onDrawPagesNums.clear();

        drawWithListener(canvas, currentPage, callbacks.getOnDraw());

        // Restores the canvas position
        canvas.translate(-currentXOffset, -currentYOffset);
    }

    private void drawWithListener(Canvas canvas, int page, OnDrawListener listener) {
        if (listener != null) {
            float translateX, translateY;
            if (swipeVertical) {
                translateX = 0;
                translateY = pdfFile.getPageOffset(page, zoom);
            } else {
                translateY = 0;
                translateX = pdfFile.getPageOffset(page, zoom);
            }

            canvas.translate(translateX, translateY);
            SizeF size = pdfFile.getPageSize(page);
            listener.onLayerDrawn(canvas,
                    toCurrentScale(size.getWidth()),
                    toCurrentScale(size.getHeight()),
                    page);

            canvas.translate(-translateX, -translateY);
        }
    }

    /** Draw a given PagePart on the canvas */
    private void drawPart(Canvas canvas, PagePart part) {
        // Can seem strange, but avoid lot of calls
        RectF pageRelativeBounds = part.getPageRelativeBounds();
        Bitmap renderedBitmap = part.getRenderedBitmap();

        if (renderedBitmap.isRecycled()) {
            return;
        }

        // Move to the target page
        float localTranslationX = 0;
        float localTranslationY = 0;
        SizeF size = pdfFile.getPageSize(part.getPage());

        if (swipeVertical) {
            localTranslationY = pdfFile.getPageOffset(part.getPage(), zoom);
            float maxWidth = pdfFile.getMaxPageWidth();
            localTranslationX = toCurrentScale(maxWidth - size.getWidth()) / 2;
        } else {
            localTranslationX = pdfFile.getPageOffset(part.getPage(), zoom);
            float maxHeight = pdfFile.getMaxPageHeight();
            localTranslationY = toCurrentScale(maxHeight - size.getHeight()) / 2;
        }
        canvas.translate(localTranslationX, localTranslationY);

        Rect srcRect = new Rect(0, 0, renderedBitmap.getWidth(),
                renderedBitmap.getHeight());

        float offsetX = toCurrentScale(pageRelativeBounds.left * size.getWidth());
        float offsetY = toCurrentScale(pageRelativeBounds.top * size.getHeight());
        float width = toCurrentScale(pageRelativeBounds.width() * size.getWidth());
        float height = toCurrentScale(pageRelativeBounds.height() * size.getHeight());

        // If we use float values for this rectangle, there will be
        // a possible gap between page parts, especially when
        // the zoom level is high.
        RectF dstRect = new RectF((int) offsetX, (int) offsetY,
                (int) (offsetX + width),
                (int) (offsetY + height));

        // Check if bitmap is in the screen
        float translationX = currentXOffset + localTranslationX;
        float translationY = currentYOffset + localTranslationY;
        if (translationX + dstRect.left >= getWidth() || translationX + dstRect.right <= 0 ||
                translationY + dstRect.top >= getHeight() || translationY + dstRect.bottom <= 0) {
            canvas.translate(-localTranslationX, -localTranslationY);
            return;
        }

        canvas.drawBitmap(renderedBitmap, srcRect, dstRect, paint);

        if (Constants.DEBUG_MODE) {
            debugPaint.setColor(part.getPage() % 2 == 0 ? Color.RED : Color.BLUE);
            canvas.drawRect(dstRect, debugPaint);
        }

        // Restore the canvas position
        canvas.translate(-localTranslationX, -localTranslationY);

    }

    /**
     * Load all the parts around the center of the screen,
     * taking into account X and Y offsets, zoom level, and
     * the current page displayed
     */
    public void loadPages() {
        if (pdfFile == null || renderingHandler == null) {
            return;
        }

        // Cancel all current tasks
        renderingHandler.removeMessages(RenderingHandler.MSG_RENDER_TASK);
        cacheManager.makeANewSet();

        pagesLoader.loadPages();
        redraw();
    }

    /** Called when the PDF is loaded */
    void loadComplete(PdfFile pdfFile) {
        state = State.LOADED;

        this.pdfFile = pdfFile;

        if (!renderingHandlerThread.isAlive()) {
            renderingHandlerThread.start();
        }
        renderingHandler = new RenderingHandler(renderingHandlerThread.getLooper(), this);
        renderingHandler.start();

        if (scrollHandle != null) {
            scrollHandle.setupLayout(this);
            isScrollHandleInit = true;
        }

        dragPinchManager.enable();

        callbacks.callOnLoadComplete(pdfFile.getPagesCount());

        jumpTo(defaultPage, false);

        PdfViewerActivity.loading = false;
        pdfViewer.getProgressBar().setVisibility(GONE);
    }

    void showErrorDialog(final Throwable t) {
        if (pdfViewer == null || t == null) {
            Timber.e("Cannot show error dialog, pdfViewer or t is null");
            return;
        }

        if (isAlertDialogShown(pdfViewer.getTakenDownDialog())) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setCancelable(false);

        if ("Password required or incorrect password.".equals(t.getLocalizedMessage())
                || ("Password required or incorrect password.").equals(t.getMessage())) {
            if (pdfViewer.getMaxIntents() > 0) {
                View layout = pdfViewer.getLayoutInflater().inflate(R.layout.dialog_pdf_password, null);
                final TextInputLayout passwordLayout = layout.findViewById(R.id.password_layout);
                final AppCompatEditText passwordText = layout.findViewById(R.id.password_text);
                final ImageView passwordError = layout.findViewById(R.id.password_text_error_icon);

                if (pdfViewer.getPassword() != null) {
                    passwordError.setVisibility(VISIBLE);
                    String text = pdfViewer.getPassword();
                    passwordText.setText(text);
                    passwordText.setSelection(text.length());
                    passwordLayout.setError(pdfViewer.getString(R.string.error_enter_password));
                    passwordLayout.setHintTextAppearance(R.style.TextAppearance_InputHint_Error);
                    passwordError.setVisibility(View.VISIBLE);
                    passwordText.getBackground().mutate().setColorFilter(ContextCompat.getColor(getContext(), R.color.red_600_red_300), PorterDuff.Mode.SRC_ATOP);
                } else {
                    passwordError.setVisibility(GONE);
                }

                passwordLayout.setEndIconVisible(false);
                passwordText.setOnFocusChangeListener((v, hasFocus) -> passwordLayout.setEndIconVisible(hasFocus));

                passwordText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        pdfViewer.reloadPDFwithPassword(textView.getText().toString());
                        return true;
                    }
                    return false;
                });

                passwordText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void afterTextChanged(Editable editable) {
                        if (passwordLayout.getError() != null && !passwordLayout.getError().toString().isEmpty()) {
                            passwordLayout.setError(null);
                            passwordLayout.setHintTextAppearance(com.google.android.material.R.style.TextAppearance_Design_Hint);
                            passwordError.setVisibility(View.GONE);
                        }
                    }
                });

                builder.setView(layout);
                builder.setTitle(getContext().getString(R.string.title_pdf_password))
                        .setMessage(getContext().getString(R.string.text_pdf_password, pdfViewer.getPdfFileName()))
                        .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, (dialogInterface, i) -> pdfViewer.finish())
                        .setPositiveButton(R.string.contact_accept, (dialogInterface, i) -> pdfViewer.reloadPDFwithPassword(passwordText.getText().toString()));
            } else {
                builder.setTitle(getResources().getString(R.string.general_error_word))
                        .setMessage(getResources().getString(R.string.error_max_pdf_password))
                        .setPositiveButton(R.string.contact_accept, (dialogInterface, i) -> pdfViewer.finish());
            }
        } else {
            builder.setMessage(isOnline(pdfViewer) ? R.string.corrupt_pdf_dialog_text
                            : R.string.error_fail_to_open_file_no_network)
                    .setPositiveButton(R.string.general_ok, (dialog, which) -> pdfViewer.finish());
        }
        try {
            builder.show();
        } catch (Exception e) {
            Timber.e(e, "PDFView.showErrorDialog: Cannot show dialog");
        }

    }

    void loadError(Throwable t) {
        state = State.ERROR;
        // store reference, because callbacks will be cleared in recycle() method
        OnErrorListener onErrorListener = callbacks.getOnError();
        recycle();
        invalidate();
        if (onErrorListener != null) {
            onErrorListener.onError(t);
        } else {
            showErrorDialog(t);
            Timber.e(t, "Load pdf error");
        }
    }

    void redraw() {
        invalidate();
    }

    /**
     * Called when a rendering task is over and
     * a PagePart has been freshly created.
     *
     * @param part The created PagePart.
     */
    public void onBitmapRendered(PagePart part) {
        // when it is first rendered part
        if (state == State.LOADED) {
            state = State.SHOWN;
            callbacks.callOnRender(pdfFile.getPagesCount());
        }

        if (part.isThumbnail()) {
            cacheManager.cacheThumbnail(part);
        } else {
            cacheManager.cachePart(part);
        }
        redraw();
    }

    public void moveTo(float offsetX, float offsetY) {
        moveTo(offsetX, offsetY, true);
    }

    /**
     * Move to the given X and Y offsets, but check them ahead of time
     * to be sure not to go outside the the big strip.
     *
     * @param offsetX    The big strip X offset to use as the left border of the screen.
     * @param offsetY    The big strip Y offset to use as the right border of the screen.
     * @param moveHandle whether to move scroll handle or not
     */
    public void moveTo(float offsetX, float offsetY, boolean moveHandle) {
        if (swipeVertical) {
            // Check X offset
            float scaledPageWidth = toCurrentScale(pdfFile.getMaxPageWidth());
            if (scaledPageWidth < getWidth()) {
                offsetX = getWidth() / 2 - scaledPageWidth / 2;
            } else {
                if (offsetX > 0) {
                    offsetX = 0;
                } else if (offsetX + scaledPageWidth < getWidth()) {
                    offsetX = getWidth() - scaledPageWidth;
                }
            }

            // Check Y offset
            float contentHeight = pdfFile.getDocLen(zoom);
            if (contentHeight < getHeight()) { // whole document height visible on screen
                offsetY = (getHeight() - contentHeight) / 2;
            } else {
                if (offsetY > 0) { // top visible
                    offsetY = 0;
                } else if (offsetY + contentHeight < getHeight()) { // bottom visible
                    offsetY = -contentHeight + getHeight();
                }
            }

            if (offsetY < currentYOffset) {
                scrollDir = ScrollDir.END;
            } else if (offsetY > currentYOffset) {
                scrollDir = ScrollDir.START;
            } else {
                scrollDir = ScrollDir.NONE;
            }
        } else {
            // Check Y offset
            float scaledPageHeight = toCurrentScale(pdfFile.getMaxPageHeight());
            if (scaledPageHeight < getHeight()) {
                offsetY = getHeight() / 2 - scaledPageHeight / 2;
            } else {
                if (offsetY > 0) {
                    offsetY = 0;
                } else if (offsetY + scaledPageHeight < getHeight()) {
                    offsetY = getHeight() - scaledPageHeight;
                }
            }

            // Check X offset
            float contentWidth = pdfFile.getDocLen(zoom);
            if (contentWidth < getWidth()) { // whole document width visible on screen
                offsetX = (getWidth() - contentWidth) / 2;
            } else {
                if (offsetX > 0) { // left visible
                    offsetX = 0;
                } else if (offsetX + contentWidth < getWidth()) { // right visible
                    offsetX = -contentWidth + getWidth();
                }
            }

            if (offsetX < currentXOffset) {
                scrollDir = ScrollDir.END;
            } else if (offsetX > currentXOffset) {
                scrollDir = ScrollDir.START;
            } else {
                scrollDir = ScrollDir.NONE;
            }
        }

        currentXOffset = offsetX;
        currentYOffset = offsetY;
        float positionOffset = getPositionOffset();

        if (moveHandle && scrollHandle != null && !documentFitsView()) {
            scrollHandle.setScroll(positionOffset);
        }

        callbacks.callOnPageScroll(getCurrentPage(), positionOffset);

        redraw();
    }

    void loadPageByOffset() {
        // Ensure that pdfFile is not null before continuing any operation
        if (pdfFile == null || pdfFile.getPagesCount() == 0) {
            return;
        }

        float offset, screenCenter;
        if (swipeVertical) {
            offset = currentYOffset;
            screenCenter = ((float) getHeight()) / 2;
        } else {
            offset = currentXOffset;
            screenCenter = ((float) getWidth()) / 2;
        }

        int page = pdfFile.getPageAtOffset(-(offset - screenCenter), zoom);

        if (page >= 0 && page <= pdfFile.getPagesCount() - 1 && page != getCurrentPage()) {
            showPage(page);
        } else {
            loadPages();
        }
    }

    /**
     * Move relatively to the current position.
     *
     * @param dx The X difference you want to apply.
     * @param dy The Y difference you want to apply.
     * @see #moveTo(float, float)
     */
    public void moveRelativeTo(float dx, float dy) {
        moveTo(currentXOffset + dx, currentYOffset + dy);
    }

    /**
     * Change the zoom level
     */
    public void zoomTo(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Change the zoom level, relatively to a pivot point.
     * It will call moveTo() to make sure the given point stays
     * in the middle of the screen.
     *
     * @param zoom  The zoom level.
     * @param pivot The point on the screen that should stays.
     */
    public void zoomCenteredTo(float zoom, PointF pivot) {
        float dzoom = zoom / this.zoom;
        zoomTo(zoom);
        float baseX = currentXOffset * dzoom;
        float baseY = currentYOffset * dzoom;
        baseX += (pivot.x - pivot.x * dzoom);
        baseY += (pivot.y - pivot.y * dzoom);
        moveTo(baseX, baseY);
    }

    /**
     * @see #zoomCenteredTo(float, PointF)
     */
    public void zoomCenteredRelativeTo(float dzoom, PointF pivot) {
        zoomCenteredTo(zoom * dzoom, pivot);
    }

    /**
     * Checks if whole document can be displayed on screen, doesn't include zoom
     *
     * @return true if whole document can displayed at once, false otherwise
     */
    public boolean documentFitsView() {
        float len = pdfFile.getDocLen(1);
        if (swipeVertical) {
            return len < getHeight();
        } else {
            return len < getWidth();
        }
    }

    public void fitToWidth(int page) {
        if (state != State.SHOWN) {
            Log.e(TAG, "Cannot fit, document not rendered yet");
            return;
        }
        zoomTo(getWidth() / pdfFile.getPageSize(page).getWidth());
        jumpTo(page);
    }

    public SizeF getPageSize(int pageIndex) {
        if (pdfFile == null) {
            return new SizeF(0, 0);
        }
        return pdfFile.getPageSize(pageIndex);
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public float getCurrentXOffset() {
        return currentXOffset;
    }

    public float getCurrentYOffset() {
        return currentYOffset;
    }

    public float toRealScale(float size) {
        return size / zoom;
    }

    public float toCurrentScale(float size) {
        return size * zoom;
    }

    public float getZoom() {
        return zoom;
    }

    public boolean isZooming() {
        return zoom != minZoom;
    }

    private void setDefaultPage(int defaultPage) {
        this.defaultPage = defaultPage;
    }

    public void resetZoom() {
        zoomTo(minZoom);
    }

    public void resetZoomWithAnimation() {
        zoomWithAnimation(minZoom);
    }

    public void zoomWithAnimation(float centerX, float centerY, float scale) {
        animationManager.startZoomAnimation(centerX, centerY, zoom, scale);
    }

    public void zoomWithAnimation(float scale) {
        animationManager.startZoomAnimation(getWidth() / 2, getHeight() / 2, zoom, scale);
    }

    private void setScrollHandle(ScrollHandle scrollHandle) {
        this.scrollHandle = scrollHandle;
    }

    public float getMinZoom() {
        return minZoom;
    }

    public void setMinZoom(float minZoom) {
        this.minZoom = minZoom;
    }

    public float getMidZoom() {
        return midZoom;
    }

    public void setMidZoom(float midZoom) {
        this.midZoom = midZoom;
    }

    public float getMaxZoom() {
        return maxZoom;
    }

    public void setMaxZoom(float maxZoom) {
        this.maxZoom = maxZoom;
    }

    public void useBestQuality(boolean bestQuality) {
        this.bestQuality = bestQuality;
    }

    public boolean isBestQuality() {
        return bestQuality;
    }

    public boolean isSwipeVertical() {
        return swipeVertical;
    }

    public boolean isSwipeEnabled() {
        return enableSwipe;
    }

    private void setSwipeVertical(boolean swipeVertical) {
        this.swipeVertical = swipeVertical;
    }

    public void enableAnnotationRendering(boolean annotationRendering) {
        this.annotationRendering = annotationRendering;
    }

    public boolean isAnnotationRendering() {
        return annotationRendering;
    }

    public void enableRenderDuringScale(boolean renderDuringScale) {
        this.renderDuringScale = renderDuringScale;
    }

    public boolean isAntialiasing() {
        return enableAntialiasing;
    }

    public void enableAntialiasing(boolean enableAntialiasing) {
        this.enableAntialiasing = enableAntialiasing;
    }

    int getSpacingPx() {
        return spacingPx;
    }

    private void setSpacing(int spacing) {
        this.spacingPx = Util.getDP(getContext(), spacing);
    }

    private void setPageFitPolicy(FitPolicy pageFitPolicy) {
        this.pageFitPolicy = pageFitPolicy;
    }

    public FitPolicy getPageFitPolicy() {
        return pageFitPolicy;
    }

    public boolean doRenderDuringScale() {
        return renderDuringScale;
    }

    /** Returns null if document is not loaded */
    public PdfDocument.Meta getDocumentMeta() {
        if (pdfFile == null) {
            return null;
        }
        return pdfFile.getMetaData();
    }

    /** Will be empty until document is loaded */
    public List<PdfDocument.Bookmark> getTableOfContents() {
        if (pdfFile == null) {
            return Collections.emptyList();
        }
        return pdfFile.getBookmarks();
    }

    /** Will be empty until document is loaded */
    public List<PdfDocument.Link> getLinks(int page) {
        if (pdfFile == null) {
            return Collections.emptyList();
        }
        return pdfFile.getPageLinks(page);
    }

    /** Use an asset file as the pdf source */
    public Configurator fromAsset(String assetName) {
        return new Configurator(new AssetSource(assetName));
    }

    /** Use a file as the pdf source */
    public Configurator fromFile(File file) {
        return new Configurator(new FileSource(file));
    }

    /** Use URI as the pdf source, for use with content providers */
    public Configurator fromUri(Uri uri) {
        return new Configurator(new UriSource(uri));
    }

    /** Use bytearray as the pdf source, documents is not saved */
    public Configurator fromBytes(byte[] bytes) {
        return new Configurator(new ByteArraySource(bytes));
    }

    /** Use stream as the pdf source. Stream will be written to bytearray, because native code does not support Java Streams */
    public Configurator fromStream(InputStream stream, String tmpFileName) {
        return new Configurator(new InputStreamSource(stream, tmpFileName));
    }

    /** Use custom source as pdf source */
    public Configurator fromSource(DocumentSource docSource) {
        return new Configurator(docSource);
    }

    private enum State {DEFAULT, LOADED, SHOWN, ERROR}

    public class Configurator {

        private final DocumentSource documentSource;

        private int[] pageNumbers = null;

        private boolean enableSwipe = true;

        private boolean enableDoubletap = true;

        private OnDrawListener onDrawListener;

        private OnDrawListener onDrawAllListener;

        private OnLoadCompleteListener onLoadCompleteListener;

        private OnErrorListener onErrorListener;

        private OnPageChangeListener onPageChangeListener;

        private OnPageScrollListener onPageScrollListener;

        private OnRenderListener onRenderListener;

        private OnTapListener onTapListener;

        private OnPageErrorListener onPageErrorListener;

        private LinkHandler linkHandler = new DefaultLinkHandler(PDFView.this);

        private int defaultPage = 0;

        private boolean swipeHorizontal = false;

        private boolean annotationRendering = false;

        private String password = null;

        private ScrollHandle scrollHandle = null;

        private boolean antialiasing = true;

        private int spacing = 0;

        private FitPolicy pageFitPolicy = FitPolicy.WIDTH;

        private Configurator(DocumentSource documentSource) {
            this.documentSource = documentSource;
        }

        public Configurator pages(int... pageNumbers) {
            this.pageNumbers = pageNumbers;
            return this;
        }

        public Configurator enableSwipe(boolean enableSwipe) {
            this.enableSwipe = enableSwipe;
            return this;
        }

        public Configurator enableDoubletap(boolean enableDoubletap) {
            this.enableDoubletap = enableDoubletap;
            return this;
        }

        public Configurator enableAnnotationRendering(boolean annotationRendering) {
            this.annotationRendering = annotationRendering;
            return this;
        }

        public Configurator onDraw(OnDrawListener onDrawListener) {
            this.onDrawListener = onDrawListener;
            return this;
        }

        public Configurator onDrawAll(OnDrawListener onDrawAllListener) {
            this.onDrawAllListener = onDrawAllListener;
            return this;
        }

        public Configurator onLoad(OnLoadCompleteListener onLoadCompleteListener) {
            this.onLoadCompleteListener = onLoadCompleteListener;
            return this;
        }

        public Configurator onPageScroll(OnPageScrollListener onPageScrollListener) {
            this.onPageScrollListener = onPageScrollListener;
            return this;
        }

        public Configurator onError(OnErrorListener onErrorListener) {
            this.onErrorListener = onErrorListener;
            return this;
        }

        public Configurator onPageError(OnPageErrorListener onPageErrorListener) {
            this.onPageErrorListener = onPageErrorListener;
            return this;
        }

        public Configurator onPageChange(OnPageChangeListener onPageChangeListener) {
            this.onPageChangeListener = onPageChangeListener;
            return this;
        }

        public Configurator onRender(OnRenderListener onRenderListener) {
            this.onRenderListener = onRenderListener;
            return this;
        }

        public Configurator onTap(OnTapListener onTapListener) {
            this.onTapListener = onTapListener;
            return this;
        }

        public Configurator linkHandler(LinkHandler linkHandler) {
            this.linkHandler = linkHandler;
            return this;
        }

        public Configurator defaultPage(int defaultPage) {
            this.defaultPage = defaultPage;
            return this;
        }

        public Configurator swipeHorizontal(boolean swipeHorizontal) {
            this.swipeHorizontal = swipeHorizontal;
            return this;
        }

        public Configurator password(String password) {
            this.password = password;
            return this;
        }

        public Configurator scrollHandle(ScrollHandle scrollHandle) {
            this.scrollHandle = scrollHandle;
            return this;
        }

        public Configurator enableAntialiasing(boolean antialiasing) {
            this.antialiasing = antialiasing;
            return this;
        }

        public Configurator spacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Configurator pageFitPolicy(FitPolicy pageFitPolicy) {
            this.pageFitPolicy = pageFitPolicy;
            return this;
        }

        public void load() {
            PDFView.this.recycle();
            PDFView.this.callbacks.setOnLoadComplete(onLoadCompleteListener);
            PDFView.this.callbacks.setOnError(onErrorListener);
            PDFView.this.callbacks.setOnDraw(onDrawListener);
            PDFView.this.callbacks.setOnDrawAll(onDrawAllListener);
            PDFView.this.callbacks.setOnPageChange(onPageChangeListener);
            PDFView.this.callbacks.setOnPageScroll(onPageScrollListener);
            PDFView.this.callbacks.setOnRender(onRenderListener);
            PDFView.this.callbacks.setOnTap(onTapListener);
            PDFView.this.callbacks.setOnPageError(onPageErrorListener);
            PDFView.this.callbacks.setLinkHandler(linkHandler);
            PDFView.this.setSwipeEnabled(enableSwipe);
            PDFView.this.enableDoubletap(enableDoubletap);
            PDFView.this.setDefaultPage(defaultPage);
            PDFView.this.setSwipeVertical(!swipeHorizontal);
            PDFView.this.enableAnnotationRendering(annotationRendering);
            PDFView.this.setScrollHandle(scrollHandle);
            PDFView.this.enableAntialiasing(antialiasing);
            PDFView.this.setSpacing(spacing);
            PDFView.this.setPageFitPolicy(pageFitPolicy);

            PDFView.this.removeCallbacks(loadRunnable);
            PDFView.this.post(loadRunnable);
        }

        private final Runnable loadRunnable = new Runnable() {
            @Override
            public void run() {
                if (pageNumbers != null) {
                    PDFView.this.load(documentSource, password, pageNumbers);
                } else {
                    PDFView.this.load(documentSource, password);
                }
            }
        };
    }
}
