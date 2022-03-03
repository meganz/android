package com.github.barteksc.pdfviewer.scroll;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.Util.dp2px;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.Util;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PdfViewerActivity;

public class DefaultScrollHandle extends ConstraintLayout implements ScrollHandle {

    float motionYOrigin;

    private final static int HANDLE_SHORT = 50;
    private final static int DEFAULT_TEXT_SIZE = 12;

    private float relativeHandlerMiddle = 0f;

    protected TextView textViewBubble;
    protected TextView textViewHandle;
    protected Context context;
    private PDFView pdfView;
    private float currentPos;
    private final Handler handler = new Handler();
    private final Runnable hidePageScrollerRunnable = this::hide;
    private int totalPages = 0;

    public DefaultScrollHandle(Context context) {
        super(context);
        logDebug("DefaultScrollHandle");
        this.context = context;
        textViewHandle = new TextView(context);
        textViewBubble = new TextView(context);

        setVisibility(INVISIBLE);
        setTextColor(ContextCompat.getColor(context, R.color.scroll_bubble_text_color));
        setTextSize(DEFAULT_TEXT_SIZE);
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        logDebug("setupLayout");

        ConstraintLayout.LayoutParams textViewHandleLp = new ConstraintLayout.LayoutParams(dp2px(45), dp2px(45));
        textViewHandleLp.endToEnd = LayoutParams.PARENT_ID;
        textViewHandleLp.topToTop = LayoutParams.PARENT_ID;
        textViewHandleLp.bottomToBottom = LayoutParams.PARENT_ID;
        textViewHandleLp.setMargins(dp2px(5), dp2px(5), dp2px(-10), dp2px(5));
        textViewHandle.setBackgroundResource(R.drawable.fastscroll_pdf_viewer);
        textViewHandle.setPadding(dp2px(10), dp2px(10), dp2px(10), dp2px(10));
        textViewHandle.setElevation(dp2px(4));
        addView(textViewHandle, textViewHandleLp);

        ConstraintLayout.LayoutParams textViewBubbleLp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        textViewBubbleLp.endToEnd = LayoutParams.PARENT_ID;
        textViewBubbleLp.topToTop = LayoutParams.PARENT_ID;
        textViewBubbleLp.bottomToBottom = LayoutParams.PARENT_ID;
        textViewBubbleLp.setMargins(0, 0, dp2px(40), 0);
        textViewBubble.setBackgroundResource(R.drawable.fastscroll_pdf_bubble);
        textViewBubble.setElevation(dp2px(4));
        textViewBubble.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        textViewBubble.setTextColor(ContextCompat.getColor(context, R.color.scroll_bubble_text_color));
        textViewBubble.setPadding(dp2px(10), dp2px(6), dp2px(10), dp2px(6));
        addView(textViewBubble, textViewBubbleLp);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        pdfView.addView(this, lp);

        this.pdfView = pdfView;
    }

    @Override
    public void destroyLayout() {
        logDebug("destroyLayout");
        pdfView.removeView(this);
    }

    @Override
    public void setScroll(float position) {
        logDebug("setScroll");

        if (!shown()) {
            show();
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable);
        }
        setPosition((pdfView.isSwipeVertical() ? pdfView.getHeight() : pdfView.getWidth()) * position);
    }

    private void setPosition(float pos) {
        logDebug("setPosition");
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
        logDebug("calculateMiddle");
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
        logDebug("hideDelayed");
        handler.postDelayed(hidePageScrollerRunnable, 1000);
    }

    @Override
    public void setPageNum(int pageNum) {
        logDebug("setPageNum");

        String strCurrentPages = String.valueOf(pageNum);
        String strTotalPages = String.valueOf(totalPages);
        String text = strCurrentPages + " / " + strTotalPages;
        if (!textViewBubble.getText().equals(text)) {
            textViewBubble.setText(text);
        }
    }

    @Override
    public boolean shown() {
        logDebug("shown boolean");
        return getVisibility() == VISIBLE;
    }

    @Override
    public void show() {
        logDebug("shown");
        setVisibility(VISIBLE);
        animate().translationX(0).setDuration(200L).withEndAction(this::hideDelayed).start();
    }

    @Override
    public void hide() {
        logDebug("hide");
        animate().translationX(200).setDuration(200L).withEndAction(() -> setVisibility(INVISIBLE)).start();
    }

    public void setTextColor(int color) {
        logDebug("setTextColor");
        textViewBubble.setTextColor(color);
    }

    /**
     * @param size text size in sp
     */
    public void setTextSize(int size) {
        logDebug("setTextSize");
        textViewBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    private boolean isPDFViewReady() {
        logDebug("isPDFViewReady");
        return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        logDebug("onTouchEvent");

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                motionYOrigin = event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                motionYOrigin = 0;
                break;
            case MotionEvent.ACTION_MOVE: {
                float newMotionY = event.getRawY();
                if (Math.abs(motionYOrigin - newMotionY) > 5 && ((PdfViewerActivity) context).isToolbarVisible()) {
                    ((PdfViewerActivity) context).setToolbarVisibilityHide(200L);
                }
            }
        }

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
            case MotionEvent.ACTION_MOVE:
                if (pdfView.isSwipeVertical()) {
                    setPosition(event.getRawY() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getHeight(), false);
                } else {
                    setPosition(event.getRawX() - currentPos + relativeHandlerMiddle);
                    pdfView.setPositionOffset(relativeHandlerMiddle / (float) getWidth(), false);
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                hideDelayed();
                return true;
        }

        return super.onTouchEvent(event);
    }

    public void setTotalPages(int nbPages) {
        totalPages = nbPages;
    }
}
