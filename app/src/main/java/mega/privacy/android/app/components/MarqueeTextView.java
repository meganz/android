package mega.privacy.android.app.components;

import android.content.Context;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import timber.log.Timber;

public class MarqueeTextView extends AppCompatTextView {

    private final Handler mHandler = new Handler();
    private final long mDelay = 200;
    private int mFirstIndex;
    private int mLastIndex;
    private int scrollIndex;
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

    public void isMarqueeIsNecessary(Context context) {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(getPaint().getTextSize());

        formatString();

        scroll(textPaint, getMaxWidth());
    }

    private void scroll(TextPaint textPaint, int width) {
        if (textPaint.measureText(text.toString()) > width) {
            Timber.d("Text more large than textview --> Animate");
            mLastIndex = text.length() - 1;
            while (textPaint.measureText(text.subSequence(0, mLastIndex - 1).toString()) > width) {
                mLastIndex--;
            }
            animateText();
        } else {
            Timber.d("Text less large than textview --> Not animate");
            setText(text);
        }
    }

    public void formatString() {
        String stringToFormat = getText().toString();
        stringToFormat = stringToFormat.replace("[A]", "");
        scrollIndex = stringToFormat.indexOf("[/A]");
        stringToFormat = stringToFormat.replace("[/A]", "");
        text = stringToFormat;
        setText(text);
    }

    private final Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            text = text + " ";
            setText(text.subSequence(mFirstIndex, mLastIndex));
            mFirstIndex++;
            mLastIndex++;

            if (mFirstIndex <= scrollIndex) {
                mHandler.postDelayed(characterAdder, mDelay);
            } else {
                mHandler.removeCallbacksAndMessages(null);
            }
        }
    };

    public void animateText() {
        mFirstIndex = 0;
        mHandler.removeCallbacksAndMessages(characterAdder);
        mHandler.postDelayed(characterAdder, mDelay);
    }
}