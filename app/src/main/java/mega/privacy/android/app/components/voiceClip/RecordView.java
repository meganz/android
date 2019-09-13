package mega.privacy.android.app.components.voiceClip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

import io.supercharge.shimmerlayout.ShimmerLayout;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

public class RecordView extends RelativeLayout {

    private final static int START_RECORD = 1;
    private final static int CANCEL_RECORD = 2;
    private final static int LOCK_RECORD = 3;
    private final static int FINISH_RECORD = 4;
    private final static int LESS_SECOND_RECORD = 5;
    private final static int FINISH_SOUND = 6;
    private final static int SOUND_START = R.raw.record_start;
    private final static int SOUND_END = R.raw.record_finished;
    private final static int SOUND_ERROR = R.raw.record_error;
    private final static int SECONDS_ALLOWED = 1;
    private final static int TIME_ANIMATION = 1500;
    private ImageView smallBlinkingMic, basketImg;
    private Chronometer counterTime;
    private TextView slideToCancel;
    private ShimmerLayout slideToCancelLayout;
    private RelativeLayout cancelRecordLayout;
    private TextView textCancelRecord;
    private float initialX, basketInitialX = 0;
    private long startTime, finalTime = 0;
    private Context context;
    private OnRecordListener recordListener;
    private boolean isSwiped = false;
    private MediaPlayer player = null;
    private AudioManager audioManager;
    private AnimationHelper animationHelper;
    private View layoutLock;
    private ImageView imageLock, imageArrow;
    private boolean isPadlockShouldBeShown = false;
    private boolean isLockpadShown = false;
    private boolean isRecordingNow = false;
    private Handler handlerStartRecord = new Handler();
    private Handler handlerShowPadLock = new Handler();
    private float previewX = 0;
    private Animation animJump, animJumpFast;
    private int count = 0;
    private DisplayMetrics outMetrics;
    final Runnable runPadLock = new Runnable() {
        @Override
        public void run() {
            if (isPadlockShouldBeShown) {
                showLock(true);
            }
        }
    };
    final Runnable runStartRecord = new Runnable() {
        @Override
        public void run() {
            if (isPadlockShouldBeShown) {
                handlerShowPadLock.postDelayed(runPadLock, TIME_ANIMATION);
            }
        }
    };
    private Display display;
    private float lastX, lastY;
    private float firstX, firstY;
    private UserBehaviour userBehaviour = UserBehaviour.NONE;

