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

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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
    private final static int TIME_ANIMATION = 500;
    private ImageView smallBlinkingMic, basketImg;
    private Chronometer counterTime;
    private TextView slideToCancel;
    private ShimmerLayout slideToCancelLayout;
    private RelativeLayout cancelRecordLayout;
    private TextView textCancelRecord;
    private float initialButtonX, basketInitialX = 0;
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
    private boolean isLockShown = false;
    private boolean isRecordingNow = false;
    private Handler handlerStartRecord = new Handler();
    private Handler handlerShowPadLock = new Handler();
    private float previewX = 0;
    private Animation animJump, animJumpFast;
    private DisplayMetrics outMetrics;
    private static int DURATION_EXPAND =  1500;
    private UserBehaviour direction = null;
    private int countHide, countShow = 0;
    ValueAnimator valueAnimator = null;
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
    private float lastX, lastY = 0;
    private float firstX, firstY = 0;
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
                logDebug("cancelRecordLayout:onClick -> hideViews");
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
        slideToCancelLayout.setVisibility(GONE);
        cancelRecordLayout.setVisibility(GONE);
        startStopCounterTime(false);
        playSound(TYPE_ERROR_RECORD);
        removeHandlers();
        if (animationHelper == null) return;
        animationHelper.animateBasket(basketInitialX);
        animationHelper.setStartRecorded(false);
    }

    private void showLock(boolean needToShow) {
        if (needToShow) {
            if(isLockShown || layoutLock.getVisibility() == VISIBLE || countShow > 0) return;
            logDebug("Showing recording lock");
            countShow ++;
            countHide = 0;
            imageArrow.setVisibility(VISIBLE);
            imageLock.setVisibility(VISIBLE);
            isPadlockShouldBeShown = false;
            initializeHeight();
            createAnimation(px2dp(175, outMetrics), DURATION_EXPAND);
        } else {
            if(!isLockShown || layoutLock.getVisibility() == View.GONE || countHide > 0) return;
            logDebug("Hiding recording lock");
            countHide ++;
            countShow = 0;
            isPadlockShouldBeShown = false;
            hideLock();
        }
    }

    private void hideLock(){
        layoutLock.setVisibility(View.GONE);
        isLockShown = false;
        clearAnimations(imageArrow);
        clearAnimations(imageLock);
        initializeHeight();

    }

    private void initializeHeight(){
        layoutLock.getLayoutParams().height = 0;
        layoutLock.requestLayout();
    }

    private void createAnimation(int value, int duration) {
        logDebug("createAnimation");

        int prevHeight = layoutLock.getLayoutParams().height;
        destroyValueAnimator();

        valueAnimator = ValueAnimator.ofInt(prevHeight, value);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                layoutLock.getLayoutParams().height = (int) animation.getAnimatedValue();
                layoutLock.requestLayout();
            }
        });

        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator animator){
                initializeHeight();
                layoutLock.setVisibility(View.VISIBLE);

            }
            @Override
            public void onAnimationEnd(Animator animation) {
                if (imageArrow == null || imageLock == null) return;
                //It is expanded
                isLockShown = true;
                imageArrow.startAnimation(animJumpFast);
                imageLock.startAnimation(animJump);
                return;
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private boolean isLessThanOneSecond(long time) {
        return time <= SECONDS_ALLOWED;
    }

    private void disableLock(){
        removeHandlers();
        showLock(false);
    }

    private void recordListenerOptions(int option, long recordTime) {
        if (recordListener == null) return;
        switch (option) {
            case START_RECORD: {
                recordListener.onStart();
                break;
            }
            case LESS_SECOND_RECORD: {
                disableLock();
                recordListener.onLessThanSecond();
                break;
            }
            case LOCK_RECORD: {
                disableLock();
                recordListener.onLock();
                break;
            }
            case CANCEL_RECORD: {
                disableLock();
                recordListener.onCancel();
                break;
            }
            case FINISH_RECORD: {
                disableLock();
                recordListener.onFinish(recordTime);
                break;
            }
            case FINISH_SOUND: {
                recordListener.finishedSound();
                break;
            }
            default:
                break;
        }
    }

    private AssetFileDescriptor updateSound(int type) {
        int soundChoosed = 0;
        switch (type) {
            case TYPE_START_RECORD: {
                soundChoosed = SOUND_START;
                break;
            }
            case TYPE_END_RECORD: {
                soundChoosed = SOUND_END;
                break;
            }
            case TYPE_ERROR_RECORD: {
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
        if (type == TYPE_START_RECORD) {
            recordListenerOptions(FINISH_SOUND, 0);
        }
    }

    public void startRecordingTime() {
        slideToCancelLayout.setVisibility(VISIBLE);
        cancelRecordLayout.setVisibility(GONE);
        isSwiped = false;

        startStopCounterTime(true);
        initHandlers();
    }

    private void initHandlers() {
        removeHandlers();
        isLockShown =false;
        isPadlockShouldBeShown = true;
        handlerStartRecord.postDelayed(runStartRecord, 100); //500 milliseconds delay to record
    }

    public void playSound(int type) {
        if (player == null) player = new MediaPlayer();

        if (player == null || audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT || audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE|| audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0 ||updateSound(type) == null) {
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

        if (type == TYPE_START_RECORD) {
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
        paramsSlide.setMargins(px2dp(20, outMetrics), 0, 0, 0);
        slideToCancelLayout.setLayoutParams(paramsSlide);
        slideToCancelLayout.setShimmerColor(Color.WHITE);
        slideToCancelLayout.setShimmerAnimationDuration(TIME_ANIMATION);
        slideToCancelLayout.setAnimationReversed(true);
        slideToCancelLayout.startShimmerAnimation();
    }

    protected void onActionDown(RelativeLayout recordBtnLayout, MotionEvent motionEvent) {
        animationHelper.setStartRecorded(true);
        animationHelper.resetBasketAnimation();
        animationHelper.resetSmallMic();
        startStopCounterTime(false);

        initialButtonX = recordBtnLayout.getX();
        firstX = motionEvent.getRawX();
        lastX = firstX;
        firstY = motionEvent.getRawY();
        lastY = firstY;
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
        logDebug("onActionMove()");
        if (isSwiped) return;

        float motionX = Math.abs(firstX - motionEvent.getRawX());
        float motionY = Math.abs(firstY - motionEvent.getRawY());

        if (motionX > motionY && lastX < firstX) {
            if(direction != UserBehaviour.CANCELING){
                direction = UserBehaviour.CANCELING;
            }
        } else if (motionY > motionX && lastY < firstY) {
            if(direction != UserBehaviour.LOCKING){
                direction = UserBehaviour.LOCKING;
            }
        } else if(direction != UserBehaviour.NONE){
            direction = UserBehaviour.NONE;
        }

        if (isRecordingNow && direction == UserBehaviour.CANCELING && (userBehaviour != UserBehaviour.CANCELING || ((motionEvent.getRawY() + (recordBtnLayout.getWidth() / 2)) > firstY)) && slideToCancelLayout.getVisibility() == VISIBLE && counterTime.getVisibility() == VISIBLE && recordListener != null) {
            if (slideToCancelLayout.getX() < counterTime.getLeft()) {
                logDebug("Canceling voice clip ");
                isSwiped = true;
                userBehaviour = UserBehaviour.CANCELING;
                animationHelper.moveRecordButtonAndSlideToCancelBack(recordBtnLayout, initialButtonX);
                slideToCancelTranslation(0);
                hideViews();
                return;
            }

            if (previewX == 0) {
                previewX = recordBtnLayout.getX();

            } else if (recordBtnLayout.getX() + 100 <= previewX && isLockShown) {
                showLock(false);

            } else if (recordBtnLayout.getX() + 100 > previewX && !isLockShown) {
                showLock(true);

            }

            float valueToTranslation = -(firstX - motionEvent.getRawX());
            slideToCancelTranslation(valueToTranslation);
            recordButtonTranslation(recordBtnLayout, valueToTranslation, 0);


        } else if (isRecordingNow && direction == UserBehaviour.LOCKING && (userBehaviour != UserBehaviour.LOCKING || ((motionEvent.getRawX() + (recordBtnLayout.getWidth() / 2)) > firstX)) && layoutLock.getVisibility() == VISIBLE && isLockShown && recordListener != null) {
            if (((firstY - motionEvent.getRawY()) >= (layoutLock.getHeight() - (recordBtnLayout.getHeight() / 2)))) {
                logDebug("Locking voice clip");
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
        userBehaviour = UserBehaviour.NONE;
        removeHandlers();
        initializeValues();
        recordButtonTranslation(recordBtnLayout, 0, 0);
        slideToCancelTranslation(0);
        startStopCounterTime(false);
        resetAnimationHelper();
        recordListenerOptions(CANCEL_RECORD, 0);
    }
    private void initializeValues(){
        firstX = 0;
        firstY = 0;
        lastX = 0;
        lastY = 0;
        isPadlockShouldBeShown = true;
        initializeHeight();
    }

    private void removeHandlers(){
        removeHandlerPadLock();
        removeHandlerRecord();
    }

    protected void onActionUp(RelativeLayout recordBtnLayout) {
        userBehaviour = UserBehaviour.NONE;
        if (startTime == 0) {
            finalTime = 0;
        } else {
            finalTime = System.currentTimeMillis() - startTime;
        }

        initializeValues();

        recordButtonTranslation(recordBtnLayout, 0, 0);
        slideToCancelTranslation(0);
        startStopCounterTime(false);

        if (isLessThanOneSecond(finalTime / 1000) && !isSwiped) {
            startTime = 0;
            recordListenerOptions(LESS_SECOND_RECORD, 0);
            resetAnimationHelper();
            return;
        }

        logDebug("More than a second");

        if (!isSwiped) {
            recordListenerOptions(FINISH_RECORD, finalTime);
            if (animationHelper == null) return;
            animationHelper.clearAlphaAnimation(true);
            animationHelper.setStartRecorded(false);
            return;
        }
        showLock(false);
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
        cancelAnimator();
        if(handlerShowPadLock == null) return;
        handlerShowPadLock.removeCallbacksAndMessages(null);
        handlerShowPadLock.removeCallbacks(runPadLock);
    }

    private void removeHandlerRecord() {
        if(handlerStartRecord == null) return;
        handlerStartRecord.removeCallbacksAndMessages(null);
        handlerStartRecord.removeCallbacks(runStartRecord);
    }

    private void cancelAnimator(){
        isLockShown = false;
        countShow = 0;
        countHide = 0;
        clearAnimations(imageArrow);
        clearAnimations(imageLock);
        layoutLock.setVisibility(View.GONE);
        destroyValueAnimator();
    }

    private void destroyValueAnimator(){
        if(valueAnimator == null) return;
        valueAnimator.removeAllListeners();
        valueAnimator.cancel();
        valueAnimator = null;
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
