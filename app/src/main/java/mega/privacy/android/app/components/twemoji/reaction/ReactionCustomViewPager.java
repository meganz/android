package mega.privacy.android.app.components.twemoji.reaction;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class ReactionCustomViewPager extends ViewPager {

    private boolean isPagingEnabled = true;

    public ReactionCustomViewPager(Context context) {
        super(context);
    }

    public ReactionCustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.isPagingEnabled && super.onInterceptTouchEvent(event);
    }
}