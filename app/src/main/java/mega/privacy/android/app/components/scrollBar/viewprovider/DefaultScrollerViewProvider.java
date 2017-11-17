package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.graphics.drawable.InsetDrawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.scrollBar.Utils;


public class DefaultScrollerViewProvider extends ScrollerViewProvider {

    protected View bubble;
    protected View handle;

    @Override
    public View provideHandleView(ViewGroup container) {
//        handle = new View(getContext());
//
//        int verticalInset = getScroller().isVertical() ? 0 : 30;
//        int horizontalInset = !getScroller().isVertical() ? 0 : 30;
//        InsetDrawable handleBg = new InsetDrawable(ContextCompat.getDrawable(getContext(), R.drawable.fastscroll__default_handle), horizontalInset, verticalInset, horizontalInset, verticalInset);
//        Utils.setBackground(handle, handleBg);
//
//        int handleWidth = getContext().getResources().getDimensionPixelSize(getScroller().isVertical() ? R.dimen.fastscroll__handle_clickable_width : R.dimen.fastscroll__handle_height);
//        int handleHeight = getContext().getResources().getDimensionPixelSize(getScroller().isVertical() ? R.dimen.fastscroll__handle_height : R.dimen.fastscroll__handle_clickable_width);
//        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(handleWidth, handleHeight);
//        handle.setLayoutParams(params);

        handle = LayoutInflater.from(getContext()).inflate(R.layout.fastscroll__default_handle, container, false);
        handle.setVisibility(View.VISIBLE);
        return handle;
    }

    @Override
    public View provideBubbleView(ViewGroup container) {
        bubble = LayoutInflater.from(getContext()).inflate(R.layout.fastscroll__default_bubble, container, false);
        bubble.setVisibility(View.VISIBLE);
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
        //return new DefaultHandleBehavior(new VisibilityAnimationManager.Builder(handle).withPivotX(1f).withPivotY(1f).build());

        return null;
    }

    @Override
    protected ViewBehavior provideBubbleBehavior() {
        //return new DefaultBubbleBehavior(new VisibilityAnimationManager.Builder(bubble).withPivotX(1f).withPivotY(1f).build());
        return null;
    }

}