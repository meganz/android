package mega.privacy.android.app.components.voiceClip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.graphics.drawable.AnimatorInflaterCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class AnimationHelper {
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
    private AlphaAnimation alphaAnimation1;
    private AlphaAnimation alphaAnimation2;
    private Handler handler1, handler2;
    private static int durationAlpha = 350;
    private static int durationTraslate = 250;
    private static int durationBasket = 800;
    private static int durationMicro = 500;

    public AnimationHelper(Context context, ImageView basketImg, ImageView smallBlinkingMic) {
        this.context = context;
        this.smallBlinkingMic = smallBlinkingMic;
        this.basketImg = basketImg;
        animatedVectorDrawable = AnimatedVectorDrawableCompat.create(context, R.drawable.recv_basket_animated);
    }

    @SuppressLint("RestrictedApi")
    public void animateBasket(float basketInitialX) {
        log("animateBasket");
        isBasketAnimating = true;
        clearAlphaAnimation(false);

        //save initial x,y values for mic icon
        if (micX == 0) {
            micX = smallBlinkingMic.getX();
            micY = smallBlinkingMic.getY();
        }

        micAnimation = (AnimatorSet) AnimatorInflaterCompat.loadAnimator(context, R.animator.delete_mic_animation);
        micAnimation.setTarget(smallBlinkingMic); // set the view you want to animate

        alphaAnimation1 = new AlphaAnimation(0.2f, 1.0f);
        alphaAnimation1.setDuration(durationAlpha);
        alphaAnimation1.setFillAfter(true);
        alphaAnimation2 = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation2.setDuration(durationAlpha);
        alphaAnimation2.setFillAfter(true);

        translateAnimation1 = new TranslateAnimation(basketInitialX, basketInitialX + 90, 0, 0);
        translateAnimation1.setDuration(durationTraslate);
        translateAnimation2 = new TranslateAnimation(basketInitialX + 90, basketInitialX, 0, 0);
        translateAnimation2.setDuration(durationAlpha);

        micAnimation.start();
        basketImg.setImageDrawable(animatedVectorDrawable);

        handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            @Override
            public void run() {
                basketImg.setVisibility(VISIBLE);
                basketImg.startAnimation(translateAnimation1);
            }
        }, durationAlpha);

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
                }, durationBasket);
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


    /*
    * Stop the current animation and revert views back to default state
    */
    public void resetBasketAnimation() {
        log("resetBasketAnimation()");
        if (!isBasketAnimating) return;
        
        if(translateAnimation1!=null){
            translateAnimation1.reset();
            translateAnimation1.cancel();
        }
        if(translateAnimation2!=null){
            translateAnimation2.reset();
            translateAnimation2.cancel();
        }
        if(micAnimation!=null) {
            micAnimation.cancel();
        }
        if(smallBlinkingMic!=null) {
            smallBlinkingMic.clearAnimation();
        }
        if(basketImg!=null) {
            basketImg.clearAnimation();
        }

        if (handler1 != null)
            handler1.removeCallbacksAndMessages(null);
        if (handler2 != null)
            handler2.removeCallbacksAndMessages(null);

        basketImg.setVisibility(INVISIBLE);
        smallBlinkingMic.setX(micX);
        smallBlinkingMic.setY(micY);
        smallBlinkingMic.setVisibility(View.GONE);
        isBasketAnimating = false;
    }


    public void clearAlphaAnimation(boolean hideView) {
        log("clearAlphaAnimation()");
        if(alphaAnimation != null){
            alphaAnimation.cancel();
            alphaAnimation.reset();
        }
        if(smallBlinkingMic!=null) {
            smallBlinkingMic.clearAnimation();
        }
        if (hideView) {
            smallBlinkingMic.setVisibility(View.GONE);
        }
    }

    public void animateSmallMicAlpha() {
        log("animateSmallMicAlpha()");
        alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setDuration(durationBasket);
        alphaAnimation.setRepeatMode(Animation.REVERSE);
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        smallBlinkingMic.startAnimation(alphaAnimation);
    }

    public void moveRecordButtonAndSlideToCancelBack(final RelativeLayout recordBtnLayout, float initialX) {
        ValueAnimator anim = ValueAnimator.ofFloat(recordBtnLayout.getX(), initialX);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.setDuration(durationMicro);

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
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

    public static void log(String message) {
        Util.log("AnimationHelper",message);
    }
}
