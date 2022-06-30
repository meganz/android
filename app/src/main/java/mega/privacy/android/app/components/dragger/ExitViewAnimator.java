package mega.privacy.android.app.components.dragger;

import static mega.privacy.android.app.utils.Constants.LOCATION_INDEX_LEFT;
import static mega.privacy.android.app.utils.Constants.LOCATION_INDEX_TOP;

import android.graphics.RectF;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListenerAdapter;
import androidx.core.view.ViewPropertyAnimatorUpdateListener;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.samples.zoomable.ZoomableDraweeView;

import timber.log.Timber;

public class ExitViewAnimator<D extends DraggableView> extends ReturnOriginViewAnimator<D> {

    @Override
    public boolean animateExit(@NonNull final D draggableView, final Direction direction,
                               int duration, final Listener listener, final int[] screenPosition, final View currentView,
                               final int[] draggableViewLocationOnScreen) {
        Timber.d("animateExit");
        draggableView.setAnimating(true);

        float scaleX;
        float scaleY;
        ImageView imageView;
        SurfaceView surfaceView;

        if (currentView != null) {
            if (screenPosition != null) {
                if (currentView instanceof ZoomableDraweeView) {
                    ZoomableDraweeView zoomableDraweeView = (ZoomableDraweeView) currentView;
                    RectF bounds = new RectF();
                    zoomableDraweeView.getHierarchy().getActualImageBounds(bounds);
                    scaleX = ((float) screenPosition[2]) / (bounds.right - bounds.left);
                    scaleY = ((float) screenPosition[3]) / (bounds.bottom - bounds.top);
                    Timber.d("ZoomableDraweeView scale: %s %s", scaleX, scaleY);
                } else if (currentView instanceof SimpleDraweeView) {
                    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) currentView;
                    RectF bounds = new RectF();
                    simpleDraweeView.getHierarchy().getActualImageBounds(bounds);
                    scaleX = ((float) screenPosition[2]) / (bounds.right - bounds.left);
                    scaleY = ((float) screenPosition[3]) / (bounds.bottom - bounds.top);
                    Timber.d("SimpleDraweeView scale: %s %s", scaleX, scaleY);
                } else if (currentView instanceof ImageView) {
                    imageView = (ImageView) currentView;
                    scaleX = ((float) screenPosition[2]) / imageView.getDrawable().getIntrinsicWidth();
                    scaleY = ((float) screenPosition[3]) / imageView.getDrawable().getIntrinsicHeight();
                    Timber.d("Scale: %s %s dimensions: %d %d position: %d %d", scaleX, scaleY, imageView.getWidth(), imageView.getHeight(), screenPosition[0], screenPosition[1]);
                } else {
                    surfaceView = (SurfaceView) currentView;
                    scaleX = ((float) screenPosition[2]) / ((float) surfaceView.getWidth());
                    scaleY = ((float) screenPosition[3]) / ((float) surfaceView.getHeight());
                    Timber.d("Scale: %s %s dimensions: %d %d position: %d %d", scaleX, scaleY, surfaceView.getWidth(), surfaceView.getHeight(), screenPosition[0], screenPosition[1]);
                }

                ViewCompat.animate(draggableView)
                        .withLayer()
                        .translationX(screenPosition[LOCATION_INDEX_LEFT]
                                - (draggableViewLocationOnScreen[LOCATION_INDEX_LEFT] + draggableView.getWidth() / 2))
                        .translationY(screenPosition[LOCATION_INDEX_TOP]
                                - (draggableViewLocationOnScreen[LOCATION_INDEX_TOP] + draggableView.getHeight() / 2))
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
                                currentView.setVisibility(View.GONE);
                                listener.showPreviousHiddenThumbnail();

                                DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
                                if (dragListener != null) {
                                    dragListener.onDraggedEnded(draggableView, direction);
                                    dragListener.onDrag(draggableView, screenPosition[0], screenPosition[1]);
                                }

                                draggableView.setAnimating(false);
                                listener.fadeOutFinish();

                                ViewCompat.animate(draggableView).setListener(null);
                            }
                        });
            } else {
                ViewCompat.animate(draggableView)
                        .withLayer()
                        .translationX(0.5f)
                        .translationY(0.5f)
                        .scaleX(0)
                        .scaleY(0)
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
                                currentView.setVisibility(View.GONE);
                                listener.showPreviousHiddenThumbnail();

                                DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
                                if (dragListener != null && screenPosition != null) {
                                    dragListener.onDraggedEnded(draggableView, direction);
                                    dragListener.onDrag(draggableView, screenPosition[0], screenPosition[1]);
                                }

                                draggableView.setAnimating(false);
                                listener.fadeOutFinish();

                                ViewCompat.animate(draggableView).setListener(null);
                            }
                        });
            }
        } else {
            ViewCompat.animate(draggableView)
                    .withLayer()
                    .translationX(0.5f)
                    .translationY(0.5f)
                    .scaleX(0)
                    .scaleY(0)
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
                            listener.showPreviousHiddenThumbnail();

                            DraggableView.DraggableViewListener dragListener = draggableView.getDragListener();
                            if (dragListener != null && screenPosition != null) {
                                dragListener.onDraggedEnded(draggableView, direction);
                                dragListener.onDrag(draggableView, screenPosition[0], screenPosition[1]);
                            }

                            draggableView.setAnimating(false);
                            listener.fadeOutFinish();

                            ViewCompat.animate(draggableView).setListener(null);
                        }
                    });
        }
        return true;
    }
}
