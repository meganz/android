package mega.privacy.android.app.components.twemoji.reaction;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager.widget.ViewPager;

public class BottomSheetUtils {

    public static void setupViewPager(ViewPager viewPager) {
        final View bottomSheetParent = getParent(viewPager);
        if (bottomSheetParent != null) {
            viewPager.addOnPageChangeListener(new BottomSheetViewPagerListener(viewPager, bottomSheetParent));
        }
    }

    private static View getParent(ViewPager viewPager) {
        return findBottomSheetParent(viewPager);
    }

    public static void setBottomSheetBehavior(int totalHeight, int peekHeight, int halfHeightDisplay, ViewPager viewPager) {
        final View bottomSheetParent = getParent(viewPager);
        if (bottomSheetParent != null) {
            final ViewPagerBottomSheetBehavior<View> mBehavior = ViewPagerBottomSheetBehavior.from(bottomSheetParent);
            if (totalHeight < halfHeightDisplay) {
                mBehavior.setState(ViewPagerBottomSheetBehavior.STATE_EXPANDED);
            } else {
                mBehavior.setPeekHeight(peekHeight);
                mBehavior.setState(ViewPagerBottomSheetBehavior.STATE_COLLAPSED);
            }
        }
    }

    private static View findBottomSheetParent(final View view) {
        View current = view;
        while (current != null) {
            final ViewGroup.LayoutParams params = current.getLayoutParams();
            if (params instanceof CoordinatorLayout.LayoutParams && ((CoordinatorLayout.LayoutParams) params).getBehavior() instanceof ViewPagerBottomSheetBehavior) {
                return current;
            }
            final ViewParent parent = current.getParent();
            current = parent == null || !(parent instanceof View) ? null : (View) parent;
        }

        return null;
    }

    private static class BottomSheetViewPagerListener extends ViewPager.SimpleOnPageChangeListener {
        private final ViewPager viewPager;
        private final ViewPagerBottomSheetBehavior<View> behavior;

        private BottomSheetViewPagerListener(ViewPager viewPager, View bottomSheetParent) {
            this.viewPager = viewPager;
            this.behavior = ViewPagerBottomSheetBehavior.from(bottomSheetParent);
        }

        @Override
        public void onPageSelected(int position) {
            viewPager.post(behavior::invalidateScrollingChild);
        }
    }
}
