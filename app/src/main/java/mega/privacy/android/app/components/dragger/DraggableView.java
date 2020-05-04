package mega.privacy.android.app.components.dragger;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatFullScreenImageViewer;

import static mega.privacy.android.app.utils.LogUtil.*;

public class DraggableView extends FrameLayout{

    boolean animate = false;
    int[] screenPosition;
    View currentView;
    public float normalizedScale;

    public static final int DEFAULT_EXIT_DURATION = 150;

    public float motionXOrigin;
    public float motionYOrigin;
    public float parentWidth;
    public float parentHeight;
    protected float oldPercentX = 0;
    protected float oldPercentY = 0;
    float maxDragPercentageY = 0.5f;
    float maxDragPercentageX = 0.75f;
    boolean listenVelocity = true;
    boolean draggable = true;
    boolean inlineMove = false;
    boolean vertical = true;
    boolean rotationEnabled = false;
    float rotationValue;
    boolean animating;
    float minVelocity;
    int exitDirection = DEFAULT_EXIT_DURATION;
    int returnOriginDuration = ReturnOriginViewAnimator.ANIMATION_RETURN_TO_ORIGIN_DURATION;
    DraggableViewListener dragListener;
    DraggableListener draggableListener;
    GestureDetectorCompat detector;
    @Nullable
    ViewAnimator<DraggableView> viewAnimator;

    //ReturnOriginViewAnimator will reset the view to this positions
    float originalViewX = 0;
    float originalViewY = 0;
    float touchInterceptSensibility = 100;

    public DraggableView(Context context) {
        this(context, null);
    }

    public DraggableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @Nullable
    public ViewAnimator<DraggableView> getViewAnimator() {
        return viewAnimator;
    }

    public void setViewAnimator(@Nullable ViewAnimator<DraggableView> viewAnimator) {
        this.viewAnimator = viewAnimator;
    }

    public void setNormalizedScale (float scale){
        normalizedScale = scale;
    }

    public void animateToOrigin(int duration) {
        if (viewAnimator != null) {
            viewAnimator.animateToOrigin(this, duration);
        }
    }

