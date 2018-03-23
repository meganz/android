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
    public boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration, final Activity activity, final int[] screenPosition, boolean portrait) {
        log("animateExit");
        draggableView.setAnimating(true);

        float scaleX;
        float scaleY;
        if (portrait){
            scaleX = screenPosition[2] / draggableView.getParentWidth();
            scaleY = screenPosition[3] / (draggableView.getParentHeight());//300
        }
        else {
            scaleX = screenPosition[2] / (draggableView.getParentWidth());
            scaleY = screenPosition[3] / (draggableView.getParentHeight());
        }
        if (screenPosition != null){
            ViewCompat.animate(draggableView)
                    .withLayer()
                    .translationX(screenPosition[0]-(draggableView.getWidth()/2))
                    .translationY(screenPosition[1]-(draggableView.getHeight()/2))
                    .scaleX(scaleX)
                    .scaleY(scaleY)
                    .rotation(0f)

                    .setUpdateListener(new ViewPropertyAnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(View view) {
                            notifyDraggableViewUpdated(draggableView);
                        }
                    })

                    .setDuration(ANIMATION_RETURN_TO_ORIGIN_DURATION)

                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
                            if (dragListener != null) {
                                dragListener.onDragCancelled(draggableView);
                                dragListener.onDrag(draggableView, screenPosition[0], screenPosition[1]);
                            }

                            draggableView.setAnimating(false);
                            activity.finish();
                            activity.overridePendingTransition(0, android.R.anim.fade_out);
                        }
                    });
        }
        else {
            ViewCompat.animate(draggableView)
                    .withLayer()
                    .scaleX(scaleX)
                    .scaleY(scaleY)
                    .rotation(0f)

                    .setUpdateListener(new ViewPropertyAnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(View view) {
                            notifyDraggableViewUpdated(draggableView);
                        }
                    })

                    .setDuration(ANIMATION_RETURN_TO_ORIGIN_DURATION)

                    .setListener(new ViewPropertyAnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(View view) {
                            DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
                            if (dragListener != null) {
                                dragListener.onDragCancelled(draggableView);
                                dragListener.onDrag(draggableView, screenPosition[0], screenPosition[1]);
                            }

                            draggableView.setAnimating(false);
                            activity.finish();
                            activity.overridePendingTransition(0, android.R.anim.fade_out);
                        }
                    });
        }


        return true;
    }

    public static void log(String message) {
        Util.log("DraggableView: ExitViewAnimator", message);
    }
}
