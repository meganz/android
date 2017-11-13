package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.os.Handler;

public class DefaultHandleBehavior implements ViewBehavior {

    private final VisibilityAnimationManager animationManager;

    public DefaultHandleBehavior(VisibilityAnimationManager animationManager) {
        this.animationManager = animationManager;
    }
    @Override
    public void onHandleGrabbed() {}

    @Override
    public void onHandleReleased() {}

    @Override
    public void onScrollStarted() {
        animationManager.show();
    }

    @Override
    public void onScrollFinished() {
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                animationManager.hide();
//            }
//        }, 3000);   //3 seconds

        animationManager.hide();

    }
}
