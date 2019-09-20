package com.github.barteksc.pdfviewer.scroll;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.util.Util;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;

import static mega.privacy.android.app.utils.LogUtil.*;

public class DefaultScrollHandle extends RelativeLayout implements ScrollHandle {

    float motionYOrigin;

    private final static int HANDLE_SHORT = 50;
    private final static int DEFAULT_TEXT_SIZE = 12;

    private float relativeHandlerMiddle = 0f;

//    protected TextView textViewBubble;
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
        logDebug("DefaultScrollHandle");
        this.context = context;
        this.inverted = inverted;
        textViewHandle = new TextView(context);
//        textViewBubble = new TextView(context);

        setVisibility(INVISIBLE);
//        setTextColor(Color.WHITE);
//        setTextSize(DEFAULT_TEXT_SIZE);
    }

    @Override
    public void setupLayout(PDFView pdfView) {
        logDebug("setupLayout");

        RelativeLayout.LayoutParams tvHlp = new RelativeLayout.LayoutParams(Util.getDP(context, 50), Util.getDP(context, 50));
        textViewHandle.setBackgroundResource(R.drawable.fastscroll_pdf_viewer);
        tvHlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        tvHlp.setMargins(Util.getDP(context, 5), 0, Util.getDP(context, -20), 0);
        textViewHandle.setPadding(Util.getDP(context, 10), Util.getDP(context, 10), Util.getDP(context, 10), Util.getDP(context, 10));
        addView(textViewHandle, tvHlp);

//        RelativeLayout.LayoutParams tvBlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        textViewBubble.setBackgroundResource(R.drawable.fastscroll__default_bubble);
//        tvBlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
//        textViewBubble.setTextAlignment(TEXT_ALIGNMENT_CENTER);
//        textViewBubble.setPadding(Util.getDP(context, 10), Util.getDP(context, 5), Util.getDP(context, 10), Util.getDP(context, 5));
//        tvBlp.setMargins(0, 0, Util.getDP(context, 30), 0);
//        addView(textViewBubble, tvBlp);


        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(Util.getDP(context, 40), RelativeLayout.LayoutParams.WRAP_CONTENT);
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

        String text = String.valueOf(pageNum);
//        if (!textViewBubble.getText().equals(text)) {
//            textViewBubble.setText(text);
//        }
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
        animate().translationX(0).setDuration(200L).withEndAction(new Runnable() {
            @Override
            public void run() {
                hideDelayed();
            }
        }).start();
    }

    @Override
    public void hide() {
        logDebug("hide");
        animate().translationX(200).setDuration(200L).withEndAction(new Runnable() {
            @Override
            public void run() {
                setVisibility(INVISIBLE);
            }
        }).start();
    }

//    public void setTextColor(int color) {
//        log("setTextColor");
//        textViewBubble.setTextColor(color);
//    }

//    /**
//     * @param size text size in sp
//     */
//    public void setTextSize(int size) {
//        log("setTextSize");
//        textViewBubble.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
//    }

    private boolean isPDFViewReady() {
        logDebug("isPDFViewReady");
        return pdfView != null && pdfView.getPageCount() > 0 && !pdfView.documentFitsView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        logDebug("onTouchEvent");

        final int action = MotionEventCompat.getActionMasked(event);

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
                if (Math.abs(motionYOrigin - newMotionY) > 5 && ((PdfViewerActivityLollipop) context).isToolbarVisible()){
                    ((PdfViewerActivityLollipop) context).setToolbarVisibilityHide(200L);
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
}
