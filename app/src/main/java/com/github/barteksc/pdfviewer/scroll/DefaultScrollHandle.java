package com.github.barteksc.pdfviewer.scroll;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.Util;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;

public class DefaultScrollHandle extends RelativeLayout implements ScrollHandle {

    private final static int HANDLE_LONG = 70;
    private final static int HANDLE_SHORT = 40;
    private final static int DEFAULT_TEXT_SIZE = 12;

    private float relativeHandlerMiddle = 0f;

    protected TextView textViewBubble;
    protected TextView textViewHandle;
    protected Context context;
    private boolean inverted;
    private PDFView pdfView;
    private float currentPos;
    private Handler handler = new Handler();
    private Runnable hidePageScrollerRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    public DefaultScrollHandle(Context context) {
        this(context, false);
    }

    public DefaultScrollHandle(Context context, boolean inverted) {
        super(context);
        log("DefaultScrollHandle");
        this.context = context;
        this.inverted = inverted;
        textViewHandle = new TextView(context);
        textViewBubble = new TextView(context);
        setVisibility(INVISIBLE);
        setTextColor(Color.WHITE);
        setTextSize(DEFAULT_TEXT_SIZE);
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        log("setupLayout");
        int align;

        // determine handler position, default is right (when scrolling vertically) or bottom (when scrolling horizontally)
        if (pdfView.isSwipeVertical()) {
            if (inverted) { // left
                align = ALIGN_PARENT_LEFT;
            } else { // right
                align = ALIGN_PARENT_RIGHT;
            }
        } else {
            if (inverted) { // top
                align = ALIGN_PARENT_TOP;
            } else { // bottom
                align = ALIGN_PARENT_BOTTOM;
            }
        }

        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);

        LayoutParams tvHlp = new LayoutParams(Util.getDP(context, 50), Util.getDP(context, 50));
        textViewHandle.setBackgroundResource(R.drawable.fastscroll__default_handle);
        tvHlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        textViewHandle.setGravity(Gravity.CENTER);
        tvHlp.setMargins(Util.getDP(context, 5), 0, Util.getDP(context, -20), 0);
        textViewHandle.setPadding(Util.getDP(context, 10), Util.getDP(context, 10), Util.getDP(context, 10), Util.getDP(context, 10));
        addView(textViewHandle, tvHlp);

        LayoutParams tvBlp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textViewBubble.setBackgroundResource(R.drawable.fastscroll__default_bubble);
        tvBlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        textViewBubble.setGravity(Gravity.CENTER);
        textViewBubble.setPadding(Util.getDP(context, 10), Util.getDP(context, 5), Util.getDP(context, 10), Util.getDP(context, 5));
        tvBlp.setMargins(Util.getDP(context, 5), 0, Util.getDP(context, 30), 0);
        addView(textViewBubble, tvBlp);

        lp.addRule(align);
        pdfView.addView(this, lp);

        this.pdfView = pdfView;
    }

    @Override
    public void destroyLayout() {
        log("destroyLayout");
        pdfView.removeView(this);
    }

    @Override
    public void setScroll(float position) {
        log("setScroll");

        if (!shown()) {
            show();
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable);
        }
        setPosition((pdfView.isSwipeVertical() ? pdfView.getHeight() : pdfView.getWidth()) * position);
    }

    private void setPosition(float pos) {
        log("setPosition");
        if (Float.isInfinite(pos) || Float.isNaN(pos)) {
            return;
        }
        float pdfViewSize;
        if (pdfView.isSwipeVertical()) {
            pdfViewSize = pdfView.getHeight();
        } else {
            pdfViewSize = pdfView.getWidth();
        }
        pos -= relativeHandlerMiddle;

        if (pos < 0) {
            pos = 0;
        } else if (pos > pdfViewSize - Util.getDP(context, HANDLE_SHORT)) {
            pos = pdfViewSize - Util.getDP(context, HANDLE_SHORT);
        }

        if (pdfView.isSwipeVertical()) {
            setY(pos);
        } else {
            setX(pos);
        }

        calculateMiddle();
        invalidate();
    }

    private void calculateMiddle() {
        log("calculateMiddle");
        float pos, viewSize, pdfViewSize;
        if (pdfView.isSwipeVertical()) {
            pos = getY();
            viewSize = getHeight();
            pdfViewSize = pdfView.getHeight();
        } else {
            pos = getX();
            viewSize = getWidth();
            pdfViewSize = pdfView.getWidth();
        }
        relativeHandlerMiddle = ((pos + relativeHandlerMiddle) / pdfViewSize) * viewSize;
    }

    @Override
    public void hideDelayed() {
        log("hideDelayed");
        handler.postDelayed(hidePageScrollerRunnable, 1000);
    }

    @Override
    public void setPageNum(int pageNum) {
        log("setPageNum");
        String text = String.valueOf(pageNum);
        if (!textViewBubble.getText().equals(text)) {
            textViewBubble.setText(text);
        }
    }

    @Override
    public boolean shown() {
        log("shown boolean");
        return getVisibility() == VISIBLE;
    }

    @Override
    public void show() {
        log("shown");
        setVisibility(VISIBLE);
    }

    @Override
    public void hide() {
        log("hide");
        setVisibility(INVISIBLE);
    }

    public void setTextColor(int color) {
        log("setTextColor");
        textViewBubble.setTextColor(color);
    }

    /**
     * @param size text size in sp
     */
    public void setTextSize(int size) {
        log("setTextSize");
        textViewBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private boolean isPDFViewReady() {
        log("isPDFViewReady");
        return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        log("onTouchEvent");

        if (!isPDFViewReady()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                pdfView.stopFling();
                handler.removeCallbacks(hidePageScrollerRunnable);
                if (pdfView.isSwipeVertical()) {
                    currentPos = event.getRawY() - getY();
                } else {
                    currentPos = event.getRawX() - getX();
                }
            case MotionEvent.ACTION_SCROLL:
                ((PdfViewerActivityLollipop) getContext()).isScrolling = true;
                ((PdfViewerActivityLollipop) getContext()).scroll = false;
                ((PdfViewerActivityLollipop) getContext()).establishScroll();
            case MotionEvent.ACTION_MOVE:
                if (pdfView.isSwipeVertical()) {
                    setPosition(event.getRawY() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getHeight(), false);
                } else {
                    setPosition(event.getRawX() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getWidth(), false);
                }
                ((PdfViewerActivityLollipop) getContext()).isScrolling = true;
                ((PdfViewerActivityLollipop) getContext()).scroll = false;
                ((PdfViewerActivityLollipop) getContext()).establishScroll();
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                hideDelayed();
                return true;
        }

        return super.onTouchEvent(event);
    }

    public static void log(String log) {
        mega.privacy.android.app.utils.Util.log("DefaultScrollHandle", log);
    }
}
