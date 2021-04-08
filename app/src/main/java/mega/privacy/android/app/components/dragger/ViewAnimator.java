package mega.privacy.android.app.components.dragger;

import android.view.View;

import androidx.annotation.NonNull;

public interface ViewAnimator<D extends DraggableView> {

    boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration,
                        Listener listener, int[] screenPosition, View currentView,
                        int[] draggableViewLocationOnScreen);

    boolean animateToOrigin(@NonNull final D draggableView, final int duration);

    void update(@NonNull final D draggableView, float percentX, float percentY);

    interface Listener {
        void showPreviousHiddenThumbnail();

        void fadeOutFinish();
    }
}
