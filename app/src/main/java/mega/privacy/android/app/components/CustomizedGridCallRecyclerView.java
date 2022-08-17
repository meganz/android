package mega.privacy.android.app.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.annotation.StyleableRes;
import androidx.recyclerview.widget.RecyclerView;

import timber.log.Timber;

public class CustomizedGridCallRecyclerView extends RecyclerView {

    private CustomizedGridLayoutManager manager;
    public int columnWidth = -1;
    private boolean isWrapContent = false;
    private int widthTotal = 0;

    private OnTouchCallback onTouchCallback;

    public interface OnTouchCallback {

        void onTouch();
    }

    public CustomizedGridCallRecyclerView(Context context) {
        super(context);
        init(context, null);
    }

    public CustomizedGridCallRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CustomizedGridCallRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs != null) {
            @StyleableRes int[] attrsArray = new int[]{
                    android.R.attr.columnWidth,
            };
            TypedArray array = context.obtainStyledAttributes(attrs, attrsArray);
            columnWidth = array.getDimensionPixelSize(0, -1);
            array.recycle();
        }

        manager = new CustomizedGridLayoutManager(getContext(), 1);
        setLayoutManager(manager);
    }

    public void setOnTouchCallback(OnTouchCallback onTouchCallback) {
        this.onTouchCallback = onTouchCallback;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        Timber.d("onMeasure-> widthSpec: %d, heightSpec: %d", widthSpec, heightSpec);
        super.onMeasure(widthSpec, heightSpec);
        if (!isWrapContent) {
            Timber.d("columnWidth :%s", columnWidth);
            if (columnWidth > 0) {
                int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
                Timber.d("spanCount: %s", spanCount);
                manager.setSpanCount(spanCount);
            }
        } else {

            ViewGroup.LayoutParams params = getLayoutParams();
            Timber.d("columnWidth :%s", columnWidth);
            if (columnWidth > 0) {
                int spanCount = Math.max(1, getMeasuredWidth() / columnWidth);
                Timber.d("spanCount: %s", spanCount);
                manager.setSpanCount(spanCount);
                params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                setLayoutParams(params);
            }
        }
    }

    public void setWrapContent() {
        isWrapContent = true;
//		widthTotal = getMeasuredWidth();
//		ViewGroup.LayoutParams params = getLayoutParams();
//		params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
//		setLayoutParams(params);
        this.invalidate();
    }

    public int findFirstCompletelyVisibleItemPosition() {
        return getLayoutManager().findFirstCompletelyVisibleItemPosition();
    }

    public int findFirstVisibleItemPosition() {
        return getLayoutManager().findFirstVisibleItemPosition();
    }

    public int getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(int columnWidth) {
        this.columnWidth = columnWidth;
    }

    class SingleTapListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (onTouchCallback != null) {
                onTouchCallback.onTouch();
            }
            return false;
        }
    }

    private final GestureDetector detector = new GestureDetector(getContext(), new SingleTapListener());

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        detector.onTouchEvent(e);
        performClick();
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public CustomizedGridLayoutManager getLayoutManager() {
        return manager;
    }
}
