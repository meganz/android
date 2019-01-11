package mega.privacy.android.app.components.voiceClip;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;

import io.supercharge.shimmerlayout.ShimmerLayout;
import mega.privacy.android.app.R;

import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;

public class RecordView extends RelativeLayout {

    public static final int DEFAULT_CANCEL_BOUNDS = 1; //8dp
    private ImageView smallBlinkingMic, basketImg;
    private Chronometer counterTime;
    private TextView slideToCancel;
    private ShimmerLayout slideToCancelLayout;
    private ImageView arrow;
    private float initialX, basketInitialY, difX = 0;
    private float basketInitialX = 0;

    private float cancelBounds = DEFAULT_CANCEL_BOUNDS;
    private long startTime, elapsedTime = 0;
    private Context context;
    private OnRecordListener recordListener;
    private boolean isSwiped, isLessThanSecondAllowed = false;
    private boolean isSoundEnabled = true;
    private int RECORD_START = R.raw.record_start;
    private int RECORD_FINISHED = R.raw.record_finished;
    private int RECORD_ERROR = R.raw.record_error;
    private MediaPlayer player;
    private AnimationHelper animationHelper;
    private View layoutLock;
    private boolean flagRB = false;
    Handler handlerStartRecord = new Handler();
    Handler handlerShowPadLock = new Handler();

    public RecordView(Context context) {
        super(context);
        this.context = context;
        init(context, null, -1, -1);
    }


