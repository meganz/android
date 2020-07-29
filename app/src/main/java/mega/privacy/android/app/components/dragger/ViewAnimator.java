package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import androidx.annotation.NonNull;
import android.view.View;

public interface ViewAnimator<D extends DraggableView> {

    boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration,
        Activity activity, int[] screenPosition, View currentView,
        int[] draggableViewLocationOnScreen);

    boolean animateToOrigin(@NonNull final D draggableView, final int duration);

    void update(@NonNull final D draggableView, float percentX, float percentY);

    interface Listener {
        void animationStarted();
        void animationEnd();
    }
}
