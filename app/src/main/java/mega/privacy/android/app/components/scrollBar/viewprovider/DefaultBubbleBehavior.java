package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.os.Handler;
import android.util.Log;

public class DefaultBubbleBehavior implements ViewBehavior {

    private final VisibilityAnimationManager animationManager;

    public DefaultBubbleBehavior(VisibilityAnimationManager animationManager) {
        this.animationManager = animationManager;
    }

    @Override
    public void onHandleGrabbed() {
        animationManager.show();
    }

    @Override
    public void onHandleReleased() {
        animationManager.hide();
    }

    @Override
    public void onScrollStarted() {

    }

    @Override
    public void onScrollFinished() {
    }

}