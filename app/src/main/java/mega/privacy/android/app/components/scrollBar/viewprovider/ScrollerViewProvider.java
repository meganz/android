package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mega.privacy.android.app.components.scrollBar.FastScroller;



public abstract class ScrollerViewProvider {

    private FastScroller scroller;
    private ViewBehavior handleBehavior;
    private ViewBehavior bubbleBehavior;

    public void setFastScroller(FastScroller scroller){
        this.scroller = scroller;
    }

    protected Context getContext(){
        return scroller.getContext();
    }

    protected FastScroller getScroller() {
        return scroller;
    }

    /**
     * @param container The container {@link FastScroller} for the view to inflate properly.
     * @return A view which will be by the {@link FastScroller} used as a handle.
     */
    public abstract View provideHandleView(ViewGroup container);

    /**
     * @param container The container {@link FastScroller} for the view to inflate properly.
     * @return A view which will be by the {@link FastScroller} used as a bubble.
     */
    public abstract View provideBubbleView(ViewGroup container);

    /**
     * Bubble view has to provide a {@link TextView} that will show the index title.
     * @return A {@link TextView} that will hold the index title.
     */
    public abstract TextView provideBubbleTextView();

    /**
     * To offset the position of the bubble relative to the handle. E.g. in {@link DefaultScrollerViewProvider}
     * the sharp corner of the bubble is aligned with the center of the handle.
     * @return the position of the bubble in relation to the handle (according to the orientation).
     */
    public abstract int getBubbleOffset();

    @Nullable
    protected abstract ViewBehavior provideHandleBehavior();

    @Nullable
    protected abstract ViewBehavior provideBubbleBehavior();

    protected ViewBehavior getHandleBehavior(){
        if(handleBehavior==null)
            handleBehavior = provideHandleBehavior();
        return handleBehavior;
    }

    protected ViewBehavior getBubbleBehavior(){
        if(bubbleBehavior==null)
            bubbleBehavior = provideBubbleBehavior();
        return bubbleBehavior;
    }

    public void onHandleGrabbed(){
        if(getHandleBehavior()!=null)
            getHandleBehavior().onHandleGrabbed();
        if(getBubbleBehavior()!=null)
            getBubbleBehavior().onHandleGrabbed();
    }

    public void onHandleReleased(){
        if(getHandleBehavior()!=null)
            getHandleBehavior().onHandleReleased();
        if(getBubbleBehavior()!=null)
            getBubbleBehavior().onHandleReleased();
    }

    public void onScrollStarted(){
        if(getHandleBehavior()!=null)
            getHandleBehavior().onScrollStarted();
        if(getBubbleBehavior()!=null)
            getBubbleBehavior().onScrollStarted();
    }

    public void onScrollFinished(){
        if(getHandleBehavior()!=null)
            getHandleBehavior().onScrollFinished();
        if(getBubbleBehavior()!=null)
            getBubbleBehavior().onScrollFinished();
    }

}