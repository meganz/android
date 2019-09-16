package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.ViewPropertyAnimatorUpdateListener;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.utils.LogUtil;
import nz.mega.sdk.MegaApiAndroid;

public class ExitViewAnimator<D extends DraggableView> extends ReturnOriginViewAnimator<D> {

    @Override
    public boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration, final Activity activity, final int[] screenPosition, final View currentView) {
        LogUtil.logDebug("animateExit");
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
                    LogUtil.logDebug("Scale: " + scaleX + " " + scaleY + " dimensions: " + touchImageView.getImageWidth() + " " + touchImageView.getImageHeight() + " position: " + screenPosition[0] + " " + screenPosition[1]);
                }
                else if (currentView instanceof ImageView){
                    imageView = (ImageView) currentView;
                    scaleX = ((float)screenPosition[2]) / imageView.getDrawable().getIntrinsicWidth();
                    scaleY = ((float)screenPosition[3]) / imageView.getDrawable().getIntrinsicHeight();
                    LogUtil.logDebug("Scale: "+scaleX+" "+scaleY+" dimensions: "+imageView.getWidth()+" "+imageView.getHeight()+ " position: "+screenPosition[0]+" "+screenPosition[1]);
                }
                else {
                    surfaceView = (SurfaceView) currentView;
                    scaleX = ((float) screenPosition[2]) / ((float) surfaceView.getWidth());
                    scaleY = ((float) screenPosition[3]) / ((float) surfaceView.getHeight());
                    LogUtil.logDebug("Scale: " + scaleX + " " + scaleY + " dimensions: " + surfaceView.getWidth() + " " + surfaceView.getHeight() + " position: " + screenPosition[2] + " " + screenPosition[3]);
                }

                ViewCompat.animate(draggableView)
                        .withLayer()
                        .translationX(screenPosition[0] - (draggableView.getWidth() / 2))
                        .translationY(screenPosition[1] - (draggableView.getHeight() / 2))
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