    public RecordView(Context context) {
        super(context);
        this.context = context;
        init(context);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context);
    }

    public static void collapse(final View v, int duration, int targetHeight) {
        int prevHeight = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public static void log(String message) {
        Util.log("RecordView", message);
    }

    private void init(Context context) {
        View view = View.inflate(context, R.layout.record_view_layout, null);
        addView(view);

        ViewGroup viewGroup = (ViewGroup) view.getParent();
        viewGroup.setClipChildren(false);

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        startTime = 0;
        isSwiped = false;
        slideToCancel = view.findViewById(R.id.slide_to_cancel);
        slideToCancel.setText(context.getString(R.string.slide_to_cancel).toUpperCase(Locale.getDefault()));
        slideToCancelLayout = view.findViewById(R.id.shimmer_layout);
        slideToCancelLayout.setVisibility(GONE);

        smallBlinkingMic = view.findViewById(R.id.glowing_mic);
        smallBlinkingMic.setVisibility(GONE);
        counterTime = view.findViewById(R.id.chrono_voice_clip);
        counterTime.setVisibility(GONE);
        basketImg = view.findViewById(R.id.basket_img);

        cancelRecordLayout = view.findViewById(R.id.rl_cancel_record);
        cancelRecordLayout.setVisibility(GONE);
        cancelRecordLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log("cancelRecordLayout:onClick -> hideViews");
                hideViews();
            }
        });

        textCancelRecord = view.findViewById(R.id.text_cancel_record);
        textCancelRecord.setText(context.getString(R.string.button_cancel).toUpperCase(Locale.getDefault()));

        animJump = AnimationUtils.loadAnimation(getContext(), R.anim.jump);
        animJumpFast = AnimationUtils.loadAnimation(getContext(), R.anim.jump_fast);
        layoutLock = view.findViewById(R.id.layout_lock);
        layoutLock.setVisibility(GONE);
        imageLock = view.findViewById(R.id.image_lock);
        imageLock.setVisibility(GONE);
        imageArrow = view.findViewById(R.id.image_arrow);
        imageArrow.setVisibility(GONE);

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        animationHelper = new AnimationHelper(context, basketImg, smallBlinkingMic);
    }

    public boolean isRecordingNow() {
        return isRecordingNow;
    }

    public void setRecordingNow(boolean recordingNow) {
        isRecordingNow = recordingNow;
    }

    private void hideViews() {
        log("hideViews");
        slideToCancelLayout.setVisibility(GONE);
        cancelRecordLayout.setVisibility(GONE);
        startStopCounterTime(false);
        playSound(Constants.TYPE_ERROR_RECORD);
        if (animationHelper == null) return;
        animationHelper.animateBasket(basketInitialX);
        animationHelper.setStartRecorded(false);
    }

    public void showLock(boolean needToShow) {
        if (needToShow) {
            count = 0;
            if (layoutLock.getVisibility() == View.GONE) {
                layoutLock.setVisibility(View.VISIBLE);
                imageArrow.setVisibility(VISIBLE);
                imageLock.setVisibility(VISIBLE);
                createAnimation(needToShow, Util.px2dp(175, outMetrics), 500);
            }
        } else {
            isPadlockShouldBeShown = false;
            count++;
            if (count == 1 && layoutLock.getVisibility() == View.VISIBLE) {
                layoutLock.setVisibility(View.GONE);
                isLockpadShown = false;
                createAnimation(needToShow, 10, 250);
            }
        }
    }

    private void createAnimation(final boolean toOpen, int value, int duration) {
        log("createAnimation");

        int prevHeight = layoutLock.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, value);
        if (!toOpen) {
            valueAnimator.setInterpolator(new DecelerateInterpolator());
        }
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                layoutLock.getLayoutParams().height = (int) animation.getAnimatedValue();
                layoutLock.requestLayout();
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (imageArrow == null || imageLock == null) return;

                if (toOpen) {
                    //It is expanded
                    isLockpadShown = true;
                    imageArrow.startAnimation(animJumpFast);
                    imageLock.startAnimation(animJump);
                } else {
                    //It is compressed
                    clearAnimations(imageArrow);
                    clearAnimations(imageLock);
                    layoutLock.setVisibility(View.GONE);
                }
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private boolean isLessThanOneSecond(long time) {
        return time <= SECONDS_ALLOWED;
    }

    private void recordListenerOptions(int option, long recordTime) {
        if (recordListener == null) return;
        switch (option) {
            case START_RECORD: {
                recordListener.onStart();
                break;
            }
            case LESS_SECOND_RECORD: {
                removeHandlerRecord();
                recordListener.onLessThanSecond();
                break;
            }
            case LOCK_RECORD: {
                recordListener.onLock();
                break;
            }
            case CANCEL_RECORD: {
                removeHandlerRecord();
                recordListener.onCancel();
                break;
            }
            case FINISH_SOUND: {
                recordListener.finishedSound();
                break;
            }
            case FINISH_RECORD: {
                removeHandlerRecord();
                recordListener.onFinish(recordTime);
                break;
            }
            default:
                break;
        }
    }

    private AssetFileDescriptor updateSound(int type) {
        int soundChoosed = 0;
        switch (type) {
            case Constants.TYPE_START_RECORD: {
                soundChoosed = SOUND_START;
                break;
            }
            case Constants.TYPE_END_RECORD: {
                soundChoosed = SOUND_END;
                break;
            }
            case Constants.TYPE_ERROR_RECORD: {
                soundChoosed = SOUND_ERROR;
                break;
            }
            default: {
                return null;
            }
        }
        return context.getResources().openRawResourceFd(soundChoosed);

    }

    private void typeStart(int type) {
        if (type == Constants.TYPE_START_RECORD) {
            recordListenerOptions(FINISH_SOUND, 0);
        }
    }

    public void startRecordingTime() {
        log("StartRecordingTime");
        slideToCancelLayout.setVisibility(VISIBLE);
        cancelRecordLayout.setVisibility(GONE);
        isSwiped = false;

        startStopCounterTime(true);
        log("StartRecordingTime");

        handlerStartRecord.postDelayed(runStartRecord, 100); //500 milliseconds delay to record
        isPadlockShouldBeShown = true;
    }

    public void playSound(int type) {
        log("playSound");
        if (player == null) player = new MediaPlayer();

        if (player == null || audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT || audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 || updateSound(type) == null) {
            typeStart(type);
            return;
        }

        try {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            AssetFileDescriptor afd = updateSound(type);
            player.reset();
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            float volume = (float) (1 - Math.log(maxVolume - volumeLevel / Math.log(maxVolume)));
            player.setVolume(volume, volume);
            player.setLooping(false);
            player.prepare();

        }
        catch (IOException e) {
            e.printStackTrace();
            typeStart(type);
            return;
        }

        player.start();

        if (type == Constants.TYPE_START_RECORD) {
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    recordListenerOptions(FINISH_SOUND, 0);
                    mp.reset();
                }
            });
        }
        else {
            player.setOnCompletionListener(null);
        }

    }

    private void displaySlideToCancel() {
        RelativeLayout.LayoutParams paramsSlide = (RelativeLayout.LayoutParams) slideToCancelLayout.getLayoutParams();
        paramsSlide.addRule(RelativeLayout.RIGHT_OF, R.id.chrono_voice_clip);
        paramsSlide.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        paramsSlide.setMargins(Util.px2dp(20, outMetrics), 0, 0, 0);
        slideToCancelLayout.setLayoutParams(paramsSlide);
        slideToCancelLayout.setShimmerColor(Color.WHITE);
        slideToCancelLayout.setShimmerAnimationDuration(TIME_ANIMATION);
        slideToCancelLayout.setAnimationReversed(true);
        slideToCancelLayout.startShimmerAnimation();
    }

    protected void onActionDown(RelativeLayout recordBtnLayout, MotionEvent motionEvent) {
        log("onActionDown()");

        animationHelper.setStartRecorded(true);
        animationHelper.resetBasketAnimation();
        animationHelper.resetSmallMic();
        startStopCounterTime(false);

        initialX = recordBtnLayout.getX();
        firstX = motionEvent.getRawX();
        firstY = motionEvent.getRawY();
        lastX = motionEvent.getRawX();
        lastY = motionEvent.getRawY();
        startTime = 0;
        isSwiped = false;

        userBehaviour = UserBehaviour.NONE;
        basketInitialX = basketImg.getX() - 90;

        recordListenerOptions(START_RECORD, 0);
    }

    private void slideToCancelTranslation(float translationX) {
        if (slideToCancelLayout == null) return;

        slideToCancelLayout.setTranslationX(translationX);
        slideToCancelLayout.setTranslationY(0);
        if (translationX == 0) {
            slideToCancelLayout.stopShimmerAnimation();
            slideToCancelLayout.setVisibility(GONE);
        }
    }

    public void recordButtonTranslation(RelativeLayout recordBtnLayout, float translationX, float translationY) {
        if (recordBtnLayout == null) return;
        recordBtnLayout.setTranslationX(translationX);
        recordBtnLayout.setTranslationY(translationY);

    }

    protected void onActionMove(RelativeLayout recordBtnLayout, MotionEvent motionEvent) {
        log("onActionMove()");
        if (isSwiped) return;

        UserBehaviour direction;
        float motionX = Math.abs(firstX - motionEvent.getRawX());
        float motionY = Math.abs(firstY - motionEvent.getRawY());

        if (motionX > motionY && lastX < firstX) {
            direction = UserBehaviour.CANCELING;
        } else if (motionY > motionX && lastY < firstY) {
            direction = UserBehaviour.LOCKING;
        } else {
            direction = UserBehaviour.NONE;
        }

        if (isRecordingNow && direction == UserBehaviour.CANCELING && (userBehaviour != UserBehaviour.CANCELING || ((motionEvent.getRawY() + (recordBtnLayout.getWidth() / 2)) > firstY)) && slideToCancelLayout.getVisibility() == VISIBLE && counterTime.getVisibility() == VISIBLE && recordListener != null) {
            if (slideToCancelLayout.getX() < counterTime.getLeft()) {
                log("CANCELING ");
                isSwiped = true;
                userBehaviour = UserBehaviour.CANCELING;
                animationHelper.moveRecordButtonAndSlideToCancelBack(recordBtnLayout, initialX);
                slideToCancelTranslation(0);
                hideViews();
                return;
            }

            if (previewX == 0) {
                previewX = recordBtnLayout.getX();
            } else if (recordBtnLayout.getX() <= (previewX - 150)) {
                showLock(false);
            }

            float valueToTranslation = -(firstX - motionEvent.getRawX());
            slideToCancelTranslation(valueToTranslation);
            recordButtonTranslation(recordBtnLayout, valueToTranslation, 0);


        } else if (isRecordingNow && direction == UserBehaviour.LOCKING && (userBehaviour != UserBehaviour.LOCKING || ((motionEvent.getRawX() + (recordBtnLayout.getWidth() / 2)) > firstX)) && layoutLock.getVisibility() == VISIBLE && isLockpadShown && recordListener != null) {
            if (((firstY - motionEvent.getRawY()) >= (layoutLock.getHeight() - (recordBtnLayout.getHeight() / 2)))) {
                log("LOCKING");
                userBehaviour = UserBehaviour.LOCKING;
                recordListenerOptions(LOCK_RECORD, 0);
                recordButtonTranslation(recordBtnLayout, 0, 0);
                slideToCancelTranslation(0);
                cancelRecordLayout.setVisibility(VISIBLE);
                return;

            }
            float valueToTranslation = -(firstY - motionEvent.getRawY());
            recordButtonTranslation(recordBtnLayout, 0, valueToTranslation);
        }

        lastX = motionEvent.getRawX();
        lastY = motionEvent.getRawY();
    }

    private void resetAnimationHelper() {
        if (animationHelper == null) return;
        animationHelper.clearAlphaAnimation(false);
        animationHelper.setStartRecorded(false);
    }

    protected void onActionCancel(RelativeLayout recordBtnLayout) {
        log("onActionCancel()");
        userBehaviour = UserBehaviour.NONE;
        removeHandlerPadLock();
        isPadlockShouldBeShown = false;
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;
        recordButtonTranslation(recordBtnLayout, 0, 0);
        slideToCancelTranslation(0);
        startStopCounterTime(false);
        resetAnimationHelper();
        showLock(false);
        recordListenerOptions(CANCEL_RECORD, 0);
    }

    protected void onActionUp(RelativeLayout recordBtnLayout) {
        log("onActionUp()");
        userBehaviour = UserBehaviour.NONE;
        if (startTime == 0) {
            finalTime = 0;
        } else {
            finalTime = System.currentTimeMillis() - startTime;
        }
        removeHandlerPadLock();
        isPadlockShouldBeShown = false;
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;

        recordButtonTranslation(recordBtnLayout, 0, 0);
        slideToCancelTranslation(0);
        startStopCounterTime(false);

        if (isLessThanOneSecond(finalTime / 1000) && !isSwiped) {
            log("onActionUp:less than a second");
            startTime = 0;
            recordListenerOptions(LESS_SECOND_RECORD, 0);
            resetAnimationHelper();
            return;
        }

        log("onActionUp:more than a second");
        showLock(false);
        if (!isSwiped) {
            recordListenerOptions(FINISH_RECORD, finalTime);
            if (animationHelper == null) return;
            animationHelper.clearAlphaAnimation(true);
            animationHelper.setStartRecorded(false);
        }
    }

    private void startStopCounterTime(boolean start) {
        if (counterTime == null) return;

        if (!start) {
            displaySlideToCancel();
            counterTime.stop();
            counterTime.setVisibility(GONE);
            startTime = 0;
            return;
        }

        if (counterTime.getVisibility() != GONE) {
            return;
        }
        startTime = System.currentTimeMillis();
        counterTime.setVisibility(VISIBLE);
        smallBlinkingMic.setVisibility(VISIBLE);
        animationHelper.animateSmallMicAlpha();
        counterTime.setBase(SystemClock.elapsedRealtime());
        counterTime.start();
    }

    public void setOnRecordListener(OnRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    public void setOnBasketAnimationEndListener(OnBasketAnimationEnd onBasketAnimationEndListener) {
        animationHelper.setOnBasketAnimationEndListener(onBasketAnimationEndListener);
    }

    private void removeHandlerPadLock() {
        handlerShowPadLock.removeCallbacksAndMessages(null);
        handlerShowPadLock.removeCallbacks(runPadLock);
    }

    private void removeHandlerRecord() {
        handlerStartRecord.removeCallbacksAndMessages(null);
        handlerStartRecord.removeCallbacks(runStartRecord);
    }

    private void clearAnimations(ImageView image1) {
        if (image1 == null) return;
        image1.clearAnimation();
        image1.setVisibility(GONE);
    }

    public enum UserBehaviour {
        CANCELING,
        LOCKING,
        NONE
    }

}
