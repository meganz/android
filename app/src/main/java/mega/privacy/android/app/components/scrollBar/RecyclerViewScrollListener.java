package mega.privacy.android.app.components.scrollBar;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class RecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private final FastScroller scroller;
    List<ScrollerListener> listeners = new ArrayList<>();
    int oldScrollState = RecyclerView.SCROLL_STATE_IDLE;

    public RecyclerViewScrollListener(FastScroller scroller) {
        this.scroller = scroller;
    }

    public void addScrollerListener(ScrollerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onScrollStateChanged(@NotNull RecyclerView recyclerView, int newScrollState) {
        super.onScrollStateChanged(recyclerView, newScrollState);
        if (newScrollState == RecyclerView.SCROLL_STATE_IDLE && oldScrollState != RecyclerView.SCROLL_STATE_IDLE) {
            scroller.getViewProvider().onScrollFinished();
            scroller.showFabButton();
        } else if (newScrollState != RecyclerView.SCROLL_STATE_IDLE && oldScrollState == RecyclerView.SCROLL_STATE_IDLE) {
            scroller.getViewProvider().onScrollStarted();
        }
        oldScrollState = newScrollState;
    }

    @Override
    public void onScrolled(@NotNull RecyclerView rv, int dx, int dy) {
        if (scroller.shouldUpdateHandlePosition()) {
            updateHandlePosition(rv);
            scroller.hideFabButton();
        }
    }

    void updateHandlePosition(RecyclerView rv) {
        float relativePos;
        if (scroller.isVertical()) {
            int offset = rv.computeVerticalScrollOffset();
            int extent = rv.computeVerticalScrollExtent();
            int range = rv.computeVerticalScrollRange();
            relativePos = offset / (float) (range - extent);
        } else {
            int offset = rv.computeHorizontalScrollOffset();
            int extent = rv.computeHorizontalScrollExtent();
            int range = rv.computeHorizontalScrollRange();
            relativePos = offset / (float) (range - extent);
        }
        scroller.setScrollerPosition(relativePos);
        notifyListeners(relativePos);
    }

    public void notifyListeners(float relativePos) {
        for (ScrollerListener listener : listeners) listener.onScroll(relativePos);
    }

    public interface ScrollerListener {
        void onScroll(float relativePos);
    }

}