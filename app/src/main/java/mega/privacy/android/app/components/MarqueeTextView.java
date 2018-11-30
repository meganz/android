package mega.privacy.android.app.components;

import android.content.Context;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import mega.privacy.android.app.utils.Util;

public class MarqueeTextView extends TextView {

    private Handler mHandler = new Handler();
    private long mDelay = 200;
    private int mFirstIndex;
    private int mLastIndex;
    private CharSequence text;

    public MarqueeTextView(Context context) {
        super(context);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public boolean isMarqueeIsNecesary() {
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(getPaint().getTextSize());
        text = getText();
        String[] s = text.toString().split(" ");
        if (s != null && s.length > 2) {
            String s1 = s[0] + " " + s[1] + " ";
            log("s1: "+s1);
            for (int i=0; i<s1.length(); i++) {
                text = text + " ";
            }
        }

        if (textPaint.measureText(text.toString()) > getMaxWidth()) {
            log("Text more large than textview --> Animate");
            mLastIndex = text.length()-1;
            while (textPaint.measureText(text.subSequence(0, mLastIndex-1).toString()) > getMaxWidth()) {
                mLastIndex--;
            }
            animateText();
            return true;
        }
        else {
            log("Text less large than textview --> Not animate");
            return false;
        }
    }

    private Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(text.subSequence(mFirstIndex, mLastIndex));
            mFirstIndex++;
            mLastIndex++;

            if (mLastIndex <= text.length()) {
                mHandler.postDelayed(characterAdder, mDelay);
            }
            else {
                mHandler.removeCallbacksAndMessages(null);
            }
        }
    };

    public void animateText () {
        mFirstIndex = 0;
        mHandler.removeCallbacksAndMessages(characterAdder);
        mHandler.postDelayed(characterAdder, mDelay);
    }

    public static void log(String message) {
        Util.log("MarqueeTextView", message);
    }
}
