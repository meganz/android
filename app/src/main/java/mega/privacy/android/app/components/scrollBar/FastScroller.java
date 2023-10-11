package mega.privacy.android.app.components.scrollBar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.viewprovider.DefaultScrollerViewProvider;
import mega.privacy.android.app.components.scrollBar.viewprovider.ScrollerViewProvider;

/**
 * Credit: <a href="Github">https://github.com/FutureMind/recycler-fast-scroll</a>
 */
public class FastScroller extends LinearLayout {


    private static final int STYLE_NONE = -1;
    private final RecyclerViewScrollListener scrollListener = new RecyclerViewScrollListener(this);
    private RecyclerView recyclerView;

    private View bubble;
    private View handle;
    private TextView bubbleTextView;

    private int bubbleOffset;
    private int bubbleTextAppearance;
    private int scrollerOrientation;

    private int maxVisibility;

    private boolean manuallyChangingPosition;

    private ScrollerViewProvider viewProvider;
    private SectionTitleProvider titleProvider;

    private FastScrollerScrollListener fastScrollerScrollListener;

    public FastScroller(Context context) {
        this(context, null);
    }

    public FastScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScroller(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setClipChildren(false);
        TypedArray style = context.obtainStyledAttributes(attrs, R.styleable.FastScroller, R.attr.fastscroll__style, 0);
        try {
            bubbleTextAppearance = style.getResourceId(R.styleable.FastScroller_fastscroll__bubbleTextAppearance, R.style.StyledScrollerTextAppearance);
        } finally {
            style.recycle();
        }
        maxVisibility = getVisibility();
        setViewProvider(new DefaultScrollerViewProvider());
    }

    public void setViewProvider(ScrollerViewProvider viewProvider) {
        removeAllViews();
        this.viewProvider = viewProvider;
        viewProvider.setFastScroller(this);
        bubble = viewProvider.provideBubbleView(this);
        bubbleTextView = viewProvider.provideBubbleTextView();
        addView(bubble);
        handle = viewProvider.provideHandleView(this);
        addView(handle);

        bubbleOffset = viewProvider.getBubbleOffset();
        initHandleMovement();
        applyStyling();
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;

        if (recyclerView.getAdapter() instanceof SectionTitleProvider)
            titleProvider = (SectionTitleProvider) recyclerView.getAdapter();

        recyclerView.addOnScrollListener(scrollListener);
        invalidateVisibility();
        recyclerView.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                invalidateVisibility();
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
                invalidateVisibility();
            }
        });
    }

    @Override
    public void setOrientation(int orientation) {
        scrollerOrientation = orientation;
        super.setOrientation(orientation == HORIZONTAL ? VERTICAL : HORIZONTAL);
    }

    public void setBubbleTextAppearance(int textAppearanceResourceId) {
        bubbleTextAppearance = textAppearanceResourceId;
        invalidate();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (!isInEditMode() && recyclerView != null) {
            scrollListener.updateHandlePosition(recyclerView);
        }
    }

    public void addScrollerListener(RecyclerViewScrollListener.ScrollerListener listener) {
        scrollListener.addScrollerListener(listener);
    }

    private void applyStyling() {
        if (bubbleTextAppearance != STYLE_NONE)
            TextViewCompat.setTextAppearance(bubbleTextView, bubbleTextAppearance);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initHandleMovement() {
        handle.setOnTouchListener((v, event) -> {
            requestDisallowInterceptTouchEvent(true);
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                if (titleProvider != null && event.getAction() == MotionEvent.ACTION_DOWN)
                    viewProvider.onHandleGrabbed();
                manuallyChangingPosition = true;
                float relativePos = getRelativeTouchPosition(event);
                setScrollerPosition(relativePos);
                setRecyclerViewPosition(relativePos);
                hideFabButton();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                manuallyChangingPosition = false;
                if (titleProvider != null) viewProvider.onHandleReleased();
                showFabButton();
                return true;
            }
            return false;
        });
    }

    private float getRelativeTouchPosition(MotionEvent event) {
        if (isVertical()) {
            float yInParent = event.getRawY() - Utils.getViewRawY(handle);
            return yInParent / (getHeight() - handle.getHeight());
        } else {
            float xInParent = event.getRawX() - Utils.getViewRawX(handle);
            return xInParent / (getWidth() - handle.getWidth());
        }
    }

    @Override
    public void setVisibility(int visibility) {
        maxVisibility = visibility;
        invalidateVisibility();
    }

    private void invalidateVisibility() {
        if (isRecyclerViewNotScrollable() || maxVisibility != View.VISIBLE) {
            super.setVisibility(INVISIBLE);
        } else {
            super.setVisibility(VISIBLE);
        }
    }

    private boolean isRecyclerViewNotScrollable() {
        if (recyclerView == null) {
            return true;
        }

        if (isVertical()) {
            return !recyclerView.canScrollVertically(1) && !recyclerView.canScrollVertically(-1);
        } else {
            return !recyclerView.canScrollHorizontally(1) && !recyclerView.canScrollHorizontally(-1);
        }
    }

    private void setRecyclerViewPosition(float relativePos) {
        if (recyclerView == null) return;
        if (recyclerView.getAdapter() == null) return;
        int itemCount = recyclerView.getAdapter().getItemCount();
        int targetPos = (int) Utils.getValueInRange(0, itemCount - 1, (int) (relativePos * (float) itemCount));
        recyclerView.scrollToPosition(targetPos);
        if (titleProvider != null && bubbleTextView != null) {
            if (titleProvider.getSectionTitle(targetPos, getContext()) != null) {
                bubbleTextView.setText(titleProvider.getSectionTitle(targetPos, getContext()));
            }
        }
        setScrollerPosition(relativePos);
    }

    void setScrollerPosition(float relativePos) {

        if (isVertical()) {
            bubble.setY(Utils.getValueInRange(0, getHeight() - bubble.getHeight(), relativePos * (getHeight() - handle.getHeight()) + bubbleOffset));
            handle.setY(Utils.getValueInRange(0, getHeight() - handle.getHeight(), relativePos * (getHeight() - handle.getHeight())));
        } else {
            bubble.setX(Utils.getValueInRange(0, getWidth() - bubble.getWidth(), relativePos * (getWidth() - handle.getWidth()) + bubbleOffset));
            handle.setX(Utils.getValueInRange(0, getWidth() - handle.getWidth(), relativePos * (getWidth() - handle.getWidth())));
        }
    }

    public boolean isVertical() {
        return scrollerOrientation == VERTICAL;
    }

    boolean shouldUpdateHandlePosition() {
        return handle != null && !manuallyChangingPosition && recyclerView.getChildCount() > 0;
    }

    ScrollerViewProvider getViewProvider() {
        return viewProvider;
    }

    /**
     * Restore FAB when the scroller disappeared / user stop scrolling.
     */
    public void showFabButton() {
        if (fastScrollerScrollListener != null) {
            fastScrollerScrollListener.onScrolledToTop();
        }
    }

    /**
     * Hide FAB when the user is scrolling scroller
     */
    public void hideFabButton() {
        if (fastScrollerScrollListener != null) {
            fastScrollerScrollListener.onScrolled();
        }
    }

    /**
     * Set up fast scroller listener
     */
    public void setUpScrollListener(FastScrollerScrollListener fastScrollerScrollListener) {
        this.fastScrollerScrollListener = fastScrollerScrollListener;
    }
}