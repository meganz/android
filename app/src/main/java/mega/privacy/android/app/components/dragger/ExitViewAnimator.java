package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.ViewPropertyAnimatorUpdateListener;
import android.view.View;

import java.util.concurrent.atomic.AtomicBoolean;

import mega.privacy.android.app.utils.Util;

public class ExitViewAnimator<D extends DraggableView> extends ReturnOriginViewAnimator<D> {

    @Override
    public boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration, final Activity activity, int[] screenPosition) {
        log("animateExit");
        draggableView.setDraggable(false);
        draggableView.setAnimating(true);

//        int translation = 0;
//        switch (direction) {
//            case LEFT:
//                translation = (int) -(draggableView.getParentWidth());
//                break;
//            case RIGHT:
//                translation = (int) (draggableView.getParentWidth());
//                break;
//            case TOP:
//                translation = (int) -draggableView.getHeight() * 3;
//                break;
//            case BOTTOM:
//                translation = (int) draggableView.getHeight() * 3;
//                break;
//        }
//
        ViewPropertyAnimatorCompat animator = null;
//
//        switch (direction) {
//            case LEFT:
//            case RIGHT:
//                animator = ViewCompat.animate(draggableView).withLayer().translationX(translation);
//                break;
//            case TOP:
//            case BOTTOM:
//                animator = ViewCompat.animate(draggableView).withLayer().translationY(translation);
//                break;
//        }
        animator = ViewCompat.animate(draggableView).withLayer().translationY(screenPosition[0]).translationX(screenPosition[1]).scaleX(0.1f).scaleY(0.1f);
        log("screenPosition: "+screenPosition[0]+" "+screenPosition[1]);
        final AtomicBoolean willUpdate = new AtomicBoolean(true);

        animator
            .setDuration(500)
            .setUpdateListener(new ViewPropertyAnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(View view) {
                    if(willUpdate.get()) {
                        notifyDraggableViewUpdated(draggableView);
                    }
                }
            })
            .setListener(new ViewPropertyAnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(View view) {
                    willUpdate.set(false);

//                    DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
//                    if (dragListener != null) {
//                        dragListener.onDraggedEnded(draggableView, direction);
//                    }

                    draggableView.setAnimating(false);

                    activity.finish();
                    activity.overridePendingTransition(0, android.R.anim.fade_out);
                }
            });


//        activity.finish();

        return true;
    }

    public static void log(String message) {
        Util.log("DraggableView: ExitViewAnimator", message);
    }
}
