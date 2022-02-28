package mega.privacy.android.app.components;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.Map;
import java.util.WeakHashMap;

public class ListenScrollChangesHelper {
    private final WeakHashMap<View, Item> mViewToListenerMap = new WeakHashMap<>();

    @SuppressLint("NewApi")
    public void addViewToListen(View view, OnScrollChangeListenerCompat listener) {
        if (view == null || listener == null)
            return;

        // Use native scrolling on newer Android devices.
        view.setOnScrollChangeListener(new OnScrollChangeListenerAdapter(listener));
        mViewToListenerMap.put(view, null);
    }

    @SuppressLint("NewApi")
    public void removeViewToListen(View view) {
        if (view == null || mViewToListenerMap.size() == 0)
            return;

        view.removeOnLayoutChangeListener(mLayoutChangeListener);
        view.setOnScrollChangeListener(null);
        mViewToListenerMap.remove(view);
    }

    public void clear() {
        for (View view : mViewToListenerMap.keySet()) {
            removeViewToListen(view);
        }
    }

    // If ViewTreeObserver is not alive, it will throw exception on call to any method except isAlive().
    private static void safeAddOnScrollChangeListener(ViewTreeObserver observer, ViewTreeObserver.OnScrollChangedListener listener) {
        if (observer.isAlive()) {
            observer.addOnScrollChangedListener(listener);
        }
    }

    private static void safeRemoveOnScrollChangeListener(ViewTreeObserver observer, ViewTreeObserver.OnScrollChangedListener listener) {
        if (observer.isAlive()) {
            observer.removeOnScrollChangedListener(listener);
        }
    }

    private final ViewTreeObserver.OnScrollChangedListener mObserverOnScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            for (Map.Entry<View, Item> entry : mViewToListenerMap.entrySet()) {
                int scrollX = Math.round(entry.getKey().getScrollX());
                int scrollY = Math.round(entry.getKey().getScrollY());
                int oldScrollX = entry.getValue().ScrollPosition.x;
                int oldScrollY = entry.getValue().ScrollPosition.y;
                if (scrollX != oldScrollX || scrollY != oldScrollY) {
                    entry.getValue().Listener.onScrollChange(entry.getKey(), scrollX, scrollY, oldScrollX, oldScrollY);
                    entry.getValue().ScrollPosition.x = scrollX;
                    entry.getValue().ScrollPosition.y = scrollY;
                }
            }
        }
    };

    // ViewTreeObserver is not guaranteed to remain valid for the lifetime of view.
    private final View.OnLayoutChangeListener mLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            Item item = mViewToListenerMap.get(view);
            if (item == null)
                return;

            if (item.Observer != view.getViewTreeObserver()) {
                safeRemoveOnScrollChangeListener(item.Observer, mObserverOnScrollChangedListener);

                item.Observer = view.getViewTreeObserver();
                safeAddOnScrollChangeListener(item.Observer, mObserverOnScrollChangedListener);
            }
        }
    };

    private static class Item {
        Point ScrollPosition;
        OnScrollChangeListenerCompat Listener;
        ViewTreeObserver Observer;

        public Item(Point scrollPosition, OnScrollChangeListenerCompat listener, ViewTreeObserver observer) {
            ScrollPosition = scrollPosition;
            Listener = listener;
            Observer = observer;
        }

    }

    public interface OnScrollChangeListenerCompat {
        void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY);
    }
}