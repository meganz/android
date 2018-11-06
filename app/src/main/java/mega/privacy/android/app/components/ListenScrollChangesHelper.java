package mega.privacy.android.app.components;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Build;
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

        // Fall-back to native solution on newer Android devices.
        if (useNativeScrollChangeListener()) {
            view.setOnScrollChangeListener(new OnScrollChangeListenerAdapter(listener));
            mViewToListenerMap.put(view, null);
            return;
        }

        if (!mViewToListenerMap.containsKey(view)) {
            // Handle case, when previously added view has the same ViewTreeObserver.
            view.getViewTreeObserver().removeOnScrollChangedListener(mObserverOnScrollChangedListener);
            view.getViewTreeObserver().addOnScrollChangedListener(mObserverOnScrollChangedListener);

            view.removeOnLayoutChangeListener(mLayoutChangeListener);
            view.addOnLayoutChangeListener(mLayoutChangeListener);
        }
        Item item = new Item(new Point(view.getScrollX(), view.getScrollY()), listener, view.getViewTreeObserver());
        mViewToListenerMap.put(view, item);
    }

    @SuppressLint("NewApi")
    public void removeViewToListen(View view) {
        if (view == null || mViewToListenerMap.size() == 0)
            return;

        view.removeOnLayoutChangeListener(mLayoutChangeListener);
        if (useNativeScrollChangeListener()) {
            view.setOnScrollChangeListener(null);
        } else if (!haveAnotherViewWithSameObserver(view)) {
            view.getViewTreeObserver().removeOnScrollChangedListener(mObserverOnScrollChangedListener);
        }
        mViewToListenerMap.remove(view);
    }

    public void clear() {
        for (View view : mViewToListenerMap.keySet()) {
            removeViewToListen(view);
        }
    }

    private boolean haveAnotherViewWithSameObserver(View view) {
        for (Map.Entry<View, Item> entry : mViewToListenerMap.entrySet()) {
            if (entry.getKey() != view && entry.getKey().getViewTreeObserver() == view.getViewTreeObserver())
                return true;
        }
        return false;
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

    private static boolean useNativeScrollChangeListener() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
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