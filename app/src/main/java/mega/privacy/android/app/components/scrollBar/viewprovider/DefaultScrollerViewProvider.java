package mega.privacy.android.app.components.scrollBar.viewprovider;

import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mega.privacy.android.app.R;


public class DefaultScrollerViewProvider extends ScrollerViewProvider {

    protected View bubble;
    protected View handle;

    @Override
    public View provideHandleView(ViewGroup container) {
        handle = LayoutInflater.from(getContext()).inflate(R.layout.fastscroll__default_handle, container, false);
        handle.setVisibility(View.INVISIBLE);
        return handle;
    }

    @Override
    public View provideBubbleView(ViewGroup container) {
        bubble = LayoutInflater.from(getContext()).inflate(R.layout.fastscroll__default_bubble, container, false);
        bubble.setVisibility(View.INVISIBLE);
        return bubble;
    }

    @Override
    public TextView provideBubbleTextView() {
        return (TextView) bubble;
    }

    @Override
    public int getBubbleOffset() {
        return (int) (getScroller().isVertical() ? ((float)handle.getHeight()/2f)-bubble.getHeight() : ((float)handle.getWidth()/2f)-bubble.getWidth());
    }

    @Override
    protected ViewBehavior provideHandleBehavior() {
        return new DefaultHandleAndBubbleBehavior((new VisibilityAnimationManager.Builder(handle).withPivotX(1f).withPivotY(1f).build()), (new VisibilityAnimationManager.Builder(bubble).withPivotX(1f).withPivotY(1f).build()));
    }

    @Override
    protected ViewBehavior provideBubbleBehavior() {
        return  null;
    }

}