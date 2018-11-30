package mega.privacy.android.app.components;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import mega.privacy.android.app.utils.Util;

public class MarqueeTextView extends TextView {
    public MarqueeTextView(Context context) {
        super(context);
    }

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MarqueeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isMarqueeIsNecesary()) {

        }
    }

    boolean isMarqueeIsNecesary() {
        TextPaint textPaint = new TextPaint();
        textPaint.setTextSize(this.getPaint().getTextSize());
        if (textPaint.measureText(this.getText().toString()) > this.getMaxWidth()) {
            log("Text more large than textview --> Animate");
            return true;
        }
        else {
            log("Text less large than textview --> Not animate");
            return false;
        }
    }

    public static void log(String message) {
        Util.log("MarqueeTextView", message);
    }
}
