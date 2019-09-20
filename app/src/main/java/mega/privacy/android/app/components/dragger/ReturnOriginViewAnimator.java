package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.ViewPropertyAnimatorUpdateListener;
import android.view.View;

import static mega.privacy.android.app.utils.LogUtil.*;

public abstract class ReturnOriginViewAnimator<D extends DraggableView> implements ViewAnimator<D> {

    public static final int ANIMATION_RETURN_TO_ORIGIN_DURATION = 500;

    public void notifyDraggableViewUpdated(@NonNull final D draggableView) {
        DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
        if (dragListener != null) {
            draggableView.update();
            dragListener.onDrag(draggableView, draggableView.getPercentX(), draggableView.getPercentY());
        }
    }

    @Override
    public boolean animateToOrigin(@NonNull final D draggableView, final int duration) {
        logDebug("animateToOrigin");
        draggableView.setAnimating(true);

        ViewCompat.animate(draggableView)
            .withLayer()
            .translationX(draggableView.getOriginalViewX())
            .translationY(draggableView.getOriginalViewY())
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
                        dragListener.onDrag(draggableView, 0, 0);
                    }
                    draggableView.setAnimating(false);
                }
            });

        return true;
    }

    @Override
    public boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration, Activity activity, int[] screenPosition, View currentView) {
        logDebug("animateExit");
        return false;
    }

    @Override
    public void update(D draggableView, float percentX, float percentY) {
        logDebug("update");
    }
}
