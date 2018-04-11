package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.ViewPropertyAnimatorUpdateListener;
import android.view.SurfaceView;
import android.view.View;

import mega.privacy.android.app.components.TouchImageView;
import mega.privacy.android.app.lollipop.managerSections.FileBrowserFragmentLollipop;
import mega.privacy.android.app.utils.Util;

public class ExitViewAnimator<D extends DraggableView> extends ReturnOriginViewAnimator<D> {

    @Override
    public boolean animateExit(@NonNull final D draggableView, final Direction direction, int duration, final Activity activity, final int[] screenPosition, View currentView) {
        log("animateExit");
        draggableView.setAnimating(true);

        float scaleX;
        float scaleY;
        TouchImageView touchImageView = null;
        SurfaceView surfaceView;

        if (currentView instanceof TouchImageView){
            touchImageView = (TouchImageView) currentView;
            scaleX = ((float)screenPosition[2]) / touchImageView.getImageWidth();
            scaleY = ((float)screenPosition[3]) / touchImageView.getImageHeight();
            log("Scale: "+scaleX+" "+scaleY+" dimensions: "+touchImageView.getImageWidth()+" "+touchImageView.getImageHeight()+ " position: "+screenPosition[0]+" "+screenPosition[1]);
        }
        else {
            surfaceView = (SurfaceView) currentView;
            scaleX = ((float)screenPosition[2]) / ((float)surfaceView.getWidth());
            scaleY = ((float)screenPosition[3]) / ((float)surfaceView.getHeight());
            log("Scale: "+scaleX+" "+scaleY+" dimensions: "+surfaceView.getWidth()+" "+surfaceView.getHeight()+ " position: "+screenPosition[2]+" "+screenPosition[3]);
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
                            FileBrowserFragmentLollipop.imageDrag.setVisibility(View.VISIBLE);
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
                            FileBrowserFragmentLollipop.imageDrag.setVisibility(View.VISIBLE);
                        }
                    });
        }


        return true;
    }

    public static void log(String message) {
        Util.log("DraggableView: ExitViewAnimator", message);
    }
}
