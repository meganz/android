package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.graphics.RectF;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorListenerAdapter;
import androidx.core.view.ViewPropertyAnimatorUpdateListener;

import com.facebook.drawee.view.SimpleDraweeView;

import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;

import static mega.privacy.android.app.utils.Constants.LOCATION_INDEX_LEFT;
import static mega.privacy.android.app.utils.Constants.LOCATION_INDEX_TOP;
import static mega.privacy.android.app.utils.LogUtil.logDebug;

public class ExitViewAnimator<D extends DraggableView> extends ReturnOriginViewAnimator<D> {

    @Override
    public boolean animateExit(@NonNull final D draggableView, final Direction direction,
        int duration, final Activity activity, final int[] screenPosition, final View currentView,
        final int[] draggableViewLocationOnScreen) {
        logDebug("animateExit");
        draggableView.setAnimating(true);

        float scaleX;
        float scaleY;
        TouchImageView touchImageView = null;
        ImageView imageView;
        SurfaceView surfaceView;

        if (currentView != null) {
            if (screenPosition != null) {
                if (currentView instanceof TouchImageView) {
                    touchImageView = (TouchImageView) currentView;
                    scaleX = ((float) screenPosition[2]) / touchImageView.getImageWidth();
                    scaleY = ((float) screenPosition[3]) / touchImageView.getImageHeight();
                    logDebug("Scale: " + scaleX + " " + scaleY + " dimensions: " + touchImageView.getImageWidth() + " " + touchImageView.getImageHeight() + " position: " + screenPosition[0] + " " + screenPosition[1]);
                }
                else if (currentView instanceof SimpleDraweeView){
                    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) currentView;
                    RectF bounds = new RectF();
                    simpleDraweeView.getHierarchy().getActualImageBounds(bounds);
                    scaleX = ((float)screenPosition[2]) / (bounds.right - bounds.left);
                    scaleY = ((float)screenPosition[3]) / (bounds.bottom - bounds.top);
                    logDebug("SimpleDraweeView scale: " + scaleX + " " + scaleY);
                }
                else if (currentView instanceof ImageView){
                    imageView = (ImageView) currentView;
                    scaleX = ((float)screenPosition[2]) / imageView.getDrawable().getIntrinsicWidth();
                    scaleY = ((float)screenPosition[3]) / imageView.getDrawable().getIntrinsicHeight();
                    logDebug("Scale: "+scaleX+" "+scaleY+" dimensions: "+imageView.getWidth()+" "+imageView.getHeight()+ " position: "+screenPosition[0]+" "+screenPosition[1]);
                }
                else {
                    surfaceView = (SurfaceView) currentView;
                    scaleX = ((float) screenPosition[2]) / ((float) surfaceView.getWidth());
                    scaleY = ((float) screenPosition[3]) / ((float) surfaceView.getHeight());
                    logDebug("Scale: " + scaleX + " " + scaleY + " dimensions: " + surfaceView.getWidth() + " " + surfaceView.getHeight() + " position: " + screenPosition[2] + " " + screenPosition[3]);
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
                                if (activity instanceof FullScreenImageViewerLollipop) {
                                    ((FullScreenImageViewerLollipop) activity).setImageDragVisibility(View.VISIBLE);
                                } else if (activity instanceof AudioVideoPlayerLollipop){
                                    ((AudioVideoPlayerLollipop) activity).setImageDragVisibility(View.VISIBLE);
                                }

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
                                if (activity instanceof FullScreenImageViewerLollipop) {
                                    ((FullScreenImageViewerLollipop) activity).setImageDragVisibility(View.VISIBLE);
                                }
                                else if (activity instanceof AudioVideoPlayerLollipop){
                                    ((AudioVideoPlayerLollipop) activity).setImageDragVisibility(View.VISIBLE);
                                }

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
        }
        else {
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
                            if (activity instanceof FullScreenImageViewerLollipop) {
                                ((FullScreenImageViewerLollipop) activity).setImageDragVisibility(View.VISIBLE);
                            } else if (activity instanceof AudioVideoPlayerLollipop) {
                                ((AudioVideoPlayerLollipop) activity).setImageDragVisibility(View.VISIBLE);
                            }

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
}
