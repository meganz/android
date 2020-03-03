package mega.privacy.android.app.components.scrollBar.viewprovider;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import androidx.annotation.AnimatorRes;
import android.view.View;

import mega.privacy.android.app.R;


public class VisibilityAnimationManager {

        protected final View view;
        protected AnimatorSet hideAnimator;
        protected AnimatorSet showAnimator;

        private float pivotXRelative;
        private float pivotYRelative;

        protected VisibilityAnimationManager(final View view, @AnimatorRes int showAnimator, @AnimatorRes int hideAnimator, float pivotXRelative, float pivotYRelative, int hideDelay){
            this.view = view;
            this.pivotXRelative = pivotXRelative;
            this.pivotYRelative = pivotYRelative;
            this.hideAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(view.getContext(), hideAnimator);
            this.hideAnimator.setStartDelay(hideDelay);
            this.hideAnimator.setTarget(view);
            this.showAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(view.getContext(), showAnimator);
            this.showAnimator.setTarget(view);
            this.hideAnimator.addListener(new AnimatorListenerAdapter() {

                //because onAnimationEnd() goes off even for canceled animations
                boolean wasCanceled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if(!wasCanceled) view.setVisibility(View.INVISIBLE);
                    wasCanceled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    wasCanceled = true;
                }
            });

            updatePivot();
        }

        public void show(){
            hideAnimator.cancel();
            if (view.getVisibility() == View.INVISIBLE) {
                view.setVisibility(View.VISIBLE);
                updatePivot();
                showAnimator.start();
            }
        }

        public void hide(){
            updatePivot();
            hideAnimator.start();
        }

        protected void updatePivot() {
            view.setPivotX(pivotXRelative*view.getMeasuredWidth());
            view.setPivotY(pivotYRelative*view.getMeasuredHeight());
        }

public static abstract class AbsBuilder<T extends VisibilityAnimationManager> {
    protected final View view;
    protected int showAnimatorResource = R.animator.fastscroll__default_show;
    protected int hideAnimatorResource = R.animator.fastscroll__default_hide;
    protected int hideDelay = 1000;
    protected float pivotX = 0.5f;
    protected float pivotY = 0.5f;

    public AbsBuilder(View view) {
        this.view = view;
    }

    public AbsBuilder<T> withShowAnimator(@AnimatorRes int showAnimatorResource){
        this.showAnimatorResource = showAnimatorResource;
        return this;
    }

    public AbsBuilder<T> withHideAnimator(@AnimatorRes int hideAnimatorResource){
        this.hideAnimatorResource = hideAnimatorResource;
        return this;
    }

    public AbsBuilder<T> withHideDelay(int hideDelay){
        this.hideDelay = hideDelay;
        return this;
    }

    public AbsBuilder<T> withPivotX(float pivotX){
        this.pivotX = pivotX;
        return this;
    }

    public AbsBuilder<T> withPivotY(float pivotY){
        this.pivotY = pivotY;
        return this;
    }

    public abstract T build();
}

public static class Builder extends AbsBuilder<VisibilityAnimationManager> {

    public Builder(View view) {
        super(view);
    }

    public VisibilityAnimationManager build(){
        return new VisibilityAnimationManager(view, showAnimatorResource, hideAnimatorResource, pivotX, pivotY, hideDelay);
    }

}

}
