package mega.privacy.android.app.components.voiceClip;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import mega.privacy.android.app.R;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static mega.privacy.android.app.utils.LogUtil.*;

public class AnimationHelper {
    private final static int DURATION_BLINK_MICRO = 500;
    private final static int DURATION_ALPHA = 250;
    private final static int MOVE_BASKET = 90;
    private Context context;
    private AnimatedVectorDrawableCompat animatedVectorDrawable;
    private ImageView basketImg, smallBlinkingMic;
    private AlphaAnimation alphaAnimation;
    private OnBasketAnimationEnd onBasketAnimationEndListener;
    private boolean isBasketAnimating, isStartRecorded = false;
    private float micX, micY = 0;
    private AnimatorSet micAnimation;
    private TranslateAnimation translateAnimation1;
    private TranslateAnimation translateAnimation2;
    private Handler handler1, handler2;

    public AnimationHelper(Context context, ImageView basketImg, ImageView smallBlinkingMic) {
        this.context = context;
        this.smallBlinkingMic = smallBlinkingMic;
        this.basketImg = basketImg;
        animatedVectorDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.recv_basket_animated);
    }

    @SuppressLint("RestrictedApi")
    public void animateBasket(float basketInitialX) {
        logDebug("animateBasket");
        isBasketAnimating = true;
        clearAlphaAnimation(false);

        //save initial x,y values for mic icon
        if (micX == 0) {
            micX = smallBlinkingMic.getX();
            micY = smallBlinkingMic.getY();
        }

        micAnimation = (AnimatorSet) AnimatorInflaterCompat.loadAnimator(context, R.animator.delete_mic_animation);
        micAnimation.setTarget(smallBlinkingMic); // set the view you want to animate

        translateAnimation1 = initializeTranslateAnimation(basketInitialX, basketInitialX + MOVE_BASKET);
        translateAnimation2 = initializeTranslateAnimation(basketInitialX + MOVE_BASKET, basketInitialX);

        micAnimation.start();
        basketImg.setImageDrawable(animatedVectorDrawable);

        handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                basketImg.setVisibility(VISIBLE);
                basketImg.startAnimation(translateAnimation1);
            }
        }, DURATION_ALPHA);

        translateAnimation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (onBasketAnimationEndListener != null) {
                    onBasketAnimationEndListener.deactivateRecordButton();
                }
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                animatedVectorDrawable.start();
                handler2 = new Handler();
                handler2.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        basketImg.startAnimation(translateAnimation2);
                        smallBlinkingMic.setVisibility(INVISIBLE);
                    }
                }, DURATION_ALPHA);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        translateAnimation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                basketImg.setVisibility(INVISIBLE);
                isBasketAnimating = false;
                if (onBasketAnimationEndListener != null && !isStartRecorded) {
                    onBasketAnimationEndListener.onAnimationEnd();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private AlphaAnimation initializeAlphaAnimation(float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setDuration(DURATION_ALPHA);
        anim.setFillAfter(true);
        return anim;
    }

    private TranslateAnimation initializeTranslateAnimation(float fromX, float toX) {
        TranslateAnimation anim = new TranslateAnimation(fromX, toX, 0, 0);
        anim.setDuration(DURATION_ALPHA);
        return anim;
    }

    private void resetAnimation(Animation anim) {
        if (anim != null) {
            anim.reset();
            anim.cancel();
        }
    }

    private void resetAnimationSet(AnimatorSet anim) {
        if (anim != null) {
            anim.cancel();
        }
    }

    private void clearAnimation(ImageView imageView) {
        if (imageView != null) {
            imageView.clearAnimation();
        }
    }

    private void removeCallbacks(Handler handler) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /*
     * Stop the current animation and revert views back to default state
     */
    public void resetBasketAnimation() {
        logDebug("resetBasketAnimation()");
        if (!isBasketAnimating) return;

        resetAnimation(translateAnimation1);
        resetAnimation(translateAnimation2);
        resetAnimationSet(micAnimation);
        clearAnimation(smallBlinkingMic);
        clearAnimation(basketImg);
        removeCallbacks(handler1);
        removeCallbacks(handler2);


        basketImg.setVisibility(INVISIBLE);
        smallBlinkingMic.setX(micX);
        smallBlinkingMic.setY(micY);
        smallBlinkingMic.setVisibility(View.GONE);
        isBasketAnimating = false;
    }

    public void clearAlphaAnimation(boolean hideView) {
        logDebug("clearAlphaAnimation()");
        resetAnimation(alphaAnimation);
        clearAnimation(smallBlinkingMic);
        if (hideView) {
            smallBlinkingMic.setVisibility(View.GONE);
        }
    }

    public void animateSmallMicAlpha() {
        logDebug("animateSmallMicAlpha()");
        alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(DURATION_BLINK_MICRO);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        smallBlinkingMic.startAnimation(alphaAnimation);
    }

    public void moveRecordButtonAndSlideToCancelBack(final RelativeLayout recordBtnLayout, float initialX) {
        ValueAnimator anim = ValueAnimator.ofFloat(recordBtnLayout.getX(), initialX);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(DURATION_ALPHA);

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float x = (Float) animation.getAnimatedValue();
                recordBtnLayout.setX(x);
            }
        });

    }

    public void resetSmallMic() {
        smallBlinkingMic.setAlpha(1.0f);
        smallBlinkingMic.setScaleX(1.0f);
        smallBlinkingMic.setScaleY(1.0f);
    }

    public void setOnBasketAnimationEndListener(OnBasketAnimationEnd onBasketAnimationEndListener) {
        this.onBasketAnimationEndListener = onBasketAnimationEndListener;
    }

    protected void onAnimationEnd() {
        if (onBasketAnimationEndListener != null) {
            onBasketAnimationEndListener.onAnimationEnd();
        }
    }

    /*
     *Check if a new recording has started when the record button was pressed
     */
    public void setStartRecorded(boolean startRecorded) {
        isStartRecorded = startRecorded;
    }
}
