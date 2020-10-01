package mega.privacy.android.app.components.voiceClip;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatSeekBar;
import java.lang.ref.WeakReference;

public class DetectorSeekBar  extends AppCompatSeekBar {
    private static final int LONG_CLICK_DELAY = 500;
    private LongClickChecker mLongClickChecker;
    private ClickChecker mClickChecker;
    private IListener mListener;

    public DetectorSeekBar(Context context) {
        this(context, null);
    }

    public DetectorSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DetectorSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLongClickChecker = new LongClickChecker(this);
        mClickChecker = new ClickChecker(this);
    }

    public void setEventListener(IListener listener) {
        mListener = listener;
        mLongClickChecker.setLongClickListener(listener);
        mClickChecker.setClickListener(listener);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isEnabled()) {
                    postDelayed(mLongClickChecker, LONG_CLICK_DELAY);
                    mClickChecker.x = event.getX();
                    mClickChecker.y = event.getY();
                    mClickChecker.time = event.getEventTime();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                removeCallbacks(mLongClickChecker);
                mClickChecker.onMoveEvent();
                break;
            case MotionEvent.ACTION_UP:
                removeCallbacks(mLongClickChecker);
                if (isEnabled()
                        && mClickChecker.checkCondition(event)) {
                    post(mClickChecker);
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(mLongClickChecker);
                removeCallbacks(mClickChecker);
                break;
        }
        return super.onTouchEvent(event);
    }

    private static class LongClickChecker implements Runnable {

        private WeakReference<IListener> mListenerRef;
        private WeakReference<DetectorSeekBar> mViewRef;

        LongClickChecker(DetectorSeekBar view) {
            mViewRef = new WeakReference<>(view);
        }

        void setLongClickListener(IListener listener) {
            mListenerRef = new WeakReference<>(listener);
        }

        @Override
        public void run() {
            if (mListenerRef != null && mListenerRef.get() != null
                    && mViewRef != null && mViewRef.get() != null) {
                mListenerRef.get().onLongClick(mViewRef.get());
            }
        }
    }

    private static class ClickChecker implements Runnable {

        private long time = 0;
        public float x;
        public float y;
        private boolean mMoved = false;

        private WeakReference<IListener> mListenerRef;
        private WeakReference<DetectorSeekBar> mViewRef;

        ClickChecker(DetectorSeekBar view) {
            mViewRef = new WeakReference<>(view);
        }

        @Override
        public void run() {
            if (mListenerRef != null && mListenerRef.get() != null
                    && mViewRef != null && mViewRef.get() != null) {
                mListenerRef.get().onClick(mViewRef.get());
            }
        }

        void onMoveEvent() {
            mMoved = true;
        }

        void setClickListener(IListener listener) {
            mListenerRef = new WeakReference<>(listener);
        }

        boolean checkCondition(MotionEvent upEvent) {
            if (upEvent != null) {
                // have moved cancel click
                if (mMoved) {
                    mMoved = false;
                    return false;
                }
                //ACTION_DOWN  ACTION_UP time too long cancel click
                boolean timeCorrect = upEvent.getEventTime() - time < LONG_CLICK_DELAY;
                time = 0;

                return timeCorrect;
            }
            return false;
        }
    }

    public interface IListener {
        void onClick(DetectorSeekBar detectorSeekBar);
        void onLongClick(DetectorSeekBar detectorSeekBar);
    }
}