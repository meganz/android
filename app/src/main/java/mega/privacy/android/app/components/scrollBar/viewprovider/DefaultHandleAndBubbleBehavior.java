package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.os.Handler;
import android.util.Log;

public class DefaultHandleAndBubbleBehavior implements ViewBehavior {

    private final VisibilityAnimationManager handle, bubble;
    private boolean var1 = false;
    private boolean var2 = false;

    public DefaultHandleAndBubbleBehavior(VisibilityAnimationManager animationManagerHandle, VisibilityAnimationManager animationManagerBubble) {
        this.handle = animationManagerHandle;
        this.bubble = animationManagerBubble;
    }

    @Override
    public void onScrollStarted() {
        handle.show();
        var1=false;
        var2=true;
    }

    @Override
    public void onScrollFinished() {
        if(!var1){
            handle.hide();
        }
    }

    @Override
    public void onHandleGrabbed() {
        handle.show();
        bubble.show();
        var1 = true;
        var2=false;
    }

    @Override
    public void onHandleReleased() {
        bubble.hide();
        if(!var2){
            handle.hide();
        }
    }
}