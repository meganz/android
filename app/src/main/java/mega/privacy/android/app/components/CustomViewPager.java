package mega.privacy.android.app.components;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomViewPager extends ViewPager {

    private Boolean disableSwipe = false;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return disableSwipe ? false : super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return disableSwipe ? false : super.onTouchEvent(event);
    }

    public void disableSwipe(Boolean disable) {
        this.disableSwipe = disable;
    }
}
