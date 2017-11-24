package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.os.Handler;
import android.util.Log;

public class DefaultHandleBehavior implements ViewBehavior {

    private final VisibilityAnimationManager animationManager;

    public DefaultHandleBehavior(VisibilityAnimationManager animationManager) {
        this.animationManager = animationManager;
    }
    @Override
    public void onHandleGrabbed() {
        //animationManager.show();

    }

    @Override
    public void onHandleReleased() {
       // animationManager.hide();
//        Log.d("******","**** HANDLE onHandleReleased");
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            public void run() {
//                animationManager.hide();
//            }
//        }, 2000);

    }

    @Override
    public void onScrollStarted() {
        animationManager.show();
    }

    @Override
    public void onScrollFinished() {

//        handler.postDelayed(new Runnable() {
//            public void run() {
//                animationManager.hide();
//            }
//        }, 2000);

     //   animationManager.hide();

    }
}
