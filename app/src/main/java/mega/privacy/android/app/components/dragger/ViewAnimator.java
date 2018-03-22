package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.support.annotation.NonNull;

public interface ViewAnimator<D extends DraggableView> {

    boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration, Activity activity, int[] screenPosition);

    boolean animateToOrigin(@NonNull final D draggableView, final int duration);

    void update(@NonNull final D draggableView, float percentX, float percentY);

    interface Listener {
        void animationStarted();
        void animationEnd();
    }
}