    public void setDraggableListener (DraggableListener draggableListener){
        this.draggableListener = draggableListener;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public boolean isListenVelocity() {
        return listenVelocity;
    }

    public void setListenVelocity(boolean listenVelocity) {
        this.listenVelocity = listenVelocity;
    }

    public DraggableViewListener getDragListener() {
        return dragListener;
    }

    public void setDragListener(DraggableViewListener dragListener) {
        this.dragListener = dragListener;
    }

    public boolean isRotationEnabled() {
        return rotationEnabled;
    }

    public void setRotationEnabled(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }

    public float getRotationValue() {
        return rotationValue;
    }

    public void setRotationValue(float rotationValue) {
        this.rotationValue = rotationValue;
    }

    public boolean isAnimating() {
        return animating;
    }

    public void setAnimating(boolean animating) {
        this.animating = animating;
    }

    public boolean isInlineMove() {
        return inlineMove;
    }

    public void setInlineMove(boolean inlineMove) {
        this.inlineMove = inlineMove;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public int getExitDirection() {
        return exitDirection;
    }

    public void setExitDirection(int exitDirection) {
        this.exitDirection = exitDirection;
    }

    public int getReturnOriginDuration() {
        return returnOriginDuration;
    }

    public void setReturnOriginDuration(int returnOriginDuration) {
        this.returnOriginDuration = returnOriginDuration;
    }

    //translationX == 0 => 0
    //-parentWidth/2 => -1
    //parentWidth/2 => -1
    public float getPercentX() {
        float percent = 2f * (ViewCompat.getTranslationX(this) - originalViewX) / getParentWidth();
        if (percent > 1) {
            percent = 1;
        }
        if (percent < -1) {
            percent = -1;
        }
        return percent;
    }

    //translationY == 0 => 0
    //-parentHeight/2 => -1
    //parentHeight/2 => -1
    public float getPercentY() {
        float percent = 2f * (ViewCompat.getTranslationY(this) - originalViewY) / getParentHeight();
        if (percent > 1) {
            percent = 1;
        }
        if (percent < -1) {
            percent = -1;
        }
        return percent;
    }

    public float getMaxDragPercentageY() {
        return maxDragPercentageY;
    }

    public void setMaxDragPercentageY(float maxDragPercentageY) {
        this.maxDragPercentageY = maxDragPercentageY;
    }

    public float getMaxDragPercentageX() {
        return maxDragPercentageX;
    }

    public void setMaxDragPercentageX(float maxDragPercentageX) {
        this.maxDragPercentageX = maxDragPercentageX;
    }

    public float getTouchInterceptSensibility() {
        return touchInterceptSensibility;
    }

    public void setTouchInterceptSensibility(float touchInterceptSensibility) {
        this.touchInterceptSensibility = touchInterceptSensibility;
    }

    public int[] getScreenPosition() {
        return screenPosition;
    }

    public void setScreenPosition(int[] screenPosition) {
        if (screenPosition != null){
            this.screenPosition = screenPosition;
        }
    }

    public void setCurrentView (View v) {
        currentView = v;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        logDebug("onInterceptTouchEvent");
        final int action = MotionEventCompat.getActionMasked(event);

        if (getContext() instanceof FullScreenImageViewerLollipop){
            if (event.getPointerCount() > 1 || normalizedScale != 1){
                setDraggable(false);
                return false;
            }
        }
        else if (getContext() instanceof ChatFullScreenImageViewer) {
            if (event.getPointerCount() > 1 || normalizedScale != 1){
                setDraggable(false);
                return false;
            }
        }

        if (draggable){
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    motionXOrigin = event.getRawX();
                    motionYOrigin = event.getRawY();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    actionUp();
                    break;
                case MotionEvent.ACTION_MOVE: {
                    float newMotionX = event.getRawX();
                    float newMotionY = event.getRawY();
                    return (Math.abs(motionYOrigin - newMotionY) > touchInterceptSensibility);
                }
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return handleTouch(event);
    }

    public void update() {
        logDebug("update()");
        float percentX = getPercentX();
        float percentY = getPercentY();
        update(percentX, percentY);
    }

    public void update(float percentX, float percentY) {
        logDebug("update");
        if (rotationEnabled) {
            ViewCompat.setRotation(this, percentX * rotationValue);
        }

        if (dragListener != null) {
            dragListener.onDrag(this, percentX, percentY);
        }

        if (viewAnimator != null) {
            viewAnimator.update(this, percentX, percentY);
        }
        oldPercentX = percentX;
        oldPercentY = percentY;
    }

    public float getMinVelocity() {
        return minVelocity;
    }

    public void setMinVelocity(float minVelocity) {
        this.minVelocity = minVelocity;
    }

    public void reset() {
    }

    public float getOldPercentX() {
        return oldPercentX;
    }

    public float getOldPercentY() {
        return oldPercentY;
    }

    public float getOriginalViewX() {
        return originalViewX;
    }

    public void setOriginalViewX(float originalViewX) {
        this.originalViewX = originalViewX;
    }

    public float getOriginalViewY() {
        return originalViewY;
    }

    public void setOriginalViewY(float originalViewY) {
        this.originalViewY = originalViewY;
    }

    public void initOriginalViewPositions() {
        this.originalViewX = ViewCompat.getTranslationX(this);
        this.originalViewY = ViewCompat.getTranslationY(this);
    }

    public boolean animateExit(Direction direction) {
        logDebug("animateExit");
        boolean animateExit = false;
        if (viewAnimator != null) {
            if (direction == Direction.NONE) {
                animateToOrigin(returnOriginDuration);
                ViewCompat.animate(this).scaleX(1f).setDuration(200);
                ViewCompat.animate(this).scaleY(1f).setDuration(200);
                animate = false;
                if (draggableListener != null){
                    draggableListener.onDragActivated(animate);
                }

            } else {
                Activity activity = (Activity) getContext();
                animateExit = viewAnimator.animateExit(DraggableView.this, direction, exitDirection, activity, screenPosition, currentView);

                if (draggableListener != null){
                    draggableListener.onDragActivated(true);
                }

            }
        }
        if (animateExit) {
            if (dragListener != null) {
                dragListener.onDraggedStarted(this, direction);
            }
        }

        return animateExit;
    }

    boolean handleTouch(MotionEvent event) {
        logDebug("handleTouch");
        if (draggable && !animating) {
            boolean handledByDetector = this.detector.onTouchEvent(event);
            if (!handledByDetector) {

                final int action = MotionEventCompat.getActionMasked(event);
                switch (action) {
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        actionUp();
                        animate  = false;
//                        draggableListener.onDragActivated(animate);
                        break;
                    case MotionEvent.ACTION_MOVE: {
                        if (!animate){
                            animate = true;
                            draggableListener.onDragActivated(animate);
                            ViewCompat.animate(this).scaleX(0.5f).setDuration(200);
                            ViewCompat.animate(this).scaleY(0.5f).setDuration(200);
                        }
                        float newMotionX = event.getRawX();
                        float newMotionY = event.getRawY();

                        float diffMotionX = newMotionX - motionXOrigin;
                        float diffMotionY = newMotionY - motionYOrigin;

                        if (draggableListener != null){
                            draggableListener.onViewPositionChanged(Math.abs(getPercentY()));
                        }
                        if (vertical) {
                            if (!inlineMove) {
                                ViewCompat.setTranslationX(this, originalViewX + diffMotionX);
                            }
                            ViewCompat.setTranslationY(this, originalViewY + diffMotionY);
                        } else {
                            if (!inlineMove) {
                                ViewCompat.setTranslationY(this, originalViewY + diffMotionY);
                            }
                            ViewCompat.setTranslationX(this, originalViewX + diffMotionX);
                        }

                        update();
                    }
                    break;
                }
            }

        }
        return true;
    }

    void actionUp() {
        logDebug("actionUp");
        float percentX = getPercentX();
        float percentY = getPercentY();

        if (viewAnimator != null) {
            boolean animated =
                (!vertical && percentX > maxDragPercentageX && animateExit(Direction.RIGHT)) ||
                    (!vertical && percentX < -maxDragPercentageX && animateExit(Direction.LEFT)) ||
                    (vertical && percentY > maxDragPercentageY && animateExit(Direction.BOTTOM)) ||
                    (vertical && percentY < -maxDragPercentageY && animateExit(Direction.TOP));
            if (!animated) {
                animateExit(Direction.NONE);
            }
        }
    }

    float getParentWidth() {
        logDebug("getParentWidth");
        if (parentWidth == 0) {
            parentWidth = ((View) getParent()).getWidth();
        }
        return parentWidth;
    }

    float getParentHeight() {
        logDebug("getParentHeight");
        if (parentHeight == 0) {
            parentHeight = ((View) getParent()).getHeight();
        }
        return parentHeight;
    }

    private void initialize(Context context) {
        detector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(@Nullable MotionEvent event1, @Nullable MotionEvent event2, float velocityX, float velocityY) {
                logDebug("onFling");
                boolean animated = false;
                if (listenVelocity && !animating && viewAnimator != null && event1 != null && event2 != null) {
                    if (vertical) {
                        if (Math.abs(velocityY) > minVelocity) {
                            float distanceY = event1.getRawY() - event2.getRawY();
                            if (distanceY < 0) {
                                animated = animateExit(Direction.TOP);
                            } else {
                                animated = animateExit(Direction.BOTTOM);
                            }
                        }
                    } else {
                        if (Math.abs(velocityX) > minVelocity) {
                            float distanceX = event1.getRawX() - event2.getRawX();
                            if (distanceX < 0) {
                                animated = animateExit(Direction.RIGHT);
                            } else {
                                animated = animateExit(Direction.LEFT);
                            }
                        }
                    }
                }
                return animated;
            }


            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (getContext() instanceof FullScreenImageViewerLollipop){
                    if (normalizedScale != 1){
                        setDraggable(false);
                    }
                }
                else if (getContext() instanceof ChatFullScreenImageViewer) {
                    if (normalizedScale != 1) {
                        setDraggable(false);
                    }
                }

                return false;
            }
        });

        this.viewAnimator = new ReturnOriginViewAnimator<DraggableView>() {
        };
    }

    public interface DraggableViewListener {
        void onDrag(DraggableView draggableView, float percentX, float percentY);

        void onDraggedStarted(DraggableView draggableView, Direction direction);

        void onDraggedEnded(DraggableView draggableView, Direction direction);

        void onDragCancelled(DraggableView draggableView);
    }

    public static abstract class DraggableViewListenerAdapter implements DraggableViewListener {
        @Override
        public void onDrag(DraggableView draggableView, float percentX, float percentY) {
        }

        @Override
        public void onDraggedStarted(DraggableView draggableView, Direction direction) {
        }

        @Override
        public void onDraggedEnded(DraggableView draggableView, Direction direction) {
        }

        @Override
        public void onDragCancelled(DraggableView draggableView) {
        }
    }

    public interface DraggableListener {

        void onViewPositionChanged(float fractionScreen);

        void onDragActivated (boolean activated);
    }

}