    public RecordView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(context, attrs, -1, -1);
    }

    public RecordView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context, attrs, defStyleAttr, -1);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        View view = View.inflate(context, R.layout.record_view_layout, null);
        addView(view);

        ViewGroup viewGroup = (ViewGroup) view.getParent();
        viewGroup.setClipChildren(false);

        arrow = view.findViewById(R.id.arrow);
        slideToCancel = view.findViewById(R.id.slide_to_cancel);
        smallBlinkingMic = view.findViewById(R.id.glowing_mic);
        counterTime = view.findViewById(R.id.counter_tv);
        basketImg = view.findViewById(R.id.basket_img);
        slideToCancelLayout = view.findViewById(R.id.shimmer_layout);
        layoutLock = view.findViewById(R.id.layout_lock);
        layoutLock.setVisibility(View.GONE);

        hideViews(true);

        if (attrs != null && defStyleAttr == -1 && defStyleRes == -1) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordView, defStyleAttr, defStyleRes);

            int slideArrowResource = typedArray.getResourceId(R.styleable.RecordView_slide_to_cancel_arrow, -1);
            String slideToCancelText = typedArray.getString(R.styleable.RecordView_slide_to_cancel_text);
            int slideMarginRight = (int) typedArray.getDimension(R.styleable.RecordView_slide_to_cancel_margin_right, 30);
            int counterTimeColor = typedArray.getColor(R.styleable.RecordView_counter_time_color, -1);
            int arrowColor = typedArray.getColor(R.styleable.RecordView_slide_to_cancel_arrow_color, -1);
            int cancelBounds = typedArray.getDimensionPixelSize(R.styleable.RecordView_slide_to_cancel_bounds, -1);

            if (cancelBounds != -1) {
                setCancelBounds(cancelBounds, false);//don't convert it to pixels since it's already in pixels
            }

            if (slideArrowResource != -1) {
                Drawable slideArrow = AppCompatResources.getDrawable(getContext(), slideArrowResource);
                arrow.setImageDrawable(slideArrow);
            }

            if (slideToCancelText != null){
                slideToCancel.setText(slideToCancelText);
            }

            if (counterTimeColor != -1) {
                setCounterTimeColor(counterTimeColor);
            }

            if (arrowColor != -1) {
                setSlideToCancelArrowColor(arrowColor);
            }

            setMarginRight(slideMarginRight, true);
            typedArray.recycle();
        }
        animationHelper = new AnimationHelper(context, basketImg, smallBlinkingMic);
    }


    private void hideViews(boolean hideSmallMic) {
        log("hideViews()");
        slideToCancelLayout.setVisibility(GONE);
        counterTime.setVisibility(GONE);
        if (hideSmallMic){
            smallBlinkingMic.setVisibility(GONE);
        }
    }

    private void showViews() {
        log("showViews()");
        slideToCancelLayout.setVisibility(VISIBLE);
        smallBlinkingMic.setVisibility(VISIBLE);
        counterTime.setVisibility(VISIBLE);
    }

    public void showLock(boolean flag){
        log("showLock() -> "+flag);
        if(flag){
            layoutLock.setVisibility(View.VISIBLE);
        }else{
            layoutLock.setVisibility(View.GONE);
        }
    }

    private boolean isLessThanOneSecond(long time) {
        return time <= 1500;
    }

    private void playSound(int soundRes) {
        log("playSound()");
        if (isSoundEnabled) {
            if (soundRes == 0)
                return;
            try {
                player = new MediaPlayer();
                AssetFileDescriptor afd = context.getResources().openRawResourceFd(soundRes);
                if (afd == null) return;
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                player.prepare();
                player.start();
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();
                    }
                });
                player.setLooping(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    final Runnable runStartRecord = new Runnable(){
        @Override
        public void run() {
            if(flagRB){
                log("runStartRecord()");
                counterTime.setBase(SystemClock.elapsedRealtime());
                counterTime.start();
                handlerShowPadLock.postDelayed(runPadLock, 3000);
                if (recordListener != null) {
                    recordListener.onStart();
                }
            }
        }
    };

    final Runnable runPadLock = new Runnable(){
        @Override
        public void run() {
            if(flagRB){
                log("runPadLock() ");
                showLock(true);
            }
        }
    };

    protected void onActionDown(RecordButton recordBtn, MotionEvent motionEvent) {
        log("onActionDown()");
        animationHelper.setStartRecorded(true);
        animationHelper.resetBasketAnimation();
        animationHelper.resetSmallMic();
        slideToCancelLayout.startShimmerAnimation();
        initialX = recordBtn.getX();
        playSound(RECORD_START);
        basketInitialY = basketImg.getY() + 90;
        basketInitialX = basketImg.getX() - 90;
        showViews();
        startTime = System.currentTimeMillis();
        isSwiped = false;
        animationHelper.animateSmallMicAlpha();
        handlerStartRecord.postDelayed(runStartRecord, 500); //500 milliseconds delay to record
        flagRB = true;
    }

    protected void onActionMove(RecordButton recordBtn, MotionEvent motionEvent) {
        log("onActionMove()");

        long time = System.currentTimeMillis() - startTime;
        if (!isSwiped) {
            log("onActionMove() - !isSwiped - slideToCancelLayout.getX(): "+slideToCancelLayout.getX()+", counterTime.getRight("+counterTime.getRight()+") + cancelBounds("+cancelBounds+"): "+(counterTime.getRight() + cancelBounds));

            //Swipe To Cancel
            if (slideToCancelLayout.getX() != 0 && slideToCancelLayout.getX() <= counterTime.getRight() + cancelBounds) {
                //if the time was less than one second then do not start basket animation
                if (isLessThanOneSecond(time)) {
                    log("onActionMove() - Swipe To Cancel less than one second --> no basket animation\");");
                    hideViews(true);
                    animationHelper.clearAlphaAnimation(false);
                    animationHelper.onAnimationEnd();

                } else {
                    log("onActionMove() - Swipe To Cancel more than one second --> start basket animation");
                    hideViews(false);
//                    animationHelper.animateBasket(basketInitialY);
                    animationHelper.animateBasket(basketInitialX);

                }

                animationHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, slideToCancelLayout, initialX, difX);

                counterTime.stop();
                slideToCancelLayout.stopShimmerAnimation();
                isSwiped = true;

                animationHelper.setStartRecorded(false);

                if (recordListener != null) {
                    recordListener.onCancel();
                }

            }else{
                log("onActionMove() - Swipe out of bounds");
                //if statement is to Prevent Swiping out of bounds
                if (motionEvent.getRawX() < initialX) {
                    log("onActionMove() - a");

                    recordBtn.animate().x(motionEvent.getRawX()).setDuration(0).start();

                    if (difX == 0) {
                        difX = (initialX - slideToCancelLayout.getX());
                    }

                    slideToCancelLayout.animate().x(motionEvent.getRawX() - difX).setDuration(0).start();
                }
            }
        }
    }

    protected void onActionUp(RecordButton recordBtn) {
        log("onActionUp()");
        elapsedTime = System.currentTimeMillis() - startTime;
        if (!isLessThanSecondAllowed && isLessThanOneSecond(elapsedTime) && !isSwiped) {
            log("onActionUp() - less than a second");
            if (recordListener != null){
                recordListener.onLessThanSecond();
            }
            animationHelper.setStartRecorded(false);
            playSound(RECORD_ERROR);

        } else {
            log("onActionUp() - more than a second");
            if (recordListener != null && !isSwiped) {
                recordListener.onFinish(elapsedTime);
            }
            animationHelper.setStartRecorded(false);

            if (!isSwiped) {
                playSound(RECORD_FINISHED);
            }
        }
        //if user has swiped then do not hide SmallMic since it will be hidden after swipe Animation
        hideViews(!isSwiped);

        if (!isSwiped) {
            animationHelper.clearAlphaAnimation(true);
        }
        animationHelper.moveRecordButtonAndSlideToCancelBack(recordBtn, slideToCancelLayout, initialX, difX);
        flagRB = false;
        counterTime.stop();
        slideToCancelLayout.stopShimmerAnimation();
    }


    private void setMarginRight(int marginRight, boolean convertToDp) {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) slideToCancelLayout.getLayoutParams();
        if (convertToDp) {
            layoutParams.rightMargin = (int) Util.toPixel(marginRight, context);
        }else {
            layoutParams.rightMargin = marginRight;
        }
        slideToCancelLayout.setLayoutParams(layoutParams);
    }

    public void setOnRecordListener(OnRecordListener recrodListener) {
        log("setOnRecordListener()");
        this.recordListener = recrodListener;
    }
    public void setOnBasketAnimationEndListener(OnBasketAnimationEnd onBasketAnimationEndListener) {
        log("setOnBasketAnimationEndListener()");
        animationHelper.setOnBasketAnimationEndListener(onBasketAnimationEndListener);
    }
    public void setSoundEnabled(boolean isEnabled) {
        isSoundEnabled = isEnabled;
    }
    public void setLessThanSecondAllowed(boolean isAllowed) {
        isLessThanSecondAllowed = isAllowed;
    }
    public void setSlideToCancelText(String text) {
        slideToCancel.setText(text);
    }
    public void setSlideToCancelTextColor(int color) {
        slideToCancel.setTextColor(color);
    }
    public void setSmallMicColor(int color) {
        smallBlinkingMic.setColorFilter(color);
    }
    public void setSmallMicIcon(int icon) {
        smallBlinkingMic.setImageResource(icon);
    }
    public void setSlideMarginRight(int marginRight) {
        setMarginRight(marginRight, true);
    }
    public void setCustomSounds(int startSound, int finishedSound, int errorSound) {
        //0 means do not play sound
        RECORD_START = startSound;
        RECORD_FINISHED = finishedSound;
        RECORD_ERROR = errorSound;
    }
    public float getCancelBounds() {
        return cancelBounds;
    }
    public void setCancelBounds(float cancelBounds) {
        setCancelBounds(cancelBounds, true);
    }
    //set Chronometer color
    public void setCounterTimeColor(int color) {
        counterTime.setTextColor(color);
    }
    public void setSlideToCancelArrowColor(int color){
        arrow.setColorFilter(color);
    }
    private void setCancelBounds(float cancelBounds, boolean convertDpToPixel) {
        float bounds = convertDpToPixel ? Util.toPixel(cancelBounds, context) : cancelBounds;
        this.cancelBounds = bounds;
    }

    public void destroyHandlers(){
        if (handlerStartRecord != null){
            handlerStartRecord.removeCallbacksAndMessages(null);
        }
        if (handlerShowPadLock != null){
            handlerShowPadLock.removeCallbacksAndMessages(null);
        }
    }
    public static void log(String message) {
        Util.log("RecordView",message);
    }
}
